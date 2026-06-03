package com.yiweilai.DreamArchive.service;

import com.yiweilai.DreamArchive.DTO.DreamContent;
import com.yiweilai.DreamArchive.mapper.DreamContentMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DreamServiceTest {

    @Test
    void updateEditableDreamUpdatesUnanalyzedDreamAndRebuildsStats() {
        DreamService service = new DreamService();
        DreamContentMapper dreamContentMapper = mock(DreamContentMapper.class);
        StatsService statsService = mock(StatsService.class);
        ReflectionTestUtils.setField(service, "dreamContentMapper", dreamContentMapper);
        ReflectionTestUtils.setField(service, "statsService", statsService);

        DreamContent existing = dream("dream-1", 7, "NONE");
        existing.setEmotion("happy");
        existing.setPlace("家中");
        existing.setCreatedAt(LocalDateTime.of(2026, 6, 3, 8, 30));
        when(dreamContentMapper.selectById("dream-1")).thenReturn(existing);
        when(dreamContentMapper.updateEditableDream("dream-1", "新的标题", "新的内容", "sad", "学校", "清晨 06:00-08:00")).thenReturn(1);

        DreamContent updated = service.updateEditableDream("dream-1", 7, "新的标题", "新的内容", "sad", "学校", "清晨 06:00-08:00");

        assertThat(updated).isNotNull();
        assertThat(updated.getTitle()).isEqualTo("新的标题");
        assertThat(updated.getContent()).isEqualTo("新的内容");
        assertThat(updated.getEmotion()).isEqualTo("sad");
        assertThat(updated.getPlace()).isEqualTo("学校");
        assertThat(updated.getTime()).isEqualTo("清晨 06:00-08:00");
        assertThat(updated.getAnalysisStatus()).isEqualTo("NONE");
        verify(statsService).rebuildStatsAfterDreamDeleted(7, LocalDate.of(2026, 6, 3));
    }

    @Test
    void updateEditableDreamRejectsAnalyzedDreams() {
        DreamService service = new DreamService();
        DreamContentMapper dreamContentMapper = mock(DreamContentMapper.class);
        StatsService statsService = mock(StatsService.class);
        ReflectionTestUtils.setField(service, "dreamContentMapper", dreamContentMapper);
        ReflectionTestUtils.setField(service, "statsService", statsService);

        when(dreamContentMapper.selectById("dream-1")).thenReturn(dream("dream-1", 7, "SUCCESS"));

        DreamContent updated = service.updateEditableDream("dream-1", 7, "新的标题", "新的内容", "sad", "学校", "清晨 06:00-08:00");

        assertThat(updated).isNull();
        verify(dreamContentMapper, never()).updateEditableDream("dream-1", "新的标题", "新的内容", "sad", "学校", "清晨 06:00-08:00");
        verify(statsService, never()).rebuildStatsAfterDreamDeleted(7, LocalDate.now());
    }

    private static DreamContent dream(String id, Integer userId, String analysisStatus) {
        DreamContent dream = new DreamContent();
        dream.setId(id);
        dream.setUserId(userId);
        dream.setTitle("旧标题");
        dream.setContent("旧内容");
        dream.setEmotion("happy");
        dream.setPlace("家中");
        dream.setTime("晚上 20:00-22:00");
        dream.setAnalysisStatus(analysisStatus);
        dream.setCreatedAt(LocalDateTime.now());
        return dream;
    }
}
