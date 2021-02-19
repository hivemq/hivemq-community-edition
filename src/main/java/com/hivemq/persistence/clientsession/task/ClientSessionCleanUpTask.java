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
package com.hivemq.persistence.clientsession.task;

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.clientsession.ClientSessionPersistenceImpl;
import com.hivemq.persistence.local.ClientSessionLocalPersistence;
import com.hivemq.util.Checkpoints;

import java.util.Set;

public class ClientSessionCleanUpTask implements SingleWriterService.Task<Void> {
    @NotNull
    private final ClientSessionLocalPersistence localPersistence;
    @NotNull
    private final ClientSessionPersistenceImpl clientSessionPersistence;

    public ClientSessionCleanUpTask(@NotNull final ClientSessionLocalPersistence localPersistence,
                                    @NotNull final ClientSessionPersistenceImpl clientSessionPersistence) {

        this.localPersistence = localPersistence;
        this.clientSessionPersistence = clientSessionPersistence;
    }

    @Override
    public @NotNull Void doTask(final int bucketIndex, @NotNull final ImmutableList<Integer> queueBuckets, final int queueIndex) {

        final Set<String> expiredSessions = localPersistence.cleanUp(bucketIndex);
        for (final String expiredSession : expiredSessions) {

            clientSessionPersistence.cleanClientData(expiredSession);
        }
        Checkpoints.checkpoint("ClientSessionCleanUpFinished");
        return null;
    }
}