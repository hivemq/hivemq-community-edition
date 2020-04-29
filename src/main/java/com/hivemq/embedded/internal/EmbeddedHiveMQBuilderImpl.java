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

package com.hivemq.embedded.internal;

import com.hivemq.embedded.EmbeddedHiveMQ;
import com.hivemq.embedded.EmbeddedHiveMQBuilder;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

/**
 * @author Georg Held
 */
public class EmbeddedHiveMQBuilderImpl implements EmbeddedHiveMQBuilder {

    private static final @NotNull Logger log = LoggerFactory.getLogger(EmbeddedHiveMQBuilderImpl.class);
    private static final @NotNull String EMBEDDED_HIVEMQ_TEMPORARY_FOLDER = "EmbeddedHiveMQ-";

    private @Nullable Path conf;
    private @Nullable Path extensions = null;
    private @Nullable Path data = null;

    private final @NotNull String usedTemporaryFolder =
            EMBEDDED_HIVEMQ_TEMPORARY_FOLDER + "-" + String.format("%05d", new Random().nextInt(1000000));

    @Override
    public @NotNull EmbeddedHiveMQBuilder withConfigurationFolder(final @Nullable Path configFolder) {
        conf = configFolder;

        return this;
    }

    @Override
    public @NotNull EmbeddedHiveMQBuilder withExtensionFolder(final @Nullable Path extensionFolder) {
        extensions = extensionFolder;

        return this;
    }

    @Override
    public @NotNull EmbeddedHiveMQBuilder withDataFolder(final @Nullable Path dataFolder) {
        data = dataFolder;

        return this;
    }

    @Override
    public @NotNull EmbeddedHiveMQ build() {


        return new EmbeddedHiveMQImpl(conf, extensions, data);
    }

    private boolean verifyConfigFolder() {
        if (conf == null) {
            log.debug("No HiveMQ config folder was set for EmbeddedHiveMQ. Using default HiveMQ configuration.");
            return true;
        }

        boolean valid = Files.isDirectory(conf) && Files.isReadable(conf);
        if (valid && log.isTraceEnabled()) {
            log.trace("Found HiveMQ config folder for EmbeddedHiveMQ at \"{}\".", conf);
        }

        final Path configFile = conf.resolve("config.xml");
        valid &= Files.isRegularFile(configFile) && Files.isReadable(configFile);

        if (valid && log.isTraceEnabled()) {
            log.trace("Found HiveMQ config file for EmbeddedHiveMQ at \"{}\".", configFile);
        }

        return valid;
    }

    private boolean verifyExtensionFolder() throws IOException {
        if (extensions == null) {

            log.debug("No HiveMQ extension folder was set for EmbeddedHiveMQ.");
            return true;
        }

        // Needs to be writable for the DISABLED mechanic.
        final boolean valid = Files.isDirectory(extensions) && Files.isWritable(extensions);
        if (valid && log.isTraceEnabled()) {
            log.trace("Found HiveMQ extension folder for EmbeddedHiveMQ at \"{}\".", conf);
        }
        return valid;
    }

    private void createTemporaryDirectory() throws IOException {
        Files.createTempDirectory(usedTemporaryFolder);
//        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//            try {
//                Files.deleteIfExists(tempDirectory);
//            } catch (final IOException e) {
//                log.error(
//                        "EmbeddedHiveMQ could not clean up used temporary directory at \"{}\", because of \"{}\".",
//                        tempDirectory, e.getMessage());
//                log.debug("Original exception: ", e);
//            }
//        }));
    }
}
