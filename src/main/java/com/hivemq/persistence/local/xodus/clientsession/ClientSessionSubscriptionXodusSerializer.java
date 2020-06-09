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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.ThreadSafe;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5RetainHandling;
import com.hivemq.mqtt.message.subscribe.Mqtt5SUBSCRIBE;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.persistence.local.xodus.XodusUtils;
import com.hivemq.util.Bytes;
import jetbrains.exodus.ByteIterable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Serializes Topic objects for ClientSessionSubscriptions. The serialization format is
 * <p>
 * <p>
 * timestamp (8 byte) | list of several topics (0 - n)
 * <p>
 * one topic is serialized as: <br/> Topiclength(4bytes)|Topicstring(UTF-8)(n-bytes)|Timestamp(8bytes)|QoS(2byte)|ID(8bytes)|Flags(1byte)|RetainHandling(1byte)
 * <p>
 * flags: <br/> bit 1: no local <br/> bit 2: retain as published
 *
 * @author Dominik Obermaier
 * @author Christoph Schäbel
 */
@ThreadSafe
public class ClientSessionSubscriptionXodusSerializer {

    /**
     * Serializes a Topic object to a byte array.
     *
     * @param topic the topic to serialize. Must not be <code>null</code>
     * @return a byte array with the layout of <code>Topiclength|Topicstring(UTF-8)|Timestamp|QoS|ID|Flags|RetainHandling</code>
     * @throws NullPointerException if the given Topic is <code>null</code>
     */
    @ThreadSafe
    public byte[] serializeValue(@NotNull final Topic topic, final long timestamp, final long id) {

        checkNotNull(topic, "Topic must not be null");
        final int retainHandling = topic.getRetainHandling().getCode();
        checkArgument(retainHandling >= 0 && retainHandling <= 2, "Retain handling code must be between 0 and 2");


        final byte[] topicBytes = topic.getTopic().getBytes(UTF_8);

        final byte[] bytes = new byte[topicBytes.length + 27];

        int cursor = 0;

        Bytes.copyIntToByteArray(topicBytes.length, bytes, cursor);
        cursor += 4;

        System.arraycopy(topicBytes, 0, bytes, cursor, topicBytes.length);
        cursor += topicBytes.length;

        Bytes.copyLongToByteArray(timestamp, bytes, cursor);
        cursor += 8;

        final int qos = topic.getQoS().getQosNumber();
        bytes[cursor] = (byte) qos;
        cursor += 1;

        Bytes.copyLongToByteArray(id, bytes, cursor);
        cursor += 8;

        //flags
        if (topic.isNoLocal()) {
            bytes[cursor] = (byte) (bytes[cursor] | 0b0001);
        }

        if (topic.isRetainAsPublished()) {
            bytes[cursor] = (byte) (bytes[cursor] | 0b0010);
        }

        cursor += 1;

        bytes[cursor] = (byte) retainHandling;
        cursor += 1;

        if (topic.getSubscriptionIdentifier() != null) {
            Bytes.copyIntToByteArray(topic.getSubscriptionIdentifier(), bytes, cursor);
        } else {
            Bytes.copyIntToByteArray(Mqtt5SUBSCRIBE.DEFAULT_NO_SUBSCRIPTION_IDENTIFIER, bytes, cursor);
        }
        cursor += 4;

        return bytes;
    }


    /**
     * Serializes a String object to a byte array.
     *
     * @param topic the topic to serialize. Must not be <code>null</code>
     * @return a byte array with the layout of <code>Topiclength|Topicstring(UTF-8)</code>
     */
    @ThreadSafe
    public byte[] serializeTopic(@NotNull final String topic) {
        checkNotNull(topic, "Topic must not be null");

        final byte[] topicBytes = topic.getBytes(UTF_8);

        final byte[] bytes = new byte[topicBytes.length + 4];

        Bytes.copyIntToByteArray(topicBytes.length, bytes, 0);
        System.arraycopy(topicBytes, 0, bytes, 4, topicBytes.length);

        return bytes;
    }


    /**
     * Returns a topic from a byte array. If the byte array contains a -1 as the first entry, the topic won't have a
     * QoS. The topic String will be read as UTF-8.
     *
     * @param bytes the byte array to deserialize. Must at least have a size of 1
     * @return The deserialized topic.
     * @throws NullPointerException     If the given byte array is <code>null</code>
     * @throws IllegalArgumentException if the byte array has a length of 0.
     */
    @ThreadSafe
    public Topic deserializeValue(@NotNull final byte[] bytes) {

        checkNotNull(bytes, "Bytes must not be null");
        checkArgument(bytes.length > 0, "Bytes must be greater than 1");

        int cursor = 0;
        final int topicLength = Bytes.readInt(bytes, cursor);
        cursor += 4;

        final String topic = new String(bytes, cursor, topicLength, UTF_8);
        cursor += topicLength;

        // Skip timestamp
        cursor += 8;

        final int qos = bytes[cursor];
        cursor += 1;

        // Skip ID
        cursor += 8;

        final boolean noLocal = (bytes[cursor] & 0b0001) != 0;
        final boolean retainAsPublished = (bytes[cursor] & 0b0010) != 0;
        cursor += 1;

        final int retainHandlingCode = bytes[cursor];

        final Mqtt5RetainHandling mqtt5RetainHandling = Mqtt5RetainHandling.fromCode(retainHandlingCode);
        cursor += 1;

        Integer subscriptionIdetifier = Bytes.readInt(bytes, cursor);
        if (subscriptionIdetifier == Mqtt5SUBSCRIBE.DEFAULT_NO_SUBSCRIPTION_IDENTIFIER) {
            subscriptionIdetifier = null;
        }
        cursor += 4;

        return new Topic(topic, QoS.valueOf(qos), noLocal, retainAsPublished, mqtt5RetainHandling, subscriptionIdetifier);
    }

    public long deserializeId(final byte[] bytes) {
        return Bytes.readLong(bytes, bytes.length - 14);
    }

    public long deserializeTimestamp(final byte[] bytes) {
        return Bytes.readLong(bytes, bytes.length - 23);
    }

    public Topic deserializeValue(final ByteIterable value) {
        checkNotNull(value, "ByteIterable must not be null");
        return deserializeValue(XodusUtils.byteIterableToBytes(value));
    }

    public byte[] serializeKey(final String client) {

        final byte[] clientIdBytes = client.getBytes(UTF_8);
        final byte[] bytes = new byte[clientIdBytes.length + 10];

        bytes[0] = (byte) (clientIdBytes.length & 0xFF);
        bytes[1] = (byte) ((clientIdBytes.length >> 8) & 0xFF);
        System.arraycopy(clientIdBytes, 0, bytes, 2, clientIdBytes.length);

        return bytes;
    }


    public String deserializeKey(final byte[] bytes) {

        final int clientIdLength = (bytes[0] & 0xFF) + ((bytes[1] & 0xFF) << 8);

        return new String(bytes, 2, clientIdLength, UTF_8);
    }
}
