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
package com.hivemq.extensions.events;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.events.EventRegistry;
import com.hivemq.extension.sdk.api.events.client.ClientLifecycleEventListener;
import com.hivemq.extension.sdk.api.events.client.ClientLifecycleEventListenerProvider;
import com.hivemq.extension.sdk.api.events.client.parameters.ClientLifecycleEventListenerProviderInput;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.verify;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
@SuppressWarnings("ALL")
public class EventRegistryImplTest {

    @Mock
    private LifecycleEventListeners eventListeners;

    private EventRegistry registry;

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);
        registry = new EventRegistryImpl(eventListeners);

    }

    @Test
    public void test_set() {

        final ClientLifecycleEventListenerProvider provider = new ClientLifecycleEventListenerProvider() {
            @Override
            public @Nullable ClientLifecycleEventListener getClientLifecycleEventListener(@NotNull ClientLifecycleEventListenerProviderInput input) {
                return null;
            }
        };

        registry.setClientLifecycleEventListener(provider);

        verify(eventListeners).addClientLifecycleEventListenerProvider(provider);

    }

    @Test(expected = NullPointerException.class)
    public void test_set_null() {

        registry.setClientLifecycleEventListener(null);

    }

}