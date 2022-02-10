/*
 * Copyright 2021 steadybit GmbH. All rights reserved.
 */

package com.steadybit.testcontainers.measure;

import org.testcontainers.containers.GenericContainer;

public class TomcatContainer extends GenericContainer<TomcatContainer> {

    public TomcatContainer() {
        super("tomcat:latest");
    }

    @Override
    protected void configure() {
        super.configure();
        this.withPrivilegedMode(true);
    }
}
