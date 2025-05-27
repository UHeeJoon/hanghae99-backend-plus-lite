package io.hhplus.tdd.point;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

/**
 * 포인트 정책 테스트
 *
 * @author :Uheejoon
 * @date :2025-05-18 오후 9:30
 */
@DisplayName("포인트 정책 테스트")
class PointPolicyTest {

    final static long MAX_BALANCE = 10_000_000_000L;
    final long originPoint = 1000L;
    final PointPolicy usePolicy = PointPolicy.USE;
    final PointPolicy chargePolicy = PointPolicy.CHARGE;

    static Stream<Object[]> validAmount() {
        return Stream.of(
          // 정상 사용 1_000 ~ 10_000_000_000L
          new Object[]{1000L, 1000L, PointPolicy.USE},
          new Object[]{2000L, 1999L, PointPolicy.USE},
          new Object[]{MAX_BALANCE, 1000L, PointPolicy.USE},
          new Object[]{MAX_BALANCE, MAX_BALANCE, PointPolicy.USE},

          // 정상 충전 10_000 ~ 10_000_000_000L
          new Object[]{1000L, 10000L, PointPolicy.CHARGE},
          new Object[]{1000L, 20000L, PointPolicy.CHARGE},
          new Object[]{0L, MAX_BALANCE, PointPolicy.CHARGE},
          new Object[]{MAX_BALANCE - 10000L, 10000L, PointPolicy.CHARGE}
        );
    }

    static Stream<Object[]> invalidAmount() {
        return Stream.of(
          // 비정상 사용 금액 < 1_000
          new Object[]{0L, PointPolicy.USE},
          new Object[]{-1L, PointPolicy.USE},
          new Object[]{999L, PointPolicy.USE},

          // 비정상 충전 금액 < 10_000
          new Object[]{0L, PointPolicy.CHARGE},
          new Object[]{-1L, PointPolicy.CHARGE},
          new Object[]{9_999L, PointPolicy.CHARGE}
        );
    }

    /**
     * 1. 포인트 사용 후의 잔액이 0보다 크거나 같을때 정상적으로 통과되어야 한다.
     * 2. 포인트 충전 후의 잔액이 10_000_000_000L보다 작거나 같을 때 정상적으로 통과되어야 한다.
     */
    @MethodSource("validAmount")
    @ParameterizedTest(name = "[정책: {2}] 잔액={0}, 입력 금액={1}")
    @DisplayName("사용 후 잔액이 0 이상이고, 충전 후 잔액이 최대 10_000_000_000 이하인 경우 예외가 발생하지 않는다.")
    void 정상_시나리오_예외없음(long origin, long amount, PointPolicy policy) {
        // when && then
        assertThatNoException()
          .isThrownBy(() -> policy.validate(origin, amount));
    }

    /**
     * 1. 포인트를 충전 시키려는 금액은 0보다 작거나 같을 수 없다.
     * 2. 포인트를 사용 시키려는 금액은 0보다 커야 한다.
     */
    @ParameterizedTest(name = "[정책: {1}] 입력 금액: {0}")
    @MethodSource("invalidAmount")
    @DisplayName("포인트 충전 금액이 0보다 작거나 같다면 IllegalArgumentException을 던진다.")
    void 입력_금액이_0보다_작으면_IllegalArgumentException를_던진다(long amount, PointPolicy policy) {
        // when & then
        assertThatIllegalArgumentException()
          .isThrownBy(() -> policy.validate(originPoint, amount))
          .withMessageContaining("이상이어야 합니다.");
    }

    /**
     * 잔여 포인트보다 사용 금액이 더 클 수 없다.
     */
    @Test
    @DisplayName("잔여 포인트보다 사용 금액이 더 크다면 IllegalStateException을 던진다.")
    void 잔여_포인트보다_사용_포인트가_더_크면_IllegalStateException를_던진다() {
        // given
        long amount = originPoint + 1;

        // when && then
        assertThatIllegalStateException()
          .isThrownBy(() -> usePolicy.validate(originPoint, amount))
          .withMessage("포인트가 부족합니다.");
    }

    /**
     * 포인트 충전 후 잔액이 10_000_000_000보다 클 수 없다.
     */
    @ParameterizedTest(name = "잔액: {0}, 충전 금액: {1}")
    @CsvSource({"0," + (MAX_BALANCE + 1), "1000," + (MAX_BALANCE)})
    @DisplayName("포인트 충전 후 잔액이 10_000_000_000보다 크다면 IllegalStateException를 던진다.")
    void 충전_후_잔액이_10_000_000_000보다_크다면_IllegalStateException를_던진다(long origin, long amount) {
        // when & then
        assertThatIllegalStateException()
          .isThrownBy(() -> chargePolicy.validate(origin, amount));
    }

}