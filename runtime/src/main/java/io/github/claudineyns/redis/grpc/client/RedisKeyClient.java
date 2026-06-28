package io.github.claudineyns.redis.grpc.client;

import io.github.claudineyns.redis.grpc.client.runtime.GrpcInvoker;
import io.github.claudineyns.redis.grpc.client.runtime.ServiceMethods;
import io.github.claudineyns.redis.grpc.v1.DelRequest;
import io.github.claudineyns.redis.grpc.v1.ExistsRequest;
import io.github.claudineyns.redis.grpc.v1.ExpireAtRequest;
import io.github.claudineyns.redis.grpc.v1.ExpireRequest;
import io.github.claudineyns.redis.grpc.v1.KeyChange;
import io.github.claudineyns.redis.grpc.v1.KeyCount;
import io.github.claudineyns.redis.grpc.v1.KeyType;
import io.github.claudineyns.redis.grpc.v1.PExpireAtRequest;
import io.github.claudineyns.redis.grpc.v1.PExpireRequest;
import io.github.claudineyns.redis.grpc.v1.PTtlRequest;
import io.github.claudineyns.redis.grpc.v1.PersistRequest;
import io.github.claudineyns.redis.grpc.v1.ScanRequest;
import io.github.claudineyns.redis.grpc.v1.ScanResponse;
import io.github.claudineyns.redis.grpc.v1.TtlRequest;
import io.github.claudineyns.redis.grpc.v1.TtlValue;
import io.github.claudineyns.redis.grpc.v1.TypeRequest;
import io.github.claudineyns.redis.grpc.v1.UnlinkRequest;
import io.smallrye.mutiny.Uni;
import io.vertx.grpc.common.ServiceMethod;
import io.vertx.grpc.common.ServiceName;

/**
 * Cliente da família KEY (genérica) — {@code KeyService} (DESIGN §7, Nível 1).
 */
public final class RedisKeyClient {

    private static final ServiceName SERVICE =
            ServiceName.create("io.github.claudineyns.redis.grpc.v1", "KeyService");

    private final GrpcInvoker invoker;

    private final ServiceMethod<KeyCount, DelRequest> del = ServiceMethods.unary(SERVICE, "Del", KeyCount.parser());
    private final ServiceMethod<KeyCount, UnlinkRequest> unlink = ServiceMethods.unary(SERVICE, "Unlink", KeyCount.parser());
    private final ServiceMethod<KeyCount, ExistsRequest> exists = ServiceMethods.unary(SERVICE, "Exists", KeyCount.parser());
    private final ServiceMethod<KeyType, TypeRequest> type = ServiceMethods.unary(SERVICE, "Type", KeyType.parser());
    private final ServiceMethod<KeyChange, ExpireRequest> expire = ServiceMethods.unary(SERVICE, "Expire", KeyChange.parser());
    private final ServiceMethod<KeyChange, PExpireRequest> pexpire = ServiceMethods.unary(SERVICE, "PExpire", KeyChange.parser());
    private final ServiceMethod<KeyChange, ExpireAtRequest> expireAt = ServiceMethods.unary(SERVICE, "ExpireAt", KeyChange.parser());
    private final ServiceMethod<KeyChange, PExpireAtRequest> pexpireAt = ServiceMethods.unary(SERVICE, "PExpireAt", KeyChange.parser());
    private final ServiceMethod<KeyChange, PersistRequest> persist = ServiceMethods.unary(SERVICE, "Persist", KeyChange.parser());
    private final ServiceMethod<TtlValue, TtlRequest> ttl = ServiceMethods.unary(SERVICE, "Ttl", TtlValue.parser());
    private final ServiceMethod<TtlValue, PTtlRequest> pttl = ServiceMethods.unary(SERVICE, "PTtl", TtlValue.parser());
    private final ServiceMethod<ScanResponse, ScanRequest> scan = ServiceMethods.unary(SERVICE, "Scan", ScanResponse.parser());

    public RedisKeyClient(final GrpcInvoker invoker) {
        this.invoker = invoker;
    }

    public Uni<KeyCount> del(final DelRequest request) {
        return invoker.call(del, request);
    }

    public Uni<KeyCount> unlink(final UnlinkRequest request) {
        return invoker.call(unlink, request);
    }

    public Uni<KeyCount> exists(final ExistsRequest request) {
        return invoker.call(exists, request);
    }

    public Uni<KeyType> type(final TypeRequest request) {
        return invoker.call(type, request);
    }

    public Uni<KeyChange> expire(final ExpireRequest request) {
        return invoker.call(expire, request);
    }

    public Uni<KeyChange> pexpire(final PExpireRequest request) {
        return invoker.call(pexpire, request);
    }

    public Uni<KeyChange> expireAt(final ExpireAtRequest request) {
        return invoker.call(expireAt, request);
    }

    public Uni<KeyChange> pexpireAt(final PExpireAtRequest request) {
        return invoker.call(pexpireAt, request);
    }

    public Uni<KeyChange> persist(final PersistRequest request) {
        return invoker.call(persist, request);
    }

    public Uni<TtlValue> ttl(final TtlRequest request) {
        return invoker.call(ttl, request);
    }

    public Uni<TtlValue> pttl(final PTtlRequest request) {
        return invoker.call(pttl, request);
    }

    public Uni<ScanResponse> scan(final ScanRequest request) {
        return invoker.call(scan, request);
    }
}
