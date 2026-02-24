package com.plog.domain.post.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

/**
 * 게시물 수정을 위해 클라이언트로부터 전달받는 요청 데이터 클래스입니다.
 * <p>
 * HTTP Request Body의 JSON 데이터를 자바 객체로 바인딩하며,
 * Jakarta Validation 어노테이션을 통해 입력 데이터의 유효성을 1차적으로 검증합니다.
 *
 * <p><b>상속 정보:</b><br>
 * 코틀린 [data class]로 정의되어 불변(Immutable) 상태를 보장합니다.
 *
 * <p><b>주요 생성자:</b><br>
 * [PostUpdateReq(String title, String content, List<String> hashtags, String thumbnail)] <br>
 *
 * <p><b>빈 관리:</b><br>
 * 스프링 빈으로 관리되지 않으며, Jackson 라이브러리에 의해 직렬화/역직렬화 시점에 생성됩니다.
 *
 * <p><b>외부 모듈:</b><br>
 * Jakarta Validation(@NotBlank)을 사용하여 유효성 검사 규칙을 정의합니다.
 *
 * @author MintyU
 * @see com.plog.domain.post.controller.PostController
 * @since 2026-01-20
 */
data class PostUpdateReq(
    @get:JvmName("title")
    @JsonProperty("title")
    @field:NotBlank(message = "제목은 필수 입력 항목입니다.")
    val title: String,

    @get:JvmName("content")
    @JsonProperty("content")
    @field:NotBlank(message = "본문은 필수 입력 항목입니다.")
    val content: String,

    @get:JvmName("hashtags")
    @JsonProperty("hashtags")
    val hashtags: List<String>? = null,

    @get:JvmName("thumbnail")
    @JsonProperty("thumbnail")
    val thumbnail: String? = null
)
