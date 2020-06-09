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

import java.io.IOException;
import java.net.ServerSocket;

/**
 * @author Georg Held
 */
public class RandomPortGenerator {

    public static int get() {
        int tries = 10000;
        while (tries > 0) {
            tries--;
            try {
                final int randomNumber = (int) (Math.round(Math.random() * 40000) + 10000);
                final ServerSocket serverSocket = new ServerSocket(randomNumber);
                final int port = serverSocket.getLocalPort();
                serverSocket.close();
                return port;
            } catch (final IOException ex) {
                //continue
            }
        }
        throw new RuntimeException("Random port not found");
    }
}
