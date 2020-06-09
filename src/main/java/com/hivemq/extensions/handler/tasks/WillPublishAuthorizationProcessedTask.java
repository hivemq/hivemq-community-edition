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

import com.google.common.util.concurrent.FutureCallback;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectReasonCode;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;
import com.hivemq.extensions.auth.parameter.PublishAuthorizerOutputImpl;
import com.hivemq.extensions.handler.PluginAuthorizerServiceImpl;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.util.Exceptions;
import io.netty.channel.ChannelHandlerContext;
import com.hivemq.extension.sdk.api.annotations.Nullable;

/**
 * @author Christoph Schäbel
 */
public class WillPublishAuthorizationProcessedTask implements FutureCallback<PublishAuthorizerOutputImpl> {

    private final @NotNull CONNECT connect;
    private final @NotNull ChannelHandlerContext ctx;

    public WillPublishAuthorizationProcessedTask(@NotNull final CONNECT connect,
                                                 @NotNull final ChannelHandlerContext ctx) {
        this.connect = connect;
        this.ctx = ctx;
    }

    @Override
    public void onSuccess(@Nullable final PublishAuthorizerOutputImpl output) {
        if (output == null) {
            //this does not happen
            return;
        }

        DisconnectReasonCode disconnectReasonCode = null;
        AckReasonCode reasonCode = null;
        String reasonString = null;

        switch (output.getAuthorizationState()) {
            case DISCONNECT:
                disconnectReasonCode = output.getDisconnectReasonCode();
                reasonCode = output.getAckReasonCode() != null ? output.getAckReasonCode() : AckReasonCode.NOT_AUTHORIZED;
                reasonString = output.getReasonString() != null ? output.getReasonString() : getReasonString(connect);
                break;
            case FAIL:
                reasonCode = output.getAckReasonCode() != null ? output.getAckReasonCode() : AckReasonCode.NOT_AUTHORIZED;
                reasonString = output.getReasonString() != null ? output.getReasonString() : getReasonString(connect);
                break;
            case UNDECIDED:
                if (!output.isAuthorizerPresent()) {
                    //providers never returned an authorizer, same as continue
                    break;
                }
                reasonCode = AckReasonCode.NOT_AUTHORIZED;
                reasonString = getReasonString(connect);
                break;
            case SUCCESS:
                reasonCode = AckReasonCode.SUCCESS;
                break;
            case CONTINUE:
                break;
            default:
                //no state left
                throw new IllegalStateException("Unknown type");
        }

        final PublishAuthorizerResult result = new PublishAuthorizerResult(reasonCode, reasonString, output.isAuthorizerPresent(), disconnectReasonCode);
        ctx.pipeline().fireUserEventTriggered(new PluginAuthorizerServiceImpl.AuthorizeWillResultEvent(connect, result));
    }

    @Override
    public void onFailure(@NotNull final Throwable t) {
        Exceptions.rethrowError("Exception at PublishAuthorization", t);
        final PublishAuthorizerResult result = new PublishAuthorizerResult(AckReasonCode.NOT_AUTHORIZED, getReasonString(connect),
                true, DisconnectReasonCode.NOT_AUTHORIZED);
        ctx.pipeline().fireUserEventTriggered(new PluginAuthorizerServiceImpl.AuthorizeWillResultEvent(connect, result));
    }

    private String getReasonString(@NotNull final CONNECT connect) {
        return "Not allowed to connect with Will Publish for unauthorized topic '" + connect.getWillPublish().getTopic() + "' with QoS '"
                + connect.getWillPublish().getQos().getQosNumber() + "' and retain '" + connect.getWillPublish().isRetain() + "'";
    }
}
