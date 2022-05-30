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
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extension.sdk.api.parameter.ExtensionInformation;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import org.mockito.Mockito;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * @author Georg Held
 */
abstract public class AbstractExtensionTest {

    protected @NotNull ExtensionStartOutput getTestExtensionStartOutput() {
        return reason -> {
        };
    }

    protected @NotNull ExtensionStartInput getTestExtensionStartInput() {
        return new ExtensionStartInput() {
            @Override
            public @NotNull ExtensionInformation getExtensionInformation() {
                return new ExtensionInformation() {
                    @Override
                    public @NotNull String getId() {
                        return "id";
                    }

                    @Override
                    public @NotNull String getName() {
                        return "name";
                    }

                    @Override
                    public @NotNull String getVersion() {
                        return "1";
                    }

                    @Override
                    public @NotNull Optional<String> getAuthor() {
                        return Optional.of("me");
                    }

                    @Override
                    public @NotNull File getExtensionHomeFolder() {
                        return new File("/tmp");
                    }
                };
            }

            @Override
            @SuppressWarnings("unchecked")
            public @NotNull Map<String, ExtensionInformation> getEnabledExtensions() {
                return Collections.EMPTY_MAP;
            }

            @Override
            public @NotNull ServerInformation getServerInformation() {
                return Mockito.mock(ServerInformation.class);
            }

            @Override
            public @NotNull Optional<String> getPreviousVersion() {
                return Optional.of("0");
            }
        };
    }
}
