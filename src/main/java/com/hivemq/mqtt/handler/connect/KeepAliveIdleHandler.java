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

package com.hivemq.mqtt.handler.connect;

import com.hivemq.logging.EventLog;
import com.hivemq.util.ChannelUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This handler disconnects MQTT clients if they were idle for too long
 * without sending any PINGREQ or other MQTT control packet.
 *
 * @author Dominik Obermaier
 */
public class KeepAliveIdleHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(KeepAliveIdleHandler.class);
    private final EventLog eventLog;

    public KeepAliveIdleHandler(final EventLog eventLog) {
        this.eventLog = eventLog;
    }

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) throws Exception {

        if (evt instanceof IdleStateEvent) {

            if (((IdleStateEvent) evt).state() == IdleState.READER_IDLE) {
                if (log.isDebugEnabled()) {

                    log.debug("Client with IP {} disconnected. The client was idle for too long without sending a MQTT control packet",
                            ChannelUtils.getChannelIP(ctx.channel()).or("UNKNOWN"));
                }
                eventLog.clientWasDisconnected(ctx.channel(), "Client was idle for too long");
                ctx.close();
                return;
            }
        }
        super.userEventTriggered(ctx, evt);
    }
}
