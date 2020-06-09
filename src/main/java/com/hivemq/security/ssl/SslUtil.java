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
package com.hivemq.security.ssl;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.configuration.service.entity.Tls;
import com.hivemq.security.exception.SslException;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import org.apache.commons.lang3.StringUtils;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.List;

/**
 * @author Georg Held
 */
@LazySingleton
public class SslUtil {

    @NotNull
    public TrustManagerFactory createTrustManagerFactory(@NotNull final String trustStoreType, @NotNull final String trustStorePath, @NotNull final String trustStorePassword) {
        try (final FileInputStream fileInputStream = new FileInputStream(new File(trustStorePath))) {
            //load keystore from TLS config
            final KeyStore keyStoreTrust = KeyStore.getInstance(trustStoreType);
            keyStoreTrust.load(fileInputStream,
                    trustStorePassword.toCharArray());

            //set up TrustManagerFactory
            final TrustManagerFactory tmFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmFactory.init(keyStoreTrust);
            return tmFactory;
        } catch (final FileNotFoundException e1) {
            throw new SslException("Cannot find TrustStore at path " + trustStorePath);
        } catch (final KeyStoreException | IOException e2) {
            throw new SslException("Not able to open or read TrustStore '"
                    + trustStorePath + "' with type '" +
                    trustStoreType + "'", e2);
        } catch (final NoSuchAlgorithmException | CertificateException e3) {
            throw new SslException("Not able to read certificate from TrustStore '" + trustStorePath, e3);
        }
    }

    @NotNull
    public KeyManagerFactory createKeyManagerFactory(@NotNull final String keyStoreType, @NotNull final String keyStorePath, @NotNull final String keyStorePassword, @NotNull final String privateKeyPassword) {
        try (final FileInputStream fileInputStream = new FileInputStream(new File(keyStorePath))) {
            //load keystore from TLS config
            final KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(fileInputStream,
                    keyStorePassword.toCharArray());

            //set up KeyManagerFactory with private-key-password from TLS config
            final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, privateKeyPassword.toCharArray());
            return kmf;

        } catch (final UnrecoverableKeyException e1) {
            throw new SslException("Not able to recover key from KeyStore, please check your private-key-password and your keyStorePassword", e1);
        } catch (final FileNotFoundException e1) {
            throw new SslException("Cannot find KeyStore at path " + keyStorePath);
        } catch (final KeyStoreException | IOException e2) {
            throw new SslException("Not able to open or read KeyStore '"
                    + keyStorePath + "' with type '" +
                    keyStoreType + "'", e2);
        } catch (final NoSuchAlgorithmException | CertificateException e3) {
            throw new SslException("Not able to read certificate from KeyStore '" + keyStorePath, e3);
        }
    }

    @NotNull
    public SslContext createSslServerContext(@NotNull final KeyManagerFactory kmf, @Nullable final TrustManagerFactory tmFactory, @Nullable final List<String> cipherSuites, @Nullable final List<String> protocols) throws SSLException {

        final SslContextBuilder sslContextBuilder = SslContextBuilder.forServer(kmf);

        sslContextBuilder.sslProvider(SslProvider.JDK).trustManager(tmFactory);

        if (protocols != null && !protocols.isEmpty()) {
            sslContextBuilder.protocols(protocols.toArray(new String[0]));
        }

        //set chosen cipher suites if available
        if (cipherSuites != null && cipherSuites.size() > 0) {
            sslContextBuilder.ciphers(cipherSuites, SupportedCipherSuiteFilter.INSTANCE);
        } else {
            sslContextBuilder.ciphers(null, SupportedCipherSuiteFilter.INSTANCE);
        }
        return sslContextBuilder.build();
    }

    @NotNull
    public KeyManagerFactory getKeyManagerFactory(@NotNull final Tls tls) throws SslException {
        return createKeyManagerFactory(tls.getKeystoreType(), tls.getKeystorePath(), tls.getKeystorePassword(), tls.getPrivateKeyPassword());
    }


    @Nullable
    public TrustManagerFactory getTrustManagerFactory(@NotNull final Tls tls) throws SslException {
        if (StringUtils.isBlank(tls.getTruststorePath())) {
            return null;
        }
        if (tls.getTruststoreType() != null && tls.getTruststorePassword() != null) {
            return createTrustManagerFactory(tls.getTruststoreType(), tls.getTruststorePath(), tls.getTruststorePassword());
        }
        return null;

    }
}
