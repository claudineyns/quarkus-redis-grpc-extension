package io.github.claudineyns.redis.grpc.client.test;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.github.claudineyns.redis.grpc.client.RedisKeyClient;
import io.github.claudineyns.redis.grpc.v1.DelRequest;
import io.github.claudineyns.redis.grpc.v1.ExistsRequest;
import io.github.claudineyns.redis.grpc.v1.ExpireAtRequest;
import io.github.claudineyns.redis.grpc.v1.ExpireRequest;
import io.github.claudineyns.redis.grpc.v1.PExpireAtRequest;
import io.github.claudineyns.redis.grpc.v1.PExpireRequest;
import io.github.claudineyns.redis.grpc.v1.PTtlRequest;
import io.github.claudineyns.redis.grpc.v1.PersistRequest;
import io.github.claudineyns.redis.grpc.v1.ScanRequest;
import io.github.claudineyns.redis.grpc.v1.TtlRequest;
import io.github.claudineyns.redis.grpc.v1.TypeRequest;
import io.github.claudineyns.redis.grpc.v1.UnlinkRequest;

/**
 * Cobertura oficial (mockada, sem servidor) da família Key. Funcional via
 * KeyLiveTest (efêmero).
 */
class RedisKeyClientTest {

    private static final Duration D = Duration.ofSeconds(5);
    private final RedisKeyClient client = new RedisKeyClient(MockGrpc.invoker());

    @Test
    void allKeyRpcsDelegate() {
        client.del(DelRequest.getDefaultInstance()).await().atMost(D);
        client.unlink(UnlinkRequest.getDefaultInstance()).await().atMost(D);
        client.exists(ExistsRequest.getDefaultInstance()).await().atMost(D);
        client.type(TypeRequest.getDefaultInstance()).await().atMost(D);
        client.expire(ExpireRequest.getDefaultInstance()).await().atMost(D);
        client.pexpire(PExpireRequest.getDefaultInstance()).await().atMost(D);
        client.expireAt(ExpireAtRequest.getDefaultInstance()).await().atMost(D);
        client.pexpireAt(PExpireAtRequest.getDefaultInstance()).await().atMost(D);
        client.persist(PersistRequest.getDefaultInstance()).await().atMost(D);
        client.ttl(TtlRequest.getDefaultInstance()).await().atMost(D);
        client.pttl(PTtlRequest.getDefaultInstance()).await().atMost(D);
        client.scan(ScanRequest.getDefaultInstance()).await().atMost(D);
    }
}
