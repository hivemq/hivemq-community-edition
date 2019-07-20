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
package com.hivemq.extension.sdk.api.auth.parameter;

/**
 * The enum is used to define how a client is affected be the overload protection.
 *
 * @author Lukas Brandl
 * @since 4.2.0
 */
public enum OverloadProtectionThrottlingLevel {

    /**
     * The amount of PUBLISH messages that each client can send is limited based on the resources of the HiveMQ cluster.
     */
    DEFAULT,

    /**
     * The amount of PUBLISH message that each client can send is NOT limited by the overload protection.
     * <b>ATTENTION:</b> Use this setting with extreme caution. Disabling the Overload Protection Mechanism potentially
     * decreases the HiveMQ resiliency against client misbehaviour. Disabling the overload protection may lead
     * to {@link OutOfMemoryError}.
     */
    NONE
}
