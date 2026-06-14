package demo.project.controller;

import demo.project.common.enums.Role;
import demo.project.dto.request.UserUpsertRequest;
import demo.project.dto.response.PageResponse;
import demo.project.dto.response.UserResponse;
import demo.project.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {
    @Mock
    private UserService userService;

    @InjectMocks
    private AdminUserController controller;

    private MockMvc mockMvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setValidator(validator)
            .build();
    }

    @AfterEach
    void tearDown() {
        validator.close();
    }

    @Test
    void searchShouldReturnPageResponse() throws Exception {
        UserResponse user = UserResponse.builder().id(1L).username("admin.root").fullName("Admin Root")
            .email("admin.root@badminton.local").phoneNumber("0901100001").role(Role.ROLE_ADMIN).enabled(true)
            .build();

        PageResponse<UserResponse> pageResponse = PageResponse.<UserResponse>builder().content(List.of(user))
            .page(0).size(10).totalElements(1).totalPages(1).build();

        when(userService.searchUsers("admin", 0, 10)).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/admin/users").param("keyword", "admin")
                .param("page", "0").param("size", "10")).andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content[0].username").value("admin.root"));

        verify(userService).searchUsers("admin", 0, 10);
    }

    @Test
    void createShouldReturnCreated() throws Exception {
        UserResponse response = UserResponse.builder().id(10L).username("customer.new").fullName("customer.new")
            .email("customer.new@badminton.local").phoneNumber("0909999999").role(Role.ROLE_CUSTOMER)
            .enabled(true).build();

        when(userService.create(any(UserUpsertRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/users").contentType(MediaType.APPLICATION_JSON)
                .content(validRequestJson())).andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.username").value("customer.new"));

        verify(userService).create(any(UserUpsertRequest.class));
    }

    @Test
    void updateShouldReturnOk() throws Exception {
        UserResponse response = UserResponse.builder().id(5L).username("manager.updated")
            .fullName("manager.updated").email("manager.updated@badminton.local").phoneNumber("0902111222")
            .role(Role.ROLE_MANAGER).enabled(true).build();

        when(userService.update(eq(5L), any(UserUpsertRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/admin/users/{id}", 5L)
                .contentType(MediaType.APPLICATION_JSON).content(validRequestJson()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.email").value("manager.updated@badminton.local"));

        verify(userService).update(eq(5L), any(UserUpsertRequest.class));
    }

    @Test
    void deleteShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/users/{id}", 99L))
            .andExpect(status().isNoContent());

        verify(userService).delete(99L);
    }

    @Test
    void createShouldReturnBadRequestWhenPayloadInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users").contentType(MediaType.APPLICATION_JSON)
            .content(invalidRequestJson())).andExpect(status().isBadRequest());

        verify(userService, never()).create(any(UserUpsertRequest.class));
        verifyNoInteractions(userService);
    }

    private static String validRequestJson() {
        return """
            {
              "username": "customer.new",
              "email": "customer.new@badminton.local",
              "password": "CustN3w#2026",
              "role": "ROLE_CUSTOMER",
              "enabled": true
            }
            """;
    }

    private static String invalidRequestJson() {
        return """
            {
              "username": "abc",
              "email": "wrong-email",
              "password": "123",
              "role": "ROLE_CUSTOMER",
              "enabled": true
            }
            """;
    }
}


