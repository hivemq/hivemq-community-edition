/*
 * Copyright 2019 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hivemq.configuration.info;

import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.io.File;

/**
 * Useful information about HiveMQ and the underlying system
 *
 * @author Christoph Schäbel
 * @since 3.0
 */
public interface SystemInformation {

    /**
     * @return the version string of HiveMQ
     */
    @NotNull
    String getHiveMQVersion();

    /**
     * @return the home folder of HiveMQ
     */
    @NotNull
    File getHiveMQHomeFolder();

    /**
     * /**
     *
     * @return the config folder of HiveMQ
     */
    @NotNull
    File getConfigFolder();

    /**
     * @return the log folder of HiveMQ
     */
    @NotNull
    File getLogFolder();

    /**
     * @return the data folder of HiveMQ
     */
    @NotNull
    File getDataFolder();

    /**
     * @return the extensions folder of HiveMQ
     */
    @NotNull
    File getExtensionsFolder();

    /**
     * @return the timestamp of HiveMQ start
     */
    long getRunningSince();

    /**
     * @return the count of CPUs HiveMQ uses
     */
    int getProcessorCount();

    /**
     * @return is HiveMQ running in embedded mode
     */
    boolean isEmbedded();
}
