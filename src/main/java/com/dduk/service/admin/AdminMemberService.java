package com.dduk.service.admin;

import com.dduk.dto.admin.MemberCreateRequestDto;
import com.dduk.dto.admin.MemberResponseDto;
import com.dduk.entity.admin.Member;
import com.dduk.entity.admin.Role;
import com.dduk.repository.admin.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public List<MemberResponseDto> getMembers() {
        return memberRepository.findAllByOrderByIdDesc().stream()
                .map(MemberResponseDto::from)
                .toList();
    }

    @Transactional
    public MemberResponseDto createMember(MemberCreateRequestDto requestDto) {
        validateCreateRequest(requestDto);

        if (memberRepository.findByLoginId(requestDto.getLoginId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다.");
        }

        Member member = Member.builder()
                .loginId(requestDto.getLoginId().trim())
                .password(passwordEncoder.encode(requestDto.getPassword()))
                .name(requestDto.getName().trim())
                .role(requestDto.getRole())
                .active(true)
                .build();

        return MemberResponseDto.from(memberRepository.save(member));
    }

    @Transactional
    public MemberResponseDto updateRole(Long memberId, Role role) {
        if (role == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "변경할 권한이 필요합니다.");
        }

        Member member = getMember(memberId);
        member.updateRole(role);
        return MemberResponseDto.from(member);
    }

    @Transactional
    public MemberResponseDto updateStatus(Long memberId, boolean active) {
        Member member = getMember(memberId);
        member.updateActive(active);
        return MemberResponseDto.from(member);
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "계정을 찾을 수 없습니다."));
    }

    private void validateCreateRequest(MemberCreateRequestDto requestDto) {
        if (requestDto.getLoginId() == null || requestDto.getLoginId().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "아이디를 입력하세요.");
        }
        if (requestDto.getPassword() == null || requestDto.getPassword().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "비밀번호를 입력하세요.");
        }
        if (requestDto.getName() == null || requestDto.getName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이름을 입력하세요.");
        }
        if (requestDto.getRole() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "권한을 선택하세요.");
        }
    }
}
