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
package com.hivemq.extensions.packets.publish;

import com.hivemq.codec.encoder.mqtt5.UnsignedDataTypes;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.annotations.ThreadSafe;
import com.hivemq.extension.sdk.api.packets.publish.ModifiableWillPublish;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author Lukas Brandl
 * @author Silvio Giebl
 */
@ThreadSafe
public class ModifiableWillPublishImpl extends ModifiablePublishPacketImpl implements ModifiableWillPublish {

    private long willDelay;

    public ModifiableWillPublishImpl(
            final @NotNull WillPublishPacketImpl willPublishPacket,
            final @NotNull FullConfigurationService configurationService) {

        super(willPublishPacket, configurationService);
        this.willDelay = willPublishPacket.willDelay;
    }

    @Override
    public long getWillDelay() {
        return willDelay;
    }

    @Override
    public void setWillDelay(final long willDelay) {
        checkArgument(willDelay >= 0, "Will delay must NOT be less than 0");
        checkArgument(willDelay < UnsignedDataTypes.UNSIGNED_INT_MAX_VALUE, "Will delay must be less than 4294967295");

        if (this.willDelay == willDelay) {
            return;
        }
        this.willDelay = willDelay;
        modified = true;
    }

    @Override
    public @NotNull WillPublishPacketImpl copy() {
        return new WillPublishPacketImpl(topic, qos, payload, retain, messageExpiryInterval, payloadFormatIndicator,
                contentType, responseTopic, correlationData, userProperties.copy(), willDelay, timestamp);
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ModifiableWillPublishImpl) || !super.equals(o)) {
            return false;
        }
        final ModifiableWillPublishImpl that = (ModifiableWillPublishImpl) o;
        return willDelay == that.willDelay;
    }

    @Override
    protected boolean canEqual(final @Nullable Object o) {
        return o instanceof ModifiableWillPublishImpl;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), willDelay);
    }
}
