package redis.storage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class KeyModificationTracker {
    private static final Map<String, AtomicLong> keyVersions = new ConcurrentHashMap<>();

    public static void notifyModified(String key) {
        keyVersions.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
    }

    public static long getVersion(String key) {
        AtomicLong version = keyVersions.get(key);
        return version != null ? version.get() : 0L;
    }
}
