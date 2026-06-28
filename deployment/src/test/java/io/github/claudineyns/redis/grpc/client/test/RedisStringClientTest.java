package io.github.claudineyns.redis.grpc.client.test;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.github.claudineyns.redis.grpc.client.RedisStringClient;
import io.github.claudineyns.redis.grpc.v1.AppendRequest;
import io.github.claudineyns.redis.grpc.v1.DecrByRequest;
import io.github.claudineyns.redis.grpc.v1.DecrRequest;
import io.github.claudineyns.redis.grpc.v1.GetDelRequest;
import io.github.claudineyns.redis.grpc.v1.GetExRequest;
import io.github.claudineyns.redis.grpc.v1.GetRequest;
import io.github.claudineyns.redis.grpc.v1.IncrByRequest;
import io.github.claudineyns.redis.grpc.v1.IncrRequest;
import io.github.claudineyns.redis.grpc.v1.MGetRequest;
import io.github.claudineyns.redis.grpc.v1.MSetRequest;
import io.github.claudineyns.redis.grpc.v1.SetRequest;
import io.github.claudineyns.redis.grpc.v1.StrlenRequest;

/**
 * Cobertura oficial (mockada, sem servidor) da família String: cada RPC delega ao
 * invoker pelo seu descritor sem erro. A correção funcional é garantida pelo
 * StringLiveTest (efêmero).
 */
class RedisStringClientTest {

    private static final Duration D = Duration.ofSeconds(5);
    private final RedisStringClient client = new RedisStringClient(MockGrpc.invoker());

    @Test
    void allStringRpcsDelegate() {
        client.get(GetRequest.getDefaultInstance()).await().atMost(D);
        client.getEx(GetExRequest.getDefaultInstance()).await().atMost(D);
        client.getDel(GetDelRequest.getDefaultInstance()).await().atMost(D);
        client.set(SetRequest.getDefaultInstance()).await().atMost(D);
        client.mset(MSetRequest.getDefaultInstance()).await().atMost(D);
        client.mget(MGetRequest.getDefaultInstance()).await().atMost(D);
        client.incr(IncrRequest.getDefaultInstance()).await().atMost(D);
        client.incrBy(IncrByRequest.getDefaultInstance()).await().atMost(D);
        client.decr(DecrRequest.getDefaultInstance()).await().atMost(D);
        client.decrBy(DecrByRequest.getDefaultInstance()).await().atMost(D);
        client.append(AppendRequest.getDefaultInstance()).await().atMost(D);
        client.strlen(StrlenRequest.getDefaultInstance()).await().atMost(D);
    }
}
