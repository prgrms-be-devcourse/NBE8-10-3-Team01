package com.plog.domain.member.controller

import com.plog.domain.member.dto.AuthInfoRes
import com.plog.domain.member.dto.AuthSignInReq
import com.plog.domain.member.dto.AuthSignUpReq
import com.plog.domain.member.service.AuthService
import com.plog.domain.post.service.PostTemplateService
import com.plog.global.response.CommonResponse
import com.plog.global.response.Response
import com.plog.global.security.TokenResolver
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

/**
 * 사용자 인증 및 권한 관리를 담당하는 컨트롤러입니다.
 *
 * 회원가입, 로그인(문서용), 로그아웃을 위한 엔드포인트를 제공하며,
 * [TokenResolver]를 통해 보안 쿠키의 생성 및 파기를 제어합니다.
 *
 * **상속 정보:**
 * 상속 정보 없음.
 *
 * **주요 생성자:**
 * `AuthController(AuthService authService, TokenResolver tokenResolver)`
 * 생성자 주입을 통해 인증 비즈니스 로직과 토큰 관리 컴포넌트를 주입받습니다.
 *
 * **빈 관리:**
 * `@RestController`를 사용하여 스프링 컨테이너의 빈으로 관리되며,
 * 모든 응답은 [CommonResponse] 형태로 반환됩니다.
 *
 * **외부 모듈:**
 * Spring Web, Jakarta Validation, Jakarta Servlet API 등을 사용합니다.
 *
 * @author minhee
 * @since 2026-01-20
 * @see AuthService
 * @see TokenResolver
 */
@RestController
@RequestMapping("/api/members")
class AuthController(
    private val authService: AuthService,
    private val tokenResolver: TokenResolver,
    private val postTemplateService: PostTemplateService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 새로운 회원을 등록(회원가입)합니다.
     * 인증/인가 도메인임을 고려하여 행위를 명시하는 경로를 예외적으로 사용합니다.
     *
     * @param req 회원가입에 필요한 이메일, 비밀번호, 닉네임 데이터
     * @return 생성된 회원의 조회 경로를 Location 헤더에 포함한 공통 응답 객체 (201 Created)
     */
    @PostMapping("/sign-up")
    fun signUp(
        @Valid @RequestBody req: AuthSignUpReq
    ): ResponseEntity<Void> {
        val memberId = authService.signUp(req)
        postTemplateService.initTemplateSeedOfUser(memberId)

        return ResponseEntity.created(URI.create("/api/members/$memberId")).build()
    }

    /**
     * 문서 확인용 로그인 API 입니다.
     *
     * 실제 로그인 로직은 `com.plog.global.security.LoginFilter`에서 처리됩니다.
     * Swagger 문서 생성을 위해 존재하며, 실제 요청 시 실행되지 않습니다.
     * 만약 실행될 시 설정 점검을 위해 예외를 던집니다.
     *
     * @param req 로그인 요청 데이터 (email, password)
     * @return 로그인 status(success), nickname, Access Token을 포함한 공통 응답 객체
     */
    @PostMapping("/sign-in")
    fun signIn(
        @Valid @RequestBody req: AuthSignInReq
    ): ResponseEntity<Response<AuthInfoRes>> {
        throw IllegalStateException("이 메서드는 LoginFilter에 의해 가로채져야 하며, 직접 호출될 수 없습니다.")
    }

    /**
     * 로그아웃을 수행합니다.
     *
     * 쿠키에서 리프레시 토큰을 추출하여 서버 측 저장소에서 무효화하고,
     * 클라이언트 브라우저의 인증 쿠키를 삭제 처리합니다.
     *
     * @param request  토큰 추출을 위한 HttpServletRequest
     * @param response 쿠키 삭제 설정을 위한 HttpServletResponse
     * @return 로그아웃 완료 메시지를 포함한 공통 응답 객체 (200 OK)
     */
    @GetMapping("/logout")
    fun logout(request: HttpServletRequest, response: HttpServletResponse): ResponseEntity<Response<Void>> {
        val refreshToken = tokenResolver.resolveRefreshToken(request)
        authService.logout(refreshToken)
        tokenResolver.deleteRefreshTokenCookie(response)

        return ResponseEntity.ok(
            CommonResponse.success(null, "로그아웃 되었습니다.")
        )
    }
}