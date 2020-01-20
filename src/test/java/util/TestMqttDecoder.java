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

package util;

import com.hivemq.codec.decoder.MQTTMessageDecoder;
import com.hivemq.codec.decoder.MqttConnectDecoder;
import com.hivemq.codec.decoder.MqttDecoders;
import com.hivemq.codec.decoder.MqttPingreqDecoder;
import com.hivemq.codec.decoder.mqtt3.*;
import com.hivemq.codec.decoder.mqtt5.*;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.limitation.TopicAliasLimiterImpl;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.handler.connack.MqttConnackSendUtil;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.handler.disconnect.Mqtt3ServerDisconnector;
import com.hivemq.mqtt.handler.disconnect.Mqtt5ServerDisconnector;
import com.hivemq.mqtt.handler.disconnect.MqttDisconnectUtil;
import com.hivemq.util.ClientIds;

import static com.hivemq.mqtt.message.connect.Mqtt5CONNECT.SESSION_EXPIRY_MAX;
import static com.hivemq.mqtt.message.publish.PUBLISH.MESSAGE_EXPIRY_INTERVAL_MAX;

/**
 * @author Christoph Schäbel
 */
public class TestMqttDecoder {

    public static MQTTMessageDecoder create() {
        return create(true);
    }

    public static MQTTMessageDecoder create(final boolean strict) {
        final FullConfigurationService fullConfig = new TestConfigurationBootstrap().getFullConfigurationService();
        fullConfig.securityConfiguration().setValidateUTF8(true);
        fullConfig.mqttConfiguration().setMaxSessionExpiryInterval(SESSION_EXPIRY_MAX);
        fullConfig.mqttConfiguration().setMaxMessageExpiryInterval(MESSAGE_EXPIRY_INTERVAL_MAX);
        return create(strict, fullConfig);
    }

    public static MQTTMessageDecoder create(final FullConfigurationService fullMqttConfigurationService) {
        return create(true, fullMqttConfigurationService);
    }

    public static MQTTMessageDecoder create(final boolean strict, final FullConfigurationService fullConfigurationService) {

        final EventLog eventLog = new EventLog();
        final MqttDisconnectUtil mqttDisconnectUtil = new MqttDisconnectUtil(eventLog);
        final MqttConnackSendUtil mqttConnackSendUtil = new MqttConnackSendUtil(eventLog);
        final Mqtt5ServerDisconnector mqtt5ServerDisconnector = new Mqtt5ServerDisconnector(mqttDisconnectUtil);
        final Mqtt3ServerDisconnector mqtt3ServerDisconnector = new Mqtt3ServerDisconnector(mqttDisconnectUtil);
        final MqttConnacker mqttConnacker = new MqttConnacker(mqttConnackSendUtil);
        final HivemqId hiveMQId = new HivemqId();

        final MqttConnectDecoder mqttConnectDecoder = new MqttConnectDecoder(mqtt5ServerDisconnector,
                mqtt3ServerDisconnector,
                mqttConnacker,
                eventLog,
                fullConfigurationService,
                hiveMQId,
                new ClientIds(hiveMQId));

        return new MQTTMessageDecoder(
                mqttConnectDecoder,
                strict,
                fullConfigurationService.mqttConfiguration(),
                eventLog,
                new MqttDecoders(new Mqtt3ConnackDecoder(eventLog),
                        new Mqtt3PublishDecoder(hiveMQId, mqtt3ServerDisconnector, fullConfigurationService),
                        new Mqtt3PubackDecoder(eventLog),
                        new Mqtt3PubrecDecoder(eventLog),
                        new Mqtt3PubcompDecoder(eventLog),
                        new Mqtt3PubrelDecoder(eventLog),
                        new Mqtt3DisconnectDecoder(eventLog),
                        new Mqtt3SubscribeDecoder(eventLog),
                        new Mqtt3UnsubscribeDecoder(eventLog),
                        new Mqtt3SubackDecoder(eventLog),
                        new Mqtt3UnsubackDecoder(eventLog),
                        new MqttPingreqDecoder(eventLog),
                        new Mqtt5PublishDecoder(mqtt5ServerDisconnector, hiveMQId, fullConfigurationService, new TopicAliasLimiterImpl()),
                        new Mqtt5DisconnectDecoder(mqtt5ServerDisconnector, fullConfigurationService),
                        new Mqtt5SubscribeDecoder(mqtt5ServerDisconnector, fullConfigurationService),
                        new Mqtt5PubackDecoder(mqtt5ServerDisconnector, fullConfigurationService),
                        new Mqtt5PubrecDecoder(mqtt5ServerDisconnector, fullConfigurationService),
                        new Mqtt5PubrelDecoder(mqtt5ServerDisconnector, fullConfigurationService),
                        new Mqtt5PubcompDecoder(mqtt5ServerDisconnector, fullConfigurationService),
                        new Mqtt5AuthDecoder(mqtt5ServerDisconnector, fullConfigurationService),
                        new Mqtt5UnsubscribeDecoder(mqtt5ServerDisconnector, fullConfigurationService)));
    }

}
