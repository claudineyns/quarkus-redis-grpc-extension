package io.github.claudineyns.redis.grpc.client;

import io.github.claudineyns.redis.grpc.client.runtime.GrpcInvoker;
import io.github.claudineyns.redis.grpc.client.runtime.ServiceMethods;
import io.github.claudineyns.redis.grpc.v1.AppendRequest;
import io.github.claudineyns.redis.grpc.v1.CounterValue;
import io.github.claudineyns.redis.grpc.v1.DecrByRequest;
import io.github.claudineyns.redis.grpc.v1.DecrRequest;
import io.github.claudineyns.redis.grpc.v1.GetDelRequest;
import io.github.claudineyns.redis.grpc.v1.GetExRequest;
import io.github.claudineyns.redis.grpc.v1.GetRequest;
import io.github.claudineyns.redis.grpc.v1.GetResponse;
import io.github.claudineyns.redis.grpc.v1.IncrByRequest;
import io.github.claudineyns.redis.grpc.v1.IncrRequest;
import io.github.claudineyns.redis.grpc.v1.LengthValue;
import io.github.claudineyns.redis.grpc.v1.MGetRequest;
import io.github.claudineyns.redis.grpc.v1.MGetResponse;
import io.github.claudineyns.redis.grpc.v1.MSetRequest;
import io.github.claudineyns.redis.grpc.v1.MSetResponse;
import io.github.claudineyns.redis.grpc.v1.SetRequest;
import io.github.claudineyns.redis.grpc.v1.SetResponse;
import io.github.claudineyns.redis.grpc.v1.StrlenRequest;
import io.smallrye.mutiny.Uni;
import io.vertx.grpc.common.ServiceMethod;
import io.vertx.grpc.common.ServiceName;

/**
 * Cliente da família KEY/VALUE — {@code StringService} (DESIGN §7, superfície
 * Nível 1: tipado por mensagem, retorna {@link Uni}, fiel 1:1 ao contrato).
 *
 * <p>Sem stubs: cada RPC é um {@link ServiceMethod} (serviço + método +
 * marshallers protobuf) que o {@link GrpcInvoker} dirige sobre o GrpcClient Vert.x.
 */
public final class RedisStringClient {

    private static final ServiceName SERVICE =
            ServiceName.create("io.github.claudineyns.redis.grpc.v1", "StringService");

    private final GrpcInvoker invoker;

    private final ServiceMethod<GetResponse, GetRequest> get = ServiceMethods.unary(SERVICE, "Get", GetResponse.parser());
    private final ServiceMethod<GetResponse, GetExRequest> getEx = ServiceMethods.unary(SERVICE, "GetEx", GetResponse.parser());
    private final ServiceMethod<GetResponse, GetDelRequest> getDel = ServiceMethods.unary(SERVICE, "GetDel", GetResponse.parser());
    private final ServiceMethod<SetResponse, SetRequest> set = ServiceMethods.unary(SERVICE, "Set", SetResponse.parser());
    private final ServiceMethod<MSetResponse, MSetRequest> mset = ServiceMethods.unary(SERVICE, "MSet", MSetResponse.parser());
    private final ServiceMethod<MGetResponse, MGetRequest> mget = ServiceMethods.unary(SERVICE, "MGet", MGetResponse.parser());
    private final ServiceMethod<CounterValue, IncrRequest> incr = ServiceMethods.unary(SERVICE, "Incr", CounterValue.parser());
    private final ServiceMethod<CounterValue, IncrByRequest> incrBy = ServiceMethods.unary(SERVICE, "IncrBy", CounterValue.parser());
    private final ServiceMethod<CounterValue, DecrRequest> decr = ServiceMethods.unary(SERVICE, "Decr", CounterValue.parser());
    private final ServiceMethod<CounterValue, DecrByRequest> decrBy = ServiceMethods.unary(SERVICE, "DecrBy", CounterValue.parser());
    private final ServiceMethod<LengthValue, AppendRequest> append = ServiceMethods.unary(SERVICE, "Append", LengthValue.parser());
    private final ServiceMethod<LengthValue, StrlenRequest> strlen = ServiceMethods.unary(SERVICE, "Strlen", LengthValue.parser());

    public RedisStringClient(final GrpcInvoker invoker) {
        this.invoker = invoker;
    }

    public Uni<GetResponse> get(final GetRequest request) {
        return invoker.call(get, request);
    }

    public Uni<GetResponse> getEx(final GetExRequest request) {
        return invoker.call(getEx, request);
    }

    public Uni<GetResponse> getDel(final GetDelRequest request) {
        return invoker.call(getDel, request);
    }

    public Uni<SetResponse> set(final SetRequest request) {
        return invoker.call(set, request);
    }

    public Uni<MSetResponse> mset(final MSetRequest request) {
        return invoker.call(mset, request);
    }

    public Uni<MGetResponse> mget(final MGetRequest request) {
        return invoker.call(mget, request);
    }

    public Uni<CounterValue> incr(final IncrRequest request) {
        return invoker.call(incr, request);
    }

    public Uni<CounterValue> incrBy(final IncrByRequest request) {
        return invoker.call(incrBy, request);
    }

    public Uni<CounterValue> decr(final DecrRequest request) {
        return invoker.call(decr, request);
    }

    public Uni<CounterValue> decrBy(final DecrByRequest request) {
        return invoker.call(decrBy, request);
    }

    public Uni<LengthValue> append(final AppendRequest request) {
        return invoker.call(append, request);
    }

    public Uni<LengthValue> strlen(final StrlenRequest request) {
        return invoker.call(strlen, request);
    }
}
