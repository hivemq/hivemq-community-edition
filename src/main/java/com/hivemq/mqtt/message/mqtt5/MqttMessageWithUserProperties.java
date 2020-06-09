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
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.codec.encoder.mqtt5.UnsignedDataTypes;
import com.hivemq.mqtt.message.MessageWithID;
import com.hivemq.mqtt.message.reason.Mqtt5ReasonCode;

import java.nio.charset.StandardCharsets;

/**
 * Base class for MQTT messages with optional User Properties.
 */
public abstract class MqttMessageWithUserProperties extends MessageWithID {

    @NotNull
    private Mqtt5UserProperties userProperties;

    public MqttMessageWithUserProperties(@NotNull final Mqtt5UserProperties userProperties) {
        Preconditions.checkNotNull(userProperties, "User properties may never be null");
        this.userProperties = userProperties;
    }

    protected int bufferSize = -1;
    protected int remainingLength = -1;
    protected int propertyLength = -1;
    protected int omittedProperties = -1;

    @NotNull
    public Mqtt5UserProperties getUserProperties() {
        return userProperties;
    }

    public void setUserProperties(final @NotNull Mqtt5UserProperties userProperties) {
        this.userProperties = userProperties;
    }


    @Override
    public void setEncodedLength(final int bufferSize) {
        this.bufferSize = bufferSize;
    }

    @Override
    public int getEncodedLength() {
        return bufferSize;
    }

    @Override
    public int getRemainingLength() {
        return remainingLength;
    }

    @Override
    public void setRemainingLength(final int remainingLength) {
        this.remainingLength = remainingLength;
    }

    @Override
    public int getPropertyLength() {
        return propertyLength;
    }

    @Override
    public void setPropertyLength(final int propertyLength) {
        this.propertyLength = propertyLength;
    }

    @Override
    public int getOmittedProperties() {
        return omittedProperties;
    }

    @Override
    public void setOmittedProperties(final int omittedProperties) {
        this.omittedProperties = omittedProperties;
    }

    /**
     * Base class for MQTT messages with an optional Reason String and optional User Properties.
     */
    public abstract static class MqttMessageWithReasonString extends MqttMessageWithUserProperties {

        private final String reasonString;

        protected MqttMessageWithReasonString(@Nullable final String reasonString, @NotNull final Mqtt5UserProperties userProperties) {
            super(userProperties);
            if (reasonString != null) {
                Preconditions.checkArgument(reasonString.getBytes(StandardCharsets.UTF_8).length <= UnsignedDataTypes.UNSIGNED_SHORT_MAX_VALUE,
                        "A reason string must never exceed 65535 bytes");

            }
            this.reasonString = reasonString;
        }

        @Nullable
        public String getReasonString() {
            return reasonString;
        }
    }


    /**
     * Base class for MQTT messages with a Reason Code, an optional Reason String and optional User Properties.
     *
     * @param <R> the type of the Reason Code.
     */
    public abstract static class MqttMessageWithReasonCode<R extends Mqtt5ReasonCode>
            extends MqttMessageWithReasonString {

        private final @NotNull R reasonCode;

        protected MqttMessageWithReasonCode(@NotNull final R reasonCode, @Nullable final String reasonString, @NotNull final Mqtt5UserProperties userProperties) {

            super(reasonString, userProperties);
            Preconditions.checkNotNull(reasonCode, "A reason code may never be null");
            this.reasonCode = reasonCode;
        }

        @NotNull
        public R getReasonCode() {
            return reasonCode;
        }

    }

    /**
     * Base class for MQTT messages with a Packet Identifier, a Reason Code, an optional Reason String and optional User
     * Properties.
     *
     * @param <R> the type of the Reason Code.
     */
    public abstract static class MqttMessageWithIdAndReasonCode<R extends Mqtt5ReasonCode>
            extends MqttMessageWithReasonCode<R> {


        protected MqttMessageWithIdAndReasonCode(final int packetIdentifier, @NotNull final R reasonCode,
                                                 @Nullable final String reasonString, @NotNull final Mqtt5UserProperties userProperties) {

            super(reasonCode, reasonString, userProperties);
            super.packetIdentifier = packetIdentifier;
        }

        public int getPacketIdentifier() {
            return packetIdentifier;
        }

    }


    /**
     * Base class for MQTT messages with a Packet Identifier, Reason Codes, an optional Reason String and optional User
     * Properties.
     *
     * @param <R> the type of the Reason Codes.
     */
    public abstract static class MqttMessageWithIdAndReasonCodes<R extends Mqtt5ReasonCode>
            extends MqttMessageWithReasonString {

        private final ImmutableList<R> reasonCodes;

        protected MqttMessageWithIdAndReasonCodes(final int packetIdentifier, @NotNull final ImmutableList<R> reasonCodes,
                                                  @Nullable final String reasonString, @NotNull final Mqtt5UserProperties userProperties) {

            super(reasonString, userProperties);
            super.packetIdentifier = packetIdentifier;
            this.reasonCodes = reasonCodes;
        }

        public int getPacketIdentifier() {
            return packetIdentifier;
        }

        @NotNull
        public ImmutableList<R> getReasonCodes() {
            return reasonCodes;
        }

    }

}
