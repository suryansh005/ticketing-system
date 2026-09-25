package com.tothenew.ticket.api.dto;

import com.tothenew.ticket.domain.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateTicketRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String description,
        @NotNull TicketPriority priority,
        UUID assigneeId) {}
