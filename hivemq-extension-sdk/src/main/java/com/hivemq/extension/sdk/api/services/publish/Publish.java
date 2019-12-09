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
package com.hivemq.extension.sdk.api.services.publish;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extension.sdk.api.packets.publish.PayloadFormatIndicator;
import com.hivemq.extension.sdk.api.services.builder.Builders;
import com.hivemq.extension.sdk.api.services.builder.PublishBuilder;

import java.nio.ByteBuffer;
import java.util.Optional;

/**
 * Represents a PUBLISH.
 * <p>
 * Contains all values of an MQTT 5 PUBLISH, but will also used to represent MQTT 3 publishes.
 *
 * @author Christoph Schäbel
 * @since 4.0.0
 */
@DoNotImplement
public interface Publish {

    /**
     * @return A new {@link PublishBuilder} to create a {@link Publish}.
     * @since 4.0.0
     */
    static PublishBuilder builder() {
        return Builders.publish();
    }

    /**
     * The quality of service level of the publish.
     *
     * @return The qos.
     * @since 4.0.0
     */
    @NotNull Qos getQos();

    /**
     * If <b>true</b> this message is a retained message, for <b>false</b> this is just a normal publish.
     *
     * @return The retain flag.
     * @since 4.0.0
     */
    boolean getRetain();

    /**
     * The topic filter the message is published to.
     *
     * @return The topic.
     * @since 4.0.0
     */
    @NotNull String getTopic();

    /**
     * If this property is present, this is the payload format indicator.
     *
     * @return An {@link Optional} that contains the payload format indicator if present.
     * @since 4.0.0
     */
    @NotNull Optional<PayloadFormatIndicator> getPayloadFormatIndicator();

    /**
     * If this property is present, this is the message expiry interval.
     *
     * @return An {@link Optional} that contains the message expiry interval if present.
     * @since 4.0.0
     */
    @NotNull Optional<Long> getMessageExpiryInterval();

    /**
     * If this property is present, this is the response topic.
     *
     * @return An {@link Optional} that contains the response topic if present.
     * @since 4.0.0
     */
    @NotNull Optional<String> getResponseTopic();

    /**
     * If this property is present, this is the correlation data.
     *
     * @return An {@link Optional} that contains the correlation data if present.
     * @since 4.0.0
     */
    @NotNull Optional<@Immutable ByteBuffer> getCorrelationData();

    /**
     * If this property is present, this is the content type.
     *
     * @return An {@link Optional} that contains the content type if present.
     * @since 4.0.0
     */
    @NotNull Optional<String> getContentType();

    /**
     * If this property is present, this is the payload.
     *
     * @return An {@link Optional} that contains the payload if present.
     * @since 4.0.0
     */
    @NotNull Optional<@Immutable ByteBuffer> getPayload();

    /**
     * The {@link UserProperties} of the PUBLISH.
     *
     * @return The user properties.
     * @since 4.0.0
     */
    @Immutable
    @NotNull UserProperties getUserProperties();
}
