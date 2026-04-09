package com.loyalty.service;

import com.loyalty.dto.FeatureCreateDTO;
import com.loyalty.dto.FeatureResponseDTO;
import com.loyalty.entity.Feature;
import com.loyalty.repository.FeatureRepository;
import com.loyalty.exception.FeatureNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeatureServiceTest {

    @Mock
    private FeatureRepository repository;

    @InjectMocks
    private FeatureService service;

    private FeatureCreateDTO validDTO;

    @BeforeEach
    void setUp() {
        validDTO = new FeatureCreateDTO();
        validDTO.setName("Test Feature");
        validDTO.setDescription("Description");
    }

    // ─── Happy Path ─────────────────────────────────────────────────────────

    @Test
    void create_success_returnsCreatedFeature() {
        when(repository.save(any(Feature.class))).thenAnswer(invocation -> {
            Feature f = invocation.getArgument(0);
            f.setUid("uuid-123");
            f.setCreatedAt(LocalDateTime.now());
            return f;
        });

        FeatureResponseDTO result = service.create(validDTO, "user-uid");

        assertNotNull(result.getUid());
        assertEquals("Test Feature", result.getName());
        verify(repository).save(any(Feature.class));
    }

    @Test
    void getByUid_success_returnsFeature() {
        Feature feature = new Feature();
        feature.setUid("uuid-123");
        feature.setName("Test");
        feature.setDescription("Desc");
        feature.setCreatedAt(LocalDateTime.now());

        when(repository.findByUid("uuid-123")).thenReturn(Optional.of(feature));

        FeatureResponseDTO result = service.getByUid("uuid-123");

        assertEquals("uuid-123", result.getUid());
        assertEquals("Test", result.getName());
    }

    // ─── Error Path ─────────────────────────────────────────────────────────

    @Test
    void getByUid_notFound_throwsFeatureNotFoundException() {
        when(repository.findByUid("nonexistent")).thenReturn(Optional.empty());

        assertThrows(FeatureNotFoundException.class, 
            () -> service.getByUid("nonexistent"));
    }

    @Test
    void deleteByUid_success_deletesFromRepository() {
        doNothing().when(repository).deleteById("uuid-123");

        service.deleteByUid("uuid-123");

        verify(repository).deleteById("uuid-123");
    }
}
