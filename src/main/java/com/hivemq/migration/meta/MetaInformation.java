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
package com.hivemq.migration.meta;


import com.hivemq.extension.sdk.api.annotations.Nullable;

/**
 * @author Christoph Schäbel
 * @author Florian Limpöck
 */
public class MetaInformation {

    private @Nullable String hivemqVersion = null;
    private @Nullable String clientSessionPersistenceVersion = null;
    private @Nullable String queuedMessagesPersistenceVersion = null;
    private @Nullable String subscriptionPersistenceVersion = null;
    private @Nullable String retainedMessagesPersistenceVersion = null;
    private @Nullable String publishPayloadPersistenceVersion = null;

    private @Nullable PersistenceType retainedMessagesPersistenceType = null;
    private @Nullable PersistenceType publishPayloadPersistenceType = null;

    private boolean dataFolderPresent = false;
    private boolean persistenceFolderPresent = false;
    private boolean metaFilePresent = false;

    @Nullable
    public String getHivemqVersion() {
        return hivemqVersion;
    }

    public void setHivemqVersion(final @Nullable String hivemqVersion) {
        this.hivemqVersion = hivemqVersion;
    }

    public boolean isDataFolderPresent() {
        return dataFolderPresent;
    }

    public void setDataFolderPresent(final boolean dataFolderPresent) {
        this.dataFolderPresent = dataFolderPresent;
    }

    public boolean isPersistenceFolderPresent() {
        return persistenceFolderPresent;
    }

    public void setPersistenceFolderPresent(final boolean persistenceFolderPresent) {
        this.persistenceFolderPresent = persistenceFolderPresent;
    }

    public boolean isMetaFilePresent() {
        return metaFilePresent;
    }

    public void setMetaFilePresent(final boolean metaFilePresent) {
        this.metaFilePresent = metaFilePresent;
    }

    @Nullable
    public String getClientSessionPersistenceVersion() {
        return clientSessionPersistenceVersion;
    }

    public void setClientSessionPersistenceVersion(final @Nullable String clientSessionPersistenceVersion) {
        this.clientSessionPersistenceVersion = clientSessionPersistenceVersion;
    }

    @Nullable
    public String getQueuedMessagesPersistenceVersion() {
        return queuedMessagesPersistenceVersion;
    }

    public void setQueuedMessagesPersistenceVersion(final @Nullable String queuedMessagesPersistenceVersion) {
        this.queuedMessagesPersistenceVersion = queuedMessagesPersistenceVersion;
    }

    @Nullable
    public String getSubscriptionPersistenceVersion() {
        return subscriptionPersistenceVersion;
    }

    public void setSubscriptionPersistenceVersion(final @Nullable String subscriptionPersistenceVersion) {
        this.subscriptionPersistenceVersion = subscriptionPersistenceVersion;
    }

    @Nullable
    public String getRetainedMessagesPersistenceVersion() {
        return retainedMessagesPersistenceVersion;
    }

    public void setRetainedMessagesPersistenceVersion(final @Nullable String retainedMessagesPersistenceVersion) {
        this.retainedMessagesPersistenceVersion = retainedMessagesPersistenceVersion;
    }

    @Nullable
    public String getPublishPayloadPersistenceVersion() {
        return publishPayloadPersistenceVersion;
    }

    public void setPublishPayloadPersistenceVersion(final @Nullable String publishPayloadPersistenceVersion) {
        this.publishPayloadPersistenceVersion = publishPayloadPersistenceVersion;
    }


    @Nullable
    public PersistenceType getRetainedMessagesPersistenceType() {
        return retainedMessagesPersistenceType;
    }

    public void setRetainedMessagesPersistenceType(final @Nullable PersistenceType retainedMessagesPersistenceType) {
        this.retainedMessagesPersistenceType = retainedMessagesPersistenceType;
    }

    @Nullable
    public PersistenceType getPublishPayloadPersistenceType() {
        return publishPayloadPersistenceType;
    }

    public void setPublishPayloadPersistenceType(final @Nullable PersistenceType publishPayloadPersistenceType) {
        this.publishPayloadPersistenceType = publishPayloadPersistenceType;
    }

    @Override
    public String toString() {
        return "MetaInformation{" +
                "hivemqVersion='" + hivemqVersion + '\'' +
                ", clientSessionPersistenceVersion='" + clientSessionPersistenceVersion + '\'' +
                ", queuedMessagesPersistenceVersion='" + queuedMessagesPersistenceVersion + '\'' +
                ", subscriptionPersistenceVersion='" + subscriptionPersistenceVersion + '\'' +
                ", retainedMessagesPersistenceVersion='" + retainedMessagesPersistenceVersion + '\'' +
                ", publishPayloadPersistenceVersion='" + publishPayloadPersistenceVersion + '\'' +
                ", retainedMessagesPersistenceType='" + retainedMessagesPersistenceType + '\'' +
                ", publishPayloadPersistenceType='" + publishPayloadPersistenceType + '\'' +
                '}';
    }
}
