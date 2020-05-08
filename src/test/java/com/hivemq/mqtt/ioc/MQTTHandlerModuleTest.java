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

package com.hivemq.mqtt.ioc;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.message.dropping.MessageDroppedServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Georg Held
 */
public class MQTTHandlerModuleTest {

    private Injector injector;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                final Injector persistenceInjector = mock(Injector.class);
                when(persistenceInjector.getInstance(MessageDroppedService.class)).thenReturn(mock(MessageDroppedServiceImpl.class));
                install(new MQTTHandlerModule(persistenceInjector));
            }
        });
    }

    @Test
    public void test_message_dropped_service_is_singleton() {
        final MessageDroppedService instance1 = injector.getInstance(MessageDroppedService.class);
        final MessageDroppedService instance2 = injector.getInstance(MessageDroppedService.class);

        assertSame(instance1, instance2);

        assertTrue(instance1 instanceof MessageDroppedServiceImpl);
    }

}