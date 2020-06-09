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
import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.async.Async;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.extensions.auth.parameter.ModifiableClientSettingsImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractAsyncOutput;
import com.hivemq.extensions.packets.general.ModifiableUserPropertiesImpl;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Florian Limpöck
 * @author Silvio Giebl
 */
abstract class AuthOutput<T> extends AbstractAsyncOutput<T> {

    private final @NotNull AtomicBoolean decided = new AtomicBoolean(false);
    private boolean authenticatorPresent = false;
    private @NotNull AuthenticationState authenticationState = AuthenticationState.UNDECIDED;

    private @Nullable ByteBuffer authenticationData;
    @Nullable String reasonString;
    @Nullable String timeoutReasonString;
    private final boolean validateUTF8;
    private final @NotNull ModifiableUserPropertiesImpl userProperties;
    private final @NotNull ModifiableDefaultPermissions defaultPermissions;
    private final @NotNull ModifiableClientSettingsImpl clientSettings;
    private int timeout;

    AuthOutput(
            final @NotNull PluginOutPutAsyncer asyncer,
            final boolean validateUTF8,
            final @NotNull ModifiableDefaultPermissions defaultPermissions,
            final @NotNull ModifiableClientSettingsImpl clientSettings,
            final int timeout) {

        super(asyncer);
        this.validateUTF8 = validateUTF8;
        this.userProperties = new ModifiableUserPropertiesImpl(ImmutableList.of(), validateUTF8);
        this.defaultPermissions = defaultPermissions;
        this.clientSettings = clientSettings;
        this.timeout = timeout;
    }

    @SuppressWarnings({"CopyConstructorMissesField"})
    AuthOutput(final @NotNull AuthOutput<T> prevOutput) {
        super(prevOutput.asyncer);
        // decided, authenticatorPresent, authenticationState are reset as the state is isolated per output object
        // authenticationData, reasonString are reset as they are set by decision methods
        validateUTF8 = prevOutput.validateUTF8;
        userProperties = new ModifiableUserPropertiesImpl(prevOutput.userProperties.asInternalList(), validateUTF8);
        defaultPermissions = prevOutput.defaultPermissions;
        clientSettings = prevOutput.clientSettings;
        timeout = prevOutput.timeout;
    }

    public void authenticateSuccessfully() {
        checkDecided("authenticateSuccessfully");
        authenticationState = AuthenticationState.SUCCESS;
    }

    public void authenticateSuccessfully(final @NotNull ByteBuffer authenticationData) {
        Preconditions.checkNotNull(authenticationData, "Authentication data must never be null");
        authenticateSuccessfully();
        this.authenticationData = authenticationData.asReadOnlyBuffer();
    }

    public void authenticateSuccessfully(final @NotNull byte[] authenticationData) {
        Preconditions.checkNotNull(authenticationData, "Authentication data must never be null");
        authenticateSuccessfully();
        this.authenticationData = ByteBuffer.wrap(authenticationData).asReadOnlyBuffer();
    }

    public void continueAuthentication() {
        checkDecided("continueAuthentication");
        authenticationState = AuthenticationState.CONTINUE;
    }

    public void continueAuthentication(final @NotNull ByteBuffer authenticationData) {
        Preconditions.checkNotNull(authenticationData, "Authentication data must never be null");
        continueAuthentication();
        this.authenticationData = authenticationData.asReadOnlyBuffer();
    }

    public void continueAuthentication(final @NotNull byte[] authenticationData) {
        Preconditions.checkNotNull(authenticationData, "Authentication data must never be null");
        continueAuthentication();
        this.authenticationData = ByteBuffer.wrap(authenticationData).asReadOnlyBuffer();
    }

    public void failAuthentication() {
        checkDecided("failAuthentication");
        authenticationState = AuthenticationState.FAILED;
    }

    public void failAuthentication(final @Nullable String reasonString) {
        failAuthentication();
        this.reasonString = reasonString;
    }

    public void nextExtensionOrDefault() {
        checkDecided("nextExtensionOrDefault");
        authenticationState = AuthenticationState.NEXT_EXTENSION_OR_DEFAULT;
    }

    public @NotNull Async<T> async(
            final @NotNull Duration timeout,
            final @NotNull TimeoutFallback fallback,
            final @Nullable String reasonString) {

        final Async<T> async = async(timeout, fallback);
        timeoutReasonString = reasonString;
        return async;
    }

    void failByTimeout() {
        decided.set(true);
        authenticationState = AuthenticationState.FAILED;
        reasonString = timeoutReasonString;
    }

    void nextByTimeout() {
        decided.set(true);
        authenticationState = AuthenticationState.NEXT_EXTENSION_OR_DEFAULT;
    }

    void failByUndecided() {
        decided.set(true);
        authenticationState = AuthenticationState.FAILED;
    }

    void failByThrowable(final @NotNull Throwable throwable) {
        decided.set(true);
        authenticationState = AuthenticationState.FAILED;
    }

    @Nullable ByteBuffer getAuthenticationData() {
        return authenticationData;
    }

    @Nullable String getReasonString() {
        return reasonString;
    }

    public @NotNull ModifiableUserPropertiesImpl getOutboundUserProperties() {
        return userProperties;
    }

    public @NotNull ModifiableDefaultPermissions getDefaultPermissions() {
        return defaultPermissions;
    }

    public @NotNull ModifiableClientSettingsImpl getClientSettings() {
        return clientSettings;
    }

    public void setTimeout(final int timeout) {
        this.timeout = timeout;
    }

    int getTimeout() {
        return timeout;
    }

    void setAuthenticatorPresent() {
        authenticatorPresent = true;
    }

    boolean isAuthenticatorPresent() {
        return authenticatorPresent;
    }

    @NotNull AuthenticationState getAuthenticationState() {
        return authenticationState;
    }

    private void checkDecided(final @NotNull String method) {
        if (!decided.compareAndSet(false, true)) {
            if (isTimedOut()) {
                throw new UnsupportedOperationException(method + " has no effect as the async operation timed out.");
            }
            throw new UnsupportedOperationException(method + " must not be called if authenticateSuccessfully, " +
                    "failAuthentication, continueAuthentication or nextExtensionOrDefault has already been called.");
        }
    }
}
