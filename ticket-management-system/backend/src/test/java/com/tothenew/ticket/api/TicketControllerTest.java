package com.tothenew.ticket.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tothenew.ticket.application.TicketService;
import com.tothenew.ticket.domain.Ticket;
import com.tothenew.ticket.domain.TicketPriority;
import com.tothenew.ticket.domain.TicketStatus;
import com.tothenew.ticket.domain.TransitionEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TicketController.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    @Test
    void createReturns201WithLocation() throws Exception {
        UUID id = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
        Instant now = Instant.parse("2026-09-24T10:57:00Z");
        Ticket ticket =
                new Ticket(
                        id,
                        "VPN drops",
                        "Hourly disconnect",
                        TicketPriority.MEDIUM,
                        TicketStatus.OPEN,
                        UUID.fromString("00000000-0000-4000-8000-000000000001"),
                        null,
                        0L,
                        now,
                        now);
        when(ticketService.create(any(), any(), any(), any())).thenReturn(ticket);

        mockMvc.perform(
                        post("/api/v1/tickets")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "title": "VPN drops",
                                          "description": "Hourly disconnect",
                                          "priority": "MEDIUM"
                                        }
                                        """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/tickets/" + id))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void createRejectsBlankTitle() throws Exception {
        mockMvc.perform(
                        post("/api/v1/tickets")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "title": "",
                                          "description": "x",
                                          "priority": "LOW"
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("title"));
    }

    @Test
    void updateDelegatesToService() throws Exception {
        UUID id = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
        UUID assignee = UUID.fromString("00000000-0000-4000-8000-000000000002");
        Instant now = Instant.parse("2026-09-24T10:57:00Z");
        Ticket updated =
                new Ticket(
                        id,
                        "Updated title",
                        "Updated description",
                        TicketPriority.HIGH,
                        TicketStatus.OPEN,
                        UUID.fromString("00000000-0000-4000-8000-000000000001"),
                        assignee,
                        1L,
                        now,
                        now);
        when(ticketService.update(eq(id), any(), any(), any(), any())).thenReturn(updated);

        mockMvc.perform(
                        patch("/api/v1/tickets/{id}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "title": "Updated title",
                                          "description": "Updated description",
                                          "priority": "HIGH",
                                          "assigneeId": "00000000-0000-4000-8000-000000000002"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated title"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.version").value(1));

        verify(ticketService)
                .update(
                        id,
                        "Updated title",
                        "Updated description",
                        TicketPriority.HIGH,
                        assignee);
    }

    @Test
    void transitionDelegatesToService() throws Exception {
        UUID id = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
        Instant now = Instant.parse("2026-09-24T10:57:00Z");
        Ticket updated =
                new Ticket(
                        id,
                        "VPN drops",
                        "Hourly disconnect",
                        TicketPriority.MEDIUM,
                        TicketStatus.IN_PROGRESS,
                        UUID.fromString("00000000-0000-4000-8000-000000000001"),
                        null,
                        1L,
                        now,
                        now);
        when(ticketService.applyTransition(eq(id), eq(TransitionEvent.START_PROGRESS)))
                .thenReturn(updated);

        mockMvc.perform(
                        patch("/api/v1/tickets/{id}/status", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"event\": \"START_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        verify(ticketService).applyTransition(id, TransitionEvent.START_PROGRESS);
    }

    @Test
    void listReturnsPaginationEnvelope() throws Exception {
        when(ticketService.list(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void listPassesStatusAndSearchToService() throws Exception {
        when(ticketService.list(any(), eq(TicketStatus.OPEN), eq("vpn")))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/tickets").param("status", "OPEN").param("search", "vpn"))
                .andExpect(status().isOk());

        verify(ticketService).list(any(), eq(TicketStatus.OPEN), eq("vpn"));
    }
}
