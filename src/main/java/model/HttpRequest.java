package model;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 解析后的 HTTP 请求模型
 */
public class HttpRequest {

    private String method;
    private String url;
    private String version;
    private String rawHeaders;
    private final Map<String, String> headers = new HashMap<>();
    private byte[] body = new byte[0];

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getRawHeaders() {
        return rawHeaders;
    }

    public void setRawHeaders(String rawHeaders) {
        this.rawHeaders = rawHeaders;
    }

    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    public void addHeader(String line) {
        int idx = line.indexOf(':');
        if (idx > 0) {
            String name = line.substring(0, idx).trim().toLowerCase();
            String value = line.substring(idx + 1).trim();
            headers.put(name, value);
        }
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body != null ? body : new byte[0];
    }

    public int getContentLength() {
        String cl = headers.get("content-length");
        if (cl == null) {
            return 0;
        }
        try {
            return Integer.parseInt(cl.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** 用于转发给目标服务器的完整请求字节（请求头 + 请求体） */
    public byte[] toForwardBytes() {
        byte[] headerBytes = rawHeaders.getBytes(StandardCharsets.ISO_8859_1);
        if (body.length == 0) {
            return headerBytes;
        }
        byte[] combined = new byte[headerBytes.length + body.length];
        System.arraycopy(headerBytes, 0, combined, 0, headerBytes.length);
        System.arraycopy(body, 0, combined, headerBytes.length, body.length);
        return combined;
    }
}
