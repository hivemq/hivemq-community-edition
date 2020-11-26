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
package com.hivemq.persistence.local.xodus.clientsession;

import com.hivemq.codec.encoder.mqtt5.Mqtt5PayloadFormatIndicator;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.connect.MqttWillPublish;
import com.hivemq.mqtt.message.mqtt5.PropertiesSerializationUtil;
import com.hivemq.persistence.clientsession.ClientSession;
import com.hivemq.persistence.clientsession.ClientSessionWill;
import com.hivemq.util.Bytes;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Key: clientid
 * <p>
 * Value:
 * <p>
 * 8 byte timestamp | 1 byte flags (connected, persistent) |
 * <p>
 * flags: 0 <- connected 1 <- persistent 2 3 4 5 6 7
 *
 * @author Christoph Schäbel
 */
public class ClientSessionPersistenceSerializer {

    private static final int NO_WILL_MARKER = -1;

    private static final byte CLIENT_CONNECTED_BIT = 0;
    private static final byte QUEUE_SIZE_PRESENT_BIT = 1;

    public byte[] serializeKey(final String clientId) {
        return clientId.getBytes(UTF_8);
    }

    public String deserializeKey(final byte[] bytes) {
        return new String(bytes, 0, bytes.length, UTF_8);
    }

    public byte[] serializeValue(final ClientSession clientSession, final long timestamp) {

        final ClientSessionWill willPublish = clientSession.getWillPublish();
        int willLength = 1; // QoS/NO_WILL_MARKER
        byte[] topic = null;
        int topicLength = 0;
        byte[] responseTopic = null;
        int responseTopicLength = 0;
        byte[] hivemqId = null;
        int hivemqIdLength = 0;
        byte[] correlationData = null;
        int correlationDataLength = 0;
        byte payloadFormatIndicator = -1;
        byte[] contentType = null;
        int contentTypeLength = 0;

        if (willPublish != null) {
            topic = willPublish.getTopic().getBytes();
            topicLength = topic.length;

            hivemqId = willPublish.getHivemqId().getBytes();
            hivemqIdLength = hivemqId.length;

            // payload ID, delay, ttl, payload format, retained, topic length, topic, hivemq id length, hivemq id , user properties
            willLength += 8 + 8 + 8 + 1 + 1 + 4 + topicLength + 4 + hivemqIdLength + PropertiesSerializationUtil.encodedSize(willPublish.getUserProperties());

            responseTopic = willPublish.getResponseTopic() != null ? willPublish.getResponseTopic().getBytes() : null;
            responseTopicLength = responseTopic != null ? responseTopic.length : 0;
            willLength += responseTopicLength + 4;

            correlationData = willPublish.getCorrelationData();
            correlationDataLength = correlationData != null ? correlationData.length : 0;
            willLength += correlationDataLength + 4;

            contentType = willPublish.getContentType() != null ? willPublish.getContentType().getBytes() : null;
            contentTypeLength = contentType != null ? contentType.length : 0;
            willLength += contentTypeLength + 4;

            payloadFormatIndicator = (byte) (willPublish.getPayloadFormatIndicator() != null ? willPublish.getPayloadFormatIndicator().getCode() : -1);
        }

        int queueLimitLength = clientSession.getQueueLimit() != null ? Long.BYTES : 0;


        final byte[] bytes = new byte[
                Long.BYTES +        // timestamp
                        Long.BYTES +        // expiry interval
                        1 +                 // flags
                        willLength +
                        queueLimitLength];
        int cursor = 0;

        Bytes.copyLongToByteArray(timestamp, bytes, 0);
        cursor += 8;
        Bytes.copyLongToByteArray(clientSession.getSessionExpiryInterval(), bytes, cursor);
        cursor += 8;

        byte flags = (byte) 0b0000_0000;
        flags = Bytes.setBit(flags, CLIENT_CONNECTED_BIT, clientSession.isConnected());
        flags = Bytes.setBit(flags, QUEUE_SIZE_PRESENT_BIT, clientSession.getQueueLimit() != null);
        bytes[cursor] = flags;
        cursor += 1;

        if (clientSession.getQueueLimit() != null) {
            Bytes.copyLongToByteArray(clientSession.getQueueLimit(), bytes, cursor);
            cursor += Long.BYTES;
        }

        if (willLength == 1) {
            bytes[cursor] = -1;
            cursor++;
        } else {

            bytes[cursor] = (byte) willPublish.getQos().getQosNumber();
            cursor += 1;

            Bytes.copyLongToByteArray(willPublish.getPublishId(), bytes, cursor);
            cursor += 8;
            Bytes.copyLongToByteArray(willPublish.getDelayInterval(), bytes, cursor);
            cursor += 8;
            Bytes.copyLongToByteArray(willPublish.getMessageExpiryInterval(), bytes, cursor);
            cursor += 8;

            bytes[cursor] = payloadFormatIndicator;
            cursor += 1;

            bytes[cursor] = (byte) (willPublish.isRetain() ? 1 : 0);
            cursor += 1;

            Bytes.copyIntToByteArray(topicLength, bytes, cursor);
            cursor += 4;
            System.arraycopy(topic, 0, bytes, cursor, topicLength);
            cursor += topicLength;

            Bytes.copyIntToByteArray(hivemqIdLength, bytes, cursor);
            cursor += 4;
            System.arraycopy(hivemqId, 0, bytes, cursor, hivemqIdLength);
            cursor += hivemqIdLength;

            Bytes.copyIntToByteArray(responseTopicLength, bytes, cursor);
            cursor += 4;
            if (responseTopicLength > 0) {
                System.arraycopy(responseTopic, 0, bytes, cursor, responseTopicLength);
                cursor += responseTopicLength;
            }
            Bytes.copyIntToByteArray(correlationDataLength, bytes, cursor);
            cursor += 4;
            if (correlationDataLength > 0) {
                System.arraycopy(correlationData, 0, bytes, cursor, correlationDataLength);
                cursor += correlationDataLength;
            }
            Bytes.copyIntToByteArray(contentTypeLength, bytes, cursor);
            cursor += 4;
            if (contentTypeLength > 0) {
                System.arraycopy(contentType, 0, bytes, cursor, contentTypeLength);
                cursor += contentTypeLength;
            }

            PropertiesSerializationUtil.write(willPublish.getUserProperties(), bytes, cursor);
        }

        return bytes;
    }

    @NotNull
    public ClientSession deserializeValue(final byte[] bytes) {

        int cursor = Long.BYTES; // skip time stamp
        final long timeToLive = Bytes.readLong(bytes, cursor);
        cursor += Long.BYTES;
        final byte flags = bytes[cursor];
        final boolean connected = Bytes.isBitSet(flags, CLIENT_CONNECTED_BIT);
        cursor += 1;

        Long queueLimit = null;
        if (Bytes.isBitSet(flags, QUEUE_SIZE_PRESENT_BIT)) {
            queueLimit = Bytes.readLong(bytes, cursor);
            cursor += Long.BYTES;
        }

        final int willQos = bytes[cursor];
        cursor += 1;
        ClientSessionWill sessionWill = null;
        if (willQos != NO_WILL_MARKER) {
            final MqttWillPublish.Mqtt5Builder willBuilder = new MqttWillPublish.Mqtt5Builder();
            willBuilder.withQos(QoS.valueOf(willQos));

            final long publishId = Bytes.readLong(bytes, cursor);
            cursor += 8;
            willBuilder.withDelayInterval(Bytes.readLong(bytes, cursor));
            cursor += 8;
            willBuilder.withMessageExpiryInterval(Bytes.readLong(bytes, cursor));
            cursor += 8;

            final byte payloadFormatCode = bytes[cursor];
            cursor += 1;
            willBuilder.withPayloadFormatIndicator(payloadFormatCode != -1 ? Mqtt5PayloadFormatIndicator.fromCode(payloadFormatCode) : null);

            willBuilder.withRetain(bytes[cursor] == 1);
            cursor += 1;

            final int topicLength = Bytes.readInt(bytes, cursor);
            cursor += 4;
            if (topicLength > 0) {
                willBuilder.withTopic(new String(bytes, cursor, topicLength));
                cursor += topicLength;
            }

            final int hivemqIdLength = Bytes.readInt(bytes, cursor);
            cursor += 4;
            if (topicLength > 0) {
                willBuilder.withHivemqId(new String(bytes, cursor, hivemqIdLength));
                cursor += hivemqIdLength;
            }

            final int responseTopicLength = Bytes.readInt(bytes, cursor);
            cursor += 4;
            if (responseTopicLength > 0) {
                willBuilder.withResponseTopic(new String(bytes, cursor, responseTopicLength));
                cursor += responseTopicLength;
            }

            final int correlationDataLength = Bytes.readInt(bytes, cursor);
            cursor += 4;
            if (correlationDataLength != 0) {
                final byte[] correlationData = new byte[correlationDataLength];
                System.arraycopy(bytes, cursor, correlationData, 0, correlationDataLength);
                willBuilder.withCorrelationData(correlationData);
                cursor += correlationDataLength;
            }

            final int contentTypeLength = Bytes.readInt(bytes, cursor);
            cursor += 4;
            if (contentTypeLength != 0) {
                willBuilder.withContentType(new String(bytes, cursor, contentTypeLength, UTF_8));
                cursor += contentTypeLength;
            }

            willBuilder.withUserProperties(PropertiesSerializationUtil.read(bytes, cursor));
            sessionWill = new ClientSessionWill(willBuilder.build(), publishId);
        }

        return new ClientSession(connected, timeToLive, sessionWill, queueLimit);
    }


    @NotNull
    public ClientSession deserializeValueWithoutWill(final byte[] bytes) {

        int cursor = Long.BYTES; // Skip timestamp
        final long timeToLive = Bytes.readLong(bytes, cursor);
        cursor += Long.BYTES;
        final byte flags = bytes[cursor];
        final boolean connected = Bytes.isBitSet(flags, 0);
        cursor += 1;

        Long queueLimit = null;
        if (Bytes.isBitSet(flags, QUEUE_SIZE_PRESENT_BIT)) {
            queueLimit = Bytes.readLong(bytes, cursor);
            cursor += Long.BYTES;
        }

        return new ClientSession(connected, timeToLive, null, queueLimit);
    }


    public long deserializeTimestamp(final byte[] bytes) {
        return Bytes.readLong(bytes, 0);
    }

}
