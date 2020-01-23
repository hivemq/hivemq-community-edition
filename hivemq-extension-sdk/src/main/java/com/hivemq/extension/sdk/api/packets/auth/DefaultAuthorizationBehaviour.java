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
package com.hivemq.extension.sdk.api.packets.auth;

import com.hivemq.extension.sdk.api.auth.Authorizer;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission;

/**
 * Default behaviour when no {@link Authorizer} or {@link TopicPermission} matched for the PUBLISH/SUBSCRIBE.
 *
 * @author Christoph Schäbel
 * @since 4.0.0
 */
public enum DefaultAuthorizationBehaviour {

    /**
     * When DENY is set and none of the default permission matches to
     * a PUBLISH topic or topic filter in a Subscription, then the client is not allowed to PUBLISH or SUBSCRIBE.
     *
     * @since 4.0.0
     */
    DENY,

    /**
     * When ALLOW is set and none of the default permission matches to
     * a PUBLISH topic or topic filter in a Subscription, then the client is allowed to PUBLISH or SUBSCRIBE.
     *
     * @since 4.0.0
     */
    ALLOW
}
