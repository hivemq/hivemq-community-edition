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
package com.hivemq.extensions;

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Georg Held
 */
public class ExtensionUtil {

    private static final Logger log = LoggerFactory.getLogger(ExtensionUtil.class);

    static boolean isValidExtensionFolder(@NotNull final Path path, final boolean logErrors) {

        checkNotNull(path, "extension path must not be null");

        final File file = path.toFile();
        if (!file.isDirectory()) {
            return false;
        }

        final File[] content = file.listFiles();

        if (content == null || content.length < 1) {
            return false;
        }

        boolean xmlPresent = false;
        boolean jarPresent = false;

        for (final File subfile : content) {
            final String name = subfile.getName();

            if ("hivemq-extension.xml".equals(name)) {
                xmlPresent = true;
            }
            if (name.endsWith(".jar")) {
                jarPresent = true;
            }
        }

        if (xmlPresent && jarPresent) {
            return true;
        }
        if (!xmlPresent) {
            //obviously no extension folder.
            return false;
        }

        //at this point jar-file is always missing and xml-file is not.
        if (logErrors) {
            log.warn("Extension folder {} does not contain a .jar file, ignoring extension", path);
        }
        return false;

    }

    @NotNull
    public static List<Path> findAllExtensionFolders(@NotNull final Path extensionPath) throws IOException {

        checkNotNull(extensionPath, "provided extension folder path CAN NOT be null");

        final ImmutableList.Builder<Path> builder = ImmutableList.builder();
        try (final DirectoryStream<Path> stream = Files.newDirectoryStream(extensionPath)) {
            for (final Path path : stream) {
                if (ExtensionUtil.isValidExtensionFolder(path, true)) {
                    log.trace("Found extension folder {}", path.toString());
                    builder.add(path);
                }
            }
        }
        return builder.build();
    }

    public static boolean disableExtensionFolder(@NotNull final Path extensionFolderPath) throws IOException {

        final File disabledFile = extensionFolderPath.resolve("DISABLED").toFile();
        if (!disabledFile.exists()) {
            return disabledFile.createNewFile();
        }
        return true;

    }
}
