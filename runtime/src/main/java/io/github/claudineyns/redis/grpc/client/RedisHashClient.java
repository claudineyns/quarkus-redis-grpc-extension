package io.github.claudineyns.redis.grpc.client;

import io.github.claudineyns.redis.grpc.client.runtime.GrpcInvoker;
import io.github.claudineyns.redis.grpc.client.runtime.ServiceMethods;
import io.github.claudineyns.redis.grpc.v1.HDelRequest;
import io.github.claudineyns.redis.grpc.v1.HExistsRequest;
import io.github.claudineyns.redis.grpc.v1.HGetAllRequest;
import io.github.claudineyns.redis.grpc.v1.HGetRequest;
import io.github.claudineyns.redis.grpc.v1.HIncrByRequest;
import io.github.claudineyns.redis.grpc.v1.HKeysRequest;
import io.github.claudineyns.redis.grpc.v1.HLenRequest;
import io.github.claudineyns.redis.grpc.v1.HMGetRequest;
import io.github.claudineyns.redis.grpc.v1.HMGetResponse;
import io.github.claudineyns.redis.grpc.v1.HScanRequest;
import io.github.claudineyns.redis.grpc.v1.HScanResponse;
import io.github.claudineyns.redis.grpc.v1.HSetNxRequest;
import io.github.claudineyns.redis.grpc.v1.HSetRequest;
import io.github.claudineyns.redis.grpc.v1.HValsRequest;
import io.github.claudineyns.redis.grpc.v1.HashChange;
import io.github.claudineyns.redis.grpc.v1.HashCount;
import io.github.claudineyns.redis.grpc.v1.HashCounter;
import io.github.claudineyns.redis.grpc.v1.HashEntries;
import io.github.claudineyns.redis.grpc.v1.HashExists;
import io.github.claudineyns.redis.grpc.v1.HashFields;
import io.github.claudineyns.redis.grpc.v1.HashValue;
import io.github.claudineyns.redis.grpc.v1.HashValues;
import io.smallrye.mutiny.Uni;
import io.vertx.grpc.common.ServiceMethod;
import io.vertx.grpc.common.ServiceName;

/**
 * Cliente da família KEY/HASH — {@code HashService} (DESIGN §7, Nível 1).
 */
public final class RedisHashClient {

    private static final ServiceName SERVICE =
            ServiceName.create("io.github.claudineyns.redis.grpc.v1", "HashService");

    private final GrpcInvoker invoker;

    private final ServiceMethod<HashCount, HSetRequest> hset = ServiceMethods.unary(SERVICE, "HSet", HashCount.parser());
    private final ServiceMethod<HashValue, HGetRequest> hget = ServiceMethods.unary(SERVICE, "HGet", HashValue.parser());
    private final ServiceMethod<HashCount, HDelRequest> hdel = ServiceMethods.unary(SERVICE, "HDel", HashCount.parser());
    private final ServiceMethod<HashExists, HExistsRequest> hexists = ServiceMethods.unary(SERVICE, "HExists", HashExists.parser());
    private final ServiceMethod<HashCount, HLenRequest> hlen = ServiceMethods.unary(SERVICE, "HLen", HashCount.parser());
    private final ServiceMethod<HMGetResponse, HMGetRequest> hmget = ServiceMethods.unary(SERVICE, "HMGet", HMGetResponse.parser());
    private final ServiceMethod<HashEntries, HGetAllRequest> hgetall = ServiceMethods.unary(SERVICE, "HGetAll", HashEntries.parser());
    private final ServiceMethod<HashFields, HKeysRequest> hkeys = ServiceMethods.unary(SERVICE, "HKeys", HashFields.parser());
    private final ServiceMethod<HashValues, HValsRequest> hvals = ServiceMethods.unary(SERVICE, "HVals", HashValues.parser());
    private final ServiceMethod<HashChange, HSetNxRequest> hsetnx = ServiceMethods.unary(SERVICE, "HSetNx", HashChange.parser());
    private final ServiceMethod<HashCounter, HIncrByRequest> hincrby = ServiceMethods.unary(SERVICE, "HIncrBy", HashCounter.parser());
    private final ServiceMethod<HScanResponse, HScanRequest> hscan = ServiceMethods.unary(SERVICE, "HScan", HScanResponse.parser());

    public RedisHashClient(final GrpcInvoker invoker) {
        this.invoker = invoker;
    }

    public Uni<HashCount> hset(final HSetRequest request) {
        return invoker.call(hset, request);
    }

    public Uni<HashValue> hget(final HGetRequest request) {
        return invoker.call(hget, request);
    }

    public Uni<HashCount> hdel(final HDelRequest request) {
        return invoker.call(hdel, request);
    }

    public Uni<HashExists> hexists(final HExistsRequest request) {
        return invoker.call(hexists, request);
    }

    public Uni<HashCount> hlen(final HLenRequest request) {
        return invoker.call(hlen, request);
    }

    public Uni<HMGetResponse> hmget(final HMGetRequest request) {
        return invoker.call(hmget, request);
    }

    public Uni<HashEntries> hgetall(final HGetAllRequest request) {
        return invoker.call(hgetall, request);
    }

    public Uni<HashFields> hkeys(final HKeysRequest request) {
        return invoker.call(hkeys, request);
    }

    public Uni<HashValues> hvals(final HValsRequest request) {
        return invoker.call(hvals, request);
    }

    public Uni<HashChange> hsetnx(final HSetNxRequest request) {
        return invoker.call(hsetnx, request);
    }

    public Uni<HashCounter> hincrby(final HIncrByRequest request) {
        return invoker.call(hincrby, request);
    }

    public Uni<HScanResponse> hscan(final HScanRequest request) {
        return invoker.call(hscan, request);
    }
}
