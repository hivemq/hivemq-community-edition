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

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Stage;
import org.junit.Test;

import javax.inject.Singleton;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;

/**
 * @author Dominik Obermaier
 */
public class LazySingletonModuleTest {

    @Test
    public void test_lazy_singleton_lazy_inition() throws Exception {
        Guice.createInjector(Stage.PRODUCTION, new LazySingletonModule(),

                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(LazySingletonClass.class);
                        bind(StandardSingleton.class);
                    }
                });

        assertEquals(true, StandardSingleton.executed.get());
        assertEquals(false, LazySingletonClass.executed.get());
    }


    @LazySingleton
    private static class LazySingletonClass {

        private static final AtomicBoolean executed = new AtomicBoolean(false);

        public LazySingletonClass() {
            executed.set(true);
        }
    }

    @Singleton
    private static class StandardSingleton {

        private static final AtomicBoolean executed = new AtomicBoolean(false);

        public StandardSingleton() {
            executed.set(true);
        }
    }
}