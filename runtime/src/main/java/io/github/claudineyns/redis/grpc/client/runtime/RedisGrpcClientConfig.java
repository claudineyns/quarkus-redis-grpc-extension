package io.github.claudineyns.redis.grpc.client.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Superfície de configuração do cliente (DESIGN §8).
 *
 * <p>É um <strong>config root de RUN_TIME</strong>: endpoint, TLS e credenciais
 * são específicos do ambiente e vêm de env/secret — nunca "assados" no build.
 * Fica sob o namespace {@code quarkus.} de propósito: como extensão, registramos
 * um config root ali, ganhando reconhecimento/validação de chaves, doc e Dev UI
 * (ver §8).
 *
 * <p>Decisão deliberada: {@link #host()} é {@link Optional} (não obrigatório no
 * mapping) para que <em>apenas adicionar a extensão</em> não quebre o startup de
 * quem ainda não configurou. A semântica "host é obrigatório para operar" é
 * validada na <em>criação do cliente</em> (producer, 2c), com erro claro.
 */
@ConfigMapping(prefix = "quarkus.redis-grpc-client")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface RedisGrpcClientConfig {

    /** Host do gateway redis-grpc. Obrigatório para operar (validado no 2c). */
    Optional<String> host();

    /** Porta do gateway (porta da Route com passthrough TLS). */
    @WithDefault("443")
    int port();

    /**
     * Nome de uma configuração TLS do <strong>Quarkus TLS Registry</strong>
     * ({@code quarkus.tls.<nome>.*}). Setado → TLS com essa config; ausente → a
     * config TLS default do registry, se existir, senão plaintext (dev).
     */
    Optional<String> tlsConfigurationName();

    /**
     * Override de SNI/authority. O cert folha tem CN = host da Route; necessário
     * quando se conecta por IP em vez do hostname.
     */
    Optional<String> authority();

    /** Credenciais de acesso (par ACCESS_KEY/SECRET_KEY). */
    Auth auth();

    interface Auth {

        /** ACCESS_KEY. Segredo — nunca logar. Sem default. */
        Optional<String> accessKey();

        /** SECRET_KEY. Segredo — nunca logar. Sem default. */
        Optional<String> secretKey();

        /** Nome do header da ACCESS_KEY (configurável no servidor). */
        @WithDefault("x-grpc-access-key")
        String accessKeyHeader();

        /** Nome do header da SECRET_KEY (configurável no servidor). */
        @WithDefault("x-grpc-secret-key")
        String secretKeyHeader();
    }
}
