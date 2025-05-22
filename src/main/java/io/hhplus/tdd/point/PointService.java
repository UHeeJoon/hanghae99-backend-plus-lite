package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * point 관련 service
 *
 * @author :Uheejoon
 * @date :2025-05-17 오후 5:53
 */
@Service
@RequiredArgsConstructor
public class PointService {

    private static final Logger log = getLogger(PointService.class);

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    /**
     * userId 로 userPoint 조회
     *
     * @param userId 조회할 유저의 ID
     * @return UserPoint
     */
    public UserPoint retrieveUserPointByUserId(final long userId) {

        return userPointTable.selectById(userId);
    }

    /**
     * userId 로 pointHistory 조회
     *
     * @param userId 조회할 유저의 ID
     * @return List<PointHistory>
     */
    public List<PointHistory> retrievePointHistoryByUserId(final long userId) {

        return pointHistoryTable.selectAllByUserId(userId);
    }

    /**
     * 포인트 충전
     *
     * @param userId 포인트 충전할 유저의 ID
     * @param amount 포인트 충전할 금액
     * @return UserPoint
     */
    public UserPoint chargePoint(final long userId, final long amount) {
        synchronized ((Object) userId) {
            final UserPoint originUserPoint = retrieveUserPointByUserId(userId);
            final long remainingPoint = originUserPoint.charge(amount).point();

            log.info("[CHARGE] - user id: {}, origin point: {}, input point: {}, remaining point : {}", userId, originUserPoint.point(), amount, remainingPoint);

            final UserPoint userPoint = userPointTable.insertOrUpdate(userId, remainingPoint);

            pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, System.currentTimeMillis());

            return userPoint;
        }
    }

    /**
     * 포인트 사용
     *
     * @param userId 포인트 사용할 유저의 ID
     * @param amount 포인트 사용할 금액
     * @return UserPoint
     */
    public UserPoint usePoint(final long userId, final long amount) {
        synchronized ((Object) userId) {
            final UserPoint originUserPoint = retrieveUserPointByUserId(userId);
            final long remainingPoint = originUserPoint.use(amount).point();

            log.info("[USE] - user id: {}, origin point: {}, input point: {}, remaining point : {}", userId, originUserPoint.point(), amount, remainingPoint);

            final UserPoint userPoint = userPointTable.insertOrUpdate(userId, remainingPoint);

            pointHistoryTable.insert(userId, remainingPoint, TransactionType.USE, System.currentTimeMillis());

            return userPoint;
        }
    }

}
