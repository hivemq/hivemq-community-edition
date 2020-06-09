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
import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import io.netty.buffer.ByteBuf;

/**
 * Collection of {@link MqttUserProperty User Properties}.
 *
 * @author Silvio Giebl
 * @author Lukas Brandl
 */
@Immutable
public class Mqtt5UserProperties {

    /**
     * Creates a collection of User Properties of the given User Properties.
     *
     * @param userProperties the User Properties.
     * @return the created collection of User Properties.
     */
    @NotNull
    public static Mqtt5UserProperties of(final @NotNull MqttUserProperty... userProperties) {
        Preconditions.checkNotNull(userProperties);
        return Mqtt5UserProperties.of(ImmutableList.copyOf(userProperties));
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
    public static @NotNull Mqtt5UserProperties of(final @NotNull ImmutableList<MqttUserProperty> userProperties) {
        return userProperties.isEmpty() ? NO_USER_PROPERTIES : new Mqtt5UserProperties(userProperties);
    }

    /**
     * Builds a collection of User Properties from the given builder.
     *
     * @param userPropertiesBuilder the builder for the User Properties.
     * @return the built collection of User Properties or {@link #NO_USER_PROPERTIES} if the builder is null.
     */
    public static @NotNull Mqtt5UserProperties build(
            final @Nullable ImmutableList.Builder<MqttUserProperty> userPropertiesBuilder) {

        return (userPropertiesBuilder == null) ? NO_USER_PROPERTIES : of(userPropertiesBuilder.build());
    }

    private final @NotNull ImmutableList<MqttUserProperty> userProperties;
    private int encodedLength = -1;

    private Mqtt5UserProperties(final @NotNull ImmutableList<MqttUserProperty> userProperties) {
        this.userProperties = userProperties;
    }

    public @NotNull ImmutableList<MqttUserProperty> asList() {
        return userProperties;
    }

    /**
     * Encodes this collection of User Properties to the given byte buffer at the current writer index.
     * <p>
     * This method does not check if name and value can not be encoded due to byte count restrictions. This check is
     * performed with the method {@link #encodedLength()} which is generally called before this method.
     *
     * @param out the byte buffer to encode to.
     */
    public void encode(final @NotNull ByteBuf out) {
        for (int i = 0; i < userProperties.size(); i++) {
            userProperties.get(i).encode(out);
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
        for (int i = 0; i < userProperties.size(); i++) {
            encodedLength += userProperties.get(i).encodedLength();
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

    @Override
    public @NotNull String toString() {
        return userProperties.toString();
    }
}
