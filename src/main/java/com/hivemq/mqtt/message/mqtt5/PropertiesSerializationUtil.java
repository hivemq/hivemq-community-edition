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

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.persistence.local.xodus.XodusUtils;
import com.hivemq.util.Bytes;

/**
 * @author Lukas Brandl
 */
public class PropertiesSerializationUtil {

    public static int encodedSize(final Mqtt5UserProperties properties) {
        int size = Integer.BYTES; // List size integer
        for (final MqttUserProperty property : properties.asList()) {
            size += XodusUtils.shortLengthStringSize(property.getName());
            size += XodusUtils.shortLengthStringSize(property.getValue());
        }
        return size;
    }

    public static int write(@NotNull final Mqtt5UserProperties properties, @NotNull final byte[] bytes, int offset) {
        Bytes.copyIntToByteArray(properties.asList().size(), bytes, offset);
        offset += Integer.BYTES;
        for (final MqttUserProperty property : properties.asList()) {
            offset = XodusUtils.serializeShortLengthString(property.getName(), bytes, offset);
            offset = XodusUtils.serializeShortLengthString(property.getValue(), bytes, offset);
        }
        return offset;
    }

    @NotNull
    public static Mqtt5UserProperties read(@NotNull final byte[] bytes, int offset) {
        final int size = Bytes.readInt(bytes, offset);
        offset += Integer.BYTES;
        final ImmutableList.Builder<MqttUserProperty> builder = ImmutableList.builderWithExpectedSize(size);

        for (int i = 0; i < size; i++) {
            final int nameLength = Bytes.readUnsignedShort(bytes, offset);
            offset += Short.BYTES;
            final String name = new String(bytes, offset, nameLength, Charsets.UTF_8);
            offset += nameLength;

            final int valueLength = Bytes.readUnsignedShort(bytes, offset);
            offset += Short.BYTES;
            final String value = new String(bytes, offset, valueLength, Charsets.UTF_8);
            offset += valueLength;

            builder.add(new MqttUserProperty(name, value));
        }
        return Mqtt5UserProperties.of(builder.build());
    }
}
