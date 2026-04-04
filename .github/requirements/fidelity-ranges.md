# HU-08: Rangos de Clasificación de Fidelidad

## Historia de Usuario

Como usuario de LOYALTY, quiero definir los rangos de clasificación de fidelidad, para segmentar a los clientes y sus beneficios.

## Criterios de Aceptación

### Scenario: Configuración exitosa de rangos de fidelidad
**Given** los rangos propuestos son completos y no se superponen  
**When** se ejecuta POST /api/v1/customer-tiers con { name, discountPercentage, hierarchyLevel, discountTypeId }  
**Then** el sistema guarda la configuración de rangos (customer_tiers)  
**And** la segmentación de clientes utiliza los nuevos umbrales

### Scenario: Rechazo por superposición de rangos
**Given** existen reglas de clasificación (classification_rule) que requieren exclusividad entre niveles  
**When** se intenta crear una classification_rule con POST /api/v1/customer-tiers/{tierId}/classification-rules con valores superpuestos  
**Then** el sistema rechaza la configuración con HTTP 400  
**And** reporta inconsistencia en la definición de niveles

### Scenario: Rechazo por discontinuidad o vacíos en rangos
**Given** la clasificación exige cobertura continua del dominio definido  
**When** se intenta guardar una configuración (POST classification_rule) con vacíos entre rangos  
**Then** el sistema rechaza la configuración con HTTP 400  
**And** mantiene la última segmentación válida

### Scenario: Rechazo por orden inválido de niveles
**Given** los niveles deben mantener progresión ascendente de exigencia (hierarchyLevel)  
**When** se intenta crear un customer_tier con hierarchyLevel menor o igual a uno existente en el mismo ecommerce  
**Then** el sistema rechaza la configuración con HTTP 400  
**And** notifica incumplimiento de jerarquía de fidelidad

### Scenario: Aplicación efectiva de la nueva segmentación
**Given** la configuración de rangos (customer_tiers + classification_rule) fue aceptada  
**When** el motor clasifica a un cliente ejecutando GET /api/v1/calculate con métricas vigentes  
**Then** el cliente queda asignado al nivel correspondiente a su rango  
**And** los beneficios aplicables se determinan según ese nivel

## Notas Técnicas (Constraints)

- **Stack:** Java 21 + Spring Boot 3.x.
- **Auth:** JWT para gestión de configuración.
- **Almacenamiento:** PostgreSQL con tablas `customer_tiers` y `classification_rule` (parte del sistema de reglas unificado).
- **Endpoints:** POST/GET/PUT/DELETE /api/v1/customer-tiers + POST/PUT/DELETE /api/v1/customer-tiers/{tierId}/classification-rules
- **Performance:** La clasificación debe ejecutarse en memoria para no impactar la latencia del /calculate.
- **Sync:** Sincronización de rangos vía RabbitMQ al Engine Service.
- Los rangos de fidelidad (customer_tiers) tienen: nombre del nivel (Bronce, Plata, Oro, Platino), discountPercentage, hierarchyLevel.
- Los rangos no deben superponerse entre niveles (validación en classification_rule).
- Los rangos deben ser continuos (sin vacíos) desde el nivel más bajo hasta el más alto.
- Cada nivel debe tener un hierarchyLevel mayor que el nivel anterior.
- La matriz de clasificación utiliza estos rangos para asignar clientes a niveles.
