package log;

import java.time.LocalDateTime;

public class ProxyLogger {

    public static void log(
            String method,
            String url,
            int status,
            boolean cacheHit){

        System.out.printf(
                "[%s] %s %s 状态:%d 缓存:%s%n",
                LocalDateTime.now(),
                method,
                url,
                status,
                cacheHit ? "HIT" : "MISS"
        );
    }
}
