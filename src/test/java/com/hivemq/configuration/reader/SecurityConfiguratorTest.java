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
package com.hivemq.configuration.reader;

import com.google.common.io.Files;
import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SecurityConfiguratorTest extends AbstractConfigurationTest {
    @Test
    public void test_security_xml() throws Exception {

        final String contents =
                "<hivemq>" +
                        "<security>" +
                        "<utf8-validation>" +
                        "<enabled>false</enabled>" +
                        "</utf8-validation>" +
                        "<allow-empty-client-id>" +
                        "<enabled>false</enabled>" +
                        "</allow-empty-client-id>" +
                        "<payload-format-validation>" +
                        "<enabled>true</enabled>" +
                        "</payload-format-validation>" +
                        "<allow-request-problem-information>" +
                        "<enabled>false</enabled>" +
                        "</allow-request-problem-information>" +
                        "</security>" +
                        "</hivemq>";
        Files.write(contents.getBytes(UTF_8), xmlFile);

        reader.applyConfig();

        assertFalse(securityConfigurationService.validateUTF8());
        assertFalse(securityConfigurationService.allowServerAssignedClientId());
        assertTrue(securityConfigurationService.payloadFormatValidation());
        assertFalse(securityConfigurationService.allowRequestProblemInformation());
    }


    @Test
    public void test_security_defaults() throws Exception {
        final String contents = "" +
                "<hivemq>" +
                "</hivemq>";

        Files.write(contents.getBytes(UTF_8), xmlFile);

        reader.applyConfig();

        assertTrue(securityConfigurationService.validateUTF8());
        assertTrue(securityConfigurationService.allowServerAssignedClientId());
        assertFalse(securityConfigurationService.payloadFormatValidation());
        assertTrue(securityConfigurationService.allowRequestProblemInformation());
    }

}