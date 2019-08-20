package com.hivemq.extensions.handler;

import com.google.common.collect.ImmutableList;
import com.hivemq.annotations.NotNull;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.interceptor.pubrec.PubrecOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubrec.parameter.PubrecOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.pubrec.parameter.PubrecOutboundOutput;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;
import com.hivemq.extension.sdk.api.packets.pubrec.ModifiablePubrecPacket;
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
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.pubrec.PUBREC;
import com.hivemq.mqtt.message.reason.Mqtt5PubRecReasonCode;
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

public class PubrecOutboundInterceptorHandlerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private HiveMQExtensions hiveMQExtensions;

    @Mock
    private HiveMQExtension plugin;

    @Mock
    private ClientContextImpl clientContext;

    private PluginOutPutAsyncer asyncer;

    private FullConfigurationService configurationService;

    private PluginTaskExecutor executor1;

    private EmbeddedChannel channel;

    private PluginTaskExecutorService pluginTaskExecutorService;

    private PubrecOutboundInterceptorHandler handler;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        executor1 = new PluginTaskExecutor(new AtomicLong());
        executor1.postConstruct();

        channel = new EmbeddedChannel();
        channel.attr(ChannelAttributes.CLIENT_ID).set("client");
        channel.attr(ChannelAttributes.REQUEST_RESPONSE_INFORMATION).set(true);
        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        when(plugin.getId()).thenReturn("plugin");

        configurationService = new TestConfigurationBootstrap().getFullConfigurationService();
        asyncer = new PluginOutputAsyncerImpl(mock(ShutdownHooks.class));
        pluginTaskExecutorService = new PluginTaskExecutorServiceImpl(() -> executor1);

        handler = new PubrecOutboundInterceptorHandler(configurationService, asyncer, hiveMQExtensions,
                pluginTaskExecutorService);
        channel.pipeline().addFirst(handler);
    }

    @Test(timeout = 5000)
    public void test_client_id_not_set() {

        channel.attr(ChannelAttributes.CLIENT_ID).set(null);

        channel.writeOutbound(testPubrec());
        channel.runPendingTasks();

        assertNull(channel.readOutbound());
    }

    @Test(timeout = 5000)
    public void test_channel_inactive() throws Exception {

        final ChannelHandlerContext context = channel.pipeline().context(handler);

        channel.close();

        handler.write(context, testPubrec(), mock(ChannelPromise.class));

        channel.runPendingTasks();

        assertNull(channel.readOutbound());
    }

    @Test(timeout = 5000)
    public void test_no_interceptors() {

        when(clientContext.getPubrecOutboundInterceptors()).thenReturn(ImmutableList.of());
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(plugin);

        final PUBREC testPubrec = testPubrec();
        channel.writeOutbound(testPubrec);
        channel.runPendingTasks();
        PUBREC pubrec = channel.readOutbound();
        while (pubrec == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pubrec = channel.readOutbound();
        }
        assertEquals(testPubrec.getReasonCode(), pubrec.getReasonCode());
    }

    @Test(timeout = 5000)
    public void test_modify() throws Exception {

        final PubrecOutboundInterceptor interceptor = getInterceptor("TestModifyOutboundInterceptor");
        final List<PubrecOutboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubrecOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(plugin);

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeOutbound(testPubrec());
        channel.runPendingTasks();
        PUBREC pubrec = channel.readOutbound();
        while (pubrec == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pubrec = channel.readOutbound();
        }

        assertEquals("modified", pubrec.getReasonString());
    }

    @Test(timeout = 5000)
    public void test_plugin_null() throws Exception {

        final PubrecOutboundInterceptor interceptor = getInterceptor("TestModifyOutboundInterceptor");
        final List<PubrecOutboundInterceptor> list = ImmutableList.of(interceptor);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
        when(clientContext.getPubrecOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(null);

        channel.writeOutbound(testPubrec());
        channel.runPendingTasks();
        PUBREC pubrec = channel.readOutbound();
        while (pubrec == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pubrec = channel.readOutbound();
        }

        assertEquals("reason", pubrec.getReasonString());
    }

    @Test(timeout = 10_000)
    public void test_timeout_failed() throws Exception {

        final PubrecOutboundInterceptor interceptor = getInterceptor("TestTimeoutFailedOutboundInterceptor");
        final List<PubrecOutboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubrecOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(plugin);

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeOutbound(testPubrec());

        channel.writeOutbound(testPubrec());
        channel.runPendingTasks();
        channel.runScheduledPendingTasks();

        assertTrue(channel.isActive());
    }

    @Test(timeout = 5000)
    public void test_exception() throws Exception {

        final PubrecOutboundInterceptor interceptor = getInterceptor("TestExceptionOutboundInterceptor");
        final List<PubrecOutboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubrecOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(plugin);

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeOutbound(testPubrec());
        channel.runPendingTasks();
        channel.runScheduledPendingTasks();

        assertTrue(channel.isActive());
    }

    @Test(timeout = 5000)
    public void test_noPartialModificationWhenException() throws Exception {

        final PubrecOutboundInterceptor interceptor = getInterceptor("TestPartialModifiedOutboundInterceptor");
        final List<PubrecOutboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubrecOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(plugin);

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeOutbound(testPubrec());
        channel.runPendingTasks();
        PUBREC pubrec = channel.readOutbound();
        while (pubrec == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pubrec = channel.readOutbound();
        }

        assertNotEquals("modified", pubrec.getReasonString());
        assertNotEquals(AckReasonCode.NOT_AUTHORIZED, pubrec.getReasonCode());
    }

    @NotNull
    private PUBREC testPubrec() {
        return new PUBREC(1, Mqtt5PubRecReasonCode.UNSPECIFIED_ERROR, "reason", Mqtt5UserProperties.NO_USER_PROPERTIES);
    }

    @NotNull
    private PubrecOutboundInterceptor getInterceptor(@NotNull final String name) throws Exception {

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("com.hivemq.extensions.handler.PubrecOutboundInterceptorHandlerTest$" + name);

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        //This classloader contains the classes from the jar file
        final IsolatedPluginClassloader
                cl =
                new IsolatedPluginClassloader(new URL[]{jarFile.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> interceptorClass =
                cl.loadClass("com.hivemq.extensions.handler.PubrecOutboundInterceptorHandlerTest$" + name);

        return (PubrecOutboundInterceptor) interceptorClass.newInstance();
    }

    public static class TestModifyOutboundInterceptor implements PubrecOutboundInterceptor {

        @Override
        public void onOutboundPubrec(
                @NotNull final PubrecOutboundInput pubrecOutboundInput,
                @NotNull final PubrecOutboundOutput pubrecOutboundOutput) {
            @Immutable final ModifiablePubrecPacket pubrecPacket = pubrecOutboundOutput.getPubrecPacket();
            pubrecPacket.setReasonString("modified");
        }
    }

    public static class TestTimeoutFailedOutboundInterceptor implements PubrecOutboundInterceptor {

        @Override
        public void onOutboundPubrec(
                @NotNull final PubrecOutboundInput pubrecOutboundInput,
                @NotNull final PubrecOutboundOutput pubrecOutboundOutput) {
            pubrecOutboundOutput.async(Duration.ofMillis(10), TimeoutFallback.FAILURE);
        }
    }

    public static class TestExceptionOutboundInterceptor implements PubrecOutboundInterceptor {

        @Override
        public void onOutboundPubrec(
                @NotNull final PubrecOutboundInput pubrecOutboundInput,
                @NotNull final PubrecOutboundOutput pubrecOutboundOutput) {
            throw new RuntimeException();
        }
    }

    public static class TestPartialModifiedOutboundInterceptor implements PubrecOutboundInterceptor {

        @Override
        public void onOutboundPubrec(
                @NotNull final PubrecOutboundInput pubrecOutboundInput,
                @NotNull final PubrecOutboundOutput pubrecOutboundOutput) {
            final ModifiablePubrecPacket pubrecPacket = pubrecOutboundOutput.getPubrecPacket();
            pubrecPacket.setReasonString("modified");
            pubrecPacket.setReasonCode(AckReasonCode.NOT_AUTHORIZED);
            throw new RuntimeException();
        }
    }

}