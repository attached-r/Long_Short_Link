package rj.highlink.testService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import rj.highlink.entity.dto.StatsQueryDTO;
import rj.highlink.entity.vo.StatsVO;
import rj.highlink.service.StatsService;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 统计服务测试
 */
@SpringBootTest
public class TestStatsService {

    @Autowired
    private StatsService statsService;

    private String testShortCode;

    @BeforeEach
    void setUp() {
        testShortCode = "test123";
    }

    @Test // 测试记录访问
    void testRecordVisit() {
        statsService.recordVisit(testShortCode, "192.168.1.1");
        statsService.recordVisit(testShortCode, "192.168.1.2");
        statsService.recordVisit(testShortCode, "192.168.1.1");

        StatsQueryDTO dto = new StatsQueryDTO();
        dto.setShortCode(testShortCode);

        StatsVO stats = statsService.getStats(dto);

        assertNotNull(stats);
        assertEquals(testShortCode, stats.getShortCode());
        assertTrue(stats.getPv() >= 3);
        assertTrue(stats.getUv() >= 2);
    }

    @Test // 测试获取统计数据
    void testGetStatsWithTimeRange() {
        String shortCode = "testTimeRange";
        LocalDateTime now = LocalDateTime.now();

        statsService.recordVisit(shortCode, "10.0.0.1");

        StatsQueryDTO dto = new StatsQueryDTO();
        dto.setShortCode(shortCode);
        dto.setStartTime(now.minusHours(1));
        dto.setEndTime(now.plusHours(1));

        StatsVO stats = statsService.getStats(dto);

        assertNotNull(stats);
        assertEquals(shortCode, stats.getShortCode());
        assertNotNull(stats.getStartTime());
        assertNotNull(stats.getEndTime());
    }

    @Test // 测试获取不存在的短链接的统计数据
    void testGetStatsForNonExistentShortCode() {
        StatsQueryDTO dto = new StatsQueryDTO();
        dto.setShortCode("nonexistent");

        StatsVO stats = statsService.getStats(dto);

        assertNotNull(stats);
        assertEquals(0L, stats.getPv());
        assertEquals(0L, stats.getUv());
    }
}
