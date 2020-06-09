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
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.migration.Migrations;
import com.hivemq.util.LocalPersistenceFileUtil;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * @author Christoph Schäbel
 * @author Florian Limpöck
 */
public class MetaFileService {

    private static final String META_FOLDER_NAME = "metadata";
    private static final String META_FILE_NAME = "versions.hmq";
    public static final MetaInformationSerializer serializer = new MetaInformationSerializer();
    private static final Logger log = LoggerFactory.getLogger(MetaFileService.class);
    private static final Logger migrationlog = LoggerFactory.getLogger(Migrations.MIGRATION_LOGGER_NAME);

    @NotNull
    public static MetaInformation readMetaFile(final @NotNull SystemInformation systemInformation) {

        final File dataFolder = systemInformation.getDataFolder();
        final MetaInformation metaInformation = new MetaInformation();

        if (!dataFolder.exists()) {
            return metaInformation;
        }
        metaInformation.setDataFolderPresent(true);

        final File persistenceFolder = new File(dataFolder, LocalPersistenceFileUtil.PERSISTENCE_SUBFOLDER_NAME);
        if (!persistenceFolder.exists()) {
            return metaInformation;
        }
        metaInformation.setPersistenceFolderPresent(true);

        final File metaFile = getMetaFile(systemInformation);
        if (!metaFile.exists()) {
            return metaInformation;
        }
        metaInformation.setMetaFilePresent(true);

        try {
            final byte[] bytes = FileUtils.readFileToByteArray(metaFile);
            final MetaInformation metaInfo = serializer.deserialize(bytes);
            migrationlog.info("Read metadata file: {}", metaInfo);
            return metaInfo;
        } catch (final IOException e) {
            migrationlog.error("Not able to read metadata file", e);
            log.trace("Not able to read meta file", e);
            metaInformation.setMetaFilePresent(false);
            return metaInformation;
        }
    }

    public static void writeMetaFile(final @NotNull SystemInformation systemInformation, final @NotNull MetaInformation metaInformation) {

        final File metaFile = getMetaFile(systemInformation);
        try {
            FileUtils.writeByteArrayToFile(metaFile, serializer.serialize(metaInformation), false);
            migrationlog.info("Write metadata file: {}", metaInformation);
        } catch (final IOException e) {
            migrationlog.error("Not able to write metadata file", e);
            log.error("Not able to write metadata file, please check your file and folder permissions");
            log.debug("Original exception", e);
        }
    }

    @NotNull
    private static File getMetaFile(final SystemInformation systemInformation) {
        return new File(systemInformation.getDataFolder(), META_FOLDER_NAME + File.separator + META_FILE_NAME);
    }

}
