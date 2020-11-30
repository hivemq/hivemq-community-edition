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
package com.hivemq.migration.persistence.legacy.serializer;

import com.hivemq.codec.encoder.mqtt5.Mqtt5PayloadFormatIndicator;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.PropertiesSerializationUtil;
import com.hivemq.persistence.RetainedMessage;
import com.hivemq.util.Bytes;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author Florian Limpöck
 */
public class RetainedMessageDeserializer_4_4 {

    @NotNull
    public static byte[] serializeKey(final @NotNull String topic) {
        checkNotNull(topic, "Topic must not be null");
        return topic.getBytes(UTF_8);
    }


    @NotNull
    public static byte[] serializeValue(final RetainedMessage retainedMessage) {

        final byte[] responseTopic = retainedMessage.getResponseTopic() == null ? null : retainedMessage.getResponseTopic().getBytes(UTF_8);
        final byte[] contentType = retainedMessage.getContentType() == null ? null : retainedMessage.getContentType().getBytes(UTF_8);
        final byte[] correlationData = retainedMessage.getCorrelationData();
        final int responseTopicLength = responseTopic != null ? responseTopic.length : 0;
        final int contentTypeLength = contentType != null ? contentType.length : 0;
        final int correlationDataLength = correlationData != null ? correlationData.length : 0;
        final int payloadFormatIndicator = retainedMessage.getPayloadFormatIndicator() != null ? retainedMessage.getPayloadFormatIndicator().getCode() : -1;

        int cursor = 0;
        final byte[] bytes = new byte[25 + PropertiesSerializationUtil.encodedSize(retainedMessage.getUserProperties()) + responseTopicLength + 4 + contentTypeLength + 4 + correlationDataLength + 4 + 1];

        bytes[cursor] = ((byte) retainedMessage.getQos().getQosNumber());
        cursor += 1;

        Bytes.copyLongToByteArray(retainedMessage.getTimestamp(), bytes, cursor);
        cursor += 8;

        Bytes.copyLongToByteArray(retainedMessage.getPublishId(), bytes, cursor);
        cursor += 8;

        Bytes.copyLongToByteArray(retainedMessage.getMessageExpiryInterval(), bytes, cursor);
        cursor += 8;

        Bytes.copyIntToByteArray(responseTopicLength, bytes, cursor);
        cursor += 4;

        if (responseTopicLength != 0) {
            System.arraycopy(responseTopic, 0, bytes, cursor, responseTopic.length);
            cursor += responseTopicLength;
        }

        Bytes.copyIntToByteArray(contentTypeLength, bytes, cursor);
        cursor += 4;

        if (contentTypeLength != 0) {
            System.arraycopy(contentType, 0, bytes, cursor, contentType.length);
            cursor += contentTypeLength;
        }

        Bytes.copyIntToByteArray(correlationDataLength, bytes, cursor);
        cursor += 4;

        if (correlationDataLength != 0) {
            System.arraycopy(correlationData, 0, bytes, cursor, correlationData.length);
            cursor += correlationDataLength;
        }

        bytes[cursor] = (byte) payloadFormatIndicator;
        cursor += 1;

        PropertiesSerializationUtil.write(retainedMessage.getUserProperties(), bytes, cursor);

        return bytes;
    }

    @NotNull
    public static String deserializeKey(final @NotNull byte[] serialized) {
        checkNotNull(serialized, "Byte array must not be null");
        return new String(serialized, 0, serialized.length, UTF_8);
    }


    @NotNull
    public static RetainedMessage deserializeValue(@NotNull final byte @NotNull [] serialized) {
        checkNotNull(serialized, "Byte array must not be null");

        final QoS qoS = QoS.valueOf(serialized[0] & 0b0000_0011);

        int cursor = 1;
        final long timestamp = Bytes.readLong(serialized, cursor);
        cursor += 8;

        final long publishId = Bytes.readLong(serialized, cursor);
        cursor += 8;

        final long ttl = Bytes.readLong(serialized, cursor);
        cursor += 8;

        final int responseTopicLength = Bytes.readInt(serialized, cursor);
        cursor += 4;
        final String responseTopic;
        if (responseTopicLength == 0) {
            responseTopic = null;
        } else {
            responseTopic = new String(serialized, cursor, responseTopicLength, UTF_8);
            cursor += responseTopicLength;
        }

        final int contentTypeLength = Bytes.readInt(serialized, cursor);
        cursor += 4;
        final String contentType;
        if (contentTypeLength == 0) {
            contentType = null;
        } else {
            contentType = new String(serialized, cursor, contentTypeLength, UTF_8);
            cursor += contentTypeLength;
        }

        final int correlationDataLength = Bytes.readInt(serialized, cursor);
        cursor += 4;
        final byte[] correlationData;
        if (correlationDataLength == 0) {
            correlationData = null;
        } else {
            correlationData = new byte[correlationDataLength];
            System.arraycopy(serialized, cursor, correlationData, 0, correlationDataLength);
            cursor += correlationDataLength;
        }

        final Mqtt5PayloadFormatIndicator payloadFormatIndicator = serialized[cursor] != -1 ? Mqtt5PayloadFormatIndicator.fromCode(serialized[cursor]) : null;
        cursor += 1;

        final Mqtt5UserProperties properties = PropertiesSerializationUtil.read(serialized, cursor);

        return new RetainedMessage(null, qoS, publishId, ttl, properties, responseTopic, contentType, correlationData, payloadFormatIndicator, timestamp);
    }
}