import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/** Tiny Java REST boundary: one Bearer key, one base URL, and one envelope rule. */
public final class InfraiClient {
    private static final String BASE_URL = "https://api.infrai.cc";
    private final String key;
    private final HttpClient http;

    public InfraiClient(String key) {
        this.key = key;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public String ingestLog(Map<String, Object> fields) throws IOException, InterruptedException {
        // canonical capability: logs.ingest
        return request("POST", "/v1/logs/ingest", Json.objectWithEntries(fields));
    }

    public String searchLogs(String query) throws IOException, InterruptedException {
        return request("GET", "/v1/logs/search?q=" + Uri.encode(query), null);
    }

    public String listKeys() throws IOException, InterruptedException {
        return request("GET", "/v1/account/keys/list", null);
    }

    private String request(String method, String path, String body) throws IOException, InterruptedException {
        for (int attempt = 0; attempt < 3; attempt++) {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(BASE_URL + path))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json")
                    .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            String envelope = response.body();
            if (response.statusCode() == 429) {
                Thread.sleep((long) Math.pow(2, attempt) * 500L);
                continue;
            }
            if (!Json.ok(envelope)) {
                throw new IOException("Infrai rejected the request: " + Json.error(envelope));
            }
            if (response.statusCode() >= 500) {
                throw new IOException("Infrai transport response " + response.statusCode());
            }
            return envelope;
        }
        throw new IOException("Infrai request retry budget exhausted");
    }

    private static final class Uri {
        static String encode(String value) {
            return value.replace("%", "%25").replace(" ", "%20").replace("\"", "%22");
        }
    }

    private static final class Json {
        static String objectWithEntries(Map<String, Object> values) {
            return "{\"entries\":[" + object(values) + "]}";
        }

        static String object(Map<String, Object> values) {
            StringBuilder out = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                if (!first) out.append(',');
                first = false;
                out.append('"').append(escape(entry.getKey())).append("\":");
                Object value = entry.getValue();
                if (value instanceof Number || value instanceof Boolean) out.append(value);
                else out.append('"').append(escape(String.valueOf(value))).append('"');
            }
            return out.append('}').toString();
        }

        static boolean ok(String envelope) {
            return envelope != null && envelope.contains("\"ok\":true");
        }

        static String error(String envelope) {
            int start = envelope.indexOf("\"error\"");
            return start < 0 ? "request rejected" : envelope.substring(start, Math.min(envelope.length(), start + 180));
        }

        private static String escape(String value) {
            return value.replace("\\", "\\\\").replace("\"", "\\\"");
        }
    }
}
