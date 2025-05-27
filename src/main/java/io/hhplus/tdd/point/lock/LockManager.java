package io.hhplus.tdd.point.lock;

/**
 * Lock을 획득하기 위한 인터페이스
 * key를 통해 lock을 획득하고, lock을 해제하는 기능을 제공한다.
 *
 * @author :Uheejoon
 * @date :2025-05-22 오전 12:29
 */
@FunctionalInterface
public interface LockManager<K> {
    Object getLock(K key);
}
