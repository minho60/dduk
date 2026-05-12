package com.dduk.controller.admin;

import com.dduk.dto.admin.MemberCreateRequestDto;
import com.dduk.dto.admin.MemberResponseDto;
import com.dduk.dto.admin.MemberRoleUpdateRequestDto;
import com.dduk.dto.admin.MemberStatusUpdateRequestDto;
import com.dduk.service.admin.AdminMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {

    private final AdminMemberService adminMemberService;

    @GetMapping
    public List<MemberResponseDto> getMembers() {
        return adminMemberService.getMembers();
    }

    @PostMapping
    public MemberResponseDto createMember(@RequestBody MemberCreateRequestDto requestDto) {
        return adminMemberService.createMember(requestDto);
    }

    @PatchMapping("/{memberId}/role")
    public MemberResponseDto updateRole(@PathVariable Long memberId, @RequestBody MemberRoleUpdateRequestDto requestDto) {
        return adminMemberService.updateRole(memberId, requestDto.getRole());
    }

    @PatchMapping("/{memberId}/status")
    public MemberResponseDto updateStatus(@PathVariable Long memberId, @RequestBody MemberStatusUpdateRequestDto requestDto) {
        return adminMemberService.updateStatus(memberId, requestDto.isActive());
    }
}
