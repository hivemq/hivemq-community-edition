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
package util.encoder;

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.codec.encoder.mqtt3.AbstractVariableHeaderLengthEncoder;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.util.Bytes;
import com.hivemq.util.Strings;
import com.hivemq.util.Utf8Utils;
import io.netty.buffer.ByteBuf;

/**
 * A MQTT Encoder which encodes {@link com.hivemq.mqtt.message.connect.CONNECT} messages.
 *
 * @author Dominik Obermaier
 */
public class Mqtt3ConnectEncoder extends AbstractVariableHeaderLengthEncoder<CONNECT> {

    private static final byte CONNECT_FIXED_HEADER = (byte) 0b0001_0000;

    private static final byte[] PROTOCOL_NAME_V311 =             //'MQTT4'
            new byte[]{0, 4, 0b0100_1101, 0b0101_0001, 0b0101_0100, 0b0101_0100, 4};

    private static final byte[] PROTOCOL_NAME_V31 =
            //'MQIsdp3'
            new byte[]{0, 6, 0b0100_1101, 0b0101_0001, 0b0100_1001, 0b0111_0011, 0b0110_0100, 0b0111_0000, 3};

    @Override
    public void encode(
            final @NotNull ClientConnection clientConnection, final @NotNull CONNECT msg, final @NotNull ByteBuf out) {

        out.writeByte(CONNECT_FIXED_HEADER);
        createRemainingLength(msg.getRemainingLength(), out);

        if (msg.getProtocolVersion() == ProtocolVersion.MQTTv3_1) {
            out.writeBytes(PROTOCOL_NAME_V31);
        } else if (msg.getProtocolVersion() == ProtocolVersion.MQTTv3_1_1) {
            out.writeBytes(PROTOCOL_NAME_V311);
        } else {
            throw new IllegalArgumentException("Protocol version must be set for a MQTT CONNECT packet");
        }

        out.writeByte(createConnectionFlag(msg));
        out.writeShort(msg.getKeepAlive());

        Strings.createPrefixedBytesFromString(msg.getClientIdentifier(), out);

        if (msg.getWillPublish() != null) {
            Strings.createPrefixedBytesFromString(msg.getWillPublish().getTopic(), out);
            Bytes.prefixBytes(msg.getWillPublish().getPayload(), out);
        }
        if (msg.getUsername() != null) {
            Strings.createPrefixedBytesFromString(msg.getUsername(), out);
        }
        if (msg.getPassword() != null) {
            Bytes.prefixBytes(msg.getPassword(), out);
        }
    }

    protected int remainingLength(final @NotNull CONNECT msg) {
        int length = 0;
        if (msg.getProtocolVersion() == ProtocolVersion.MQTTv3_1) {
            length += PROTOCOL_NAME_V31.length;
        } else if (msg.getProtocolVersion() == ProtocolVersion.MQTTv3_1_1) {
            length += PROTOCOL_NAME_V311.length;
        } else {
            throw new IllegalArgumentException("Protocol version must be set for a MQTT CONNECT packet");
        }
        length += 1; // connect flag
        length += 2; // keep alive time
        length += Utf8Utils.encodedLength(msg.getClientIdentifier()) + 2;
        if (msg.getWillPublish() != null) {
            length += Utf8Utils.encodedLength(msg.getWillPublish().getTopic()) + 2;
            length += msg.getWillPublish().getPayload().length + 2;
        }
        if (msg.getUsername() != null) {
            length += Utf8Utils.encodedLength(msg.getUsername()) + 2;
        }
        if (msg.getPassword() != null) {
            length += msg.getPassword().length + 2;
        }
        return length;
    }

    /**
     * Creates the correct connect flags according to the MQTT spec
     *
     * @param message the {@link com.hivemq.mqtt.message.connect.CONNECT} message
     * @return a byte which has the flags set accordingly for the given MQTT CONNECT message
     */
    private byte createConnectionFlag(final CONNECT message) {
        byte connectFlag = 0b0000_0000;

        if (message.getUsername() != null) {
            connectFlag |= 0b1000_0000;
        }
        if (message.getPassword() != null) {
            connectFlag |= 0b0100_0000;
        }
        if (message.getWillPublish() != null) {

            connectFlag |= 0b0000_0100;

            connectFlag |= message.getWillPublish().getQos().getQosNumber() << 3;

            if (message.getWillPublish().isRetain()) {
                connectFlag |= 0b0010_0000;
            }
        }
        //Mqtt 3 Clean Session = Mqtt 5 CleanStart + Expiry 0
        if (message.isCleanStart() && message.getSessionExpiryInterval() == 0) {
            connectFlag |= 0b0000_0010;
        }
        return connectFlag;
    }
}
