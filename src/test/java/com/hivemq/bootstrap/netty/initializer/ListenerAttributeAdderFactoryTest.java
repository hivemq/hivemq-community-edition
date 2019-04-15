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

package com.hivemq.bootstrap.netty.initializer;

import com.hivemq.configuration.service.entity.TcpListener;
import com.hivemq.configuration.service.impl.listener.InternalListenerConfigurationService;
import com.hivemq.configuration.service.impl.listener.ListenerConfigurationServiceImpl;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Dominik Obermaier
 */
public class ListenerAttributeAdderFactoryTest {

    private InternalListenerConfigurationService listenerConfigurationService;


    @Before
    public void setUp() throws Exception {

        listenerConfigurationService = new ListenerConfigurationServiceImpl();
    }

    @Test
    public void get_listener_not_cached() throws Exception {

        final ListenerAttributeAdderFactory factory = new ListenerAttributeAdderFactory(listenerConfigurationService);

        final ListenerAttributeAdderFactory.ListenerAttributeAdder adder = factory.get(new TcpListener(1883, "localhost"));
        final ListenerAttributeAdderFactory.ListenerAttributeAdder adder2 = factory.get(new TcpListener(1883, "localhost"));

        assertNotNull(adder);
        assertNotNull(adder2);

        //Since the adder is not cached we get a new version every time
        assertNotSame(adder, adder2);
    }

    @Test
    public void get_cached_listener() throws Exception {

        final TcpListener listener = new TcpListener(1883, "localhost");
        listenerConfigurationService.addListener(listener);

        final ListenerAttributeAdderFactory factory = new ListenerAttributeAdderFactory(listenerConfigurationService);

        final ListenerAttributeAdderFactory.ListenerAttributeAdder adder = factory.get(listener);
        final ListenerAttributeAdderFactory.ListenerAttributeAdder adder2 = factory.get(listener);

        assertSame(adder, adder2);
    }

    @Test
    public void get_cached_listener_update() throws Exception {

        final TcpListener listener = new TcpListener(1883, "localhost");
        final TcpListener listener2 = new TcpListener(1884, "localhost");
        listenerConfigurationService.addListener(listener);

        final ListenerAttributeAdderFactory factory = new ListenerAttributeAdderFactory(listenerConfigurationService);

        final ListenerAttributeAdderFactory.ListenerAttributeAdder adder = factory.get(listener2);
        final ListenerAttributeAdderFactory.ListenerAttributeAdder adder2 = factory.get(listener2);

        assertNotSame(adder, adder2);

        listenerConfigurationService.addListener(listener2);

        final ListenerAttributeAdderFactory.ListenerAttributeAdder adder3 = factory.get(listener2);
        final ListenerAttributeAdderFactory.ListenerAttributeAdder adder4 = factory.get(listener2);
        assertSame(adder3, adder4);
    }
}