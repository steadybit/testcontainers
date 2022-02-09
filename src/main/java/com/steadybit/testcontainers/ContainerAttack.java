package com.steadybit.testcontainers;

import org.testcontainers.containers.Container;

import java.util.ArrayList;
import java.util.Arrays;

public abstract class ContainerAttack<SELF extends ContainerAttack<SELF>> {
    protected ArrayList<Container<?>> containers;

    public SELF forContainers(Container<?>... containers) {
        this.containers = new ArrayList<>(Arrays.asList(containers));
        return (SELF) this;
    }

    public void exec(Runnable run) {
        start();
        try {
            run.run();
        }finally {
            stop();
        }
    }

    public abstract void stop();

    public abstract void start();
}
