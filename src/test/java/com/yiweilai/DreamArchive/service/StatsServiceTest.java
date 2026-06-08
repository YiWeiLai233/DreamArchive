package com.yiweilai.DreamArchive.service;

import com.yiweilai.DreamArchive.DTO.DreamStats;
import com.yiweilai.DreamArchive.DTO.DreamStatsEntity;
import com.yiweilai.DreamArchive.mapper.DreamStatsMapper;
import com.yiweilai.DreamArchive.mapper.StatsMapper;
import com.yiweilai.DreamArchive.util.Result;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StatsServiceTest {

    @Test
    void getDreamStatsUsesDreamContentCountForTotalDreams() {
        StatsService service = new StatsService();
        StatsMapper statsMapper = mock(StatsMapper.class);
        DreamStatsMapper dreamStatsMapper = mock(DreamStatsMapper.class);
        ReflectionTestUtils.setField(service, "statsMapper", statsMapper);
        ReflectionTestUtils.setField(service, "dreamStatsMapper", dreamStatsMapper);

        int userId = 7;
        when(statsMapper.countByUserId(userId)).thenReturn(5);
        when(dreamStatsMapper.selectRecentDays(userId, 7)).thenReturn(List.of(
                statsFor(LocalDate.now().minusDays(1), 1),
                statsFor(LocalDate.now(), 1)
        ));
        when(dreamStatsMapper.selectPlaceStatsByUser(userId)).thenReturn(List.of());

        Result<DreamStats> result = service.getDreamStats(userId);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getTotalDreams()).isEqualTo(5);
        assertThat(result.getData().getRecentTrend()).hasSize(2);
    }

    @Test
    void getTotalDreamsUsesDreamContentCountForAllDreams() {
        StatsService service = new StatsService();
        StatsMapper statsMapper = mock(StatsMapper.class);
        DreamStatsMapper dreamStatsMapper = mock(DreamStatsMapper.class);
        ReflectionTestUtils.setField(service, "statsMapper", statsMapper);
        ReflectionTestUtils.setField(service, "dreamStatsMapper", dreamStatsMapper);

        int userId = 7;
        when(statsMapper.countByUserId(userId)).thenReturn(5);
        when(dreamStatsMapper.selectRecentDays(userId, 365)).thenReturn(List.of(
                statsFor(LocalDate.now(), 2)
        ));

        Result<Integer> result = service.getTotalDreams(userId);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(5);
    }

    private static DreamStatsEntity statsFor(LocalDate date, int totalDreams) {
        DreamStatsEntity stats = new DreamStatsEntity(7, date);
        stats.setTotalDreams(totalDreams);
        return stats;
    }
}
