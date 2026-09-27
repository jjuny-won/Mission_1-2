package com.mission.domain.post.dto;

import com.mission.domain.post.entity.Post;

import java.time.LocalDateTime;

public record PostResponse(Long id,
                           String title,
                           String content,
                           Long authorId,
                           String authorNickname,
                           LocalDateTime createdAt,
                           LocalDateTime updatedAt) {

        public static PostResponse from(Post post) {
            return new PostResponse(
                    post.getId(),
                    post.getTitle(),
                    post.getContent(),
                    post.getAuthor().getId(),
                    post.getAuthor().getNickname(),
                    post.getCreatedAt(),
                    post.getUpdatedAt()
            );
        }
}
