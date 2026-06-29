package io.github.claudineyns.redis.grpc.client.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.claudineyns.redis.grpc.client.RedisGrpcClient;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;

/**
 * Wiring (2c): com {@code host} configurado, o producer monta o GrpcClient
 * (plaintext, sem conectar) e o {@link RedisGrpcClient} é injetável. A validação
 * funcional (chamadas reais) vem no módulo integration-tests (2f).
 */
public class RedisGrpcClientTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .overrideConfigKey("quarkus.redis-grpc-client.host", "localhost");

    @Inject
    RedisGrpcClient redis;

    @Test
    public void aggregatorAndStringFamilyAreInjectable() {
        assertNotNull(redis, "RedisGrpcClient should be injectable");
        assertNotNull(redis.string(), "string() family should be available");
    }
}
