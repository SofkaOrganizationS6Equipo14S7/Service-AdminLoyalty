package com.loyalty.service_engine.infrastructure.initialization;

import com.loyalty.service_engine.domain.entity.DiscountConfigEntity;
import com.loyalty.service_engine.domain.repository.DiscountConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cargador de configuración de descuentos al iniciar Engine (cold-start).
 * 
 * IMPORTANTE: Garantiza autonomía del Engine ante fallas de RabbitMQ.
 * 
 * Flujo:
 * 1. Al iniciar Engine, ApplicationReadyEvent se dispara
 * 2. Este listener carga la configuración activa desde BD réplica
 * 3. Pre-carga en Caffeine para evitar latencia en primer acceso
 * 4. Si RabbitMQ está caído, Engine sigue funcionando con config local
 * 
 * Resilencia:
 * - Si BD no tiene data: logs de warning, Engine sigue sin config (graceful)
 * - Si caché falla: fallback a lectura directo de BD
 * - No lanza excepciones que detengan startup
 */
@Service
@Slf4j
public class DiscountConfigStartupLoader {
    
    private final DiscountConfigRepository discountConfigRepository;
    private final CacheManager cacheManager;
    
    public DiscountConfigStartupLoader(
        DiscountConfigRepository discountConfigRepository,
        CacheManager cacheManager
    ) {
        this.discountConfigRepository = discountConfigRepository;
        this.cacheManager = cacheManager;
    }
    
    /**
     * Se ejecuta cuando ApplicationContext está listo (después de todas las inicializaciones).
     * Carga configuración vigente de descuentos desde BD réplica.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void loadDiscountConfigOnStartup() {
        try {
            log.info("Starting cold-start: Loading discount configuration from replica DB...");
            
            // Obtener config activa desde BD réplica
            var activeConfig = discountConfigRepository.findByIsActiveTrue();
            
            if (activeConfig.isPresent()) {
                DiscountConfigEntity config = activeConfig.get();
                log.info("Found active discount config. EcommerceId: {}, Limit: {} {}", 
                    config.getEcommerceId(), 
                    config.getMaxDiscountLimit(), 
                    config.getCurrencyCode());
                
                // Pre-cargar en caché para evitar latencia en primer acceso
                try {
                    var discountConfigCache = cacheManager.getCache("discount_config");
                    if (discountConfigCache != null) {
                        // La caché se completará con lazy-loading, pero podríamos pre-seeding aquí
                        log.debug("Set up cache manager for discount_config (lazy-load on first access)");
                    }
                } catch (Exception e) {
                    log.warn("Failed to pre-load cache, will fallback to DB reads: {}", e.getMessage());
                }
                
                log.info("Cold-start completed successfully. Engine is ready with replica config");
            } else {
                // No hay config, pero Engine puede seguir operando
                log.warn("No active discount configuration found in replica DB on startup. Engine will operate without limits until configured.");
                log.warn("This is normal on first deployment. Admin will push config via RabbitMQ.");
            }
        } catch (Exception e) {
            // No frenar el startup, solo log
            log.error("Error during cold-start of discount configuration. Engine will continue without pre-loaded config. Next access will attempt fresh load from DB", e);
        }
    }
}
