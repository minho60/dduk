package com.dduk.dto.admin;

import com.dduk.entity.admin.Role;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemberCreateRequestDto {
    private String loginId;
    private String password;
    private String name;
    private Role role;
}
