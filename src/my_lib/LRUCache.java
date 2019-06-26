package my_lib;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author Texhnolyze
 * @param <K> key 
 * @param <V extends LRUCache.Sizeable> value (heavy object to be cached). Must implement LRUCache.Sizeable
 */
public interface LRUCache<K, V extends LRUCache.Sizeable> {
    
    public interface Sizeable {long getSizeBytes();}
    public interface ByKeySizeableLoader<K, V extends Sizeable> {V load(K key);}
    
    long getSizeBytes();
    long getMaxCapacityBytes();
    void setMaxCapacity(long capacityBytes);
    
    ByKeySizeableLoader<K, V> getByKeySizeableLoader();
    void setByKeySizeableLoader(ByKeySizeableLoader<K, V> loader);
    
    V getFromCacheOrLoad(K key);
    Iterable<Map.Entry<K, V>> fromLeastToMostRecentlyUsed();
    
    public static <K, V extends Sizeable> LRUCache<K, V> defaultImpl(long capacityBytes, ByKeySizeableLoader<K, V> loader) {
        return new LRUCacheImpl<>(capacityBytes, loader);
    }
    
    static class LRUCacheImpl<K, V extends Sizeable> implements LRUCache<K, V> {

        private long sizeBytes;
        private long capacityBytes;
        private ByKeySizeableLoader<K, V> loader;
        private final LinkedHashMap<K, V> map = new LinkedHashMap<>();
        
        LRUCacheImpl(long capacityBytes, ByKeySizeableLoader<K, V> loader) {
            this.capacityBytes = capacityBytes;
            this.loader = loader;
        }
        
        @Override public long getSizeBytes() {return sizeBytes;}
        @Override public long getMaxCapacityBytes() {return capacityBytes;}

        @Override
        public void setMaxCapacity(long capacityBytes) {
            if (capacityBytes <= 0)
                throw new IllegalArgumentException("Capacity must be greater than 0.");
            this.capacityBytes = capacityBytes;
            Iterator<Entry<K, V>> it = map.entrySet().iterator();
            while (sizeBytes > capacityBytes) {
                V v = it.next().getValue();
                sizeBytes -= v.getSizeBytes();
                it.remove();
            }
        }
        
        @Override public ByKeySizeableLoader<K, V> getByKeySizeableLoader() {return loader;}
        @Override public void setByKeySizeableLoader(ByKeySizeableLoader<K, V> loader) {this.loader = loader;}

        @Override
        public V getFromCacheOrLoad(K key) {
            V val = map.get(key);
            if (val != null) {
                map.remove(key);
                map.put(key, val);
            } else {
                val = loader.load(key);
                if (val == null)
                    return null;
                long size = val.getSizeBytes();
                if (capacityBytes < size)
                    throw new RuntimeException("Insufficient cache space.");
                Iterator<Entry<K, V>> it = map.entrySet().iterator();
                while (sizeBytes + size > capacityBytes) {
                    V v = it.next().getValue();
                    sizeBytes -= v.getSizeBytes();
                    it.remove();
                }
                sizeBytes += val.getSizeBytes();
                map.put(key, val);
            }                
            return val;
        }
        
        @Override
        public Iterable<Map.Entry<K, V>> fromLeastToMostRecentlyUsed() {
            return Collections.unmodifiableSet(map.entrySet());
        }
        
    }
    
}
