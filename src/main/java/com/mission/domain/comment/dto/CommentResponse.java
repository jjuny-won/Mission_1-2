package com.mission.domain.comment.dto;

import com.mission.domain.comment.entity.Comment;
import com.mission.domain.post.dto.PostResponse;

import java.time.LocalDateTime;

public record CommentResponse (
        Long id,
        Long postId,
        String content,
        Long authorId,
        String authorNickname,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
){
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.getContent(),
                comment.getAuthor().getId(),
                comment.getAuthor().getNickname(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
