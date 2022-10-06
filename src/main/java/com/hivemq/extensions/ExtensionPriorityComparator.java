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

import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.Comparator;

/**
 * @author Florian Limpöck
 */
public class ExtensionPriorityComparator implements Comparator<String> {

    private final @NotNull HiveMQExtensions hiveMQExtensions;

    public ExtensionPriorityComparator(@NotNull final HiveMQExtensions hiveMQExtensions) {
        this.hiveMQExtensions = hiveMQExtensions;
    }

    @Override
    public int compare(@NotNull final String o1, @NotNull final String o2) {

        if (o1.equals(o2)) {
            return 0;
        }

        final HiveMQExtension extension1 = hiveMQExtensions.getExtension(o1);
        final HiveMQExtension extension2 = hiveMQExtensions.getExtension(o2);

        if (extension1 == null && extension2 == null) {
            return 0;
        }
        if (extension1 == null) {
            return 1;
        }
        if (extension2 == null) {
            return -1;
        }

        if (extension1.getPriority() > extension2.getPriority()) {
            return -1;
        } else if (extension1.getPriority() < extension2.getPriority()) {
            return 1;
        } else {
            return o1.compareTo(o2);
        }
    }
}