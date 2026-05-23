package com.yiweilai.DreamArchive.config;

import com.yiweilai.DreamArchive.DTO.Message;
import com.yiweilai.DreamArchive.util.JsonUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Component
public class Aiconfig {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Aiconfig.class);
    @Value("${ai.api.url}")
    private String url;

    @Value("${ai.api.key}")
    private String apiKey;

    @Value("${ai.api.model}")
    private String model;
    public String ConnAi(String content){
        List<Message> ai = new ArrayList<>();
        ai.add(new Message("user",content));
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
