//package com.jakdang.labs.config;
//
//import com.fasterxml.jackson.core.JsonGenerator;
//import com.fasterxml.jackson.core.JsonParser;
//import com.fasterxml.jackson.databind.*;
//import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
//import com.fasterxml.jackson.databind.module.SimpleModule;
//import com.fasterxml.jackson.databind.ser.std.StdSerializer;
//import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
//import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponseType;
//import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
//
//import java.io.IOException;
//import java.util.Map;
//
///**
// * Spring Security OAuth2 클래스 직렬화/역직렬화를 위한 Jackson 모듈
// */
//public class OAuth2JacksonModule extends SimpleModule {
//
//    public OAuth2JacksonModule() {
//        super(OAuth2JacksonModule.class.getName());
//
//        // OAuth2AuthorizationResponseType 직렬화/역직렬화 등록
//        addSerializer(OAuth2AuthorizationResponseType.class, new OAuth2AuthorizationResponseTypeSerializer());
//        addDeserializer(OAuth2AuthorizationResponseType.class, new OAuth2AuthorizationResponseTypeDeserializer());
//
//        // 필요시 OAuth2AuthorizationRequest 직렬화/역직렬화 등록
//        // 현재는 responseType 부분만 처리하고 있습니다
//    }
//
//    /**
//     * OAuth2AuthorizationResponseType 열거형 직렬화
//     */
//    static class OAuth2AuthorizationResponseTypeSerializer extends StdSerializer<OAuth2AuthorizationResponseType> {
//
//        public OAuth2AuthorizationResponseTypeSerializer() {
//            super(OAuth2AuthorizationResponseType.class);
//        }
//
//        @Override
//        public void serialize(OAuth2AuthorizationResponseType value, JsonGenerator gen, SerializerProvider provider) throws IOException {
//            gen.writeString(value.getValue());
//        }
//    }
//
//    /**
//     * OAuth2AuthorizationResponseType 열거형 역직렬화
//     */
//    static class OAuth2AuthorizationResponseTypeDeserializer extends StdDeserializer<OAuth2AuthorizationResponseType> {
//
//        public OAuth2AuthorizationResponseTypeDeserializer() {
//            super(OAuth2AuthorizationResponseType.class);
//        }
//
//        @Override
//        public OAuth2AuthorizationResponseType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
//            String value = p.getValueAsString();
//            if (OAuth2ParameterNames.CODE.equals(value)) {
//                return OAuth2AuthorizationResponseType.CODE;
//            } else if ("token".equals(value)) {
//                return OAuth2AuthorizationResponseType.CODE;
//            }
//            return null;
//        }
//    }
//}