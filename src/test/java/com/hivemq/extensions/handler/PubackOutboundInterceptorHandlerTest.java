package com.hivemq.extensions.handler;

import com.google.common.collect.ImmutableList;
import com.hivemq.annotations.NotNull;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.interceptor.puback.PubackOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.puback.parameter.PubackOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.puback.parameter.PubackOutboundOutput;
import com.hivemq.extension.sdk.api.packets.puback.ModifiablePubackPacket;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;
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
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.mqtt.message.reason.Mqtt5PubAckReasonCode;
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
import static org.mockito.Mockito.*;

/**
 * @author Yannick Weber
 */
public class PubackOutboundInterceptorHandlerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private HiveMQExtensions hiveMQExtensions;

    @Mock
    private HiveMQExtension plugin;

    @Mock
    private ClientContextImpl clientContext;

    private PluginTaskExecutor executor1;

    private EmbeddedChannel channel;

    private PubackOutboundInterceptorHandler handler;

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

        final FullConfigurationService configurationService =
                new TestConfigurationBootstrap().getFullConfigurationService();
        final PluginOutPutAsyncer asyncer = new PluginOutputAsyncerImpl(mock(ShutdownHooks.class));
        final PluginTaskExecutorService pluginTaskExecutorService = new PluginTaskExecutorServiceImpl(() -> executor1);

        handler = new PubackOutboundInterceptorHandler(configurationService, asyncer, hiveMQExtensions,
                pluginTaskExecutorService);
        channel.pipeline().addFirst(handler);
    }

    @Test(timeout = 5000)
    public void test_client_id_not_set() {

        channel.attr(ChannelAttributes.CLIENT_ID).set(null);

        channel.writeOutbound(testPuback());
        channel.runPendingTasks();

        assertNull(channel.readOutbound());
    }

    @Test(timeout = 5000)
    public void test_channel_inactive() throws Exception {

        final ChannelHandlerContext context = channel.pipeline().context(handler);

        channel.close();

        handler.write(context, testPuback(), mock(ChannelPromise.class));

        channel.runPendingTasks();

        assertNull(channel.readOutbound());
    }

    @Test(timeout = 5000)
    public void test_no_interceptors() {

        when(clientContext.getPubackOutboundInterceptors()).thenReturn(ImmutableList.of());
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(plugin);

        final PUBACK testPuback = testPuback();
        channel.writeOutbound(testPuback);
        channel.runPendingTasks();
        PUBACK puback = channel.readOutbound();
        while (puback == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            puback = channel.readOutbound();
        }
        assertEquals(testPuback.getReasonCode(), puback.getReasonCode());
    }


    @Test(timeout = 5000)
    public void test_modify() throws Exception {

        final PubackOutboundInterceptor interceptor = getInterceptor("TestModifyOutboundInterceptor");
        final List<PubackOutboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubackOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(plugin);

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeOutbound(testPuback());
        channel.runPendingTasks();
        PUBACK puback = channel.readOutbound();
        while (puback == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            puback = channel.readOutbound();
        }

        assertEquals("modified", puback.getReasonString());
    }

    @Test(timeout = 5000)
    public void test_plugin_null() throws Exception {

        final PubackOutboundInterceptor interceptor = getInterceptor("TestModifyOutboundInterceptor");
        final List<PubackOutboundInterceptor> list = ImmutableList.of(interceptor);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
        when(clientContext.getPubackOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(null);

        channel.writeOutbound(testPuback());
        channel.runPendingTasks();
        PUBACK puback = channel.readOutbound();
        while (puback == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            puback = channel.readOutbound();
        }

        assertEquals("reason", puback.getReasonString());
    }

    @Test(timeout = 10_000)
    public void test_timeout_failed() throws Exception {

        final PubackOutboundInterceptor interceptor = getInterceptor("TestTimeoutFailedOutboundInterceptor");
        final List<PubackOutboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubackOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(plugin);

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeOutbound(testPuback());

        channel.writeOutbound(testPuback());
        channel.runPendingTasks();
        channel.runScheduledPendingTasks();

        assertTrue(channel.isActive());
    }

    @Test(timeout = 5000)
    public void test_exception() throws Exception {

        final PubackOutboundInterceptor interceptor = getInterceptor("TestExceptionOutboundInterceptor");
        final List<PubackOutboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubackOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(plugin);

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeOutbound(testPuback());
        channel.runPendingTasks();
        channel.runScheduledPendingTasks();

        assertTrue(channel.isActive());
    }

    @Test(timeout = 5000)
    public void test_noPartialModificationWhenException() throws Exception {

        final PubackOutboundInterceptor interceptor = getInterceptor("TestPartialModifiedOutboundInterceptor");
        final List<PubackOutboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubackOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(plugin);

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeOutbound(testPuback());
        channel.runPendingTasks();
        PUBACK puback = channel.readOutbound();
        while (puback == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            puback = channel.readOutbound();
        }

        assertNotEquals("modified", puback.getReasonString());
        assertNotEquals(AckReasonCode.NOT_AUTHORIZED, puback.getReasonCode());
    }


    @NotNull
    private PUBACK testPuback() {
        return new PUBACK(1, Mqtt5PubAckReasonCode.UNSPECIFIED_ERROR, "reason", Mqtt5UserProperties.NO_USER_PROPERTIES);
    }

    @NotNull
    private PubackOutboundInterceptor getInterceptor(@NotNull final String name) throws Exception {

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("com.hivemq.extensions.handler.PubackOutboundInterceptorHandlerTest$" + name);

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        //This classloader contains the classes from the jar file
        final IsolatedPluginClassloader
                cl = new IsolatedPluginClassloader(new URL[]{jarFile.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> interceptorClass = cl.loadClass("com.hivemq.extensions.handler.PubackOutboundInterceptorHandlerTest$" + name);

        return (PubackOutboundInterceptor) interceptorClass.newInstance();
    }

    public static class TestModifyOutboundInterceptor implements PubackOutboundInterceptor {

        @Override
        public void onOutboundPuback(@NotNull final PubackOutboundInput pubackOutboundInput, @NotNull final PubackOutboundOutput pubackOutboundOutput) {
            @Immutable final ModifiablePubackPacket pubackPacket = pubackOutboundOutput.getPubackPacket();
            pubackPacket.setReasonString("modified");
        }
    }

    public static class TestTimeoutFailedOutboundInterceptor implements PubackOutboundInterceptor {

        @Override
        public void onOutboundPuback(@NotNull final PubackOutboundInput pubackOutboundInput, @NotNull final PubackOutboundOutput pubackOutboundOutput) {
            pubackOutboundOutput.async(Duration.ofMillis(10), TimeoutFallback.FAILURE);
        }
    }

    public static class TestExceptionOutboundInterceptor implements PubackOutboundInterceptor {

        @Override
        public void onOutboundPuback(@NotNull final PubackOutboundInput pubackOutboundInput, @NotNull final PubackOutboundOutput pubackOutboundOutput) {
            throw new RuntimeException();
        }
    }

    public static class TestPartialModifiedOutboundInterceptor implements PubackOutboundInterceptor {

        @Override
        public void onOutboundPuback(
                @NotNull final PubackOutboundInput pubrecOutboundInput,
                @NotNull final PubackOutboundOutput pubackOutboundOutput) {
            final ModifiablePubackPacket pubackPacket = pubackOutboundOutput.getPubackPacket();
            pubackPacket.setReasonString("modified");
            pubackPacket.setReasonCode(AckReasonCode.NOT_AUTHORIZED);
            throw new RuntimeException();
        }
    }

}