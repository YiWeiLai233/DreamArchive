package com.yiweilai.DreamArchive.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Configuration
@Slf4j
public class Aiconfig {
    public String ConnAi(String content){
        //修改成jackson
        String json = """
                {"temperature":0.5,"messages":[{"role":"user","content":"在一个晚上我梦到在房间里面我面对着镜子镜子里面有一个我但是和我做的动作不一样,我很恐慌"}],"model":"gpt-5-nano"}
                """;
        try{
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request =HttpRequest.newBuilder()
                    .uri(URI.create("https://api.ruyun.fun/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer sk-GSGIRQDVzsgORLgvfeW96DogGb2bNsCDOEA29tXpIcw8lxtN")
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();
            // 创建线程池用于执行请求
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Callable<HttpResponse<String>> task = () -> client.send(request, HttpResponse.BodyHandlers.ofString());
            Future<HttpResponse<String>> future = executor.submit(task);
            HttpResponse<String> response = future.get(60, TimeUnit.SECONDS);

//            log.info(response.toString());
            return response.body();
        }catch (Exception e){
            e.printStackTrace();
        }
        return "0";
    }
    @Value("${ai.api.url}")
    private String url;

    @Value("${ai.api.key}")
    private String apiKey;

    @Value("${ai.api.model}")
    private String model;

    public Aiconfig() {
    }

    public Aiconfig(String url, String apiKey, String model) {
        this.url = url;
        this.apiKey = apiKey;
        this.model = model;
    }

    /**
     * 获取
     * @return url
     */
    public String getUrl() {
        return url;
    }

    /**
     * 设置
     * @param url
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * 获取
     * @return apiKey
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * 设置
     * @param apiKey
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * 获取
     * @return model
     */
    public String getModel() {
        return model;
    }

    /**
     * 设置
     * @param model
     */
    public void setModel(String model) {
        this.model = model;
    }

    public String toString() {
        return "Aiconfig{url = " + url + ", apiKey = " + apiKey + ", model = " + model + "}";
    }

}
