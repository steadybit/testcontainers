package com.steadybit.testcontainers;

import org.testcontainers.containers.Container;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public interface ContainerAttack extends AutoCloseable {

    default <T> T exec(Supplier<T> run) {
        try (ContainerAttack self = this) {
            this.start();
            return run.get();
        }
    }

    default void exec(Runnable run) {
        this.exec(() -> {
            run.run();
            return null;
        });
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