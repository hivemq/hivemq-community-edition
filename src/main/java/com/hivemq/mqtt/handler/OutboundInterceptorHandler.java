package com.hivemq.mqtt.handler;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.handler.*;
import com.hivemq.mqtt.message.PINGRESP;
import com.hivemq.mqtt.message.connack.CONNACK;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.pubrec.PUBREC;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import com.hivemq.mqtt.message.suback.SUBACK;
import com.hivemq.mqtt.message.unsuback.UNSUBACK;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Daniel Krüger
 */
@Singleton
@ChannelHandler.Sharable
public class OutboundInterceptorHandler extends ChannelOutboundHandlerAdapter {

    private final @NotNull ConnackOutboundInterceptorHandler connackOutboundInterceptorHandler;

    private final @NotNull PublishOutboundInterceptorHandler publishOutboundInterceptorHandler;
    private final @NotNull PubackInterceptorHandler pubackInterceptorHandler;
    private final @NotNull PubrecInterceptorHandler pubrecInterceptorHandler;
    private final @NotNull PubrelInterceptorHandler pubrelInterceptorHandler;
    private final @NotNull PubcompInterceptorHandler pubcompInterceptorHandler;

    private final @NotNull SubackOutboundInterceptorHandler subackOutboundInterceptorHandler;
    private final @NotNull UnsubackOutboundInterceptorHandler unsubackOutboundInterceptorHandler;

    private final @NotNull PingInterceptorHandler pingInterceptorHandler;
    private final @NotNull DisconnectInterceptorHandler disconnectInterceptorHandler;

    @Inject
    public OutboundInterceptorHandler(
            final @NotNull ConnackOutboundInterceptorHandler connackOutboundInterceptorHandler,
            final @NotNull PublishOutboundInterceptorHandler publishOutboundInterceptorHandler,
            final @NotNull PubackInterceptorHandler pubackInterceptorHandler,
            final @NotNull PubrecInterceptorHandler pubrecInterceptorHandler,
            final @NotNull PubrelInterceptorHandler pubrelInterceptorHandler,
            final @NotNull PubcompInterceptorHandler pubcompInterceptorHandler,
            final @NotNull SubackOutboundInterceptorHandler subackOutboundInterceptorHandler,
            final @NotNull UnsubackOutboundInterceptorHandler unsubackOutboundInterceptorHandler,
            final @NotNull PingInterceptorHandler pingInterceptorHandler,
            final @NotNull DisconnectInterceptorHandler disconnectInterceptorHandler) {
        this.connackOutboundInterceptorHandler = connackOutboundInterceptorHandler;
        this.publishOutboundInterceptorHandler = publishOutboundInterceptorHandler;
        this.pubackInterceptorHandler = pubackInterceptorHandler;
        this.pubrecInterceptorHandler = pubrecInterceptorHandler;
        this.pubrelInterceptorHandler = pubrelInterceptorHandler;
        this.pubcompInterceptorHandler = pubcompInterceptorHandler;
        this.subackOutboundInterceptorHandler = subackOutboundInterceptorHandler;
        this.unsubackOutboundInterceptorHandler = unsubackOutboundInterceptorHandler;
        this.pingInterceptorHandler = pingInterceptorHandler;
        this.disconnectInterceptorHandler = disconnectInterceptorHandler;
    }

    @Override
    public void write(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull Object msg,
            final @NotNull ChannelPromise promise) {
        //the order is important: it has to be ordered by the expected frequency to avoid instance of checks
        if (msg instanceof PUBLISH) {
            publishOutboundInterceptorHandler.handlePublish(ctx, (PUBLISH) msg, promise);
        } else if (msg instanceof PUBACK) {
            pubackInterceptorHandler.handleOutboundPuback(ctx, ((PUBACK) msg), promise);
        } else if (msg instanceof PUBREC) {
            pubrecInterceptorHandler.handleOutboundPubrec(ctx, ((PUBREC) msg), promise);
        } else if (msg instanceof PUBREL) {
            pubrelInterceptorHandler.handleOutboundPubrel(ctx, (PUBREL) msg, promise);
        } else if (msg instanceof PUBCOMP) {
            pubcompInterceptorHandler.handleOutboundPubcomp(ctx, ((PUBCOMP) msg), promise);
        } else if (msg instanceof PINGRESP) {
            pingInterceptorHandler.handleOutboundPingResp(ctx, ((PINGRESP) msg), promise);
        } else if (msg instanceof SUBACK) {
            subackOutboundInterceptorHandler.handleOutboundSuback(ctx, (SUBACK) msg, promise);
        } else if (msg instanceof UNSUBACK) {
            unsubackOutboundInterceptorHandler.handleOutboundUnsuback(ctx, (UNSUBACK) msg, promise);
        } else if (msg instanceof CONNACK) {
            connackOutboundInterceptorHandler.writeConnack(ctx, (CONNACK) msg, promise);
        } else if (msg instanceof DISCONNECT) {
            disconnectInterceptorHandler.handleOutboundDisconnect(ctx, ((DISCONNECT) msg), promise);
        } else {
            ctx.write(msg, promise);
        }
    }
}
