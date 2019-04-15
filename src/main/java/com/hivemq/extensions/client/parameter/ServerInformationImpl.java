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

package com.hivemq.extensions.client.parameter;

import com.hivemq.annotations.NotNull;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;

import javax.inject.Inject;
import java.io.File;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
@LazySingleton
public class ServerInformationImpl implements ServerInformation {

    @NotNull
    private final SystemInformation systemInformation;

    @Inject
    public ServerInformationImpl(@NotNull final SystemInformation systemInformation) {
        this.systemInformation = systemInformation;
    }

    @NotNull
    @Override
    public String getVersion() {
        return systemInformation.getHiveMQVersion();
    }

    @NotNull
    @Override
    public File getHomeFolder() {
        return systemInformation.getHiveMQHomeFolder();
    }

    @NotNull
    @Override
    public File getDataFolder() {
        return systemInformation.getDataFolder();
    }

    @NotNull
    @Override
    public File getLogFolder() {
        return systemInformation.getLogFolder();
    }

    @NotNull
    @Override
    public File getExtensionsFolder() {
        return systemInformation.getExtensionsFolder();
    }
}
