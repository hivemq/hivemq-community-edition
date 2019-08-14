package com.hivemq.extensions.handler;

import com.hivemq.annotations.NotNull;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.extension.sdk.api.interceptor.pingrequestresponse.PingRequestResponseInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pingrequestresponse.parameter.PingRequestResponseInput;
import com.hivemq.extension.sdk.api.interceptor.pingrequestresponse.parameter.PingRequestResponseOutput;
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
import com.hivemq.mqtt.message.PINGREQ;
import com.hivemq.mqtt.message.PINGRESP;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.embedded.EmbeddedChannel;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class PingRequestResponseInterceptorHandlerTest {

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

        final PingRequestResponseInterceptorHandler handler =
                new PingRequestResponseInterceptorHandler(pluginTaskExecutorService, asyncer, hiveMQExtensions);
        channel.pipeline().addFirst(handler);
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

    @Test(timeout = 5000, expected = ClosedChannelException.class)
    public void test_pingresp_channel_closed() {
        channel.close();
        channel.writeInbound(new PINGRESP());
    }

    @Test(timeout = 5000)
    public void test_read_pingreq_send_pingresp() throws Exception {
        final ClientContextImpl clientContext
                = new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());

        final PingRequestResponseInterceptor interceptor = getIsolatedInterceptor("TestInterceptor");
        clientContext.addPingRequestResponseInterceptor(interceptor);

        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedPluginClassloader.class))).thenReturn(plugin);

        channel.writeInbound(new PINGREQ());
        PINGREQ pingreq = channel.readInbound();
        while (pingreq == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pingreq = channel.readInbound();
        }
        channel.writeOutbound(new PINGRESP());
        PINGRESP pingresp = channel.readOutbound();
        while (pingresp == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pingresp = channel.readOutbound();
        }

    }

    private PingRequestResponseInterceptor getIsolatedInterceptor(final @NotNull String name) throws Exception {
        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("com.hivemq.extensions.handler.PingRequestResponseInterceptorHandlerTest$" + name);

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        final IsolatedPluginClassloader
                cl =
                new IsolatedPluginClassloader(new URL[]{jarFile.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> interceptorClass =
                cl.loadClass("com.hivemq.extensions.handler.PingRequestResponseInterceptorHandlerTest$" + name);

        final PingRequestResponseInterceptor interceptor =
                (PingRequestResponseInterceptor) interceptorClass.newInstance();

        return interceptor;
    }

    public static class TestInterceptor implements PingRequestResponseInterceptor {

        int counter = 0;

        @Override
        public void onPing(
                @NotNull final PingRequestResponseInput pingRequestResponseInput,
                @NotNull final PingRequestResponseOutput pingRequestResponseOutput) {
            System.out.println("lul");
            counter++;
            if (counter == 1) {
                System.out.println("Received Ping request");
            }
            if (counter == 2) {
                System.out.println("Sent out Ping response");
            }
        }
    }

}