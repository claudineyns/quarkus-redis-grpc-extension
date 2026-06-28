package io.github.claudineyns.redis.grpc.client.test;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.github.claudineyns.redis.grpc.client.RedisHashClient;
import io.github.claudineyns.redis.grpc.v1.HDelRequest;
import io.github.claudineyns.redis.grpc.v1.HExistsRequest;
import io.github.claudineyns.redis.grpc.v1.HGetAllRequest;
import io.github.claudineyns.redis.grpc.v1.HGetRequest;
import io.github.claudineyns.redis.grpc.v1.HIncrByRequest;
import io.github.claudineyns.redis.grpc.v1.HKeysRequest;
import io.github.claudineyns.redis.grpc.v1.HLenRequest;
import io.github.claudineyns.redis.grpc.v1.HMGetRequest;
import io.github.claudineyns.redis.grpc.v1.HScanRequest;
import io.github.claudineyns.redis.grpc.v1.HSetNxRequest;
import io.github.claudineyns.redis.grpc.v1.HSetRequest;
import io.github.claudineyns.redis.grpc.v1.HValsRequest;

/**
 * Cobertura oficial (mockada, sem servidor) da família Hash. Funcional via
 * HashLiveTest (efêmero).
 */
class RedisHashClientTest {

    private static final Duration D = Duration.ofSeconds(5);
    private final RedisHashClient client = new RedisHashClient(MockGrpc.invoker());

    @Test
    void allHashRpcsDelegate() {
        client.hset(HSetRequest.getDefaultInstance()).await().atMost(D);
        client.hget(HGetRequest.getDefaultInstance()).await().atMost(D);
        client.hdel(HDelRequest.getDefaultInstance()).await().atMost(D);
        client.hexists(HExistsRequest.getDefaultInstance()).await().atMost(D);
        client.hlen(HLenRequest.getDefaultInstance()).await().atMost(D);
        client.hmget(HMGetRequest.getDefaultInstance()).await().atMost(D);
        client.hgetall(HGetAllRequest.getDefaultInstance()).await().atMost(D);
        client.hkeys(HKeysRequest.getDefaultInstance()).await().atMost(D);
        client.hvals(HValsRequest.getDefaultInstance()).await().atMost(D);
        client.hsetnx(HSetNxRequest.getDefaultInstance()).await().atMost(D);
        client.hincrby(HIncrByRequest.getDefaultInstance()).await().atMost(D);
        client.hscan(HScanRequest.getDefaultInstance()).await().atMost(D);
    }
}
