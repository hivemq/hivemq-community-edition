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
package com.hivemq.extensions.auth.parameter;

import com.google.common.base.Preconditions;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.auth.parameter.ModifiableClientSettings;
import com.hivemq.extension.sdk.api.auth.parameter.OverloadProtectionThrottlingLevel;

/**
 * @author Lukas Brandl
 */
public class ModifiableClientSettingsImpl implements ModifiableClientSettings {

    private int receiveMaximum;
    private @NotNull OverloadProtectionThrottlingLevel overloadProtectionThrottlingLevel =
            OverloadProtectionThrottlingLevel.DEFAULT;
    private boolean modified = false;
    private @Nullable Long queueSizeMaximum;

    public ModifiableClientSettingsImpl(final int receiveMaximum, @Nullable final Long queueSizeMaximum) {
        this.receiveMaximum = receiveMaximum;
        this.queueSizeMaximum = queueSizeMaximum;
    }

    @Override
    public void setClientReceiveMaximum(final int receiveMaximum) {
        Preconditions.checkArgument(receiveMaximum >= 1, "Receive maximum must NOT be less than 1 was " + receiveMaximum + ".");
        Preconditions.checkArgument(receiveMaximum <= 65535, "Receive maximum must NOT be more than 65535 was " + receiveMaximum + ".");
        if (this.receiveMaximum == receiveMaximum) {
            return;
        }
        this.receiveMaximum = receiveMaximum;
        modified = true;
    }

    @Override
    public void setOverloadProtectionThrottlingLevel(final @NotNull OverloadProtectionThrottlingLevel level) {
        Preconditions.checkNotNull(level, "Overload protection throttling level must not be null");
        if (this.overloadProtectionThrottlingLevel == level) {
            return;
        }
        this.overloadProtectionThrottlingLevel = level;
        modified = true;
    }

    @Override
    public void setClientQueueSizeMaximum(final long queueSizeMaximum) {
        Preconditions.checkArgument(queueSizeMaximum >= 1, "Queue size maximum must NOT be less than 1, was " + queueSizeMaximum + ".");
        if (this.queueSizeMaximum != null && queueSizeMaximum == this.queueSizeMaximum) {
            return;
        }
        this.queueSizeMaximum = queueSizeMaximum;
        modified = true;
    }

    @Override
    public @NotNull OverloadProtectionThrottlingLevel getOverloadProtectionThrottlingLevel() {
        return overloadProtectionThrottlingLevel;
    }

    @Override
    public int getClientReceiveMaximum() {
        return receiveMaximum;
    }

    @Nullable
    public Long getQueueSizeMaximum() {
        return queueSizeMaximum;
    }

    public boolean isModified() {
        return modified;
    }
}
