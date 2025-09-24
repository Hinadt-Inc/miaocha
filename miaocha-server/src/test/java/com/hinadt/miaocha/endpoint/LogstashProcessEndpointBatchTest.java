package com.hinadt.miaocha.endpoint;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hinadt.miaocha.application.logstash.task.TaskService;
import com.hinadt.miaocha.application.service.LogstashAlertRecipientsService;
import com.hinadt.miaocha.application.service.LogstashProcessService;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Test for batch operations in LogstashProcessEndpoint */
@WebMvcTest(LogstashProcessEndpoint.class)
public class LogstashProcessEndpointBatchTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private LogstashProcessService logstashProcessService;

    @MockBean private LogstashAlertRecipientsService alertRecipientsService;

    @MockBean private TaskService taskService;

    @Autowired private ObjectMapper objectMapper;

    @Test
    public void testBatchStartInstances() throws Exception {
        // Arrange
        List<Long> instanceIds = Arrays.asList(1L, 2L, 3L);

        // Act & Assert
        mockMvc.perform(
                        post("/api/logstash/processes/instances/batch/start")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(instanceIds)))
                .andExpect(status().isOk());
    }

    @Test
    public void testBatchStopInstances() throws Exception {
        // Arrange
        List<Long> instanceIds = Arrays.asList(1L, 2L, 3L);

        // Act & Assert
        mockMvc.perform(
                        post("/api/logstash/processes/instances/batch/stop")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(instanceIds)))
                .andExpect(status().isOk());
    }

    @Test
    public void testBatchStartInstancesWithEmptyList() throws Exception {
        // Arrange
        List<Long> instanceIds = Arrays.asList();

        // Act & Assert
        mockMvc.perform(
                        post("/api/logstash/processes/instances/batch/start")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(instanceIds)))
                .andExpect(status().isOk());
    }
}
