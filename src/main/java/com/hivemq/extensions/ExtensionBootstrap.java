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

import com.hivemq.embedded.EmbeddedExtension;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * @author Christoph Schäbel
 * @author Georg Held
 */
public interface ExtensionBootstrap {

    /**
     * Starts HiveMQ's extension system and the runtime-reload for extensions.
     * <p/>
     * Already installed (and enabled) extensions are loaded and started here.
     * <p>
     * This method runs asynchronously and does NOT block until the extensions are started.
     */
    @NotNull CompletableFuture<Void> startExtensionSystem(@Nullable EmbeddedExtension embeddedExtension);

    /**
     * Stops all currently enabled HiveMQ extensions and the extension system.
     * <p>
     * This method blocks until the extensions are stopped.
     */
    void stopExtensionSystem();
}
