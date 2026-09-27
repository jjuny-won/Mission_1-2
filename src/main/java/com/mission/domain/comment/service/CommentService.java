package com.mission.domain.comment.service;

import com.mission.domain.comment.dto.CommentCreateRequest;
import com.mission.domain.comment.dto.CommentResponse;
import com.mission.domain.comment.dto.CommentUpdateRequest;
import com.mission.domain.comment.entity.Comment;
import com.mission.domain.comment.repository.CommentRepository;
import com.mission.domain.member.entity.Member;
import com.mission.domain.member.repository.MemberRepository;
import com.mission.domain.post.entity.Post;
import com.mission.domain.post.repository.PostRepository;
import com.mission.global.exception.BusinessException;
import com.mission.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService{

    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public CommentResponse create(Long memberId, Long postId, CommentCreateRequest request){
        Member author = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        Comment comment = commentRepository.save(new Comment(request.content(), author, post));
        return CommentResponse.from(comment);
    }

    public List<CommentResponse> getList(Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        return commentRepository.findAllByPostIdWithAuthor(postId).stream()
                .map(CommentResponse::from)
                .toList();
    }

    @Transactional
    public CommentResponse update(Long memberId, Long postId, Long commentId,
                                  CommentUpdateRequest request) {
        Comment comment = findComment(postId, commentId);
        checkAuthor(comment, memberId);

        comment.update(request.content());
        commentRepository.flush();   // 수정 시각을 확정 ->  응답 생성

        return CommentResponse.from(comment);
    }

    @Transactional
    public void delete(Long memberId, Long postId, Long commentId) {
        Comment comment = findComment(postId, commentId);
        checkAuthor(comment, memberId);

        commentRepository.delete(comment);
    }


    private Comment findComment(Long postId, Long commentId) {
        return commentRepository.findByIdAndPostIdWithAuthor(commentId, postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
    }
    private void checkAuthor(Comment comment, Long memberId) {
        if (!comment.isWrittenBy(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

}
