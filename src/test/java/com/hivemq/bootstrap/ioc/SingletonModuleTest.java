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
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SingletonModuleTest {

    @Test
    public void test_standard_guice_module_executed_twice() throws Exception {
        final StandardModuleWithConfiguredMethod module1 = new StandardModuleWithConfiguredMethod();
        final StandardModuleWithConfiguredMethod module2 = new StandardModuleWithConfiguredMethod();

        Guice.createInjector(module1, module2);

        assertTrue(module1.accessed && module2.accessed);
    }

    @Test
    public void test_singleton_module_configured_executed_once() throws Exception {
        final SingletonModuleWithConfiguredMethod module1 = new SingletonModuleWithConfiguredMethod();
        final SingletonModuleWithConfiguredMethod module2 = new SingletonModuleWithConfiguredMethod();

        Guice.createInjector(module1, module2);

        assertTrue(module1.accessed ^ module2.accessed);
    }

    @Test
    public void test_singleton_provider_module_configured_executed_once() throws Exception {
        final SingletonProviderModuleWithConfiguredMethod module1 = new SingletonProviderModuleWithConfiguredMethod();
        final SingletonProviderModuleWithConfiguredMethod module2 = new SingletonProviderModuleWithConfiguredMethod();

        final Injector injector = Guice.createInjector(module1, module2);
        injector.getInstance(String.class);
        injector.getInstance(String.class);

        assertTrue(module1.accessed ^ module2.accessed);
        assertTrue(module1.provided ^ module2.provided);
    }


    @Test
    public void test_instantiate_on_startup() throws Exception {

        @SuppressWarnings("unchecked")
        class EagerModule extends SingletonModule {

            public EagerModule() {
                super(EagerModule.class);
            }

            @Override
            protected void configure() {
                instantiateOnStartup(OnStartup.class);
            }
        }

        final EagerModule eagerModule = new EagerModule();

        Guice.createInjector(eagerModule);


        assertEquals(true, OnStartup.executed.get());
        assertTrue(eagerModule.toString().contains("key=class"));


    }

    @SuppressWarnings("unchecked")
    private static class SingletonModuleWithConfiguredMethod extends SingletonModule {

        public boolean accessed = false;

        public SingletonModuleWithConfiguredMethod() {
            super(SingletonModuleWithConfiguredMethod.class);
        }

        @Override
        protected void configure() {
            accessed = true;
        }
    }

    @SuppressWarnings("unchecked")
    private static class SingletonProviderModuleWithConfiguredMethod extends SingletonModule {

        public boolean accessed = false;
        public boolean provided = false;

        public SingletonProviderModuleWithConfiguredMethod() {
            super(SingletonModuleWithConfiguredMethod.class);
        }

        @Override
        protected void configure() {
            accessed = true;
        }

        @Provides
        public String provideString() {
            provided = true;
            return "blub";
        }
    }

    private static class StandardModuleWithConfiguredMethod extends AbstractModule {

        public boolean accessed = false;

        @Override
        protected void configure() {
            accessed = true;
        }
    }

    private static class OnStartup {

        final public static AtomicBoolean executed = new AtomicBoolean(false);

        public OnStartup() {
            executed.set(true);
        }
    }
}