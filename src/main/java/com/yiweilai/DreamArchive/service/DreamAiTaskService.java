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

    @Async
    public void completeDreamAiFields(String dreamId, String content, boolean generateTitle, boolean analyze) {
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
                String interpretation = aiService.analyzeDream(content);
                dreamService.updateInterpretation(dreamId, interpretation);
            } catch (Exception e) {
                log.warn("Background dream analysis failed for {}", dreamId, e);
                dreamService.updateInterpretation(dreamId, "AI 解析失败，你可以稍后重新解析。");
            }
        }
    }
}
