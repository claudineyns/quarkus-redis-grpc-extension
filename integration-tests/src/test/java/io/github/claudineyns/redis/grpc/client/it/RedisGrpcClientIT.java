package io.github.claudineyns.redis.grpc.client.it;

import io.quarkus.test.junit.QuarkusIntegrationTest;

/**
 * Roda os mesmos testes contra o <strong>artefato empacotado</strong> — jar em
 * JVM-mode (mvn verify) e binário <strong>native</strong> com {@code -Dnative}.
 * É aqui que a reflection registrada pelo 2e é validada de verdade em native.
 */
@QuarkusIntegrationTest
class RedisGrpcClientIT extends RedisGrpcClientTest {
}
