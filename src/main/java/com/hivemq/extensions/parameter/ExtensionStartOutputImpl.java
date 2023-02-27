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
package com.hivemq.extensions.parameter;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Georg Held
 */
public class ExtensionStartOutputImpl implements ExtensionStartOutput {

    private @Nullable String reason = null;

    @Override
    public void preventExtensionStartup(@NotNull final String reason) {
        checkNotNull(reason, "A reason for preventing an extension startup must be given.");
        this.reason = reason;
    }

    public @NotNull Optional<String> getReason() {
        return Optional.ofNullable(reason);
    }
}
