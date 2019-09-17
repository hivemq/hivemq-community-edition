package com.hivemq.extensions.handler;

import com.google.common.collect.ImmutableList;
import com.hivemq.annotations.NotNull;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.interceptor.disconnect.DisconnectInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.disconnect.parameter.DisconnectInboundInput;
import com.hivemq.extension.sdk.api.interceptor.disconnect.parameter.DisconnectInboundOutput;
import com.hivemq.extension.sdk.api.packets.disconnect.ModifiableDisconnectPacket;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginOutputAsyncerImpl;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.PluginTaskExecutorServiceImpl;
import com.hivemq.extensions.executor.task.PluginTaskExecutor;
import com.hivemq.extensions.packets.general.ModifiableDefaultPermissionsImpl;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.embedded.EmbeddedChannel;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import util.TestConfigurationBootstrap;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class DisconnectInboundInterceptorHandlerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private HiveMQExtension extension;

    @Mock
    private HiveMQExtensions hiveMQExtensions;

    @Mock
    private ClientContextImpl clientContext;

    @Mock
    private FullConfigurationService configurationService;

    private PluginTaskExecutor executor;
    private EmbeddedChannel channel;
    public static AtomicBoolean isTriggered = new AtomicBoolean();

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        isTriggered.set(false);
        executor = new PluginTaskExecutor(new AtomicLong());
        executor.postConstruct();

        channel = new EmbeddedChannel();
        channel.attr(ChannelAttributes.CLIENT_ID).set("client");
        channel.attr(ChannelAttributes.REQUEST_RESPONSE_INFORMATION).set(true);
        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        when(extension.getId()).thenReturn("extension");

        configurationService = new TestConfigurationBootstrap().getFullConfigurationService();
        final PluginOutPutAsyncer asyncer = new PluginOutputAsyncerImpl(Mockito.mock(ShutdownHooks.class));
        final PluginTaskExecutorService pluginTaskExecutorService = new PluginTaskExecutorServiceImpl(() -> executor);

        final DisconnectInboundInterceptorHandler handler = new DisconnectInboundInterceptorHandler(
                configurationService, asyncer, hiveMQExtensions, pluginTaskExecutorService);
        channel.pipeline().addFirst(handler);
    }

    @After
    public void tearDown() {
        executor.stop();
        channel.close();
    }

    @Test(timeout = 5000)
    public void test_read_simple_disconnect() throws Exception {
        final ClientContextImpl clientContext =
                new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());

        final DisconnectInboundInterceptor interceptor =
                getIsolatedInboundInterceptor("SimpleDisconnectTestInterceptor");
        clientContext.addDisconnectInboundInterceptor(interceptor);

        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedPluginClassloader.class))).thenReturn(extension);

        channel.writeInbound(testDisconnect());
        DISCONNECT disconnect = channel.readInbound();
        while (disconnect == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            disconnect = channel.readInbound();
        }
        Assert.assertTrue(isTriggered.get());
        Assert.assertNotNull(disconnect);
    }

    @Test(timeout = 20000)
    public void test_modified() throws Exception {
        final DisconnectInboundInterceptor interceptor =
                getIsolatedInboundInterceptor("TestModifyInboundInterceptor");
        final List<DisconnectInboundInterceptor> interceptors = ImmutableList.of(interceptor);

        when(clientContext.getDisconnectInboundInterceptors()).thenReturn(interceptors);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeInbound(testDisconnect());
        channel.runPendingTasks();
        DISCONNECT disconnect = channel.readInbound();
        while (disconnect == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            disconnect = channel.readInbound();
        }
        Assert.assertTrue(isTriggered.get());
        assertEquals("modified", disconnect.getReasonString());
    }

    private @NotNull DISCONNECT testDisconnect() {
        return new DISCONNECT(
                Mqtt5DisconnectReasonCode.UNSPECIFIED_ERROR, "reason", Mqtt5UserProperties.NO_USER_PROPERTIES,
                "serverReference", 1);
    }

    private DisconnectInboundInterceptor getIsolatedInboundInterceptor(final @NotNull String name) throws Exception {
        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("com.hivemq.extensions.handler.DisconnectInboundInterceptorHandlerTest$" + name);

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        final IsolatedPluginClassloader
                cl =
                new IsolatedPluginClassloader(new URL[]{jarFile.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> interceptorClass =
                cl.loadClass("com.hivemq.extensions.handler.DisconnectInboundInterceptorHandlerTest$" + name);

        return (DisconnectInboundInterceptor) interceptorClass.newInstance();
    }

    public static class SimpleDisconnectTestInterceptor implements DisconnectInboundInterceptor {

        @Override
        public void onInboundDisconnect(
                final @NotNull DisconnectInboundInput disconnectInboundInput,
                final @NotNull DisconnectInboundOutput disconnectInboundOutput) {
            System.out.println("Intercepting DISCONNECT at: " + System.currentTimeMillis());
            isTriggered.set(true);
        }
    }


    public static class TestModifyInboundInterceptor implements DisconnectInboundInterceptor {

        @Override
        public void onInboundDisconnect(
                final @NotNull DisconnectInboundInput disconnectInboundInput,
                final @NotNull DisconnectInboundOutput disconnectInboundOutput) {
            final ModifiableDisconnectPacket packet = disconnectInboundOutput.getDisconnectPacket();
            System.out.println("Modifed disconnect packet");
            packet.setReasonString("modified");
            isTriggered.set(true);
        }
    }



}
