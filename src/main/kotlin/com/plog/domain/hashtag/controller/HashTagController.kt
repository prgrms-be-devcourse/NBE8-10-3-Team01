package com.plog.domain.hashtag.controller

import com.plog.domain.hashtag.service.HashTagService
import com.plog.domain.post.dto.PostListRes
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


/**
 * 해시태그 관련 요청을 처리하는 컨트롤러입니다.
 * 주로 특정 해시태그가 포함된 게시글을 검색하는 기능을 담당합니다.
 */
@RestController
@RequestMapping("/api/hashtags")
class HashTagController(
    private val hashTagService: HashTagService
) {

    /**
     * [해시태그 기반 게시글 페이징 검색]
     * 특정 키워드를 가진 해시태그를 검색하여 해당 태그가 달린 게시글 목록을 반환합니다.
     * * @param keyword 검색할 해시태그 키워드 (대소문자 구분 없이 처리됨)
     * @param pageable 페이징 설정 (기본값: 페이지당 10개, ID 역순 정렬)
     * @return 검색된 게시글들의 페이징 데이터 (PostListRes DTO 형태)
     * * 호출 예시: GET /api/hashtags/search?keyword=Java&page=0&size=10
     */
    @GetMapping("/search")
    fun search(
        @RequestParam("keyword") keyword: String?,
        // @PageableDefault를 통해 클라이언트가 파라미터를 보내지 않아도 기본 페이징 전략을 유지
        @PageableDefault(size = 10, sort = ["id"], direction = Sort.Direction.DESC) pageable: Pageable
    ): ResponseEntity<Page<PostListRes>> {
        val result = hashTagService.searchPostsByTag(keyword, pageable)
        return ResponseEntity.ok(result)
    }
}