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
package com.hivemq.configuration.reader;

import com.google.common.io.Files;
import com.hivemq.configuration.service.entity.*;
import org.junit.Test;

import java.io.File;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ListenerConfiguratorTest extends AbstractConfigurationTest {


    @Test
    public void test_read_tls_listener() throws Exception {

        final String contents = "" +
                "<hivemq>" +
                "    <listeners>" +
                "        <tls-tcp-listener>" +
                "            <port>8883</port>" +
                "            <bind-address>0.0.0.0</bind-address>" +
                "            <name>my-tls-tcp-listener</name>" +
                "            <tls>" +
                "               <cipher-suites>" +
                "                   <cipher-suite>TLS_RSA_WITH_AES_128_CBC_SHA</cipher-suite>" +
                "               </cipher-suites>" +
                "                <protocols>" +
                "                    <protocol>TLSv1.2</protocol>" +
                "                </protocols>" +
                "                <keystore>" +
                "                    <path>/absolute/path.jks</path>" +
                "                    <password>password-keystore</password>" +
                "                    <private-key-password>password-key</private-key-password>" +
                "                </keystore>" +
                "                <truststore>" +
                "                    <path>no_absolute_path.jks</path>" +
                "                    <password>password-truststore</password>" +
                "                </truststore>" +
                "                <client-authentication-mode>NONE</client-authentication-mode>" +
                "                <native-ssl>true</native-ssl>" +
                "            </tls>" +
                "        </tls-tcp-listener>" +
                "    </listeners>" +
                "</hivemq>";

        Files.write(contents.getBytes(UTF_8), xmlFile);

        reader.applyConfig();

        final TlsTcpListener tlsTcpListener = listenerConfigurationService.getTlsTcpListeners().get(0);

        assertEquals(8883, tlsTcpListener.getPort());
        assertEquals("0.0.0.0", tlsTcpListener.getBindAddress());

        assertEquals("password-keystore", tlsTcpListener.getTls().getKeystorePassword());
        assertEquals("/absolute/path.jks", tlsTcpListener.getTls().getKeystorePath());
        assertEquals("password-key", tlsTcpListener.getTls().getPrivateKeyPassword());
        assertEquals(1, tlsTcpListener.getTls().getProtocols().size());
        assertEquals(1, tlsTcpListener.getTls().getCipherSuites().size());
        assertEquals(Tls.ClientAuthMode.NONE, tlsTcpListener.getTls().getClientAuthMode());

        //Check if the relative path was made absolute
        assertEquals(true, new File(tlsTcpListener.getTls().getTruststorePath()).isAbsolute());
        assertEquals("password-truststore", tlsTcpListener.getTls().getTruststorePassword());
        assertEquals("my-tls-tcp-listener", tlsTcpListener.getName());

    }

    @Test
    public void test_read_tls_listener_without_trust_store() throws Exception {

        final String contents = "" +
                "<hivemq>" +
                "    <listeners>" +
                "        <tls-tcp-listener>" +
                "            <port>8883</port>" +
                "            <bind-address>0.0.0.0</bind-address>" +
                "            <tls>" +
                "                <protocols>" +
                "                    <protocol>TLSv1.2</protocol>" +
                "                </protocols>" +
                "                <keystore>" +
                "                    <path>/absolute/path.jks</path>" +
                "                    <password>password-keystore</password>" +
                "                    <private-key-password>password-key</private-key-password>" +
                "                </keystore>" +
                "                <native-ssl>false</native-ssl>" +
                "            </tls>" +
                "        </tls-tcp-listener>" +
                "    </listeners>" +
                "</hivemq>";

        Files.write(contents.getBytes(UTF_8), xmlFile);

        reader.applyConfig();

        final TlsTcpListener tlsTcpListener = listenerConfigurationService.getTlsTcpListeners().get(0);

        assertEquals(8883, tlsTcpListener.getPort());
        assertEquals("0.0.0.0", tlsTcpListener.getBindAddress());

        assertEquals("password-keystore", tlsTcpListener.getTls().getKeystorePassword());
        assertEquals("/absolute/path.jks", tlsTcpListener.getTls().getKeystorePath());
        assertEquals("password-key", tlsTcpListener.getTls().getPrivateKeyPassword());

        //Check if the relative path was made absolute
        assertNull(tlsTcpListener.getTls().getTruststorePath());
    }

    @Test
    public void test_read_multiple_tcp_listeners() throws Exception {

        final String contents = "" +
                "<hivemq>" +
                "    <listeners>" +
                "       <tcp-listener>" +
                "           <port>1883</port>" +
                "           <bind-address>0.0.0.0</bind-address>" +
                "            <name>my-tcp-listener</name>" +
                "       </tcp-listener>" +
                "       <tcp-listener>" +
                "           <port>1884</port>" +
                "           <bind-address>0.0.0.0</bind-address>" +
                "            <name>my-tcp-listener</name>" +
                "       </tcp-listener>" +
                "       <tcp-listener>" +
                "           <port>1885</port>" +
                "           <bind-address>0.0.0.0</bind-address>" +
                "            <name>my-tcp-listener</name>" +
                "       </tcp-listener>" +
                "    </listeners>" +
                "</hivemq>";

        Files.write(contents.getBytes(UTF_8), xmlFile);

        reader.applyConfig();

        final TcpListener tcpListener1 = listenerConfigurationService.getTcpListeners().get(0);
        final TcpListener tcpListener2 = listenerConfigurationService.getTcpListeners().get(1);
        final TcpListener tcpListener3 = listenerConfigurationService.getTcpListeners().get(2);

        assertEquals("my-tcp-listener", tcpListener1.getName());
        assertEquals("my-tcp-listener-1", tcpListener2.getName());
        assertEquals("my-tcp-listener-2", tcpListener3.getName());
    }

    @Test
    public void test_read_tcp_listener_white_space_name() throws Exception {

        final String contents = "" +
                "<hivemq>" +
                "    <listeners>" +
                "       <tcp-listener>" +
                "           <port>1883</port>" +
                "           <bind-address>0.0.0.0</bind-address>" +
                "           <name>     </name>" +
                "       </tcp-listener>" +
                "    </listeners>" +
                "</hivemq>";

        Files.write(contents.getBytes(UTF_8), xmlFile);

        reader.applyConfig();

        final TcpListener tcpListener = listenerConfigurationService.getTcpListeners().get(0);

        assertEquals(1883, tcpListener.getPort());
        assertEquals("0.0.0.0", tcpListener.getBindAddress());
        assertEquals("tcp-listener-1883", tcpListener.getName());
    }

    @Test
    public void test_read_tcp_listener() throws Exception {

        final String contents = "" +
                "<hivemq>" +
                "    <listeners>" +
                "       <tcp-listener>" +
                "           <port>1883</port>" +
                "           <bind-address>0.0.0.0</bind-address>" +
                "       </tcp-listener>" +
                "    </listeners>" +
                "</hivemq>";

        Files.write(contents.getBytes(UTF_8), xmlFile);

        reader.applyConfig();

        final TcpListener tcpListener = listenerConfigurationService.getTcpListeners().get(0);

        assertEquals(1883, tcpListener.getPort());
        assertEquals("0.0.0.0", tcpListener.getBindAddress());
    }

    @Test
    public void test_read_websocket_listener() throws Exception {

        final String contents = "" +
                "<hivemq>" +
                "    <listeners>" +
                "        <websocket-listener>\n" +
                "            <port>8000</port>\n" +
                "            <bind-address>0.0.0.0</bind-address>\n" +
                "            <path>/mqtt</path>\n" +
                "            <subprotocols>\n" +
                "                <subprotocol>mqttv3.1</subprotocol>\n" +
                "            </subprotocols>\n" +
                "            <allow-extensions>false</allow-extensions>\n" +
                "        </websocket-listener>" +
                "    </listeners>" +
                "</hivemq>";

        Files.write(contents.getBytes(UTF_8), xmlFile);

        reader.applyConfig();

        final WebsocketListener websocketListener = listenerConfigurationService.getWebsocketListeners().get(0);
        assertEquals(8000, websocketListener.getPort());
        assertEquals("0.0.0.0", websocketListener.getBindAddress());
        assertEquals("/mqtt", websocketListener.getPath());
        assertEquals("mqttv3.1", websocketListener.getSubprotocols().get(0));
        assertEquals(false, websocketListener.getAllowExtensions());
    }

    @Test
    public void test_read_tls_websocket_listener() throws Exception {

        final String contents = "" +
                "<hivemq>" +
                "    <listeners>" +
                "       <tls-websocket-listener>" +
                "           <port>8000</port>" +
                "           <bind-address>0.0.0.0</bind-address>" +
                "           <path>/mqtt</path>" +
                "           <subprotocols>" +
                "               <subprotocol>mqttv3.1</subprotocol>" +
                "           </subprotocols>" +
                "           <allow-extensions>false</allow-extensions>" +
                "           <tls>" +
                "              <keystore>" +
                "                  <path>/path/to/the/key/store.jks</path>" +
                "                  <password>password-keystore</password>" +
                "                  <private-key-password>password-key</private-key-password>" +
                "              </keystore>" +
                "              <truststore>" +
                "                  <path>/path/to/the/trust/store.jks</path>" +
                "                  <password>password-truststore</password>" +
                "              </truststore>" +
                "              <client-authentication-mode>NONE</client-authentication-mode>" +
                "           </tls>" +
                "       </tls-websocket-listener>" +
                "    </listeners>" +
                "</hivemq>";

        Files.write(contents.getBytes(UTF_8), xmlFile);

        reader.applyConfig();

        final TlsWebsocketListener websocketListener = listenerConfigurationService.getTlsWebsocketListeners().get(0);
        assertEquals(8000, websocketListener.getPort());
        assertEquals("0.0.0.0", websocketListener.getBindAddress());
        assertEquals("/mqtt", websocketListener.getPath());
        assertEquals("mqttv3.1", websocketListener.getSubprotocols().get(0));
        assertEquals(false, websocketListener.getAllowExtensions());

        assertEquals("/path/to/the/key/store.jks", websocketListener.getTls().getKeystorePath());
        assertEquals("password-keystore", websocketListener.getTls().getKeystorePassword());
        assertEquals("password-key", websocketListener.getTls().getPrivateKeyPassword());

        assertEquals("/path/to/the/trust/store.jks", websocketListener.getTls().getTruststorePath());
        assertEquals("password-truststore", websocketListener.getTls().getTruststorePassword());

    }

}