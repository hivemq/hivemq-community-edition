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
package com.hivemq.statistics;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import util.RandomPortGenerator;

import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * @author Christoph Schäbel
 */
public class UsageStatisticsSenderImplTest {

    private final int port = RandomPortGenerator.get();

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(port);

    private UsageStatisticsSender sender;


    @Before
    public void before() {
        sender = new UsageStatisticsSenderImpl() {

            @NotNull
            @Override
            String getUrl() {
                return "http://localhost:" + port + "/api/test";
            }
        };
    }

    @Test
    public void test_send_success() {

        stubFor(post(urlEqualTo("/api/test"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("OK")));

        sender.sendStatistics("payload");

        final String digest = BaseEncoding.base64().encode(Hashing.sha256().
                hashString("payload", StandardCharsets.UTF_8).asBytes());

        verify(postRequestedFor(urlMatching("/api/test"))
                .withRequestBody(matching("payload"))
                .withHeader("Content-Type", matching("application/json"))
                .withHeader("hmq-digest", matching(digest)));

    }

    @Test
    public void test_send_fail() {

        stubFor(post(urlEqualTo("/api/test"))
                .willReturn(aResponse()
                        .withStatus(403)
                        .withBody("NOT OK")));

        sender.sendStatistics("payload");

        final String digest = BaseEncoding.base64().encode(Hashing.sha256().
                hashString("payload", StandardCharsets.UTF_8).asBytes());

        //we don't expect any exceptions
    }


}