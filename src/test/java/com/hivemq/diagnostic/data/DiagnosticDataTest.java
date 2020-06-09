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

import com.hivemq.configuration.info.SystemInformationImpl;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class DiagnosticDataTest {


    SystemPropertyInformation systemPropertyInformation;

    HiveMQInformation hiveMQInformation;

    HiveMQSystemInformation systemInformation;

    NetworkInterfaceInformation networkInterfaceInformation;

    private DiagnosticData data;

    @Before
    public void setUp() throws Exception {

        systemPropertyInformation = new SystemPropertyInformation();
        hiveMQInformation = new HiveMQInformation(new SystemInformationImpl());
        systemInformation = new HiveMQSystemInformation();
        networkInterfaceInformation = new NetworkInterfaceInformation();

        data = new DiagnosticData(systemPropertyInformation, hiveMQInformation, systemInformation, networkInterfaceInformation);

    }

    @Test
    public void test_hivemq_diagnostics_all_headlines_available() throws Exception {
        final String diagnosticData = data.get();

        assertTrue(diagnosticData.contains("Generated at"));
        assertTrue(diagnosticData.contains("HiveMQ Information"));
        assertTrue(diagnosticData.contains("Java System Properties"));
        assertTrue(diagnosticData.contains("System Information"));
        assertTrue(diagnosticData.contains("Network Interfaces"));
    }

    @Test
    public void test_hivemq_diagnostics_data_available() throws Exception {

        final String diagnosticData = data.get();
        System.out.println(diagnosticData);

        assertTrue(diagnosticData.contains("HiveMQ Version"));
        assertTrue(diagnosticData.contains("os.name"));
        assertTrue(diagnosticData.contains("Available Processors"));
        assertTrue(diagnosticData.contains("MAC Address"));
    }

}