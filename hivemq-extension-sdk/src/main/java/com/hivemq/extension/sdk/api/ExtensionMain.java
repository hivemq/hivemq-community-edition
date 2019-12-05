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
package com.hivemq.extension.sdk.api;


import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.Authenticator;
import com.hivemq.extension.sdk.api.auth.Authorizer;
import com.hivemq.extension.sdk.api.interceptor.Interceptor;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopOutput;

/**
 * The main starting point for each HiveMQ extension.
 * <p>
 * Each extension must implement at least this interface.
 * The implementation of this interface is used by HiveMQ to load and enable/disable an extension.
 *
 * @author Christoph Schäbel
 * @author Georg Held
 * @since 4.0.0
 */
public interface ExtensionMain {

    /**
     * This method is called by HiveMQ if an extension is enabled.<br/>
     * It can be used to setup the extension and register client interceptors and callbacks with HiveMQ.
     * <p>
     * If this method returns HiveMQ assumes that this extension is finished starting up.
     * <p>
     * There are multiple options when this method can be called by HiveMQ:<br/>
     * <ul>
     * <li>When HiveMQ starts up</li>
     * <li>When the extension is enabled at runtime due to an administrative action</li>
     * </ul>
     * It is possible to abort the extension start if the extension isn't ready for use (i.e. missing configuration
     * file) with
     * {@link ExtensionStartOutput#preventExtensionStartup(String)}.
     *
     * @param extensionStartInput  A {@link ExtensionStartInput}.
     * @param extensionStartOutput A {@link ExtensionStartOutput}.
     * @since 4.0.0
     */
    void extensionStart(@NotNull ExtensionStartInput extensionStartInput, @NotNull ExtensionStartOutput extensionStartOutput);

    /**
     * This method is called by HiveMQ if an extension is disabled.
     * It can be used to shut down the extension.<br/>
     * {@link Interceptor}s, {@link Authorizer} and {@link Authenticator} from this extension are automatically
     * de-registered by HiveMQ and do not need to be de-registered manually.
     * <p>
     * If this method returns HiveMQ assumes that this extension is finished shutting down.
     * <p>
     * There are multiple options when this method can be called by HiveMQ:<br/>
     * <ul>
     * <li>When HiveMQ shuts down</li>
     * <li>When the extension is disabled at runtime due to an administrative action</li>
     * </ul>
     *
     * @param extensionStopInput  A {@link ExtensionStartInput}
     * @param extensionStopOutput A {@link ExtensionStartOutput}
     * @since 4.0.0
     */
    void extensionStop(@NotNull ExtensionStopInput extensionStopInput, @NotNull ExtensionStopOutput extensionStopOutput);
}
