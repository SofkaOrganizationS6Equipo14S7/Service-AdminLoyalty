# HU-08: Rangos de Clasificación de Fidelidad

## Historia de Usuario

Como usuario de LOYALTY, quiero definir los rangos de clasificación de fidelidad, para segmentar a los clientes y sus beneficios.

## Criterios de Aceptación

### Scenario: Configuración exitosa de rangos de fidelidad
**Given** los rangos propuestos son completos y no se superponen  
**When** se registran los nuevos umbrales de clasificación  
**Then** el sistema guarda la configuración de rangos  
**And** la segmentación de clientes utiliza los nuevos umbrales

### Scenario: Rechazo por superposición de rangos
**Given** existen reglas de clasificación que requieren exclusividad entre niveles  
**When** se define una configuración con rangos superpuestos  
**Then** el sistema rechaza la configuración  
**And** reporta inconsistencia en la definición de niveles

### Scenario: Rechazo por discontinuidad o vacíos en rangos
**Given** la clasificación exige cobertura continua del dominio definido  
**When** se registra una configuración con vacíos entre rangos  
**Then** el sistema rechaza la configuración  
**And** mantiene la última segmentación válida

### Scenario: Rechazo por orden inválido de niveles
**Given** los niveles deben mantener progresión ascendente de exigencia  
**When** se define un nivel superior con umbral menor o igual que uno inferior  
**Then** el sistema rechaza la configuración  
**And** notifica incumplimiento de jerarquía de fidelidad

### Scenario: Aplicación efectiva de la nueva segmentación
**Given** la configuración de rangos fue aceptada  
**When** el motor clasifica a un cliente con métricas vigentes  
**Then** el cliente queda asignado al nivel correspondiente a su rango  
**And** los beneficios aplicables se determinan según ese nivel

## Notas Técnicas (Constraints)

- **Stack:** Java 21 + Spring Boot 3.x.
- **Auth:** JWT para gestión de configuración.
- **Almacenamiento:** PostgreSQL con tablas para `fidelity_ranges`.
- **Performance:** La clasificación debe ejecutarse en memoria para no impactar la latencia del /calculate.
- **Sync:** Sincronización de rangos vía RabbitMQ al Engine Service.
- Los rangos de fidelidad tienen: nombre del nivel (Bronce, Plata, Oro, Platino), umbral mínimo, umbral máximo.
- Los rangos no deben superponerse entre niveles.
- Los rangos deben ser continuos (sin vacíos) desde el nivel más bajo hasta el más alto.
- Cada nivel debe tener un umbral mayor que el nivel anterior.
- La matriz de clasificación utiliza estos rangos para asignar clientes a niveles.
