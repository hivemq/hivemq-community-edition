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

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.configuration.HivemqId;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Florian Limpöck
 */
@LazySingleton
public class ClientIds {

    private final AtomicLong clientIDCounter = new AtomicLong(0);
    private final String hivemqId;
    private final HashFunction hashFunction = Hashing.murmur3_128();

    @Inject
    public ClientIds(final HivemqId hiveMQId) {
        this.hivemqId = hiveMQId.get();
    }

    public String generateNext() {

        final long currentCounter = clientIDCounter.getAndIncrement();
        final String rawID = "hmq_" + hivemqId + "_" + currentCounter + "_" + System.currentTimeMillis();
        return "hmq_" + hivemqId + "_" + currentCounter + "_" + hashFunction.hashString(rawID, StandardCharsets.UTF_8).toString();

    }
}
