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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.clientsession.ClientSessionPersistence;
import com.hivemq.persistence.clientsession.ClientSessionPersistenceImpl;
import com.hivemq.persistence.clientsession.PendingWillMessages;
import com.hivemq.persistence.local.ClientSessionLocalPersistence;
import com.hivemq.util.Checkpoints;

import java.util.Set;

public class ClientSessionCleanUpTask implements SingleWriterService.Task<Void> {

    private final @NotNull ClientSessionLocalPersistence localPersistence;
    private final @NotNull ClientSessionPersistence clientSessionPersistence;
    private final @NotNull PendingWillMessages pendingWillMessages;

    public ClientSessionCleanUpTask(
            final @NotNull ClientSessionLocalPersistence localPersistence,
            final @NotNull ClientSessionPersistenceImpl clientSessionPersistence,
            final @NotNull PendingWillMessages pendingWillMessages) {

        this.localPersistence = localPersistence;
        this.clientSessionPersistence = clientSessionPersistence;
        this.pendingWillMessages = pendingWillMessages;
    }

    @Override
    public @Nullable Void doTask(final int bucketIndex) {
        final Set<String> expiredSessions = localPersistence.cleanUp(bucketIndex);
        for (final String expiredSession : expiredSessions) {
            pendingWillMessages.sendWillIfPending(expiredSession);
            clientSessionPersistence.cleanClientData(expiredSession);
        }
        Checkpoints.checkpoint("ClientSessionCleanUpFinished");
        return null;
    }
}
