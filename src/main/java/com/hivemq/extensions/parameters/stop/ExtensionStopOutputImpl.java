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

package com.hivemq.extensions.parameters.stop;

import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopOutput;

import java.util.Optional;

/**
 * @author Georg Held
 */
public class ExtensionStopOutputImpl implements ExtensionStopOutput {

    private Throwable throwable = null;

    @NotNull
    public Optional<Throwable> getThrowable() {
        return Optional.ofNullable(this.throwable);
    }

    public void setThrowable(@Nullable final Throwable throwable) {
        this.throwable = throwable;
    }
}
