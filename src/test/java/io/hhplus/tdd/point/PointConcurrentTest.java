package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 동시성 테스트
 *
 * @author :Uheejoon
 * @date :2025-05-20 오후 8:37
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {PointService.class, UserPointTable.class, PointHistoryTable.class})
class PointConcurrentTest {

    @Autowired
    PointService pointService;

    final int threadCount = 50;
    final long userId = 1L;
    final int executeCount = 100;
    final long chargeAmount = 10000L;
    final long usingAmount = 1000L;

    /**
     * 동시에 요청 시 chargeAmount * executeCount 만큼 충전되어야 한다.
     */
    @Test
    void 동시에_충전하면_정확한_최종_포인트가_남아야_한다() throws Exception {
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch latch = new CountDownLatch(executeCount);

        for (int i = 0; i < executeCount; i++) {
//            final long userId = i;
            executor.submit(() -> {
                pointService.chargePoint(userId, chargeAmount);
                latch.countDown();
            });
        }

        latch.await();
        executor.shutdown();
//        var list = IntStream.range(0, executeCount).mapToLong((i) -> pointService.retrieveUserPointByUserId(userId).point()).boxed().toList();
//        assertThat(list).allMatch((point) -> point == chargeAmount);;

        final UserPoint userPoint = pointService.retrieveUserPointByUserId(userId);
        assertThat(userPoint.point()).isEqualTo(executeCount * chargeAmount);


    }

    @Test
    void 동시에_사용했을_때_정확한_최종_포인트가_남아야_한다() throws Exception {

        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch latch = new CountDownLatch(executeCount);

        pointService.chargePoint(userId, usingAmount * executeCount);

        for (int i = 0; i < executeCount; i++) {
            executor.submit(() -> {
                pointService.usePoint(userId, usingAmount);
                latch.countDown();
            });
        }

        latch.await();
        executor.shutdown();
        final UserPoint userPoint = pointService.retrieveUserPointByUserId(userId);
        assertThat(userPoint.point()).isEqualTo(0L);
    }

}