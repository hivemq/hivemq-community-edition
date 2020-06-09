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

import java.io.IOException;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import static com.hivemq.util.ResourceUtils.getResource;

/**
 * @author Christoph Schäbel
 * @author Lukas Brandl
 */
public class ManifestUtils {

    public static String getValueFromManifest(final Class clazz, final String name) {

        try {

            final URL resource = getResource(clazz, "META-INF/MANIFEST.MF");

            if (resource == null) {
                return null;
            }

            final Manifest manifest = new Manifest(resource.openStream());
            // do stuff with it
            final Attributes attributes = manifest.getMainAttributes();
            return attributes.getValue(name);
        } catch (final IOException e) {
            return null;
        }
    }
}
