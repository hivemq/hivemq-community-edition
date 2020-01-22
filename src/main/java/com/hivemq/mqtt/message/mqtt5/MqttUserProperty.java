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

package com.hivemq.mqtt.message.mqtt5;

import com.google.common.base.Preconditions;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.codec.encoder.mqtt5.MqttBinaryData;
import com.hivemq.codec.encoder.mqtt5.UnsignedDataTypes;
import com.hivemq.extension.sdk.api.packets.general.UserProperty;
import io.netty.buffer.ByteBuf;

import javax.annotation.concurrent.Immutable;
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
    public static MqttUserProperty of(@NotNull final String name, @NotNull final String value) {

        return new MqttUserProperty(name, value);
    }

    /**
     * Creates a MqttUserProperty from any class implementing the {@link UserProperty} interface.
     *
     * @param userProperty a {@link UserProperty} instance
     * @return the created MqttUserProperty
     */
    public static MqttUserProperty of(@NotNull final UserProperty userProperty) {
        return new MqttUserProperty(userProperty.getName(), userProperty.getValue());
    }

    /**
     * Validates and decodes a User Property from the given byte buffer at the current reader index.
     *
     * @param in           the byte buffer to decode from.
     * @param validateUTF8 true if should not characters must be validated
     * @return the decoded User Property or null if the name and/or value are not valid UTF-8 encoded Strings.
     */
    @Nullable
    public static MqttUserProperty decode(@NotNull final ByteBuf in, final boolean validateUTF8) {
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

    public MqttUserProperty(@NotNull final String name, @NotNull final String value) {

        Preconditions.checkArgument(name.getBytes(StandardCharsets.UTF_8).length <= UnsignedDataTypes.UNSIGNED_SHORT_MAX_VALUE,
                "A user property name must never exceed 65535 bytes");


        Preconditions.checkArgument(value.getBytes(StandardCharsets.UTF_8).length <= UnsignedDataTypes.UNSIGNED_SHORT_MAX_VALUE,
                "A user property value must never exceed 65535 bytes");


        this.name = name;
        this.value = value;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public String getValue() {
        return value;
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

}
