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
package com.hivemq.extensions.services.interceptor;

import com.hivemq.extension.sdk.api.interceptor.connack.ConnackOutboundInterceptorProvider;
import com.hivemq.extension.sdk.api.interceptor.connect.ConnectInboundInterceptorProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

/**
 * @author Florian Limpöck
 * @since 4.2.0
 */
public class GlobalInterceptorRegistryImplTest {

    private GlobalInterceptorRegistryImpl globalInterceptorRegistry;

    @Mock
    private Interceptors interceptors;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        globalInterceptorRegistry = new GlobalInterceptorRegistryImpl(interceptors);
    }

    @Test(expected = NullPointerException.class)
    public void test_add_null_connect_inbound() {
        globalInterceptorRegistry.setConnectInboundInterceptorProvider(null);
    }

    @Test(expected = NullPointerException.class)
    public void test_add_null_connack_outbound() {
        globalInterceptorRegistry.setConnackOutboundInterceptorProvider(null);
    }

    @Test
    public void test_add_connect_inbound_success() {
        globalInterceptorRegistry.setConnectInboundInterceptorProvider(((e) -> null));
        verify(interceptors).addConnectInboundInterceptorProvider(any(ConnectInboundInterceptorProvider.class));
    }

    @Test
    public void test_add_connack_outbound_success() {
        globalInterceptorRegistry.setConnackOutboundInterceptorProvider(((e) -> null));
        verify(interceptors).addConnackOutboundInterceptorProvider(any(ConnackOutboundInterceptorProvider.class));
    }
}