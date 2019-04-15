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

package com.hivemq.extensions.client.parameter;

import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.info.SystemInformationImpl;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
@SuppressWarnings("NullabilityAnnotations")
public class ServerInformationImplTest {

    private ServerInformation serverInformation;

    private SystemInformation systemInformation;

    @Before
    public void setUp() throws Exception {

        systemInformation = new SystemInformationImpl();
        serverInformation = new ServerInformationImpl(systemInformation);
    }

    @Test
    public void test_server_and_system_information_equal() {

        assertEquals(systemInformation.getDataFolder(), serverInformation.getDataFolder());
        assertEquals(systemInformation.getHiveMQHomeFolder(), serverInformation.getHomeFolder());
        assertEquals(systemInformation.getLogFolder(), serverInformation.getLogFolder());
        assertEquals(systemInformation.getExtensionsFolder(), serverInformation.getExtensionsFolder());
        assertEquals(systemInformation.getHiveMQVersion(), serverInformation.getVersion());

    }
}