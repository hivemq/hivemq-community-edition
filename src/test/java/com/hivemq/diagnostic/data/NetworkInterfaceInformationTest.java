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

import java.net.NetworkInterface;

import static org.junit.Assert.*;

@SuppressWarnings("NullabilityAnnotations")
public class NetworkInterfaceInformationTest {

    private NetworkInterfaceInformation networkInterfaceInformation;

    private NetworkInterface networkInterface;

    @Before
    public void setUp() throws Exception {
        networkInterface = NetworkInterface.getByName("Does_Not_Exist");
        networkInterfaceInformation = new NetworkInterfaceInformation();
    }

    @Test
    public void getIsLoopback_whenUsingNameOfNonExistentInterface_thenErrorLineIsReturned() {
        final String networkInterfaceInfo = networkInterfaceInformation.getIsLoopback(networkInterface);
        assertNotNull(networkInterfaceInfo);
        assertEquals("Could not determine if interface is loopback interface", networkInterfaceInfo);
    }

    @Test
    public void getIsP2P_whenUsingNameOfNonExistentInterface_thenErrorLineIsReturned() {
        final String networkInterfaceInfo = networkInterfaceInformation.getIsP2P(networkInterface);
        assertNotNull(networkInterfaceInfo);
        assertEquals("Could not determine if interface is P2P interface", networkInterfaceInfo);
    }

    @Test
    public void getIsUp_whenUsingNameOfNonExistentInterface_thenErrorLineIsReturned() {
        final String networkInterfaceInfo = networkInterfaceInformation.getIsUp(networkInterface);
        assertNotNull(networkInterfaceInfo);
        assertEquals("Could not determine if interface is up", networkInterfaceInfo);
    }

    @Test
    public void getIsVirtual_whenUsingNameOfNonExistentInterface_thenErrorLineIsReturned() {
        final String networkInterfaceInfo = networkInterfaceInformation.getIsVirtual(networkInterface);
        assertNotNull(networkInterfaceInfo);
        assertEquals("Could not determine if interface is virtual", networkInterfaceInfo);
    }

    @Test
    public void getSupportsMulticast_whenUsingNameOfNonExistentInterface_thenErrorLineIsReturned() {
        final String networkInterfaceInfo = networkInterfaceInformation.getSupportsMulticast(networkInterface);
        assertNotNull(networkInterfaceInfo);
        assertEquals("Could not determine if interface supports multicast", networkInterfaceInfo);
    }

    @Test
    public void getMTU_whenUsingNameOfNonExistentInterface_thenErrorLineIsReturned() {
        final String networkInterfaceInfo = networkInterfaceInformation.getMTU(networkInterface);
        assertNotNull(networkInterfaceInfo);
        assertEquals("Could not determine MTU", networkInterfaceInfo);
    }

    @Test
    public void getMacAddress_whenUsingNameOfNonExistentInterface_thenErrorLineIsReturned() {
        final String networkInterfaceInfo = networkInterfaceInformation.getMacAddress(networkInterface);
        assertNotNull(networkInterfaceInfo);
        assertEquals("Could not determine MAC Address", networkInterfaceInfo);
    }

    @Test
    public void formatMACAddress_whenGivenBytes_thenMACAddressIsCorrectlyFormatted() {
        final byte[] bytes = new byte[]{-56, 42, 20, 83, 98, 102};

        final String macString = NetworkInterfaceInformation.formatMACAddress(bytes);

        assertEquals("C8-2A-14-53-62-66", macString);
    }

    @Test
    public void formatMACAddress_whenEmptyByteArray_thenIllegalArgumentExceptionThrown() {
        assertThrows(IllegalArgumentException.class, () -> NetworkInterfaceInformation.formatMACAddress(new byte[0]));
    }

    @Test
    public void formatMACAddress_whenByteArrayIsShorterThanMACLength_thenIllegalArgumentExceptionThrown() {
        assertThrows(IllegalArgumentException.class, () -> NetworkInterfaceInformation.formatMACAddress(new byte[5]));
    }

    @Test
    public void formatMACAddress_whenByteArrayIsLongerThanMACLength_thenIllegalArgumentExceptionThrown() {
        assertThrows(IllegalArgumentException.class, () -> NetworkInterfaceInformation.formatMACAddress(new byte[7]));
    }

    @Test
    public void formatMACAddress_whenNullPassedAsArgument_thenNullPointerExceptionThrown() {
        assertThrows(NullPointerException.class, () -> NetworkInterfaceInformation.formatMACAddress(null));
    }
}
