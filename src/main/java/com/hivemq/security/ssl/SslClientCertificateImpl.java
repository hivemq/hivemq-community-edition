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

import com.google.common.base.Preconditions;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.security.auth.SslClientCertificate;
import com.hivemq.security.exception.PropertyNotFoundException;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;

import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

/**
 * @author Christoph Schäbel
 */
public class SslClientCertificateImpl implements SslClientCertificate {

    private final Certificate[] certificates;

    public SslClientCertificateImpl(@NotNull final Certificate[] certificates) {
        Preconditions.checkNotNull(certificates, "Certificates must not be null");
        this.certificates = certificates;
    }

    @Override
    public Certificate certificate() {
        return certificates[0];
    }

    @Override
    public Certificate[] certificateChain() {
        return certificates;
    }

    @Override
    public String commonName() {
        return certificateProperty(BCStyle.CN);
    }

    @Override
    public String organization() {
        return certificateProperty(BCStyle.O);
    }

    @Override
    public String organizationalUnit() {
        return certificateProperty(BCStyle.OU);
    }

    @Override
    public String title() {
        return certificateProperty(BCStyle.T);
    }

    @Override
    public String serial() {
        return certificateProperty(BCStyle.SN);
    }

    @Override
    public String country() {
        return certificateProperty(BCStyle.C);
    }

    @Override
    public String locality() {
        return certificateProperty(BCStyle.L);
    }

    @Override
    public String state() {
        return certificateProperty(BCStyle.ST);
    }

    private String certificateProperty(final ASN1ObjectIdentifier objectIdentifier) {

        try {
            final X509Certificate cert = (X509Certificate) certificate();

            //x500 name values may be here or in extension
            final String subjectProperty = subjectProperty(objectIdentifier, cert);

            if (subjectProperty != null) {
                return subjectProperty;
            }

            if (objectIdentifier.equals(BCStyle.SN)) {
                return cert.getSerialNumber().toString();
            }

            //x500 name values may be here or in subject
            final Extension extension = new JcaX509CertificateHolder(cert).getExtension(objectIdentifier);
            if (extension == null) {
                return null;
            }
            return extension.getParsedValue().toString();

        } catch (final Exception e) {
            throw new PropertyNotFoundException("Not able to get property from certificate", e);
        }
    }

    @Nullable
    private String subjectProperty(final ASN1ObjectIdentifier objectIdentifier, final X509Certificate cert) throws CertificateEncodingException {
        final X500Name x500name = new JcaX509CertificateHolder(cert).getSubject();
        final RDN[] rdNs = x500name.getRDNs(objectIdentifier);
        if (rdNs.length < 1) {
            return null;
        }
        final RDN cn = rdNs[0];
        return IETFUtils.valueToString(cn.getFirst().getValue());
    }

}
