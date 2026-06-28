package io.github.claudineyns.redis.grpc.client;

import io.github.claudineyns.redis.grpc.client.runtime.GrpcInvoker;

/**
 * Ponto de entrada público da extensão (DESIGN §7): um agregador injetável que
 * expõe as famílias de comandos do gateway redis-grpc via acessores. Encapsula
 * todo o baixo nível (canal Vert.x, descritores, marshalling, headers).
 *
 * <pre>{@code
 * @Inject RedisGrpcClient redis;
 * Uni<GetResponse> r = redis.string().get(GetRequest.newBuilder().setKey("k").build());
 * }</pre>
 *
 * <p>Famílias adicionadas gradualmente: String (KEY/VALUE) primeiro; Hash, Set e
 * Key a seguir.
 */
public final class RedisGrpcClient {

    private final RedisStringClient string;
    private final RedisHashClient hash;
    private final RedisSetClient set;
    private final RedisKeyClient key;

    public RedisGrpcClient(final GrpcInvoker invoker) {
        this.string = new RedisStringClient(invoker);
        this.hash = new RedisHashClient(invoker);
        this.set = new RedisSetClient(invoker);
        this.key = new RedisKeyClient(invoker);
    }

    /** Família KEY/VALUE ({@code StringService}). */
    public RedisStringClient string() {
        return string;
    }

    /** Família KEY/HASH ({@code HashService}). */
    public RedisHashClient hash() {
        return hash;
    }

    /** Família SET ({@code SetService}). */
    public RedisSetClient set() {
        return set;
    }

    /** Família KEY genérica ({@code KeyService}). */
    public RedisKeyClient key() {
        return key;
    }
}
