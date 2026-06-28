package io.github.claudineyns.redis.grpc.client.test;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.github.claudineyns.redis.grpc.client.RedisSetClient;
import io.github.claudineyns.redis.grpc.v1.SAddRequest;
import io.github.claudineyns.redis.grpc.v1.SCardRequest;
import io.github.claudineyns.redis.grpc.v1.SIsMemberRequest;
import io.github.claudineyns.redis.grpc.v1.SMIsMemberRequest;
import io.github.claudineyns.redis.grpc.v1.SMembersRequest;
import io.github.claudineyns.redis.grpc.v1.SPopRequest;
import io.github.claudineyns.redis.grpc.v1.SRemRequest;
import io.github.claudineyns.redis.grpc.v1.SScanRequest;

/**
 * Cobertura oficial (mockada, sem servidor) da família Set. Funcional via
 * SetLiveTest (efêmero).
 */
class RedisSetClientTest {

    private static final Duration D = Duration.ofSeconds(5);
    private final RedisSetClient client = new RedisSetClient(MockGrpc.invoker());

    @Test
    void allSetRpcsDelegate() {
        client.sadd(SAddRequest.getDefaultInstance()).await().atMost(D);
        client.srem(SRemRequest.getDefaultInstance()).await().atMost(D);
        client.scard(SCardRequest.getDefaultInstance()).await().atMost(D);
        client.sismember(SIsMemberRequest.getDefaultInstance()).await().atMost(D);
        client.smismember(SMIsMemberRequest.getDefaultInstance()).await().atMost(D);
        client.smembers(SMembersRequest.getDefaultInstance()).await().atMost(D);
        client.spop(SPopRequest.getDefaultInstance()).await().atMost(D);
        client.sscan(SScanRequest.getDefaultInstance()).await().atMost(D);
    }
}
