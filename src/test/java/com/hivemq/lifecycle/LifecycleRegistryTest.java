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

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Test;

import javax.annotation.PreDestroy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class LifecycleRegistryTest {

    @Test
    public void test_is_singleton() throws Exception {
        final Injector injector = Guice.createInjector();
        final LifecycleRegistry instance = injector.getInstance(LifecycleRegistry.class);
        final LifecycleRegistry instance2 = injector.getInstance(LifecycleRegistry.class);

        assertSame(instance, instance2);
    }

    @Test
    public void test_is_predestroy_is_executed() throws Exception {
        final LifecycleRegistry registry = new LifecycleRegistry();

        final CountDownLatch latch = new CountDownLatch(1);
        class PredestroyClass {
            @PreDestroy
            public void preDestroy() {
                latch.countDown();
            }
        }

        registry.addPreDestroyMethod(PredestroyClass.class.getMethod("preDestroy"), new PredestroyClass());
        registry.executePreDestroy();

        assertEquals(true, latch.await(1, TimeUnit.SECONDS));
    }
}