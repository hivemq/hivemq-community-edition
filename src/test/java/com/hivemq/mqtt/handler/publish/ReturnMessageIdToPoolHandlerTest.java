/*
 * Copyright 2019 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hivemq.mqtt.handler.publish;

import com.hivemq.mqtt.message.MessageIDPools;
import com.hivemq.mqtt.message.pool.MessageIDPool;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author Dominik Obermaier
 */
public class ReturnMessageIdToPoolHandlerTest {

    @Mock
    MessageIDPools pools;
    @Mock
    MessageIDPool pool;

    private EmbeddedChannel embeddedChannel;

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);

        when(pool.takeNextId()).thenReturn(100);
        when(pools.forClientOrNull(anyString())).thenReturn(pool);
        embeddedChannel = new EmbeddedChannel(new ReturnMessageIdToPoolHandler(pools));
        embeddedChannel.attr(ChannelAttributes.CLIENT_ID).set("clientId");
    }

    @Test
    public void test_return_qos_1_message_id() throws Exception {

        final PUBACK puback = new PUBACK(pool.takeNextId());
        embeddedChannel.writeInbound(puback);

        verify(pool).returnId(eq(100));

    }

    @Test
    public void test_return_qos_2_message_id() throws Exception {

        final PUBCOMP pubcomp = new PUBCOMP(pool.takeNextId());
        embeddedChannel.writeInbound(pubcomp);

        verify(pool).returnId(eq(100));
    }

    @Test
    public void test_dont_return_message_id() throws Exception {

        final PUBREL pubrel = new PUBREL(pool.takeNextId());
        embeddedChannel.writeInbound(pubrel);

        verify(pool, never()).returnId(anyInt());
    }

    @Test
    public void test_dont_return_invalid_message_id() {

        final PUBACK puback = new PUBACK(-1);
        embeddedChannel.writeInbound(puback);

        verify(pool, never()).returnId(anyInt());
    }
}