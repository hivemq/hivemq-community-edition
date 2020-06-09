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
package com.hivemq.bootstrap.ioc.lazysingleton;

import com.google.inject.Scope;

/**
 * A lazy Singleton Scope for objects which should <b>not</b> be created eagerly on application startup.
 * <p>
 * For true lazy behaviour, make sure to use a {@link javax.inject.Provider} for these lazy singleton
 * objects
 *
 * @author Dominik Obermaier
 */
public class LazySingletonScope {

    /* Singleton, we don't need more instances of it */
    private static final Scope instance = new LazySingletonScopeImpl();

    /**
     * Returns the LazySingletonScope
     *
     * @return the LazySingletonScope
     */
    public static Scope get() {
        return instance;
    }

    private LazySingletonScope() {
        //Don't instantiate, this is a singleton
    }
}
