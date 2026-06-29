package io.github.claudineyns.redis.grpc.client.runtime;

import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Inject;

/**
 * Implementação Micrometer de {@link RedisGrpcMetrics} (2g).
 *
 * <p><strong>Só é registrada como bean quando o consumidor tem Micrometer</strong> —
 * exclusivamente via o build step gated (que define o escopo no
 * {@code AdditionalBeanBuildItem}). <strong>Sem anotação de escopo de propósito:</strong>
 * o runtime jar é indexado (2e) e, portanto, um bean archive; uma anotação aqui faria
 * o Arc auto-descobrir a classe e exigir um {@code MeterRegistry} mesmo sem Micrometer.
 * Mantendo-a sem escopo, o gate é a única porta de entrada.
 *
 * <p>Emite um {@code Timer "redis.grpc.client.call"} (contagem + latência) com tags
 * de <strong>baixa cardinalidade</strong>: {@code service}/{@code method}/{@code status}.
 * A chave Redis nunca vira tag. O registry faz cache dos meters por nome+tags.
 */
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
