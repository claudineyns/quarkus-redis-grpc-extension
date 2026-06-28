package io.github.claudineyns.redis.grpc.client.deployment;

import java.util.Optional;

import io.github.claudineyns.redis.grpc.client.runtime.MicrometerRedisGrpcMetrics;
import io.github.claudineyns.redis.grpc.client.runtime.RedisGrpcClientProducer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.runtime.metrics.MetricsFactory;

class RedisGrpcClientProcessor {

    private static final String FEATURE = "redis-grpc-client";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    /**
     * Registra o producer do cliente como bean, garantindo que o consumidor sempre
     * o descubra (DESIGN §5). Unremovable: o {@link RedisGrpcClientProducer} fica
     * mesmo que o Arc não veja injeção direta no momento da análise.
     */
    @BuildStep
    AdditionalBeanBuildItem registerClient() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(RedisGrpcClientProducer.class)
                .setUnremovable()
                .build();
    }

    /**
     * Métricas (2g) <strong>opcionais</strong>: só registra a impl Micrometer quando
     * o consumidor tem o sistema de métricas <strong>Micrometer</strong> ativo —
     * sinalizado pelo {@link MetricsCapabilityBuildItem} (produzido pelo
     * quarkus-micrometer). Isso é preciso (distingue de smallrye-metrics) e garante
     * que o bean {@code MeterRegistry} existe. Sem isso, a impl nunca vira bean (nem
     * é carregada) e o producer cai no NOOP.
     */
    @BuildStep
    void registerMetrics(final Optional<MetricsCapabilityBuildItem> metricsCapability,
            final BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        if (metricsCapability.isPresent()
                && metricsCapability.get().metricsSupported(MetricsFactory.MICROMETER)) {
            additionalBeans.produce(AdditionalBeanBuildItem.builder()
                    .addBeanClass(MicrometerRedisGrpcMetrics.class)
                    .setUnremovable()
                    .build());
        }
    }
}
