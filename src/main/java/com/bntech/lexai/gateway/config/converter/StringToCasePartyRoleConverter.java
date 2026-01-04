package com.bntech.lexai.gateway.config.converter;

import com.bntech.lexai.gateway.data.object.CasePartyRole;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToCasePartyRoleConverter implements Converter<String, CasePartyRole> {

    @Override
    public CasePartyRole convert(String source) {
        return CasePartyRole.fromValue(source);
    }
}
