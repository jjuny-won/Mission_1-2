package com.mission.global.initData;

import com.mission.domain.comment.dto.CommentCreateRequest;
import com.mission.domain.comment.service.CommentService;
import com.mission.domain.member.dto.SignupRequest;
import com.mission.domain.member.entity.Member;
import com.mission.domain.member.repository.MemberRepository;
import com.mission.domain.member.service.MemberService;
import com.mission.domain.post.dto.PostCreateRequest;
import com.mission.domain.post.repository.PostRepository;
import com.mission.domain.post.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
@Profile("dev")
@RequiredArgsConstructor
public class InitData {
    private static final String PASSWORD = "password123";

    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final PostRepository postRepository;
    private final PostService postService;
    private final CommentService commentService;

    @Bean
    public ApplicationRunner devInitDataRunner() {
        return args -> {
            if (postRepository.count() > 0) {
                log.info("게시글이 이미 있어 더미 데이터 생성을 건너뜁니다.");
                return;
            }

            // 회원 3명 (이미 있으면 기존 회원 사용)
            Long user1 = findOrSignup("user1@test.com", "유저1");
            Long user2 = findOrSignup("user2@test.com", "유저2");
            Long user3 = findOrSignup("user3@test.com", "유저3");

            // 게시글 12개 (홀수 번째는 user1, 짝수 번째는 user2)
            List<Long> postIds = new ArrayList<>();
            for (int i = 1; i <= 12; i++) {
                Long authorId = (i % 2 == 1) ? user1 : user2;
                Long postId = postService.create(authorId,
                        new PostCreateRequest("테스트 글 " + i, "테스트 글 " + i + "의 본문입니다.")).id();
                postIds.add(postId);
            }

            // 댓글: 마지막 글에 3개, 그 앞 글에 1개, 나머지는 0개
            Long latest = postIds.get(11);
            Long second = postIds.get(10);
            commentService.create(user2, latest, new CommentCreateRequest("첫 번째 댓글입니다."));
            commentService.create(user3, latest, new CommentCreateRequest("두 번째 댓글입니다."));
            commentService.create(user1, latest, new CommentCreateRequest("작성자 답글입니다."));
            commentService.create(user3, second, new CommentCreateRequest("좋은 글이네요."));

            log.info("더미 데이터 생성 완료: 회원 3명, 게시글 12개, 댓글 4개");
        };
    }

    private Long findOrSignup(String email, String nickname) {
        return memberRepository.findByEmail(email)
                .map(Member::getId)
                .orElseGet(() -> memberService.signup(new SignupRequest(email, PASSWORD, nickname)).id());
    }
}
