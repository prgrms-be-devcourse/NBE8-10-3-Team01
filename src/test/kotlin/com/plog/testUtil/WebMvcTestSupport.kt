package com.plog.testUtil

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.plog.domain.member.service.AuthService
import com.plog.global.security.*
import com.plog.global.security.oauth2.CustomOAuth2UserService
import com.plog.global.security.oauth2.handler.OAuth2SuccessHandler
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@AutoConfigureMockMvc(addFilters = false)
@Import(SecurityTestConfig::class)
abstract class WebMvcTestSupport {

    @Autowired
    protected lateinit var context: WebApplicationContext

    protected lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUpMockMvc() {
        this.mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .build()
    }

    protected val objectMapper: ObjectMapper = ObjectMapper()
        .registerModule(KotlinModule.Builder().build())
        .registerModule(JavaTimeModule())

    @MockitoBean
    lateinit var jwtUtils: JwtUtils

    @MockitoBean
    lateinit var tokenResolver: TokenResolver

    @MockitoBean
    lateinit var tokenStore: TokenStore

    @MockitoBean
    lateinit var authenticationConfiguration: AuthenticationConfiguration

    @MockitoBean
    lateinit var authService: AuthService

    @MockitoBean
    lateinit var customUserDetailsService: CustomUserDetailsService

    @MockitoBean
    lateinit var customOAuth2UserService: CustomOAuth2UserService

    @MockitoBean
    lateinit var oAuth2SuccessHandler: OAuth2SuccessHandler

    @MockitoBean
    lateinit var customAuthenticationFilter: CustomAuthenticationFilter
}
