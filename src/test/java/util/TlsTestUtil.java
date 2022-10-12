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

import com.hivemq.configuration.service.entity.Tls;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.ArrayList;

public final class TlsTestUtil {

    public static @NotNull Tls createDefaultTLS() {
        return createDefaultTLSBuilder().build();
    }

    public static @NotNull Tls.Builder createDefaultTLSBuilder() {
        return new Tls.Builder()
                .withKeystorePath("")
                .withKeystorePassword("")
                .withKeystoreType("JKS")
                .withPrivateKeyPassword("")
                .withHandshakeTimeout(10)
                .withClientAuthMode(Tls.ClientAuthMode.NONE)
                .withCipherSuites(new ArrayList<>())
                .withProtocols(new ArrayList<>());
    }

    private TlsTestUtil() {
    }
}
