package com.bntech.lexai.gateway.rest.controller;

import com.bntech.lexai.gateway.rest.dto.document.EventDto;
import com.bntech.lexai.gateway.rest.dto.document.details.PersonDetailsDto;
import com.bntech.lexai.gateway.data.object.EventType;
import com.bntech.lexai.gateway.data.object.EntityType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class EventDeserializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    void testDeserializeEventWithParticipants() throws Exception {
        String jsonPayload = """
            {
              "caseId": "650e8400-e29b-41d4-a716-446655440001",
              "type": "court",
              "status": true,
              "eventDate": "2026-01-02T17:12:44",
              "details": "Sample event with participants",
              "participants": [
                {
                  "id": "850e8400-e29b-41d4-a716-446655440001",
                  "name": "John Doe",
                  "relevance": "HIGH",
                  "details": {
                    "entityType": "INDIVIDUAL",
                    "firstName": "John",
                    "lastName": "Doe",
                    "email": "john.doe@example.com"
                  }
                }
              ]
            }
            """;

        EventDto event = objectMapper.readValue(jsonPayload, EventDto.class);

        assertThat(event.getCaseId()).isEqualTo("650e8400-e29b-41d4-a716-446655440001");
        assertThat(event.getType()).isEqualTo(EventType.COURT);
        assertThat(event.isStatus()).isTrue();
        assertThat(event.getEventDate()).isEqualTo(LocalDateTime.of(2026, 1, 2, 17, 12, 44));
        assertThat(event.getParticipants()).hasSize(1);
        assertThat(event.getParticipants().get(0).getDetails()).isInstanceOf(PersonDetailsDto.class);
        assertThat(((PersonDetailsDto)event.getParticipants().get(0).getDetails()).getFirstName()).isEqualTo("John");
    }

    @Test
    void testDeserializeEventWithMixedCaseEnums() throws Exception {
        String jsonPayload = """
            {
              "caseId": "650e8400-e29b-41d4-a716-446655440001",
              "type": "COURT",
              "status": true,
              "eventDate": "2026-01-02T17:12:44",
              "participants": [
                {
                  "details": {
                    "entityType": "individual"
                  }
                }
              ]
            }
            """;

        EventDto event = objectMapper.readValue(jsonPayload, EventDto.class);
        assertThat(event.getType()).isEqualTo(EventType.COURT);
        assertThat(event.getParticipants().get(0).getDetails().getEntityType()).isEqualTo(EntityType.INDIVIDUAL);
    }

    @Test
    void testDeserializeEventWithFullPayload() throws Exception {
        String jsonPayload = """
            {
              "id": "950e8400-e29b-41d4-a716-446655440001",
              "caseId": "650e8400-e29b-41d4-a716-446655440001",
              "type": "court",
              "status": true,
              "eventDate": "2026-01-02T17:12:44",
              "details": "Full payload test",
              "createdAt": "2026-01-02T17:12:44",
              "teamId": "550e8400-e29b-41d4-a716-446655440100",
              "ownerId": 1,
              "participants": [
                {
                  "id": "850e8400-e29b-41d4-a716-446655440001",
                  "name": "Acme Corp",
                  "relevance": "HIGH",
                  "details": {
                    "entityType": "COMPANY",
                    "fullName": "Acme Corp Ltd",
                    "industry": "Tech",
                    "streetAddress": "123 Tech Lane"
                  }
                }
              ]
            }
            """;

        EventDto event = objectMapper.readValue(jsonPayload, EventDto.class);
        assertThat(event.getId()).isEqualTo("950e8400-e29b-41d4-a716-446655440001");
        assertThat(event.getOwnerId()).isEqualTo(1L);
        assertThat(event.getTeamId().toString()).isEqualTo("550e8400-e29b-41d4-a716-446655440100");
    }

    @Test
    void testDeserializeEventWithVaryingDatePrecision() throws Exception {
        // Test with milliseconds
        String jsonWithMillis = """
            {
              "eventDate": "2026-01-02T17:12:44.123",
              "type": "court"
            }
            """;
        EventDto event1 = objectMapper.readValue(jsonWithMillis, EventDto.class);
        assertThat(event1.getEventDate()).isEqualTo(LocalDateTime.of(2026, 1, 2, 17, 12, 44, 123000000));

        // Test without milliseconds
        String jsonWithoutMillis = """
            {
              "eventDate": "2026-01-02T17:12:44",
              "type": "court"
            }
            """;
        EventDto event2 = objectMapper.readValue(jsonWithoutMillis, EventDto.class);
        assertThat(event2.getEventDate()).isEqualTo(LocalDateTime.of(2026, 1, 2, 17, 12, 44));
    }
}
