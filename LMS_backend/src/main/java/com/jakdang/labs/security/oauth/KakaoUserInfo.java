//package com.jakdang.labs.security.oauth;
//
//import java.util.Map;
//
//public class KakaoUserInfo implements OAuth2UserInfo {
//
//    private Map<String, Object> attributes; //oauth2User.getAttributes()
//
//    public KakaoUserInfo(Map<String, Object> attributes) {
//        this.attributes = attributes;
//    }
//
//    @Override
//    public String getProviderId() {
//        return (String)attributes.get("id").toString();
//    }
//
//    @Override
//    public String getProvider() {
//       return "kakao";
//    }
//
//    @Override
//    public String getEmail() {
//        Map<String, Object> map = ((Map<String, Object>)attributes.get("kakao_account"));
//       String email = (String)map.get("email");
//       return email ;
//    }
//
//    @Override
//    public String getName() {
//       Map<String, Object> map = ((Map<String, Object>)attributes.get("properties"));
//       String name = (String)map.get("nickname");
//       return name ;
//    }
//
//}
