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
package util;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.codec.decoder.MQTTMessageDecoder;
import com.hivemq.codec.decoder.MqttConnectDecoder;
import com.hivemq.codec.decoder.MqttDecoders;
import com.hivemq.codec.decoder.MqttPingreqDecoder;
import com.hivemq.codec.decoder.mqtt3.Mqtt3DisconnectDecoder;
import com.hivemq.codec.decoder.mqtt3.Mqtt3PubackDecoder;
import com.hivemq.codec.decoder.mqtt3.Mqtt3PubcompDecoder;
import com.hivemq.codec.decoder.mqtt3.Mqtt3PublishDecoder;
import com.hivemq.codec.decoder.mqtt3.Mqtt3PubrecDecoder;
import com.hivemq.codec.decoder.mqtt3.Mqtt3PubrelDecoder;
import com.hivemq.codec.decoder.mqtt3.Mqtt3SubscribeDecoder;
import com.hivemq.codec.decoder.mqtt3.Mqtt3UnsubscribeDecoder;
import com.hivemq.codec.decoder.mqtt5.Mqtt5AuthDecoder;
import com.hivemq.codec.decoder.mqtt5.Mqtt5DisconnectDecoder;
import com.hivemq.codec.decoder.mqtt5.Mqtt5PubackDecoder;
import com.hivemq.codec.decoder.mqtt5.Mqtt5PubcompDecoder;
import com.hivemq.codec.decoder.mqtt5.Mqtt5PublishDecoder;
import com.hivemq.codec.decoder.mqtt5.Mqtt5PubrecDecoder;
import com.hivemq.codec.decoder.mqtt5.Mqtt5PubrelDecoder;
import com.hivemq.codec.decoder.mqtt5.Mqtt5SubscribeDecoder;
import com.hivemq.codec.decoder.mqtt5.Mqtt5UnsubscribeDecoder;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.limitation.TopicAliasLimiterImpl;
import com.hivemq.logging.EventLog;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.metrics.handler.GlobalMQTTMessageCounter;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.handler.connack.MqttConnackerImpl;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnectorImpl;
import com.hivemq.util.ClientIds;

import static com.hivemq.mqtt.message.connect.Mqtt5CONNECT.SESSION_EXPIRY_MAX;
import static com.hivemq.mqtt.message.publish.PUBLISH.MESSAGE_EXPIRY_INTERVAL_MAX;

/**
 * @author Christoph Schäbel
 */
public class TestMqttDecoder {

    public static MQTTMessageDecoder create() {
        final FullConfigurationService fullConfig = new TestConfigurationBootstrap().getFullConfigurationService();
        fullConfig.securityConfiguration().setValidateUTF8(true);
        fullConfig.mqttConfiguration().setMaxSessionExpiryInterval(SESSION_EXPIRY_MAX);
        fullConfig.mqttConfiguration().setMaxMessageExpiryInterval(MESSAGE_EXPIRY_INTERVAL_MAX);
        return create(fullConfig);
    }

    public static MQTTMessageDecoder create(final @NotNull FullConfigurationService fullConfigurationService) {

        final EventLog eventLog = new EventLog();
        final HivemqId hiveMQId = new HivemqId();
        final MqttServerDisconnector disconnector = new MqttServerDisconnectorImpl(eventLog);
        final MqttConnacker mqttConnacker = new MqttConnackerImpl(eventLog);
        final MetricsHolder metricsHolder = new MetricsHolder(new MetricRegistry());

        final MqttConnectDecoder mqttConnectDecoder =
                new MqttConnectDecoder(mqttConnacker, fullConfigurationService, hiveMQId, new ClientIds(hiveMQId));

        return new MQTTMessageDecoder(mqttConnectDecoder,
                fullConfigurationService.mqttConfiguration(),
                new MqttDecoders(new Mqtt3PublishDecoder(hiveMQId, disconnector, fullConfigurationService),
                        new Mqtt3PubackDecoder(disconnector, fullConfigurationService),
                        new Mqtt3PubrecDecoder(disconnector, fullConfigurationService),
                        new Mqtt3PubcompDecoder(disconnector, fullConfigurationService),
                        new Mqtt3PubrelDecoder(disconnector, fullConfigurationService),
                        new Mqtt3DisconnectDecoder(disconnector, fullConfigurationService),
                        new Mqtt3SubscribeDecoder(disconnector, fullConfigurationService),
                        new Mqtt3UnsubscribeDecoder(disconnector, fullConfigurationService),
                        new MqttPingreqDecoder(disconnector),
                        new Mqtt5PublishDecoder(disconnector,
                                hiveMQId,
                                fullConfigurationService,
                                new TopicAliasLimiterImpl()),
                        new Mqtt5DisconnectDecoder(disconnector, fullConfigurationService),
                        new Mqtt5SubscribeDecoder(disconnector, fullConfigurationService),
                        new Mqtt5PubackDecoder(disconnector, fullConfigurationService),
                        new Mqtt5PubrecDecoder(disconnector, fullConfigurationService),
                        new Mqtt5PubrelDecoder(disconnector, fullConfigurationService),
                        new Mqtt5PubcompDecoder(disconnector, fullConfigurationService),
                        new Mqtt5AuthDecoder(disconnector, fullConfigurationService),
                        new Mqtt5UnsubscribeDecoder(disconnector, fullConfigurationService)),
                disconnector,
                new GlobalMQTTMessageCounter(metricsHolder));
    }
}
