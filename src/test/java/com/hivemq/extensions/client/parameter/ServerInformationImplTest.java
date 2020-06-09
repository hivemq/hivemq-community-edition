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
package com.hivemq.extensions.client.parameter;

import com.google.common.collect.ImmutableList;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.info.SystemInformationImpl;
import com.hivemq.configuration.service.entity.TcpListener;
import com.hivemq.configuration.service.entity.Tls;
import com.hivemq.configuration.service.entity.TlsTcpListener;
import com.hivemq.configuration.service.impl.listener.ListenerConfigurationService;
import com.hivemq.extension.sdk.api.client.parameter.Listener;
import com.hivemq.extension.sdk.api.client.parameter.ListenerType;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Iterator;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
@SuppressWarnings("NullabilityAnnotations")
public class ServerInformationImplTest {

    private ServerInformation serverInformation;

    private SystemInformation systemInformation;

    @Mock
    private ListenerConfigurationService listenerConfigurationService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        systemInformation = new SystemInformationImpl();
        serverInformation = new ServerInformationImpl(systemInformation, listenerConfigurationService);
    }

    @Test
    public void test_server_and_system_information_equal() {

        assertEquals(systemInformation.getDataFolder(), serverInformation.getDataFolder());
        assertEquals(systemInformation.getHiveMQHomeFolder(), serverInformation.getHomeFolder());
        assertEquals(systemInformation.getLogFolder(), serverInformation.getLogFolder());
        assertEquals(systemInformation.getExtensionsFolder(), serverInformation.getExtensionsFolder());
        assertEquals(systemInformation.getHiveMQVersion(), serverInformation.getVersion());

    }

    @Test
    public void test_get_listeners() {
        final TcpListener tcpListener = new TcpListener(1883, "127.0.0.1");
        final TlsTcpListener tlsTcpListener = new TlsTcpListener(1883, "127.0.0.1", mock(Tls.class));
        when(listenerConfigurationService.getListeners()).thenReturn(ImmutableList.of(tcpListener, tlsTcpListener));
        final Set<Listener> listeners = serverInformation.getListener();
        final Iterator<Listener> iterator = listeners.iterator();
        final Listener first = iterator.next();
        final Listener second = iterator.next();

        if (first.getListenerType() == ListenerType.TCP_LISTENER) {
            assertEquals(ListenerType.TLS_TCP_LISTENER, second.getListenerType());
        } else {
            assertEquals(ListenerType.TLS_TCP_LISTENER, first.getListenerType());
            assertEquals(ListenerType.TCP_LISTENER, second.getListenerType());
        }

    }
}