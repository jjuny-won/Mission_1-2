package com.mission.domain.post.controller;

import com.mission.domain.post.dto.PostCreateRequest;
import com.mission.domain.post.dto.PostListItem;
import com.mission.domain.post.dto.PostResponse;
import com.mission.domain.post.dto.PostUpdateRequest;
import com.mission.domain.post.service.PostService;
import com.mission.global.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) //1단계 필터가 넣어 둔 회원 ID 꺼내오기
    public PostResponse create(@AuthenticationPrincipal Long memberId,
                               @Valid @RequestBody PostCreateRequest request) {
        return postService.create(memberId, request);
    }

    @GetMapping("/{postId}")
    public PostResponse get(@PathVariable Long postId) {
        return postService.get(postId);
    }

    @GetMapping
    public PageResponse<PostListItem> getList(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        return postService.getList(page, size);
    }

    @PutMapping("/{postId}")
    public PostResponse update(@AuthenticationPrincipal Long memberId,
                               @PathVariable Long postId,
                               @Valid @RequestBody PostUpdateRequest request) {
        return postService.update(memberId, postId, request);
    }

    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Long memberId,
                       @PathVariable Long postId) {
        postService.delete(memberId, postId);
    }

}
