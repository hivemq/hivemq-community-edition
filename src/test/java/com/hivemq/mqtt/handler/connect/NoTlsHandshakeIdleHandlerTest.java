package com.hivemq.mqtt.handler.connect;

import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.atomic.AtomicBoolean;

import static io.netty.handler.timeout.IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT;
import static io.netty.handler.timeout.IdleStateEvent.FIRST_WRITER_IDLE_STATE_EVENT;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Florian Limpöck
 * @since 4.4.2
 */
@SuppressWarnings("NullabilityAnnotations")
public class NoTlsHandshakeIdleHandlerTest {

    @Mock
    MqttServerDisconnector mqttServerDisconnector;

    private NoTlsHandshakeIdleHandler handler;
    private EmbeddedChannel embeddedChannel;
    private AtomicBoolean userEventTriggered;

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);
        handler = new NoTlsHandshakeIdleHandler(mqttServerDisconnector);
        userEventTriggered = new AtomicBoolean(false);
        final ChannelInboundHandlerAdapter eventAdapter = new ChannelInboundHandlerAdapter(){
            @Override
            public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) {
                userEventTriggered.set(true);
            }
        };

        embeddedChannel = new EmbeddedChannel();
        embeddedChannel.pipeline().addLast(handler);
        embeddedChannel.pipeline().addLast(eventAdapter);
    }

    @Test
    public void test_nothing_happens_for_non_idle_state_event() throws Exception {
        handler.userEventTriggered(embeddedChannel.pipeline().context(handler), "SomeEvent");

        verify(mqttServerDisconnector, never()).logAndClose(any(Channel.class), any(), any());
        assertTrue(userEventTriggered.get());
    }

    @Test
    public void test_nothing_happens_for_idle_state_writer_event() throws Exception {

        handler.userEventTriggered(embeddedChannel.pipeline().context(handler), FIRST_WRITER_IDLE_STATE_EVENT);

        verify(mqttServerDisconnector, never()).logAndClose(any(Channel.class), any(), any());
        assertTrue(userEventTriggered.get());
    }

    @Test
    public void test_idle_state_reader_event() throws Exception {

        handler.userEventTriggered(embeddedChannel.pipeline().context(handler), FIRST_READER_IDLE_STATE_EVENT);

        verify(mqttServerDisconnector, times(1)).logAndClose(any(Channel.class), any(), any());
        assertFalse(userEventTriggered.get());
    }
}