/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.security.ssl;

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.bootstrap.ClientConnectionContext;
import com.hivemq.configuration.service.entity.TcpListener;
import com.hivemq.configuration.service.entity.Tls;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.junit.Test;
import util.DummyClientConnection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Christoph Schäbel
 */
public class SslSniHandlerTest {

    @Test
    public void test_replaceHandler() throws Exception {

        final SslHandler sslHandler = mock(SslHandler.class);
        final SslContext sslContext = mock(SslContext.class);
        final SslFactory sslFactory = mock(SslFactory.class);
        final Tls tls = mock(Tls.class);

        final Channel ch = mock(Channel.class);
        final MqttServerDisconnector mqttServerDisconnector = mock(MqttServerDisconnector.class);

        final SslSniHandler sslSniHandler = new SslSniHandler(sslContext,
                sslFactory,
                mqttServerDisconnector, ch, tls, (final Channel channel) -> {});
        final Channel channel = new EmbeddedChannel(sslSniHandler);
        final DummyClientConnection dummyClientConnection = new DummyClientConnection(channel, null, new TcpListener(8883, "localhost", "ssl"));


        final Future<Channel> value = new DefaultPromise<>(GlobalEventExecutor.INSTANCE);
        when(sslHandler.handshakeFuture()).thenReturn(value);
        when(sslFactory.getSslHandler(ch, tls, sslContext, "abc.com", 8883)).thenReturn(sslHandler);

        channel.attr(ClientConnectionContext.CHANNEL_ATTRIBUTE_NAME).set(dummyClientConnection);

        sslSniHandler.replaceHandler(channel.pipeline().firstContext(), "abc.com", sslContext);

        assertEquals("abc.com", ClientConnection.of(channel).getAuthSniHostname());
        assertSame(sslHandler, channel.pipeline().get(SslHandler.class));
    }
}
