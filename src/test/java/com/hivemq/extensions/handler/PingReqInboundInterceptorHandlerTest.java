package com.hivemq.extensions.handler;

import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.pingrequest.PingRequestInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pingrequest.parameter.PingRequestInboundInput;
import com.hivemq.extension.sdk.api.interceptor.pingrequest.parameter.PingRequestInboundOutput;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutputAsyncerImpl;
import com.hivemq.extensions.executor.PluginTaskExecutorServiceImpl;
import com.hivemq.extensions.executor.task.PluginTaskExecutor;
import com.hivemq.extensions.packets.general.ModifiableDefaultPermissionsImpl;
import com.hivemq.mqtt.message.PINGREQ;
import com.hivemq.mqtt.message.ProtocolVersion;
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

import java.io.File;
import java.net.URL;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Robin Atherton
 * @since 4.2.0
 */
public class PingReqInboundInterceptorHandlerTest {

    private PluginTaskExecutor executor1;
    private EmbeddedChannel channel;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private HiveMQExtensions hiveMQExtensions;

    @Mock
    private HiveMQExtension plugin;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        executor1 = new PluginTaskExecutor(new AtomicLong());
        executor1.postConstruct();

        channel = new EmbeddedChannel();
        channel.attr(ChannelAttributes.CLIENT_ID).set("test_client");

        final PluginOutputAsyncerImpl asyncer = new PluginOutputAsyncerImpl(Mockito.mock(ShutdownHooks.class));
        final PluginTaskExecutorServiceImpl pluginTaskExecutorService = new PluginTaskExecutorServiceImpl(() -> executor1);

        final PingReqInboundInterceptorHandler pingreqHandler =
                new PingReqInboundInterceptorHandler(pluginTaskExecutorService, asyncer, hiveMQExtensions);

        channel.pipeline().addFirst(pingreqHandler);
    }

    @After
    public void tearDown() {
        executor1.stop();
        channel.close();
    }

    @Test(timeout = 5000, expected = ClosedChannelException.class)
    public void test_pingreq_channel_closed() {
        channel.close();
        channel.writeInbound(new PINGREQ());
    }

    @Test(timeout = 5000)
    public void test_pingreq_client_id_not_set() {
        channel.attr(ChannelAttributes.CLIENT_ID).set(null);
        channel.writeInbound(new PINGREQ());
        Assert.assertNull(channel.readInbound());
    }

    @Test(timeout = 5000)
    public void test_read_pingreq_mqtt3() throws Exception {
        final ClientContextImpl clientContext
                = new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());

        final PingRequestInboundInterceptor interceptor = getIsolatedInterceptor();
        clientContext.addPingRequestInboundInterceptor(interceptor);

        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedPluginClassloader.class))).thenReturn(plugin);

        channel.writeInbound(new PINGREQ());
        PINGREQ pingreq = channel.readInbound();
        while(pingreq == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pingreq = channel.readInbound();
        }

        Assert.assertNotNull(pingreq);
    }

    @Test(timeout = 5000)
    public void test_read_pingreq_mqtt5() throws Exception {
        final ClientContextImpl clientContext
                = new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());

        final PingRequestInboundInterceptor interceptor = getIsolatedInterceptor();

        clientContext.addPingRequestInboundInterceptor(interceptor);

        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedPluginClassloader.class))).thenReturn(plugin);

        channel.writeInbound(new PINGREQ());
        PINGREQ pingreq = channel.readInbound();
        while(pingreq == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pingreq = channel.readInbound();
        }
        Assert.assertNotNull(pingreq);
    }

    private PingRequestInboundInterceptor getIsolatedInterceptor() throws Exception {
        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("com.hivemq.extensions.handler.PingReqInboundInterceptorHandlerTest$PingReqTestInterceptor");

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        final IsolatedPluginClassloader cl = new IsolatedPluginClassloader(new URL[]{jarFile.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> interceptorClass = cl.loadClass("com.hivemq.extensions.handler.PingReqInboundInterceptorHandlerTest$PingReqTestInterceptor");

        final PingRequestInboundInterceptor interceptor = (PingRequestInboundInterceptor) interceptorClass.newInstance();

        return interceptor;
    }

    public static class PingReqTestInterceptor implements PingRequestInboundInterceptor {
        @Override
        public void onPingReq(final @NotNull PingRequestInboundInput input, final @NotNull PingRequestInboundOutput output) {
            System.out.println("INTERCEPTING " + System.currentTimeMillis());
        }
    }

}