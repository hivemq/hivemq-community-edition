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

import com.hivemq.configuration.service.entity.Tls;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.security.exception.SslException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public final class SslUtil {

    public static @NotNull KeyManagerFactory getKeyManagerFactory(final @NotNull Tls tls) throws SslException {
        return createKeyManagerFactory(tls.getKeystoreType(),
                tls.getKeystorePath(),
                tls.getKeystorePassword(),
                tls.getPrivateKeyPassword());
    }

    public static @NotNull KeyManagerFactory createKeyManagerFactory(
            final @NotNull String keyStoreType,
            final @NotNull String keyStorePath,
            final @NotNull String keyStorePassword,
            final @NotNull String privateKeyPassword) {
        try (final FileInputStream fileInputStream = new FileInputStream(keyStorePath)) {
            //load keystore from TLS config
            final KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(fileInputStream, keyStorePassword.toCharArray());

            //set up KeyManagerFactory with private-key-password from TLS config
            final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, privateKeyPassword.toCharArray());
            return kmf;

        } catch (final UnrecoverableKeyException e1) {
            throw new SslException(
                    "Not able to recover key from KeyStore, please check your private-key-password and your keyStorePassword",
                    e1);
        } catch (final FileNotFoundException e) {
            throw new SslException("Cannot find KeyStore at path '" + keyStorePath + "'");
        } catch (final KeyStoreException | IOException e) {
            throw new SslException(String.format("Not able to open or read KeyStore '%s' with type '%s'",
                    keyStorePath,
                    keyStoreType), e);
        } catch (final NoSuchAlgorithmException | CertificateException e) {
            throw new SslException("Not able to read the certificate from KeyStore '" + keyStorePath + "'", e);
        }
    }

    public static @Nullable TrustManagerFactory getTrustManagerFactory(final @NotNull Tls tls) throws SslException {
        return isNotBlank(tls.getTruststorePath()) &&
                tls.getTruststoreType() != null &&
                tls.getTruststorePassword() != null ?
                createTrustManagerFactory(tls.getTruststoreType(),
                        tls.getTruststorePath(),
                        tls.getTruststorePassword()) :
                null;
    }

    public static @NotNull TrustManagerFactory createTrustManagerFactory(
            final @NotNull String trustStoreType,
            final @NotNull String trustStorePath,
            final @NotNull String trustStorePassword) {
        try (final FileInputStream fileInputStream = new FileInputStream(trustStorePath)) {
            //load keystore from TLS config
            final KeyStore keyStoreTrust = KeyStore.getInstance(trustStoreType);
            keyStoreTrust.load(fileInputStream, trustStorePassword.toCharArray());

            //set up TrustManagerFactory
            final TrustManagerFactory tmFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmFactory.init(keyStoreTrust);
            return tmFactory;
        } catch (final FileNotFoundException e) {
            throw new SslException("Cannot find TrustStore at path '" + trustStorePath + "'");
        } catch (final KeyStoreException | IOException e) {
            throw new SslException(String.format("Not able to open or read TrustStore '%s' with type '%s'",
                    trustStorePath,
                    trustStoreType), e);
        } catch (final NoSuchAlgorithmException | CertificateException e) {
            throw new SslException("Not able to read the certificate from TrustStore '" + trustStorePath + "'", e);
        }
    }

    private SslUtil() {
    }
}
