package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

/**
 * 포인트 서비스 테스트
 *
 * @author :Uheejoon
 * @date :2025-05-17 오후 6:49
 */
@DisplayName("포인트 서비스")
@Import({PointService.class, UserPointTable.class, PointHistoryTable.class})
@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @Mock
    private UserPointTable userPointTable;
    @Mock
    private PointHistoryTable pointHistoryTable;

    @InjectMocks
    private PointService pointService;

    @Nested
    @DisplayName("포인트 사용/충전 내역 조회")
    class PointHistoryTest {
        /**
         * 주어진 userId로 포인트 히스토리를 조회하면
         * pointHistoryTable.selectAllByUserId가 한 번 호출되고,
         * 반환된 리스트가 그대로 응답으로 나와야 한다.
         */
        @Test
        @DisplayName("포인트 히스토리를 조회를 하면 히스토리 리스트를 반환해야 한다.")
        void 포인트_히스토리를_조회하면_히스토리_리스트를_반환해야한다() {
            // given
            final long userId = 1L;
            final List<PointHistory> expected = List.of(
              new PointHistory(1L, userId, 1000L, TransactionType.CHARGE, System.currentTimeMillis()),
              new PointHistory(2L, userId, 1000L, TransactionType.CHARGE, System.currentTimeMillis()),
              new PointHistory(3L, userId, 1000L, TransactionType.USE, System.currentTimeMillis()),
              new PointHistory(4L, userId, 1000L, TransactionType.USE, System.currentTimeMillis())
            );
            given(pointHistoryTable.selectAllByUserId(userId))
              .willReturn(expected);

            // when
            List<PointHistory> actual = pointService.retrievePointHistoryByUserId(userId);

            // then
            then(pointHistoryTable).should(times(1))
              .selectAllByUserId(userId);

            assertThat(actual)
              .isNotNull()
              .hasSameSizeAs(expected)
              .containsExactlyElementsOf(expected);
        }

    }


    @Nested
    @DisplayName("포인트 조회")
    class RetrievePoint {

        final long userId = 1L;

        /**
         * 사용자에 등록된 포인트가 없을 때 조회하면
         * UserPoint.empty가 반환되어야 한다.
         */
        @Test
        @DisplayName("포인트가 존재하지 않은 사용자가 포인트를 조회했을 때 0포인트가 조회되어야 한다.")
        void 포인트가_존재하지_않은_사용자가_조회하면_0포인트가_조회되어야한다() {
            // given
            final long now = System.currentTimeMillis();
            given(userPointTable.selectById(userId))
              .willReturn(UserPoint.empty(userId));

            // when
            final UserPoint userPoint = pointService.retrieveUserPointByUserId(userId);

            // then
            then(userPointTable).should(times(1)).selectById(userId);
            then(pointHistoryTable).shouldHaveNoInteractions();

            assertThat(userPoint)
              .withFailMessage("userPoint는 null이면 안 됨")
              .isNotNull();
            assertThat(userPoint.id())
              .withFailMessage("userPoint의 id는 userId와 같아야 함")
              .isEqualTo(userId);

            assertThat(userPoint.point())
              .withFailMessage("userPoint의 point는 0이어야 함")
              .isZero();
            assertThat(userPoint.updateMillis())
              .withFailMessage("userPoint의 updateMillis는 현재 시간 이전이어야 함")
              .isLessThanOrEqualTo(System.currentTimeMillis())
              .withFailMessage("바로 직전에 만들어진 값이여야함")
              .isGreaterThan(now - 1000); // 1초 내에 생성된 값이어야
        }

        /**
         * 사용자에 등록된 포인트가 있을 때 조회하면
         * 해당 포인트 객체가 그대로 반환되어야 한다.
         */
        @Test
        @DisplayName("포인트가 존재하는 사용자가 포인트를 조회했을 때 포인트가 조회되어야 한다.")
        void 포인트가_존재하는_사용자가_포인트를_조회하면_해당_포인트가_조회_되어야한다() {
            // given
            final long now = System.currentTimeMillis();
            final long point = 1000L;
            given(userPointTable.selectById(userId))
              .willReturn(new UserPoint(userId, point, now));

            // when
            final UserPoint userPoint = pointService.retrieveUserPointByUserId(userId);

            // then
            then(userPointTable).should(times(1)).selectById(userId);
            then(pointHistoryTable).shouldHaveNoInteractions();

            assertThat(userPoint)
              .withFailMessage("userPoint는 null이면 안 됨")
              .isNotNull();
            assertThat(userPoint.id())
              .withFailMessage("userPoint의 id는 userId와 같아야 함")
              .isEqualTo(userId);

            assertThat(userPoint.point())
              .withFailMessage("userPoint의 point는 기존 값과 같아야 함")
              .isEqualTo(point);
            assertThat(userPoint.updateMillis())
              .withFailMessage("userPoint의 updateMillis는 현재 시간 이전이어야 함")
              .isLessThanOrEqualTo(System.currentTimeMillis());

        }
    }

    @Nested
    @DisplayName("포인트 충전")
    class ChargePoint {
        /**
         * 신규 사용자(또는 기존 포인트가 없는 사용자)가 chargePoint 호출 시
         * selectById 호출 되고
         * insertOrUpdate 호출해 포인트 amount 만큼 충전
         * 해당 충전 내역을 히스토리 테이블에 삽입하고
         * UserPoint 를 반환 해야한다.
         */
        @Test
        @DisplayName("포인트 충전 정상 시나리오")
        void 포인트_충전_정상() {
            // given
            final long userId = 1L;
            final long amount = 10_000L;
            final long updateMillis = System.currentTimeMillis();
            given(userPointTable.selectById(userId)).willReturn(UserPoint.empty(userId));
            given(userPointTable.insertOrUpdate(userId, amount)).willReturn(new UserPoint(userId, amount, updateMillis));
            given(pointHistoryTable.insert(
              eq(userId),
              eq(amount),
              eq(TransactionType.CHARGE),
              anyLong()))
              .willReturn(new PointHistory(1L, userId, amount, TransactionType.CHARGE, updateMillis));

            // when
            UserPoint result = pointService.chargePoint(userId, amount);

            // then
            then(userPointTable).should(times(1)).selectById(userId);
            then(userPointTable).should(times(1)).insertOrUpdate(eq(userId), eq(amount));
            then(pointHistoryTable).should(times(1))
              .insert(eq(userId), eq(amount), eq(TransactionType.CHARGE), anyLong());

            assertThat(result.id()).isEqualTo(userId);
            assertThat(result.point()).isEqualTo(amount);
        }

        /**
         * 충전 금액이 10,000 미만일 경우
         * IllegalArgumentException이 발생하고,
         * insertOrUpdate 및 history insert가 호출되지 않아야 한다.
         */
        @Test
        @DisplayName("충전 금액이 10,000보다 작다면 IllegalArgumentException를 발생시키고 종료된다.")
        void 충전금액은_10_000_이상이어야_한다() {
            // given
            final long userId = 1L;
            final long invalidAmount = 0L;
            given(userPointTable.selectById(userId)).willReturn(UserPoint.empty(userId));

            // when & then
            assertThatIllegalArgumentException()
              .isThrownBy(() -> pointService.chargePoint(userId, invalidAmount))
              .withMessage("충전 금액은 10000 이상이어야 합니다.");

            then(userPointTable).should(never()).insertOrUpdate(anyLong(), anyLong());
            then(pointHistoryTable).shouldHaveNoInteractions();
        }

        /**
         * 충전 후 잔액이 10,000,000,000을 초과할 경우
         * IllegalStateException이 발생하고,
         * insertOrUpdate 및 history insert가 호출되지 않아야 한다.
         */
        @Test
        @DisplayName("충전 후 잔액이 10,000,000,000 보다 크다면 IllegalStateException를 발생시키고 종료된다.")
        void 충전_후_잔액이_10_000_000_000_보다_클_수_없다() {
            // given
            final long userId = 1L;
            final long invalidAmount = 10_000_000_001L;
            given(userPointTable.selectById(userId)).willReturn(UserPoint.empty(userId));

            // when & then
            assertThatIllegalStateException()
              .isThrownBy(() -> pointService.chargePoint(userId, invalidAmount))
              .withMessage("충전 후 잔액은 최대 10000000000을 초과할 수 없습니다.");

            then(userPointTable).should(never()).insertOrUpdate(anyLong(), anyLong());
            then(pointHistoryTable).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("포인트 사용")
    class PointUse {

        final long userId = 1L;
        final long originPoint = 10_000L;
        final long amount = 1_000L;
        final long remainingPoint = originPoint - amount;
        final long updateMillis = System.currentTimeMillis();

        /**
         * 기존 포인트가 충분할 때 usePoint 호출 시
         * selectById 호출
         * insertOrUpdate 호출(잔여 포인트 감소)
         * pointHistoryTable.insert 호출
         * 반환 UserPoint 필드 검증
         */
        @Test
        @DisplayName("포인트 사용 정상 시나리오")
        void 포인트_사용_정상() {
            // given
            given(userPointTable.selectById(userId)).willReturn(new UserPoint(userId, originPoint, updateMillis));
            given(userPointTable.insertOrUpdate(userId, remainingPoint)).willReturn(new UserPoint(userId, remainingPoint, updateMillis));
            given(pointHistoryTable.insert(
              eq(userId),
              eq(remainingPoint),
              eq(TransactionType.USE),
              anyLong()))
              .willReturn(new PointHistory(1L, userId, remainingPoint, TransactionType.USE, updateMillis));

            // when
            UserPoint result = pointService.usePoint(userId, amount);

            // then
            then(userPointTable).should(times(1)).selectById(userId);
            then(userPointTable).should(times(1)).insertOrUpdate(eq(userId), eq(remainingPoint));
            then(pointHistoryTable).should(times(1))
              .insert(eq(userId), eq(remainingPoint), eq(TransactionType.USE), anyLong());

            assertThat(result.id()).isEqualTo(userId);
            assertThat(result.point()).isEqualTo(remainingPoint);
        }

        /**
         * 사용 금액이 1,000 미만일 경우
         * IllegalArgumentException이 발생하고,
         * insertOrUpdate 및 history insert가 호출되지 않아야 한다.
         */
        @Test
        @DisplayName("충전 금액이 1,000보다 작다면 IllegalArgumentException를 발생시키고 종료된다.")
        void 사용금액은_1_000_이상이어야_한다() {
            // given
            final long userId = 1L;
            final long invalidAmount = 999L;
            given(userPointTable.selectById(userId)).willReturn(new UserPoint(userId, originPoint, updateMillis));

            // when & then
            assertThatIllegalArgumentException()
              .isThrownBy(() -> pointService.usePoint(userId, invalidAmount))
              .withMessage("사용 금액은 1000 이상이어야 합니다.");

            then(userPointTable).should(never()).insertOrUpdate(anyLong(), anyLong());
            then(pointHistoryTable).shouldHaveNoInteractions();
        }

        /**
         * 사용 후 잔액이 0보다 작을 경우
         * IllegalStateException이 발생하고,
         * insertOrUpdate 및 history insert가 호출되지 않아야 한다.
         */
        @Test
        @DisplayName("사용 후 잔액이 0 보다 작다면 IllegalStateException를 발생시키고 종료된다.")
        void 사용_후_잔액이_0보다_작을_수_없다() {
            // given
            given(userPointTable.selectById(userId)).willReturn(new UserPoint(userId, originPoint, updateMillis));

            // when & then
            assertThatIllegalStateException()
              .isThrownBy(() -> pointService.usePoint(userId, originPoint + 1))
              .withMessage("포인트가 부족합니다.");

            then(userPointTable).should(never()).insertOrUpdate(anyLong(), anyLong());
            then(pointHistoryTable).shouldHaveNoInteractions();
        }
    }

}