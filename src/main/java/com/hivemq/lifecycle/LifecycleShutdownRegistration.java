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
package com.hivemq.lifecycle;

import com.hivemq.common.shutdown.ShutdownHooks;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

/**
 * @author Dominik Obermaier
 */
public class LifecycleShutdownRegistration {

    private final ShutdownHooks shutdownHooks;
    private final LifecycleHiveMQShutdownHook shutdownHook;


    @Inject
    LifecycleShutdownRegistration(final ShutdownHooks shutdownHooks, final LifecycleHiveMQShutdownHook shutdownHook) {
        this.shutdownHooks = shutdownHooks;
        this.shutdownHook = shutdownHook;
    }

    @PostConstruct
    public void postConstruct() {
        shutdownHooks.add(shutdownHook);
    }

}
