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
package com.hivemq.extension.sdk.api.packets.general;


import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;

/**
 * A representation for a user property.
 *
 * @author Christoph Schäbel
 * @since 4.0.0
 */
@DoNotImplement
public interface UserProperty {

    /**
     * @return The name of the user property.
     * @since 4.0.0
     */
    @NotNull String getName();

    /**
     * @return The value of the user property.
     * @since 4.0.0
     */
    @NotNull String getValue();
}
