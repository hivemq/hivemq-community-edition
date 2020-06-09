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
package com.hivemq.diagnostic.data;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.net.NetworkInterface;

import static org.junit.Assert.assertNotNull;

/**
 * @author Florian Limpöck
 * @since 4.1.0
 */
@SuppressWarnings("NullabilityAnnotations")
public class NetworkInterfaceInformationTest {

    private NetworkInterfaceInformation networkInterfaceInformation;

    private NetworkInterface networkInterface;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        networkInterface = NetworkInterface.getByName("Does_Not_Exist");
        networkInterfaceInformation = new NetworkInterfaceInformation();
    }

    @Test
    public void test_getIsLoopback_failed() {
        assertNotNull(networkInterfaceInformation.getIsLoopback(networkInterface));
    }

    @Test
    public void test_getIsP2P_failed() {
        assertNotNull(networkInterfaceInformation.getIsP2P(networkInterface));
    }

    @Test
    public void test_getIsUp_failed() {
        assertNotNull(networkInterfaceInformation.getIsUp(networkInterface));
    }

    @Test
    public void test_getIsVirtual_failed() {
        assertNotNull(networkInterfaceInformation.getIsVirtual(networkInterface));
    }

    @Test
    public void test_getSupportsMulticast_failed() {
        assertNotNull(networkInterfaceInformation.getSupportsMulticast(networkInterface));
    }

    @Test
    public void test_getMTU_failed() {
        assertNotNull(networkInterfaceInformation.getMTU(networkInterface));
    }

    @Test
    public void test_getMacaddress_failed() {
        assertNotNull(networkInterfaceInformation.getMacaddress(networkInterface));
    }
}
