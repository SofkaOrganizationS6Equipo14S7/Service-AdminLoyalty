package com.loyalty.repository;

import com.loyalty.entity.Feature;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class FeatureRepositoryTest {

    @Autowired
    private FeatureRepository repository;

    @Test
    void findByUid_exists_returnsFeature() {
        Feature feature = new Feature();
        feature.setUid(UUID.randomUUID().toString());
        feature.setName("Test Feature");
        feature.setDescription("Description");
        
        repository.save(feature);

        Optional<Feature> result = repository.findByUid(feature.getUid());

        assertTrue(result.isPresent());
        assertEquals("Test Feature", result.get().getName());
    }

    @Test
    void findByUid_notExists_returnsEmpty() {
        Optional<Feature> result = repository.findByUid("nonexistent-uid");
        
        assertTrue(result.isEmpty());
    }

    @Test
    void save_persistsFeature() {
        Feature feature = new Feature();
        feature.setUid(UUID.randomUUID().toString());
        feature.setName("New Feature");

        Feature saved = repository.save(feature);

        assertNotNull(saved.getUid());
        assertTrue(repository.existsById(saved.getUid()));
    }

    @Test
    void deleteByUid_removesFeature() {
        Feature feature = new Feature();
        feature.setUid(UUID.randomUUID().toString());
        feature.setName("To Delete");
        
        Feature saved = repository.save(feature);
        repository.deleteById(saved.getUid());

        assertFalse(repository.existsById(saved.getUid()));
    }

    @Test
    void existsByName_returnsTrue() {
        Feature feature = new Feature();
        feature.setUid(UUID.randomUUID().toString());
        feature.setName("Unique Name");
        
        repository.save(feature);

        assertTrue(repository.existsByName("Unique Name"));
    }
}
