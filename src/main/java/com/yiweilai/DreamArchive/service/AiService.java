package com.yiweilai.DreamArchive.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.yiweilai.DreamArchive.DTO.AI;
import com.yiweilai.DreamArchive.DTO.messages;
import com.yiweilai.DreamArchive.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Service
public class AiService {
    private static final Logger log = LoggerFactory.getLogger(AiService.class);
    @Value("${ai.api.url}")
    private static String url;

    @Value("${ai.api.key}")
    private static String apiKey;

    @Value("${ai.api.model}")
    private  static String model;
    public String aiSerice(String content) throws JsonProcessingException {
        String OldDream="";
        List<messages> ai = new ArrayList<messages>();
        ai.add(new messages("user",content+OldDream));
        //修改成jackson
        String json = JsonUtil.toJSON(ai, model);
        try{
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request =HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer "+apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();
            // 创建线程池用于执行请求
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Callable<HttpResponse<String>> task = () -> client.send(request, HttpResponse.BodyHandlers.ofString());
            Future<HttpResponse<String>> future = executor.submit(task);
            HttpResponse<String> response = future.get(60, TimeUnit.SECONDS);
            log.info(response.toString());
            return response.body();
        }catch (Exception e){
            e.printStackTrace();
        }
        return "200";
    }

}
