package com.plog.domain.hashtag.entity;

import com.plog.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

// 데이터의 중복을 막고, 효율적으로 관리하기 위해서 해시태그 이름에 유니크 제약조건을 추가합니다.
@Entity
public class HashTag extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    protected HashTag() {
    }

    public HashTag(String name) {
        this.name = name;
    }

    public static HashTagBuilder builder() {
        return new HashTagBuilder();
    }

    public static class HashTagBuilder {
        private String name;

        HashTagBuilder() {
        }

        public HashTagBuilder name(String name) {
            this.name = name;
            return this;
        }

        public HashTag build() {
            return new HashTag(name);
        }
    }

    public String getName() {
        return name;
    }
}