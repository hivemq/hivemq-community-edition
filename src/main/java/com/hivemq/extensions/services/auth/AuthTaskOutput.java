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

package com.hivemq.extensions.services.auth;

import com.google.common.base.Preconditions;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.async.Async;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.auth.parameter.EnhancedAuthOutput;
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.extension.sdk.api.packets.general.DisconnectedReasonCode;
import com.hivemq.extension.sdk.api.packets.general.ModifiableUserProperties;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractAsyncOutput;
import com.hivemq.extensions.executor.task.PluginTaskOutput;
import com.hivemq.extensions.packets.general.InternalUserProperties;
import com.hivemq.extensions.packets.general.ModifiableUserPropertiesImpl;
import com.hivemq.extensions.packets.general.ReasonCodeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Daniel Krüger
 * @author Florian Limpöck
 */
public class AuthTaskOutput extends AbstractAsyncOutput<EnhancedAuthOutput> implements PluginTaskOutput, EnhancedAuthOutput, AuthOutput {

    private static final Logger log = LoggerFactory.getLogger(AuthTaskOutput.class);

    private final @NotNull AtomicBoolean decided = new AtomicBoolean(false);
    private final @NotNull AtomicBoolean authenticatorPresent = new AtomicBoolean(false);

    private final boolean isReAuth;
    private final boolean validateUTF8;
    private final @NotNull ModifiableClientSettingsImpl modifiableClientSettings;
    private final @NotNull ModifiableDefaultPermissions defaultPermissions;
    private final @NotNull AuthenticationContext authenticationContext;
    private @NotNull DisconnectedReasonCode disconnectedReasonCode = DisconnectedReasonCode.NOT_AUTHORIZED;
    private @Nullable ModifiableUserPropertiesImpl modifiableUserProperties;
    private @Nullable InternalUserProperties legacyUserProperties;
    private @Nullable ByteBuffer authenticationData;
    private @Nullable String reasonString = "Authentication failed by extension";
    private int timeout;
    private final boolean supportsEnhancedAuth;

    AuthTaskOutput(final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull ModifiableClientSettingsImpl clientSettings,
            final @NotNull ModifiableDefaultPermissions defaultPermissions,
            final @NotNull AuthenticationContext authenticationContext,
            final boolean validateUTF8,
            final boolean isReAuth,
            final int timeout,
            final boolean supportsEnhancedAuth) {
        super(asyncer);
        this.validateUTF8 = validateUTF8;
        this.authenticationContext = authenticationContext;
        this.defaultPermissions = defaultPermissions;
        this.timeout = timeout;
        this.modifiableClientSettings = clientSettings;
        this.isReAuth = isReAuth;
        this.supportsEnhancedAuth = supportsEnhancedAuth;
    }

    AuthTaskOutput(final @NotNull AuthTaskOutput authTaskOutput) {
        this(authTaskOutput.asyncer,
                authTaskOutput.modifiableClientSettings,
                authTaskOutput.defaultPermissions,
                authTaskOutput.authenticationContext,
                authTaskOutput.validateUTF8,
                authTaskOutput.isReAuth,
                authTaskOutput.timeout,
                authTaskOutput.supportsEnhancedAuth);
        if (authTaskOutput.getChangedUserProperties() != null) {
            this.legacyUserProperties = authTaskOutput.getChangedUserProperties().consolidate();
            this.modifiableUserProperties = new ModifiableUserPropertiesImpl(legacyUserProperties, validateUTF8);
        }
        this.reasonString = authTaskOutput.reasonString;
        this.disconnectedReasonCode = authTaskOutput.disconnectedReasonCode;
        this.authenticationData = authTaskOutput.authenticationData;
        this.timeout = authTaskOutput.timeout;
    }

    @Override
    public @NotNull Async<EnhancedAuthOutput> async(final @NotNull Duration timeout) {
        return async(timeout, TimeoutFallback.FAILURE, DisconnectedReasonCode.NOT_AUTHORIZED, "Authentication failed by timeout");
    }

    @Override
    public @NotNull Async<EnhancedAuthOutput> async(
            final @NotNull Duration timeout,
            final @NotNull TimeoutFallback timeoutFallback) {

        return async(timeout, timeoutFallback, DisconnectedReasonCode.NOT_AUTHORIZED, "Authentication failed by timeout");
    }

    @Override
    public @NotNull Async<EnhancedAuthOutput> async(
            final @NotNull Duration timeout,
            final @NotNull TimeoutFallback timeoutFallback,
            final @NotNull DisconnectedReasonCode reasonCode) {

        return async(timeout, timeoutFallback, reasonCode, "Authentication failed by timeout");
    }

    @Override
    public @NotNull Async<EnhancedAuthOutput> async(
            final @NotNull Duration timeout,
            final @NotNull TimeoutFallback timeoutFallback,
            final @Nullable String reasonString) {

        return async(timeout, timeoutFallback, DisconnectedReasonCode.NOT_AUTHORIZED, reasonString);
    }

    public @NotNull Async<EnhancedAuthOutput> async(
            final @NotNull Duration timeout,
            final @NotNull TimeoutFallback timeoutFallback,
            final @NotNull DisconnectedReasonCode reasonCode,
            final @Nullable String reasonString) {

        Preconditions.checkNotNull(timeout, "Duration must never be null");
        Preconditions.checkNotNull(timeoutFallback, "Fallback must never be null");
        Preconditions.checkNotNull(reasonCode, "Reason code must never be null");
        checkReasonCode(reasonCode);

        this.disconnectedReasonCode = reasonCode;
        this.reasonString = reasonString;
        return super.async(timeout, timeoutFallback);
    }


    @Override
    public void continueAuthentication() {
        checkContinueAuthentication();
        this.authenticationContext.setAuthenticationState(AuthenticationState.CONTINUE);
    }

    @Override
    public void continueAuthentication(final @NotNull ByteBuffer authenticationData) {
        checkContinueAuthentication();
        Preconditions.checkNotNull(authenticationData, "Authentication data must never be null");
        this.authenticationContext.setAuthenticationState(AuthenticationState.CONTINUE);
        this.authenticationData = authenticationData.asReadOnlyBuffer();
    }

    @Override
    public void continueAuthentication(final @NotNull byte[] authenticationData) {
        checkContinueAuthentication();
        Preconditions.checkNotNull(authenticationData, "Authentication data must never be null");
        this.authenticationContext.setAuthenticationState(AuthenticationState.CONTINUE);
        this.authenticationData = ByteBuffer.wrap(authenticationData).asReadOnlyBuffer();
    }

    public void checkContinueAuthentication() {
        checkDecided("continueAuthentication");
        if (!supportsEnhancedAuth) {
            throw new UnsupportedOperationException("Continue authentication is not supported as the client does not support enhanced authentication.");
        }
    }


    @Override
    public void authenticateSuccessfully() {
        checkDecided("authenticateSuccessfully");
        this.authenticationContext.setAuthenticationState(AuthenticationState.SUCCESS);
    }

    @Override
    public void authenticateSuccessfully(final @NotNull ByteBuffer authenticationData) {
        checkDecided("authenticateSuccessfully");
        Preconditions.checkNotNull(authenticationData, "Authentication data must never be null");
        this.authenticationContext.setAuthenticationState(AuthenticationState.SUCCESS);
        this.authenticationData = authenticationData.asReadOnlyBuffer();
    }

    @Override
    public void authenticateSuccessfully(final @NotNull byte[] authenticationData) {
        checkDecided("authenticateSuccessfully");
        Preconditions.checkNotNull(authenticationData, "Authentication data must never be null");
        this.authenticationContext.setAuthenticationState(AuthenticationState.SUCCESS);
        this.authenticationData = ByteBuffer.wrap(authenticationData).asReadOnlyBuffer();
    }


    @Override
    public void failAuthentication() {
        checkDecided("failAuthentication");
        this.authenticationContext.setAuthenticationState(AuthenticationState.FAILED);
    }

    @Override
    public void failAuthentication(final @NotNull DisconnectedReasonCode reasonCode) {
        checkDecided("failAuthentication");
        checkReasonCode(reasonCode);
        this.authenticationContext.setAuthenticationState(AuthenticationState.FAILED);
        this.disconnectedReasonCode = reasonCode;
    }

    @Override
    public void failAuthentication(final @Nullable String reasonString) {
        checkDecided("failAuthentication");
        this.authenticationContext.setAuthenticationState(AuthenticationState.FAILED);
        this.reasonString = reasonString;
    }

    @Override
    public void failAuthentication(final @NotNull DisconnectedReasonCode reasonCode, final @Nullable String reasonString) {
        checkDecided("failAuthentication");
        checkReasonCode(reasonCode);
        this.authenticationContext.setAuthenticationState(AuthenticationState.FAILED);
        this.disconnectedReasonCode = reasonCode;
        this.reasonString = reasonString;
    }


    @Override
    public void nextExtensionOrDefault() {
        checkDecided("nextExtensionOrDefault");
        this.authenticationContext.setAuthenticationState(AuthenticationState.NEXT_EXTENSION_OR_DEFAULT);
    }

    public int getTimeout() {
        return timeout;
    }

    @Override
    public void setTimeout(final int timeout) {
        this.timeout = timeout;
    }

    @Override
    public @NotNull ModifiableUserProperties getOutboundUserProperties() {
        if (modifiableUserProperties == null) {
            modifiableUserProperties = new ModifiableUserPropertiesImpl(legacyUserProperties, validateUTF8);
        }
        return modifiableUserProperties;
    }

    @Override
    public @NotNull ModifiableClientSettingsImpl getClientSettings() {
        return modifiableClientSettings;
    }

    @Override
    public @NotNull ModifiableDefaultPermissions getDefaultPermissions() {
        return this.defaultPermissions;
    }

    public @Nullable ByteBuffer getAuthenticationData() {
        return authenticationData;
    }

    public @NotNull AuthenticationState getAuthenticationState() {
        return this.authenticationContext.getAuthenticationState();
    }

    public void failByTimeout() {
        this.decided.set(true);
        this.authenticationContext.setAuthenticationState(AuthenticationState.FAILED);
    }

    public void nextByTimeout() {
        this.decided.set(true);
        this.authenticationContext.setAuthenticationState(AuthenticationState.NEXT_EXTENSION_OR_DEFAULT);
    }

    @Nullable String getReasonString() {
        return reasonString;
    }

    void setThrowable(final @NotNull Throwable throwable) {
        decided.set(true);
        this.disconnectedReasonCode = DisconnectedReasonCode.UNSPECIFIED_ERROR;
        this.authenticationContext.setAuthenticationState(AuthenticationState.FAILED);
        reasonString = "Unhandled exception in authentication extension";
        log.warn("Uncaught exception was thrown from a plugin during authentication. Plugins are responsible on their own to handle exceptions.", throwable);
    }

    @Nullable InternalUserProperties getChangedUserProperties() {
        return modifiableUserProperties;
    }

    @NotNull
    public DisconnectedReasonCode getDisconnectedReasonCode() {
        return disconnectedReasonCode;
    }

    void authenticatorPresent() {
        authenticatorPresent.set(true);
    }

    public boolean isAuthenticatorPresent() {
        return authenticatorPresent.get();
    }

    private void checkDecided(final @NotNull String method) {
        if (!decided.compareAndSet(false, true)) {
            throw new UnsupportedOperationException(method + " must not be called if authenticateSuccessfully, " +
                    "failAuthentication or nextExtensionOrDefault has already been called");
        }
    }

    private void checkReasonCode(final @NotNull DisconnectedReasonCode disconnectedReasonCode) {
        Preconditions.checkNotNull(disconnectedReasonCode, "Reason code must never be null");
        if (isReAuth) {
            Preconditions.checkArgument(disconnectedReasonCode != DisconnectedReasonCode.NORMAL_DISCONNECTION,
                    "DISCONNECT reason code must not be 'NORMAL_DISCONNECTION' for failed authentication");
            Preconditions.checkArgument(ReasonCodeUtil.toMqtt5DisconnectReasonCode(disconnectedReasonCode) != null,
                    "The disconnected reason code '" + disconnectedReasonCode.name() + "' cannot be used for DISCONNECT messages.");
        } else {
            Preconditions.checkArgument(disconnectedReasonCode != DisconnectedReasonCode.SUCCESS,
                    "CONNACK reason code must not be 'SUCCESS' for failed authentication");
            Preconditions.checkArgument(ReasonCodeUtil.toMqtt5ConnAckReasonCode(disconnectedReasonCode) != null,
                    "The disconnected reason code '" + disconnectedReasonCode.name() + "' cannot be used for CONNACK messages.");
        }
    }

}