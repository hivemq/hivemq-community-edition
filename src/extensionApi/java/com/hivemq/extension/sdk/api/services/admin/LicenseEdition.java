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
 * The enum represents the edition for which HiveMQ is licensed.
 *
 * @author Lukas Brandl
 * @since 4.2.0
 */
public enum LicenseEdition {

    /**
     * The open source edition of HiveMQ.
     *
     * @since 4.2.0
     */
    COMMUNITY,

    /**
     * An otherwise commercial instance of HiveMQ that is running without a license file.
     *
     * @since 4.2.0
     */
    TRIAL,

    /**
     * Professional edition of HiveMQ.
     *
     * @since 4.2.0
     */
    PROFESSIONAL,

    /**
     * Enterprise edition of HiveMQ.
     *
     * @since 4.2.0
     */
    ENTERPRISE
}
