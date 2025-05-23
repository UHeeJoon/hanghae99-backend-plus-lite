package io.hhplus.tdd.point;

import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * 포인트 관련 정책
 *
 * @author :Uheejoon
 * @date :2025-05-18 오후 9:10
 */
public enum PointPolicy {
    /**
     * 충전 포인트 정책
     * <li>충전 금액은 최소 10000 이상</li>
     * <li>충전 후 잔액은 10_000_000_000L 이하 - 이상일 경우 {@link IllegalStateException}</li>
     */
    CHARGE {
        private static final long MIN_CHARGE_AMOUNT = 10_000;
        private static final long MAX_BALANCE = 10_000_000_000L;

        @Override
        public void validate(final long currentPoint, final long amount) {
            if (amount < MIN_CHARGE_AMOUNT) {
                log.warn("{} - 유효하지 않은 충전 요청: amount={}", name(), amount);
                throw new IllegalArgumentException("충전 금액은 " + MIN_CHARGE_AMOUNT + " 이상이어야 합니다.");
            }
            if (currentPoint + amount > MAX_BALANCE) {
                log.info("포인트를 {}보다 많이 보유할 수 없습니다. {}", MAX_BALANCE,
                  pointLogMessage(amount, currentPoint, currentPoint + amount));
                throw new IllegalStateException("충전 후 잔액은 최대 " + MAX_BALANCE + "을 초과할 수 없습니다.");
            }
        }
    },

    /**
     * 사용 포인트 정책
     * <li>사용 금액은 최소 1,000 이상</li>
     * <li>사용 후 잔액은 음수일 수 없음 - 음수일 경우 {@link IllegalStateException}</li>
     */
    USE {
        private static final long MIN_USE_AMOUNT = 1_000;
        private static final long MIN_BALANCE = 0;

        @Override
        public void validate(final long currentPoint, final long amount) {
            if (amount < MIN_USE_AMOUNT) {
                log.warn("{} - 유효하지 않은 사용 요청: amount={}", name(), amount);
                throw new IllegalArgumentException("사용 금액은 " + MIN_USE_AMOUNT + " 이상이어야 합니다.");
            }
            if (currentPoint - amount < MIN_BALANCE) {
                log.info("포인트가 부족합니다. {}", pointLogMessage(amount, currentPoint, currentPoint - amount));
                throw new IllegalStateException("포인트가 부족합니다.");
            }
        }
    };

    protected final Logger log = getLogger(PointPolicy.class);

    /**
     * 포인트 유효성 검증
     *
     * @param currentPoint 현재 포인트
     * @param amount       입력 포인트 수량
     */
    public abstract void validate(final long currentPoint, final long amount);

    /**
     * 포인트 로그 메시지 생성
     *
     * @param amount       입력 포인트 수량
     * @param currentPoint 현재 포인트
     * @param afterPoint   포인트 변동 후 포인트
     * @return 포인트 로그 메시지
     */
    protected String pointLogMessage(final long amount, final long currentPoint, final long afterPoint) {
        return "요청: %d, 현재: %d, 결과: %d".formatted(amount, currentPoint, afterPoint);
    }
}
