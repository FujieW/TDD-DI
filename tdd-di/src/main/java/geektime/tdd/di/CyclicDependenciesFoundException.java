package geektime.tdd.di;

import java.util.HashSet;
import java.util.Set;

public class CyclicDependenciesFoundException extends RuntimeException{

    private Set<Class<?>> components = new HashSet<>();

    public CyclicDependenciesFoundException(Class<?> clazz) {
        components.add(clazz);
    }

    public CyclicDependenciesFoundException(Class<?> componentType, CyclicDependenciesFoundException cyclicDependenciesFoundException) {
        this.components.add(componentType);
        this.components.addAll(cyclicDependenciesFoundException.getComponents());
    }

    public Set<Class<?>> getComponents() {
        return components;
    }
}
