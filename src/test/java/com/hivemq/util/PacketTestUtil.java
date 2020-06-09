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
package com.hivemq.util;

import com.hivemq.mqtt.message.reason.Mqtt5PubRecReasonCode;

/**
 * @author Waldemar Ruck
 * @since 4.0
 */
public class PacketTestUtil {

    private static PacketTestUtil instance;

    private PacketTestUtil() {
    }

    public static synchronized PacketTestUtil getInstance() {
        if (PacketTestUtil.instance == null) {
            PacketTestUtil.instance = new PacketTestUtil();
        }
        return PacketTestUtil.instance;
    }

    public byte[] getInvalidPubrel() {
        return new byte[]{

                //fixed header
                //  type, flags
                (byte) 0b0110_0010,
                //  remaining length
                3,
                //   packet identifier
                0, 5,
                //  reason code
                0x50

        };
    }

    public byte[] getInvalidPubrec() {
        return new byte[]{
                // fixed header
                //   type, flags
                (byte) 0b0101_0000,
                //   remaining length
                14,
                //   packet identifier
                0, 5,
                //   reason code (continue)
                (byte) Mqtt5PubRecReasonCode.SUCCESS.getCode(),
                //   properties
                8,
                //     reason string
                0x1F, 0, 7, 's', 'u', 'c', 'c', 'e', 's', 's',
                //     user properties
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e'
        };
    }

    public byte[] getInvalidPuback() {
        return new byte[]{
                // fixed header
                //   type, flags
                0b0100_0000,
                //   remaining length
                4,
                // variable header
                //   packet identifier
                0, 5,
                //   reason code (success)
                (byte) 1234,
                //   properties
                0
        };
    }

    public byte[] getInvalidPublish() {

        return new byte[]{
                // fixed header
                //   type, flags
                0b0011_0011,
                //   remaining length
                70,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   packet identifier
                0, (byte) 1,
                //   properties length
                50,
        };
    }

    public byte[] getPublish(final int id) {

        return new byte[]{
                // fixed header
                //   type, flags
                0b0011_0011,
                //   remaining length
                70,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   packet identifier
                0, (byte) id,
                //   properties length
                50,
                //     payload format indicator
                0x01, 0,
                //     message expiry interval
                0x02, 0, 0, 0, 10,
                //     topic alias
                0x23, 0, 3,
                //     response topic
                0x08, 0, 8, 'r', 'e', 's', 'p', 'o', 'n', 's', 'e',
                //     correlation data
                0x09, 0, 5, 5, 4, 3, 2, 1,
                //     user properties
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e',
                //     content type
                0x03, 0, 4, 't', 'e', 'x', 't',
                // payload
                1, 2, 3, 4, 5, 6, 7, 8, 9, 10
        };
    }

    public byte[] getConnectWithOut(final boolean requestProblemInformation) {

        return new byte[]{
                // fixed header
                //   type, flags
                0b0001_0000,
                // remaining length (223)
                (byte) (128 + 76), 1,
                // variable header
                //   protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b1110_1110,
                //   keep alive
                0, 60,
                //   properties
                88,
                //     session expiry interval
                0x11, 0, 0, 0, 10,
                //     request response information
                0x19, 1,
                //     request problem information
                0x17, requestProblemInformation ? (byte) 1 : (byte) 0,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                //     auth data
                0x16, 0, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
                //     receive maximum
                0x21, 0, 5,
                //     topic alias maximum
                0x22, 0, 10,
                //     maximum packet size
                0x27, 0, 0, 0, 100,
                //     user properties
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e', //
                0x26, 0, 4, 't', 'e', 's', 't', 0, 6, 'v', 'a', 'l', 'u', 'e', '2', //
                0x26, 0, 5, 't', 'e', 's', 't', '2', 0, 5, 'v', 'a', 'l', 'u', 'e',
                // payload
                //   client identifier
                0, 4, 't', 'e', 's', 't',
                //   will properties
                63,
                //     message expiry interval
                0x02, 0, 0, 0, 10,
                //     payload format indicator
                0x01, 1,
                //     content type
                0x03, 0, 4, 't', 'e', 'x', 't',
                //     user property
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e', //
                0x26, 0, 4, 't', 'e', 's', 't', 0, 6, 'v', 'a', 'l', 'u', 'e', '2', //
                0x26, 0, 5, 't', 'e', 's', 't', '2', 0, 5, 'v', 'a', 'l', 'u', 'e',
                //     will delay interval
                0x18, 0, 0, 0, 5,
                //   will topic
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   will payload
                0, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
                //   username
                0, 8, 'u', 's', 'e', 'r', 'n', 'a', 'm', 'e',
                //   password
                0, 4, 'p', 'a', 's', 's'
        };
    }

    public byte[] getConnect(final boolean requestProblemInformation) {

        return new byte[]{
                // fixed header
                //   type, flags
                0b0001_0000,
                // remaining length (183)
                (byte) (128 + 36), 1,
                // variable header
                //   protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1110,
                //   keep alive
                0, 60,
                //   properties
                64,
                //     session expiry interval
                0x11, 0, 0, 0, 10,
                //     request response information
                0x19, 1,
                //     request problem information
                0x17, requestProblemInformation ? (byte) 1 : (byte) 0,
                //     receive maximum
                0x21, 0, 5,
                //     topic alias maximum
                0x22, 0, 10,
                //     maximum packet size
                0x27, 0, 0, 0, 100,
                //     user properties
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e', //
                0x26, 0, 4, 't', 'e', 's', 't', 0, 6, 'v', 'a', 'l', 'u', 'e', '2', //
                0x26, 0, 5, 't', 'e', 's', 't', '2', 0, 5, 'v', 'a', 'l', 'u', 'e',
                // payload
                //   client identifier
                0, 4, 't', 'e', 's', 't',
                //   will properties
                63,
                //     message expiry interval
                0x02, 0, 0, 0, 10,
                //     payload format indicator
                0x01, 1,
                //     content type
                0x03, 0, 4, 't', 'e', 'x', 't',
                //     user property
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e', //
                0x26, 0, 4, 't', 'e', 's', 't', 0, 6, 'v', 'a', 'l', 'u', 'e', '2', //
                0x26, 0, 5, 't', 'e', 's', 't', '2', 0, 5, 'v', 'a', 'l', 'u', 'e',
                //     will delay interval
                0x18, 0, 0, 0, 5,
                //   will topic
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   will payload
                0, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
        };
    }
}
