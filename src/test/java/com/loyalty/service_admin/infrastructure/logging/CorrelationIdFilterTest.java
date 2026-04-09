package com.loyalty.service_admin.infrastructure.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CorrelationIdFilter Unit Tests")
class CorrelationIdFilterTest {

    @InjectMocks
    private CorrelationIdFilter filter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Test
    void testExistingCorrelationId_isPreserved() throws ServletException, IOException {
        String existingId = "existing-correlation-id";
        when(request.getHeader("X-Correlation-Id")).thenReturn(existingId);

        doAnswer(invocation -> {
            assertEquals(existingId, MDC.get("correlationId"));
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader("X-Correlation-Id", existingId);
        verify(filterChain).doFilter(request, response);
        assertNull(MDC.get("correlationId")); // cleaned up after filter
    }

    @Test
    void testMissingCorrelationId_generatesNew() throws ServletException, IOException {
        when(request.getHeader("X-Correlation-Id")).thenReturn(null);

        doAnswer(invocation -> {
            String generated = MDC.get("correlationId");
            assertNotNull(generated);
            assertFalse(generated.isBlank());
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader(eq("X-Correlation-Id"), anyString());
        assertNull(MDC.get("correlationId")); // cleaned up
    }

    @Test
    void testBlankCorrelationId_generatesNew() throws ServletException, IOException {
        when(request.getHeader("X-Correlation-Id")).thenReturn("   ");

        doAnswer(invocation -> {
            String generated = MDC.get("correlationId");
            assertNotNull(generated);
            assertNotEquals("   ", generated);
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testMDC_clearedEvenOnException() throws ServletException, IOException {
        when(request.getHeader("X-Correlation-Id")).thenReturn("test-id");
        doThrow(new ServletException("error")).when(filterChain).doFilter(request, response);

        assertThrows(ServletException.class,
                () -> filter.doFilterInternal(request, response, filterChain));

        assertNull(MDC.get("correlationId")); // must be cleaned up even on exception
    }
}
