package com.mission.domain.post.dto;

import java.time.LocalDateTime;

public record PostListItem (
        Long id,
        String title,
        String authorNickname,
        Long commentCount,
        LocalDateTime createdAt
){
}
