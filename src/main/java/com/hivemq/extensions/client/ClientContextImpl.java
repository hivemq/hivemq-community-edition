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
package com.hivemq.extensions.client;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.Interceptor;
import com.hivemq.extension.sdk.api.interceptor.disconnect.DisconnectInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.disconnect.DisconnectOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pingreq.PingReqInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pingresp.PingRespOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.puback.PubackInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.puback.PubackOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubcomp.PubcompInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubcomp.PubcompOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.publish.PublishInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.publish.PublishOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubrec.PubrecInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubrec.PubrecOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubrel.PubrelInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubrel.PubrelOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.suback.SubackOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.subscribe.SubscribeInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.unsuback.UnsubackOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.unsubscribe.UnsubscribeInboundInterceptor;
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class ClientContextImpl {

    private final @NotNull HiveMQExtensions hiveMQExtensions;
    private final @NotNull ModifiableDefaultPermissions defaultPermissions;
    private volatile @NotNull ImmutableList<PublishInboundInterceptor> publishInbounds = ImmutableList.of();
    private volatile @NotNull ImmutableList<PublishOutboundInterceptor> publishOutbounds = ImmutableList.of();
    private volatile @NotNull ImmutableList<PubackInboundInterceptor> pubackInbounds = ImmutableList.of();
    private volatile @NotNull ImmutableList<PubackOutboundInterceptor> pubackOutbounds = ImmutableList.of();
    private volatile @NotNull ImmutableList<PubrecInboundInterceptor> pubrecInbounds = ImmutableList.of();
    private volatile @NotNull ImmutableList<PubrecOutboundInterceptor> pubrecOutbounds = ImmutableList.of();
    private volatile @NotNull ImmutableList<PubrelInboundInterceptor> pubrelInbounds = ImmutableList.of();
    private volatile @NotNull ImmutableList<PubrelOutboundInterceptor> pubrelOutbounds = ImmutableList.of();
    private volatile @NotNull ImmutableList<PubcompInboundInterceptor> pubcompInbounds = ImmutableList.of();
    private volatile @NotNull ImmutableList<PubcompOutboundInterceptor> pubcompOutbounds = ImmutableList.of();
    private volatile @NotNull ImmutableList<SubscribeInboundInterceptor> subscribeInbounds = ImmutableList.of();
    private volatile @NotNull ImmutableList<SubackOutboundInterceptor> subackOutbounds = ImmutableList.of();
    private volatile @NotNull ImmutableList<UnsubscribeInboundInterceptor> unsubscribeInbounds = ImmutableList.of();
    private volatile @NotNull ImmutableList<UnsubackOutboundInterceptor> unsubackOutbounds = ImmutableList.of();
    private volatile @NotNull ImmutableList<DisconnectInboundInterceptor> disconnectInbounds = ImmutableList.of();
    private volatile @NotNull ImmutableList<DisconnectOutboundInterceptor> disconnectOutbounds = ImmutableList.of();
    private volatile @NotNull ImmutableList<PingReqInboundInterceptor> pingReqInbounds = ImmutableList.of();
    private volatile @NotNull ImmutableList<PingRespOutboundInterceptor> pingRespOutbounds = ImmutableList.of();

    public ClientContextImpl(
            final @NotNull HiveMQExtensions hiveMQExtensions,
            final @NotNull ModifiableDefaultPermissions defaultPermissions) {

        this.hiveMQExtensions = hiveMQExtensions;
        this.defaultPermissions = defaultPermissions;
    }

    public synchronized void addPublishInboundInterceptor(final @NotNull PublishInboundInterceptor interceptor) {
        publishInbounds = addInterceptor(publishInbounds, interceptor);
    }

    public synchronized void addPublishOutboundInterceptor(final @NotNull PublishOutboundInterceptor interceptor) {
        publishOutbounds = addInterceptor(publishOutbounds, interceptor);
    }

    public synchronized void addPubackInboundInterceptor(final @NotNull PubackInboundInterceptor interceptor) {
        pubackInbounds = addInterceptor(pubackInbounds, interceptor);
    }

    public synchronized void addPubackOutboundInterceptor(final @NotNull PubackOutboundInterceptor interceptor) {
        pubackOutbounds = addInterceptor(pubackOutbounds, interceptor);
    }

    public synchronized void addPubrecInboundInterceptor(final @NotNull PubrecInboundInterceptor interceptor) {
        pubrecInbounds = addInterceptor(pubrecInbounds, interceptor);
    }

    public synchronized void addPubrecOutboundInterceptor(final @NotNull PubrecOutboundInterceptor interceptor) {
        pubrecOutbounds = addInterceptor(pubrecOutbounds, interceptor);
    }

    public synchronized void addPubrelInboundInterceptor(final @NotNull PubrelInboundInterceptor interceptor) {
        pubrelInbounds = addInterceptor(pubrelInbounds, interceptor);
    }

    public synchronized void addPubrelOutboundInterceptor(final @NotNull PubrelOutboundInterceptor interceptor) {
        pubrelOutbounds = addInterceptor(pubrelOutbounds, interceptor);
    }

    public synchronized void addPubcompInboundInterceptor(final @NotNull PubcompInboundInterceptor interceptor) {
        pubcompInbounds = addInterceptor(pubcompInbounds, interceptor);
    }

    public synchronized void addPubcompOutboundInterceptor(final @NotNull PubcompOutboundInterceptor interceptor) {
        pubcompOutbounds = addInterceptor(pubcompOutbounds, interceptor);
    }

    public synchronized void addSubscribeInboundInterceptor(final @NotNull SubscribeInboundInterceptor interceptor) {
        subscribeInbounds = addInterceptor(subscribeInbounds, interceptor);
    }

    public synchronized void addSubackOutboundInterceptor(final @NotNull SubackOutboundInterceptor interceptor) {
        subackOutbounds = addInterceptor(subackOutbounds, interceptor);
    }

    public synchronized void addUnsubscribeInboundInterceptor(
            final @NotNull UnsubscribeInboundInterceptor interceptor) {

        unsubscribeInbounds = addInterceptor(unsubscribeInbounds, interceptor);
    }

    public synchronized void addUnsubackOutboundInterceptor(final @NotNull UnsubackOutboundInterceptor interceptor) {
        unsubackOutbounds = addInterceptor(unsubackOutbounds, interceptor);
    }

    public synchronized void addDisconnectInboundInterceptor(final @NotNull DisconnectInboundInterceptor interceptor) {
        disconnectInbounds = addInterceptor(disconnectInbounds, interceptor);
    }

    public synchronized void addDisconnectOutboundInterceptor(
            final @NotNull DisconnectOutboundInterceptor interceptor) {

        disconnectOutbounds = addInterceptor(disconnectOutbounds, interceptor);
    }

    public synchronized void addPingReqInboundInterceptor(final @NotNull PingReqInboundInterceptor interceptor) {
        pingReqInbounds = addInterceptor(pingReqInbounds, interceptor);
    }

    public synchronized void addPingRespOutboundInterceptor(final @NotNull PingRespOutboundInterceptor interceptor) {
        pingRespOutbounds = addInterceptor(pingRespOutbounds, interceptor);
    }

    public synchronized void removePublishInboundInterceptor(final @NotNull PublishInboundInterceptor interceptor) {
        publishInbounds = removeInterceptor(publishInbounds, interceptor);
    }

    public synchronized void removePublishOutboundInterceptor(final @NotNull PublishOutboundInterceptor interceptor) {
        publishOutbounds = removeInterceptor(publishOutbounds, interceptor);
    }

    public synchronized void removePubackInboundInterceptor(final @NotNull PubackInboundInterceptor interceptor) {
        pubackInbounds = removeInterceptor(pubackInbounds, interceptor);
    }

    public synchronized void removePubackOutboundInterceptor(final @NotNull PubackOutboundInterceptor interceptor) {
        pubackOutbounds = removeInterceptor(pubackOutbounds, interceptor);
    }

    public synchronized void removePubrecInboundInterceptor(final @NotNull PubrecInboundInterceptor interceptor) {
        pubrecInbounds = removeInterceptor(pubrecInbounds, interceptor);
    }

    public synchronized void removePubrecOutboundInterceptor(final @NotNull PubrecOutboundInterceptor interceptor) {
        pubrecOutbounds = removeInterceptor(pubrecOutbounds, interceptor);
    }

    public synchronized void removePubrelInboundInterceptor(final @NotNull PubrelInboundInterceptor interceptor) {
        pubrelInbounds = removeInterceptor(pubrelInbounds, interceptor);
    }

    public synchronized void removePubrelOutboundInterceptor(final @NotNull PubrelOutboundInterceptor interceptor) {
        pubrelOutbounds = removeInterceptor(pubrelOutbounds, interceptor);
    }

    public synchronized void removePubcompInboundInterceptor(final @NotNull PubcompInboundInterceptor interceptor) {
        pubcompInbounds = removeInterceptor(pubcompInbounds, interceptor);
    }

    public synchronized void removePubcompOutboundInterceptor(final @NotNull PubcompOutboundInterceptor interceptor) {
        pubcompOutbounds = removeInterceptor(pubcompOutbounds, interceptor);
    }

    public synchronized void removeSubscribeInboundInterceptor(final @NotNull SubscribeInboundInterceptor interceptor) {
        subscribeInbounds = removeInterceptor(subscribeInbounds, interceptor);
    }

    public synchronized void removeSubackOutboundInterceptor(final @NotNull SubackOutboundInterceptor interceptor) {
        subackOutbounds = removeInterceptor(subackOutbounds, interceptor);
    }

    public synchronized void removeUnsubscribeInboundInterceptor(
            final @NotNull UnsubscribeInboundInterceptor interceptor) {

        unsubscribeInbounds = removeInterceptor(unsubscribeInbounds, interceptor);
    }

    public synchronized void removeUnsubackOutboundInterceptor(final @NotNull UnsubackOutboundInterceptor interceptor) {
        unsubackOutbounds = removeInterceptor(unsubackOutbounds, interceptor);
    }

    public synchronized void removeDisconnectInboundInterceptor(
            final @NotNull DisconnectInboundInterceptor interceptor) {

        disconnectInbounds = removeInterceptor(disconnectInbounds, interceptor);
    }

    public synchronized void removeDisconnectOutboundInterceptor(
            final @NotNull DisconnectOutboundInterceptor interceptor) {

        disconnectOutbounds = removeInterceptor(disconnectOutbounds, interceptor);
    }

    public synchronized void removePingReqInboundInterceptor(final @NotNull PingReqInboundInterceptor interceptor) {
        pingReqInbounds = removeInterceptor(pingReqInbounds, interceptor);
    }

    public synchronized void removePingRespOutboundInterceptor(final @NotNull PingRespOutboundInterceptor interceptor) {
        pingRespOutbounds = removeInterceptor(pingRespOutbounds, interceptor);
    }

    public @Immutable @NotNull List<@NotNull Interceptor> getAllInterceptorsOfExtension(
            final @NotNull ClassLoader extensionClassLoader) {

        return streamAllInterceptors()
                .filter(interceptor -> interceptor.getClass().getClassLoader().equals(extensionClassLoader))
                .collect(ImmutableList.toImmutableList());
    }

    public @Immutable @NotNull List<@NotNull Interceptor> getAllInterceptors() {
        return streamAllInterceptors()
                .sorted(Comparator.comparingInt(this::getExtensionPriority).reversed())
                .collect(ImmutableList.toImmutableList());
    }

    private @NotNull Stream<Interceptor> streamAllInterceptors() {
        return Streams.concat(
                publishInbounds.stream(), publishOutbounds.stream(),
                pubackInbounds.stream(), pubackOutbounds.stream(),
                pubrecInbounds.stream(), pubrecOutbounds.stream(),
                pubrelInbounds.stream(), pubrelOutbounds.stream(),
                pubcompInbounds.stream(), pubcompOutbounds.stream(),
                subscribeInbounds.stream(), subackOutbounds.stream(),
                unsubscribeInbounds.stream(), unsubackOutbounds.stream(),
                disconnectInbounds.stream(), disconnectOutbounds.stream(),
                pingReqInbounds.stream(), pingRespOutbounds.stream());
    }

    public @Immutable @NotNull List<@NotNull PublishInboundInterceptor> getPublishInboundInterceptorsOfExtension(
            final @NotNull ClassLoader extensionClassLoader) {

        return filterInterceptorsOfExtension(publishInbounds, extensionClassLoader);
    }

    public @Immutable @NotNull List<@NotNull PublishInboundInterceptor> getPublishInboundInterceptors() {
        return publishInbounds;
    }

    public @Immutable @NotNull List<@NotNull PublishOutboundInterceptor> getPublishOutboundInterceptorsOfExtension(
            final @NotNull ClassLoader extensionClassLoader) {

        return filterInterceptorsOfExtension(publishOutbounds, extensionClassLoader);
    }

    public @Immutable @NotNull List<@NotNull PublishOutboundInterceptor> getPublishOutboundInterceptors() {
        return publishOutbounds;
    }

    public @Immutable @NotNull List<@NotNull PubackInboundInterceptor> getPubackInboundInterceptorsOfExtension(
            final @NotNull ClassLoader extensionClassLoader) {

        return filterInterceptorsOfExtension(pubackInbounds, extensionClassLoader);
    }

    public @Immutable @NotNull List<@NotNull PubackInboundInterceptor> getPubackInboundInterceptors() {
        return pubackInbounds;
    }

    public @Immutable @NotNull List<@NotNull PubackOutboundInterceptor> getPubackOutboundInterceptorsOfExtension(
            final @NotNull ClassLoader extensionClassLoader) {

        return filterInterceptorsOfExtension(pubackOutbounds, extensionClassLoader);
    }

    public @Immutable @NotNull List<@NotNull PubackOutboundInterceptor> getPubackOutboundInterceptors() {
        return pubackOutbounds;
    }

    public @Immutable @NotNull List<@NotNull PubrecInboundInterceptor> getPubrecInboundInterceptorsOfExtension(
            final @NotNull ClassLoader extensionClassLoader) {

        return filterInterceptorsOfExtension(pubrecInbounds, extensionClassLoader);
    }

    public @Immutable @NotNull List<@NotNull PubrecInboundInterceptor> getPubrecInboundInterceptors() {
        return pubrecInbounds;
    }

    public @Immutable @NotNull List<@NotNull PubrecOutboundInterceptor> getPubrecOutboundInterceptorsOfExtension(
            final @NotNull ClassLoader extensionClassLoader) {

        return filterInterceptorsOfExtension(pubrecOutbounds, extensionClassLoader);
    }

    public @Immutable @NotNull List<@NotNull PubrecOutboundInterceptor> getPubrecOutboundInterceptors() {
        return pubrecOutbounds;
    }

    public @Immutable @NotNull List<@NotNull PubrelInboundInterceptor> getPubrelInboundInterceptorsOfExtension(
            final @NotNull ClassLoader extensionClassLoader) {

        return filterInterceptorsOfExtension(pubrelInbounds, extensionClassLoader);
    }

    public @Immutable @NotNull List<@NotNull PubrelInboundInterceptor> getPubrelInboundInterceptors() {
        return pubrelInbounds;
    }

    public @Immutable @NotNull List<@NotNull PubrelOutboundInterceptor> getPubrelOutboundInterceptorsOfExtension(
            final @NotNull ClassLoader extensionClassLoader) {

        return filterInterceptorsOfExtension(pubrelOutbounds, extensionClassLoader);
    }

    public @Immutable @NotNull List<@NotNull PubrelOutboundInterceptor> getPubrelOutboundInterceptors() {
        return pubrelOutbounds;
    }

    public @Immutable @NotNull List<@NotNull PubcompInboundInterceptor> getPubcompInboundInterceptorsOfExtension(
            final @NotNull ClassLoader extensionClassLoader) {

        return filterInterceptorsOfExtension(pubcompInbounds, extensionClassLoader);
    }

    public @Immutable @NotNull List<@NotNull PubcompInboundInterceptor> getPubcompInboundInterceptors() {
        return pubcompInbounds;
    }

    public @Immutable @NotNull List<@NotNull PubcompOutboundInterceptor> getPubcompOutboundInterceptorsOfExtension(
            final @NotNull ClassLoader extensionClassLoader) {

        return filterInterceptorsOfExtension(pubcompOutbounds, extensionClassLoader);
    }

    public @Immutable @NotNull List<@NotNull PubcompOutboundInterceptor> getPubcompOutboundInterceptors() {
        return pubcompOutbounds;
    }

    public @Immutable @NotNull List<@NotNull SubscribeInboundInterceptor> getSubscribeInboundInterceptorsOfExtension(
            final @NotNull ClassLoader extensionClassLoader) {

        return filterInterceptorsOfExtension(subscribeInbounds, extensionClassLoader);
    }

    public @Immutable @NotNull List<@NotNull SubscribeInboundInterceptor> getSubscribeInboundInterceptors() {
        return subscribeInbounds;
    }

    public @Immutable @NotNull List<@NotNull SubackOutboundInterceptor> getSubackOutboundInterceptorsOfExtension(
            final @NotNull ClassLoader extensionClassLoader) {

        return filterInterceptorsOfExtension(subackOutbounds, extensionClassLoader);
    }

    public @Immutable @NotNull List<@NotNull SubackOutboundInterceptor> getSubackOutboundInterceptors() {
        return subackOutbounds;
    }

    public @Immutable @NotNull List<@NotNull UnsubscribeInboundInterceptor> getUnsubscribeInboundInterceptorsOfExtension(
            final @NotNull ClassLoader extensionClassLoader) {

        return filterInterceptorsOfExtension(unsubscribeInbounds, extensionClassLoader);
    }

    public @Immutable @NotNull List<@NotNull UnsubscribeInboundInterceptor> getUnsubscribeInboundInterceptors() {
        return unsubscribeInbounds;
    }

    public @Immutable @NotNull List<@NotNull UnsubackOutboundInterceptor> getUnsubackOutboundInterceptorsOfExtension(
            final @NotNull ClassLoader extensionClassLoader) {

        return filterInterceptorsOfExtension(unsubackOutbounds, extensionClassLoader);
    }

    public @Immutable @NotNull List<@NotNull UnsubackOutboundInterceptor> getUnsubackOutboundInterceptors() {
        return unsubackOutbounds;
    }

    public @Immutable @NotNull List<@NotNull DisconnectInboundInterceptor> getDisconnectInboundInterceptorsOfExtension(
            final @NotNull ClassLoader extensionClassLoader) {

        return filterInterceptorsOfExtension(disconnectInbounds, extensionClassLoader);
    }

    public @Immutable @NotNull List<@NotNull DisconnectInboundInterceptor> getDisconnectInboundInterceptors() {
        return disconnectInbounds;
    }

    public @Immutable @NotNull List<@NotNull DisconnectOutboundInterceptor> getDisconnectOutboundInterceptorsOfExtension(
            final @NotNull ClassLoader extensionClassLoader) {

        return filterInterceptorsOfExtension(disconnectOutbounds, extensionClassLoader);
    }

    public @Immutable @NotNull List<@NotNull DisconnectOutboundInterceptor> getDisconnectOutboundInterceptors() {
        return disconnectOutbounds;
    }

    public @Immutable @NotNull List<@NotNull PingReqInboundInterceptor> getPingReqInboundInterceptorsOfExtension(
            final @NotNull ClassLoader extensionClassLoader) {

        return filterInterceptorsOfExtension(pingReqInbounds, extensionClassLoader);
    }

    public @Immutable @NotNull List<@NotNull PingReqInboundInterceptor> getPingReqInboundInterceptors() {
        return pingReqInbounds;
    }

    public @Immutable @NotNull List<@NotNull PingRespOutboundInterceptor> getPingRespOutboundInterceptorsOfExtension(
            final @NotNull ClassLoader extensionClassLoader) {

        return filterInterceptorsOfExtension(pingRespOutbounds, extensionClassLoader);
    }

    public @Immutable @NotNull List<@NotNull PingRespOutboundInterceptor> getPingRespOutboundInterceptors() {
        return pingRespOutbounds;
    }

    public @NotNull ModifiableDefaultPermissions getDefaultPermissions() {
        return defaultPermissions;
    }

    private <T extends Interceptor> @NotNull ImmutableList<T> addInterceptor(
            final @NotNull ImmutableList<T> interceptors, final @NotNull T interceptor) {

        if (interceptors.isEmpty()) {
            return ImmutableList.of(interceptor);
        }
        final int priority = getExtensionPriority(interceptor);
        int low = 0;
        int high = interceptors.size() - 1;
        while (low <= high) {
            final int mid = low + high >>> 1;
            final T midInterceptor = interceptors.get(mid);
            final int midPriority = getExtensionPriority(midInterceptor);

            if (midPriority >= priority) {
                if (midPriority == priority && midInterceptor == interceptor) {
                    return interceptors;
                }
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        final ImmutableList.Builder<T> builder = ImmutableList.builderWithExpectedSize(interceptors.size() + 1);
        for (int i = 0; i < low; i++) {
            builder.add(interceptors.get(i));
        }
        builder.add(interceptor);
        for (int i = low; i < interceptors.size(); i++) {
            builder.add(interceptors.get(i));
        }
        return builder.build();
    }

    private <T extends Interceptor> @NotNull ImmutableList<T> removeInterceptor(
            final @NotNull ImmutableList<T> interceptors, final @NotNull T interceptor) {

        if (interceptors.isEmpty()) {
            return interceptors;
        }
        if (interceptors.size() == 1 && interceptors.get(0) == interceptor) {
            return ImmutableList.of();
        }
        final int priority = getExtensionPriority(interceptor);
        int low = 0;
        int high = interceptors.size() - 1;
        while (low <= high) {
            final int mid = low + high >>> 1;
            final T midInterceptor = interceptors.get(mid);
            final int midPriority = getExtensionPriority(midInterceptor);

            if (midPriority >= priority) {
                if (midPriority == priority && midInterceptor == interceptor) {
                    final ImmutableList.Builder<T> builder =
                            ImmutableList.builderWithExpectedSize(interceptors.size() - 1);
                    for (int i = 0; i < mid; i++) {
                        builder.add(interceptors.get(i));
                    }
                    for (int i = mid + 1; i < interceptors.size(); i++) {
                        builder.add(interceptors.get(i));
                    }
                    return builder.build();
                }
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return interceptors;
    }

    private <T extends Interceptor> @NotNull ImmutableList<T> filterInterceptorsOfExtension(
            final @NotNull ImmutableList<T> interceptors,
            final @NotNull ClassLoader extensionClassloader) {

        final ImmutableList.Builder<T> builder = ImmutableList.builder();
        for (int i = 0; i < interceptors.size(); i++) {
            final T interceptor = interceptors.get(i);
            if (interceptor.getClass().getClassLoader().equals(extensionClassloader)) {
                builder.add(interceptor);
            }
        }
        return builder.build();
    }

    private <T extends Interceptor> @NotNull ImmutableList<T> removeInterceptorsOfExtension(
            final @NotNull ImmutableList<T> interceptors,
            final @NotNull ClassLoader extensionClassloader) {

        final ImmutableList.Builder<T> builder = ImmutableList.builder();
        for (int i = 0; i < interceptors.size(); i++) {
            final T interceptor = interceptors.get(i);
            if (!interceptor.getClass().getClassLoader().equals(extensionClassloader)) {
                builder.add(interceptor);
            }
        }
        return builder.build();
    }

    private int getExtensionPriority(final @NotNull Object object) {
        final HiveMQExtension extension = hiveMQExtensions.getExtensionForClassloader(object.getClass().getClassLoader());
        if (extension != null) {
            return extension.getPriority();
        } else {
            return -1;
        }
    }
}
