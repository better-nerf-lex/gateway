package com.bntech.lexai.gateway.data.entity.document;

import lombok.*;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "event_participants")
public class EventParticipant {
    @Column("event_id")
    private UUID eventId;

    @Column("legal_entity_id")
    private UUID legalEntityId;
}
