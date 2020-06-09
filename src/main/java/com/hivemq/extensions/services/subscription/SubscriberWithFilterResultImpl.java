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
package com.hivemq.extensions.services.subscription;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.services.subscription.SubscriberWithFilterResult;

/**
 * @author Christoph Schäbel
 */
public class SubscriberWithFilterResultImpl implements SubscriberWithFilterResult {

    @NotNull
    private final String clientId;

    public SubscriberWithFilterResultImpl(@NotNull final String clientId) {
        this.clientId = clientId;
    }

    /**
     * @return the subscribers MQTT client identifier
     */
    @NotNull
    public String getClientId() {
        return clientId;
    }
}
