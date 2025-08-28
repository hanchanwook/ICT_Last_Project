package com.jakdang.labs.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.jakdang.labs.exceptions.handler.CustomException;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.support.PageJacksonModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Configuration
public class FeignConfig {
    @Value("${fegin.school.appId}")
    private String appId;
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    @Bean
    public ObjectMapper feignObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new PageJacksonModule());
        return mapper;
    }

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                template.header("AppId", appId);  // AppId 헤더 추가
            }
        };
    }

    @Bean
    public ErrorDecoder feignErrorDecoder() {
        return new FeignErrorDecoder();
    }

    public static class FeignErrorDecoder implements ErrorDecoder {

        private final ErrorDecoder defaultErrorDecoder = new Default();
        @Override
        public Exception decode(String methodKey, Response response) {
            try {
                // 응답 본문을 String으로 읽어오기
                String responseBody = Util.toString(response.body().asReader());

                // 응답 본문을 Map으로 변환
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> errorResponse = objectMapper.readValue(responseBody, Map.class);

                // 중첩된 JSON에서 메시지를 추출
                String errorMessage = (String) errorResponse.get("message");

                // 메시지가 잘못된 경우에는 원본 메시지를 그대로 반환
                if (errorMessage == null) {
                    errorMessage = responseBody;  // 예외 메시지가 없으면 원본 메시지 사용
                }

                // CustomException 던지기
                return new CustomException(errorMessage, 500);

            } catch (Exception e) {
                // 예외가 발생한 경우 기본 ErrorDecoder 사용
                return defaultErrorDecoder.decode(methodKey, response);
            }
        }
    }
}
