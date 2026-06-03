package cache;

/**
 * 一条缓存记录，包含：
 *   - 响应的原始字节
 *   - 存入时间
 *   - 过期时间（默认 5 分钟）
 */
public class CacheEntry {

    // 缓存默认有效期：5分钟（毫秒）
    private static final long DEFAULT_TTL_MS = 5 * 60 * 1000L;

    private final byte[] responseBytes;  // HTTP 响应的完整字节（包括响应头和响应体）
    private final long   createdAt;      // 存入缓存的时间戳
    private final long   ttlMs;          // 有效期（毫秒）

    public CacheEntry(byte[] responseBytes) {
        this(responseBytes, DEFAULT_TTL_MS);
    }

    public CacheEntry(byte[] responseBytes, long ttlMs) {
        this.responseBytes = responseBytes;
        this.createdAt     = System.currentTimeMillis();
        this.ttlMs         = ttlMs;
    }

    /** 判断缓存是否已过期 */
    public boolean isExpired() {
        return System.currentTimeMillis() - createdAt > ttlMs;
    }

    public byte[] getResponseBytes() {
        return responseBytes;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    /** 缓存大小（字节） */
    public int getSizeBytes() {
        return responseBytes.length;
    }
}
