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
package com.hivemq.extensions.services.initializer;

import com.hivemq.extension.sdk.api.services.intializer.ClientInitializer;
import com.hivemq.extension.sdk.api.services.intializer.InitializerRegistry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.verify;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
@SuppressWarnings("NullabilityAnnotations")
public class InitializerRegistryImplTest {

    @Mock
    private Initializers initializers;

    private InitializerRegistry registry;

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);
        registry = new InitializerRegistryImpl(initializers);

    }

    @Test
    public void test_set() {

        final ClientInitializer clientInitializer = (input, pipeline) -> {
        };

        registry.setClientInitializer(clientInitializer);

        verify(initializers).addClientInitializer(clientInitializer);

    }

    @Test(expected = NullPointerException.class)
    public void test_set_null() {

        registry.setClientInitializer(null);

    }
}