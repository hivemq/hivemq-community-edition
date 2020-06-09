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
package com.hivemq.mqtt.topic;

import com.hivemq.util.Bytes;

/**
 * @author Christoph Schäbel
 */
public class SubscriptionFlags {

    public static final int SHARED_SUBSCRIPTION = 1;
    public static final int RETAIN_AS_PUBLISHED = 2;
    public static final int NO_LOCAL = 3;

    public static byte getDefaultFlags(final boolean isSharedSubscription, final boolean retainAsPublished, final boolean noLocal) {
        byte flags = Bytes.setBit((byte) 0, SHARED_SUBSCRIPTION, isSharedSubscription);
        flags = Bytes.setBit(flags, RETAIN_AS_PUBLISHED, retainAsPublished);
        return Bytes.setBit(flags, NO_LOCAL, noLocal);
    }

}
