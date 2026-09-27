package com.mission.domain.member.entity;

import com.mission.global.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Getter
public class Member extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 30)

    private String nickname;

    public Member(String email, String encodedPassword, String nickname) {
        this.email = email;
        this.password = encodedPassword;
        this.nickname = nickname;
    }
}
