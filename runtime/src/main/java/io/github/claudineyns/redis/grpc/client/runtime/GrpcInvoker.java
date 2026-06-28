package io.github.claudineyns.redis.grpc.client.runtime;

import java.util.function.Consumer;

import io.github.claudineyns.redis.grpc.client.RedisGrpcException;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.common.GrpcStatus;
import io.vertx.grpc.common.ServiceMethod;

/**
 * Helper de chamada compartilhado (P1, sem stubs) — ponto único por onde toda RPC
 * unária passa.
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li><strong>Credenciais (2d):</strong> anexa os headers via {@code headerDecorator}.
 *   <li><strong>Propagação de status:</strong> lê {@code response.status()}; se não-OK,
 *       falha o {@link Uni} com {@link RedisGrpcException} tipada (código + nome do
 *       status gRPC), em vez do erro cru do Vert.x.
 *   <li><strong>Métricas (2g):</strong> mede a duração e registra service/method/status
 *       no {@link RedisGrpcMetrics} (no-op se desligado/sem Micrometer).
 * </ul>
 */
public final class GrpcInvoker {

    private final GrpcClient client;
    private final SocketAddress address;
    private final Consumer<MultiMap> headerDecorator;
    private final RedisGrpcMetrics metrics;

    public GrpcInvoker(final GrpcClient client, final SocketAddress address,
            final Consumer<MultiMap> headerDecorator, final RedisGrpcMetrics metrics) {
        this.client = client;
        this.address = address;
        this.headerDecorator = headerDecorator;
        this.metrics = metrics;
    }

    public <Req, Resp> Uni<Resp> call(final ServiceMethod<Resp, Req> method, final Req request) {
        final String service = method.serviceName().name();
        final String methodName = method.methodName();
        // Uni FRIO: a cadeia Vert.x só dispara na subscrição; mede a duração por
        // subscrição (deferred captura o início) e registra no onItemOrFailure.
        return Uni.createFrom().deferred(() -> {
            final long start = System.nanoTime();
            return Uni.createFrom().<Resp>completionStage(() -> client
                    .request(address, method)
                    .compose(req -> {
                        headerDecorator.accept(req.headers()); // 2d
                        return req.send(request);
                    })
                    .compose(response -> response.last().transform(ar -> {
                        // Status chega nos trailers; aqui já está disponível.
                        final GrpcStatus status = response.status();
                        if (status == null || status == GrpcStatus.OK) {
                            return ar.succeeded()
                                    ? Future.succeededFuture(ar.result())
                                    : Future.failedFuture(ar.cause());
                        }
                        return Future.failedFuture(
                                new RedisGrpcException(status.code, status.name(), response.statusMessage()));
                    }))
                    .toCompletionStage())
                    .onItemOrFailure().invoke((item, failure) ->
                            metrics.record(service, methodName, statusOf(failure), System.nanoTime() - start));
        });
    }

    private static String statusOf(final Throwable failure) {
        if (failure == null) {
            return GrpcStatus.OK.name();
        }
        if (failure instanceof RedisGrpcException grpc) {
            return grpc.getStatus();
        }
        // Falhas de transporte/timeout (sem status gRPC do servidor).
        return "ERROR";
    }
}
