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
package com.hivemq.mqtt.handler.publish;

import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.pubrec.PUBREC;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5PubRecReasonCode;
import com.hivemq.util.ReasonStrings;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import javax.inject.Inject;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This is a {@link ChannelDuplexHandler} for MQTT 5 Flow Control.
 * <p>
 * Each time the Client sends a PUBLISH packet at QoS > 0, the send quota will be decremented.
 * <p>
 * The send quota is incremented by 1:
 * <p>
 * - Each time a PUBACK or PUBCOMP packet is sent by the server, regardless of whether the PUBACK or PUBCOMP carried an error code.
 * <p>
 * - Each time a PUBREC packet is sent by the server with a {@link Mqtt5PubRecReasonCode} of 0x80 or greater.
 * <p>
 * A Client gets disconnected if its send quota is '-1'.
 *
 * @author Florian Limpöck
 *
 * @since 4.0.0
 */
public class FlowControlHandler extends ChannelDuplexHandler {

    private final AtomicInteger serverSendQuota;
    private final int serverReceiveMaximum;
    private final MqttServerDisconnector serverDisconnector;

    @Inject
    public FlowControlHandler(final MqttConfigurationService mqttConfigurationService, final MqttServerDisconnector serverDisconnector) {
        this.serverReceiveMaximum = mqttConfigurationService.serverReceiveMaximum();
        this.serverDisconnector = serverDisconnector;
        this.serverSendQuota = new AtomicInteger(serverReceiveMaximum);
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {


        if (msg instanceof PUBLISH) {
            //client already disconnected. No need to read more publishes.
            if (serverSendQuota.get() < 0) {
                return;
            }
            final PUBLISH publish = (PUBLISH) msg;

            //decrement sendQuota for qos > 0 publish messages and disconnect client when quota gets negative
            if (QoS.AT_MOST_ONCE != publish.getQoS() && serverSendQuota.getAndDecrement() == 0) {
                serverDisconnector.disconnect(ctx.channel(),
                        "A client (IP: {}) sent too many concurrent PUBLISH messages. Disconnecting client.",
                        "Sent too many concurrent PUBLISH messages",
                        Mqtt5DisconnectReasonCode.RECEIVE_MAXIMUM_EXCEEDED,
                        ReasonStrings.DISCONNECT_RECEIVE_MAXIMUM_EXCEEDED);
                return;
            }
        }

        super.channelRead(ctx, msg);
    }

    @Override
    public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {

        //do not check msg instance and increment quota if its already at maximum
        if (serverSendQuota.get() == serverReceiveMaximum) {
            super.write(ctx, msg, promise);
            return;
        }

        if (msg instanceof PUBACK) {
            serverSendQuota.incrementAndGet();
        }

        if (msg instanceof PUBCOMP) {
            serverSendQuota.incrementAndGet();
        }

        if (msg instanceof PUBREC) {
            final PUBREC pubrec = (PUBREC) msg;
            if (pubrec.getReasonCode().getCode() >= 0x80) {
                serverSendQuota.incrementAndGet();
            }

        }

        super.write(ctx, msg, promise);
    }

    public int getServerSendQuota() {
        return serverSendQuota.get();
    }
}
