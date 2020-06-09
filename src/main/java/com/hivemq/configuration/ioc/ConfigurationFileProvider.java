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
package com.hivemq.configuration.ioc;

import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.reader.ConfigurationFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * The Guice provider which creates a {@link com.hivemq.configuration.reader.ConfigurationFile}
 *
 * @author Dominik Obermaier
 */
public class ConfigurationFileProvider {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationFileProvider.class);


    public static ConfigurationFile get(final SystemInformation systemInformation) {

        final File configFileFolder = systemInformation.getConfigFolder();

        final boolean configFolderOk = checkConfigFolder(configFileFolder);

        final File configFile = new File(configFileFolder, "config.xml");

        boolean configFileOk = false;
        if (configFolderOk) {
            configFileOk = configFileOk(configFile);
        }

        if (configFolderOk && configFileOk) {

            return new ConfigurationFile(configFile);
        } else {
            return new ConfigurationFile(null);
        }
    }

    private static boolean configFileOk(final File configFile) {
        boolean configFileOk = false;
        if (!configFile.exists()) {
            log.error("The configuration file {} does not exist. Using HiveMQ default config", configFile.getAbsolutePath());
        } else if (!configFile.isFile()) {
            log.error("The configuration file {} is not file. Using HiveMQ default config", configFile.getAbsolutePath());
        } else if (!configFile.canRead()) {
            log.error("The configuration file {} cannot be read by HiveMQ. Using HiveMQ default config", configFile.getAbsolutePath());
        } else {
            configFileOk = true;
            if (!configFile.canWrite()) {
                log.warn("The configuration file {} is read only and cannot be written by HiveMQ.", configFile.getAbsolutePath());
            }
        }

        return configFileOk;
    }

    private static boolean checkConfigFolder(final File configFileFolder) {
        boolean configFolderOk = false;
        if (!configFileFolder.exists()) {
            log.error("The configuration file folder {} does not exist. Using HiveMQ default config", configFileFolder.getAbsolutePath());
        } else if (!configFileFolder.isDirectory()) {
            log.error("The configuration file folder {} is not a folder. Using HiveMQ default config", configFileFolder.getAbsolutePath());
        } else if (!configFileFolder.canRead()) {
            log.error("The configuration file folder {} cannot be read by HiveMQ. Using HiveMQ default config", configFileFolder.getAbsolutePath());
        } else {
            configFolderOk = true;
        }
        return configFolderOk;
    }
}
