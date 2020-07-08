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
package com.hivemq.util;

import com.google.inject.Singleton;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.exceptions.UnrecoverableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Util for handling system environment variables
 *
 * @author Christoph Schäbel
 */
@Singleton
public class EnvVarUtil {

    private static final Logger log = LoggerFactory.getLogger(EnvVarUtil.class);

    /**
     * Get a Java system property or system environment variable with the specified name.
     * If a variable with the same name exists in both targets the Java system property is returned.
     *
     * @param name the name of the environment variable
     * @return the value of the environment variable with the specified name
     */
    public @Nullable String getValue(final @NotNull String name) {
        //also check java properties if system variable is not found
        final String systemProperty = System.getProperty(name);
        if (systemProperty != null) {
            return systemProperty;
        }

        return System.getenv(name);
    }

    /**
     * Replaces placeholders like '${VAR_NAME}' with the according environment variables.
     *
     * @param text the text which contains placeholders (or not)
     * @return the text with all the placeholders replaced
     * @throws UnrecoverableException if a variable used in a placeholder is not set
     */
    public @NotNull String replaceEnvironmentVariablePlaceholders(final @NotNull String text) {

        final StringBuffer resultString = new StringBuffer();

        final Matcher matcher = Pattern.compile("\\$\\{(.*?)\\}")
                .matcher(text);

        while (matcher.find()) {

            if (matcher.groupCount() < 1) {
                //this should never happen
                log.warn("Found unexpected environment variable placeholder in config.xml");
                matcher.appendReplacement(resultString, "");
                continue;
            }

            final String varName = matcher.group(1);

            final String replacement = getValue(varName);

            if (replacement == null) {
                log.error("Environment Variable {} for HiveMQ config.xml is not set.", varName);
                throw new UnrecoverableException(false);
            }

            //sets replacement for this match
            matcher.appendReplacement(resultString, escapeReplacement(replacement));

        }

        //adds everything except the replacements to the string buffer
        matcher.appendTail(resultString);

        return resultString.toString();
    }

    private @NotNull String escapeReplacement(final @NotNull String replacement) {
        return replacement
                .replace("\\", "\\\\")
                .replace("$", "\\$");
    }
}
