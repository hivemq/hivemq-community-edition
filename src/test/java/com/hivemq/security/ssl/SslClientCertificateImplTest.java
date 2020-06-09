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

import com.hivemq.security.auth.SslClientCertificate;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SslClientCertificateImplTest {

    private Certificate certificate;
    private SslClientCertificate clientCertificate;

    @Before
    public void before() throws Exception {

        certificate = generateCert();

        final Certificate[] certificates = new Certificate[1];
        certificates[0] = certificate;

        clientCertificate = new SslClientCertificateImpl(certificates);
    }


    @Test
    public void test_certificate() {
        assertEquals(certificate, clientCertificate.certificate());
    }

    @Test
    public void test_commonName() {
        assertEquals("Test commonName", clientCertificate.commonName());
    }

    @Test
    public void test_organization() {
        assertEquals("Test organization", clientCertificate.organization());
    }

    @Test
    public void test_organizationalUnit() {
        assertEquals("Test Unit", clientCertificate.organizationalUnit());
    }

    @Test
    public void test_title() {
        assertEquals("Test Title", clientCertificate.title());
    }

    @Test
    public void test_serial() {
        assertEquals("123456789", clientCertificate.serial());
    }

    @Test
    public void test_country() {
        assertEquals("DE", clientCertificate.country());
    }

    @Test
    public void test_locality() {
        assertEquals("Test locality", clientCertificate.locality());
    }

    @Test
    public void test_state() {
        assertEquals("Test state", clientCertificate.state());
    }

    @Test
    public void test_bad_common_name() throws Exception {
        createBadCert();
        assertEquals("", clientCertificate.commonName());
    }

    @Test
    public void test_bad_extension() throws Exception {
        createBadCert();
        assertNull(clientCertificate.organization());
        assertNull(clientCertificate.organizationalUnit());
        assertNull(clientCertificate.title());
        assertEquals("0", clientCertificate.serial());
        assertNull(clientCertificate.country());
        assertNull(clientCertificate.locality());
        assertNull(clientCertificate.state());
        assertNull(clientCertificate.state());
    }

    @Test(expected = NullPointerException.class)
    public void test_certs_not_null() throws Exception {
        new SslClientCertificateImpl(null);
    }

    @Test
    public void test_cert_with_extensions() throws Exception {

        certificate = generateCertWithExtension();

        final Certificate[] certificates = new Certificate[1];
        certificates[0] = certificate;

        clientCertificate = new SslClientCertificateImpl(certificates);

        assertEquals("Test commonName", clientCertificate.commonName());
        assertEquals("Test organization", clientCertificate.organization());
        assertEquals("Test Unit", clientCertificate.organizationalUnit());
        assertEquals("Test Title", clientCertificate.title());
        assertEquals("123456789", clientCertificate.serial());
        assertEquals("DE", clientCertificate.country());
        assertEquals("Test locality", clientCertificate.locality());
        assertEquals("Test state", clientCertificate.state());

    }

    private void createBadCert() throws Exception {
        certificate = generateBadCert();

        final Certificate[] certificates = new Certificate[1];
        certificates[0] = certificate;

        clientCertificate = new SslClientCertificateImpl(certificates);
    }

    private Certificate generateBadCert() throws Exception {
        final KeyPair keyPair = createKeyPair();

        final JcaX509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(
                new X500Name("CN="),
                BigInteger.valueOf(0),
                new Date(System.currentTimeMillis() - 10000),
                new Date(System.currentTimeMillis() + 10000),
                new X500Name("CN="),
                keyPair.getPublic()
        );

        return getCertificate(keyPair, certificateBuilder);
    }

    private Certificate generateCert() throws Exception {
        final KeyPair keyPair = createKeyPair();

        final JcaX509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(
                new X500Name("CN=Test commonName, C=DE, O=Test organization, OU=Test Unit, T=Test Title, L=Test locality, ST=Test state"),
                BigInteger.valueOf(123456789),
                new Date(System.currentTimeMillis() - 10000),
                new Date(System.currentTimeMillis() + 10000),
                new X500Name("CN=Test commonName, C=DE, O=Test organization, OU=Test Unit, T=Test Title, L=Test locality, ST=Test state"),
                keyPair.getPublic()
        );

        return getCertificate(keyPair, certificateBuilder);
    }

    private Certificate generateCertWithExtension() throws Exception {
        final KeyPair keyPair = createKeyPair();

        final JcaX509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(
                new X500Name("CN=Test commonName"),
                BigInteger.valueOf(123456789),
                new Date(System.currentTimeMillis() - 10000),
                new Date(System.currentTimeMillis() + 10000),
                new X500Name("CN=Test commonName"),
                keyPair.getPublic()
        );

        certificateBuilder.addExtension(BCStyle.C, false, new DERUTF8String("DE"));
        certificateBuilder.addExtension(BCStyle.O, false, new DERUTF8String("Test organization"));
        certificateBuilder.addExtension(BCStyle.OU, false, new DERUTF8String("Test Unit"));
        certificateBuilder.addExtension(BCStyle.T, false, new DERUTF8String("Test Title"));
        certificateBuilder.addExtension(BCStyle.L, false, new DERUTF8String("Test locality"));
        certificateBuilder.addExtension(BCStyle.ST, false, new DERUTF8String("Test state"));

        return getCertificate(keyPair, certificateBuilder);
    }

    private Certificate getCertificate(final KeyPair keyPair, final JcaX509v3CertificateBuilder certificateBuilder) throws OperatorCreationException, CertificateException {

        Security.addProvider(new BouncyCastleProvider());

        JcaContentSignerBuilder signerBuilder = new JcaContentSignerBuilder("SHA256WithRSAEncryption");
        signerBuilder = signerBuilder.setProvider(BouncyCastleProvider.PROVIDER_NAME);

        final ContentSigner contentSigner = signerBuilder.build(keyPair.getPrivate());

        JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
        converter = converter.setProvider(BouncyCastleProvider.PROVIDER_NAME);

        return converter.getCertificate(certificateBuilder.build(contentSigner));
    }

    private KeyPair createKeyPair() throws InvalidKeySpecException, NoSuchAlgorithmException {

        final RSAKeyPairGenerator gen = new RSAKeyPairGenerator();

        gen.init(new RSAKeyGenerationParameters(BigInteger.valueOf(3), new SecureRandom(), 1024, 80));
        final AsymmetricCipherKeyPair keypair = gen.generateKeyPair();

        final RSAKeyParameters publicKey = (RSAKeyParameters) keypair.getPublic();
        final RSAPrivateCrtKeyParameters privateKey = (RSAPrivateCrtKeyParameters) keypair.getPrivate();

        final PublicKey pubKey = KeyFactory.getInstance("RSA").generatePublic(
                new RSAPublicKeySpec(publicKey.getModulus(), publicKey.getExponent()));

        final PrivateKey privKey = KeyFactory.getInstance("RSA").generatePrivate(
                new RSAPrivateCrtKeySpec(publicKey.getModulus(), publicKey.getExponent(),
                        privateKey.getExponent(), privateKey.getP(), privateKey.getQ(),
                        privateKey.getDP(), privateKey.getDQ(), privateKey.getQInv()));

        return new KeyPair(pubKey, privKey);
    }

}