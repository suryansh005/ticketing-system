package com.tothenew.ticket.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tothenew.ticket.api.dto.CommentResponse;
import com.tothenew.ticket.application.CommentService;
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

@WebMvcTest(TicketCommentController.class)
class TicketCommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommentService commentService;

    @Test
    void listReturnsPaginationEnvelope() throws Exception {
        UUID ticketId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
        when(commentService.list(eq(ticketId), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));

        mockMvc.perform(get("/api/v1/tickets/{ticketId}/comments", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(50));

        verify(commentService).list(eq(ticketId), any());
    }

    @Test
    void createReturns201WithLocation() throws Exception {
        UUID ticketId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
        UUID commentId = UUID.fromString("4fa85f64-5717-4562-b3fc-2c963f66afa7");
        UUID authorId = UUID.fromString("00000000-0000-4000-8000-000000000001");
        Instant now = Instant.parse("2026-09-24T10:57:00Z");
        CommentResponse created =
                new CommentResponse(commentId, ticketId, authorId, "Looks good", now);
        when(commentService.add(eq(ticketId), eq("Looks good"))).thenReturn(created);

        mockMvc.perform(
                        post("/api/v1/tickets/{ticketId}/comments", ticketId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"body\": \"Looks good\"}"))
                .andExpect(status().isCreated())
                .andExpect(
                        header().string(
                                "Location",
                                "/api/v1/tickets/" + ticketId + "/comments/" + commentId))
                .andExpect(jsonPath("$.id").value(commentId.toString()))
                .andExpect(jsonPath("$.body").value("Looks good"));

        verify(commentService).add(ticketId, "Looks good");
    }

    @Test
    void createRejectsBlankBody() throws Exception {
        UUID ticketId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

        mockMvc.perform(
                        post("/api/v1/tickets/{ticketId}/comments", ticketId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"body\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
