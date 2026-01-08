package com.bntech.lexai.gateway.config.converter;

import com.bntech.lexai.gateway.data.object.CasePartyRole;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class StringToCasePartyRoleConverter implements Converter<String, CasePartyRole> {

    @Override
    public CasePartyRole convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        for (CasePartyRole role : CasePartyRole.values()) {
            if (role.getValue().equalsIgnoreCase(source) || role.name().equalsIgnoreCase(source)) {
                return role;
            }
        }
        return null;
    }
}
