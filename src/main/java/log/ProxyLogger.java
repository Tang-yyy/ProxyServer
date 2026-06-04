package log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

public class ProxyLogger {

    private static final Path LOG_FILE = Paths.get("proxy.log");
    private static PrintWriter fileWriter;

    static {
        try {
            fileWriter = new PrintWriter(
                    new OutputStreamWriter(new FileOutputStream(LOG_FILE.toFile(), true), StandardCharsets.UTF_8),
                    true);
            System.out.println("[Logger] 日志文件：" + LOG_FILE.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("[Logger] 无法打开日志文件：" + e.getMessage());
        }
    }

    public static void log(String method, String url, int status, boolean cacheHit) {
        String msg = String.format("[%s] %s %s 状态:%d 缓存:%s",
                LocalDateTime.now(), method, url, status, cacheHit ? "HIT" : "MISS");
        System.out.println(msg);
        if (fileWriter != null) {
            fileWriter.println(msg);
        }
    }
}
