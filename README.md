# Steadybit Tesctontainers

## What is it?

Steadybit Testcontainers is a helper library to the [Testcontainers Project](https://testcontainers.org) to implement Resilience Tests. 

## Getting Started

> We have a [BlogPost discussing Resilience Tests with Testcontainers](https://www.steadybit.com/blog/resilience-testing-using-testcontainers/) which gives a more detailed explanation.

### 1. Add Steadybit Testcontainers to your project:
Add this to the test dependencies in your `pom.xml`:
```xml
<dependency>  
  <groupId>com.steadybit</groupId>
  <artifactId>steadybit-testcontainers</artifactId>
  <version>1.0.0</version>
  <scope>test</scope>
</dependency>
```

### 2. Add some Chaos to your Testcontainers Test:

Here is an example for delaying the Redis traffic:
```java
@Testcontainers
public class RedisBackedCacheIntTest {

    private RedisBackedCache underTest;

    @Container
    public GenericContainer redis = new GenericContainer(DockerImageName.parse("redis:5.0.3-alpine")).withExposedPorts(6379);

    @BeforeEach
    public void setUp() {
        underTest = new RedisBackedCache(redis.getHost(), redis.getFirstMappedPort());
    }

    @Test
    public void testFindingAnInsertedValue() {
        cache.put("foo", "FOO");

        Optional<String> foundObject = Steadybit.networkDelayPackages(Duration.ofSeconds(2))
                .forContainers(redis)
                .exec(() -> {
                    //this code runs after the attack was started as soon as this codes completes the attack will be stopped.
                    return cache.get("foo", String.class);
                });

        assertTrue("When an object in the cache is retrieved, it can be found", foundObject.isPresent());
        assertEquals("When we put a String in to the cache and retrieve it, the value is the same", "FOO", foundObject.get());
    }
}
```

## Available Attacks

- `networkDelayPackages`: Delays egress tcp/udp network packages for containers (on eth0 by default)
- `networkLoosePackages`: Looses egress tcp/udp network packages for containers (on eth0 by default)
- `networkCorruptPackages`: Corrupts egress tcp/udp network packages for containers (on eth0 by default)
- `networkLimitBandwidth`: Limits tcp/udp network bandwidth for containers (on eth0 by default)
- `networkBlackhole`: Blocks all network traffic for containers
- `networkBlockDns`: Blocks all network traffic for containers on dns port (53)