/*
 * Copyright 2021 steadybit GmbH. All rights reserved.
 */

package com.steadybit.testcontainers.trafficcontrol;

public class TrafficControlException extends RuntimeException {

    public TrafficControlException(String message) {
        super(message);
    }

    public TrafficControlException(String message, Throwable cause) {
        super(message, cause);
    }
}