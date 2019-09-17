package single.plugin.interceptor.pubrel;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.annotations.NotNull;
import com.hivemq.configuration.service.SecurityConfigurationService;
import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.interceptor.pubrel.PubrelInboundInterceptor;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopOutput;
import com.hivemq.extension.sdk.api.services.Services;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.connack.CONNACK;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import com.hivemq.mqtt.message.reason.Mqtt5PubRelReasonCode;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.Checkpoints;
import com.hivemq.util.TestMessageEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class PubrelInboundInterceptorPriorityIT {

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

        final Bootstrap client = createClient();
        final Channel channel = client.connect("127.0.0.1", rule.getPort()).sync().channel();
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.pipeline().addLast(new SimpleChannelInboundHandler<CONNACK>() {
            @Override
            protected void channelRead0(final ChannelHandlerContext ctx, final CONNACK msg) {
                connackLatch.countDown();
                System.out.println("connack");
            }
        });

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                17,
                // variable header
                //   protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                0,
                // payload
                //   client identifier
                0, 4, 't', 'e', 's', 't'
        };
        channel.writeAndFlush(Unpooled.wrappedBuffer(encoded)).sync();

        connackLatch.await(10, TimeUnit.SECONDS);

        final Mqtt5UserProperties
                userProperties = Mqtt5UserProperties.builder().add(new MqttUserProperty("prop1", "unmodified")).build();
        final PUBREL pubrel =
                new PUBREL(1337, Mqtt5PubRelReasonCode.PACKET_IDENTIFIER_NOT_FOUND, "unmodified", userProperties);
        channel.writeAndFlush(pubrel);

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

            final PubrelInboundInterceptor interceptor = (pubrelInboundInput, pubrelInboundOutput) -> {

                final CountDownLatch interceptorCalled = SharedState.get("interceptor3", CountDownLatch.class);
                interceptorCalled.countDown();

            };

            Services.initializerRegistry()
                    .setClientInitializer(
                            (initializerInput, clientContext) -> clientContext.addPubrelInboundInterceptor(
                                    interceptor));
        }

        @Override
        public void extensionStop(@NotNull final ExtensionStopInput input, @NotNull final ExtensionStopOutput output) {

        }
    }

    public static class Extension2 implements ExtensionMain {

        @Override
        public void extensionStart(@NotNull final ExtensionStartInput psi, @NotNull final ExtensionStartOutput pso) {

            final PubrelInboundInterceptor interceptor = (pubrelInboundInput, pubrelInboundOutput) -> {

                final CountDownLatch interceptorCalled = SharedState.get("interceptor2", CountDownLatch.class);
                interceptorCalled.countDown();

            };

            Services.initializerRegistry()
                    .setClientInitializer(
                            (initializerInput, clientContext) -> clientContext.addPubrelInboundInterceptor(
                                    interceptor));
        }

        @Override
        public void extensionStop(@NotNull final ExtensionStopInput input, @NotNull final ExtensionStopOutput output) {

        }
    }

    public static class Extension1 implements ExtensionMain {

        @Override
        public void extensionStart(@NotNull final ExtensionStartInput psi, @NotNull final ExtensionStartOutput pso) {

            final PubrelInboundInterceptor interceptor = (pubrelInboundInput, pubrelInboundOutput) -> {

                final CountDownLatch interceptorCalled = SharedState.get("interceptor1", CountDownLatch.class);
                interceptorCalled.countDown();

            };

            Services.initializerRegistry()
                    .setClientInitializer(
                            (initializerInput, clientContext) -> clientContext.addPubrelInboundInterceptor(
                                    interceptor));
        }

        @Override
        public void extensionStop(@NotNull final ExtensionStopInput input, @NotNull final ExtensionStopOutput output) {

        }
    }

}
