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

    public RedisGrpcClient(final GrpcInvoker invoker) {
        this.string = new RedisStringClient(invoker);
    }

    /** Família KEY/VALUE ({@code StringService}). */
    public RedisStringClient string() {
        return string;
    }
}
