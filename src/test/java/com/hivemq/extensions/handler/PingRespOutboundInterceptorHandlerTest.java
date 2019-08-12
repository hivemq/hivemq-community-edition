package com.hivemq.extensions.handler;

import com.hivemq.annotations.NotNull;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.extension.sdk.api.interceptor.pingresponse.PingResponseOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pingresponse.parameter.PingResponseOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.pingresponse.parameter.PingResponseOutboundOutput;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginOutputAsyncerImpl;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.PluginTaskExecutorServiceImpl;
import com.hivemq.extensions.executor.task.PluginTaskExecutor;
import com.hivemq.mqtt.message.PINGRESP;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.embedded.EmbeddedChannel;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.net.URL;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

public class PingRespOutboundInterceptorHandlerTest {

    private PluginTaskExecutor executor1;
    private EmbeddedChannel channel;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private PluginOutPutAsyncer asyncer;

    @Mock
    private HiveMQExtension plugin;

    @Mock
    private HiveMQExtensions hiveMQExtensions;

    @Mock
    private PluginTaskExecutorService pluginTaskExecutorService;

    private PingRespOutboundInterceptorHandler handler;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        executor1 = new PluginTaskExecutor(new AtomicLong());
        executor1.postConstruct();

        channel = new EmbeddedChannel();
        channel.attr(ChannelAttributes.CLIENT_ID).set("client");
        channel.attr(ChannelAttributes.REQUEST_RESPONSE_INFORMATION).set(true);
        when(plugin.getId()).thenReturn("plugin");

        asyncer = new PluginOutputAsyncerImpl(Mockito.mock(ShutdownHooks.class));
        pluginTaskExecutorService = new PluginTaskExecutorServiceImpl(() -> executor1);

        handler = new PingRespOutboundInterceptorHandler(pluginTaskExecutorService, asyncer, hiveMQExtensions);
        channel.pipeline().addFirst(handler);
    }

    @Test(timeout = 5000, expected = ClosedChannelException.class)
    public void test_pingresp_channel_closed() {
        channel.close();
        channel.writeOutbound(new PINGRESP());
    }

    @Test(timeout = 5000)
    public void test_pingresp_successful() throws Exception {
        final PingResponseOutboundInterceptor pingResponseOutboundInterceptor = getInterceptor("TestPingResponseInterceptor");
        when(hiveMQExtensions.getExtension(eq("plugin"))).thenReturn(plugin);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeOutbound(new PINGRESP());
        channel.runPendingTasks();
        PINGRESP pingresp = channel.readOutbound();
        Assert.assertNotNull(pingresp);
    }


    private @NotNull PingResponseOutboundInterceptor getInterceptor(final @NotNull String name)
            throws Exception {
        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("com.hivemq.extensions.handler.PingRespOutboundInterceptorHandlerTest$" + name);

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        final IsolatedPluginClassloader cl = new IsolatedPluginClassloader(new URL[]{jarFile.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> providerClass = cl.loadClass("com.hivemq.extensions.handler.PingRespOutboundInterceptorHandlerTest$" + name);

        return (PingResponseOutboundInterceptor) providerClass.newInstance();
    }


    public static class TestPingResponseInterceptor implements PingResponseOutboundInterceptor {

        @Override
        public void onPingResp(final @NotNull PingResponseOutboundInput input, final @NotNull PingResponseOutboundOutput output) {
            System.out.println("Ping response sent");
        }
    }

}