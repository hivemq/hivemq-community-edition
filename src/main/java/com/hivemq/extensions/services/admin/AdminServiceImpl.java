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
package com.hivemq.extensions.services.admin;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extension.sdk.api.services.admin.AdminService;
import com.hivemq.extension.sdk.api.services.admin.LicenseEdition;
import com.hivemq.extension.sdk.api.services.admin.LicenseInformation;
import com.hivemq.extension.sdk.api.services.admin.LifecycleStage;

import javax.inject.Inject;

/**
 * @author Lukas Brandl
 */
public class AdminServiceImpl implements AdminService {

    @NotNull
    private final ServerInformation serverInformation;

    @NotNull
    private LifecycleStage lifecycleStage = LifecycleStage.STARTING;

    @Inject
    public AdminServiceImpl(@NotNull final ServerInformation serverInformation) {
        this.serverInformation = serverInformation;
    }

    public void hivemqStarted() {
        lifecycleStage = LifecycleStage.STARTED_SUCCESSFULLY;
    }

    @NotNull
    @Override
    public ServerInformation getServerInformation() {
        return serverInformation;
    }

    @NotNull
    @Override
    public LifecycleStage getCurrentStage() {
        return lifecycleStage;
    }

    @NotNull
    @Override
    public LicenseInformation getLicenseInformation() {
        return new LicenseInformationImpl(LicenseEdition.COMMUNITY);
    }

}
