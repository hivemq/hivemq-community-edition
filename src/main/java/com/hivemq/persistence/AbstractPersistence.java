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
package com.hivemq.persistence;

import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Lukas Brandl
 */
public abstract class AbstractPersistence {

    private static final Logger log = LoggerFactory.getLogger(AbstractPersistence.class);

    @NotNull
    protected ListenableFuture<Void> closeDB(
            final @NotNull LocalPersistence localPersistence,
            final @NotNull ProducerQueues singleWriter) {
        return singleWriter.shutdown((bucketIndex) -> {
            try {
                localPersistence.closeDB(bucketIndex);
            } catch (final Throwable t) {
                log.warn("Persistence not closed properly: " + t.getMessage());
                log.debug("Original exception:", t);
                Exceptions.rethrowError(t);
            }
            return null;
        });
    }
}
