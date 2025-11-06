package com.yiweilai.DreamArchive;

import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class main {
    private static final Logger log = LoggerFactory.getLogger(main.class);

    public static void main(String[] args) {
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
            System.out.println(response.body());
            log.info(response.toString());
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
