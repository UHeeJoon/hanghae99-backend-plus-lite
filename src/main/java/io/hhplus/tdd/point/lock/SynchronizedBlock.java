package io.hhplus.tdd.point.lock;

/**
 * 동기적인 실행을 위한 인터페이스
 *
 * @author :Uheejoon
 * @date :2025-05-22 오전 12:36
 */
@FunctionalInterface
public interface SynchronizedBlock<T> {
    T execute();
}
