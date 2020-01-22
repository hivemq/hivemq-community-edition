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

package com.hivemq.util;

import com.hivemq.extension.sdk.api.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * @author Florian Limpöck
 */
public class ResourceUtils {

    private static final Logger log = LoggerFactory.getLogger(ResourceUtils.class);

    @Nullable
    public static URL getResource(final Class clazz, final String resourcePath) {

        try {

            if (clazz == null || resourcePath == null) {
                return null;
            }

            final ClassLoader cl = clazz.getClassLoader();
            final Enumeration<URL> resources = cl.getResources(resourcePath);
            final List<URL> urls = new ArrayList<>();
            // There could be multiple jar file (windows service for example)
            while (resources.hasMoreElements()) {
                urls.add(resources.nextElement());
            }
            if (urls.isEmpty()) {
                // There is no Resource
                return null;
            }
            URL url = null;
            if (urls.size() == 1) {
                // There is only one jar file
                url = urls.get(0);
            } else {
                for (final URL currentUrl : urls) {
                    if (currentUrl.getPath().contains("hivemq.jar")) {
                        // If there are multiple jar files, we pick the one that contains the substring "hivemq.jar"
                        url = currentUrl;
                        break;
                    }
                }
                if (url == null) {
                    // If non of the urls contains the substring "hivemq.jar" we return the first one
                    url = urls.get(0);
                }
            }

            return url;
        } catch (final IOException e) {
            log.warn("Could not read resource " + resourcePath);
            log.debug("Original exception: ", e);
            return null;
        }

    }
}
