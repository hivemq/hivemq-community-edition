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
package com.hivemq.metrics.handler;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import util.DummyHandler;

import java.util.List;
import java.util.concurrent.Executors;

import static com.hivemq.bootstrap.netty.ChannelHandlerNames.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MetricsInitializerTest {

    private @NotNull EmbeddedChannel embeddedChannel;

    @Before
    public void setUp() throws Exception {

        embeddedChannel = new EmbeddedChannel(new DummyHandler());
        embeddedChannel.pipeline().addFirst(ALL_CHANNELS_GROUP_HANDLER, new DummyHandler());
        embeddedChannel.pipeline().addFirst(MQTT_MESSAGE_ENCODER, new DummyHandler());
        embeddedChannel.pipeline().addFirst(STATISTICS_INITIALIZER,
                new MetricsInitializer(
                        new GlobalTrafficCounter(new MetricRegistry(), Executors.newSingleThreadScheduledExecutor()))
        );
    }

    @Test
    public void test_initializer_removed_itself() throws Exception {
        final List<String> names = embeddedChannel.pipeline().names();

        //Let's see if the initializer was removed
        assertFalse(names.contains(STATISTICS_INITIALIZER));
        assertTrue(names.contains(GLOBAL_TRAFFIC_COUNTER));
    }
}