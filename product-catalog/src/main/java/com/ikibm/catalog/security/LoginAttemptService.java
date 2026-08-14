package com.ikibm.catalog.security;

import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Basit bellek-içi login deneme sınırı (brute-force koruması). Tek örnek için
 * yeterli; identifier (e-posta/kullanıcı adı) başına pencere içindeki başarısız
 * deneme sayısını tutar. Eski Node loginRateLimiter karşılığı.
 */
@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 15 * 60 * 1000L; // 15 dk

    private record Attempt(int count, long windowStart) {}

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    private String key(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }

    public void recordFailure(String id) {
        if (id == null || id.isBlank()) return;
        long now = System.currentTimeMillis();
        attempts.compute(key(id), (k, a) -> {
            if (a == null || now - a.windowStart() > WINDOW_MS) return new Attempt(1, now);
            return new Attempt(a.count() + 1, a.windowStart());
        });
    }

    public void reset(String id) {
        if (id != null && !id.isBlank()) attempts.remove(key(id));
    }

    public boolean isBlocked(String id) {
        if (id == null || id.isBlank()) return false;
        Attempt a = attempts.get(key(id));
        if (a == null) return false;
        if (System.currentTimeMillis() - a.windowStart() > WINDOW_MS) {
            attempts.remove(key(id));
            return false;
        }
        return a.count() >= MAX_ATTEMPTS;
    }
}
