package com.hina.log.logstash;

import com.hina.log.application.logstash.LogstashMachineConnectionValidator;
import com.hina.log.application.service.MachineService;
import com.hina.log.common.exception.BusinessException;
import com.hina.log.common.exception.ErrorCode;
import com.hina.log.domain.entity.Machine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LogstashMachineConnectionValidatorTest {

    @Mock
    private MachineService machineService;

    @InjectMocks
    private LogstashMachineConnectionValidator connectionValidator;

    private Machine testMachine;

    @BeforeEach
    void setUp() {
        testMachine = new Machine();
        testMachine.setId(1L);
        testMachine.setName("test-machine");
        testMachine.setIp("192.168.1.100");
        testMachine.setPort(22);
    }

    @Test
    void testValidateSingleMachineConnection_Success() {
        // Mock successful connection
        when(machineService.testConnection(1L)).thenReturn(true);

        // Should not throw exception
        assertDoesNotThrow(() -> connectionValidator.validateSingleMachineConnection(testMachine));

        verify(machineService, times(1)).testConnection(1L);
    }

    @Test
    void testValidateSingleMachineConnection_Failed() {
        // Mock failed connection
        when(machineService.testConnection(1L)).thenReturn(false);

        // Should throw BusinessException
        BusinessException exception = assertThrows(BusinessException.class,
                () -> connectionValidator.validateSingleMachineConnection(testMachine));

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
        BusinessException exception = assertThrows(BusinessException.class,
                () -> connectionValidator.validateSingleMachineConnection(testMachine));

        assertEquals(ErrorCode.MACHINE_CONNECTION_FAILED, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("验证机器"));
        assertTrue(exception.getMessage().contains("连接时发生异常"));

        verify(machineService, times(1)).testConnection(1L);
    }

    @Test
    void testValidateSingleMachineConnection_NullMachine() {
        // Should throw BusinessException for null machine
        BusinessException exception = assertThrows(BusinessException.class,
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
        BusinessException exception = assertThrows(BusinessException.class,
                () -> connectionValidator.validateMachineConnectionById(1L));

        assertEquals(ErrorCode.MACHINE_CONNECTION_FAILED, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("无法连接到机器"));

        verify(machineService, times(1)).testConnection(1L);
    }

    @Test
    void testValidateMachineConnectionById_NullId() {
        // Should throw BusinessException for null ID
        BusinessException exception = assertThrows(BusinessException.class,
                () -> connectionValidator.validateMachineConnectionById(null));

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("机器ID不能为空", exception.getMessage());

        verify(machineService, never()).testConnection(anyLong());
    }
} 