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

import com.hivemq.util.ChannelAttributes;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

/**
 * @author Christoph Schäbel
 */
public class SslSniHandlerTest {

    @Test
    public void test_replaceHandler() throws Exception {

        final SslHandler sslHandler = mock(SslHandler.class);
        final SslContext sslContext = mock(SslContext.class);

        final SslSniHandler sslSniHandler = new SslSniHandler(sslHandler, sslContext);
        final Channel channel = new EmbeddedChannel(sslSniHandler);

        sslSniHandler.replaceHandler(channel.pipeline().firstContext(), "abc.com", sslContext);

        assertEquals("abc.com", channel.attr(ChannelAttributes.AUTH_SNI_HOSTNAME).get());
        assertSame(sslHandler, channel.pipeline().get(SslHandler.class));
    }


}