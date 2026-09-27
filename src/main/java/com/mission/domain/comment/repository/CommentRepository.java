package com.mission.domain.comment.repository;

import com.mission.domain.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {


    @Modifying
    @Query("delete from Comment c where c.post.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);

    //글별 댓글 목록
    @Query("select c from Comment c join fetch c.author where c.post.id = :postId order by c.id asc")
    List<Comment> findAllByPostIdWithAuthor(@Param("postId") Long postId);

    // 수정 및 삭제 - 해당 글 댓글인지 확인
    @Query("select c from Comment c join fetch c.author where c.id = :commentId and c.post.id = :postId")
    Optional<Comment> findByIdAndPostIdWithAuthor(@Param("commentId") Long commentId,
                                                  @Param("postId") Long postId);

}