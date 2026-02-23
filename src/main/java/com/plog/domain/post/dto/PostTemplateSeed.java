package com.plog.domain.post.dto;

/**
 * 파일을 읽어와, 사용자가 초기에 주어지는 post template seed 를 메모리에 저장할 때 사용합니다.
 *
 * @author jack8
 * @since 2026-01-26
 */
public record PostTemplateSeed (
       String name,
       String title,
       String content) {

    public static PostTemplateSeedBuilder builder() {
        return new PostTemplateSeedBuilder();
    }

    public static class PostTemplateSeedBuilder {
        private String name;
        private String title;
        private String content;

        PostTemplateSeedBuilder() {
        }

        public PostTemplateSeedBuilder name(String name) {
            this.name = name;
            return this;
        }

        public PostTemplateSeedBuilder title(String title) {
            this.title = title;
            return this;
        }

        public PostTemplateSeedBuilder content(String content) {
            this.content = content;
            return this;
        }

        public PostTemplateSeed build() {
            return new PostTemplateSeed(name, title, content);
        }
    }
}
