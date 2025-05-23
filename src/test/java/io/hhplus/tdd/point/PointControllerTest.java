package io.hhplus.tdd.point;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.hhplus.tdd.database.UserPointTable;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserPointTable userPointTable;

    private final long userId = 1L;
    private final long originAmount = 1_000_000_000L;
    private final int threadCount = 10;
    private final int executeCount = 50;

    @BeforeEach
    void setUp() {
        // 초기 포인트 설정
        userPointTable.insertOrUpdate(userId, originAmount);
    }

    /**
     *
     */
    @Test
    void 동시_포인트_사용_정합성_검증() throws Exception {
        long useAmount = 1000L;

        concurrentTest("/point/{id}/use", useAmount);

        // then: 최종 포인트가 0 이상이고, 최대 1000 이하인지 확인
        UserPoint result = userPointTable.selectById(userId);
        long remaining = result.point();

        assertThat(remaining).isEqualTo(originAmount - executeCount * useAmount);
    }

    /**
     *
     */
    @Test
    void 동시_포인트_충전_정합성_검증() throws Exception {
        long chargeAmount = 10000L;

        concurrentTest("/point/{id}/charge", chargeAmount);

        // then: 최종 포인트가 0 이상이고, 최대 1000 이하인지 확인
        UserPoint result = userPointTable.selectById(userId);
        long remaining = result.point();

        assertThat(remaining).isEqualTo(originAmount + executeCount * chargeAmount);
    }

    /**
     * 동시성 메서드
     */
    void concurrentTest(String url, long amount) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(executeCount);

        for (int i = 0; i < executeCount; i++) {
            executor.submit(() -> {
                try {
                    mockMvc.perform(patch(url, userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(String.valueOf(amount)))
                            .andExpect(status().isOk());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                latch.countDown();
            });
        }

        latch.await();
        executor.shutdown();
    }
}