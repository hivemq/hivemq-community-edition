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
package util.encoder;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.codec.encoder.MQTTMessageEncoder;
import com.hivemq.configuration.service.SecurityConfigurationService;
import com.hivemq.configuration.service.impl.SecurityConfigurationServiceImpl;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.logging.EventLog;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.metrics.handler.GlobalMQTTMessageCounter;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnectorImpl;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import io.netty.channel.ChannelHandler;

import static org.mockito.Mockito.mock;

/**
 * @author Dominik Obermaier
 * @author Lukas Brandl
 */
@ChannelHandler.Sharable
public class TestMessageEncoder extends MQTTMessageEncoder {

    private final @NotNull SecurityConfigurationService securityConfigurationService;

    public TestMessageEncoder() {
        this(mock(MessageDroppedService.class), new SecurityConfigurationServiceImpl());
    }

    public TestMessageEncoder(
            final MessageDroppedService messageDroppedService,
            final SecurityConfigurationService securityConfigurationService) {

        super(new TestEncoderFactory(messageDroppedService, securityConfigurationService,
                        new MqttServerDisconnectorImpl(new EventLog()), new Mqtt3ConnectEncoder(), new Mqtt3SubscribeEncoder(),
                        new Mqtt3UnsubscribeEncoder(), new PingreqEncoder()),
                new GlobalMQTTMessageCounter(new MetricsHolder(new MetricRegistry())));

        this.securityConfigurationService = securityConfigurationService;
    }

    public @NotNull SecurityConfigurationService getSecurityConfigurationService() {
        return securityConfigurationService;
    }
}
