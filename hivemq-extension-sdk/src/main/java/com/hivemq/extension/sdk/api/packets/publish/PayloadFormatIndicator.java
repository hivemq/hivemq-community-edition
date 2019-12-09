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
package com.hivemq.extension.sdk.api.packets.publish;

/**
 * Indicator for the payload.
 *
 * @author Christoph Schäbel
 * @since 4.0.0
 */
public enum PayloadFormatIndicator {
    /**
     * The format of the bytes of the payload is not known.
     *
     * @since 4.0.0
     */
    UNSPECIFIED,
    /**
     * The format of the bytes of the payload is UTF-8.
     *
     * @since 4.0.0
     */
    UTF_8
}
