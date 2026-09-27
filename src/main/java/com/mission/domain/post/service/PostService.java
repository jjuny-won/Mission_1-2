package com.mission.domain.post.service;

import com.mission.domain.member.entity.Member;
import com.mission.domain.member.repository.MemberRepository;
import com.mission.domain.post.comment.repository.CommentRepository;
import com.mission.domain.post.dto.PostCreateRequest;
import com.mission.domain.post.dto.PostListItem;
import com.mission.domain.post.dto.PostResponse;
import com.mission.domain.post.dto.PostUpdateRequest;
import com.mission.domain.post.entity.Post;
import com.mission.domain.post.repository.PostRepository;
import com.mission.global.exception.BusinessException;
import com.mission.global.exception.ErrorCode;
import com.mission.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private static final int MAX_PAGE_SIZE = 50;

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public PostResponse create(Long memberId, PostCreateRequest request) {
        Member author = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Post post = postRepository.save(new Post(request.title(), request.content(), author));
        return PostResponse.from(post);
    }

    @Transactional
    public PostResponse update(Long memberId, Long postId, PostUpdateRequest request) {
        Post post = findPostWithAuthor(postId);
        checkAuthor(post, memberId);

        post.update(request.title(), request.content());
        postRepository.flush();   // 수정 시각을 확정한 뒤 응답 생성

        return PostResponse.from(post);
    }

    @Transactional
    public void delete(Long memberId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        checkAuthor(post, memberId);

        commentRepository.deleteByPostId(postId);   // 댓글 먼저 삭제
        postRepository.delete(post);
    }

    public PageResponse<PostListItem> getList(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        Page<PostListItem> result = postRepository.findPostList(PageRequest.of(safePage, safeSize));
        return PageResponse.from(result);
    }

    public PostResponse get(Long postId) {
        return PostResponse.from(findPostWithAuthor(postId));
    }


    private Post findPostWithAuthor(Long postId) {
        return postRepository.findWithAuthorById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
    }

    private void checkAuthor(Post post, Long memberId) {
        if (!post.isWrittenBy(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }



}
