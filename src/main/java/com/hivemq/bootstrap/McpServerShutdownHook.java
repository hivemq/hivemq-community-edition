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

import com.hivemq.common.shutdown.HiveMQShutdownHook;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shutdown hook for the MCP server.
 *
 * @author HiveMQ
 */
public class McpServerShutdownHook implements HiveMQShutdownHook {

    private static final Logger log = LoggerFactory.getLogger(McpServerShutdownHook.class);

    private final @NotNull UndertowJaxrsServer server;

    public McpServerShutdownHook(final @NotNull UndertowJaxrsServer server) {
        this.server = server;
    }

    @Override
    public @NotNull String name() {
        return "MCP Server Shutdown";
    }

    @Override
    public @NotNull Priority priority() {
        return Priority.LOW;
    }

    @Override
    public void run() {
        log.debug("Shutting down MCP server");
        try {
            server.stop();
            log.info("MCP server stopped successfully");
        } catch (final Exception e) {
            log.error("Failed to stop MCP server", e);
        }
    }
}
