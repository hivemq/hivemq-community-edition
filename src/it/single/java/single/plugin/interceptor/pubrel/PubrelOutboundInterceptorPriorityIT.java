package single.plugin.interceptor.pubrel;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.annotations.NotNull;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAck;
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAckReasonCode;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishResult;
import com.hivemq.configuration.service.SecurityConfigurationService;
import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.interceptor.pubrel.PubrelOutboundInterceptor;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopOutput;
import com.hivemq.extension.sdk.api.services.Services;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.util.Checkpoints;
import com.hivemq.util.TestMessageEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.junit.Rule;
import org.junit.Test;
import util.TestMqttDecoder;
import util.it.EmbeddedHiveMQRule;
import util.it.SharedState;
import util.it.TestPluginUtil;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;

public class PubrelOutboundInterceptorPriorityIT {

    @Rule
    public final EmbeddedHiveMQRule rule = new EmbeddedHiveMQRule(false);

    @Test(timeout = 90_000)
    public void test_correct_priority_order() throws Exception {

        final File hiveMqPluginFolder = rule.getEmbeddedHiveMQ().getSystemInformation().getExtensionsFolder();

        TestPluginUtil.createPlugin(hiveMqPluginFolder,
                "test-extension-3", "Test Extension 3", "1.2.3", "100", Extension3.class, true);

        TestPluginUtil.createPlugin(hiveMqPluginFolder,
                "test-extension-2", "Test Extension 2", "1.2.3", "200", Extension2.class, true);

        TestPluginUtil.createPlugin(hiveMqPluginFolder,
                "test-extension-1", "Test Extension 1", "1.2.3", "300", Extension1.class, true);

        final CountDownLatch interceptor1Called = new CountDownLatch(1);
        SharedState.set("interceptor1", interceptor1Called);

        final CountDownLatch interceptor2Called = new CountDownLatch(1);
        SharedState.set("interceptor2", interceptor2Called);

        final CountDownLatch interceptor3Called = new CountDownLatch(1);
        SharedState.set("interceptor3", interceptor3Called);

        final CountDownLatch connackLatch = new CountDownLatch(1);

        rule.start();

        Checkpoints.waitForCheckpoint("extension-started", 1);

        final Mqtt5BlockingClient publisher = MqttClient.builder()
                .identifier("publisher")
                .serverHost("127.0.0.1")
                .serverPort(rule.getPort())
                .useMqttVersion5()
                .buildBlocking();

        final Mqtt5BlockingClient subscriber = MqttClient.builder()
                .identifier("subscriber")
                .serverHost("127.0.0.1")
                .serverPort(rule.getPort())
                .useMqttVersion5()
                .buildBlocking();

        final Mqtt5ConnAck connack1 = publisher.connect();

        final Mqtt5ConnAck connack2 = subscriber.connect();


        assertEquals(Mqtt5ConnAckReasonCode.SUCCESS, connack1.getReasonCode());
        assertEquals(Mqtt5ConnAckReasonCode.SUCCESS, connack2.getReasonCode());

        subscriber.subscribeWith().topicFilter("mytopic/mySubtopic/").qos(MqttQos.EXACTLY_ONCE).send();

        try {
            final Mqtt5PublishResult publishResult = publisher.publishWith()
                    .topic("mytopic/mySubtopic/")
                    .qos(MqttQos.EXACTLY_ONCE)
                    .payload("payload".getBytes(StandardCharsets.UTF_8))
                    .send();
        } finally {
            publisher.disconnect();
        }

        interceptor1Called.await();
        interceptor2Called.await();
        interceptor3Called.await();

    }

    private Bootstrap createClient() {
        final MessageDroppedService
                messageDroppedService = rule.getEmbeddedHiveMQ().getInjectableInstance(MessageDroppedService.class);
        final SecurityConfigurationService securityConfigurationService =
                rule.getEmbeddedHiveMQ().getInjectableInstance(
                        SecurityConfigurationService.class);
        final EventLoopGroup group = new NioEventLoopGroup();
        final Bootstrap b = new Bootstrap();
        b.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(final SocketChannel ch) {
                        final ChannelPipeline p = ch.pipeline();
                        final MetricsHolder metricsHolder = new MetricsHolder(new MetricRegistry());
                        p.addLast(TestMqttDecoder.create(false));
                        p.addLast(new TestMessageEncoder(messageDroppedService, securityConfigurationService));

                    }
                });

        return b;
    }

    public static class Extension3 implements ExtensionMain {

        @Override
        public void extensionStart(@NotNull final ExtensionStartInput psi, @NotNull final ExtensionStartOutput pso) {

            final PubrelOutboundInterceptor interceptor = (pubrelOutboundInput, pubrelOutboundOutput) -> {

                final CountDownLatch interceptorCalled = SharedState.get("interceptor3", CountDownLatch.class);
                interceptorCalled.countDown();

            };

            Services.initializerRegistry()
                    .setClientInitializer(
                            (initializerInput, clientContext) -> clientContext.addPubrelOutboundInterceptor(
                                    interceptor));
        }

        @Override
        public void extensionStop(@NotNull final ExtensionStopInput input, @NotNull final ExtensionStopOutput output) {

        }
    }

    public static class Extension2 implements ExtensionMain {

        @Override
        public void extensionStart(@NotNull final ExtensionStartInput psi, @NotNull final ExtensionStartOutput pso) {

            final PubrelOutboundInterceptor interceptor = (pubrelOutboundInput, pubrelOutboundOutput) -> {

                final CountDownLatch interceptorCalled = SharedState.get("interceptor2", CountDownLatch.class);
                interceptorCalled.countDown();

            };

            Services.initializerRegistry()
                    .setClientInitializer(
                            (initializerInput, clientContext) -> clientContext.addPubrelOutboundInterceptor(
                                    interceptor));
        }

        @Override
        public void extensionStop(@NotNull final ExtensionStopInput input, @NotNull final ExtensionStopOutput output) {

        }
    }

    public static class Extension1 implements ExtensionMain {

        @Override
        public void extensionStart(@NotNull final ExtensionStartInput psi, @NotNull final ExtensionStartOutput pso) {

            final PubrelOutboundInterceptor interceptor = (pubrelOutboundInput, pubrelOutboundOutput) -> {

                final CountDownLatch interceptorCalled = SharedState.get("interceptor1", CountDownLatch.class);
                interceptorCalled.countDown();

            };

            Services.initializerRegistry()
                    .setClientInitializer(
                            (initializerInput, clientContext) -> clientContext.addPubrelOutboundInterceptor(
                                    interceptor));
        }

        @Override
        public void extensionStop(@NotNull final ExtensionStopInput input, @NotNull final ExtensionStopOutput output) {

        }
    }

}
