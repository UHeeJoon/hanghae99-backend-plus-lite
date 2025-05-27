package io.hhplus.tdd.point.lock;

/**
 * 특정 키에 대한 동기화 블록을 실행하는 템플릿 클래스
 * PointLockManager를 활용하여 키별 락을 관리하고
 * 동기화된 블록 실행에 타임아웃 및 예외 처리 기능을 추가
 *
 * @param <K> 락 식별 키 타입
 *
 *
 * @author :Uheejoon
 * @date :2025-05-22 오전 12:28
 */
public class SynchronizedLockTemplate<K> {

    private final LockManager<K> lockManager;
    private final long defaultTimeoutMillis;

    // 락 획득 실패 시 재시도 정책
    private final int maxRetries;
    private final long retryDelayMillis;

    /**
     * 기본 생성자: PointLockManager를 사용하여 5초 타임아웃 설정
     */
    public SynchronizedLockTemplate() {
        this(new PointLockManager<>(), 5000L);
    }

    /**
     * 타임아웃을 지정할 수 있는 생성자
     *
     * @param timeoutMillis 기본 타임아웃 (밀리초)
     */
    public SynchronizedLockTemplate(long timeoutMillis) {
        this(new PointLockManager<>(), timeoutMillis);
    }

    /**
     * 락 매니저와 타임아웃을 지정할 수 있는 생성자
     *
     * @param lockManager 락 매니저 구현체
     * @param defaultTimeoutMillis 기본 타임아웃 (밀리초)
     */
    public SynchronizedLockTemplate(LockManager<K> lockManager, long defaultTimeoutMillis) {
        this(lockManager, defaultTimeoutMillis, 3, 100L);
    }

    /**
     * 모든 속성을 지정할 수 있는 생성자
     *
     * @param lockManager 락 매니저 구현체
     * @param defaultTimeoutMillis 기본 타임아웃 (밀리초)
     * @param maxRetries 최대 재시도 횟수
     * @param retryDelayMillis 재시도 간 대기시간 (밀리초)
     */
    public SynchronizedLockTemplate(LockManager<K> lockManager, long defaultTimeoutMillis,
                                    int maxRetries, long retryDelayMillis) {
        this.lockManager = lockManager;
        this.defaultTimeoutMillis = defaultTimeoutMillis;
        this.maxRetries = maxRetries;
        this.retryDelayMillis = retryDelayMillis;
    }

    /**
     * 기본 타임아웃으로 락을 획득하고 동기화 블록 실행
     *
     * @param lockKey 락 키
     * @param block 실행할 블록
     * @return 블록 실행 결과
     * @throws LockTimeoutException 타임아웃 발생 시
     * @throws LockExecutionException 블록 실행 중 예외 발생 시
     */
    public <T> T executeWithLock(K lockKey, SynchronizedBlock<T> block) {
        return executeWithLock(lockKey, block, defaultTimeoutMillis);
    }

    /**
     * 지정된 타임아웃으로 락을 획득하고 동기화 블록 실행
     *
     * @param lockKey 락 키
     * @param block 실행할 블록
     * @param timeoutMillis 타임아웃 (밀리초)
     * @return 블록 실행 결과
     * @throws LockTimeoutException 타임아웃 발생 시
     * @throws LockExecutionException 블록 실행 중 예외 발생 시
     */
    public <T> T executeWithLock(K lockKey, SynchronizedBlock<T> block, long timeoutMillis) {
        if (lockKey == null) {
            throw new IllegalArgumentException("Lock key cannot be null");
        }

        final long startTime = System.currentTimeMillis();
        final long endTime = startTime + timeoutMillis;

        // 락 획득을 시도할 객체
        final Object lock = lockManager.getLock(lockKey);

        Exception lastException = null;

        // 타임아웃 내에서 락 획득 시도
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            // 타임아웃 체크
            if (System.currentTimeMillis() > endTime) {
                throw new LockTimeoutException(
                  String.format("Failed to acquire lock for key %s after %d ms", lockKey, timeoutMillis));
            }

            // 락 획득 시도
            try {
                synchronized (lock) {
                    try {
                        return block.execute();
                    } catch (Exception e) {
                        throw new LockExecutionException(
                          String.format("Error executing block with lock for key: %s", lockKey), e);
                    }
                }
            } catch (LockExecutionException e) {
                // 락은 획득했으나 실행 중 예외 발생 - 바로 예외 전파
                throw e;
            } catch (Exception e) {
                // 락 획득 실패 또는 기타 예외 - 재시도 가능
                lastException = e;

                // 잠시 대기 후 재시도
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(Math.min(retryDelayMillis, endTime - System.currentTimeMillis()));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new LockInterruptedException("Interrupted while waiting to acquire lock", ie);
                    }
                }
            }
        }

        // 모든 시도 실패
        throw new LockTimeoutException(
          String.format("Failed to acquire lock for key %s after %d attempts", lockKey, maxRetries + 1),
          lastException);
    }

    /**
     * 락 획득 시도 중 타임아웃 발생 시 던지는 예외
     */
    public static class LockTimeoutException extends RuntimeException {
        public LockTimeoutException(String message) {
            super(message);
        }

        public LockTimeoutException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * 락 획득 대기 중 인터럽트 발생 시 던지는 예외
     */
    public static class LockInterruptedException extends RuntimeException {
        public LockInterruptedException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * 락을 획득한 후 블록 실행 중 예외 발생 시 던지는 예외
     */
    public static class LockExecutionException extends RuntimeException {
        public LockExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}
