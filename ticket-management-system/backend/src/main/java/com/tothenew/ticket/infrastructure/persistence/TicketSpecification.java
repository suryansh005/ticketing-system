package com.tothenew.ticket.infrastructure.persistence;

import com.tothenew.ticket.domain.TicketStatus;
import org.springframework.data.jpa.domain.Specification;

public final class TicketSpecification {

    private TicketSpecification() {}

    public static Specification<TicketEntity> hasStatus(TicketStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), status);
    }

    public static Specification<TicketEntity> matchesSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.or(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("title")), pattern),
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("description")), pattern));
    }
}
