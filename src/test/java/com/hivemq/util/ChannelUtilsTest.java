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
package com.hivemq.util;

import com.google.common.base.Optional;
import com.hivemq.security.auth.ClientToken;
import com.hivemq.security.ssl.SslClientCertificateImpl;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.local.LocalAddress;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.DummyHandler;
import util.TestChannelAttribute;

import java.net.InetSocketAddress;
import java.security.cert.Certificate;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("deprecation")
public class ChannelUtilsTest {

    @Mock
    Channel channel;


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void test_channel_ip() throws Exception {
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress(0));

        final Optional<String> channelIP = ChannelUtils.getChannelIP(channel);

        assertEquals(true, channelIP.isPresent());
        assertEquals("0.0.0.0", channelIP.get());
    }

    @Test
    public void test_no_socket_address_available() throws Exception {
        when(channel.remoteAddress()).thenReturn(null);

        assertEquals(false, ChannelUtils.getChannelIP(channel).isPresent());
    }

    @Test
    public void test_no_inet_socket_address_available() throws Exception {
        when(channel.remoteAddress()).thenReturn(new LocalAddress("myId"));

        assertEquals(false, ChannelUtils.getChannelIP(channel).isPresent());
    }

    @Test
    public void test_token_from_channel_only_client_id() throws Exception {
        final EmbeddedChannel channel = new EmbeddedChannel(new DummyHandler());
        channel.attr(ChannelAttributes.CLIENT_ID).set("theId");

        final ClientToken clientToken = ChannelUtils.tokenFromChannel(channel);

        assertEquals("theId", clientToken.getClientId());
        assertEquals(false, clientToken.getCertificate().isPresent());
        assertEquals(false, clientToken.getUsername().isPresent());
        assertEquals(false, clientToken.getPassword().isPresent());
        assertEquals(false, clientToken.getPasswordBytes().isPresent());
    }

    @Test
    public void test_token_from_channel_client_id_and_cert() throws Exception {
        final EmbeddedChannel channel = new EmbeddedChannel(new DummyHandler());
        channel.attr(ChannelAttributes.CLIENT_ID).set("theId");
        channel.attr(ChannelAttributes.AUTH_CERTIFICATE).set(new SslClientCertificateImpl(new Certificate[]{}));

        final ClientToken clientToken = ChannelUtils.tokenFromChannel(channel);

        assertEquals("theId", clientToken.getClientId());
        assertEquals(true, clientToken.getCertificate().isPresent());
        assertEquals(false, clientToken.getUsername().isPresent());
        assertEquals(false, clientToken.getPassword().isPresent());
        assertEquals(false, clientToken.getPasswordBytes().isPresent());
    }

    @Test
    public void test_token_from_channel_client_id_and_username() throws Exception {
        final EmbeddedChannel channel = new EmbeddedChannel(new DummyHandler());
        channel.attr(ChannelAttributes.CLIENT_ID).set("theId");
        channel.attr(ChannelAttributes.AUTH_USERNAME).set("user");

        final ClientToken clientToken = ChannelUtils.tokenFromChannel(channel);

        assertEquals("theId", clientToken.getClientId());
        assertEquals(false, clientToken.getCertificate().isPresent());
        assertEquals("user", clientToken.getUsername().get());
        assertEquals(false, clientToken.getPassword().isPresent());
        assertEquals(false, clientToken.getPasswordBytes().isPresent());
    }

    @Test
    public void test_token_from_channel_client_id_and_username_and_password() throws Exception {
        final EmbeddedChannel channel = new EmbeddedChannel(new DummyHandler());
        channel.attr(ChannelAttributes.CLIENT_ID).set("theId");
        channel.attr(ChannelAttributes.AUTH_USERNAME).set("user");
        channel.attr(ChannelAttributes.AUTH_PASSWORD).set("pass".getBytes(UTF_8));

        final ClientToken clientToken = ChannelUtils.tokenFromChannel(channel);

        assertEquals("theId", clientToken.getClientId());
        assertEquals(false, clientToken.getCertificate().isPresent());
        assertEquals("user", clientToken.getUsername().get());
        assertEquals("pass", clientToken.getPassword().get());
        assertArrayEquals("pass".getBytes(UTF_8), clientToken.getPasswordBytes().get());
    }

    @Test
    public void test_messages_in_flight() {
        final Channel channel = mock(Channel.class);
        final ChannelPipeline pipeline = mock(ChannelPipeline.class);
        when(channel.pipeline()).thenReturn(pipeline);
        when(channel.attr(ChannelAttributes.IN_FLIGHT_MESSAGES)).thenReturn(new TestChannelAttribute<>(null));
        when(channel.attr(ChannelAttributes.IN_FLIGHT_MESSAGES_SENT)).thenReturn(new TestChannelAttribute<>(true));
        assertFalse(ChannelUtils.messagesInFlight(channel));

        when(channel.attr(ChannelAttributes.IN_FLIGHT_MESSAGES)).thenReturn(new TestChannelAttribute<>(new AtomicInteger(1)));
        assertTrue(ChannelUtils.messagesInFlight(channel));

        when(channel.attr(ChannelAttributes.IN_FLIGHT_MESSAGES)).thenReturn(new TestChannelAttribute<>(new AtomicInteger(0)));
        assertFalse(ChannelUtils.messagesInFlight(channel));
    }

    private class TestAttribute<T> implements Attribute<T> {

        private final T object;

        private TestAttribute(final T object) {
            this.object = object;
        }

        @Override
        public AttributeKey<T> key() {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public T get() {
            return object;
        }

        @Override
        public void set(final T value) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public T getAndSet(final T value) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public T setIfAbsent(final T value) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public T getAndRemove() {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public boolean compareAndSet(final T oldValue, final T newValue) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public void remove() {
            throw new RuntimeException("Not implemented");
        }
    }

}