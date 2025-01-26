package com.poissonnerie.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.logging.Logger;
import java.util.logging.Level;

public class CacheManager {
    private static final Logger LOGGER = Logger.getLogger(CacheManager.class.getName());
    private static final int DEFAULT_MAX_SIZE = 1000;
    private static final long DEFAULT_EXPIRATION_TIME = 5; // minutes
    private static CacheManager instance;
    private final Map<String, Cache<?>> caches;
    private final ScheduledExecutorService cleanupExecutor;

    private CacheManager() {
        this.caches = new ConcurrentHashMap<>();
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "CacheCleanupThread");
            thread.setDaemon(true);
            return thread;
        });
        
        // Programme le nettoyage périodique du cache
        cleanupExecutor.scheduleAtFixedRate(
            this::cleanupExpiredEntries,
            1,
            1,
            TimeUnit.MINUTES
        );
    }

    public static synchronized CacheManager getInstance() {
        if (instance == null) {
            instance = new CacheManager();
        }
        return instance;
    }

    public <T> Cache<T> getCache(String name) {
        return getCache(name, DEFAULT_MAX_SIZE, DEFAULT_EXPIRATION_TIME);
    }

    @SuppressWarnings("unchecked")
    public <T> Cache<T> getCache(String name, int maxSize, long expirationMinutes) {
        return (Cache<T>) caches.computeIfAbsent(name, 
            k -> new Cache<>(maxSize, expirationMinutes));
    }

    private void cleanupExpiredEntries() {
        try {
            caches.values().forEach(Cache::removeExpiredEntries);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Erreur lors du nettoyage du cache", e);
        }
    }

    public void shutdown() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static class Cache<T> {
        private final Map<String, CacheEntry<T>> cache;
        private final long expirationTime;

        private Cache(int maxSize, long expirationMinutes) {
            this.cache = Collections.synchronizedMap(
                new LinkedHashMap<String, CacheEntry<T>>(maxSize + 1, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<String, CacheEntry<T>> eldest) {
                        return size() > maxSize;
                    }
                }
            );
            this.expirationTime = TimeUnit.MINUTES.toMillis(expirationMinutes);
        }

        public void put(String key, T value) {
            if (key == null) {
                throw new IllegalArgumentException("La clé ne peut pas être null");
            }
            cache.put(key, new CacheEntry<>(value));
        }

        public T get(String key) {
            if (key == null) {
                return null;
            }
            CacheEntry<T> entry = cache.get(key);
            if (entry != null && !entry.isExpired()) {
                entry.updateAccessTime();
                return entry.getValue();
            } else if (entry != null) {
                cache.remove(key);
            }
            return null;
        }

        public void remove(String key) {
            cache.remove(key);
        }

        public void clear() {
            cache.clear();
        }

        private void removeExpiredEntries() {
            cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        }

        private class CacheEntry<V> {
            private final V value;
            private long lastAccessTime;

            CacheEntry(V value) {
                this.value = value;
                this.lastAccessTime = System.currentTimeMillis();
            }

            V getValue() {
                return value;
            }

            void updateAccessTime() {
                this.lastAccessTime = System.currentTimeMillis();
            }

            boolean isExpired() {
                return System.currentTimeMillis() - lastAccessTime > expirationTime;
            }
        }
    }
}
