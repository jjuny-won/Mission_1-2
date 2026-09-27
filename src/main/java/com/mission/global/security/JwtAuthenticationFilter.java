package com.mission.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

//Component 가 붙으면 블릿 필터로 자동 등록 -> 필터 두번 등록됨
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    //요청 하나당 딱 한 번만 실행되도록 보장하는 Spring의 필터 부모 클래스

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws  IOException, ServletException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if(header != null && header.startsWith("Bearer "))
        {
            String token = header.substring(7);
            Long memberId = jwtProvider.getMemberId(token);

            if(memberId!=null){
                var authentication =
                        new UsernamePasswordAuthenticationToken(memberId, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication); //요청 기록
            }
        }
        chain.doFilter(request, response);
    }
}
