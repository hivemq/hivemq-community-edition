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
import com.hivemq.extension.sdk.api.auth.parameter.SubscriptionAuthorizerOutput;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectReasonCode;
import com.hivemq.extension.sdk.api.packets.subscribe.SubackReasonCode;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractAsyncOutput;
import com.hivemq.extensions.executor.task.PluginTaskOutput;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * @author Lukas Brandl
 */
public class SubscriptionAuthorizerOutputImpl extends AbstractAsyncOutput<SubscriptionAuthorizerOutput> implements SubscriptionAuthorizerOutput, PluginTaskOutput, Supplier<SubscriptionAuthorizerOutputImpl> {

    private @Nullable SubackReasonCode subackReasonCode;
    private @Nullable String reasonString;
    private @NotNull DisconnectReasonCode disconnectReasonCode = DisconnectReasonCode.NOT_AUTHORIZED;

    private @NotNull AuthorizationState authorizationState = AuthorizationState.UNDECIDED;

    private final @NotNull AtomicBoolean completed = new AtomicBoolean(false);
    private final @NotNull AtomicBoolean authorizerPresent = new AtomicBoolean(false);

    public enum AuthorizationState {
        SUCCESS, CONTINUE, FAIL, DISCONNECT, UNDECIDED
    }

    public SubscriptionAuthorizerOutputImpl(final @NotNull PluginOutPutAsyncer asyncer) {
        super(asyncer);
    }

    @Override
    public @NotNull SubscriptionAuthorizerOutputImpl get() {
        return this;
    }

    @Override
    public void authorizeSuccessfully() {
        checkCompleted("authorizeSuccessfully");
        authorizationState = AuthorizationState.SUCCESS;
    }

    @Override
    public void failAuthorization() {
        checkCompleted("failAuthorization");
        this.subackReasonCode = SubackReasonCode.NOT_AUTHORIZED;
        authorizationState = AuthorizationState.FAIL;
    }

    public void forceFailedAuthorization(){
        completed.set(true);
        this.subackReasonCode = SubackReasonCode.NOT_AUTHORIZED;
        authorizationState = AuthorizationState.FAIL;
    }

    @Override
    public void failAuthorization(final @NotNull SubackReasonCode reasonCode) {
        checkCompleted("failAuthorization");
        Preconditions.checkNotNull(reasonCode, "reason code must never be null");
        if (reasonCode == SubackReasonCode.GRANTED_QOS_0 ||
                reasonCode == SubackReasonCode.GRANTED_QOS_1 ||
                reasonCode == SubackReasonCode.GRANTED_QOS_2) {
            throw new IllegalArgumentException("fail must use a SUBACK Error code");
        }
        this.subackReasonCode = reasonCode;
        authorizationState = AuthorizationState.FAIL;
    }

    @Override
    public void failAuthorization(final @NotNull SubackReasonCode reasonCode, final @NotNull String reasonString) {
        checkCompleted("failAuthorization");
        Preconditions.checkNotNull(reasonCode, "reason code must never be null");
        Preconditions.checkNotNull(reasonString, "reason string must never be null");
        if (reasonCode == SubackReasonCode.GRANTED_QOS_0 ||
                reasonCode == SubackReasonCode.GRANTED_QOS_1 ||
                reasonCode == SubackReasonCode.GRANTED_QOS_2) {
            throw new IllegalArgumentException("Fail must use a SUBACK Error code");
        }
        this.subackReasonCode = reasonCode;
        this.reasonString = reasonString;
        authorizationState = AuthorizationState.FAIL;
    }

    @Override
    public void disconnectClient() {
        checkCompleted("disconnectClient");
        this.disconnectReasonCode = DisconnectReasonCode.NOT_AUTHORIZED;
        authorizationState = AuthorizationState.DISCONNECT;
    }

    @Override
    public void disconnectClient(final @NotNull DisconnectReasonCode reasonCode) {
        checkCompleted("disconnectClient");
        Preconditions.checkNotNull(reasonCode, "reason code must never be null");
        this.disconnectReasonCode = reasonCode;
        authorizationState = AuthorizationState.DISCONNECT;
    }

    @Override
    public void disconnectClient(final @NotNull DisconnectReasonCode reasonCode, final @NotNull String reasonString) {
        checkCompleted("disconnectClient");
        Preconditions.checkNotNull(reasonCode, "reason code must never be null");
        Preconditions.checkNotNull(reasonString, "reason string must never be null");
        this.disconnectReasonCode = reasonCode;
        this.reasonString = reasonString;
        authorizationState = AuthorizationState.DISCONNECT;
    }

    @Override
    public void nextExtensionOrDefault() {
        if (completed.get()) {
            throw new UnsupportedOperationException("nextExtensionOrDefault must not be called if authorizeSuccessfully, " +
                    "failAuthorization, disconnectClient or nextExtensionOrDefault has already been called or if the async output has already timed out");
        }
        authorizationState = AuthorizationState.CONTINUE;
    }

    public @Nullable SubackReasonCode getSubackReasonCode() {
        return subackReasonCode;
    }

    public @Nullable String getReasonString() {
        return reasonString;
    }

    public @NotNull DisconnectReasonCode getDisconnectReasonCode() {
        return disconnectReasonCode;
    }

    public @NotNull AuthorizationState getAuthorizationState() {
        return authorizationState;
    }

    private void checkCompleted(final @NotNull String method) {
        if (!completed.compareAndSet(false, true)) {
            throw new UnsupportedOperationException(method + " must not be called if authorizeSuccessfully, " +
                    "failAuthorization, disconnectClient or nextExtensionOrDefault has already been called");
        }
    }

    public boolean isCompleted() {
        return completed.get();
    }

    public void authorizerPresent() {
        authorizerPresent.set(true);
    }

    public boolean isAuthorizerPresent() {
        return authorizerPresent.get();
    }
}
