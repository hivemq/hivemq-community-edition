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
package com.hivemq.mqtt.handler;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.handler.*;
import com.hivemq.mqtt.message.PINGREQ;
import com.hivemq.mqtt.message.PINGRESP;
import com.hivemq.mqtt.message.auth.AUTH;
import com.hivemq.mqtt.message.connack.CONNACK;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.pubrec.PUBREC;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import com.hivemq.mqtt.message.suback.SUBACK;
import com.hivemq.mqtt.message.unsuback.UNSUBACK;
import com.hivemq.mqtt.message.unsubscribe.UNSUBSCRIBE;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

/**
 * @author Daniel Krüger
 */
public class InterceptorHandlerTest {

    @Mock
    private @NotNull ConnectInboundInterceptorHandler connectInboundInterceptorHandler;
    @Mock
    private @NotNull ConnackOutboundInterceptorHandler connackOutboundInterceptorHandler;
    @Mock
    private @NotNull PublishOutboundInterceptorHandler publishOutboundInterceptorHandler;
    @Mock
    private @NotNull PubackInterceptorHandler pubackInterceptorHandler;
    @Mock
    private @NotNull PubrecInterceptorHandler pubrecInterceptorHandler;
    @Mock
    private @NotNull PubrelInterceptorHandler pubrelInterceptorHandler;
    @Mock
    private @NotNull PubcompInterceptorHandler pubcompInterceptorHandler;
    @Mock
    private @NotNull SubackOutboundInterceptorHandler subackOutboundInterceptorHandler;
    @Mock
    private @NotNull UnsubscribeInboundInterceptorHandler unsubscribeInboundInterceptorHandler;
    @Mock
    private @NotNull UnsubackOutboundInterceptorHandler unsubackOutboundInterceptorHandler;
    @Mock
    private @NotNull PingInterceptorHandler pingInterceptorHandler;
    @Mock
    private @NotNull DisconnectInterceptorHandler disconnectInterceptorHandler;
    @Mock
    private @NotNull ChannelHandlerContext channelHandlerContext;
    @Mock
    private @NotNull ChannelPromise channelPromise;

    private @NotNull InterceptorHandler interceptorHandler;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        interceptorHandler = new InterceptorHandler(
                connectInboundInterceptorHandler,
                connackOutboundInterceptorHandler,
                publishOutboundInterceptorHandler,
                pubackInterceptorHandler,
                pubrecInterceptorHandler,
                pubrelInterceptorHandler,
                pubcompInterceptorHandler,
                subackOutboundInterceptorHandler,
                unsubscribeInboundInterceptorHandler,
                unsubackOutboundInterceptorHandler,
                pingInterceptorHandler,
                disconnectInterceptorHandler);
    }

    @Test
    public void test_read_connect() {
        interceptorHandler.channelRead(channelHandlerContext, mock(CONNECT.class));
        verify(connectInboundInterceptorHandler, times(1)).handleInboundConnect(any(), any());
    }

    @Test
    public void test_read_puback() {
        interceptorHandler.channelRead(channelHandlerContext, mock(PUBACK.class));
        verify(pubackInterceptorHandler, times(1)).handleInboundPuback(any(), any());
    }

    @Test
    public void test_read_pubrec() {
        interceptorHandler.channelRead(channelHandlerContext, mock(PUBREL.class));
        verify(pubrelInterceptorHandler, times(1)).handleInboundPubrel(any(), any());
    }

    @Test
    public void test_read_pubrel() {
        interceptorHandler.channelRead(channelHandlerContext, mock(PUBREL.class));
        verify(pubrelInterceptorHandler, times(1)).handleInboundPubrel(any(), any());
    }

    @Test
    public void test_read_pubcomp() {
        interceptorHandler.channelRead(channelHandlerContext, mock(PUBCOMP.class));
        verify(pubcompInterceptorHandler, times(1)).handleInboundPubcomp(any(), any());
    }

    @Test
    public void test_read_unsubscribe() {
        interceptorHandler.channelRead(channelHandlerContext, mock(UNSUBSCRIBE.class));
        verify(unsubscribeInboundInterceptorHandler, times(1)).handleInboundUnsubscribe(any(), any());
    }

    @Test
    public void test_read_pingreq() {
        interceptorHandler.channelRead(channelHandlerContext, mock(PINGREQ.class));
        verify(pingInterceptorHandler, times(1)).handleInboundPingReq(any(), any());
    }

    @Test
    public void test_read_disconnect() {
        interceptorHandler.channelRead(channelHandlerContext, mock(DISCONNECT.class));
        verify(disconnectInterceptorHandler, times(1)).handleInboundDisconnect(any(), any());
    }

    @Test
    public void test_read_without_responsible_interceptor() {
        interceptorHandler.channelRead(channelHandlerContext, mock(AUTH.class));
        verify(channelHandlerContext, times(1)).fireChannelRead(any());
    }

    @Test
    public void test_write_connack() {
        interceptorHandler.write(channelHandlerContext, mock(CONNACK.class), channelPromise);
        verify(connackOutboundInterceptorHandler, times(1)).handleOutboundConnack(any(), any(), any());
    }

    @Test
    public void test_write_publish() {
        interceptorHandler.write(channelHandlerContext, mock(PUBLISH.class), channelPromise);
        verify(publishOutboundInterceptorHandler, times(1)).handleOutboundPublish(any(), any(), any());
    }

    @Test
    public void test_write_puback() {
        interceptorHandler.write(channelHandlerContext, mock(PUBACK.class), channelPromise);
        verify(pubackInterceptorHandler, times(1)).handleOutboundPuback(any(), any(), any());
    }

    @Test
    public void test_write_pubrec() {
        interceptorHandler.write(channelHandlerContext, mock(PUBREC.class), channelPromise);
        verify(pubrecInterceptorHandler, times(1)).handleOutboundPubrec(any(), any(), any());
    }

    @Test
    public void test_write_pubrel() {
        interceptorHandler.write(channelHandlerContext, mock(PUBREL.class), channelPromise);
        verify(pubrelInterceptorHandler, times(1)).handleOutboundPubrel(any(), any(), any());
    }

    @Test
    public void test_write_pubcomp() {
        interceptorHandler.write(channelHandlerContext, mock(PUBCOMP.class), channelPromise);
        verify(pubcompInterceptorHandler, times(1)).handleOutboundPubcomp(any(), any(), any());
    }

    @Test
    public void test_write_suback() {
        interceptorHandler.write(channelHandlerContext, mock(SUBACK.class), channelPromise);
        verify(subackOutboundInterceptorHandler, times(1)).handleOutboundSuback(any(), any(), any());
    }

    @Test
    public void test_write_unsuback() {
        interceptorHandler.write(channelHandlerContext, mock(UNSUBACK.class), channelPromise);
        verify(unsubackOutboundInterceptorHandler, times(1)).handleOutboundUnsuback(any(), any(), any());
    }

    @Test
    public void test_write_pingresp() {
        interceptorHandler.write(channelHandlerContext, mock(PINGRESP.class), channelPromise);
        verify(pingInterceptorHandler, times(1)).handleOutboundPingResp(any(), any(), any());
    }

    @Test
    public void test_write_disconnect() {
        interceptorHandler.write(channelHandlerContext, mock(DISCONNECT.class), channelPromise);
        verify(disconnectInterceptorHandler, times(1)).handleOutboundDisconnect(any(), any(), any());
    }

    @Test
    public void test_write_without_responsible_interceptor() {
        interceptorHandler.write(channelHandlerContext, mock(AUTH.class), channelPromise);
        verify(channelHandlerContext, times(1)).write(any(), any());
    }
}
