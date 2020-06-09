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

import java.io.IOException;

import static com.hivemq.configuration.service.RestrictionsConfigurationService.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

@SuppressWarnings("UnstableApiUsage")
public class RestrictionConfiguratorTest extends AbstractConfigurationTest {

    @Test
    public void test_restrictions_xml() throws Exception {

        final String contents =
                "<hivemq>" +
                        "<restrictions>" +
                        "<max-connections>500</max-connections>" +
                        "<max-client-id-length>400</max-client-id-length>" +
                        "<max-topic-length>400</max-topic-length>" +
                        "<no-connect-idle-timeout>300</no-connect-idle-timeout>" +
                        "<incoming-bandwidth-throttling>200</incoming-bandwidth-throttling>" +
                        "</restrictions>" +
                        "</hivemq>";
        Files.write(contents.getBytes(UTF_8), xmlFile);

        reader.applyConfig();

        assertEquals(500, restrictionsConfigurationService.maxConnections());
        assertEquals(400, restrictionsConfigurationService.maxClientIdLength());
        assertEquals(400, restrictionsConfigurationService.maxTopicLength());
        assertEquals(300, restrictionsConfigurationService.noConnectIdleTimeout());
        assertEquals(200, restrictionsConfigurationService.incomingLimit());

    }

    @Test
    public void test_restriction_negative_values() throws Exception {

        final String contents =
                "<hivemq>" +
                        "<restrictions>" +
                        "<max-connections>-100</max-connections>" +
                        "<max-client-id-length>-100</max-client-id-length>" +
                        "<max-topic-length>-100</max-topic-length>" +
                        "<no-connect-idle-timeout>-100</no-connect-idle-timeout>" +
                        "<incoming-bandwidth-throttling>-100</incoming-bandwidth-throttling>" +
                        "</restrictions>" +
                        "</hivemq>";
        Files.write(contents.getBytes(UTF_8), xmlFile);

        reader.applyConfig();

        assertEquals(MAX_CONNECTIONS_DEFAULT, restrictionsConfigurationService.maxConnections());
        assertEquals(MAX_CLIENT_ID_LENGTH_DEFAULT, restrictionsConfigurationService.maxClientIdLength());
        assertEquals(MAX_TOPIC_LENGTH_DEFAULT, restrictionsConfigurationService.maxTopicLength());
        assertEquals(NO_CONNECT_IDLE_TIMEOUT_DEFAULT, restrictionsConfigurationService.noConnectIdleTimeout());
        assertEquals(INCOMING_BANDWIDTH_THROTTLING_DEFAULT, restrictionsConfigurationService.incomingLimit());

    }

    @Test
    public void test_tooHigh() throws IOException {
        final String contents =
                "<hivemq>" +
                        "<restrictions>" +
                        "<max-connections>500</max-connections>" +
                        "<max-client-id-length>123456</max-client-id-length>" +
                        "<max-topic-length>123456</max-topic-length>" +
                        "<no-connect-idle-timeout>300</no-connect-idle-timeout>" +
                        "<incoming-bandwidth-throttling>200</incoming-bandwidth-throttling>" +
                        "</restrictions>" +
                        "</hivemq>";
        Files.write(contents.getBytes(UTF_8), xmlFile);

        reader.applyConfig();

        assertEquals(500, restrictionsConfigurationService.maxConnections());
        assertEquals(MAX_CLIENT_ID_LENGTH_DEFAULT, restrictionsConfigurationService.maxClientIdLength());
        assertEquals(MAX_TOPIC_LENGTH_DEFAULT, restrictionsConfigurationService.maxTopicLength());
        assertEquals(300, restrictionsConfigurationService.noConnectIdleTimeout());
        assertEquals(200, restrictionsConfigurationService.incomingLimit());
    }
}