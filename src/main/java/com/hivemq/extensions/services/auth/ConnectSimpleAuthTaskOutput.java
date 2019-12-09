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
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthOutput;
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode;
import com.hivemq.extension.sdk.api.packets.general.ModifiableUserProperties;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractAsyncOutput;
import com.hivemq.extensions.executor.task.PluginTaskOutput;
import com.hivemq.extensions.packets.general.InternalUserProperties;
import com.hivemq.extensions.packets.general.ModifiableUserPropertiesImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Georg Held
 */
public class ConnectSimpleAuthTaskOutput extends AbstractAsyncOutput<SimpleAuthOutput>
        implements PluginTaskOutput, SimpleAuthOutput, AuthOutput {

    private static final Logger log = LoggerFactory.getLogger(ConnectSimpleAuthTaskOutput.class);

    private final @NotNull ModifiableClientSettingsImpl modifiableClientSettings;
    private final @NotNull ModifiableDefaultPermissions defaultPermissions;
    private final @NotNull AuthenticationContext authenticationContext;
    private final boolean validateUTF8;
    private final @NotNull AtomicBoolean decided = new AtomicBoolean(false);
    private final @NotNull AtomicBoolean authenticatorPresent = new AtomicBoolean(false);
    private @Nullable ModifiableUserPropertiesImpl modifiableUserProperties;
    private @Nullable InternalUserProperties legacyUserProperties;
    private @NotNull ConnackReasonCode connackReasonCode;
    private @Nullable String reasonString;


    ConnectSimpleAuthTaskOutput(final @NotNull PluginOutPutAsyncer asyncer,
                                final @NotNull ModifiableClientSettingsImpl clientSettings,
                                final @NotNull ModifiableDefaultPermissions defaultPermissions,
                                final @NotNull AuthenticationContext authenticationContext,
                                final boolean validateUTF8) {
        super(asyncer);
        this.validateUTF8 = validateUTF8;
        this.modifiableClientSettings = clientSettings;
        this.defaultPermissions = defaultPermissions;
        this.authenticationContext = authenticationContext;
        this.connackReasonCode = ConnackReasonCode.NOT_AUTHORIZED;
    }

    ConnectSimpleAuthTaskOutput(final @NotNull ConnectSimpleAuthTaskOutput connectSimpleAuthTaskOutput) {
        this(connectSimpleAuthTaskOutput.asyncer,
                connectSimpleAuthTaskOutput.modifiableClientSettings,
                connectSimpleAuthTaskOutput.defaultPermissions,
                connectSimpleAuthTaskOutput.authenticationContext,
                connectSimpleAuthTaskOutput.validateUTF8);

        if (connectSimpleAuthTaskOutput.getChangedUserProperties() != null) {
            legacyUserProperties = connectSimpleAuthTaskOutput.getChangedUserProperties().consolidate();
            modifiableUserProperties = new ModifiableUserPropertiesImpl(legacyUserProperties, validateUTF8);
        }
        reasonString = connectSimpleAuthTaskOutput.reasonString;
        connackReasonCode = connectSimpleAuthTaskOutput.connackReasonCode;
    }

    @Override
    public @NotNull Async<SimpleAuthOutput> async(
            final @NotNull Duration duration, final @NotNull TimeoutFallback timeoutFallback,
            final @NotNull ConnackReasonCode connackReasonCode) {
        return async(duration, timeoutFallback, connackReasonCode, "Authentication failed by timeout");
    }

    @Override
    public @NotNull Async<SimpleAuthOutput> async(
            final @NotNull Duration duration, final @NotNull TimeoutFallback timeoutFallback,
            final @NotNull String reasonString) {
        return async(duration, timeoutFallback, ConnackReasonCode.NOT_AUTHORIZED, reasonString);
    }

    @Override
    public @NotNull Async<SimpleAuthOutput> async(
            final @NotNull Duration duration, final @NotNull TimeoutFallback timeoutFallback) {
        Preconditions.checkNotNull(duration, "Duration must never be null");
        Preconditions.checkNotNull(timeoutFallback, "Fallback must never be null");
        return async(duration, timeoutFallback, ConnackReasonCode.NOT_AUTHORIZED, "Authentication failed by timeout");
    }

    @Override
    public @NotNull Async<SimpleAuthOutput> async(final @NotNull Duration duration) {
        Preconditions.checkNotNull(duration, "Duration must never be null");
        return async(duration, TimeoutFallback.FAILURE, ConnackReasonCode.NOT_AUTHORIZED, "Authentication failed by timeout");
    }

    public @NotNull Async<SimpleAuthOutput> async(
            final @NotNull Duration duration, final @NotNull TimeoutFallback timeoutFallback,
            final @NotNull ConnackReasonCode connackReasonCode, final @NotNull String reasonString) {

        Preconditions.checkNotNull(duration, "Duration must never be null");
        Preconditions.checkNotNull(timeoutFallback, "Fallback must never be null");
        Preconditions.checkNotNull(connackReasonCode, "Reason code must never be null");
        Preconditions.checkNotNull(reasonString, "Reason string must never be null");

        checkArgument(
                connackReasonCode != ConnackReasonCode.SUCCESS,
                "CONNACK reason code must not be SUCCESS for timed out authentication");

        this.connackReasonCode = connackReasonCode;
        this.reasonString = reasonString;
        return super.async(duration, timeoutFallback);
    }

    public void authenticateSuccessfully() {
        checkDecided("authenticateSuccessfully");
        this.authenticationContext.setAuthenticationState(AuthenticationState.SUCCESS);
    }

    public void failAuthentication() {
        failAuthentication("Authentication failed by extension");
    }

    public void failAuthentication(final @NotNull String reasonString) {
        failAuthentication(ConnackReasonCode.NOT_AUTHORIZED, reasonString);
    }

    public void failAuthentication(
            final @NotNull ConnackReasonCode connackReasonCode, final @NotNull String reasonString) {

        checkDecided("failAuthentication");
        checkNotNull(connackReasonCode, "CONNACK reason code must not be null");
        checkNotNull(reasonString, "CONNACK reason string must not be null");

        checkArgument(
                connackReasonCode != ConnackReasonCode.SUCCESS,
                "CONNACK reason code must not be SUCCESS for failAuthentication");

        this.authenticationContext.setAuthenticationState(AuthenticationState.FAILED);
        this.connackReasonCode = connackReasonCode;
        this.reasonString = reasonString;
    }

    public void nextExtensionOrDefault() {
        checkDecided("nextExtensionOrDefault");
        this.authenticationContext.setAuthenticationState(AuthenticationState.NEXT_EXTENSION_OR_DEFAULT);
    }

    private void checkDecided(final @NotNull String method) {
        if (!decided.compareAndSet(false, true)) {
            throw new UnsupportedOperationException(method + " must not be called if authenticateSuccessfully, " +
                    "failAuthentication or nextExtensionOrDefault has already been called");
        }
    }

    public @NotNull ModifiableUserProperties getOutboundUserProperties() {
        if (modifiableUserProperties == null) {
            modifiableUserProperties = new ModifiableUserPropertiesImpl(legacyUserProperties, validateUTF8);
        }
        return modifiableUserProperties;

    }

    public @NotNull ModifiableDefaultPermissions getDefaultPermissions() {
        return defaultPermissions;
    }

    public @NotNull ModifiableClientSettingsImpl getClientSettings() {
        return modifiableClientSettings;
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

    @NotNull ConnackReasonCode getConnackReasonCode() {
        return connackReasonCode;
    }

    @Nullable String getReasonString() {
        return reasonString;
    }

    @Nullable InternalUserProperties getChangedUserProperties() {
        return modifiableUserProperties;
    }

    public void authenticatorPresent() {
        authenticatorPresent.set(true);
    }

    public boolean isAuthenticatorPresent() {
        return authenticatorPresent.get();
    }

    void setThrowable(final @NotNull Throwable throwable) {
        decided.set(true);
        this.authenticationContext.setAuthenticationState(AuthenticationState.FAILED);
        this.connackReasonCode = ConnackReasonCode.UNSPECIFIED_ERROR;
        reasonString = "Unhandled exception in authentication extension";
        log.warn("Uncaught exception was thrown from an extension during authentication. Extensions are responsible on their own to handle exceptions.", throwable);
    }

}