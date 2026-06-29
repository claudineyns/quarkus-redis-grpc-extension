package io.github.claudineyns.redis.grpc.client.deployment;

import java.util.Optional;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

import io.github.claudineyns.redis.grpc.client.runtime.MicrometerRedisGrpcMetrics;
import io.github.claudineyns.redis.grpc.client.runtime.RedisGrpcClientProducer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.runtime.metrics.MetricsFactory;

class RedisGrpcClientProcessor {

    private static final Logger LOG = Logger.getLogger(RedisGrpcClientProcessor.class);

    private static final String FEATURE = "redis-grpc-client";

    /** Pacote das classes de mensagem protobuf geradas (java_package do contrato). */
    private static final String MESSAGE_PACKAGE_PREFIX = "io.github.claudineyns.redis.grpc.v1.";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    /**
     * Registra o producer do cliente como bean, garantindo que o consumidor sempre
     * o descubra (DESIGN §5). Unremovable: o {@link RedisGrpcClientProducer} fica
     * mesmo que o Arc não veja injeção direta no momento da análise.
     */
    @BuildStep
    AdditionalBeanBuildItem registerClient() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(RedisGrpcClientProducer.class)
                .setUnremovable()
                .build();
    }

    /**
     * Métricas (2g) <strong>opcionais</strong>: só registra a impl Micrometer quando
     * o consumidor tem o sistema de métricas <strong>Micrometer</strong> ativo —
     * sinalizado pelo {@link MetricsCapabilityBuildItem} (produzido pelo
     * quarkus-micrometer). Isso é preciso (distingue de smallrye-metrics) e garante
     * que o bean {@code MeterRegistry} existe. Sem isso, a impl nunca vira bean (nem
     * é carregada) e o producer cai no NOOP.
     */
    @BuildStep
    void registerMetrics(final Optional<MetricsCapabilityBuildItem> metricsCapability,
            final BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        if (metricsCapability.isPresent()
                && metricsCapability.get().metricsSupported(MetricsFactory.MICROMETER)) {
            additionalBeans.produce(AdditionalBeanBuildItem.builder()
                    .addBeanClass(MicrometerRedisGrpcMetrics.class)
                    // Escopo definido aqui (a classe não tem anotação — ver javadoc dela):
                    // o gate é a única porta; sem isso o runtime jar indexado (2e) a
                    // auto-descobriria e exigiria MeterRegistry sem Micrometer presente.
                    .setDefaultScope(DotNames.APPLICATION_SCOPED)
                    .setUnremovable()
                    .build());
        }
    }

    /**
     * As classes de mensagem geradas vivem no nosso runtime jar, que por padrão não
     * entra no índice Jandex do consumidor. Pedimos a indexação dele para que o scan
     * abaixo as encontre.
     */
    @BuildStep
    IndexDependencyBuildItem indexRuntime() {
        return new IndexDependencyBuildItem("io.github.claudineyns", "redis-grpc-client");
    }

    /**
     * Native (2e): registra as classes de mensagem protobuf geradas para reflection.
     * O {@code protobuf-java} usa reflection na {@code FieldAccessorTable}
     * (getters/setters) e nos builders. Varremos o índice pelo pacote gerado (em vez
     * de lista fixa) para cobrir automaticamente as mensagens atuais e futuras.
     * Best-effort: validado só quando um build native rodar.
     */
    @BuildStep
    void registerProtobufMessagesForReflection(final CombinedIndexBuildItem combinedIndex,
            final BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        final String[] classes = combinedIndex.getIndex().getKnownClasses().stream()
                .map(ClassInfo::name)
                .map(DotName::toString)
                .filter(name -> name.startsWith(MESSAGE_PACKAGE_PREFIX))
                .toArray(String[]::new);
        if (classes.length > 0) {
            reflectiveClasses.produce(ReflectiveClassBuildItem.builder(classes).methods().fields().build());
        }
        LOG.debugf("redis-grpc-client: registered %d protobuf message class(es) for native reflection",
                classes.length);
    }
}
