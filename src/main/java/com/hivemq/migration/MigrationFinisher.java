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
package com.hivemq.migration;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.migration.meta.MetaFileService;
import com.hivemq.migration.meta.MetaInformation;
import com.hivemq.migration.meta.PersistenceType;
import com.hivemq.persistence.clientqueue.ClientQueueXodusLocalPersistence;
import com.hivemq.persistence.local.xodus.RetainedMessageRocksDBLocalPersistence;
import com.hivemq.persistence.local.xodus.RetainedMessageXodusLocalPersistence;
import com.hivemq.persistence.local.xodus.clientsession.ClientSessionSubscriptionXodusLocalPersistence;
import com.hivemq.persistence.local.xodus.clientsession.ClientSessionXodusLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadRocksDBLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadXodusLocalPersistence;

/**
 * @author Christoph Schäbel
 * @author Florian Limpöck
 */
class MigrationFinisher {

    private final @NotNull SystemInformation systemInformation;
    private final @NotNull PersistenceType retainedType;
    private final @NotNull PersistenceType payloadType;

    public MigrationFinisher(final @NotNull SystemInformation systemInformation) {
        this.systemInformation = systemInformation;
        this.retainedType = InternalConfigurations.RETAINED_MESSAGE_PERSISTENCE_TYPE.get();
        this.payloadType = InternalConfigurations.PAYLOAD_PERSISTENCE_TYPE.get();
    }

    public void finishMigration() {
        saveNewStateToMetaFile();
    }

    private void saveNewStateToMetaFile() {

        final MetaInformation metaFile = MetaFileService.readMetaFile(systemInformation);
        metaFile.setHivemqVersion(systemInformation.getHiveMQVersion());

        metaFile.setClientSessionPersistenceVersion(ClientSessionXodusLocalPersistence.PERSISTENCE_VERSION);
        metaFile.setQueuedMessagesPersistenceVersion(ClientQueueXodusLocalPersistence.PERSISTENCE_VERSION);
        metaFile.setSubscriptionPersistenceVersion(ClientSessionSubscriptionXodusLocalPersistence.PERSISTENCE_VERSION);
        metaFile.setRetainedMessagesPersistenceVersion(retainedType == PersistenceType.FILE_NATIVE ? RetainedMessageRocksDBLocalPersistence.PERSISTENCE_VERSION : RetainedMessageXodusLocalPersistence.PERSISTENCE_VERSION);
        metaFile.setPublishPayloadPersistenceVersion(payloadType == PersistenceType.FILE_NATIVE ? PublishPayloadRocksDBLocalPersistence.PERSISTENCE_VERSION : PublishPayloadXodusLocalPersistence.PERSISTENCE_VERSION);
        metaFile.setRetainedMessagesPersistenceType(retainedType);
        metaFile.setPublishPayloadPersistenceType(payloadType);

        MetaFileService.writeMetaFile(systemInformation, metaFile);
    }
}
