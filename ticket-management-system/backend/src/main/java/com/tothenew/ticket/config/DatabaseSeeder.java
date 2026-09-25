package com.tothenew.ticket.config;

import com.tothenew.ticket.infrastructure.persistence.UserEntity;
import com.tothenew.ticket.infrastructure.persistence.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    static final UUID ALICE_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    static final UUID BOB_ID = UUID.fromString("00000000-0000-4000-8000-000000000002");
    static final UUID CAROL_ID = UUID.fromString("00000000-0000-4000-8000-000000000003");
    static final UUID DAVE_ID = UUID.fromString("00000000-0000-4000-8000-000000000004");

    private final UserRepository userRepository;

    public DatabaseSeeder(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        userRepository.saveAll(
                List.of(
                        UserEntity.builder()
                                .id(ALICE_ID)
                                .name("Alice Smith")
                                .email("alice.smith@example.com")
                                .build(),
                        UserEntity.builder()
                                .id(BOB_ID)
                                .name("Bob Jones")
                                .email("bob.jones@example.com")
                                .build(),
                        UserEntity.builder()
                                .id(CAROL_ID)
                                .name("Carol Nguyen")
                                .email("carol.nguyen@example.com")
                                .build(),
                        UserEntity.builder()
                                .id(DAVE_ID)
                                .name("Dave Patel")
                                .email("dave.patel@example.com")
                                .build()));
    }
}
