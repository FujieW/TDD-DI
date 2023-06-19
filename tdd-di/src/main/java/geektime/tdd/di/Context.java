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

        providers.put(type, (Provider<Type>)() -> {
            try {
                Object[] dependencies = Arrays.stream(injectConstructor.getParameters())
                    .map(p -> get(p.getType()).orElseThrow(DependencyNotFoundException::new))
                    .toArray(Object[]::new);
                return (Type)injectConstructor.newInstance(dependencies);
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
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

    public <Type> Optional<Type> get(Class<Type> typeClass) {
        return Optional.ofNullable(providers.get(typeClass)).map(provider -> (Type)provider.get());
    }
}
