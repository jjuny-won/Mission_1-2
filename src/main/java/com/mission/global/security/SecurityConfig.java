package com.mission.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mission.global.exception.ErrorCode;
import com.mission.global.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

@EnableWebSecurity
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper; //JSON 변환기 응답 변환 시 사용

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) //csrf 끄기
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable) //Bearer 를 쓰기 때문에 끄기
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) //세션을 만들지 않게함 - 인증 정보 매번 새로 채우기
                .headers(h -> h.frameOptions(f -> f.sameOrigin())) // 같은 출처(우리 서버)의 iframe은 허용
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PathRequest.toH2Console()).permitAll() //H2 Console 주소 허용
                        .requestMatchers("/error").permitAll() // 없는 주소로 요청시
                        .requestMatchers(HttpMethod.POST, "/api/members", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/posts/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> writeError(res, ErrorCode.UNAUTHORIZED))
                        //인증이 필요한 주소인데 인증 정보가 없을 때 호출
                        .accessDeniedHandler((req, res, ex) -> writeError(res, ErrorCode.FORBIDDEN)))
                        //로그인은 했지만 권한(역할)이 부족할 때 호출
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();

    }

    private void writeError(HttpServletResponse res, ErrorCode code) throws IOException {
        res.setStatus(code.getStatus().value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(res.getWriter(), ErrorResponse.of(code));
    }

}
