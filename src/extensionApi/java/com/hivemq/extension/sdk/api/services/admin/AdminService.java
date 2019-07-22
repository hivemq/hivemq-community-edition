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
package com.hivemq.extension.sdk.api.services.admin;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;

/**
 * This service provides general information about the state of this HiveMQ instance.
 *
 * @author Lukas Brandl
 * @since 4.2.0
 */
public interface AdminService {

    /**
     * @return Information about the HiveMQ instance the extension is running in.
     * @since 4.2.0
     */
    @NotNull
    ServerInformation getServerInformation();

    /**
     * @return Information about the current stage of this HiveMQ instance.
     * @since 4.2.0
     */
    @NotNull
    LifecycleStage getCurrentStage();

    /**
     * @return Information about the license that is used by HiveMQ.
     * @since 4.2.0
     */
    @NotNull
    LicenseInformation getLicenseInformation();

}
