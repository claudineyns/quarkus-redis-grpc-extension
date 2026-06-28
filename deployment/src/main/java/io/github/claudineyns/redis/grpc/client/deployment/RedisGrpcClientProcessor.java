package io.github.claudineyns.redis.grpc.client.deployment;

import io.github.claudineyns.redis.grpc.client.runtime.RedisGrpcClientProducer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

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
}
