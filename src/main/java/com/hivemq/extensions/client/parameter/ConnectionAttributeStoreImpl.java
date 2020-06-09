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
package com.hivemq.extensions.client.parameter;

import com.google.common.base.Preconditions;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionAttributeStore;
import com.hivemq.util.Bytes;
import io.netty.channel.Channel;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

/**
 * @author Silvio Giebl
 * @author Florian Limpöck
 */
public class ConnectionAttributeStoreImpl implements ConnectionAttributeStore {

    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private final @NotNull Channel channel;

    public ConnectionAttributeStoreImpl(@NotNull final Channel channel) {
        Preconditions.checkNotNull(channel, "channel must not be null");
        this.channel = channel;
    }

    @Override
    public void put(@NotNull final String key, @NotNull final ByteBuffer value) {
        Preconditions.checkNotNull(key, "Key of connection attribute must not be null.");
        Preconditions.checkNotNull(value, "Value of connection attribute must not be null.");

        ConnectionAttributes.getInstance(channel).put(key, value);
    }

    @Override
    public void putAsString(@NotNull final String key, @NotNull final String value) {
        Preconditions.checkNotNull(key, "Key of connection attribute must not be null.");
        Preconditions.checkNotNull(value, "Value of connection attribute must not be null.");

        putAsString(key, value, DEFAULT_CHARSET);
    }

    @Override
    public void putAsString(@NotNull final String key, @NotNull final String value, @NotNull final Charset charset) {
        Preconditions.checkNotNull(key, "Key of connection attribute must not be null.");
        Preconditions.checkNotNull(value, "Value of connection attribute must not be null.");
        Preconditions.checkNotNull(charset, "Charset of connection attribute must not be null.");

        put(key, ByteBuffer.wrap(value.getBytes(charset)));
    }

    @NotNull
    @Override
    public Optional<ByteBuffer> get(@NotNull final String key) {
        Preconditions.checkNotNull(key, "Key of connection attribute must not be null.");

        final ConnectionAttributes connectionAttributes = ConnectionAttributes.getInstanceIfPresent(channel);
        if (connectionAttributes == null) {
            return Optional.empty();
        }
        return connectionAttributes.get(key);
    }

    @NotNull
    @Override
    public Optional<String> getAsString(@NotNull final String key) {
        Preconditions.checkNotNull(key, "Key of connection attribute must not be null.");

        return getAsString(key, DEFAULT_CHARSET);
    }

    @NotNull
    @Override
    public Optional<String> getAsString(@NotNull final String key, @NotNull final Charset charset) {
        Preconditions.checkNotNull(key, "Key of connection attribute must not be null.");
        Preconditions.checkNotNull(charset, "Charset of connection attribute must not be null.");

        final byte[] bytes = Bytes.getBytesFromReadOnlyBuffer(get(key));

        if (bytes == null) {
            return Optional.empty();
        }
        return Optional.of(new String(bytes, charset));
    }

    @NotNull
    @Override
    public Optional<Map<String, ByteBuffer>> getAll() {
        final ConnectionAttributes connectionAttributes = ConnectionAttributes.getInstanceIfPresent(channel);
        if (connectionAttributes == null) {
            return Optional.empty();
        }
        return connectionAttributes.getAll();
    }

    @NotNull
    @Override
    public Optional<ByteBuffer> remove(@NotNull final String key) {
        Preconditions.checkNotNull(key, "Key of connection attribute must not be null.");

        final ConnectionAttributes connectionAttributes = ConnectionAttributes.getInstanceIfPresent(channel);
        if (connectionAttributes == null) {
            return Optional.empty();
        }
        return connectionAttributes.remove(key);
    }

    @Override
    public void clear() {
        final ConnectionAttributes connectionAttributes = ConnectionAttributes.getInstanceIfPresent(channel);
        if (connectionAttributes != null) {
            connectionAttributes.clear();
        }
    }

}
