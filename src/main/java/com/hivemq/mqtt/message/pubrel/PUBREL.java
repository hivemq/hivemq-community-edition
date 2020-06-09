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
package com.hivemq.mqtt.message.pubrel;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extensions.packets.pubrel.PubrelPacketImpl;
import com.hivemq.mqtt.message.MessageType;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttMessageWithUserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.reason.Mqtt5PubRelReasonCode;
import com.hivemq.util.ObjectMemoryEstimation;

/**
 * The MQTT PUBREL message
 *
 * @author Dominik Obermaier
 * @author Waldemar Ruck
 * @since 1.4
 */
public class PUBREL extends MqttMessageWithUserProperties.MqttMessageWithIdAndReasonCode<Mqtt5PubRelReasonCode>
        implements Mqtt3PUBREL, Mqtt5PUBREL {

    private static final int SIZE_NOT_CALCULATED = -1;

    @Nullable
    private Long publishTimestamp;
    @Nullable
    private Long expiryInterval;

    private int sizeInMemory = SIZE_NOT_CALCULATED;

    //MQTT 3
    public PUBREL(final int packetIdentifier) {
        super(packetIdentifier, Mqtt5PubRelReasonCode.SUCCESS, null, Mqtt5UserProperties.NO_USER_PROPERTIES);
    }

    public PUBREL(final int packetIdentifier, final Long publishTimestamp, final Long expiryInterval) {
        super(packetIdentifier, Mqtt5PubRelReasonCode.SUCCESS, null, Mqtt5UserProperties.NO_USER_PROPERTIES);
        this.publishTimestamp = publishTimestamp;
        this.expiryInterval = expiryInterval;
    }

    //MQTT 5
    public PUBREL(
            final int packetIdentifier,
            final @NotNull Mqtt5PubRelReasonCode reasonCode,
            final @Nullable String reasonString,
            final @NotNull Mqtt5UserProperties userProperties) {

        super(packetIdentifier, reasonCode, reasonString, userProperties);
    }

    public PUBREL(
            final int packetIdentifier,
            final @NotNull Mqtt5PubRelReasonCode reasonCode,
            final @Nullable String reasonString,
            final @NotNull Mqtt5UserProperties userProperties,
            final @Nullable Long publishTimestamp,
            final @Nullable Long expiryInterval) {

        super(packetIdentifier, reasonCode, reasonString, userProperties);
        this.publishTimestamp = publishTimestamp;
        this.expiryInterval = expiryInterval;
    }

    @Override
    public @NotNull MessageType getType() {
        return MessageType.PUBREL;
    }

    public @Nullable Long getExpiryInterval() {
        return expiryInterval;
    }

    public void setExpiryInterval(@Nullable final Long expiryInterval) {
        this.expiryInterval = expiryInterval;
    }

    public @Nullable Long getPublishTimestamp() {
        return publishTimestamp;
    }

    public void setPublishTimestamp(@Nullable final Long publishTimestamp) {
        this.publishTimestamp = publishTimestamp;
    }

    public static @NotNull PUBREL from(final @NotNull PubrelPacketImpl packet) {
        return new PUBREL(
                packet.getPacketIdentifier(),
                Mqtt5PubRelReasonCode.from(packet.getReasonCode()),
                packet.getReasonString().orElse(null),
                Mqtt5UserProperties.of(packet.getUserProperties().asInternalList()));
    }

    /**
     * @return an approximately size of the pubrel object in bytes.
     */
    public int getEstimatedSizeInMemory() {
        if (sizeInMemory != SIZE_NOT_CALCULATED) {
            return sizeInMemory;
        }
        int size = 0;
        size += ObjectMemoryEstimation.objectShellSize();
        size += ObjectMemoryEstimation.intSize(); // sizeInMemory
        size += ObjectMemoryEstimation.intSize(); // packet id
        size += ObjectMemoryEstimation.enumSize(); // reason code
        size += ObjectMemoryEstimation.stringSize(getReasonString()); // reason code
        size += ObjectMemoryEstimation.longWrapperSize(); //publish timestamp
        size += ObjectMemoryEstimation.longWrapperSize(); //expiry interval

        size += 24; //User Properties Overhead
        for (final MqttUserProperty userProperty : getUserProperties().asList()) {
            size += 24; //UserProperty Object Overhead
            size += ObjectMemoryEstimation.stringSize(userProperty.getName());
            size += ObjectMemoryEstimation.stringSize(userProperty.getValue());
        }

        sizeInMemory = size;
        return sizeInMemory;
    }
}
