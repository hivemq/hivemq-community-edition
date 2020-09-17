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

package com.hivemq.embedded;

import com.hivemq.embedded.internal.EmbeddedHiveMQBuilderImpl;
import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.nio.file.Path;

@DoNotImplement
public interface EmbeddedHiveMQBuilder {

    /**
     * @return a new EmbeddedHiveMQBuilder.
     */
    static @NotNull EmbeddedHiveMQBuilder builder() {
        return new EmbeddedHiveMQBuilderImpl();
    }

    /**
     * Sets the HIVEMQ_CONFIG_FOLDER to the given argument. Using this method overrides all other ways to configure the
     * HIVEMQ_CONFIG_FOLDER.
     *
     * @param configFolder the used configuration folder.
     * @return this builder.
     * @see <a href="https://www.hivemq.com/docs/hivemq/latest/user-guide/configuration.html#folders">HiveMQ
     *         folders</a>
     */
    @NotNull EmbeddedHiveMQBuilder withConfigurationFolder(@Nullable Path configFolder);

    /**
     * Sets the HIVEMQ_DATA_FOLDER to the given argument. Using this method overrides all other ways to configure the
     * HIVEMQ_DATA_FOLDER.
     *
     * @param dataFolder the used data folder.
     * @return this builder.
     * @see <a href="https://www.hivemq.com/docs/hivemq/latest/user-guide/configuration.html#folders">HiveMQ
     *         folders</a>
     */
    @NotNull EmbeddedHiveMQBuilder withDataFolder(@Nullable Path dataFolder);

    /**
     * Sets the HIVEMQ_EXTENSION_FOLDER to the given argument. Using this method overrides all other ways to configure
     * it.
     *
     * @param extensionsFolder the used extensions folder.
     * @return this builder.
     * @see <a href="https://www.hivemq.com/docs/hivemq/latest/user-guide/configuration.html#folders">HiveMQ
     *         folders</a>
     */
    @NotNull EmbeddedHiveMQBuilder withExtensionsFolder(@Nullable Path extensionsFolder);

    /**
     * Sets the {@link EmbeddedExtension} to the given argument.
     *
     * @param embeddedExtension the used embedded extension.
     * @return this builder.
     */
    @NotNull EmbeddedHiveMQBuilder withEmbeddedExtension(@Nullable EmbeddedExtension embeddedExtension);

    /**
     * Concludes the EmbeddedHiveMQ build process.
     * <p>
     * Beware that this method sets the {@link com.hivemq.configuration.service.InternalConfigurations#AUTH_DENY_UNAUTHENTICATED_CONNECTIONS}
     * to false. If you do not want to start EmbeddedHiveMQ in this permissive mode, reset it to true before calling
     * {@link EmbeddedHiveMQ#start()}.
     *
     * @return a new EmbeddedHiveMQ with the configured HiveMQ folders.
     */
    @NotNull EmbeddedHiveMQ build();

}
