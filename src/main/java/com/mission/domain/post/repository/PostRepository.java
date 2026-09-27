package com.mission.domain.post.repository;

import com.mission.domain.post.dto.PostListItem;
import com.mission.domain.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("select p from Post p join fetch p.author where p.id = :postId")
    Optional<Post> findWithAuthorById(@Param("postId") Long postId);

    //목록 조회  쿼리
    @Query(value = """
            select new com.mission.domain.post.dto.PostListItem(
                p.id, p.title, m.nickname, count(c.id), p.createdAt)
            from Post p
            join p.author m
            left join Comment c on c.post = p
            group by p.id, p.title, m.nickname, p.createdAt
            order by p.id desc
            """,
            countQuery = "select count(p) from Post p")
    Page<PostListItem> findPostList(Pageable pageable);
}
