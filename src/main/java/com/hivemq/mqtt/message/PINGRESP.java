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
package com.hivemq.mqtt.message;

import com.hivemq.extension.sdk.api.annotations.NotNull;

/**
 * The MQTT PINGRESP message
 *
 * @author Dominik Obermaier
 * @since 1.4
 */
public class PINGRESP implements Message {

    public static final PINGRESP INSTANCE = new PINGRESP();

    @NotNull
    @Override
    public MessageType getType() {
        return MessageType.PINGRESP;
    }

    @Override
    public void setEncodedLength(final int bufferSize) {
        //noop since ping resp is always 2 bytes
    }

    @Override
    public int getEncodedLength() {
        return 2;
    }

    @Override
    public void setRemainingLength(final int length) {
        //noop
    }

    @Override
    public int getRemainingLength() {
        return 0;
    }

    @Override
    public void setPropertyLength(final int length) {
        //noop
    }

    @Override
    public int getPropertyLength() {
        return 0;
    }

    @Override
    public void setOmittedProperties(final int omittedProperties) {
        //noop
    }

    @Override
    public int getOmittedProperties() {
        return 0;
    }
}
