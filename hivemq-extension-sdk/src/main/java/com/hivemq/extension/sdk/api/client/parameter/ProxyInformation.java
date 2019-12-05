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
package com.hivemq.extension.sdk.api.client.parameter;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;

/**
 * This class encapsulates all information that is forwarded by a load
 * balancer which uses the PROXY protocol. Except from the source connection
 * information and the load balancer information, most of the values are optional.
 * <p>
 * HiveMQ supports arbitrary TLVs which can be retrieved as raw TLV values (see {@link #getRawTLVs()})
 *
 * @author Dominik Obermaier
 * @author Christoph Schäbel
 * @since 4.0.0
 */
@DoNotImplement
public interface ProxyInformation {

    /**
     * @return The original source port of the MQTT client.
     * @since 4.0.0
     */
    int getSourcePort();

    /**
     * @return The original source address of the MQTT client.
     * @since 4.0.0
     */
    @NotNull InetAddress getSourceAddress();

    /**
     * @return The port of the load balancer that is used to proxy the client connection.
     * @since 4.0.0
     */
    int getProxyPort();

    /**
     * @return The address of the load balancer that is used to proxy the client connection.
     * @since 4.0.0
     */
    @NotNull InetAddress getProxyAddress();

    /**
     * If the PROXY protocol implementation of the load balancer supports TLVs and proxies
     * a SSL connection, this method returns the TLS version of the original SSL connection.
     *
     * @return An {@link Optional} that contains the original TLS version if supported by the load balancer.
     * @since 4.0.0
     */
    @NotNull Optional<String> getTlsVersion();

    /**
     * If the PROXY protocol implementation of the load balancer supports TLVs and proxies
     * a SSL connection with a X509 client certificate that is sent by the MQTT client,
     * this method returns the forwarded common name of the X509 client certificate
     * (if the client used one to authenticate the SSL connection).
     *
     * @return An {@link Optional} that contains the Common Name of the X509 client certificate.
     * @since 4.0.0
     */
    @NotNull Optional<String> getSslCertificateCN();

    /**
     * HiveMQ supports arbitrary TLVs, even TLVs that aren't specified by the PROXY protocol. This map
     * contains all the raw TLVs that are sent by the load balancer.
     * <p>
     * The key is the byte value of the TLV type and the value is the raw TLV as byte value.
     *
     * @return A {@link Map} with raw TLVs.
     * @since 4.0.0
     */
    @NotNull Map<Byte, ByteBuffer> getRawTLVs();
}
