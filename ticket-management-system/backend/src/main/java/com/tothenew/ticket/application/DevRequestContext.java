package com.tothenew.ticket.application;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DevRequestContext implements RequestContext {

    private static final UUID DEV_REQUESTER_ID =
            UUID.fromString("00000000-0000-4000-8000-000000000001");

    @Override
    public UUID currentUserId() {
        return DEV_REQUESTER_ID;
    }
}
