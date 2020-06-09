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
package com.hivemq.mqtt.message.mqtt5;

import com.google.common.base.Preconditions;
import com.hivemq.codec.encoder.mqtt5.MqttBinaryData;
import com.hivemq.codec.encoder.mqtt5.UnsignedDataTypes;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.general.UserProperty;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

/**
 * @author Silvio Giebl
 * @author Lukas Brandl
 */
@Immutable
public class MqttUserProperty implements UserProperty {

    /**
     * Creates an User Property of the given name and value.
     *
     * @param name  the name of the User Property.
     * @param value the value of the User Property.
     * @return the created User Property.
     */
    public static MqttUserProperty of(final @NotNull String name, final @NotNull String value) {
        return new MqttUserProperty(name, value);
    }

    /**
     * Validates and decodes a User Property from the given byte buffer at the current reader index.
     *
     * @param in           the byte buffer to decode from.
     * @param validateUTF8 true if should not characters must be validated
     * @return the decoded User Property or null if the name and/or value are not valid UTF-8 encoded Strings.
     */
    public static @Nullable MqttUserProperty decode(final @NotNull ByteBuf in, final boolean validateUTF8) {
        final String name = MqttBinaryData.decodeString(in, validateUTF8);
        if (name == null) {
            return null;
        }
        final String value = MqttBinaryData.decodeString(in, validateUTF8);
        if (value == null) {
            return null;
        }
        return new MqttUserProperty(name, value);
    }

    private final @NotNull String name;
    private final @NotNull String value;

    public MqttUserProperty(final @NotNull String name, final @NotNull String value) {

        Preconditions.checkArgument(
                name.getBytes(StandardCharsets.UTF_8).length <= UnsignedDataTypes.UNSIGNED_SHORT_MAX_VALUE,
                "A user property name must never exceed 65535 bytes");

        Preconditions.checkArgument(
                value.getBytes(StandardCharsets.UTF_8).length <= UnsignedDataTypes.UNSIGNED_SHORT_MAX_VALUE,
                "A user property value must never exceed 65535 bytes");

        this.name = name;
        this.value = value;
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

    @Override
    public @NotNull String getValue() {
        return value;
    }

    void encode(final @NotNull ByteBuf out) {
        out.writeByte(MessageProperties.USER_PROPERTY);
        MqttBinaryData.encode(name, out);
        MqttBinaryData.encode(value, out);
    }

    int encodedLength() {
        return 1 + MqttBinaryData.encodedLength(name) + MqttBinaryData.encodedLength(value);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MqttUserProperty)) {
            return false;
        }
        final MqttUserProperty that = (MqttUserProperty) o;
        return name.equals(that.name) && value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return 31 * name.hashCode() + value.hashCode();
    }

    @Override
    public @NotNull String toString() {
        return "(" + name + ", " + value + ")";
    }
}
