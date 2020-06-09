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
package com.hivemq.extensions.auth;

import com.google.common.base.Preconditions;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.async.Async;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.auth.parameter.EnhancedAuthOutput;
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.extension.sdk.api.packets.general.DisconnectedReasonCode;
import com.hivemq.extensions.auth.parameter.ModifiableClientSettingsImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.util.ReasonStrings;

import java.time.Duration;

/**
 * @author Silvio Giebl
 */
public class ReAuthOutput extends AuthOutput<EnhancedAuthOutput> implements EnhancedAuthOutput {

    private @NotNull Mqtt5DisconnectReasonCode reasonCode = Mqtt5DisconnectReasonCode.NOT_AUTHORIZED;
    private @NotNull Mqtt5DisconnectReasonCode timeoutReasonCode = Mqtt5DisconnectReasonCode.NOT_AUTHORIZED;

    public ReAuthOutput(
            final @NotNull PluginOutPutAsyncer asyncer,
            final boolean validateUTF8,
            final @NotNull ModifiableDefaultPermissions defaultPermissions,
            final @NotNull ModifiableClientSettingsImpl clientSettings,
            final int timeout) {

        super(asyncer, validateUTF8, defaultPermissions, clientSettings, timeout);
        setDefaultReasonStrings();
    }

    ReAuthOutput(final @NotNull AuthOutput<EnhancedAuthOutput> prevOutput) {
        super(prevOutput);
        setDefaultReasonStrings();
    }

    private void setDefaultReasonStrings() {
        reasonString = ReasonStrings.RE_AUTH_FAILED;
        timeoutReasonString = ReasonStrings.RE_AUTH_FAILED_EXTENSION_TIMEOUT;
    }

    @Override
    public void failAuthentication(final @NotNull DisconnectedReasonCode reasonCode) {
        final Mqtt5DisconnectReasonCode disconnectReasonCode = checkReasonCode(reasonCode);
        failAuthentication();
        this.reasonCode = disconnectReasonCode;
    }

    @Override
    public void failAuthentication(
            final @NotNull DisconnectedReasonCode reasonCode,
            final @Nullable String reasonString) {

        final Mqtt5DisconnectReasonCode disconnectReasonCode = checkReasonCode(reasonCode);
        failAuthentication(reasonString);
        this.reasonCode = disconnectReasonCode;
    }

    public @NotNull Async<EnhancedAuthOutput> async(
            final @NotNull Duration timeout,
            final @NotNull TimeoutFallback fallback,
            final @NotNull DisconnectedReasonCode reasonCode) {

        final Mqtt5DisconnectReasonCode disconnectReasonCode = checkReasonCode(reasonCode);
        final Async<EnhancedAuthOutput> async = async(timeout, fallback);
        timeoutReasonCode = disconnectReasonCode;
        return async;
    }

    public @NotNull Async<EnhancedAuthOutput> async(
            final @NotNull Duration timeout,
            final @NotNull TimeoutFallback fallback,
            final @NotNull DisconnectedReasonCode reasonCode,
            final @Nullable String reasonString) {

        final Mqtt5DisconnectReasonCode disconnectReasonCode = checkReasonCode(reasonCode);
        final Async<EnhancedAuthOutput> async = async(timeout, fallback, reasonString);
        timeoutReasonCode = disconnectReasonCode;
        return async;
    }

    @Override
    void failByTimeout() {
        super.failByTimeout();
        reasonCode = timeoutReasonCode;
    }

    @Override
    void failByUndecided() {
        super.failByUndecided();
        reasonCode = Mqtt5DisconnectReasonCode.NOT_AUTHORIZED;
        reasonString = ReasonStrings.RE_AUTH_FAILED_UNDECIDED;
    }

    @Override
    void failByThrowable(final @NotNull Throwable throwable) {
        super.failByThrowable(throwable);
        reasonCode = Mqtt5DisconnectReasonCode.UNSPECIFIED_ERROR;
        reasonString = ReasonStrings.RE_AUTH_FAILED_EXCEPTION;
    }

    @NotNull Mqtt5DisconnectReasonCode getReasonCode() {
        return reasonCode;
    }

    private static @NotNull Mqtt5DisconnectReasonCode checkReasonCode(
            final @NotNull DisconnectedReasonCode disconnectedReasonCode) {

        Preconditions.checkNotNull(disconnectedReasonCode, "Disconnected reason code must never be null");
        final Mqtt5DisconnectReasonCode disconnectReasonCode = Mqtt5DisconnectReasonCode.from(disconnectedReasonCode);
        Preconditions.checkArgument(disconnectReasonCode != null,
                "The disconnected reason code " + disconnectedReasonCode.name() +
                        " is not a DISCONNECT reason code and therefore must not be used during re-authentication.");
        Preconditions.checkArgument(disconnectReasonCode != Mqtt5DisconnectReasonCode.NORMAL_DISCONNECTION,
                "DISCONNECT reason code must not be NORMAL_DISCONNECTION for failed authentication");
        Preconditions.checkArgument(disconnectReasonCode.canBeSentByServer(),
                "The DISCONNECT reason code " + disconnectedReasonCode.name() + " cannot be sent by the server.");
        return disconnectReasonCode;
    }
}
