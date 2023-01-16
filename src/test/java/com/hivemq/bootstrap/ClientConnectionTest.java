package com.hivemq.bootstrap;

import com.hivemq.mqtt.handler.publish.PublishFlushHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.local.LocalAddress;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ClientConnectionTest {

    ClientConnection clientConnection;

    final Channel channel = mock(Channel.class);
    final ChannelPipeline pipeline = mock(ChannelPipeline.class);
    final PublishFlushHandler publishFlushHandler = mock(PublishFlushHandler.class);

    @Before
    public void setUp() {
        when(channel.pipeline()).thenReturn(pipeline);
        clientConnection = new ClientConnection(channel, publishFlushHandler);
    }

    @Test
    public void isMessagesInFlight_whenInFlightMessagesFlagIsSet_thenMessagesAreInFlight() {
        clientConnection.setInFlightMessagesSent(true);

        assertFalse(clientConnection.isMessagesInFlight());
    }

    @Test
    public void isMessagesInFlight_whenInFlightCountIsIncremented_thenMessagesAreInFlight() {
        clientConnection.setInFlightMessagesSent(true);

        clientConnection.incrementInFlightCount();
        assertTrue(clientConnection.isMessagesInFlight());
    }

    @Test
    public void isMessagesInFlight_whenInFlightCountIsIncrementedAndDecrementedToZero_thenMessagesAreNotInFlight() {
        clientConnection.setInFlightMessagesSent(true);

        clientConnection.incrementInFlightCount();
        clientConnection.decrementInFlightCount();

        assertFalse(clientConnection.isMessagesInFlight());
    }

    @Test
    public void getChannelIP_whenChannelRemoteAddrIsSet_thenRemoteAddrIsReturned() {
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress(0));

        final Optional<String> channelIP = clientConnection.getChannelIP();

        assertTrue(channelIP.isPresent());
        assertEquals("0.0.0.0", channelIP.get());
    }

    @Test
    public void getChannelIP_whenNoSocketAddrIsSet_thenNoAddressIsReturned() {
        when(channel.remoteAddress()).thenReturn(null);
        assertFalse(clientConnection.getChannelIP().isPresent());
    }

    @Test
    public void getChannelIP_whenNoInetSocketAddrIsSet_thenNoAddressIsReturned() {
        when(channel.remoteAddress()).thenReturn(new LocalAddress("myId"));
        assertFalse(clientConnection.getChannelIP().isPresent());
    }
}