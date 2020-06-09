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
package com.hivemq.persistence.payload;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Florian Limpöck
 */
public class PublishPayloadXodusSerializerTest {

    private final PublishPayloadXodusSerializer serializer = new PublishPayloadXodusSerializer();

    @Test
    public void test_serialize_key() throws Exception {
        final byte[] bytes = serializer.serializeKey(1234L, 5L);

        assertEquals(16, bytes.length);

        final PublishPayloadXodusLocalPersistence.KeyPair keyPair = serializer.deserializeKey(bytes);

        assertEquals(1234L, keyPair.getId());
        assertEquals(5L, keyPair.getChunkIndex());
    }
}