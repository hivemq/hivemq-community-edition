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
package com.hivemq.mqtt.handler.connect;

import com.hivemq.mqtt.message.connack.CONNACK;
import com.hivemq.mqtt.message.connack.Mqtt3ConnAckReturnCode;

/**
 * @author Christoph Schäbel
 */
public class ConnackMessages {

    public static final CONNACK ACCEPTED_MSG_NO_SESS = new CONNACK(Mqtt3ConnAckReturnCode.ACCEPTED, false);

    public static final CONNACK ACCEPTED_MSG_SESS_PRESENT = new CONNACK(Mqtt3ConnAckReturnCode.ACCEPTED, true);

    public static final CONNACK REFUSED_NOT_AUTHORIZED = new CONNACK(Mqtt3ConnAckReturnCode.REFUSED_NOT_AUTHORIZED);
}
