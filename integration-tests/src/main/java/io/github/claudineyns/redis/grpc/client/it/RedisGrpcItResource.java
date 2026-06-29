package io.github.claudineyns.redis.grpc.client.it;

import io.github.claudineyns.redis.grpc.client.RedisGrpcClient;
import io.github.claudineyns.redis.grpc.v1.GetRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * App mínima de integração (2f): prova, sobre o <strong>artefato empacotado</strong>
 * (jar em JVM-mode; binário native com {@code -Dnative}), que
 * <ul>
 *   <li>(a) o {@link RedisGrpcClient} está <strong>fiado</strong> pela extensão;
 *   <li>(b) a <strong>reflection do protobuf</strong> registrada pelo <strong>2e</strong>
 *       funciona — lendo um campo via {@code FieldAccessorTable} ({@code getField}).
 *       Um build native que tenha perdido uma classe de mensagem falha aqui.
 * </ul>
 */
@Path("/redis-grpc-it")
public class RedisGrpcItResource {

    @Inject
    RedisGrpcClient client;

    @GET
    @Path("/wired")
    @Produces(MediaType.TEXT_PLAIN)
    public boolean wired() {
        return client != null;
    }

    @GET
    @Path("/proto-roundtrip")
    @Produces(MediaType.TEXT_PLAIN)
    public String protoRoundtrip() throws Exception {
        final GetRequest req = GetRequest.newBuilder().setKey("it-key").build();
        final GetRequest parsed = GetRequest.parseFrom(req.toByteArray());
        // Leitura dinâmica via descritor -> FieldAccessorTable (reflection do 2e):
        return (String) parsed.getField(GetRequest.getDescriptor().findFieldByName("key"));
    }
}
