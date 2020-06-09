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
package com.hivemq.configuration.service.impl.listener;

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.configuration.service.entity.*;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static util.TlsTestUtil.createDefaultTLS;

public class ListenerConfigurationServiceImplTest {

    private ListenerConfigurationServiceImpl listenerConfigurationService;

    @Before
    public void setUp() throws Exception {

        listenerConfigurationService = new ListenerConfigurationServiceImpl();
    }

    /*
     * Adding listeners
     */

    @Test
    public void test_add_listeners() {

        final TcpListener tcpListener = new TcpListener(1883, "localhost");
        final WebsocketListener websocketListener = new WebsocketListener.Builder()
                .port(1884)
                .bindAddress("localhost")
                .build();

        final TlsTcpListener tlsTcpListener = new TlsTcpListener(1885, "localhost", createDefaultTLS());

        final TlsWebsocketListener tlsWebsocketListener = new TlsWebsocketListener.Builder()
                .port(1886)
                .bindAddress("localhost")
                .tls(createDefaultTLS())
                .build();


        listenerConfigurationService.addListener(tcpListener);
        listenerConfigurationService.addListener(websocketListener);
        listenerConfigurationService.addListener(tlsTcpListener);
        listenerConfigurationService.addListener(tlsWebsocketListener);

        final List<Listener> listeners = listenerConfigurationService.getListeners();

        assertEquals(4, listeners.size());

        assertEquals(1, listenerConfigurationService.getTcpListeners().size());
        assertEquals(1, listenerConfigurationService.getTlsTcpListeners().size());
        assertEquals(1, listenerConfigurationService.getWebsocketListeners().size());
        assertEquals(1, listenerConfigurationService.getTlsWebsocketListeners().size());

        assertSame(listenerConfigurationService.getTcpListeners().get(0), tcpListener);
        assertSame(listenerConfigurationService.getTlsTcpListeners().get(0), tlsTcpListener);
        assertSame(listenerConfigurationService.getWebsocketListeners().get(0), websocketListener);
        assertSame(listenerConfigurationService.getTlsWebsocketListeners().get(0), tlsWebsocketListener);

    }

    @Test(expected = IllegalArgumentException.class)
    public void test_add_invalid_listener_type() {

        listenerConfigurationService.addListener(new Listener() {
            @Override
            public int getPort() {
                return 0;
            }

            @Override
            public String getBindAddress() {
                return null;
            }

            @Override
            public String readableName() {
                return null;
            }

            @Override
            public @NotNull String getName() {
                return "name";
            }

        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_add_invalid_listener_type_subclass_of_tcplistener() {

        listenerConfigurationService.addListener(new TcpListener(1883, "localhost") {
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_add_invalid_listener_type_subclass_of_tlstcplistener() {

        listenerConfigurationService.addListener(new TlsTcpListener(1883, "localhost", createDefaultTLS()) {
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_add_invalid_listener_type_subclass_of_websocketlistener() {

        final WebsocketListener subclass = new WebsocketListener(123, null, null, false,
                null, null) {
        };

        listenerConfigurationService.addListener(subclass);
    }


    @Test
    public void test_get_listeners_immutable() {

        listenerConfigurationService.addListener(new TcpListener(1883, "localhost"));

        final List<Listener> listeners = listenerConfigurationService.getListeners();

        try {
            listeners.add(new TcpListener(1884, "localhost"));
            fail();
        } catch (final Exception e) {
            //Expected
        }

        try {
            listeners.clear();
            fail();
        } catch (final Exception e) {
            //Expected
        }

    }

    @Test(expected = NullPointerException.class)
    public void test_null_update_listener() {
        listenerConfigurationService.addUpdateListener(null);

    }

    @Test
    public void test_listener_callback_added_init_called() throws Exception {

        final CountDownLatch latch = new CountDownLatch(1);
        final TcpListener listener = new TcpListener(123, "localhost");
        listenerConfigurationService.addListener(listener);
        listenerConfigurationService.addUpdateListener(new InternalListenerConfigurationService.UpdateListener() {
            @Override
            public void onRegister(@NotNull final ImmutableList<Listener> allListeners) {

                assertEquals(1, allListeners.size());
                assertEquals(listener, allListeners.get(0));
                latch.countDown();
            }

            @Override
            public void update(@NotNull final Listener newListener, @NotNull final ImmutableList<Listener> allListeners) {

                //We fail the test if this method is called
                fail();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));

    }

    @Test
    public void test_listener_callback_added_after_init() throws Exception {

        final CountDownLatch latch = new CountDownLatch(1);
        final TcpListener listener = new TcpListener(123, "localhost");
        listenerConfigurationService.addUpdateListener(new InternalListenerConfigurationService.UpdateListener() {
            @Override
            public void onRegister(@NotNull final ImmutableList<Listener> allListeners) {

                assertEquals(0, allListeners.size());
            }

            @Override
            public void update(@NotNull final Listener newListener, @NotNull final ImmutableList<Listener> allListeners) {

                assertEquals(1, allListeners.size());
                assertEquals(listener, allListeners.get(0));
                assertEquals(listener, newListener);
                latch.countDown();

            }
        });
        listenerConfigurationService.addListener(listener);

        assertTrue(latch.await(3, TimeUnit.SECONDS));

    }
}