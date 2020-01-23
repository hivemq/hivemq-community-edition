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
package com.hivemq.extension.sdk.api.auth;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.parameter.PublishAuthorizerInput;
import com.hivemq.extension.sdk.api.auth.parameter.PublishAuthorizerOutput;

/**
 * Interface for the PUBLISH authorization.
 * <p>
 * An Authorizer is always called by the same Thread for all messages from the same client.
 * <p>
 * If the same instance is shared between multiple clients it can be called in different Threads and must therefore be
 * thread-safe.
 * <p>
 *
 * @author Christoph Schäbel
 * @since 4.0.0
 */
public interface PublishAuthorizer extends Authorizer {

    /**
     * This method is called for every PUBLISH packet and Will Publish, that the {@link PublishAuthorizer} is delegated
     * to authorize.
     *
     * @param publishAuthorizerInput  The {@link PublishAuthorizerInput}.
     * @param publishAuthorizerOutput The {@link PublishAuthorizerOutput}.
     * @since 4.0.0
     */
    void authorizePublish(@NotNull PublishAuthorizerInput publishAuthorizerInput, @NotNull PublishAuthorizerOutput publishAuthorizerOutput);
}
