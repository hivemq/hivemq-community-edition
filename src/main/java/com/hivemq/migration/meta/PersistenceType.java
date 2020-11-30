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
package com.hivemq.migration.meta;

import com.hivemq.extension.sdk.api.annotations.NotNull;

/**
 * @author Florian Limpöck
 */
public enum PersistenceType {
    FILE, FILE_NATIVE;

    private static final PersistenceType DEFAULT = FILE_NATIVE;

    @NotNull
    public static PersistenceType forCode(final int code){
        if (code < 0 || code >= values().length) {
            throw new IllegalArgumentException("No persistence type found for code: " + code);
        }
        return values()[code];
    }

}
