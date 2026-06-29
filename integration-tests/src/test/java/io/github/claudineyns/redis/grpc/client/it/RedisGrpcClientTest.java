package io.github.claudineyns.redis.grpc.client.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Testes via HTTP (RestAssured) sobre os endpoints da app de integração.
 * Em JVM-mode rodam dentro do processo de teste (@QuarkusTest); o {@code IT}
 * irmão reusa-os contra o artefato empacotado.
 */
@QuarkusTest
class RedisGrpcClientTest {

    @Test
    void clientIsWired() {
        given().when().get("/redis-grpc-it/wired")
                .then().statusCode(200).body(is("true"));
    }

    @Test
    void protobufRoundTripUsesReflection() {
        given().when().get("/redis-grpc-it/proto-roundtrip")
                .then().statusCode(200).body(is("it-key"));
    }
}
