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
package com.hivemq.codec.decoder.mqtt5;

import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnectorImpl;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.After;
import org.junit.Before;
import org.slf4j.LoggerFactory;
import util.LogbackCapturingAppender;
import util.TestMqttDecoder;

public class AbstractMqttDecoderTest {

    protected EmbeddedChannel channel;
    LogbackCapturingAppender logCapture;

    @Before
    public void setUp() {
        logCapture = LogbackCapturingAppender.Factory.weaveInto(LoggerFactory.getLogger(MqttServerDisconnectorImpl.class));
        createChannel();
    }

    @After
    public void tearDown() {
        channel.close();
    }

    protected void createChannel() {
        channel = new EmbeddedChannel(TestMqttDecoder.create());
    }

}