package com.tothenew.ticket.api;

import com.tothenew.ticket.api.dto.CreateTicketRequest;
import com.tothenew.ticket.api.dto.PageResponse;
import com.tothenew.ticket.api.dto.TicketResponse;
import com.tothenew.ticket.api.dto.TicketUpdateRequest;
import com.tothenew.ticket.api.dto.TransitionRequest;
import com.tothenew.ticket.application.TicketService;
import com.tothenew.ticket.domain.Ticket;
import com.tothenew.ticket.domain.TicketStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tickets")
@Validated
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody CreateTicketRequest request) {
        Ticket created =
                ticketService.create(
                        request.title(),
                        request.description(),
                        request.priority(),
                        request.assigneeId());
        URI location = URI.create("/api/v1/tickets/" + created.getId());
        return ResponseEntity.created(location).body(TicketResponse.from(created));
    }

    @GetMapping("/{ticketId}")
    public TicketResponse getById(@PathVariable UUID ticketId) {
        return TicketResponse.from(ticketService.getById(ticketId));
    }

    @GetMapping
    public PageResponse<TicketResponse> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) String search) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<TicketResponse> result =
                ticketService.list(pageable, status, search).map(TicketResponse::from);
        return PageResponse.from(result);
    }

    @PatchMapping("/{ticketId}")
    public TicketResponse update(
            @PathVariable UUID ticketId, @Valid @RequestBody TicketUpdateRequest request) {
        return TicketResponse.from(
                ticketService.update(
                        ticketId,
                        request.title(),
                        request.description(),
                        request.priority(),
                        request.assigneeId()));
    }

    @PatchMapping("/{ticketId}/status")
    public TicketResponse applyTransition(
            @PathVariable UUID ticketId, @Valid @RequestBody TransitionRequest request) {
        return TicketResponse.from(ticketService.applyTransition(ticketId, request.event()));
    }
}
