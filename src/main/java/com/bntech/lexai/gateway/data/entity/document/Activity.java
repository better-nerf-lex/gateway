package com.bntech.lexai.gateway.data.entity.document;

import com.bntech.lexai.gateway.data.entity.BaseEntity;
import com.bntech.lexai.gateway.data.object.ActionType;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "activity")
public class Activity extends BaseEntity<UUID> {

    @Column()
    private ActionType action;

    public boolean isNew() {
        return true;
    }
}
