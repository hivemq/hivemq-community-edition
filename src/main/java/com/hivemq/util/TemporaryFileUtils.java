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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * @author Florian Limpöck
 */
public class TemporaryFileUtils {

    private static final Logger log = LoggerFactory.getLogger(TemporaryFileUtils.class);

    private static final String TEMP_FOLDER_NAME = "tmp";

    /**
     * delete a '/tmp' folder in any giver data folder.
     *
     * @param dataFolder the folder to delete the temporary folder from.
     */
    public static void deleteTmpFolder(@NotNull final File dataFolder) {

        final String tmpFolder = dataFolder.getPath() + File.separator + TEMP_FOLDER_NAME;
        try {
            //ungraceful shutdown does not delete tmp folders, so we clean them up on broker start
            FileUtils.deleteDirectory(new File(tmpFolder));
        } catch (final IOException e) {
            //No error because it's not business breaking
            log.warn("The temporary folder could not be deleted ({}).", tmpFolder);
            if (log.isDebugEnabled()) {
                log.debug("Original Exception: ", e);
            }
        }

    }

}
