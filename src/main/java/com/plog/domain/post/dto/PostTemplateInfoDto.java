package com.plog.domain.post.dto;

import com.plog.domain.post.entity.PostTemplate;

/**
 * post template 을 생성하기 위한 record 입니다.
 *
 * @author jack8
 * @since 2026-01-26
 */
public record PostTemplateInfoDto(

        Long id,

        String name,

        String title,

        String content
) {

    public static PostTemplateInfoDtoBuilder builder() {
        return new PostTemplateInfoDtoBuilder();
    }

    public static class PostTemplateInfoDtoBuilder {
        private Long id;
        private String name;
        private String title;
        private String content;

        PostTemplateInfoDtoBuilder() {
        }

        public PostTemplateInfoDtoBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public PostTemplateInfoDtoBuilder name(String name) {
            this.name = name;
            return this;
        }

        public PostTemplateInfoDtoBuilder title(String title) {
            this.title = title;
            return this;
        }

        public PostTemplateInfoDtoBuilder content(String content) {
            this.content = content;
            return this;
        }

        public PostTemplateInfoDto build() {
            return new PostTemplateInfoDto(id, name, title, content);
        }
    }

    public static PostTemplateInfoDto to(PostTemplate template) {
        return PostTemplateInfoDto.builder()
                .id(template.getId())
                .name(template.getName())
                .title(template.getTitle())
                .content(template.getContent())
                .build();
    }
}
