package io.github.claudineyns.redis.grpc.client;

import io.github.claudineyns.redis.grpc.client.runtime.GrpcInvoker;
import io.github.claudineyns.redis.grpc.client.runtime.ServiceMethods;
import io.github.claudineyns.redis.grpc.v1.SAddRequest;
import io.github.claudineyns.redis.grpc.v1.SCardRequest;
import io.github.claudineyns.redis.grpc.v1.SIsMemberRequest;
import io.github.claudineyns.redis.grpc.v1.SMIsMemberRequest;
import io.github.claudineyns.redis.grpc.v1.SMembersRequest;
import io.github.claudineyns.redis.grpc.v1.SPopRequest;
import io.github.claudineyns.redis.grpc.v1.SRemRequest;
import io.github.claudineyns.redis.grpc.v1.SScanRequest;
import io.github.claudineyns.redis.grpc.v1.SScanResponse;
import io.github.claudineyns.redis.grpc.v1.SetCount;
import io.github.claudineyns.redis.grpc.v1.SetMembers;
import io.github.claudineyns.redis.grpc.v1.SetMembership;
import io.github.claudineyns.redis.grpc.v1.SetMemberships;
import io.smallrye.mutiny.Uni;
import io.vertx.grpc.common.ServiceMethod;
import io.vertx.grpc.common.ServiceName;

/**
 * Cliente da família SET — {@code SetService} (DESIGN §7, Nível 1).
 */
public final class RedisSetClient {

    private static final ServiceName SERVICE =
            ServiceName.create("io.github.claudineyns.redis.grpc.v1", "SetService");

    private final GrpcInvoker invoker;

    private final ServiceMethod<SetCount, SAddRequest> sadd = ServiceMethods.unary(SERVICE, "SAdd", SetCount.parser());
    private final ServiceMethod<SetCount, SRemRequest> srem = ServiceMethods.unary(SERVICE, "SRem", SetCount.parser());
    private final ServiceMethod<SetCount, SCardRequest> scard = ServiceMethods.unary(SERVICE, "SCard", SetCount.parser());
    private final ServiceMethod<SetMembership, SIsMemberRequest> sismember = ServiceMethods.unary(SERVICE, "SIsMember", SetMembership.parser());
    private final ServiceMethod<SetMemberships, SMIsMemberRequest> smismember = ServiceMethods.unary(SERVICE, "SMIsMember", SetMemberships.parser());
    private final ServiceMethod<SetMembers, SMembersRequest> smembers = ServiceMethods.unary(SERVICE, "SMembers", SetMembers.parser());
    private final ServiceMethod<SetMembers, SPopRequest> spop = ServiceMethods.unary(SERVICE, "SPop", SetMembers.parser());
    private final ServiceMethod<SScanResponse, SScanRequest> sscan = ServiceMethods.unary(SERVICE, "SScan", SScanResponse.parser());

    public RedisSetClient(final GrpcInvoker invoker) {
        this.invoker = invoker;
    }

    public Uni<SetCount> sadd(final SAddRequest request) {
        return invoker.call(sadd, request);
    }

    public Uni<SetCount> srem(final SRemRequest request) {
        return invoker.call(srem, request);
    }

    public Uni<SetCount> scard(final SCardRequest request) {
        return invoker.call(scard, request);
    }

    public Uni<SetMembership> sismember(final SIsMemberRequest request) {
        return invoker.call(sismember, request);
    }

    public Uni<SetMemberships> smismember(final SMIsMemberRequest request) {
        return invoker.call(smismember, request);
    }

    public Uni<SetMembers> smembers(final SMembersRequest request) {
        return invoker.call(smembers, request);
    }

    public Uni<SetMembers> spop(final SPopRequest request) {
        return invoker.call(spop, request);
    }

    public Uni<SScanResponse> sscan(final SScanRequest request) {
        return invoker.call(sscan, request);
    }
}
