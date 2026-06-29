package io.github.claudineyns.redis.grpc.client.runtime;

/**
 * Abstração neutra de métricas do cliente (2g) — <strong>sem tipos Micrometer</strong>.
 *
 * <p>Essa indireção é o que torna as métricas <em>opcionais</em>: o {@link GrpcInvoker}
 * depende só desta interface, então o caminho de quem não tem Micrometer nunca
 * carrega classes do Micrometer. Quando o consumidor tem Micrometer, um build step
 * (gated por {@code Capability.MICROMETER}) registra a implementação
 * {@code MicrometerRedisGrpcMetrics}; caso contrário usa-se {@link #NOOP}.
 */
public interface RedisGrpcMetrics {

    /**
     * Registra uma chamada concluída.
     *
     * @param service       nome do serviço (tag, ex.: {@code StringService})
     * @param method        nome do método (tag, ex.: {@code Get})
     * @param status        status gRPC do desfecho (tag, ex.: {@code OK})
     * @param durationNanos duração da chamada em nanos
     */
    void record(String service, String method, String status, long durationNanos);

    /** Implementação no-op (métricas desligadas ou Micrometer ausente). */
    RedisGrpcMetrics NOOP = (service, method, status, durationNanos) -> { };
}
