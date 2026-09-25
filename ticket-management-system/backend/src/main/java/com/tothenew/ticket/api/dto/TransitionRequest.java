package com.tothenew.ticket.api.dto;

import com.tothenew.ticket.domain.TransitionEvent;
import jakarta.validation.constraints.NotNull;

public record TransitionRequest(@NotNull TransitionEvent event) {}
