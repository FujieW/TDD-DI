package geektime.tdd.di;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ContextTest {

    private Context context;

    @BeforeEach
    public void setUp() {
        context = new Context();
    }

    @Nested
    public class ComponentConstruction {
        // TODO : instance
        @Test
        public void should_bind_type_to_a_specific_instance() {
            Component instance = new Component() {};
            context.bind(Component.class, instance);
            assertSame(instance, context.get(Component.class).get());
        }

        // TODO ： abstract class
        // TODO : interface
        @Test
        public void should_() {
            Optional<Component> component = context.get(Component.class);
            assertTrue(!component.isPresent());
        }

        @Nested
        public class ConstructorInjection {
            // TODO： No args Constructor
            @Test
            public void should_bind_type_to_a_class_with_default_constructor() {
                context.bind(Component.class, ComponentWithDefaultConstructor.class);
                Component component = context.get(Component.class).get();
                assertNotNull(component);
                assertTrue(component instanceof ComponentWithDefaultConstructor);

            }

            // TODO： with dependencies
            @Test
            public void should_bind_to_a_type_with_inject_constructor() {
                Dependency dependency = new Dependency() {};

                context.bind(Component.class, ComponentWithInjectConstructor.class);
                context.bind(Dependency.class, dependency);

                Component component = context.get(Component.class).get();
                assertNotNull(component);
                assertSame(dependency, ((ComponentWithInjectConstructor)component).getDependency());
            }

            // TODO: A -> B -> C
            @Test
            public void should_bind_to_a_type_with_transitive_dependencies() {
                context.bind(Component.class, ComponentWithInjectConstructor.class);
                context.bind(Dependency.class, DependencyWithInjectorConstructor.class);
                context.bind(String.class, "indirect dependency");

                Component instance = context.get(Component.class).get();
                assertNotNull(instance);

                Dependency dependency = ((ComponentWithInjectConstructor)instance).getDependency();
                assertNotNull(dependency);

                String dependencyStr = ((DependencyWithInjectorConstructor)dependency).getDependency();
                assertNotNull(dependencyStr);

                assertEquals(dependencyStr, "indirect dependency");

            }

            // TODO: multi inject constructors
            @Test
            public void should_throw_exception_if_multi_inject_constructors() {
                assertThrows(IllegalComponentException.class, () -> {
                    context.bind(Component.class, ComponentWithMultiInjectConstructor.class);
                });
            }

            // TODO: no default constructor and inject constructor
            @Test
            public void should_throw_exception_if_no_inject_nor_default_constructors() {
                assertThrows(IllegalComponentException.class, () -> {
                    context.bind(Component.class, ComponentWithInjectNorDefaultConstructor.class);
                });
            }

            // TODO: dependencies not exist
            @Test
            public void should_throw_exception_if_dependency_not_found() {
                context.bind(Component.class, ComponentWithInjectConstructor.class);
                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class,
                    () -> context.get(Component.class).get());

                assertEquals(Dependency.class, exception.getDependency());
                assertEquals(Component.class, exception.getComponent());
            }

            @Test
            public void should_throw_exception_if_transitive_dependency_not_found() {
                context.bind(Component.class, ComponentWithInjectConstructor.class);
                context.bind(Dependency.class, DependencyWithInjectorConstructor.class);
                // context.bind(String.class, "indirect dependency");
                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class,
                    () -> context.get(Component.class).get());

                assertEquals(String.class, exception.getDependency());
                assertEquals(Dependency.class, exception.getComponent());
            }

            @Test
            public void should_throw_exception_if_cyclic_dependencies_found() {
                context.bind(Component.class, ComponentWithInjectConstructor.class);
                context.bind(Dependency.class, DependencyDependentOnComponent.class);

                CyclicDependenciesFoundException exception = assertThrows(
                    CyclicDependenciesFoundException.class, () -> {
                        context.get(Component.class);
                    });

                Set<Class<?>> classes = exception.getComponents();
                assertEquals(2, classes.size());
                assertTrue(classes.contains(Component.class));
                assertTrue(classes.contains(Dependency.class));
            }

            // TODO : A->B->C->A
            @Test
            public void should_throw_exception_if_transitive_cyclic_dependencies() {
                context.bind(Component.class, ComponentWithInjectConstructor.class);
                context.bind(Dependency.class, DependencyB.class);
                context.bind(AnotherDependency.class, DependencyC.class);
                context.bind(Component.class, ComponentWithInjectConstructor.class);

                CyclicDependenciesFoundException exception = assertThrows(
                    CyclicDependenciesFoundException.class, () -> {
                        context.get(Component.class);
                    });

                Set<Class<?>> components = exception.getComponents();
                assertEquals(3, components.size());
                assertTrue(components.contains(Component.class));
                assertTrue(components.contains(Dependency.class));
                assertTrue(components.contains(AnotherDependency.class));
            }
        }

        @Nested
        public class FieldInjection {
        }

        @Nested
        public class MethodInjection {
        }

    }

    @Nested
    public class DependenciesSelection {
    }

    @Nested
    public class LifeCycleManagement {
    }

}

interface Component {

}

interface Dependency {

}

interface AnotherDependency {

}

class ComponentWithMultiInjectConstructor implements Component {
    @Inject
    public ComponentWithMultiInjectConstructor(String name, String value) {

    }

    @Inject
    public ComponentWithMultiInjectConstructor(String name) {

    }
}

class ComponentWithInjectNorDefaultConstructor implements Component {

    public ComponentWithInjectNorDefaultConstructor(String name, String value) {

    }
}

class ComponentWithDefaultConstructor implements Component {
    public ComponentWithDefaultConstructor() {
    }
}

class DependencyWithInjectorConstructor implements Dependency {
    private String dependency;

    @Inject
    public DependencyWithInjectorConstructor(String dependency) {
        this.dependency = dependency;
    }

    public String getDependency() {
        return dependency;
    }
}

class ComponentWithInjectConstructor implements Component {
    private Dependency dependency;

    @Inject
    public ComponentWithInjectConstructor(Dependency dependency) {
        this.dependency = dependency;
    }

    public Dependency getDependency() {
        return dependency;
    }
}

class DependencyDependentOnComponent implements Dependency {
    private Component component;

    @Inject
    public DependencyDependentOnComponent(Component component) {
        this.component = component;
    }
}

class DependencyB implements Dependency {
    private AnotherDependency dependencyC;

    @Inject

    public DependencyB(AnotherDependency dependencyC) {
        this.dependencyC = dependencyC;
    }
}

class DependencyC implements AnotherDependency {
    private Component componentA;

    @Inject
    public DependencyC(Component componentA) {
        this.componentA = componentA;
    }
}





