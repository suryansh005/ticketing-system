package com.tothenew.ticket.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tothenew.ticket.api.dto.UserResponse;
import com.tothenew.ticket.application.UserService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void listReturnsUsers() throws Exception {
        UUID id = UUID.fromString("00000000-0000-4000-8000-000000000002");
        when(userService.listAll())
                .thenReturn(List.of(new UserResponse(id, "Bob Jones", "bob.jones@example.com")));

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].name").value("Bob Jones"))
                .andExpect(jsonPath("$[0].email").value("bob.jones@example.com"));

        verify(userService).listAll();
    }
}
