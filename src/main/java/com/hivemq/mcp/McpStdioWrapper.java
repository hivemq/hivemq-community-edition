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
package com.hivemq.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

/**
 * STDIO wrapper for the HiveMQ MCP server.
 * This allows Claude Desktop and MCP Inspector to connect to the HTTP-based MCP server via stdio protocol.
 *
 * Reads JSON-RPC messages from stdin, forwards them to the HTTP MCP server,
 * and writes responses back to stdout.
 *
 * @author HiveMQ
 */
public class McpStdioWrapper {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String MCP_SERVER_URL = "https://localhost:8888";

    private static @Nullable String accessToken;
    private static @Nullable String clientId;
    private static @Nullable String clientSecret;

    public static void main(String[] args) {
        // Disable SSL certificate verification for self-signed certificates
        disableSslVerification();

        // Log file for debugging
        try (java.io.PrintWriter log = new java.io.PrintWriter(new java.io.FileWriter("/tmp/mcp-wrapper.log", true))) {
            log.println("=== MCP STDIO Wrapper started ===");
            log.flush();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    JsonNode requestId = null;
                    try {
                        log.println("RECV: " + line);
                        log.flush();

                        // Parse JSON-RPC request from stdin
                        final JsonNode request = objectMapper.readTree(line);
                        final String method = request.path("method").asText();
                        requestId = request.path("id");

                        log.println("METHOD: " + method + " ID: " + requestId);
                        log.flush();

                        // Skip if method is empty
                        if (method.isEmpty()) {
                            log.println("Empty method, skipping");
                            log.flush();
                            continue;
                        }

                        // If this is a notification (no id), just skip it - no response needed
                        if (requestId.isMissingNode() || requestId.isNull()) {
                            log.println("Notification (no id), skipping response");
                            log.flush();
                            continue;
                        }

                        // Handle the request
                        final JsonNode response = handleRequest(method, request.path("params"), requestId);

                        // Write response to stdout
                        final String responseStr = objectMapper.writeValueAsString(response);
                        log.println("SEND: " + responseStr);
                        log.flush();
                        System.out.println(responseStr);
                        System.out.flush();

                    } catch (Exception e) {
                        log.println("ERROR: " + e.getMessage());
                        e.printStackTrace(log);
                        log.flush();

                        // Only write error if we have a valid request ID
                        if (requestId != null && !requestId.isNull() && (requestId.isNumber() || requestId.isTextual())) {
                            writeError(requestId, -32603, "Internal error: " + e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                log.println("IO ERROR: " + e.getMessage());
                e.printStackTrace(log);
                log.flush();
            }
        } catch (Exception e) {
            // Can't even log
        }
    }

    private static @NotNull JsonNode handleRequest(
            final @NotNull String method,
            final @NotNull JsonNode params,
            final @NotNull JsonNode id) throws Exception {

        switch (method) {
            case "initialize":
                return handleInitialize(params, id);
            case "resources/list":
                return handleResourcesList(id);
            case "resources/read":
                return handleResourcesRead(params, id);
            case "tools/list":
                return handleToolsList(id);
            case "tools/call":
                return handleToolsCall(params, id);
            case "prompts/list":
                return handlePromptsList(id);
            default:
                return createErrorResponse(id, -32601, "Method not found: " + method);
        }
    }

    private static @NotNull JsonNode handleInitialize(
            final @NotNull JsonNode params,
            final @NotNull JsonNode id) throws Exception {

        // Register OAuth client if not already registered
        if (clientId == null) {
            registerOAuthClient();
        }

        // Get access token
        if (accessToken == null) {
            getAccessToken();
        }

        // Return initialize response
        final ObjectNode result = objectMapper.createObjectNode();
        result.put("protocolVersion", "2024-11-05");

        final ObjectNode serverInfo = objectMapper.createObjectNode();
        serverInfo.put("name", "hivemq-mcp");
        serverInfo.put("version", "1.0.0");
        result.set("serverInfo", serverInfo);

        final ObjectNode capabilities = objectMapper.createObjectNode();

        // Advertise resources capability with subscribe support
        final ObjectNode resourcesCapability = objectMapper.createObjectNode();
        resourcesCapability.put("subscribe", false);
        resourcesCapability.put("listChanged", false);
        capabilities.set("resources", resourcesCapability);

        // Advertise tools capability
        final ObjectNode toolsCapability = objectMapper.createObjectNode();
        toolsCapability.put("listChanged", false);
        capabilities.set("tools", toolsCapability);

        result.set("capabilities", capabilities);

        return createSuccessResponse(id, result);
    }

    private static @NotNull JsonNode handleResourcesList(final @NotNull JsonNode id) throws Exception {
        ensureAuthenticated();

        // Call HTTP MCP server
        final JsonNode httpResponse = httpGet(MCP_SERVER_URL + "/resources/list");

        return createSuccessResponse(id, httpResponse);
    }

    private static @NotNull JsonNode handleResourcesRead(
            final @NotNull JsonNode params,
            final @NotNull JsonNode id) throws Exception {
        ensureAuthenticated();

        final String uri = params.path("uri").asText();

        // Call HTTP MCP server
        final JsonNode httpResponse = httpGet(MCP_SERVER_URL + "/resources/read?uri=" + uri);

        return createSuccessResponse(id, httpResponse);
    }

    private static @NotNull JsonNode handleToolsList(final @NotNull JsonNode id) {
        // Return tools list with connection count tool
        final ObjectNode result = objectMapper.createObjectNode();
        final ArrayNode tools = objectMapper.createArrayNode();

        // Add get_connection_count tool
        final ObjectNode tool = objectMapper.createObjectNode();
        tool.put("name", "get_mqtt_connection_count");
        tool.put("description", "Get the current number of active MQTT client connections in HiveMQ");

        final ObjectNode inputSchema = objectMapper.createObjectNode();
        inputSchema.put("type", "object");
        inputSchema.set("properties", objectMapper.createObjectNode());
        inputSchema.set("required", objectMapper.createArrayNode());

        tool.set("inputSchema", inputSchema);
        tools.add(tool);

        result.set("tools", tools);
        return createSuccessResponse(id, result);
    }

    private static @NotNull JsonNode handleToolsCall(
            final @NotNull JsonNode params,
            final @NotNull JsonNode id) throws Exception {
        final String toolName = params.path("name").asText();

        if ("get_mqtt_connection_count".equals(toolName)) {
            ensureAuthenticated();

            // Call HTTP MCP server to get connection count
            final JsonNode httpResponse = httpGet(MCP_SERVER_URL + "/resources/read?uri=hivemq://connections/count");
            final String count = httpResponse.path("contents").get(0).path("text").asText();

            final ObjectNode result = objectMapper.createObjectNode();
            final ArrayNode content = objectMapper.createArrayNode();

            final ObjectNode textContent = objectMapper.createObjectNode();
            textContent.put("type", "text");
            textContent.put("text", "Current MQTT connection count: " + count);
            content.add(textContent);

            result.set("content", content);
            return createSuccessResponse(id, result);
        }

        return createErrorResponse(id, -32602, "Unknown tool: " + toolName);
    }

    private static @NotNull JsonNode handlePromptsList(final @NotNull JsonNode id) {
        // Return empty prompts list
        final ObjectNode result = objectMapper.createObjectNode();
        result.set("prompts", objectMapper.createArrayNode());
        return createSuccessResponse(id, result);
    }

    private static void registerOAuthClient() throws Exception {
        try {
            final Map<String, Object> registrationRequest = new HashMap<>();
            registrationRequest.put("client_name", "Claude Desktop MCP STDIO Wrapper");
            registrationRequest.put("redirect_uris", new String[]{"http://localhost:9999/callback"});

            final JsonNode response = httpPost(MCP_SERVER_URL + "/oauth/register", registrationRequest, null);

            clientId = response.path("client_id").asText();
            clientSecret = response.path("client_secret").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to register OAuth client. Is HiveMQ running on port 8888? Error: " + e.getMessage(), e);
        }
    }

    private static void getAccessToken() throws Exception {
        // Get authorization code
        final String authCode = getAuthorizationCode();

        // Exchange code for token
        final Map<String, String> tokenRequest = new HashMap<>();
        tokenRequest.put("grant_type", "authorization_code");
        tokenRequest.put("code", authCode);
        tokenRequest.put("client_id", clientId);
        tokenRequest.put("client_secret", clientSecret);
        tokenRequest.put("redirect_uri", "http://localhost:9999/callback");

        final JsonNode response = httpPostForm(MCP_SERVER_URL + "/oauth/token", tokenRequest, null);

        accessToken = response.path("access_token").asText();
    }

    private static @NotNull String getAuthorizationCode() throws Exception {
        final String authUrl = MCP_SERVER_URL + "/oauth/authorize" +
                "?client_id=" + clientId +
                "&redirect_uri=http://localhost:9999/callback" +
                "&response_type=code";

        final URL url = new URL(authUrl);
        final HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setInstanceFollowRedirects(false);

        final int responseCode = conn.getResponseCode();
        if (responseCode == 302 || responseCode == 303) {
            final String location = conn.getHeaderField("Location");
            // Extract code from redirect URL
            final String[] parts = location.split("code=");
            if (parts.length > 1) {
                return parts[1].split("&")[0];
            }
        }

        throw new RuntimeException("Failed to get authorization code");
    }

    private static void ensureAuthenticated() throws Exception {
        if (accessToken == null) {
            if (clientId == null) {
                registerOAuthClient();
            }
            getAccessToken();
        }
    }

    private static @NotNull JsonNode httpGet(final @NotNull String urlString) throws Exception {
        final URL url = new URL(urlString);
        final HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        if (accessToken != null) {
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        }

        return readResponse(conn);
    }

    private static @NotNull JsonNode httpPost(
            final @NotNull String urlString,
            final @NotNull Map<String, Object> body,
            final @Nullable String token) throws Exception {

        final URL url = new URL(urlString);
        final HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        if (token != null) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(objectMapper.writeValueAsBytes(body));
            os.flush();
        }

        return readResponse(conn);
    }

    private static @NotNull JsonNode httpPostForm(
            final @NotNull String urlString,
            final @NotNull Map<String, String> formData,
            final @Nullable String token) throws Exception {

        final URL url = new URL(urlString);
        final HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        if (token != null) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }
        conn.setDoOutput(true);

        // Build form data
        final StringBuilder formBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : formData.entrySet()) {
            if (formBuilder.length() > 0) {
                formBuilder.append("&");
            }
            formBuilder.append(entry.getKey()).append("=").append(entry.getValue());
        }

        try (OutputStream os = conn.getOutputStream()) {
            os.write(formBuilder.toString().getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        return readResponse(conn);
    }

    private static @NotNull JsonNode readResponse(final @NotNull HttpURLConnection conn) throws Exception {
        final int responseCode = conn.getResponseCode();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream(),
                        StandardCharsets.UTF_8))) {

            final StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }

            return objectMapper.readTree(response.toString());
        }
    }

    private static @NotNull JsonNode createSuccessResponse(
            final @NotNull JsonNode id,
            final @NotNull JsonNode result) {

        final ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.set("id", id);
        response.set("result", result);
        return response;
    }

    private static @NotNull JsonNode createErrorResponse(
            final @NotNull JsonNode id,
            final int code,
            final @NotNull String message) {

        final ObjectNode error = objectMapper.createObjectNode();
        error.put("code", code);
        error.put("message", message);

        final ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.set("id", id);
        response.set("error", error);
        return response;
    }

    private static void writeError(
            final @NotNull JsonNode id,
            final int code,
            final @NotNull String message) {
        try {
            final JsonNode errorResponse = createErrorResponse(id, code, message);
            System.out.println(objectMapper.writeValueAsString(errorResponse));
            System.out.flush();
        } catch (Exception e) {
            // Silent fail
        }
    }

    private static void disableSslVerification() {
        try {
            // Create a trust manager that accepts all certificates
            final TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
            };

            // Install the all-trusting trust manager
            final SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            // Silent fail
        }
    }
}
