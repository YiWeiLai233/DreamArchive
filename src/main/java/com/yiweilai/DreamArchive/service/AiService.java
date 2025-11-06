package com.yiweilai.DreamArchive.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.yiweilai.DreamArchive.DTO.AI;
import com.yiweilai.DreamArchive.DTO.messages;
import com.yiweilai.DreamArchive.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    public String aiSerice() throws JsonProcessingException {

        //修改成jackson
        String json = """
                {"temperature":0.5,"messages":[{"role":"user","content":"在一个晚上我梦到在房间里面我面对着镜子镜子里面有一个我但是和我做的动作不一样,我很恐慌"}],"model":"gpt-5-nano"}
                """;

        List<messages> ai = new ArrayList<messages>();
        messages m = new messages("user","test");
        ai.add(m);
        System.out.println(JsonUtil.toJSON(ai, "gpt5"));

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
        return "0";
    }

}
