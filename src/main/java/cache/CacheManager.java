package cache;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CacheManager {

    private static final int MAX_ENTRIES = 500;

    private final Map<String, CacheEntry> cacheMap = new ConcurrentHashMap<>();

    public CacheEntry get(String url) {
        CacheEntry entry = cacheMap.get(url);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired()) {
            cacheMap.remove(url);
            return null;
        }
        return entry;
    }

    public void put(String url, CacheEntry entry) {
        evictIfNeeded();
        cacheMap.put(url, entry);
    }

    private void evictIfNeeded() {
        if (cacheMap.size() < MAX_ENTRIES) {
            return;
        }
        // 优先淘汰过期条目
        for (Iterator<Map.Entry<String, CacheEntry>> it = cacheMap.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, CacheEntry> e = it.next();
            if (e.getValue().isExpired()) {
                it.remove();
            }
        }
        if (cacheMap.size() < MAX_ENTRIES) {
            return;
        }
        // 仍满则移除任意一条（ConcurrentHashMap 迭代器支持 remove）
        Iterator<String> keys = cacheMap.keySet().iterator();
        if (keys.hasNext()) {
            keys.next();
            keys.remove();
        }
    }

    public int size() {
        return cacheMap.size();
    }
}
