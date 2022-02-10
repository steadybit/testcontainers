package com.steadybit.testcontainers.dns;

import com.steadybit.testcontainers.measure.EchoTcpContainer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Testcontainers
public class TestcontainersDnsResolverTest {
    @Container
    static GenericContainer<?> target = new EchoTcpContainer()
            .withExtraHost("some-extra-host", "192.168.2.1")
            .withExtraHost("some-extra-host", "192.168.2.2");

    @Test
    void should_resolve() {
        List<String> resolved = new TestcontainersDnsResolver(target).resolve(Arrays.asList("127.0.0.1", "heise.de", "some-extra-host"));
        assertThat(resolved).containsExactlyInAnyOrder("127.0.0.1", "193.99.144.80", "192.168.2.1", "192.168.2.2");
    }

    @Test
    void should_throw_when_not_resolved() {
        assertThatThrownBy(() -> new TestcontainersDnsResolver(target).resolve(Collections.singletonList("doesnt-exist")))
                .hasMessageContaining("doesnt-exist");
    }
}