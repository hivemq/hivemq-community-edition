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

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.Scopes;


/**
 * The actual Implementation of the LazySingleton. Just delegates to the
 * standard Guice singleton scope
 *
 * @author Dominik Obermaier
 */
class LazySingletonScopeImpl implements Scope {
    @Override
    public <T> Provider<T> scope(final Key<T> key, final Provider<T> unscoped) {
        return Scopes.SINGLETON.scope(key, unscoped);
    }

}