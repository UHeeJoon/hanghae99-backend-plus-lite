package io.hhplus.tdd.point;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.function.Supplier;

@RestController
@RequestMapping("/point")
@RequiredArgsConstructor
public class PointController {

    private static final Logger log = LoggerFactory.getLogger(PointController.class);
    private final PointService pointService;

    /**
     * TODO - 특정 유저의 포인트를 조회하는 기능을 작성해주세요.
     */
    @GetMapping("{id}")
    public UserPoint point(
      @PathVariable long id
    ) {
        return loggingMethodTime(() -> pointService.retrieveUserPointByUserId(id));
    }

    /**
     * TODO - 특정 유저의 포인트 충전/이용 내역을 조회하는 기능을 작성해주세요.
     */
    @GetMapping("{id}/histories")
    public List<PointHistory> history(
      @PathVariable long id
    ) {
        return loggingMethodTime(() -> pointService.retrievePointHistoryByUserId(id));
    }

    /**
     * TODO - 특정 유저의 포인트를 충전하는 기능을 작성해주세요.
     */
    @PatchMapping("{id}/charge")
    public UserPoint charge(
      @PathVariable long id,
      @RequestBody long amount
    ) {
        return loggingMethodTime(() -> pointService.chargePoint(id, amount));
    }

    /**
     * TODO - 특정 유저의 포인트를 사용하는 기능을 작성해주세요.
     */
    @PatchMapping("{id}/use")
    public UserPoint use(
      @PathVariable long id,
      @RequestBody long amount
    ) {
        return loggingMethodTime(() -> pointService.usePoint(id, amount));
    }

    /**
     * 메서드 실행 시간 로깅 - 간단 구현
     */
    private <T> T loggingMethodTime(final Supplier<T> method) {
        final long startTime = System.currentTimeMillis();
        final T methodRunningResult = method.get();
        final long endTime = System.currentTimeMillis();
        log.info("method execute time : {}ms", endTime - startTime);
        return methodRunningResult;
    }
}
