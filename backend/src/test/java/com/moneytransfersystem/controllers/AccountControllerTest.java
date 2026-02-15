package com.moneytransfersystem.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneytransfersystem.domain.entities.Account;
import com.moneytransfersystem.domain.enums.AccountStatus;
import com.moneytransfersystem.exception.GlobalExceptionHandler;
import com.moneytransfersystem.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountController Unit Tests")
class AccountControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private AccountController accountController;

    private ObjectMapper objectMapper;
    private Account testAccount;
    private String testAccountId;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
                .standaloneSetup(accountController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        this.objectMapper = new ObjectMapper();

        testAccountId = UUID.randomUUID().toString();
        testAccount = Account.builder()
                .id(testAccountId)
                .holderName("John Doe")
                .balance(BigDecimal.valueOf(1000.00))
                .status(AccountStatus.ACTIVE)
                .password("encodedPassword")
                .build();
    }

    @Test
    @DisplayName("Should get account by ID successfully")
    void testGetAccount_Success() throws Exception {
        when(accountService.findById(testAccountId))
                .thenReturn(Optional.of(testAccount));

        mockMvc.perform(get("/api/accounts/{id}", testAccountId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(testAccountId)))
                .andExpect(jsonPath("$.holderName", equalTo("John Doe")))
                .andExpect(jsonPath("$.balance", equalTo(1000.00)))
                .andExpect(jsonPath("$.status", equalTo("ACTIVE")));

        verify(accountService, times(1)).findById(testAccountId);
    }

    @Test
    @DisplayName("Should return 404 when account not found")
    void testGetAccount_NotFound() throws Exception {
        String nonExistentId = UUID.randomUUID().toString();
        when(accountService.findById(nonExistentId))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/accounts/{id}", nonExistentId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(accountService, times(1)).findById(nonExistentId);
    }

    @Test
    @DisplayName("Should create account with valid payload")
    void testCreateAccount_Success() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("holderName", "Jane Smith");
        payload.put("password", "password123");
        payload.put("balance", 5000.00);

        when(accountService.createAccount("Jane Smith", BigDecimal.valueOf(5000.00), "password123"))
                .thenReturn(testAccount);

        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(testAccountId)))
                .andExpect(jsonPath("$.holderName", equalTo("John Doe")))
                .andExpect(jsonPath("$.balance", equalTo(1000.00)));

        verify(accountService, times(1)).createAccount("Jane Smith", BigDecimal.valueOf(5000.00), "password123");
    }

    @Test
    @DisplayName("Should create account with Integer balance")
    void testCreateAccount_IntegerBalance() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("holderName", "Test User");
        payload.put("password", "password123");
        payload.put("balance", 1000); // Integer instead of Double

        when(accountService.createAccount("Test User", BigDecimal.valueOf(1000), "password123"))
                .thenReturn(testAccount);

        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        verify(accountService, times(1)).createAccount("Test User", BigDecimal.valueOf(1000), "password123");
    }

    @Test
    @DisplayName("Should create account with Double balance")
    void testCreateAccount_DoubleBalance() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("holderName", "Test User");
        payload.put("password", "password123");
        payload.put("balance", 1500.50);

        when(accountService.createAccount("Test User", BigDecimal.valueOf(1500.50), "password123"))
                .thenReturn(testAccount);

        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        verify(accountService, times(1)).createAccount("Test User", BigDecimal.valueOf(1500.50), "password123");
    }

    @Test
    @DisplayName("Should login with correct credentials successfully")
    void testLogin_Success() throws Exception {
        Map<String, String> payload = new HashMap<>();
        payload.put("accountId", testAccountId);
        payload.put("password", "correctPassword");

        when(accountService.authenticate(testAccountId, "correctPassword"))
                .thenReturn(true);

        mockMvc.perform(post("/api/accounts/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", equalTo("Login successful")))
                .andExpect(jsonPath("$.accountId", equalTo(testAccountId)));

        verify(accountService, times(1)).authenticate(testAccountId, "correctPassword");
    }

    @Test
    @DisplayName("Should return 401 with incorrect credentials")
    void testLogin_Failure() throws Exception {
        Map<String, String> payload = new HashMap<>();
        payload.put("accountId", testAccountId);
        payload.put("password", "wrongPassword");

        when(accountService.authenticate(testAccountId, "wrongPassword"))
                .thenReturn(false);

        mockMvc.perform(post("/api/accounts/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", equalTo("Invalid credentials")));

        verify(accountService, times(1)).authenticate(testAccountId, "wrongPassword");
    }

    @Test
    @DisplayName("Should handle missing accountId in login")
    void testLogin_MissingAccountId() throws Exception {
        Map<String, String> payload = new HashMap<>();
        payload.put("password", "password123");

        mockMvc.perform(post("/api/accounts/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Should handle missing password in login")
    void testLogin_MissingPassword() throws Exception {
        Map<String, String> payload = new HashMap<>();
        payload.put("accountId", testAccountId);

        mockMvc.perform(post("/api/accounts/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Should return OK status code for successful create account")
    void testCreateAccount_StatusOk() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("holderName", "New Account");
        payload.put("password", "password");
        payload.put("balance", 2000.00);

        when(accountService.createAccount(anyString(), org.mockito.ArgumentMatchers.<BigDecimal>any(), anyString()))
                .thenReturn(testAccount);

        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return account data with correct data types")
    void testGetAccount_DataTypes() throws Exception {
        when(accountService.findById(testAccountId))
                .thenReturn(Optional.of(testAccount));

        mockMvc.perform(get("/api/accounts/{id}", testAccountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.holderName").exists())
                .andExpect(jsonPath("$.balance").isNumber())
                .andExpect(jsonPath("$.status").exists());
    }
}
