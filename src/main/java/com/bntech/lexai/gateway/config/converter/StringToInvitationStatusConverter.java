package com.bntech.lexai.gateway.config.converter;

import com.bntech.lexai.gateway.data.object.InvitationStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.lang.NonNull;

@ReadingConverter
public class StringToInvitationStatusConverter implements Converter<String, InvitationStatus> {

    @Override
    public InvitationStatus convert(@NonNull String source) {
        try {
            return InvitationStatus.valueOf(source);
        } catch (IllegalArgumentException e) {
            return InvitationStatus.PENDING;
        }
    }
}
