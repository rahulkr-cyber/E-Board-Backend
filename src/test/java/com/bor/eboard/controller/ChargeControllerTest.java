package com.bor.eboard.controller;

import com.bor.eboard.charge.controller.ChargeController;
import com.bor.eboard.charge.dto.ChargeResponse;
import com.bor.eboard.charge.dto.CreateChargeRequest;
import com.bor.eboard.charge.service.ChargeService;
import com.bor.eboard.common.exception.ValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ChargeController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
        })
@Import(TestMethodSecurityConfig.class)
@DisplayName("ChargeController")
class ChargeControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private ChargeService chargeService;

    private CreateChargeRequest request() {
        CreateChargeRequest req = new CreateChargeRequest();
        req.setFromUserId(UUID.randomUUID());
        req.setToUserId(UUID.randomUUID());
        req.setChargeType("TEMPORARY");
        req.setEffectiveFrom(LocalDateTime.now());
        return req;
    }

    @Test
    @WithMockUser(authorities = "CHARGE_MANAGE")
    @DisplayName("creates a charge and returns success for an authorized user")
    void createSucceeds() throws Exception {
        when(chargeService.create(any()))
                .thenReturn(ChargeResponse.builder().status("ACTIVE").build());

        mockMvc.perform(post("/api/v1/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    @DisplayName("forbids creation for a user without CHARGE_MANAGE")
    void createForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "CHARGE_MANAGE")
    @DisplayName("returns 400 when the service rejects the request")
    void createValidationError() throws Exception {
        when(chargeService.create(any()))
                .thenThrow(new ValidationException("Invalid charge type: BOGUS"));

        mockMvc.perform(post("/api/v1/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(authorities = "POSTING_VIEW")
    @DisplayName("lists active charges for a viewer")
    void listActive() throws Exception {
        when(chargeService.getActive()).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/v1/charge/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
