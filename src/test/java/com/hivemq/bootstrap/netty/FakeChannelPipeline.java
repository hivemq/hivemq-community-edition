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
package com.hivemq.bootstrap.netty;

import com.google.common.collect.Lists;
import io.netty.channel.*;
import io.netty.util.concurrent.EventExecutorGroup;

import java.net.SocketAddress;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Christoph Schäbel
 */
public class FakeChannelPipeline implements ChannelPipeline {

    private final List<String> names = Lists.newArrayList();

    @Override
    public ChannelPipeline addFirst(final String name, final ChannelHandler handler) {
        names.add(0, name);
        return this;
    }

    @Override
    public ChannelPipeline addFirst(final EventExecutorGroup group, final String name, final ChannelHandler handler) {
        names.add(0, name);
        return this;
    }

    @Override
    public ChannelPipeline addLast(final String name, final ChannelHandler handler) {
        names.add(name);
        return this;
    }

    @Override
    public ChannelPipeline addLast(final EventExecutorGroup group, final String name, final ChannelHandler handler) {
        names.add(name);
        return this;
    }

    @Override
    public ChannelPipeline addBefore(final String baseName, final String name, final ChannelHandler handler) {
        names.add(names.indexOf(baseName), name);
        return this;
    }

    @Override
    public ChannelPipeline addBefore(final EventExecutorGroup group, final String baseName, final String name, final ChannelHandler handler) {
        names.add(names.indexOf(baseName), name);
        return this;
    }

    @Override
    public ChannelPipeline addAfter(final String baseName, final String name, final ChannelHandler handler) {
        names.add(names.indexOf(baseName) + 1, name);
        return this;
    }

    @Override
    public ChannelPipeline addAfter(final EventExecutorGroup group, final String baseName, final String name, final ChannelHandler handler) {
        names.add(names.indexOf(baseName) + 1, name);
        return this;
    }

    @Override
    public ChannelPipeline addFirst(final ChannelHandler... handlers) {
        return this;
    }

    @Override
    public ChannelPipeline addFirst(final EventExecutorGroup group, final ChannelHandler... handlers) {
        return this;
    }

    @Override
    public ChannelPipeline addLast(final ChannelHandler... handlers) {
        return this;
    }

    @Override
    public ChannelPipeline addLast(final EventExecutorGroup group, final ChannelHandler... handlers) {
        return this;
    }

    @Override
    public ChannelPipeline remove(final ChannelHandler handler) {
        return this;
    }

    @Override
    public ChannelHandler remove(final String name) {
        names.remove(name);
        return null;
    }

    @Override
    public <T extends ChannelHandler> T remove(final Class<T> handlerType) {
        return null;
    }

    @Override
    public ChannelHandler removeFirst() {
        names.remove(0);
        return null;
    }

    @Override
    public ChannelHandler removeLast() {
        names.remove(names.size() - 1);
        return null;
    }

    @Override
    public ChannelPipeline replace(final ChannelHandler oldHandler, final String newName, final ChannelHandler newHandler) {
        return this;
    }

    @Override
    public ChannelHandler replace(final String oldName, final String newName, final ChannelHandler newHandler) {
        return null;
    }

    @Override
    public <T extends ChannelHandler> T replace(final Class<T> oldHandlerType, final String newName, final ChannelHandler newHandler) {
        return null;
    }

    @Override
    public ChannelHandler first() {
        return null;
    }

    @Override
    public ChannelHandlerContext firstContext() {
        return null;
    }

    @Override
    public ChannelHandler last() {
        return null;
    }

    @Override
    public ChannelHandlerContext lastContext() {
        return null;
    }

    @Override
    public ChannelHandler get(final String name) {
        return null;
    }

    @Override
    public <T extends ChannelHandler> T get(final Class<T> handlerType) {
        return null;
    }

    @Override
    public ChannelHandlerContext context(final ChannelHandler handler) {
        return null;
    }

    @Override
    public ChannelHandlerContext context(final String name) {
        return null;
    }

    @Override
    public ChannelHandlerContext context(final Class<? extends ChannelHandler> handlerType) {
        return null;
    }

    @Override
    public Channel channel() {
        return null;
    }

    @Override
    public List<String> names() {
        return names;
    }

    @Override
    public Map<String, ChannelHandler> toMap() {
        return null;
    }

    @Override
    public ChannelPipeline fireChannelRegistered() {
        return null;
    }

    @Override
    public ChannelPipeline fireChannelUnregistered() {
        return this;
    }

    @Override
    public ChannelPipeline fireChannelActive() {
        return this;
    }

    @Override
    public ChannelPipeline fireChannelInactive() {
        return null;
    }

    @Override
    public ChannelPipeline fireExceptionCaught(final Throwable cause) {
        return null;
    }

    @Override
    public ChannelPipeline fireUserEventTriggered(final Object event) {
        return this;
    }

    @Override
    public ChannelPipeline fireChannelRead(final Object msg) {
        return this;
    }

    @Override
    public ChannelPipeline fireChannelReadComplete() {
        return this;
    }

    @Override
    public ChannelPipeline fireChannelWritabilityChanged() {
        return this;
    }

    @Override
    public ChannelFuture bind(final SocketAddress localAddress) {
        return null;
    }

    @Override
    public ChannelFuture connect(final SocketAddress remoteAddress) {
        return null;
    }

    @Override
    public ChannelFuture connect(final SocketAddress remoteAddress, final SocketAddress localAddress) {
        return null;
    }

    @Override
    public ChannelFuture disconnect() {
        return null;
    }

    @Override
    public ChannelFuture close() {
        return null;
    }

    @Override
    public ChannelFuture deregister() {
        return null;
    }

    @Override
    public ChannelFuture bind(final SocketAddress localAddress, final ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelFuture connect(final SocketAddress remoteAddress, final ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelFuture connect(final SocketAddress remoteAddress, final SocketAddress localAddress, final ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelFuture disconnect(final ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelFuture close(final ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelFuture deregister(final ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelPipeline read() {
        return this;
    }

    @Override
    public ChannelFuture write(final Object msg) {
        return null;
    }

    @Override
    public ChannelFuture write(final Object msg, final ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelPipeline flush() {
        return this;
    }

    @Override
    public ChannelFuture writeAndFlush(final Object msg, final ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelFuture writeAndFlush(final Object msg) {
        return null;
    }

    @Override
    public ChannelPromise newPromise() {
        return null;
    }

    @Override
    public ChannelProgressivePromise newProgressivePromise() {
        return null;
    }

    @Override
    public ChannelFuture newSucceededFuture() {
        return null;
    }

    @Override
    public ChannelFuture newFailedFuture(final Throwable cause) {
        return null;
    }

    @Override
    public ChannelPromise voidPromise() {
        return null;
    }

    @Override
    public Iterator<Map.Entry<String, ChannelHandler>> iterator() {
        return null;
    }
}
