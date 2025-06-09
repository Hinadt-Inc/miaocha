package com.hinadt.miaocha.logstash;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.hinadt.miaocha.application.logstash.LogstashMachineConnectionValidator;
import com.hinadt.miaocha.application.service.MachineService;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LogstashMachineInfoConnectionValidatorTest {

    @Mock private MachineService machineService;

    @InjectMocks private LogstashMachineConnectionValidator connectionValidator;

    private MachineInfo testMachineInfo;

    @BeforeEach
    void setUp() {
        testMachineInfo = new MachineInfo();
        testMachineInfo.setId(1L);
        testMachineInfo.setName("test-machine");
        testMachineInfo.setIp("192.168.1.100");
        testMachineInfo.setPort(22);
    }

    @Test
    void testValidateSingleMachineConnection_Success() {
        // Mock successful connection
        when(machineService.testConnection(1L)).thenReturn(true);

        // Should not throw exception
        assertDoesNotThrow(
                () -> connectionValidator.validateSingleMachineConnection(testMachineInfo));

        verify(machineService, times(1)).testConnection(1L);
    }

    @Test
    void testValidateSingleMachineConnection_Failed() {
        // Mock failed connection
        when(machineService.testConnection(1L)).thenReturn(false);

        // Should throw BusinessException
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> connectionValidator.validateSingleMachineConnection(testMachineInfo));

        assertEquals(ErrorCode.MACHINE_CONNECTION_FAILED, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("无法连接到机器"));
        assertTrue(exception.getMessage().contains("192.168.1.100"));

        verify(machineService, times(1)).testConnection(1L);
    }

    @Test
    void testValidateSingleMachineConnection_Exception() {
        // Mock exception during connection test
        when(machineService.testConnection(1L)).thenThrow(new RuntimeException("Network error"));

        // Should throw BusinessException
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> connectionValidator.validateSingleMachineConnection(testMachineInfo));

        assertEquals(ErrorCode.MACHINE_CONNECTION_FAILED, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("验证机器"));
        assertTrue(exception.getMessage().contains("连接时发生异常"));

        verify(machineService, times(1)).testConnection(1L);
    }

    @Test
    void testValidateSingleMachineConnection_NullMachine() {
        // Should throw BusinessException for null machine
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> connectionValidator.validateSingleMachineConnection(null));

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("机器信息不能为空", exception.getMessage());

        verify(machineService, never()).testConnection(anyLong());
    }

    @Test
    void testValidateMachineConnectionById_Success() {
        // Mock successful connection
        when(machineService.testConnection(1L)).thenReturn(true);

        // Should not throw exception
        assertDoesNotThrow(() -> connectionValidator.validateMachineConnectionById(1L));

        verify(machineService, times(1)).testConnection(1L);
    }

    @Test
    void testValidateMachineConnectionById_Failed() {
        // Mock failed connection
        when(machineService.testConnection(1L)).thenReturn(false);

        // Should throw BusinessException
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> connectionValidator.validateMachineConnectionById(1L));

        assertEquals(ErrorCode.MACHINE_CONNECTION_FAILED, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("无法连接到机器"));

        verify(machineService, times(1)).testConnection(1L);
    }

    @Test
    void testValidateMachineConnectionById_NullId() {
        // Should throw BusinessException for null ID
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> connectionValidator.validateMachineConnectionById(null));

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("机器ID不能为空", exception.getMessage());

        verify(machineService, never()).testConnection(anyLong());
    }
}
