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
package com.hivemq.bootstrap.ioc;

import com.google.inject.AbstractModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A module which ensures that its {@link com.google.inject.AbstractModule#configure()} method is
 * only called once in the whole lifecycle of Guice for a given key. This ensures it's not problem to install
 * a module more than once.
 * <p>
 * Typically the key is the overriding class itself
 *
 * @author Dominik Obermaier
 */
public abstract class SingletonModule<T> extends AbstractModule {

    private static final Logger log = LoggerFactory.getLogger(SingletonModule.class);

    protected T key;

    public SingletonModule(final T key) {
        this.key = key;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof SingletonModule
                && ((SingletonModule) obj).key.equals(key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getName() + "(key=" + key.toString() + ")";
    }


    /**
     * Instantiates a class on startup of the dependency injection container.
     * <p>
     * This class is instantiated as singleton.
     * <p>
     * This method is most useful for application configuration and initialization logic
     *
     * @param clazz the class of the object to create on startup
     */
    public void instantiateOnStartup(final Class<?> clazz) {
        log.trace("Instantiating {} as eager singleton", clazz.getCanonicalName());
        bind(clazz).asEagerSingleton();
    }


}
