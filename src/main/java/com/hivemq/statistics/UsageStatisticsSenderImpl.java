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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * @author Christoph Schäbel
 */
public class UsageStatisticsSenderImpl implements UsageStatisticsSender {

    private static final Logger log = LoggerFactory.getLogger(UsageStatisticsSenderImpl.class);

    public void sendStatistics(final @Nullable String jsonPayload) {

        if (jsonPayload == null) {
            return;
        }

        final String url = getUrl();

        HttpURLConnection connection = null;
        try {

            final URL myurl = new URL(url);
            connection = (HttpURLConnection) myurl.openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);

            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("hmq-digest", BaseEncoding.base64().encode(Hashing.sha256().hashString(jsonPayload, StandardCharsets.UTF_8).asBytes()));
            connection.setRequestProperty("Content-Type", "application/json");

            try (final DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
                wr.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
                wr.flush();
            }

            final StringBuilder content;

            try (final BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {

                String line;
                content = new StringBuilder();

                while ((line = in.readLine()) != null) {
                    content.append(line);
                    content.append(System.lineSeparator());
                }
            }

            final int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                log.debug("Unable to send statistics to server, response code:  {}", responseCode);
                log.trace("response content: {}", content.toString());
            } else {
                log.debug("Sent anonymous usage statistics");
            }

            try {
                connection.getOutputStream().close();
            } catch (final Exception e) {
                //ignore
            }
            try {
                connection.getInputStream().close();
            } catch (final Exception e) {
                //ignore;
            }


        } catch (final Exception e) {

            if (connection != null) {

                try {
                    final OutputStream outputStream = connection.getOutputStream();
                    if (outputStream != null) outputStream.close();
                } catch (final Exception ex) {
                    //ignore
                }

                try {
                    final InputStream inputStream = connection.getInputStream();
                    if (inputStream != null) inputStream.close();
                } catch (final Exception ex) {
                    //ignore
                }
            }

            log.debug("Unable to send statistics to server: " + e.getMessage());
            log.trace("original exception", e);

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @VisibleForTesting
    @NotNull String getUrl() {
        return "https://analytics.hivemq.com/v1";
    }

}
