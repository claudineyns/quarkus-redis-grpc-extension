package io.github.claudineyns.redis.grpc.client.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class RedisGrpcClientProcessor {

    private static final String FEATURE = "redis-grpc-client";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }
}
