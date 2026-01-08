package com.bntech.lexai.gateway.config.converter;

import com.bntech.lexai.gateway.data.object.ActionType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class StringToActionTypeConverter implements Converter<String, ActionType> {

    @Override
    public ActionType convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        for (ActionType type : ActionType.values()) {
            if (type.getValue().equalsIgnoreCase(source) || type.name().equalsIgnoreCase(source)) {
                return type;
            }
        }
        return null;
    }
}
