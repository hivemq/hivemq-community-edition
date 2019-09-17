package single.plugin.interceptor.pubrel;

import com.hivemq.annotations.NotNull;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAck;
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAckReasonCode;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishResult;
import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.async.Async;
import com.hivemq.extension.sdk.api.interceptor.pubrel.PubrelOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubrel.parameter.PubrelOutboundOutput;
import com.hivemq.extension.sdk.api.packets.pubrel.ModifiablePubrelPacket;
import com.hivemq.extension.sdk.api.packets.pubrel.PubrelPacket;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopOutput;
import com.hivemq.extension.sdk.api.services.Services;
import com.hivemq.util.Checkpoints;
import org.junit.Rule;
import org.junit.Test;
import util.it.EmbeddedHiveMQRule;
import util.it.SharedState;
import util.it.TestPluginUtil;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;

public class PubrelOutboundInterceptorAsyncIT {

    @Rule
    public final EmbeddedHiveMQRule embeddedHiveMQRule = new EmbeddedHiveMQRule(false);

    @Test(timeout = 90_000)
    public void test_modify_values_mqtt_5() throws Exception {
        final File hiveMqPluginFolder =
                embeddedHiveMQRule.getEmbeddedHiveMQ().getSystemInformation().getExtensionsFolder();

        TestPluginUtil.createPlugin(hiveMqPluginFolder,
                "test-extension-1", "Test Extension", "1.2.3", "200", ModifierMain.class, true);

        TestPluginUtil.createPlugin(hiveMqPluginFolder,
                "test-extension-2", "Test Extension 2", "1.2.3", "100", ModifyCheckerMain.class, true);

        final CountDownLatch modifierCalled = new CountDownLatch(1);
        SharedState.set("modifierCalled", modifierCalled);

        final CountDownLatch modifiedCalled = new CountDownLatch(1);
        SharedState.set("modifyCheckerCalled", modifiedCalled);

        embeddedHiveMQRule.start();

        Checkpoints.waitForCheckpoint("extension-started", 1);

        final Mqtt5BlockingClient publisher = MqttClient.builder()
                .identifier("publisher")
                .serverHost("127.0.0.1")
                .serverPort(embeddedHiveMQRule.getPort())
                .useMqttVersion5()
                .buildBlocking();

        final Mqtt5BlockingClient subscriber = MqttClient.builder()
                .identifier("subscriber")
                .serverHost("127.0.0.1")
                .serverPort(embeddedHiveMQRule.getPort())
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

        modifierCalled.await();
        System.out.println("First Interceptor got called.");

        modifiedCalled.await();
        System.out.println("Pubrel got intercepted and modified");
    }

    public static class ModifierMain implements ExtensionMain {

        @Override
        public void extensionStart(@NotNull final ExtensionStartInput psi, @NotNull final ExtensionStartOutput pso) {

            final PubrelOutboundInterceptor interceptor = (pubrelOutboundInput, pubrelOutboundOutput) -> {

                final Async<PubrelOutboundOutput> async = pubrelOutboundOutput.async(Duration.ofSeconds(5));

                new Thread(() -> {
                    final ModifiablePubrelPacket pubrelPacket = pubrelOutboundOutput.getPubrelPacket();
                    pubrelPacket.getUserProperties().clear();
                    pubrelPacket.getUserProperties().addUserProperty("prop1", "modified");
                    async.resume();
                }).start();

                final CountDownLatch interceptorCalled = SharedState.get("modifierCalled", CountDownLatch.class);
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

    public static class ModifyCheckerMain implements ExtensionMain {

        @Override
        public void extensionStart(@NotNull final ExtensionStartInput psi, @NotNull final ExtensionStartOutput pso) {

            final PubrelOutboundInterceptor interceptor = (pubrelOutboundInput, pubrelOutboundOutput) -> {

                Thread.sleep(5000);

                final PubrelPacket pubrelPacket = pubrelOutboundInput.getPubrelPacket();
                assertEquals("modified", pubrelPacket.getUserProperties().getFirst("prop1").get());

                final CountDownLatch interceptorCalled = SharedState.get("modifyCheckerCalled", CountDownLatch.class);
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
