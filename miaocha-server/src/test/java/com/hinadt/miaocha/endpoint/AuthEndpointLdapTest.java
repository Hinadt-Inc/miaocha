package com.hinadt.miaocha.endpoint;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hinadt.miaocha.config.LdapProperties;
import com.hinadt.miaocha.domain.dto.auth.LoginRequestDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** LDAP认证功能测试 */
@SpringBootTest
@ActiveProfiles("integration-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {"ldap.enabled=true"})
public class AuthEndpointLdapTest {

    @Autowired private WebApplicationContext webApplicationContext;

    @Autowired private ObjectMapper objectMapper;

    @MockBean private LdapProperties ldapProperties;

    private MockMvc mockMvc;

    @Test
    public void testGetAuthProviders_shouldIncludeLdapProvider() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mockMvc.perform(get("/api/auth/providers"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[?(@.providerId == 'ldap')]").exists())
                .andExpect(
                        jsonPath("$.data[?(@.providerId == 'ldap')].displayName").value("企业LDAP"));
    }

    @Test
    public void testLdapLogin_withValidCredentials_shouldSucceed() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setEmail("test@company.com");
        loginRequest.setPassword("password");
        loginRequest.setProviderId("ldap");

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.loginType").value("ldap"));
    }

    @Test
    public void testLdapLogin_withInvalidCredentials_shouldFail() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setEmail("test@company.com");
        loginRequest.setPassword("wrongpassword");
        loginRequest.setProviderId("ldap");

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    public void testSystemLogin_shouldStillWork() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setEmail("existing@example.com"); // 需要先在数据库中创建这个用户
        loginRequest.setPassword("password");
        // providerId为空，应该使用系统默认认证

        // 这个测试需要根据你的实际数据调整
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
