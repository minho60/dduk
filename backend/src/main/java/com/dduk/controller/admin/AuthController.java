package com.dduk.controller.admin;

import com.dduk.config.PrincipalDetails;
import com.dduk.dto.admin.AuthMeResponseDto;
import com.dduk.dto.admin.LoginRequestDto;
import com.dduk.dto.admin.LoginResponseDto;
import com.dduk.service.admin.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponseDto login(@RequestBody LoginRequestDto requestDto) {
        return authService.login(requestDto);
    }

    @GetMapping("/me")
    public AuthMeResponseDto me(@AuthenticationPrincipal PrincipalDetails principalDetails) {
        return authService.getCurrentMember(principalDetails);
    }
}
