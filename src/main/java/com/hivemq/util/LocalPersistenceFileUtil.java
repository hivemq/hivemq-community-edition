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

import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.configuration.info.SystemInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;

/**
 * A utility for the local file persistences
 *
 * @author Dominik Obermaier
 */
@LazySingleton
public class LocalPersistenceFileUtil {

    public static final String PERSISTENCE_SUBFOLDER_NAME = "persistence";
    private static final Logger log = LoggerFactory.getLogger(LocalPersistenceFileUtil.class);


    private final SystemInformation systemInformation;

    @Inject
    LocalPersistenceFileUtil(final SystemInformation systemInformation) {
        this.systemInformation = systemInformation;
    }

    public synchronized File getLocalPersistenceFolder() {
        final File dataFolder = systemInformation.getDataFolder();

        final File persistenceFolder = new File(dataFolder, PERSISTENCE_SUBFOLDER_NAME);
        if (!persistenceFolder.exists()) {
            log.debug("Folder {} does not exist, trying to create it", persistenceFolder.getAbsolutePath());
            final boolean createdDirectory = persistenceFolder.mkdirs();
            if (createdDirectory) {
                log.debug("Created folder {}", dataFolder.getAbsolutePath());
            }
        }
        return persistenceFolder;
    }

    public synchronized File getVersionedLocalPersistenceFolder(final String persistence, final String version) {
        final File versionedFolder = new File(getLocalPersistenceFolder(), persistence + File.separator + version);
        if (!versionedFolder.exists()) {
            log.debug("Folder {} does not exist, trying to create it", versionedFolder.getAbsolutePath());
            versionedFolder.mkdirs();
        }
        return versionedFolder;
    }
}
