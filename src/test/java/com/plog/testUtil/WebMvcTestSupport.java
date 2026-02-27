package com.plog.testUtil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import com.plog.domain.member.service.AuthService;
import com.plog.global.auth.CustomOAuth2UserService;
import com.plog.global.auth.oauth2.handler.OAuth2SuccessHandler;
import com.plog.global.security.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * WebMvcTest 에 대한 테스트 유틸입니다.
 * <p>
 * autowired 된 MockMvc 및 사용할 Objectmapper 를 사전에 정의합니다.
 * 보안 필터가 요구하는 공통 보안 빈들을 Mock 객체로 등록합니다.
 *
 * <p><b>상속 정보:</b><br>
 * 모든 slice 테스트의 부모 추상 클래스입니다.
 *
 *
 * @author jack8
 * @since 2026-01-20
 */

@WebMvcTest
@EnableAutoConfiguration(exclude = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        OAuth2ClientAutoConfiguration.class,
        OAuth2ClientWebSecurityAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import({SecurityTestConfig.class})
public abstract class WebMvcTestSupport {

    @Autowired
    protected WebApplicationContext context;

    protected MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .build();
    }

    protected final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new KotlinModule.Builder().build())
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @MockitoBean
    protected JwtUtils jwtUtils;

    @MockitoBean
    protected TokenResolver tokenResolver;

    @MockitoBean
    protected TokenStore tokenStore;

    @MockitoBean
    protected AuthenticationConfiguration authenticationConfiguration;

    @MockitoBean
    protected AuthService authService;

    @MockitoBean
    protected CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    protected CustomOAuth2UserService customOAuth2UserService;

    @MockitoBean
    protected OAuth2SuccessHandler oAuth2SuccessHandler;

    @MockitoBean
    protected CustomAuthenticationFilter customAuthenticationFilter;
}