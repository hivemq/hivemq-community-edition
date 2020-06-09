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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author Florian Limpöck
 */
public class MetaInformationSerializer {

    @NotNull
    public byte[] serialize(final @NotNull MetaInformation metaInformation) {


        final byte[] hivemqVersion =
                metaInformation.getHivemqVersion() != null ? metaInformation.getHivemqVersion().getBytes(UTF_8) :
                        new byte[0];
        final byte[] publishPayloadPersistenceVersion = metaInformation.getPublishPayloadPersistenceVersion() != null ?
                metaInformation.getPublishPayloadPersistenceVersion().getBytes(UTF_8) : new byte[0];
        final byte[] retainedMessagesPersistenceVersion =
                metaInformation.getRetainedMessagesPersistenceVersion() != null ?
                        metaInformation.getRetainedMessagesPersistenceVersion().getBytes(UTF_8) : new byte[0];
        final byte[] subscriptionPersistenceVersion = metaInformation.getSubscriptionPersistenceVersion() != null ?
                metaInformation.getSubscriptionPersistenceVersion().getBytes(UTF_8) : new byte[0];
        final byte[] clientSessionPersistenceVersion = metaInformation.getClientSessionPersistenceVersion() != null ?
                metaInformation.getClientSessionPersistenceVersion().getBytes(UTF_8) : new byte[0];
        final byte[] queuedMessagesPersistenceVersion = metaInformation.getQueuedMessagesPersistenceVersion() != null ?
                metaInformation.getQueuedMessagesPersistenceVersion().getBytes(UTF_8) : new byte[0];

        final byte retainedMessagesPersistenceType = metaInformation.getRetainedMessagesPersistenceType() != null ?
                (byte) metaInformation.getRetainedMessagesPersistenceType().ordinal() : -1;
        final byte publishPayloadPersistenceType = metaInformation.getPublishPayloadPersistenceType() != null ?
                (byte) metaInformation.getPublishPayloadPersistenceType().ordinal() : -1;

        final int bufferSize = 6 * 4 + //6 * int(4 byte) for byte[] length.
                hivemqVersion.length +
                publishPayloadPersistenceVersion.length +
                retainedMessagesPersistenceVersion.length +
                subscriptionPersistenceVersion.length +
                clientSessionPersistenceVersion.length +
                queuedMessagesPersistenceVersion.length +
                2; //types

        final ByteBuffer byteBuffer = ByteBuffer.allocate(bufferSize);

        putByteArray(hivemqVersion, byteBuffer);
        putByteArray(publishPayloadPersistenceVersion, byteBuffer);
        putByteArray(retainedMessagesPersistenceVersion, byteBuffer);
        putByteArray(subscriptionPersistenceVersion, byteBuffer);
        putByteArray(clientSessionPersistenceVersion, byteBuffer);
        putByteArray(queuedMessagesPersistenceVersion, byteBuffer);

        byteBuffer.put(retainedMessagesPersistenceType);
        byteBuffer.put(publishPayloadPersistenceType);

        return byteBuffer.array();
    }

    private void putByteArray(final byte[] source, final @NotNull ByteBuffer destination) {
        if (source.length > 0) {
            destination.putInt(source.length);
            destination.put(source);
        } else {
            destination.putInt(0);
        }
    }

    @NotNull
    public MetaInformation deserialize(final @NotNull byte[] metaBytes) {

        final ByteBuffer metaFileAsByteBuffer = ByteBuffer.wrap(metaBytes);

        final String hivemqVersion = getStringFromBuffer(metaFileAsByteBuffer);
        final String publishPayloadPersistenceVersion = getStringFromBuffer(metaFileAsByteBuffer);
        final String retainedMessagesPersistenceVersion = getStringFromBuffer(metaFileAsByteBuffer);
        final String subscriptionPersistenceVersion = getStringFromBuffer(metaFileAsByteBuffer);
        final String clientSessionPersistenceVersion = getStringFromBuffer(metaFileAsByteBuffer);
        final String queuedMessagesPersistenceVersion = getStringFromBuffer(metaFileAsByteBuffer);

        final PersistenceType retainedMessagePersistenceType = getTypeFromBuffer(metaFileAsByteBuffer);
        final PersistenceType publishPayloadPersistenceType = getTypeFromBuffer(metaFileAsByteBuffer);

        final MetaInformation metaInformation = new MetaInformation();

        metaInformation.setHivemqVersion(hivemqVersion);
        metaInformation.setClientSessionPersistenceVersion(clientSessionPersistenceVersion);
        metaInformation.setQueuedMessagesPersistenceVersion(queuedMessagesPersistenceVersion);
        metaInformation.setRetainedMessagesPersistenceVersion(retainedMessagesPersistenceVersion);
        metaInformation.setSubscriptionPersistenceVersion(subscriptionPersistenceVersion);
        metaInformation.setPublishPayloadPersistenceVersion(publishPayloadPersistenceVersion);

        metaInformation.setRetainedMessagesPersistenceType(retainedMessagePersistenceType);
        metaInformation.setPublishPayloadPersistenceType(publishPayloadPersistenceType);

        //always true at this point
        metaInformation.setMetaFilePresent(true);
        metaInformation.setPersistenceFolderPresent(true);
        metaInformation.setDataFolderPresent(true);

        return metaInformation;
    }

    private PersistenceType getTypeFromBuffer(final ByteBuffer metaFileAsByteBuffer) {
        final byte typeAsByte = metaFileAsByteBuffer.get();
        return typeAsByte > -1 ? PersistenceType.forCode(typeAsByte) : null;
    }

    @Nullable
    private String getStringFromBuffer(final ByteBuffer metaFileAsByteBuffer) {
        final int stringLength = metaFileAsByteBuffer.getInt();
        if (stringLength == 0) {
            return null;
        }
        final byte[] versionAsByteArray = new byte[stringLength];
        metaFileAsByteBuffer.get(versionAsByteArray, 0, stringLength);
        return new String(versionAsByteArray, UTF_8);
    }
}
