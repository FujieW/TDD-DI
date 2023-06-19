package geektime.tdd.di;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

public class Context {

    private Map<Class<?>, Provider<?>> providers = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        providers.put(type, (Provider<Type>)() -> instance);

    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation) {
        Constructor<Implementation> injectConstructor = getInjectConstructor(implementation);

        providers.put(type, new ConstructorInjectionProvider<>(type, injectConstructor));
    }

    public <Type> Optional<Type> get(Class<Type> typeClass) {
        return Optional.ofNullable(providers.get(typeClass)).map(provider -> (Type)provider.get());
    }

    class ConstructorInjectionProvider<Type> implements Provider<Type>{

        private Class<?> componentType;
        private Constructor<Type> injectConstructor;

        private boolean constructing = false;

        public ConstructorInjectionProvider(Class<?> componentType, Constructor<Type> injectConstructor) {
            this.componentType = componentType;
            this.injectConstructor = injectConstructor;
        }

        @Override
        public Type get() {
            try {
                if (constructing) {
                    throw new CyclicDependenciesFoundException(componentType);
                }
                constructing = true;
                // 这里是一个递归，处理依赖传递的情况
                Object[] dependencies = Arrays.stream(injectConstructor.getParameters())
                    .map(p -> Context.this.get(p.getType()).orElseThrow(() -> new DependencyNotFoundException(p.getType(), this.componentType)))
                    .toArray(Object[]::new);
                return injectConstructor.newInstance(dependencies);
            } catch (CyclicDependenciesFoundException e){
                throw new CyclicDependenciesFoundException(componentType, e);
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            } finally {
                constructing = false;
            }
        }
    }


    private static <Type> Constructor<Type> getInjectConstructor(Class<Type> implementation) {

        List<Constructor<?>> injectConstructors = Arrays.stream(implementation.getConstructors())
            .filter(c -> c.isAnnotationPresent(Inject.class)).collect(Collectors.toList());

        if (injectConstructors.size() > 1) {
            throw new IllegalComponentException();
        }

        return (Constructor<Type>)injectConstructors.stream().findFirst().orElseGet(() -> {
            try {
                return implementation.getConstructor();
            } catch (NoSuchMethodException e) {
                throw new IllegalComponentException();
            }
        });
    }
}
