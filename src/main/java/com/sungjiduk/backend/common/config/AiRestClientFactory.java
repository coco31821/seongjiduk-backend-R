package com.sungjiduk.backend.common.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** 모든 AI 호출에 동일한 연결 deadline 정책을 강제한다. */
@Component
public class AiRestClientFactory {
    private final String baseUrl;
    private final Duration connectTimeout;
    public AiRestClientFactory(
            @org.springframework.beans.factory.annotation.Value("${seongjiduk.ai-service.base-url:http://localhost:8000}") String baseUrl,
            @org.springframework.beans.factory.annotation.Value("${seongjiduk.ai-service.connect-timeout:1s}") Duration connectTimeout) {
        this.baseUrl = baseUrl;
        this.connectTimeout = connectTimeout;
    }
    public RestClient create(Duration readTimeout) {
        HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(connectTimeout).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(readTimeout);
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }
}
