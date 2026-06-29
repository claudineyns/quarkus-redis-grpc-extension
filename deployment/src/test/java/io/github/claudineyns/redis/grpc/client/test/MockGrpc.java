package io.github.claudineyns.redis.grpc.client.test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.claudineyns.redis.grpc.client.runtime.GrpcInvoker;
import io.smallrye.mutiny.Uni;

/**
 * Helper de teste (cobertura oficial, sem servidor): devolve um {@link GrpcInvoker}
 * mockado cujo {@code call(...)} completa com {@code null}. Chamar os métodos de
 * alto nível exercita a superfície da API (delegação + inicialização dos
 * descritores) sem rede. A correção funcional é garantida pelos live tests.
 */
final class MockGrpc {

    private MockGrpc() {
    }

    static GrpcInvoker invoker() {
        final GrpcInvoker invoker = mock(GrpcInvoker.class);
        when(invoker.call(any(), any())).thenReturn(Uni.createFrom().nullItem());
        return invoker;
    }
}
