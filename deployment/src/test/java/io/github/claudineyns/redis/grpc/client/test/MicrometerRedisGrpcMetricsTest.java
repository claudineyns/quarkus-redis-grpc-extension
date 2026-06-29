package io.github.claudineyns.redis.grpc.client.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.github.claudineyns.redis.grpc.client.runtime.MicrometerRedisGrpcMetrics;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Cobertura oficial (sem servidor) do registro Micrometer (2g): {@code record}
 * cria/atualiza um {@code Timer "redis.grpc.client.call"} com as tags corretas.
 */
class MicrometerRedisGrpcMetricsTest {

    @Test
    void recordsTimerWithBoundedTags() {
        final SimpleMeterRegistry registry = new SimpleMeterRegistry();
        final MicrometerRedisGrpcMetrics metrics = new MicrometerRedisGrpcMetrics(registry);

        metrics.record("StringService", "Get", "OK", TimeUnit.MILLISECONDS.toNanos(5));
        metrics.record("StringService", "Get", "OK", TimeUnit.MILLISECONDS.toNanos(7));
        metrics.record("SetService", "SAdd", "FAILED_PRECONDITION", TimeUnit.MILLISECONDS.toNanos(3));

        final Timer ok = registry.find("redis.grpc.client.call")
                .tag("service", "StringService").tag("method", "Get").tag("status", "OK").timer();
        assertNotNull(ok, "OK timer should exist");
        assertEquals(2L, ok.count());

        final Timer err = registry.find("redis.grpc.client.call")
                .tag("service", "SetService").tag("method", "SAdd").tag("status", "FAILED_PRECONDITION").timer();
        assertNotNull(err, "error timer should exist");
        assertEquals(1L, err.count());

        // a chave Redis NUNCA é tag (cardinalidade)
        assertNull(registry.find("redis.grpc.client.call").tag("key", "anything").timer());
    }
}
