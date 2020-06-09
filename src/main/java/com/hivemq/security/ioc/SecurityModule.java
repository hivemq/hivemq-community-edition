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
package com.hivemq.security.ioc;

import com.hivemq.bootstrap.ioc.SingletonModule;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.security.ssl.SslContextFactory;
import com.hivemq.security.ssl.SslContextFactoryImpl;
import com.hivemq.security.ssl.SslContextStore;
import com.hivemq.security.ssl.SslFactory;

import java.util.concurrent.ScheduledExecutorService;

/**
 * @author Georg Held
 */
public class SecurityModule extends SingletonModule<Class<SecurityModule>> {

    public SecurityModule() {
        super(SecurityModule.class);
    }

    @Override
    protected void configure() {
        bind(SslFactory.class).in(LazySingleton.class);
        bind(SslContextStore.class).in(LazySingleton.class);

        bind(SslContextFactory.class).to(SslContextFactoryImpl.class);
        bind(ScheduledExecutorService.class).annotatedWith(Security.class)
                .toProvider(SecurityExecutorProvider.class)
                .in(LazySingleton.class);
    }
}
