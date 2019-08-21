package com.hivemq.extensions.handler;

import com.google.common.collect.ImmutableList;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.interceptor.disconnect.DisconnectOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.disconnect.parameter.DisconnectOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.disconnect.parameter.DisconnectOutboundOutput;
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
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.TestConfigurationBootstrap;

import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DisconnectOutboundInterceptorHandlerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private HiveMQExtensions hiveMQExtensions;

    @Mock
    private HiveMQExtension extension;

    @Mock
    private ClientContextImpl clientContext;

    private PluginOutPutAsyncer asyncer;

    private FullConfigurationService configurationService;

    private PluginTaskExecutor executor;

    private EmbeddedChannel channel;

    private PluginTaskExecutorService pluginTaskExecutorService;

    private DisconnectOutboundInterceptorHandler handler;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        executor = new PluginTaskExecutor(new AtomicLong());
        executor.postConstruct();

        channel = new EmbeddedChannel();

        channel.attr(ChannelAttributes.CLIENT_ID).set("client");
        channel.attr(ChannelAttributes.REQUEST_RESPONSE_INFORMATION).set(true);
        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        when(extension.getId()).thenReturn("extension");

        configurationService = new TestConfigurationBootstrap().getFullConfigurationService();
        asyncer = new PluginOutputAsyncerImpl(mock(ShutdownHooks.class));
        pluginTaskExecutorService = new PluginTaskExecutorServiceImpl(() -> executor);

        handler = new DisconnectOutboundInterceptorHandler(configurationService, asyncer, hiveMQExtensions,
                pluginTaskExecutorService);
        channel.pipeline().addFirst(handler);
    }

    @Test(timeout = 5000)
    public void test_client_id_not_set() {
        channel.attr(ChannelAttributes.CLIENT_ID).set(null);
        channel.writeOutbound(testDisconnect());
        channel.runPendingTasks();
        assertNull(channel.readOutbound());
    }

    @Test(timeout = 5000)
    public void test_channel_inactive() throws Exception {
        final ChannelHandlerContext context = channel.pipeline().context(handler);
        channel.close();

        handler.write(context, testDisconnect(), mock(ChannelPromise.class));
        channel.runPendingTasks();
        assertNull(channel.readOutbound());
    }

    @Test(timeout = 5000)
    public void test_no_interceptors() {
        when(clientContext.getDisconnectOutboundInterceptors()).thenReturn(ImmutableList.of());
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        final DISCONNECT disconnect = testDisconnect();
        channel.writeOutbound(disconnect);
        channel.runPendingTasks();
        DISCONNECT readDisconnect = channel.readOutbound();
        while (readDisconnect == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            readDisconnect = channel.readOutbound();
        }
        assertEquals(disconnect.getReasonCode(), readDisconnect.getReasonCode());

    }

    @Test(timeout = 5000)
    public void test_plugin_null() throws Exception {

        final DisconnectOutboundInterceptor interceptor =
                getIsolatedOutboundInterceptor("TestModifyOutboundInterceptor");
        final List<DisconnectOutboundInterceptor> list = ImmutableList.of(interceptor);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
        when(clientContext.getDisconnectOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(null);

        channel.writeOutbound(testDisconnect());
        channel.runPendingTasks();
        DISCONNECT disconnect = channel.readOutbound();
        while (disconnect == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            disconnect = channel.readOutbound();
        }

        assertEquals("reason", disconnect.getReasonString());
    }

    @Test(timeout = 5000)
    public void test_modified() throws Exception {

        final DisconnectOutboundInterceptor interceptor =
                getIsolatedOutboundInterceptor("TestModifyOutboundInterceptor");
        final List<DisconnectOutboundInterceptor> interceptors = ImmutableList.of(interceptor);

        when(clientContext.getDisconnectOutboundInterceptors()).thenReturn(interceptors);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeOutbound(testDisconnect());
        channel.runPendingTasks();
        DISCONNECT disconnect = channel.readOutbound();
        while (disconnect == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            disconnect = channel.readOutbound();
        }

        assertEquals("modified", disconnect.getReasonString());
    }

    @Test(timeout = 10_000)
    public void test_timeout_failed() throws Exception {

        final DisconnectOutboundInterceptor interceptor =
                getIsolatedOutboundInterceptor("TestTimeoutFailedOutboundInterceptor");
        final List<DisconnectOutboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getDisconnectOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeOutbound(testDisconnect());

        channel.runPendingTasks();
        channel.runScheduledPendingTasks();

        assertTrue(channel.isActive());
    }

    @Test(timeout = 5000)
    public void test_exception() throws Exception {

        final DisconnectOutboundInterceptor interceptor =
                getIsolatedOutboundInterceptor("TestExceptionOutboundInterceptor");
        final List<DisconnectOutboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getDisconnectOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeOutbound(testDisconnect());
        channel.runPendingTasks();
        channel.runScheduledPendingTasks();

        assertTrue(channel.isActive());
    }

    private @NotNull DISCONNECT testDisconnect() {
        return new DISCONNECT(
                Mqtt5DisconnectReasonCode.UNSPECIFIED_ERROR, "reason", Mqtt5UserProperties.NO_USER_PROPERTIES,
                "serverReference", 1);
    }

    private DisconnectOutboundInterceptor getIsolatedOutboundInterceptor(final @NotNull String name) throws Exception {
        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("com.hivemq.extensions.handler.DisconnectOutboundInterceptorHandlerTest$" + name);

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        final IsolatedPluginClassloader
                cl =
                new IsolatedPluginClassloader(new URL[]{jarFile.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> interceptorClass =
                cl.loadClass("com.hivemq.extensions.handler.DisconnectOutboundInterceptorHandlerTest$" + name);

        return (DisconnectOutboundInterceptor) interceptorClass.newInstance();
    }

    public static class TestModifyOutboundInterceptor implements DisconnectOutboundInterceptor {

        @Override
        public void onOutboundDisconnect(
                final @NotNull DisconnectOutboundInput disconnectOutboundInput,
                final @NotNull DisconnectOutboundOutput disconnectOutboundOutput) {
            final ModifiableDisconnectPacket packet = disconnectOutboundOutput.getDisconnectPacket();
            packet.setReasonString("modified");
        }
    }

    public static class TestTimeoutFailedOutboundInterceptor implements DisconnectOutboundInterceptor {

        @Override
        public void onOutboundDisconnect(
                final @NotNull DisconnectOutboundInput disconnectOutboundInput,
                final @NotNull DisconnectOutboundOutput disconnectOutboundOutput) {
            disconnectOutboundOutput.async(Duration.ofMillis(10), TimeoutFallback.FAILURE);
        }
    }

    public static class TestExceptionOutboundInterceptor implements DisconnectOutboundInterceptor {

        @Override
        public void onOutboundDisconnect(
                final @NotNull DisconnectOutboundInput disconnectOutboundInput,
                final @NotNull DisconnectOutboundOutput disconnectOutboundOutput) {
            throw new RuntimeException();
        }
    }

}