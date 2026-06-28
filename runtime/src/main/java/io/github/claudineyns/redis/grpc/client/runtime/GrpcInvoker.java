package io.github.claudineyns.redis.grpc.client.runtime;

import java.util.function.Consumer;

import io.smallrye.mutiny.Uni;
import io.vertx.core.MultiMap;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.client.GrpcClientResponse;
import io.vertx.grpc.common.ServiceMethod;

/**
 * Helper de chamada compartilhado (P1, sem stubs).
 *
 * <p>Dado um {@link ServiceMethod} (descritor: serviço + método + marshallers) e
 * um request, dirige o {@link GrpcClient} do Vert.x e devolve um {@link Uni} frio.
 * É o <strong>ponto único</strong> por onde toda RPC unária passa — e, portanto,
 * onde as credenciais são anexadas (2d), via o {@code headerDecorator}.
 *
 * <p>Como o contrato é 100% unário, a resposta é colhida com
 * {@link GrpcClientResponse#last()} (a mensagem única do stream).
 */
public final class GrpcInvoker {

    private final GrpcClient client;
    private final SocketAddress address;
    private final Consumer<MultiMap> headerDecorator;

    public GrpcInvoker(final GrpcClient client, final SocketAddress address,
            final Consumer<MultiMap> headerDecorator) {
        this.client = client;
        this.address = address;
        this.headerDecorator = headerDecorator;
    }

    public <Req, Resp> Uni<Resp> call(final ServiceMethod<Resp, Req> method, final Req request) {
        // Uni FRIO: a cadeia Vert.x só dispara na subscrição (supplier deferred),
        // preservando a semântica reativa do Mutiny.
        return Uni.createFrom().completionStage(() -> client
                .request(address, method)
                .compose(req -> {
                    headerDecorator.accept(req.headers()); // 2d: credenciais (se houver)
                    return req.send(request);
                })
                .compose(GrpcClientResponse::last)
                .toCompletionStage());
    }
}
