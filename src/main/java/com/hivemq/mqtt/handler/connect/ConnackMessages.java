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

    public static final CONNACK REFUSED_UNACCEPTABLE_PROTOCOL_VERSION = new CONNACK(Mqtt3ConnAckReturnCode.REFUSED_UNACCEPTABLE_PROTOCOL_VERSION);
    public static final CONNACK REFUSED_IDENTIFIER_REJECTED = new CONNACK(Mqtt3ConnAckReturnCode.REFUSED_IDENTIFIER_REJECTED);
    public static final CONNACK REFUSED_SERVER_UNAVAILABLE = new CONNACK(Mqtt3ConnAckReturnCode.REFUSED_SERVER_UNAVAILABLE);
    public static final CONNACK REFUSED_BAD_USERNAME_OR_PASSWORD = new CONNACK(Mqtt3ConnAckReturnCode.REFUSED_BAD_USERNAME_OR_PASSWORD);
    public static final CONNACK REFUSED_NOT_AUTHORIZED = new CONNACK(Mqtt3ConnAckReturnCode.REFUSED_NOT_AUTHORIZED);

    public static CONNACK getMessageForRefusedCode(final Mqtt3ConnAckReturnCode returnCode) {

        final CONNACK connack;

        switch (returnCode) {
            case REFUSED_UNACCEPTABLE_PROTOCOL_VERSION:
                connack = ConnackMessages.REFUSED_UNACCEPTABLE_PROTOCOL_VERSION;
                break;

            case REFUSED_IDENTIFIER_REJECTED:
                connack = ConnackMessages.REFUSED_IDENTIFIER_REJECTED;
                break;

            case REFUSED_SERVER_UNAVAILABLE:
                connack = ConnackMessages.REFUSED_SERVER_UNAVAILABLE;
                break;

            case REFUSED_BAD_USERNAME_OR_PASSWORD:
                connack = ConnackMessages.REFUSED_BAD_USERNAME_OR_PASSWORD;
                break;

            case REFUSED_NOT_AUTHORIZED:
                connack = ConnackMessages.REFUSED_NOT_AUTHORIZED;
                break;

            default:
                throw new IllegalArgumentException("Unknown return code");

        }

        return connack;
    }

}
