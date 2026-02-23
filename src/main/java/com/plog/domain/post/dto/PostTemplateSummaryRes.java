package com.plog.domain.post.dto;

import com.plog.domain.post.entity.PostTemplate;

/**
 * 사용자가 본인이 작성한 템플릿을 가져올 때 리스트로서 반환되는 dto 입니다.
 *
 * @author jack8
 * @since 2026-01-26
 */
public record PostTemplateSummaryRes(
        String name,
        Long id
) {
    public static PostTemplateSummaryResBuilder builder() {
        return new PostTemplateSummaryResBuilder();
    }

    public static class PostTemplateSummaryResBuilder {
        private String name;
        private Long id;

        PostTemplateSummaryResBuilder() {
        }

        public PostTemplateSummaryResBuilder name(String name) {
            this.name = name;
            return this;
        }

        public PostTemplateSummaryResBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public PostTemplateSummaryRes build() {
            return new PostTemplateSummaryRes(name, id);
        }
    }

    public static PostTemplateSummaryRes to(PostTemplate postTemplate) {
        return PostTemplateSummaryRes.builder()
                .name(postTemplate.getName())
                .id(postTemplate.getId())
                .build();
    }
}
