package com.tothenew.ticket.application;

import com.tothenew.ticket.api.dto.UserResponse;
import com.tothenew.ticket.infrastructure.persistence.UserRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listAll() {
        return userRepository.findAll(Sort.by("name")).stream().map(UserResponse::from).toList();
    }
}
