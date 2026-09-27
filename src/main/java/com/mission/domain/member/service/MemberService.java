package com.mission.domain.member.service;

import com.mission.domain.member.dto.MemberResponse;
import com.mission.domain.member.dto.SignupRequest;
import com.mission.domain.member.entity.Member;
import com.mission.domain.member.repository.MemberRepository;
import com.mission.global.exception.BusinessException;
import com.mission.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public MemberResponse signup(SignupRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        Member member = new Member(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.nickname()
        );
        memberRepository.save(member);

        return MemberResponse.from(member);
    }
}