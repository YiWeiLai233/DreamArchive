package com.yiweilai.DreamArchive.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class DreamAiTaskService {
    private static final Logger log = LoggerFactory.getLogger(DreamAiTaskService.class);

    @Autowired
    private AiService aiService;

    @Autowired
    private DreamService dreamService;

    public void completeDreamAiFields(String dreamId, String content, String imageUrl, boolean generateTitle, boolean analyze) {
        completeDreamAiFields(dreamId, content, imageUrl, null, null, null, generateTitle, analyze);
    }

    @Async
    public void completeDreamAiFields(String dreamId, String content, String imageUrl, String emotion, String place, String time, boolean generateTitle, boolean analyze) {
        if (generateTitle) {
            try {
                String title = aiService.generateDreamTitle(content);
                dreamService.updateTitle(dreamId, title);
            } catch (Exception e) {
                log.warn("Background dream title generation failed for {}", dreamId, e);
            }
        }

        if (analyze) {
            try {
                String interpretation = aiService.analyzeDream(content, imageUrl, emotion, place, time);
                dreamService.updateInterpretation(dreamId, interpretation);
                dreamService.updateAnalysisStatus(dreamId, "SUCCESS", null);
            } catch (Exception e) {
                log.warn("Background dream analysis failed for {}", dreamId, e);
                dreamService.updateInterpretation(dreamId, "");
                dreamService.updateAnalysisStatus(dreamId, "FAILED", "AI 解析失败，请稍后重试");
            }
        }
    }
}
