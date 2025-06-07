package com.bntech.lexai.gateway.mapper;

import com.bntech.lexai.gateway.data.entity.security.User;
import com.bntech.lexai.gateway.rest.dto.security.UserDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toDto(User user);
}
