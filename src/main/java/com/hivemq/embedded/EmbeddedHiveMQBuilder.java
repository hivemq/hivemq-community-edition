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
package com.hivemq.embedded;

import com.hivemq.embedded.internal.EmbeddedHiveMQBuilderImpl;
import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.nio.file.Path;

@DoNotImplement
public interface EmbeddedHiveMQBuilder {

    static @NotNull EmbeddedHiveMQBuilder newBuilder() {
        return new EmbeddedHiveMQBuilderImpl();
    }

    @NotNull EmbeddedHiveMQBuilder withConfigurationFolder(@NotNull Path configFolder);

    @NotNull EmbeddedHiveMQBuilder withExtensionFolder(@NotNull Path extensionFolder);

    @NotNull EmbeddedHiveMQBuilder withDataFolder(@Nullable Path dataFolder);

    @NotNull EmbeddedHiveMQ build();
}
