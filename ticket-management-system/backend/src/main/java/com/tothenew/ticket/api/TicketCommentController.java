package com.tothenew.ticket.api;

import com.tothenew.ticket.api.dto.CommentResponse;
import com.tothenew.ticket.api.dto.CreateCommentRequest;
import com.tothenew.ticket.api.dto.PageResponse;
import com.tothenew.ticket.application.CommentService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tickets/{ticketId}/comments")
@Validated
public class TicketCommentController {

    private final CommentService commentService;

    public TicketCommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public PageResponse<CommentResponse> list(
            @PathVariable UUID ticketId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        PageRequest pageable =
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<CommentResponse> result = commentService.list(ticketId, pageable);
        return PageResponse.from(result);
    }

    @PostMapping
    public ResponseEntity<CommentResponse> create(
            @PathVariable UUID ticketId, @Valid @RequestBody CreateCommentRequest request) {
        CommentResponse created = commentService.add(ticketId, request.body());
        URI location =
                URI.create("/api/v1/tickets/" + ticketId + "/comments/" + created.id());
        return ResponseEntity.created(location).body(created);
    }
}
