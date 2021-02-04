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
package com.hivemq.mqtt.handler.publish;

import com.google.common.util.concurrent.SettableFuture;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.EncoderException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.channels.ClosedChannelException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author Lukas Brandl
 */
public class PublishWriteFailedListenerTest {

    @Mock
    ChannelFuture channelFuture;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void test_failed() throws Exception {
        final SettableFuture<PublishStatus> statusFuture = SettableFuture.create();
        when(channelFuture.cause()).thenReturn(new EncoderException());

        final PublishWriteFailedListener promiseListener = new PublishWriteFailedListener(statusFuture);
        promiseListener.operationComplete(channelFuture);

        assertEquals(PublishStatus.FAILED, statusFuture.get());
    }

    @Test
    public void test_channel_closed() throws Exception {
        final SettableFuture<Void> storedInPersistenceFuture = SettableFuture.create();
        final SettableFuture<PublishStatus> statusFuture = SettableFuture.create();
        storedInPersistenceFuture.set(null);
        when(channelFuture.cause()).thenReturn(new ClosedChannelException());

        final PublishWriteFailedListener promiseListener = new PublishWriteFailedListener(statusFuture);
        promiseListener.operationComplete(channelFuture);

        assertEquals(PublishStatus.NOT_CONNECTED, statusFuture.get());
    }
}