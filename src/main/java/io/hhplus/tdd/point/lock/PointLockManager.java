package io.hhplus.tdd.point.lock;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 키(K)별 고유한 락 객체를 관리하는 매니저
 * 사용되지 않는 락은 자동으로 GC되고 주기적으로 맵에서 제거됩니다.
 *
 * @param <K> 락을 식별하는 키의 타입
 */
public class PointLockManager<K> implements LockManager<K> {

    /**
     * 락 객체에 대한 약한 참조와 키를 함께 보관하는 내부 참조 객체
     */
    private static class LockRef<K> extends WeakReference<Object> {
        final K key;

        LockRef(Object lock, ReferenceQueue<Object> queue, K key) {
            super(lock, queue);
            this.key = key;
        }
    }

    // key -> LockRef 맵
    private final ConcurrentHashMap<K, LockRef<K>> lockMap = new ConcurrentHashMap<>();
    private final ReferenceQueue<Object> referenceQueue = new ReferenceQueue<>();

    // 자동 정리 스케줄러
    private final ScheduledExecutorService scheduler;

    /**
     * 기본 생성자: 60초마다 cleanup() 실행
     */
    public PointLockManager() {
        this(60, TimeUnit.SECONDS);
    }

    /**
     * 정리 주기를 지정하는 생성자
     *
     * @param period 정리 간격
     * @param unit   시간 단위
     */
    public PointLockManager(long period, TimeUnit unit) {
        scheduler = new ScheduledThreadPoolExecutor(1, r -> {
            Thread t = new Thread(r, "PointLockManager-Cleanup");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleWithFixedDelay(this::cleanup, period, period, unit);
    }

    /**
     * 주어진 키에 대응되는 락 객체를 반환합니다.
     * 동일 키로 반복 호출 시 동일 락 객체를 재사용하며,
     * 약 참조 대상이 GC되면 새로 생성됩니다.
     *
     * @param key 락 식별 키 (null 불가)
     * @return 락 객체 (절대 null 아님)
     */
    @Override
    public Object getLock(K key) {
        if (key == null) {
            throw new IllegalArgumentException("Lock key cannot be null");
        }

        // ReferenceQueue에서 회수된 참조를 즉시 정리
        cleanup();

        // Atomic하게 LockRef 생성 또는 재사용
        LockRef<K> ref = lockMap.compute(key, (k, oldRef) -> {
            Object existing = (oldRef != null) ? oldRef.get() : null;
            if (existing == null) {
                Object newLock = new Object();
                return new LockRef<>(newLock, referenceQueue, k);
            }
            return oldRef;
        });

        return ref.get();
    }

    /**
     * ReferenceQueue에서 회수된 LockRef를 꺼내 lockMap에서 제거
     *
     * @return 제거된 엔트리 개수
     */
    @SuppressWarnings("unchecked")
    public int cleanup() {
        int removed = 0;
        LockRef<K> ref;
        while ((ref = (LockRef<K>) referenceQueue.poll()) != null) {
            if (lockMap.remove(ref.key, ref)) {
                removed++;
            }
        }
        return removed;
    }

    /**
     * 스케줄러 종료 및 맵 정리
     */
    public void shutdown() {
        scheduler.shutdownNow();
        lockMap.clear();
    }
}
