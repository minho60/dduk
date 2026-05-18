package com.dduk.controller.admin;

import com.dduk.config.PrincipalDetails;
import com.dduk.dto.admin.AdminMemberPageResponseDto;
import com.dduk.dto.admin.MemberCreateRequestDto;
import com.dduk.dto.admin.MemberResponseDto;
import com.dduk.dto.admin.MemberRoleUpdateRequestDto;
import com.dduk.dto.admin.MemberStatusUpdateRequestDto;
import com.dduk.entity.admin.Role;
import com.dduk.service.admin.AdminMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {

    private final AdminMemberService adminMemberService;

    @GetMapping
    public AdminMemberPageResponseDto getMembers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return adminMemberService.getMembers(keyword, role, active, page, size);
    }

    @GetMapping("/{memberId}")
    public MemberResponseDto getMemberDetail(@PathVariable Long memberId) {
        return adminMemberService.getMemberDetail(memberId);
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
    public MemberResponseDto updateStatus(
            @PathVariable Long memberId,
            @RequestBody MemberStatusUpdateRequestDto requestDto,
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        Long actorMemberId = principalDetails != null ? principalDetails.getMember().getId() : null;
        return adminMemberService.updateStatus(memberId, requestDto.isActive(), actorMemberId);
    }
}
