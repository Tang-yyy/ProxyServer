package cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CacheManager {

    private final Map<String, CacheEntry> cacheMap =
            new ConcurrentHashMap<>();

    public CacheEntry get(String url){

        CacheEntry entry = cacheMap.get(url);

        if(entry == null){
            return null;
        }

        if(entry.isExpired()){
            cacheMap.remove(url);
            return null;
        }

        return entry;
    }

    public void put(String url, CacheEntry entry){

        cacheMap.put(url, entry);
    }

    public int size(){
        return cacheMap.size();
    }
}
