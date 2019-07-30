package com.hivemq.mqtt.handler.connect;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.connect.CONNECT;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.NoSuchElementException;

import static com.hivemq.bootstrap.netty.ChannelHandlerNames.NEW_CONNECTION_IDLE_HANDLER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.NO_CONNECT_IDLE_EVENT_HANDLER;

/**
 * @author Lukas Brandl
 */
public class RemoveConnectIdleHandler extends SimpleChannelInboundHandler<CONNECT> {

    private static final Logger log = LoggerFactory.getLogger(RemoveConnectIdleHandler.class);

    @Override
    protected void channelRead0(@NotNull final ChannelHandlerContext ctx, @NotNull final CONNECT msg) throws Exception {
        try {
            ctx.pipeline().remove(NEW_CONNECTION_IDLE_HANDLER);
            ctx.pipeline().remove(NO_CONNECT_IDLE_EVENT_HANDLER);
            ctx.pipeline().remove(this);
        } catch (final NoSuchElementException ex) {
            //no problem, because if these handlers are not in the pipeline anyway, we still get the expected result here
            log.trace("Not able to remove no connect idle handler");
        }
        ctx.fireChannelRead(msg);
    }
}
