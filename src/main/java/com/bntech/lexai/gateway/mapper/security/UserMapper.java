package com.bntech.lexai.gateway.mapper.security;

import com.bntech.lexai.gateway.data.entity.security.User;
import com.bntech.lexai.gateway.rest.dto.security.UserDto;
import com.bntech.lexai.gateway.security.UserPrincipal;
import org.mapstruct.Mapper;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {
    String COGNITO_USER_PASSWORD_PLACEHOLDER = "{noop}cognito-authenticated-user";

    UserDto toDto(User user);

    default UserPrincipal toUserPrincipal(User user, List<? extends GrantedAuthority> authorities) {
        return new UserPrincipal(
                user.getId(),
                user.getUsername(),
                principalPassword(user),
                user.isEnabled(),
                true,
                true,
                true,
                authorities);
    }

    private String principalPassword(User user) {
        return user.getPassword() == null ? COGNITO_USER_PASSWORD_PLACEHOLDER : user.getPassword();
    }
}
