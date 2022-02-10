package com.steadybit.testcontainers;

import org.testcontainers.containers.Container;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public interface ContainerAttack extends AutoCloseable {
    default void exec(Runnable run) {
        try (ContainerAttack self = this) {
            this.start();
            run.run();
        }
    }

    void stop();

    void start();

    @Override
    default void close() {
        this.stop();
    }

    abstract class Builder<T extends ContainerAttack> {
        protected List<Container<?>> containers;

        public T forContainers(Container<?>... containers) {
            this.containers = new ArrayList<>(Arrays.asList(containers));
            return this.build();
        }

        protected abstract T build();
    }
}