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
package com.hivemq.extension.sdk.api.client.parameter;


import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.ThreadSafe;
import com.hivemq.extension.sdk.api.services.exception.LimitExceededException;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Optional;

/**
 * Through this service an extension can manage client connection attributes with the same lifetime as the connection.
 * <p>
 * The ConnectionAttributeStore is a key-value store for storing arbitrary data as additional information
 * within the MQTT client connection. All data is stored in-memory and the maximum amount of a single key-value
 * pair is 10 kilobytes.
 * <p>
 * A Connection Attribute is arbitrary binary data. For convenience purposes methods like {@link #putAsString(String,
 * String)}
 * are available in case String representations should be stored. If complex objects are desired to be stored in
 * the ConnectionAttributeStore, manual serialization and deserialization must be implemented by the extension
 * developer.
 * <p>
 * The ConnectionAttributeStore is useful for storing temporary data or data that needs to be cleaned up automatically
 * after
 * the MQTT client disconnected. This store is also useful for storing temporary information that needs to be shared
 * across
 * callbacks.
 * <p>
 * The ConnectionAttributeStore is thread safe.
 *
 * @author Silvio Giebl
 * @author Christoph Schäbel
 * @since 4.0.0
 */
@ThreadSafe
@DoNotImplement
public interface ConnectionAttributeStore {

    /**
     * Sets the given connection attribute for the connected client.
     *
     * @param key   The key of the connection attribute.
     * @param value The value of the connection attribute.
     * @throws LimitExceededException A {@link LimitExceededException} is thrown when the size of the passed value
     *                                exceeds the maximum allowed size of 10 kilobytes for the value.
     * @since 4.0.0
     */
    void put(@NotNull String key, @NotNull ByteBuffer value);

    /**
     * Sets the given connection attribute as UTF-8 String representation for the connected client.
     *
     * @param key   The key of the connection attribute.
     * @param value The value of the connection attribute as a string.
     * @throws LimitExceededException A {@link LimitExceededException} is thrown when the size of the passed value
     *                                exceeds the maximum allowed size of 10 kilobytes for the value.
     * @since 4.0.0
     */
    void putAsString(@NotNull String key, @NotNull String value);

    /**
     * Sets the given connection attribute as String representation for the connected client with a given charset.
     *
     * @param key     The key of the connection attribute.
     * @param value   The value of the connection attribute as a string with the given charset.
     * @param charset The {@link Charset} of the given value.
     * @throws LimitExceededException A {@link LimitExceededException} is thrown when the size of the passed value
     *                                exceeds the maximum allowed size of 10 kilobytes for the value.
     * @since 4.0.0
     */
    void putAsString(@NotNull String key, @NotNull String value, @NotNull Charset charset);

    /**
     * Retrieves the value of the connection attribute with the given key for the connected client.
     *
     * @param key The key of the connection attribute.
     * @return An {@link Optional} containing the value of the connection attribute if present.
     * @since 4.0.0
     */
    @NotNull Optional<ByteBuffer> get(@NotNull String key);

    /**
     * Retrieves the value of the connection attribute with the given key for the connected client as UTF-8 string.
     *
     * @param key The key of the connection attribute.
     * @return An {@link Optional} containing the value of the connection attribute as a string if present.
     * @since 4.0.0
     */
    @NotNull Optional<String> getAsString(@NotNull String key);

    /**
     * Retrieves the value of the connection attribute with the given key for the connected client as string with the
     * given charset.
     *
     * @param key     The key of the connection attribute.
     * @param charset The {@link Charset} of the value of the connection attribute.
     * @return An {@link Optional} containing the value of the connection attribute as a string with the given charset
     * if present.
     * @since 4.0.0
     */
    @NotNull Optional<String> getAsString(@NotNull String key, @NotNull Charset charset);

    /**
     * Retrieves all connection attributes for the connected client.
     *
     * @return An {@link Optional} containing all connection attributes as a map of key and value pairs if present.
     * @since 4.0.0
     */
    @NotNull Optional<Map<String, ByteBuffer>> getAll();

    /**
     * Removes the connection attribute with the given key for the connected client.
     *
     * @param key The key of the connection attribute.
     * @return An {@link Optional} containing the value of the removed connection attribute if it was present.
     * @since 4.0.0
     */
    @NotNull Optional<ByteBuffer> remove(@NotNull String key);

    /**
     * Clears all connection attributes for the connected client.
     *
     * @since 4.0.0
     */
    void clear();
}
