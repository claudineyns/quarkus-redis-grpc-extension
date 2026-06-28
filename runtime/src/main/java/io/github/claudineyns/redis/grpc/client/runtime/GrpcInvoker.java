package io.github.claudineyns.redis.grpc.client.runtime;

import java.util.Locale;
import java.util.function.Consumer;

import org.jboss.logging.Logger;

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
 *   <li><strong>Logging (2h):</strong> log de acesso por chamada em DEBUG
 *       (service/method/status/durationMs); falhas inesperadas (transporte) em WARN.
 *       Nunca loga segredos nem valores.
 * </ul>
 */
public final class GrpcInvoker {

    private static final Logger LOG = Logger.getLogger(GrpcInvoker.class);

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
                    .onItemOrFailure().invoke((item, failure) -> {
                        final long durationNanos = System.nanoTime() - start;
                        final String status = statusOf(failure);
                        metrics.record(service, methodName, status, durationNanos); // 2g
                        logCall(service, methodName, status, durationNanos, failure); // 2h
                    });
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

    /**
     * Log de acesso (2h): sucesso e erros gRPC esperados (status do servidor) em
     * DEBUG; falhas inesperadas (transporte/timeout) em WARN. Sem segredos/valores.
     */
    private static void logCall(final String service, final String method, final String status,
            final long durationNanos, final Throwable failure) {
        // Locale.ROOT: duração estável (ponto decimal) independente do locale do host.
        final String durationMs = String.format(Locale.ROOT, "%.1f", durationNanos / 1_000_000.0);
        if (failure != null && !(failure instanceof RedisGrpcException)) {
            LOG.warnf(failure, "%s/%s failed (%s) in %s ms", service, method, status, durationMs);
            return;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debugf("%s/%s -> %s in %s ms", service, method, status, durationMs);
        }
    }
}
