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
import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.codec.encoder.mqtt5.MqttBinaryData;
import com.hivemq.extension.sdk.api.packets.general.UserProperty;
import com.hivemq.extensions.packets.general.InternalUserProperties;
import io.netty.buffer.ByteBuf;
import kotlin.text.Charsets;

import java.util.List;
import java.util.Optional;

/**
 * Collection of {@link MqttUserProperty User Properties}.
 *
 * @author Silvio Giebl
 * @author Lukas Brandl
 */
public class Mqtt5UserProperties {

    /**
     * Creates a collection of User Properties of the given User Properties.
     *
     * @param userProperties the User Properties.
     * @return the created collection of User Properties.
     */
    @NotNull
    public static Mqtt5UserProperties of(@NotNull final MqttUserProperty... userProperties) {
        Preconditions.checkNotNull(userProperties);

        final ImmutableList.Builder<MqttUserProperty> builder = ImmutableList.builder();
        for (final MqttUserProperty userProperty : userProperties) {
            builder.add(userProperty);
        }
        return Mqtt5UserProperties.of(builder.build());
    }

    @NotNull
    public static Mqtt5UserPropertiesBuilder builder() {
        return new Mqtt5UserPropertiesBuilder();
    }

    /**
     * Empty collection of User Properties.
     */
    public static final Mqtt5UserProperties NO_USER_PROPERTIES = new Mqtt5UserProperties(ImmutableList.of());

    /**
     * Creates a collection of User Properties from the given immutable list of User Properties.
     *
     * @param userProperties the immutable list of User Properties.
     * @return the created collection of User Properties or {@link #NO_USER_PROPERTIES} if the list is empty.
     */
    @NotNull
    public static Mqtt5UserProperties of(@NotNull final ImmutableList<MqttUserProperty> userProperties) {
        return userProperties.isEmpty() ? NO_USER_PROPERTIES : new Mqtt5UserProperties(userProperties);
    }

    /**
     * Builds a collection of User Properties from the given builder.
     *
     * @param userPropertiesBuilder the builder for the User Properties.
     * @return the built collection of User Properties or {@link #NO_USER_PROPERTIES} if the builder is null.
     */
    @NotNull
    public static Mqtt5UserProperties build(
            @Nullable final ImmutableList.Builder<MqttUserProperty> userPropertiesBuilder) {
        return (userPropertiesBuilder == null) ? NO_USER_PROPERTIES : of(userPropertiesBuilder.build());
    }

    private final ImmutableList<MqttUserProperty> userProperties;
    private int encodedLength = -1;

    private Mqtt5UserProperties(@NotNull final ImmutableList<MqttUserProperty> userProperties) {
        this.userProperties = userProperties;
    }

    @NotNull
    public ImmutableList<MqttUserProperty> asList() {
        return userProperties;
    }

    public int size() {
        return userProperties.size();
    }

    /**
     * @return a for the extension system suitable representation of this object.
     */
    @NotNull
    public InternalUserProperties getPluginUserProperties() {
        return new PluginUserProperties();
    }

    /**
     * Encodes this collection of User Properties to the given byte buffer at the current writer index.
     * <p>
     * This method does not check if name and value can not be encoded due to byte count restrictions. This check is
     * performed with the method {@link #encodedLength()} which is generally called before this method.
     *
     * @param out the byte buffer to encode to.
     */
    public void encode(@NotNull final ByteBuf out) {
        if (!userProperties.isEmpty()) {
            for (int i = 0; i < userProperties.size(); i++) {
                final MqttUserProperty userProperty = userProperties.get(i);
                out.writeByte(MessageProperties.USER_PROPERTY);
                MqttBinaryData.encode(userProperty.getName().getBytes(Charsets.UTF_8), out);
                MqttBinaryData.encode(userProperty.getValue().getBytes(Charsets.UTF_8), out);
            }
        }
    }

    /**
     * Calculates the byte count of this collection of User Properties according to the MQTT 5 specification.
     *
     * @return the encoded length of this collection of User Properties.
     */
    public int encodedLength() {
        if (encodedLength == -1) {
            encodedLength = calculateEncodedLength();
        }
        return encodedLength;
    }

    private int calculateEncodedLength() {
        int encodedLength = 0;
        if (!userProperties.isEmpty()) {
            for (int i = 0; i < userProperties.size(); i++) {
                final MqttUserProperty userProperty = userProperties.get(i);
                encodedLength += 1 + MqttBinaryData.encodedLength(userProperty.getName()) + MqttBinaryData.encodedLength(userProperty.getValue());
            }
        }
        return encodedLength;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Mqtt5UserProperties)) {
            return false;
        }
        final Mqtt5UserProperties that = (Mqtt5UserProperties) o;
        return userProperties.equals(that.userProperties);
    }

    @Override
    public int hashCode() {
        return userProperties.hashCode();
    }

    private class PluginUserProperties implements InternalUserProperties {

        @NotNull
        @Override
        public InternalUserProperties consolidate() {
            return this;
        }

        @Override
        public @NotNull ImmutableList<MqttUserProperty> asImmutableList() {
            return userProperties;
        }

        @NotNull
        @Override
        public Optional<String> getFirst(@NotNull final String key) {
            return userProperties.stream()
                    .filter(p -> key.equals(p.getName()))
                    .map(MqttUserProperty::getValue).findAny();
        }

        @NotNull
        @Override
        public List<String> getAllForName(@NotNull final String key) {
            return userProperties.stream()
                    .filter(p -> key.equals(p.getName()))
                    .map(MqttUserProperty::getValue)
                    .collect(ImmutableList.toImmutableList());
        }

        @NotNull
        @Override
        public List<UserProperty> asList() {
            return userProperties.stream().collect(ImmutableList.toImmutableList());
        }

        @Override
        public boolean isEmpty() {
            return userProperties.isEmpty();
        }
    }
}
