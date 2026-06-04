package filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AccessController {

    private final Set<String> blacklist = new HashSet<>();
    private final Path blacklistFile;
    private long lastModified = 0;

    public AccessController() {
        this(Paths.get("blacklist.txt"));
    }

    public AccessController(Path blacklistFile) {
        this.blacklistFile = blacklistFile;
        reloadIfChanged();
    }

    private void reloadIfChanged() {
        try {
            if (!Files.exists(blacklistFile)) {
                Files.write(blacklistFile, List.of("# 黑名单域名，一行一个", "www.blocked.com"),
                        StandardCharsets.UTF_8);
            }
            long mod = Files.getLastModifiedTime(blacklistFile).toMillis();
            if (mod == lastModified) {
                return;
            }
            blacklist.clear();
            for (String line : Files.readAllLines(blacklistFile, StandardCharsets.UTF_8)) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                blacklist.add(line);
            }
            lastModified = mod;
            System.out.println("[AccessController] 已加载黑名单 " + blacklist.size() + " 条");
        } catch (IOException e) {
            System.err.println("[AccessController] 加载黑名单失败：" + e.getMessage());
        }
    }

    public boolean isAllowed(String host) {
        reloadIfChanged();
        return !blacklist.contains(host);
    }
}
