package com.hivemq.extensions.handler;

import com.google.common.collect.ImmutableMap;
import com.hivemq.annotations.NotNull;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.async.Async;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extension.sdk.api.interceptor.connect.ConnectInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.connect.ConnectInboundInterceptorProvider;
import com.hivemq.extension.sdk.api.interceptor.connect.parameter.ConnectInboundOutput;
import com.hivemq.extension.sdk.api.interceptor.connect.parameter.ConnectInboundProviderInput;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginOutputAsyncerImpl;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.PluginTaskExecutorServiceImpl;
import com.hivemq.extensions.executor.task.PluginTaskExecutor;
import com.hivemq.extensions.services.interceptor.Interceptors;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.connack.Mqtt3ConnAckReturnCode;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import util.TestConfigurationBootstrap;

import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * @author Lukas Brandl
 */
public class ConnectInboundInterceptorHandlerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private HiveMQExtensions hiveMQExtensions;

    @Mock
    private HiveMQExtension plugin;

    @Mock
    private Interceptors interceptors;

    @Mock
    private ServerInformation serverInformation;

    @Mock
    private MqttConnacker connacker;

    private PluginOutPutAsyncer asyncer;

    private FullConfigurationService configurationService;

    private PluginTaskExecutor executor1;

    private EmbeddedChannel channel;

    private PluginTaskExecutorService pluginTaskExecutorService;

    private ConnectInboundInterceptorHandler handler;

    private HivemqId hivemqId = new HivemqId();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        executor1 = new PluginTaskExecutor(new AtomicLong());
        executor1.postConstruct();

        channel = new EmbeddedChannel();
        channel.attr(ChannelAttributes.CLIENT_ID).set("client");
        when(plugin.getId()).thenReturn("plugin");

        configurationService = new TestConfigurationBootstrap().getFullConfigurationService();
        asyncer = new PluginOutputAsyncerImpl(Mockito.mock(ShutdownHooks.class));
        pluginTaskExecutorService = new PluginTaskExecutorServiceImpl(() -> executor1);

        handler = new ConnectInboundInterceptorHandler(configurationService, asyncer, hiveMQExtensions,
                pluginTaskExecutorService,
                hivemqId, interceptors, serverInformation, connacker);
        channel.pipeline().addFirst(handler);
    }

    @Test(timeout = 5000)
    public void test_client_id_not_set() {

        channel.attr(ChannelAttributes.CLIENT_ID).set(null);

        channel.writeInbound(testConnect());

        assertNull(channel.readInbound());
    }

    @Test
    public void test_modify() throws Exception {

        final ConnectInboundInterceptorProvider interceptorProvider = getInterceptor("TestModifyInboundInterceptor");
        when(interceptors.connectInterceptorProviders()).thenReturn(ImmutableMap.of("plugin", interceptorProvider));
        when(hiveMQExtensions.getExtension(eq("plugin"))).thenReturn(plugin);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeInbound(testConnect());
        channel.runPendingTasks();
        CONNECT connect = channel.readInbound();
        while (connect == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            connect = channel.readInbound();
        }

        assertEquals("modified", connect.getClientIdentifier());
    }

    @Test
    public void test_timeout_failed() throws Exception {

        final ConnectInboundInterceptorProvider interceptorProvider =
                getInterceptor("TestTimeoutFailedInboundInterceptor");
        when(interceptors.connectInterceptorProviders()).thenReturn(ImmutableMap.of("plugin", interceptorProvider));
        when(hiveMQExtensions.getExtension(eq("plugin"))).thenReturn(plugin);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeInbound(testConnect());
        channel.runPendingTasks();
        verify(connacker, timeout(5000)).connackError(any(Channel.class), anyString(), anyString(),
                any(Mqtt5ConnAckReasonCode.class),
                any(Mqtt3ConnAckReturnCode.class), anyString());
    }

    @Test
    public void test_exception() throws Exception {

        final ConnectInboundInterceptorProvider interceptorProvider = getInterceptor("TestExceptionInboundInterceptor");
        when(interceptors.connectInterceptorProviders()).thenReturn(ImmutableMap.of("plugin", interceptorProvider));
        when(hiveMQExtensions.getExtension(eq("plugin"))).thenReturn(plugin);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeInbound(testConnect());
        channel.runPendingTasks();

        CONNECT connect = channel.readInbound();
        while (connect == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            connect = channel.readInbound();
        }

        assertEquals("client", connect.getClientIdentifier());
        verify(connacker, timeout(5000)).connackError(any(Channel.class), anyString(), anyString(), any(Mqtt5ConnAckReasonCode.class),
                any(Mqtt3ConnAckReturnCode.class), anyString());
    }

    @NotNull
    private CONNECT testConnect() {
        return new CONNECT.Mqtt5Builder().withClientIdentifier("client").build();
    }

    @NotNull
    private ConnectInboundInterceptorProvider getInterceptor(@NotNull final String name) throws Exception {


        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("com.hivemq.extensions.handler.ConnectInboundInterceptorHandlerTest$" + name);

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        //This classloader contains the classes from the jar file
        final IsolatedPluginClassloader cl =
                new IsolatedPluginClassloader(new URL[]{jarFile.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> providerClass =
                cl.loadClass("com.hivemq.extensions.handler.ConnectInboundInterceptorHandlerTest$" + name);

        return (ConnectInboundInterceptorProvider) providerClass.newInstance();
    }

    public static class TestModifyInboundInterceptor implements ConnectInboundInterceptorProvider {

        @Override
        public @Nullable ConnectInboundInterceptor getConnectInterceptor(
                @NotNull final ConnectInboundProviderInput providerInput) {
            System.out.println("Provider called");
            return (input, output) -> output.getConnectPacket().setClientId("modified");
        }
    }

    public static class TestTimeoutFailedInboundInterceptor implements ConnectInboundInterceptorProvider {

        @Override
        public @Nullable ConnectInboundInterceptor getConnectInterceptor(
                @NotNull final ConnectInboundProviderInput providerInput) {
            System.out.println("Provider called");
            return (input, output) -> {
                final Async<ConnectInboundOutput> async =
                        output.async(Duration.ofMillis(10), TimeoutFallback.FAILURE);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            };
        }
    }

    public static class TestExceptionInboundInterceptor implements ConnectInboundInterceptorProvider {

        @Override
        public @Nullable ConnectInboundInterceptor getConnectInterceptor(
                @NotNull final ConnectInboundProviderInput providerInput) {
            System.out.println("Provider called");
            return (input, output) -> {
                throw new RuntimeException("test");
            };
        }
    }
}