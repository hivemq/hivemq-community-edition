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

/**
 * @author Lukas Brandl
 * @since 4.2.0
 */
public enum LifecycleStage {

    /**
     * HiveMQ is currently in the startup process. Listeners and clustering may not be available yet.
     *
     * @since 4.2.0
     */
    STARTING,

    /**
     * The HiveMQ startup process is complete. In this stage clients can connect to HiveMQ and the HiveMQ instance has
     * joined the cluster if there is a cluster.
     *
     * @since 4.2.0
     */
    STARTED_SUCCESSFULLY
}
