package com.plog.domain.hashtag.controller;

import com.plog.domain.hashtag.entity.HashTag;
import com.plog.domain.hashtag.service.HashTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hashtags")
@RequiredArgsConstructor
public class HashTagController {

    private final HashTagService hashTagService;

    @GetMapping
    public List<HashTag> getAllHashTags() {
        return hashTagService.getAllHashTags();
    }
}