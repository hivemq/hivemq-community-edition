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

import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import util.DummyHandler;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Christoph Schäbel
 */
public class ChannelInactiveHandlerTest {


    private Channel channel;

    private ChannelInactiveHandler channelInactiveHandler;

    @Before
    public void before() {
        channelInactiveHandler = new ChannelInactiveHandler();
        channel = new EmbeddedChannel(new DummyHandler(), channelInactiveHandler);
    }

    @Test
    public void test_channel_inactive_called() throws Exception {

        final AtomicBoolean called = new AtomicBoolean(false);

        channelInactiveHandler.addCallback("id", new ChannelInactiveHandler.ChannelInactiveCallback() {
            @Override
            public void channelInactive() {
                called.set(true);
            }
        });

        channel.close();

        assertTrue(called.get());
    }

    @Test
    public void test_channel_inactive_called_muultiple() throws Exception {

        final AtomicBoolean called = new AtomicBoolean(false);

        channelInactiveHandler.addCallback("id", new ChannelInactiveHandler.ChannelInactiveCallback() {
            @Override
            public void channelInactive() {
                called.set(true);
            }
        });

        final AtomicBoolean called2 = new AtomicBoolean(false);

        channelInactiveHandler.addCallback("id2", new ChannelInactiveHandler.ChannelInactiveCallback() {
            @Override
            public void channelInactive() {
                called2.set(true);
            }
        });

        channel.close();

        assertTrue(called.get());
        assertTrue(called2.get());
    }


    @Test
    public void test_channel_inactive_remove() throws Exception {

        final AtomicBoolean called = new AtomicBoolean(false);

        channelInactiveHandler.addCallback("id", new ChannelInactiveHandler.ChannelInactiveCallback() {
            @Override
            public void channelInactive() {
                called.set(true);
            }
        });

        channelInactiveHandler.removeCallback("id");

        channel.close();

        assertFalse(called.get());
    }

    @Test
    public void test_channel_inactive_remove_multiple() throws Exception {

        final AtomicBoolean called = new AtomicBoolean(false);

        channelInactiveHandler.addCallback("id", new ChannelInactiveHandler.ChannelInactiveCallback() {
            @Override
            public void channelInactive() {
                called.set(true);
            }
        });

        final AtomicBoolean called2 = new AtomicBoolean(false);

        channelInactiveHandler.addCallback("id2", new ChannelInactiveHandler.ChannelInactiveCallback() {
            @Override
            public void channelInactive() {
                called2.set(true);
            }
        });

        channelInactiveHandler.removeCallback("id");

        channel.close();

        assertFalse(called.get());
        assertTrue(called2.get());
    }

}