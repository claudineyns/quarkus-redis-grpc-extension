package io.github.claudineyns.redis.grpc.client.runtime;

import java.util.Optional;
import java.util.function.Consumer;

import io.github.claudineyns.redis.grpc.client.RedisGrpcClient;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.client.GrpcClient;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

/**
 * Fiação CDI (DESIGN §7, 2c) — <strong>producer, sem {@code @Recorder}</strong>:
 * nada exige trabalho em build/static-init, então construímos o {@link GrpcClient}
 * em runtime a partir do {@link Vertx} gerenciado + config + TLS Registry, e
 * produzimos o {@link RedisGrpcClient}.
 */
@ApplicationScoped
public class RedisGrpcClientProducer {

    @Inject
    Vertx vertx;

    @Inject
    RedisGrpcClientConfig config;

    @Inject
    TlsConfigurationRegistry tlsRegistry;

    private GrpcClient grpcClient;

    @Produces
    @ApplicationScoped
    public RedisGrpcClient redisGrpcClient() {
        // "host obrigatório para operar" validado aqui (não no config root), para
        // que apenas ter a extensão no classpath não quebre o startup (ver config).
        final String host = config.host().orElseThrow(() -> new IllegalStateException(
                "quarkus.redis-grpc-client.host is required to use the redis-grpc client"));
        final SocketAddress address = SocketAddress.inetSocketAddress(config.port(), host);
        this.grpcClient = GrpcClient.client(vertx, buildHttpOptions());
        return new RedisGrpcClient(new GrpcInvoker(grpcClient, address, buildHeaderDecorator()));
    }

    /**
     * Decorador de headers (2d): anexa ACCESS_KEY/SECRET_KEY <strong>apenas se
     * ambos</strong> estiverem configurados; caso contrário chama sem credenciais
     * (o servidor responde UNAUTHENTICATED se as exigir). Valores são segredos —
     * nunca logados.
     */
    private Consumer<MultiMap> buildHeaderDecorator() {
        final Optional<String> accessKey = config.auth().accessKey();
        final Optional<String> secretKey = config.auth().secretKey();
        if (accessKey.isEmpty() || secretKey.isEmpty()) {
            return headers -> { /* sem credenciais */ };
        }
        final String accessKeyHeader = config.auth().accessKeyHeader();
        final String secretKeyHeader = config.auth().secretKeyHeader();
        final String accessKeyValue = accessKey.get();
        final String secretKeyValue = secretKey.get();
        return headers -> headers
                .set(accessKeyHeader, accessKeyValue)
                .set(secretKeyHeader, secretKeyValue);
    }

    private HttpClientOptions buildHttpOptions() {
        final HttpClientOptions options = new HttpClientOptions().setProtocolVersion(HttpVersion.HTTP_2);

        final Optional<TlsConfiguration> tls = config.tlsConfigurationName().flatMap(tlsRegistry::get);
        if (tls.isEmpty()) {
            // Sem tls-configuration-name -> plaintext h2c (prior knowledge): dev/fake.
            return options.setSsl(false).setHttp2ClearTextUpgrade(false);
        }

        final TlsConfiguration cfg = tls.get();
        options.setSsl(true).setUseAlpn(true);
        if (cfg.isTrustAll()) {
            options.setTrustAll(true);
        } else if (cfg.getTrustStoreOptions() != null) {
            options.setTrustOptions(cfg.getTrustStoreOptions());
        }
        if (cfg.getKeyStoreOptions() != null) {
            options.setKeyCertOptions(cfg.getKeyStoreOptions());
        }
        return options;
    }

    @PreDestroy
    void close() {
        if (grpcClient != null) {
            grpcClient.close();
        }
    }
}
