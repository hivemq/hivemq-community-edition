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

package com.hivemq.extension.sdk.api.services.subscription;

/**
 * Enum to filter the subscriptions by type.
 *
 * @author Christoph Schäbel
 * @since 4.2.0
 */
public enum SubscriptionType {

    /**
     * Include individual and shared subscriptions.
     *
     * @since 4.2.0
     */
    ALL,

    /**
     * Only include individual subscriptions.
     *
     * @since 4.2.0
     */
    INDIVIDUAL,

    /**
     * Only include shared subscriptions.
     *
     * @since 4.2.0
     */
    SHARED
}
