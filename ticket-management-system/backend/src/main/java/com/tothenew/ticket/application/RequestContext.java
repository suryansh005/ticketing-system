package com.tothenew.ticket.application;

import java.util.UUID;

/**
 * Supplies the authenticated principal id. Replaced by Spring Security in a later phase.
 */
public interface RequestContext {

    UUID currentUserId();
}
