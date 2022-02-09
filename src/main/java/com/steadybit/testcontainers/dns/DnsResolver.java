package com.steadybit.testcontainers.dns;

import org.testcontainers.containers.Container;

import java.util.List;

public interface DnsResolver {

    List<String> resolve(List<String> addresses);

    static DnsResolver forContainer(Container<?> targetContainer) {
        return new TestcontainerDnsResolver(targetContainer);
    }

}
