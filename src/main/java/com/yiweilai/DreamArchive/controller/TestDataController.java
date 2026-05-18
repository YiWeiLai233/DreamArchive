package com.yiweilai.DreamArchive.controller;

import com.yiweilai.DreamArchive.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/test")
public class TestDataController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 添加测试梦境数据
     * POST /api/test/add-dreams?email=859399899@qq.com
     */
    @PostMapping("/add-dreams")
    public Result<String> addTestDreams(@RequestParam String email) {
        try {
            // 获取用户 ID
            Integer userId = jdbcTemplate.queryForObject(
                    "SELECT id FROM user WHERE email = ?",
                    Integer.class,
                    email
            );

            if (userId == null) {
                return Result.error("用户不存在: " + email);
            }

            // 插入测试数据
            String[][] dreams = {
                    {UUID.randomUUID().toString(), String.valueOf(userId), "2026-05-12", "家里", "开心", "梦见自己在星空下飞翔，看到了美丽的银河", "这个梦象征着你对自由的渴望"},
                    {UUID.randomUUID().toString(), String.valueOf(userId), "2026-05-13", "学校", "平静", "梦见回到了大学校园，在图书馆看书", "怀旧的梦境，代表对过去美好时光的思念"},
                    {UUID.randomUUID().toString(), String.valueOf(userId), "2026-05-14", "海边", "开心", "梦见在海边捡贝壳，海浪轻轻拍打着沙滩", "海洋象征着内心深处的情感世界"},
                    {UUID.randomUUID().toString(), String.valueOf(userId), "2026-05-15", "森林", "恐惧", "梦见在一片漆黑的森林里迷路了", "迷路的梦可能反映现实生活中的迷茫感"},
                    {UUID.randomUUID().toString(), String.valueOf(userId), "2026-05-16", "城市", "难过", "梦见和朋友走散了，找不到回家的路", "分离焦虑的表现，珍惜身边的人"},
                    {UUID.randomUUID().toString(), String.valueOf(userId), "2026-05-17", "山上", "开心", "梦见爬到了山顶，看到了壮丽的日出", "象征着克服困难后获得的成就感"},
                    {UUID.randomUUID().toString(), String.valueOf(userId), "2026-05-18", "家里", "平静", "梦见和家人一起吃晚饭，很温馨", "家庭温暖的体现，内心感到安全"}
            };

            for (String[] dream : dreams) {
                jdbcTemplate.update(
                        "INSERT INTO dream (id, user_id, time, place, emotion, content, interpretation) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        dream[0], Integer.parseInt(dream[1]), dream[2], dream[3], dream[4], dream[5], dream[6]
                );
            }

            // 更新统计表
            jdbcTemplate.execute("""
                INSERT INTO dream_stats (user_id, stat_date, total_dreams, happy_count, sad_count, scared_count, peaceful_count)
                VALUES
                    (%d, '2026-05-12', 1, 1, 0, 0, 0),
                    (%d, '2026-05-13', 1, 0, 0, 0, 1),
                    (%d, '2026-05-14', 1, 1, 0, 0, 0),
                    (%d, '2026-05-15', 1, 0, 0, 1, 0),
                    (%d, '2026-05-16', 1, 0, 1, 0, 0),
                    (%d, '2026-05-17', 1, 1, 0, 0, 0),
                    (%d, '2026-05-18', 1, 0, 0, 0, 1)
                ON DUPLICATE KEY UPDATE
                    total_dreams = VALUES(total_dreams),
                    happy_count = VALUES(happy_count),
                    sad_count = VALUES(sad_count),
                    scared_count = VALUES(scared_count),
                    peaceful_count = VALUES(peaceful_count)
            """.formatted(userId, userId, userId, userId, userId, userId, userId));

            // 更新地点统计
            jdbcTemplate.execute("""
                INSERT INTO dream_place_stats (user_id, place, dream_count)
                VALUES
                    (%d, '家里', 2),
                    (%d, '学校', 1),
                    (%d, '海边', 1),
                    (%d, '森林', 1),
                    (%d, '城市', 1),
                    (%d, '山上', 1)
                ON DUPLICATE KEY UPDATE dream_count = VALUES(dream_count)
            """.formatted(userId, userId, userId, userId, userId, userId));

            return Result.success("成功添加 7 条测试数据");
        } catch (Exception e) {
            return Result.error("添加数据失败: " + e.getMessage());
        }
    }
}
