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

import com.hivemq.configuration.info.SystemInformation;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Random ID used for the anonymous statistics to correlate multiple information sets to one HiveMQ installation
 *
 * @author Christoph Schäbel
 */
@Singleton
public class HivemqId {

    private static final Logger log = LoggerFactory.getLogger(HivemqId.class);

    private final SystemInformation systemInformation;

    private String hivemqId;

    @Inject
    public HivemqId(final SystemInformation systemInformation) {
        this.systemInformation = systemInformation;
    }

    @PostConstruct
    void postConstruct() {
        readId();
    }

    private void readId() {

        final File file = new File(systemInformation.getDataFolder(), "meta.id");
        if (!file.exists()) {
            try {
                FileUtils.writeStringToFile(file, getHivemqId(), StandardCharsets.UTF_8);
                return;
            } catch (final IOException e) {
                log.debug("Not able to write installation ID to file {}", file.getAbsolutePath());
                log.trace("original exception");
            }
        }

        if (file.canRead()) {
            try {
                final String hivemqId = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

                //prevent manipulation
                if (hivemqId.length() != 36) {
                    return;
                }

                this.hivemqId = hivemqId;
            } catch (final IOException e) {
                log.debug("Not able to read installation ID from file {}", file.getAbsolutePath());
                log.trace("original exception");
            }
        }

    }

    public String getHivemqId() {
        if (hivemqId == null) {
            hivemqId = UUID.randomUUID().toString();
        }
        return hivemqId;
    }
}
