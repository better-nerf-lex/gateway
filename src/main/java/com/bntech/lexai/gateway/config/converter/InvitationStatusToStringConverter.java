package com.bntech.lexai.gateway.config.converter;

import com.bntech.lexai.gateway.data.object.InvitationStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.lang.NonNull;

@WritingConverter
public class InvitationStatusToStringConverter implements Converter<InvitationStatus, String> {

    @Override
    public String convert(@NonNull InvitationStatus source) {
        return source.name();
    }
}
