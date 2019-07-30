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

package com.hivemq.extension.sdk.api.packets.publish;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.interceptor.connect.parameter.ConnectInboundInput;
import com.hivemq.extension.sdk.api.packets.connect.ConnectPacket;
import com.hivemq.extension.sdk.api.packets.connect.WillPublishPacket;
import com.hivemq.extension.sdk.api.packets.general.ModifiableUserProperties;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extension.sdk.api.services.builder.Builders;
import com.hivemq.extension.sdk.api.services.builder.WillPublishBuilder;
import com.hivemq.extension.sdk.api.interceptor.connect.ConnectInboundInterceptor;

import java.nio.ByteBuffer;
import java.util.Optional;

/**
 * A modifiable version of the {@link ConnectPacket}.
 *
 * @author Lukas Brandl
 * @since 4.2.0
 */
public interface ModifiableConnectPacket extends ConnectPacket {

    /**
     * Set the client ID.
     * <p>
     * In case the client ID is changed, future interceptors may be called by a different thread for the same client.
     * Extensions need to ensure thread-safety for shared objects in this case.
     * Interceptors are still called in the same order for the client.
     * <p>
     * The client ID provided by the {@link ClientInformation} in the {@link ConnectInboundInput} is not updated until
     * all {@link ConnectInboundInterceptor} for this CONNECT are finished.
     *
     * @param clientId The new client ID of the CONNECT.
     * @throws IllegalArgumentException If the client ID is not a valid UTF-8 string.
     * @throws IllegalArgumentException If the client ID exceeds the maximum client ID length.
     * @throws IllegalArgumentException If the client ID is empty.
     * @since 4.2.0
     */
    void setClientId(@NotNull String clientId);

    /**
     * Set the clean start flag.
     * <p>
     * For an MQTT 3 client clean start has the same value as clean session by default.
     * However this MQTT 5 property can be used in the same way as for MQTT 5 client via this method.
     *
     * @param cleanStart The new clean start flag of the CONNECT.
     * @since 4.2.0
     */
    void setCleanStart(boolean cleanStart);

    /**
     * Set the will publish.
     * <p>
     * Use {@link Builders#willPublish()} to get the {@link WillPublishBuilder} to create a will publish.
     * To remove a will publish set this to null.
     * <p>
     * To modify the existing {@link WillPublishPacket} use {@link #getModifiableWillPublish()}.
     *
     * @param willPublish The new will publish for the CONNECT.
     * @since 4.2.0
     */
    void setWillPublish(@Nullable WillPublishPacket willPublish);

    /**
     * Set the expiry interval.
     * <p>
     * For an MQTT 3 client the expiry will be 0 by default if clean session is false.
     * This method may be used to set a custom session expiry for MQTT 3 clients.
     *
     * @param expiryInterval The new expiry interval for the CONNECT.
     * @throws IllegalArgumentException If the expiry interval is more than the configured maximum.
     * @since 4.2.0
     */
    void setSessionExpiryInterval(long expiryInterval);

    /**
     * Set the keep alive.
     *
     * @param keepAlive The new keep alive for the CONNECT.
     * @throws IllegalArgumentException If the keep alive is more than the configured maximum.
     * @since 4.2.0
     */
    void setKeepAlive(int keepAlive);

    /**
     * Set the receive maximum.
     *
     * @param receiveMaximum The new receive maximum for the CONNECT.
     * @throws IllegalArgumentException If the receive maximum is less than one or more than '65535'.
     * @since 4.2.0
     */
    void setReceiveMaximum(int receiveMaximum);

    /**
     * Set the maximum packet size.
     *
     * @param maximumPacketSize The new maximum packet size for the CONNECT.
     * @throws IllegalArgumentException If the maximum packet size is less than one or more than the configured
     *                                  maximum.
     * @since 4.2.0
     */
    void setMaximumPacketSize(int maximumPacketSize);

    /**
     * Set the topic alias maximum.
     * This setting is only respected for MQTT 5 clients. For MQTT 3.x clients this setting is ignored.
     *
     * @param topicAliasMaximum The new topic alias maximum for the CONNECT.
     * @throws IllegalArgumentException If the topic alias maximum is more than '65535'.
     * @since 4.2.0
     */
    void setTopicAliasMaximum(int topicAliasMaximum);

    /**
     * Set the request response information.
     * This setting is only respected for MQTT 5 clients. For MQTT 3.x clients this setting is ignored.
     *
     * @param requestResponseInformation The new request response information flag for the CONNECT.
     * @since 4.2.0
     */
    void setRequestResponseInformation(boolean requestResponseInformation);

    /**
     * Set the request problem information.
     * This setting is only respected for MQTT 5 clients. For MQTT 3.x clients this setting is ignored.
     *
     * @param requestProblemInformation The new request problem information flag for the CONNECT.
     * @since 4.2.0
     */
    void setRequestProblemInformation(boolean requestProblemInformation);

    /**
     * Set the authentication method.
     *
     * @param authenticationMethod The new authentication method of the CONNECT.
     * @throws IllegalArgumentException If the authentication method is not a valid UTF-8 string.
     * @since 4.2.0
     */
    void setAuthenticationMethod(@Nullable String authenticationMethod);

    /**
     * Set the authentication data.
     *
     * @param authenticationData The new authentication data of the CONNECT.
     * @since 4.2.0
     */
    void setAuthenticationData(@Nullable ByteBuffer authenticationData);

    /**
     * Set the username.
     *
     * @param userName The new username for the CONNECT.
     * @since 4.2.0
     */
    void setUserName(@Nullable String userName);

    /**
     * Set the password.
     *
     * @param password The new password for the CONNECT.
     * @since 4.2.0
     */
    void setPassword(@Nullable ByteBuffer password);

    /**
     * Get the modifiable {@link UserProperties} of the CONNECT packet.
     *
     * @return Modifiable user properties.
     * @since 4.2.0
     */
    @Override
    @NotNull
    ModifiableUserProperties getUserProperties();

    /**
     * Get a modifiable instance of the {@link WillPublishPacket}.
     *
     * @return Modifiable will publish.
     * @since 4.2.0
     */
    @NotNull
    Optional<ModifiableWillPublish> getModifiableWillPublish();
}
