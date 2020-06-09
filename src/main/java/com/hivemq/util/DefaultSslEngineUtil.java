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
package com.hivemq.util;

import com.google.common.collect.ImmutableList;
import com.hivemq.annotations.ReadOnly;
import com.hivemq.security.exception.SslException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * A utility class which allows to find out information about
 * the default SSL Engine from the JVM
 *
 * @author Christoph Schäbel
 * @author Dominik Obermaier
 */
public class DefaultSslEngineUtil {

    /**
     * Returns a list of all supported Cipher Suites of the JVM.
     *
     * @return a list of all supported cipher suites of the JVM
     * @throws SslException
     */
    @ReadOnly
    public List<String> getSupportedCipherSuites() throws SslException {

        try {
            final SSLEngine engine = getDefaultSslEngine();

            return ImmutableList.copyOf(engine.getSupportedCipherSuites());

        } catch (final NoSuchAlgorithmException | KeyManagementException e) {
            throw new SslException("Not able to get list of supported cipher suites from JVM", e);
        }
    }

    /**
     * Returns a list of all supported protocols by the JVM.
     *
     * @return a list of all supported protocols by the JVM
     * @throws SslException
     */
    @ReadOnly
    public List<String> getSupportedProtocols() throws SslException {

        try {
            final SSLEngine engine = getDefaultSslEngine();

            return ImmutableList.copyOf(engine.getSupportedProtocols());

        } catch (final NoSuchAlgorithmException | KeyManagementException e) {
            throw new SslException("Not able to get list of supported protocols from JVM", e);
        }
    }

    /**
     * Returns a list of all enabled protocols of the JVM
     *
     * @return a list of all enabled protocls of the JVM
     * @throws SslException
     */
    @ReadOnly
    public List<String> getEnabledProtocols() throws SslException {
        try {
            final SSLEngine engine = getDefaultSslEngine();

            return ImmutableList.copyOf(engine.getEnabledProtocols());

        } catch (final NoSuchAlgorithmException | KeyManagementException e) {
            throw new SslException("Not able to get list of enabled protocols from JVM", e);
        }
    }

    /**
     * Returns a list of all enabled cipher suites of the JVM
     *
     * @return a list of all enabled cipher suites of the JVM
     * @throws SslException
     */
    @ReadOnly
    public List<String> getEnabledCipherSuites() throws SslException {
        try {
            final SSLEngine engine = getDefaultSslEngine();

            return ImmutableList.copyOf(engine.getEnabledCipherSuites());

        } catch (final NoSuchAlgorithmException | KeyManagementException e) {
            throw new SslException("Not able to get list of enabled cipher suites from JVM", e);
        }
    }

    private SSLEngine getDefaultSslEngine() throws NoSuchAlgorithmException, KeyManagementException {

        final SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, null, null);

        return context.createSSLEngine();
    }
}
