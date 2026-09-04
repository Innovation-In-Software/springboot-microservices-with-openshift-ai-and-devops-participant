package com.md287.risk.api;

import com.md287.risk.api.exception.GlobalExceptionHandler;
import com.md287.risk.config.CorrelationIdFilter;
import com.md287.risk.config.SecurityConfig;
import com.md287.risk.service.AssessmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AssessmentController.class)
@Import({GlobalExceptionHandler.class, CorrelationIdFilter.class, SecurityConfig.class})
@TestPropertySource(properties = {
        "md287.jwt.issuer=md287-lab",
        "md287.jwt.secret=md287-lab-only-hmac-secret-32bytes!"
})
class AssessmentSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssessmentService assessmentService;

    @Test
    void getWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/assessments/TXN-AABBCCDD"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void reviewWithReadScopeReturns403() throws Exception {
        mockMvc.perform(post("/api/v1/assessments/TXN-AABBCCDD/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_risk.read")))
                        .content("""
                                {"decision":"APPROVE","reason":"false positive"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }
}
