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
package com.hivemq.codec.encoder;

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.codec.encoder.mqtt3.AbstractVariableHeaderLengthEncoder;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.publish.PUBLISH;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Christoph Schäbel
 */
public class AbstractVariableHeaderLengthEncoderTest {

    private TestAbstractVariableHeaderLengthEncoder encoder;

    @Before
    public void before() {
        encoder = new TestAbstractVariableHeaderLengthEncoder();
    }

    @Test
    public void test_remaining_length_encoder_0() throws Exception {

        final ByteBuf remainingLength = encoder.getRemainingLength(0);

        assertEquals(1, remainingLength.readableBytes());
        assertEquals(0, remainingLength.readByte());
    }

    @Test
    public void test_remaining_length_encoder_1() throws Exception {

        final ByteBuf remainingLength = encoder.getRemainingLength(1);

        assertEquals(1, remainingLength.readableBytes());
        assertEquals(1, remainingLength.readByte());
    }

    @Test
    public void test_remaining_length_encoder_1_bytes() throws Exception {

        final ByteBuf remainingLength = encoder.getRemainingLength(123);

        assertEquals(1, remainingLength.readableBytes());
        assertEquals((byte) 0x7b, remainingLength.readByte());
    }

    @Test
    public void test_remaining_length_encoder_2_bytes() throws Exception {

        final ByteBuf remainingLength = encoder.getRemainingLength(1234);

        assertEquals(2, remainingLength.readableBytes());
        assertEquals((byte) 0xD2, remainingLength.readByte());
        assertEquals(9, remainingLength.readByte());
    }

    @Test
    public void test_remaining_length_encoder_3_bytes() throws Exception {

        final ByteBuf remainingLength = encoder.getRemainingLength(123456);

        assertEquals(3, remainingLength.readableBytes());
        assertEquals((byte) 0xC0, remainingLength.readByte());
        assertEquals((byte) 0xC4, remainingLength.readByte());
        assertEquals(7, remainingLength.readByte());
    }

    @Test
    public void test_remaining_length_encoder_4_bytes() throws Exception {

        final ByteBuf remainingLength = encoder.getRemainingLength(2345678);

        assertEquals(4, remainingLength.readableBytes());
        assertEquals((byte) 0xCE, remainingLength.readByte());
        assertEquals((byte) 0x95, remainingLength.readByte());
        assertEquals((byte) 0x8F, remainingLength.readByte());
        assertEquals(1, remainingLength.readByte());
    }

    @SuppressWarnings("NullabilityAnnotations")
    private class TestAbstractVariableHeaderLengthEncoder extends AbstractVariableHeaderLengthEncoder<PUBLISH> {

        public ByteBuf getRemainingLength(final int length) {
            int val = length;
            final ByteBuf buf = Unpooled.buffer();

            do {
                byte b = (byte) (val % 128);
                val = val / 128;
                if (val > 0) {
                    b = (byte) (b | (byte) 128);
                }
                buf.writeByte(b);
            } while (val > 0);

            return buf;
        }

        @Override
        public void encode(@NotNull ClientConnection clientConnection, @NotNull PUBLISH msg, @NotNull ByteBuf out) {

        }

        @Override
        public int bufferSize(final @NotNull ClientConnection clientConnection, final @NotNull PUBLISH msg) {
            return 256;
        }

        @Override
        protected int remainingLength(final @NotNull PUBLISH msg) {
            return 0;
        }
    }

}