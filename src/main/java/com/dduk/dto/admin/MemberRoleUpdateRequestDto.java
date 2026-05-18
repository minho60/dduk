package com.dduk.dto.admin;

import com.dduk.entity.admin.Role;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemberRoleUpdateRequestDto {
    private Role role;
}
