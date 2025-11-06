package com.yiweilai.DreamArchive;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Http {
    static HttpClient client=HttpClient.newHttpClient();
    public static void main(String[] args) {
        try {
            String url="https://www.bilibili.com";
            HttpRequest Request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type","text/html")
                    .header("Content-Type","charset=utf-8")
                    .GET()
                    .build();
            HttpResponse<String> response=client.send(Request,HttpResponse.BodyHandlers.ofString());
            System.out.println(response.body());
        }catch (Exception e) {
            e.printStackTrace();
        }

    }
}
