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
package com.hivemq.extensions.handler.tasks;

import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectReasonCode;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;

/**
 * @author Christoph Schäbel
 */
public class PublishAuthorizerResult {

    private final @Nullable DisconnectReasonCode disconnectReasonCode;
    private final @Nullable AckReasonCode ackReasonCode;
    private final @Nullable String reasonString;
    private final boolean authorizerPresent;

    public PublishAuthorizerResult(@Nullable final AckReasonCode ackReasonCode,
                                   @Nullable final String reasonString,
                                   final boolean authorizerPresent,
                                   @Nullable final DisconnectReasonCode disconnectReasonCode) {
        this.ackReasonCode = ackReasonCode;
        this.reasonString = reasonString;
        this.authorizerPresent = authorizerPresent;
        this.disconnectReasonCode = disconnectReasonCode;
    }

    public PublishAuthorizerResult(@Nullable final AckReasonCode ackReasonCode,
                                   @Nullable final String reasonString,
                                   final boolean authorizerPresent) {
        this.ackReasonCode = ackReasonCode;
        this.reasonString = reasonString;
        this.authorizerPresent = authorizerPresent;
        this.disconnectReasonCode = null;
    }

    @Nullable
    public AckReasonCode getAckReasonCode() {
        return ackReasonCode;
    }

    @Nullable
    public String getReasonString() {
        return reasonString;
    }

    public boolean isAuthorizerPresent() {
        return authorizerPresent;
    }

    @Nullable
    public DisconnectReasonCode getDisconnectReasonCode() {
        return disconnectReasonCode;
    }
}
