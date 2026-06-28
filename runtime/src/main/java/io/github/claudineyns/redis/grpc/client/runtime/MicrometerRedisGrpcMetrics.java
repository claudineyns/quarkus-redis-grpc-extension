package io.github.claudineyns.redis.grpc.client.runtime;

import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Implementação Micrometer de {@link RedisGrpcMetrics} (2g).
 *
 * <p><strong>Só é registrada como bean quando o consumidor tem Micrometer</strong>
 * (build step gated por {@code Capability.MICROMETER}); portanto esta classe — a
 * única que referencia tipos Micrometer — nunca é carregada sem Micrometer presente.
 *
 * <p>Emite um {@code Timer "redis.grpc.client.call"} (contagem + latência) com tags
 * de <strong>baixa cardinalidade</strong>: {@code service}/{@code method}/{@code status}.
 * A chave Redis nunca vira tag. O registry faz cache dos meters por nome+tags.
 */
@ApplicationScoped
public class MicrometerRedisGrpcMetrics implements RedisGrpcMetrics {

    static final String TIMER_NAME = "redis.grpc.client.call";

    private final MeterRegistry registry;

    @Inject
    public MicrometerRedisGrpcMetrics(final MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void record(final String service, final String method, final String status,
            final long durationNanos) {
        registry.timer(TIMER_NAME, "service", service, "method", method, "status", status)
                .record(durationNanos, TimeUnit.NANOSECONDS);
    }
}
