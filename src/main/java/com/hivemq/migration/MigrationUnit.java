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
package com.hivemq.migration;

import com.hivemq.extension.sdk.api.annotations.NotNull;

/**
 * @author Christoph Schäbel
 * @author Florian Limpöck
 */
public enum MigrationUnit {

    // As the MigrationUnit enum is used in a TreeSet the order of declaration is the order of persistence migration
    FILE_PERSISTENCE_PUBLISH_PAYLOAD("publish payload file persistence"),
    FILE_PERSISTENCE_RETAINED_MESSAGES("retained message file persistence"),
    PAYLOAD_ID_RETAINED_MESSAGES("retained message payload id"),
    PAYLOAD_ID_CLIENT_QUEUE("client queue payload id");

    private final @NotNull String description;

    MigrationUnit(final @NotNull String description) {
        this.description = description;
    }

    @NotNull
    @Override
    public String toString() {
        return description;
    }
}
