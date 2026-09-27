package com.mission.domain.comment.controller;

import com.mission.domain.comment.dto.CommentCreateRequest;
import com.mission.domain.comment.dto.CommentResponse;
import com.mission.domain.comment.dto.CommentUpdateRequest;
import com.mission.domain.comment.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse create(@AuthenticationPrincipal Long memberId,
                                  @PathVariable Long postId,
                                  @Valid @RequestBody CommentCreateRequest request) {
        return commentService.create(memberId, postId, request);
    }

    @GetMapping
    public List<CommentResponse> getList(@PathVariable Long postId) {
        return commentService.getList(postId);
    }

    @PutMapping("/{commentId}")
    public CommentResponse update(@AuthenticationPrincipal Long memberId,
                                  @PathVariable Long postId,
                                  @PathVariable Long commentId,
                                  @Valid @RequestBody CommentUpdateRequest request) {
        return commentService.update(memberId, postId, commentId, request);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Long memberId,
                       @PathVariable Long postId,
                       @PathVariable Long commentId) {
        commentService.delete(memberId, postId, commentId);
    }
}
