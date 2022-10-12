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
package util;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.bc.BcECContentSignerBuilder;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TestKeyStoreGenerator {

    public static final String KEY_ALIAS = "hivemqkeys";

    private final @NotNull BouncyCastleProvider bouncyCastleProvider;

    public TestKeyStoreGenerator() {
        bouncyCastleProvider = new BouncyCastleProvider();
        Security.addProvider(bouncyCastleProvider);
    }

    public void release() {
        Security.removeProvider(bouncyCastleProvider.getName());
    }

    @NotNull
    public File generateKeyStore(final @NotNull String name, final @NotNull String keystoreType, final @NotNull String keyStorePassword, final @NotNull String privateKeyPassword) throws Exception {
        return generateKeyStore(name, keystoreType, keyStorePassword, privateKeyPassword, true, false);
    }

    @NotNull
    public File generateKeyStore(final @NotNull String name, final @NotNull String keystoreType, final @NotNull String keyStorePassword, final @NotNull String privateKeyPassword, final boolean withX500, final boolean eclipticCurve) throws Exception {

        final KeyStore ks = KeyStore.getInstance(keystoreType);
        ks.load(null);

        final KeyPair keyPair = eclipticCurve ? generateECKeyPair() : generateRSAKeyPair();
        final X509Certificate certificate = generateX509Certificate(keyPair, name, withX500, eclipticCurve);

        final X509Certificate[] certificateChain = {certificate};

        ks.setKeyEntry(KEY_ALIAS, keyPair.getPrivate(), privateKeyPassword.toCharArray(), certificateChain);

        final File keyStoreFile = File.createTempFile(name, null);
        keyStoreFile.deleteOnExit();

        final FileOutputStream fos = new FileOutputStream(
                keyStoreFile);
        ks.store(fos, keyStorePassword.toCharArray());
        fos.close();
        return keyStoreFile;
    }

    @NotNull
    private X509Certificate generateX509Certificate(final @NotNull KeyPair keyPair, final @NotNull String name, final boolean withX500, final boolean eclipticCurve) throws Exception {

        final X500Name x500Name;

        if (withX500) {
            //CN = Common Name, OU = Organisational Unit, O = Organisation, C = Country, ST = State
            x500Name = new X500Name("CN=" + name + ", OU=" + name + ", O=" + name + ", C=" + name + ", ST=" + name);
        } else {
            //At least 1 attribute is required
            x500Name = new X500Name("CN=" + name);
        }

        final X509v3CertificateBuilder builder = new X509v3CertificateBuilder(

                x500Name,
                BigInteger.valueOf(new SecureRandom().nextLong()),
                new Date(System.currentTimeMillis() - 10000),
                new Date(System.currentTimeMillis() + 24L * 3600 * 1000),
                x500Name,
                SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded()));

        final List<GeneralName> altNames = new ArrayList<>();
        altNames.add(new GeneralName(GeneralName.dNSName, "localhost"));
        altNames.add(new GeneralName(GeneralName.iPAddress, "127.0.0.1"));
        final GeneralNames subjectAltNames = GeneralNames.getInstance(new DERSequence(
                altNames.toArray(new GeneralName[]{})));
        builder.addExtension(Extension.subjectAlternativeName, false, subjectAltNames);

        final X509CertificateHolder holder = builder.build(eclipticCurve ? createECContentSigner(keyPair) : createRSAContentSigner(keyPair));
        final org.bouncycastle.asn1.x509.Certificate certificate = holder.toASN1Structure();

        final InputStream is = new ByteArrayInputStream(certificate.getEncoded());

        final X509Certificate x509Certificate = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(is);
        is.close();
        return x509Certificate;
    }

    @NotNull
    public KeyPair generateRSAKeyPair() throws NoSuchProviderException, NoSuchAlgorithmException {
        final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
        keyPairGenerator.initialize(2048, new SecureRandom());
        return keyPairGenerator.generateKeyPair();
    }

    @NotNull
    public KeyPair generateECKeyPair() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchProviderException {
        final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC", "BC");
        keyGen.initialize(new ECGenParameterSpec("secp256r1"), new SecureRandom());
        return keyGen.generateKeyPair();
    }

    @NotNull
    private ContentSigner createRSAContentSigner(final KeyPair keyPair) throws Exception {
        final AlgorithmIdentifier signatureAlgorithmId = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA256withRSA");
        final AlgorithmIdentifier digestAlgorithmId = new DefaultDigestAlgorithmIdentifierFinder().find(signatureAlgorithmId);

        final byte[] encoded = keyPair.getPrivate().getEncoded();
        final AsymmetricKeyParameter privateKey = PrivateKeyFactory.createKey(encoded);

        return new BcRSAContentSignerBuilder(signatureAlgorithmId, digestAlgorithmId).build(privateKey);
    }

    @NotNull
    private ContentSigner createECContentSigner(final KeyPair keyPair) throws Exception {
        final AlgorithmIdentifier signatureAlgorithmId = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA256withECDSA");
        final AlgorithmIdentifier digestAlgorithmId = new DefaultDigestAlgorithmIdentifierFinder().find(signatureAlgorithmId);

        final byte[] encoded = keyPair.getPrivate().getEncoded();
        final AsymmetricKeyParameter privateKey = PrivateKeyFactory.createKey(encoded);

        return new BcECContentSignerBuilder(signatureAlgorithmId, digestAlgorithmId).build(privateKey);
    }

}
