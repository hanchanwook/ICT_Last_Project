package com.jakdang.labs.config;

import com.fasterxml.jackson.databind.ObjectMapper;
//import com.jakdang.labs.security.oauth.service.CustomOAuth2UserService;
//import com.jakdang.labs.security.oauth.handler.OAuth2AuthenticationFailureHandler;
//import com.jakdang.labs.security.oauth.handler.OAuth2AuthenticationSuccessHandler;
//import com.jakdang.labs.security.oauth.service.HttpCookieOAuth2AuthorizationRequestRepository;
// import com.jakdang.labs.api.jaegyeom.common.GetMemberIdService;
import com.jakdang.labs.security.service.CustomUserDetailsService;
import com.jakdang.labs.security.jwt.*;
import com.jakdang.labs.security.jwt.service.LogoutService;
import com.jakdang.labs.security.jwt.service.TokenService;
import com.jakdang.labs.security.jwt.utils.JwtUtil;
import com.jakdang.labs.security.jwt.utils.TokenUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // 상수로 public URL 패턴 정의
    public static final String[] PUBLIC_URLS = {
        "/api/find/**",
        "/api/auth/login",
        "/api/auth/signUp",
        "/api/auth/refresh",
        "/api/auth/join-sns",
        "/api/auth/join-sns2",
        "/refresh",
        "/api/public/**",
        "/api/health/**",
        "/api/simple-health/**",
        "/oauth2/authorization/**",
        "/oauth/callback", 
        "/login/oauth2/code/**",
        "/ws/**", "/ws/chat/**",
        "/api/channel/all",
        "/api/channel/active",
        "/api/staff/members",
        "/api/staff/members/*/permissions",
        "/api/staff/permissions",
        "/api/staff/permissions/main",
        "/api/staff/*/permissions",
        "/api/student/qr/check-in",
        "/api/student/qr/scan",
        "/api/student/qr/validate"
    };
//    .requestMatchers("/ws/**").permitAll()
    public static final String[] SWAGGER_URLS = {
        "/swagger-ui/**", 
        "/api-docs/**", 
        "/swagger-resources/**", 
        "/v3/api-docs/**"
    };

    public static final String[] GET_PUBLIC_URLS = {
        "/api/posts",
        "/api/posts/comments/*",
        "/api/organizations/recommended",
        "/api/organizations/detail/*"
    };

    public static final String[] POST_PUBLIC_URLS = {
        "/api/v2/file/findAll"
    };

    private final JwtUtil jwtUtil;
    private final TokenUtils tokenUtils;
    private final TokenService tokenService;
    private final LogoutService logoutService;
    private final ObjectMapper objectMapper;
    private final CustomUserDetailsService userDetailsService;
    // private final GetMemberIdService getMemberIdService;
//    private final CustomOAuth2UserService customOAuth2UserService;
//    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
//    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

//    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    @Value("${app.frontend-url}")
    private String frontendUrl;


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(32,32,1,1<<12,3);
    }

    @Bean
    public AuthenticationManager authenticationManager() throws Exception {
        DaoAuthenticationProvider userAuthProvider = new DaoAuthenticationProvider();
        userAuthProvider.setUserDetailsService(userDetailsService);
        userAuthProvider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(List.of(userAuthProvider));
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {
        // CSRF 비활성화 및 세션 관리 설정
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"인증되지 않은 사용자.\"}");
                        })
                );

        // OAUTH 설정
//        http
//                .oauth2Login(oauth2 -> oauth2
//                        .authorizationEndpoint(endpoint -> endpoint
//                                .baseUri("/oauth2/authorization")
//                                .authorizationRequestRepository(httpCookieOAuth2AuthorizationRequestRepository))
//                        .redirectionEndpoint(endpoint -> endpoint
//                                .baseUri("/login/oauth2/code/*"))
//                        .userInfoEndpoint(userInfo -> userInfo
//                                .userService(customOAuth2UserService))
//                        .loginPage("/api/auth/login"));
//                        .successHandler(oAuth2AuthenticationSuccessHandler)
//                        .failureHandler(oAuth2AuthenticationFailureHandler));

        // 인증/인가 설정
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        .requestMatchers(SWAGGER_URLS).permitAll()
                        .requestMatchers(HttpMethod.GET, GET_PUBLIC_URLS).permitAll()
                        .requestMatchers(HttpMethod.POST, POST_PUBLIC_URLS).permitAll()
                        .requestMatchers("/api/master/**").hasAnyRole("ADMIN", "TEACHER")
                        .requestMatchers("/ws/**", "/ws/chat/**", "/ws/chat/info").permitAll()
                        .requestMatchers("/api/student/qr/**").permitAll() // QR 관련 엔드포인트는 인증 없이 허용
                        .requestMatchers("/api/**").permitAll() // 임시시
                        .anyRequest().authenticated());

        // 필터 설정
        http
                .addFilterBefore(new JWTFilter(jwtUtil, tokenUtils), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new LogoutFilter(logoutService, tokenUtils, objectMapper), JWTFilter.class)
                .addFilterAt(new LoginFilter(authenticationManager, tokenService, tokenUtils, objectMapper /*,getMemberIdService*/), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(frontendUrl, "http://localhost:7001", "https://jakdanglabs.com",
         "https://admin.jakdanglabs.com", "http://localhost:5174","http://lmsync.site" )); // 프론트엔드 도메인 설정
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers",
            "Cache-Control",
            "Pragma"
        ));
        
        // 쿠키 허용
        configuration.setAllowCredentials(true);
        
        // 프리플라이트 요청 캐시 시간
        configuration.setMaxAge(3600L);
        
        // 노출할 헤더
        configuration.setExposedHeaders(Arrays.asList(
            "Set-Cookie",
            "Authorization",
            "Access-Control-Allow-Credentials",
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Headers"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}