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
import com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode;
import com.hivemq.extension.sdk.api.packets.general.DisconnectedReasonCode;
import com.hivemq.extensions.auth.parameter.ModifiableClientSettingsImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.util.ReasonStrings;

import java.nio.ByteBuffer;
import java.time.Duration;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Silvio Giebl
 */
public class ConnectAuthOutput extends AuthOutput<EnhancedAuthOutput> implements EnhancedAuthOutput {

    private @NotNull Mqtt5ConnAckReasonCode reasonCode = Mqtt5ConnAckReasonCode.NOT_AUTHORIZED;
    private @NotNull Mqtt5ConnAckReasonCode timeoutReasonCode = Mqtt5ConnAckReasonCode.NOT_AUTHORIZED;
    private final boolean supportsEnhancedAuth;

    public ConnectAuthOutput(
            final @NotNull PluginOutPutAsyncer asyncer,
            final boolean validateUTF8,
            final @NotNull ModifiableDefaultPermissions defaultPermissions,
            final @NotNull ModifiableClientSettingsImpl clientSettings,
            final int timeout,
            final boolean supportsEnhancedAuth) {

        super(asyncer, validateUTF8, defaultPermissions, clientSettings, timeout);
        this.supportsEnhancedAuth = supportsEnhancedAuth;
        setDefaultReasonStrings();
    }

    ConnectAuthOutput(final @NotNull ConnectAuthOutput prevOutput) {
        super(prevOutput);
        supportsEnhancedAuth = prevOutput.supportsEnhancedAuth;
        setDefaultReasonStrings();
    }

    private void setDefaultReasonStrings() {
        reasonString = ReasonStrings.AUTH_FAILED;
        timeoutReasonString = ReasonStrings.AUTH_FAILED_EXTENSION_TIMEOUT;
    }

    @Override
    public void authenticateSuccessfully(final @NotNull ByteBuffer authenticationData) {
        checkEnhancedAuthSupport();
        super.authenticateSuccessfully(authenticationData);
    }

    @Override
    public void authenticateSuccessfully(final @NotNull byte[] authenticationData) {
        checkEnhancedAuthSupport();
        super.authenticateSuccessfully(authenticationData);
    }

    @Override
    public void continueAuthentication() {
        checkEnhancedAuthSupport();
        super.continueAuthentication();
    }

    private void checkEnhancedAuthSupport() {
        if (!supportsEnhancedAuth) {
            throw new UnsupportedOperationException(
                    "Continue authentication is not supported as the client does not support enhanced authentication.");
        }
    }

    public void failAuthentication(final @NotNull ConnackReasonCode reasonCode) {
        final Mqtt5ConnAckReasonCode connAckReasonCode = checkReasonCode(reasonCode);
        failAuthentication();
        this.reasonCode = connAckReasonCode;
    }

    public void failAuthentication(final @NotNull ConnackReasonCode reasonCode, final @Nullable String reasonString) {
        final Mqtt5ConnAckReasonCode connAckReasonCode = checkReasonCode(reasonCode);
        failAuthentication(reasonString);
        this.reasonCode = connAckReasonCode;
    }

    @Override
    public void failAuthentication(final @NotNull DisconnectedReasonCode reasonCode) {
        final Mqtt5ConnAckReasonCode connAckReasonCode = checkReasonCode(reasonCode);
        failAuthentication();
        this.reasonCode = connAckReasonCode;
    }

    @Override
    public void failAuthentication(
            final @NotNull DisconnectedReasonCode reasonCode,
            final @Nullable String reasonString) {

        final Mqtt5ConnAckReasonCode connAckReasonCode = checkReasonCode(reasonCode);
        failAuthentication(reasonString);
        this.reasonCode = connAckReasonCode;
    }

    public @NotNull Async<EnhancedAuthOutput> async(
            final @NotNull Duration timeout,
            final @NotNull TimeoutFallback fallback,
            final @NotNull ConnackReasonCode reasonCode) {

        final Mqtt5ConnAckReasonCode connAckReasonCode = checkReasonCode(reasonCode);
        final Async<EnhancedAuthOutput> async = async(timeout, fallback);
        timeoutReasonCode = connAckReasonCode;
        return async;
    }

    public @NotNull Async<EnhancedAuthOutput> async(
            final @NotNull Duration timeout,
            final @NotNull TimeoutFallback fallback,
            final @NotNull ConnackReasonCode reasonCode,
            final @Nullable String reasonString) {

        final Mqtt5ConnAckReasonCode connAckReasonCode = checkReasonCode(reasonCode);
        final Async<EnhancedAuthOutput> async = async(timeout, fallback, reasonString);
        timeoutReasonCode = connAckReasonCode;
        return async;
    }

    @Override
    public @NotNull Async<EnhancedAuthOutput> async(
            final @NotNull Duration timeout,
            final @NotNull TimeoutFallback fallback,
            final @NotNull DisconnectedReasonCode reasonCode) {

        final Mqtt5ConnAckReasonCode connAckReasonCode = checkReasonCode(reasonCode);
        final Async<EnhancedAuthOutput> async = async(timeout, fallback);
        timeoutReasonCode = connAckReasonCode;
        return async;
    }

    @Override
    public @NotNull Async<EnhancedAuthOutput> async(
            final @NotNull Duration timeout,
            final @NotNull TimeoutFallback fallback,
            final @NotNull DisconnectedReasonCode reasonCode,
            final @Nullable String reasonString) {

        final Mqtt5ConnAckReasonCode connAckReasonCode = checkReasonCode(reasonCode);
        final Async<EnhancedAuthOutput> async = async(timeout, fallback, reasonString);
        timeoutReasonCode = connAckReasonCode;
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
        reasonCode = Mqtt5ConnAckReasonCode.NOT_AUTHORIZED;
        reasonString = ReasonStrings.AUTH_FAILED_UNDECIDED;
    }

    @Override
    void failByThrowable(final @NotNull Throwable throwable) {
        super.failByThrowable(throwable);
        reasonCode = Mqtt5ConnAckReasonCode.UNSPECIFIED_ERROR;
        reasonString = ReasonStrings.AUTH_FAILED_EXCEPTION;
    }

    @NotNull Mqtt5ConnAckReasonCode getReasonCode() {
        return reasonCode;
    }

    private static @NotNull Mqtt5ConnAckReasonCode checkReasonCode(final @NotNull ConnackReasonCode reasonCode) {
        checkNotNull(reasonCode, "CONNACK reason code must never be null");
        checkArgument(
                reasonCode != ConnackReasonCode.SUCCESS,
                "CONNACK reason code must not be SUCCESS for failed authentication");
        return Mqtt5ConnAckReasonCode.from(reasonCode);
    }

    private static @NotNull Mqtt5ConnAckReasonCode checkReasonCode(
            final @NotNull DisconnectedReasonCode disconnectedReasonCode) {

        Preconditions.checkNotNull(disconnectedReasonCode, "Disconnected reason code must never be null");
        final Mqtt5ConnAckReasonCode connackReasonCode = Mqtt5ConnAckReasonCode.from(disconnectedReasonCode);
        Preconditions.checkArgument(connackReasonCode != null,
                "The disconnected reason code " + disconnectedReasonCode.name() +
                        " is not a CONNACK reason code and therefore must not be used during connect authentication.");
        return connackReasonCode;
    }
}
