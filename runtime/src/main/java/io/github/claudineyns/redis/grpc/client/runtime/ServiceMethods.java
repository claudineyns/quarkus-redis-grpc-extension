package io.github.claudineyns.redis.grpc.client.runtime;

import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;

import io.vertx.grpc.common.GrpcMessageDecoder;
import io.vertx.grpc.common.GrpcMessageEncoder;
import io.vertx.grpc.common.ServiceMethod;
import io.vertx.grpc.common.ServiceName;

/**
 * Fábrica compartilhada de descritores de método cliente (P1, sem stubs).
 *
 * <p>O contrato é 100% unário, então toda RPC se reduz a um {@link ServiceMethod}
 * com encoder protobuf genérico para o request e decoder a partir do
 * {@code parser()} da mensagem de resposta — evita duplicar essa construção em
 * cada família.
 */
public final class ServiceMethods {

    private ServiceMethods() {
    }

    public static <Req extends MessageLite, Resp> ServiceMethod<Resp, Req> unary(
            final ServiceName service, final String method, final Parser<Resp> responseParser) {
        return ServiceMethod.client(service, method,
                GrpcMessageEncoder.encoder(),
                GrpcMessageDecoder.decoder(responseParser));
    }
}
