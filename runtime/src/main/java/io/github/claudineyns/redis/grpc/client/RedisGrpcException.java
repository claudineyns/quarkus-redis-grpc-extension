package io.github.claudineyns.redis.grpc.client;

/**
 * Erro de uma chamada ao gateway redis-grpc cujo <strong>status gRPC não é OK</strong>
 * (DESIGN do proxy §5.1: erros reais do Redis viram status gRPC).
 *
 * <p>Carrega o <strong>código</strong> e o <strong>nome</strong> do status gRPC
 * (conceitos universais — não vaza tipos Vert.x na API). Ex.: WRONGTYPE no Redis
 * chega como {@code FAILED_PRECONDITION} (código 9); token inválido como
 * {@code UNAUTHENTICATED} (16).
 */
public final class RedisGrpcException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int statusCode;
    private final String status;

    public RedisGrpcException(final int statusCode, final String status, final String message) {
        super("gRPC " + status + " (" + statusCode + ")"
                + (message == null || message.isBlank() ? "" : ": " + message));
        this.statusCode = statusCode;
        this.status = status;
    }

    /** Código numérico do status gRPC (ex.: 9). */
    public int getStatusCode() {
        return statusCode;
    }

    /** Nome do status gRPC (ex.: {@code FAILED_PRECONDITION}). */
    public String getStatus() {
        return status;
    }
}
