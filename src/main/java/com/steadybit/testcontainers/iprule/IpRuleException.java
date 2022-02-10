/*
 * Copyright 2021 steadybit GmbH. All rights reserved.
 */

package com.steadybit.testcontainers.iprule;

public class IpRuleException extends RuntimeException {

    public IpRuleException(String message) {
        super(message);
    }

    public IpRuleException(String message, Throwable cause) {
        super(message, cause);
    }
}