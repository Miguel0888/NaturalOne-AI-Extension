package de.bund.zrb.natural.ui.chat.internal.ollama;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Minimal Ollama HTTP client using only Java 8 standard libraries.
 *
 * Uses /api/chat with stream=false.
 */
public final class OllamaChatClient {

    private final OllamaConfigStore configStore;

    private volatile HttpURLConnection activeConnection;

    public OllamaChatClient(OllamaConfigStore configStore) {
        this.configStore = configStore;
    }

    public void cancelActiveRequest() {
        HttpURLConnection c = activeConnection;
        if (c != null) {
            try {
                c.disconnect();
            } catch (Exception ignore) {
                // ignore
            }
        }
    }

    public String chat(String systemPrompt, String userPrompt) throws Exception {
        String baseUrl = configStore.loadBaseUrl();
        String model = configStore.loadModel();

        String endpoint = baseUrl;
        if (endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }
        endpoint = endpoint + "/api/chat";

        String payload = buildPayload(model, systemPrompt, userPrompt);

        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        activeConnection = conn;

        conn.setRequestMethod("POST");
        conn.setConnectTimeout(2000);
        conn.setReadTimeout(60000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

        OutputStream os = null;
        try {
            os = conn.getOutputStream();
            os.write(payload.getBytes("UTF-8"));
            os.flush();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (Exception ignore) {
                    // ignore
                }
            }
        }

        int status = conn.getResponseCode();
        boolean ok = status >= 200 && status < 300;
        String body = readBody(conn, ok);

        if (!ok) {
            throw new IllegalStateException("Ollama HTTP " + status + ": " + body);
        }

        String content = extractAssistantContent(body);
        if (content == null) {
            throw new IllegalStateException("Ollama response did not contain message.content. Body: " + body);
        }
        return content;
    }

    private static String buildPayload(String model, String systemPrompt, String userPrompt) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"model\":\"").append(escapeJson(model)).append("\",");
        sb.append("\"stream\":false,");
        sb.append("\"messages\":[");
        if (systemPrompt != null && systemPrompt.trim().length() > 0) {
            sb.append("{\"role\":\"system\",\"content\":\"").append(escapeJson(systemPrompt)).append("\"},");
        }
        sb.append("{\"role\":\"user\",\"content\":\"").append(escapeJson(userPrompt)).append("\"}");
        sb.append("]}");
        return sb.toString();
    }

    private static String readBody(HttpURLConnection conn, boolean ok) throws Exception {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(ok ? conn.getInputStream() : conn.getErrorStream(), "UTF-8"));
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString().trim();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (Exception ignore) {
                    // ignore
                }
            }
        }
    }

    /**
     * Extract message.content from a typical /api/chat response:
     * { "message": { "role":"assistant", "content":"..." }, ... }
     */
    private static String extractAssistantContent(String json) {
        if (json == null) {
            return null;
        }
        int msgIdx = json.indexOf("\"message\"");
        if (msgIdx < 0) {
            return null;
        }
        int contentIdx = json.indexOf("\"content\"", msgIdx);
        if (contentIdx < 0) {
            return null;
        }
        int colon = json.indexOf(':', contentIdx);
        if (colon < 0) {
            return null;
        }
        int firstQuote = json.indexOf('"', colon + 1);
        if (firstQuote < 0) {
            return null;
        }
        return parseJsonString(json, firstQuote);
    }

    private static String parseJsonString(String json, int firstQuote) {
        StringBuilder out = new StringBuilder();
        int i = firstQuote + 1;
        while (i < json.length()) {
            char c = json.charAt(i++);
            if (c == '"') {
                return out.toString();
            }
            if (c != '\\') {
                out.append(c);
                continue;
            }
            if (i >= json.length()) {
                return out.toString();
            }
            char esc = json.charAt(i++);
            switch (esc) {
                case '"':
                    out.append('"');
                    break;
                case '\\':
                    out.append('\\');
                    break;
                case '/':
                    out.append('/');
                    break;
                case 'b':
                    out.append('\b');
                    break;
                case 'f':
                    out.append('\f');
                    break;
                case 'n':
                    out.append('\n');
                    break;
                case 'r':
                    out.append('\r');
                    break;
                case 't':
                    out.append('\t');
                    break;
                case 'u':
                    if (i + 3 < json.length()) {
                        String hex = json.substring(i, i + 4);
                        i += 4;
                        try {
                            out.append((char) Integer.parseInt(hex, 16));
                        } catch (NumberFormatException e) {
                            // ignore malformed unicode
                        }
                    }
                    break;
                default:
                    out.append(esc);
                    break;
            }
        }
        return out.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        String hex = Integer.toHexString((int) c);
                        sb.append("\\u");
                        for (int j = hex.length(); j < 4; j++) {
                            sb.append('0');
                        }
                        sb.append(hex);
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}

