package com.mission.domain.auth.service;

import com.mission.domain.auth.dto.LoginRequest;
import com.mission.domain.auth.dto.TokenResponse;
import com.mission.domain.member.entity.Member;
import com.mission.domain.member.repository.MemberRepository;
import com.mission.global.exception.BusinessException;
import com.mission.global.exception.ErrorCode;
import com.mission.global.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public TokenResponse login(LoginRequest request){
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        String token = jwtProvider.createAccessToken(member.getId());
        return TokenResponse.bearer(token);
    }
}
