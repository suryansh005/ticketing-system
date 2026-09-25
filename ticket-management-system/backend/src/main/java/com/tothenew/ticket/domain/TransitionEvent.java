package com.tothenew.ticket.domain;

/**
 * Lifecycle commands for ticket status changes. Clients send these via the transitions API;
 * direct status assignment is not supported.
 */
public enum TransitionEvent {
    START_PROGRESS,
    RESOLVE,
    CLOSE,
    CANCEL
}
