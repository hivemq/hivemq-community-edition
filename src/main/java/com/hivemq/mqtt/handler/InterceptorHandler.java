package com.hivemq.mqtt.handler;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.handler.ConnectInboundInterceptorHandler;
import com.hivemq.mqtt.message.connect.CONNECT;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Daniel Krüger
 */
@Singleton
@ChannelHandler.Sharable
public class InterceptorHandler extends ChannelInboundHandlerAdapter {

    private final @NotNull ConnectInboundInterceptorHandler connectInboundInterceptorHandler;

    @Inject
    public InterceptorHandler(ConnectInboundInterceptorHandler connectInboundInterceptorHandler) {
        this.connectInboundInterceptorHandler = connectInboundInterceptorHandler;
    }

    @Override
    public void channelRead(final @NotNull ChannelHandlerContext ctx, final @NotNull Object msg) {
        if (msg instanceof CONNECT) {
            connectInboundInterceptorHandler.readConnect(ctx, (CONNECT) msg);
        }





        else {
            ctx.fireChannelRead(msg);
        }
    }




}
