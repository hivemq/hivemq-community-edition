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
package com.hivemq.bootstrap;

import com.google.inject.Injector;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mcp.ResourcesEndpoint;
import com.hivemq.mcp.oauth.OAuthAuthenticationFilter;
import com.hivemq.mcp.oauth.OAuthEndpoint;
import com.hivemq.mcp.oauth.OAuthMetadataEndpoint;
import io.undertow.Undertow;
import io.undertow.servlet.api.DeploymentInfo;
import org.jboss.resteasy.core.ResteasyDeploymentImpl;
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.Options;
import org.xnio.SslClientAuthMode;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * Bootstrap class for starting and stopping the MCP server.
 *
 * @author HiveMQ
 */
@Singleton
public class McpServerBootstrap {

    private static final Logger log = LoggerFactory.getLogger(McpServerBootstrap.class);

    private final @NotNull ShutdownHooks shutdownHooks;
    private final @NotNull Injector injector;
    private @Nullable UndertowJaxrsServer server;

    @Inject
    public McpServerBootstrap(
            final @NotNull ShutdownHooks shutdownHooks,
            final @NotNull Injector injector) {
        this.shutdownHooks = shutdownHooks;
        this.injector = injector;
    }

    /**
     * Starts the MCP server on the configured port.
     */
    public void start() {
        start(InternalConfigurations.MCP_SERVER_PORT.get());
    }

    /**
     * Starts the MCP server on the specified port.
     *
     * @param port the port to bind the MCP server to
     */
    public void start(final int port) {
        if (server != null) {
            log.warn("MCP server is already running");
            return;
        }

        try {
            log.info("Starting MCP server on HTTPS port {}", port);

            server = new UndertowJaxrsServer();

            // Create SSL context with self-signed certificate for development
            final SSLContext sslContext = createSelfSignedSSLContext();

            final Undertow.Builder serverBuilder = Undertow.builder()
                    .addHttpsListener(port, "0.0.0.0", sslContext)
                    .setSocketOption(Options.SSL_CLIENT_AUTH_MODE, SslClientAuthMode.NOT_REQUESTED);

            server.start(serverBuilder);

            // Create ResteasyDeployment and register resources
            final ResteasyDeploymentImpl deployment = new ResteasyDeploymentImpl();
            deployment.getResources().add(injector.getInstance(ResourcesEndpoint.class));
            deployment.getResources().add(injector.getInstance(OAuthMetadataEndpoint.class));
            deployment.getResources().add(injector.getInstance(OAuthEndpoint.class));

            // Register OAuth authentication filter
            deployment.getProviders().add(new OAuthAuthenticationFilter());

            // Create deployment info and deploy
            final DeploymentInfo deploymentInfo = server.undertowDeployment(deployment, "/");
            deploymentInfo.setDeploymentName("MCP Server");
            deploymentInfo.setContextPath("/");
            deploymentInfo.setClassLoader(getClass().getClassLoader());

            server.deploy(deploymentInfo);

            // Register shutdown hook
            shutdownHooks.add(new McpServerShutdownHook(server));

            log.info("MCP server started successfully on port {}", port);
        } catch (final Exception e) {
            log.error("Failed to start MCP server", e);
            throw new RuntimeException("Failed to start MCP server", e);
        }
    }

    /**
     * Creates a self-signed SSL context for development/testing purposes.
     * Uses BouncyCastle to generate a self-signed certificate.
     * In production, this should be replaced with proper certificate management.
     *
     * @return configured SSLContext
     * @throws Exception if SSL context creation fails
     */
    private @NotNull SSLContext createSelfSignedSSLContext() throws Exception {
        // Add BouncyCastle as a security provider
        Security.addProvider(new BouncyCastleProvider());

        // Generate RSA key pair
        final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        final KeyPair keyPair = keyPairGenerator.generateKeyPair();

        // Generate self-signed certificate
        final long now = System.currentTimeMillis();
        final Date startDate = new Date(now);
        final Date endDate = new Date(now + 365L * 24 * 60 * 60 * 1000); // Valid for 1 year

        final X500Name issuer = new X500Name("CN=HiveMQ MCP Server, O=HiveMQ, C=DE");
        final BigInteger serialNumber = new BigInteger(64, new SecureRandom());

        final X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuer,
                serialNumber,
                startDate,
                endDate,
                issuer,
                keyPair.getPublic()
        );

        final ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA")
                .setProvider("BC")
                .build(keyPair.getPrivate());

        final X509Certificate certificate = new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(certBuilder.build(signer));

        // Create KeyStore and add the certificate
        final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        final char[] password = "changeit".toCharArray();
        keyStore.setKeyEntry("mcpserver", keyPair.getPrivate(), password,
                new java.security.cert.Certificate[]{certificate});

        // Create KeyManagerFactory
        final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password);

        // Initialize SSL context
        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom());

        log.warn("MCP Server is using a self-signed certificate. This should not be used in production!");

        return sslContext;
    }
}
