# Historias de Usuario - Loyalty Discount Engine

## HU-01: Inicio y cierre de sesión (Auth Management)

Como Administrador, quiero iniciar y cerrar sesión en el Dashboard, para gestionar las reglas de negocio de forma segura mediante un token de identidad.

### Criterios de Aceptación

#### Scenario: Inicio de sesión exitoso
**Given** usuario registrado con credenciales válidas en loyalty-admin  
**When** envía POST /api/v1/auth/login  
**Then** el sistema retorna un JWT válido con los claims de userId y roles  
**And** el status code es 200 OK

#### Scenario: Intento con credenciales erróneas o campos vacíos
**When** envía credenciales inválidas o falta el username/password  
**Then** el sistema retorna 401 Unauthorized  
**And** el mensaje indica "Invalid credentials" o "Missing fields"

#### Scenario: Cierre de sesión (Logout)
**Given** usuario con JWT activo  
**When** envía POST /api/v1/auth/logout  
**Then** el sistema invalida el token (vía blacklist en caché o simplemente borrando el token en el cliente)  
**And** el acceso a endpoints protegidos queda denegado

### Notas Técnicas (Constraints)

- **Service:** Implementar exclusivamente en loyalty-admin (Puerto 8081).
- **Security:** Contraseñas hasheadas con BCrypt.
- **JWT:** Generación usando la clave secreta compartida (Symmetric).
- **Stateless:** No usar HttpSession. Todo se maneja vía el Header Authorization: Bearer {token}.
- **Output:** El login debe retornar un JSON con el token y la fecha de expiración.

---

## HU-02: Gestión de Usuarios por Ecommerce

Como Admin, quiero crear usuarios vinculados a un ecommerce, para garantizar que cada uno gestione únicamente sus propias reglas de descuento sin afectar a otros ecommerce.

### Criterios de Aceptación

#### Scenario: Creación de usuario estándar por ADMIN
**Given** existe un usuario con rol ADMIN  
**And** un ecommerce contrató los servicios de LOYALTY  
**When** el ADMIN crea un usuario estándar asociado a ese ecommerce  
**Then** el usuario queda vinculado exclusivamente a dicho ecommerce

#### Scenario: Validar que el usuario estándar solo accede a su ecommerce
**Given** existe un usuario estándar asociado a un ecommerce  
**When** el usuario inicia sesión  
**Then** solo puede visualizar y gestionar información de su ecommerce  
**And** no puede acceder a datos de otros ecommerce

#### Scenario: Crear usuario estándar para ecommerce
**Given** soy un ADMIN con acceso al sistema  
**And** existe un ecommerce registrado  
**When** creo un nuevo usuario estándar asociado a ese ecommerce  
**Then** el sistema genera las credenciales del usuario  
**And** el usuario queda vinculado al ecommerce especificado

#### Scenario: Listar usuarios por ecommerce
**Given** soy un ADMIN con acceso al sistema  
**And** existen usuarios estándar asociados a un ecommerce  
**When** consulto los usuarios de ese ecommerce  
**Then** el sistema muestra la lista de usuarios vinculados  
**And** cada usuario muestra: username, rol, email, fecha de creación

#### Scenario: Actualizar usuario (cambio de ecommerce)
**Given** soy un ADMIN  
**And** existe un usuario estándar asociado a un ecommerce  
**When** intento cambiar el ecommerce asociado al usuario  
**Then** el sistema actualiza la vinculación  
**And** el usuario ahora pertenece al nuevo ecommerce

#### Scenario: Eliminar usuario estándar
**Given** soy un ADMIN  
**And** existe un usuario estándar asociado a un ecommerce  
**When** elimino el usuario  
**Then** el sistema elimina el perfil de forma permanente  
**And** el usuario ya no puede acceder al sistema

#### Scenario: SUPERADMIN crea ADMIN para un ecommerce
**Given** existe un usuario con rol SUPERADMIN  
**And** un ecommerce contrató los servicios de LOYALTY  
**When** el SUPERADMIN crea un ADMIN para ese ecommerce  
**Then** el ADMIN queda vinculado al ecommerce  
**And** el ADMIN puede gestionar usuarios estándar de su ecommerce

#### Scenario: Listar todos los ecommerce (SUPERADMIN)
**Given** soy un SUPERADMIN con acceso al sistema  
**When** consulto todos los ecommerce registrados  
**Then** el sistema muestra la lista de todos los ecommerce  
**And** puedo gestionar los ADMIN de cada ecommerce

### Notas Técnicas (Constraints)

- **Vinculación:** 
  - USUARIO ESTÁNDAR: Debe estar vinculado a exactamente un ecommerce.
  - ADMIN: Debe estar vinculado a exactamente un ecommerce.
  - SUPERADMIN: No está vinculado a ningún ecommerce específico (acceso global).
- **Aislamiento:** Consultas en BD filtran por `ecommerce_id` del contexto de seguridad.
- **Auth:** JWT con claim `ecommerce_id` y `role` para control de acceso.
- **Stack:** Java 21 + Spring Boot 3.x.
- **Validación:** No permitir crear usuarios sin asignar un ecommerce válido (excepto SUPERADMIN).
- **SUPERADMIN:** Acceso total, no tiene restricción de `ecommerce_id`, puede gestionar todos los ecommerce.
- **ADMIN:** Acceso completo a su ecommerce específico, puede crear/gestionar usuarios estándar de su ecommerce.
- **USUARIO ESTÁNDAR:** Acceso limitado según permisos asignados por el ADMIN de su ecommerce.

---

## HU-03: Administra su Ecommerce

**Como** STORE_ADMIN
**Quiero** ser administrador de mi ecommerce  
**Para** crear, modificar y eliminar usuarios estándar de mi ecommerce, gestionar reglas de descuento, productos y clientes, y acceder exclusivamente a la información y métricas de mi ecommerce sin poder ver datos de otros ecommerce.

### Criterios de Aceptación

#### Scenario: Crear usuario estándar por STORE_ADMIN
**Given** existe un usuario con rol STORE_ADMIN  
**When** el STORE_ADMIN crea un usuario estándar asociado a ese ecommerce  
**Then** el usuario queda vinculado exclusivamente a dicho ecommerce

#### Scenario: Validar que el usuario estándar solo accede a su ecommerce
**Given** existe un usuario estándar asociado a un ecommerce  
**When** el usuario inicia sesión  
**Then** solo puede visualizar y gestionar información de su ecommerce  
**And** no puede acceder a datos de otros ecommerce

#### Scenario: Crear usuario estándar para ecommerce
**Given** soy un STORE_ADMIN con acceso al sistema  
**And** existe un ecommerce registrado  
**When** creo un nuevo usuario estándar asociado a ese ecommerce  
**Then** el sistema genera las credenciales del usuario  
**And** el usuario queda vinculado al ecommerce especificado

#### Scenario: Listar usuarios por ecommerce
**Given** soy un STORE_ADMIN con acceso al sistema  
**And** existen usuarios estándar asociados a un ecommerce  
**When** consulto los usuarios de ese ecommerce  
**Then** el sistema muestra la lista de usuarios vinculados  
**And** cada usuario muestra: username, rol, email, fecha de creación

#### Scenario: Actualizar datos de perfil de usuario estándar
**Given** soy un STORE_ADMIN  
**And** existe un usuario estándar asociado a mi ecommerce  
**When** actualizo los datos del usuario (nombre, email)  
**Then** el sistema actualiza la información del usuario  
**And** el usuario mantiene la vinculación con mi ecommerce

#### Scenario: Eliminar usuario estándar
**Given** soy un STORE_ADMIN  
**And** existe un usuario estándar asociado a un ecommerce  
**When** elimino el usuario  
**Then** el sistema elimina el perfil de forma permanente  
**And** el usuario ya no puede acceder al sistema

### Notas Técnicas (Constraints)

- **Vinculación:** STORE_ADMIN debe estar vinculado a exactamente un ecommerce.
- **Aislamiento:** Consultas en BD filtran por `ecommerce_id` del contexto de seguridad.
- **Auth:** JWT con claim `role: STORE_ADMIN` y `ecommerce_id`.
- **Stack:** Java 21 + Spring Boot 3.x.
- **Validación:** STORE_ADMIN solo puede crear/gestionar usuarios estándar de su propio ecommerce.
- **Permisos:** Puede gestionar reglas de descuento, productos y clientes de su ecommerce.

---

## HU-04: Usuario Estándar

**Como** USER
**Quiero** tener acceso limitado según los permisos que me asigne el STORE_ADMIN  
**Para** visualizar y gestionar únicamente la información de mi ecommerce, sin poder crear otros usuarios y estando vinculado a un ecommerce específico.

### Criterios de Aceptación

#### Scenario: Iniciar sesión en el sistema
**Given** existe un usuario con rol USER  
**And** está vinculado a un ecommerce específico  
**When** el usuario inicia sesión  
**Then** el sistema valida las credenciales  
**And** redirige al dashboard de su ecommerce

#### Scenario: Acceder solo a su ecommerce
**Given** soy un USER vinculado a un ecommerce  
**When** intento acceder a información de otro ecommerce  
**Then** el sistema deniega el acceso  
**And** muestra un mensaje de error

#### Scenario: Visualizar información del ecommerce
**Given** soy un USER con acceso al sistema  
**When** consulto la información de mi ecommerce  
**Then** el sistema muestra los datos accesibles según mis permisos  
**And** no puedo modificar configuraciones del ecommerce

#### Scenario: Actualizar mi perfil
**Given** soy un USER  
**When** actualizo mi información de perfil (nombre, email, contraseña)  
**Then** el sistema actualiza los datos  
**And** mi vinculación con el ecommerce se mantiene

### Notas Técnicas (Constraints)

- **Vinculación:** USER debe estar vinculado a exactamente un ecommerce.
- **Aislamiento:** Consultas en BD filtran por `ecommerce_id` del contexto de seguridad.
- **Auth:** JWT con claim `role: USER` y `ecommerce_id`.
- **Stack:** Java 21 + Spring Boot 3.x.
- **Validación:** No puede crear usuarios, solo acceso limitado según permisos asignados por STORE_ADMIN.
- **Permisos:** Solo puede visualizar y gestionar información según los permisos asignados.

---

## HU-05: Gestión y Validación de API Keys

Como Super Admin, quiero gestionar y validar las API Keys de cada ecommerce, para asegurar que solo sistemas autorizados puedan acceder a sus recursos en la plataforma.

### Criterios de Aceptación

#### Scenario: Crear una clave de acceso para un ecommerce válido
**Given** soy un Super Admin con acceso al sistema  
**And** existe un ecommerce registrado  
**When** creo una nueva clave de acceso para ese ecommerce  
**Then** el sistema genera la clave de acceso correctamente

#### Scenario: Ver las claves de acceso de un ecommerce
**Given** soy un Super Administrador con acceso al sistema  
**And** existen claves de acceso registradas para un ecommerce  
**When** consulto las claves de acceso de ese ecommerce  
**Then** el sistema muestra la lista de claves de acceso asociadas  
**And** oculta parte de la información de cada clave para proteger su seguridad

#### Scenario: Validar API Key en Engine Service
**Given** soy un sistema con una API Key válida  
**When** envío una request al Engine Service  
**Then** el sistema valida la API Key contra la caché local  
**And** permite el acceso si es válida

#### Scenario: Rechazar API Key inválida
**Given** soy un sistema con una API Key inválida  
**When** envío una request al Engine Service  
**Then** el sistema responde con HTTP 401 Unauthorized

### Notas Técnicas (Constraints)

- **Sync:** API Keys se sincronizan de Admin Service a Engine Service vía RabbitMQ.
- **Formato:** UUID v4.
- **Masking:** mostrar solo `****XXXX` (últimos 4 caracteres).
- **Caché:** Engine Service mantiene caché local de API Keys válidas.
- **Stack:** Java 21 + Spring Boot 3.x.

---

## HU-06: Gestión de Reglas de Temporada

Como usuario de LOYALTY, quiero crear, editar y eliminar reglas de temporada para automatizar las promociones por demanda de temporada.

### Criterios de Aceptación

#### Scenario: Creación exitosa de una regla de temporada
**Given** no hay una regla activa para esa temporada  
**When** se registra una regla de descuento con vigencia y beneficio válidos  
**Then** la regla queda almacenada en el sistema  
**And** la regla queda disponible para su aplicación durante la vigencia definida

#### Scenario: Rechazo de creación por superposición de fechas
**Given** ya hay una regla activa para esa temporada  
**When** se intenta registrar una nueva regla con las mismas fechas  
**Then** el sistema rechaza el registro  
**And** informa el conflicto de superposición de fechas entre reglas de temporada

#### Scenario: Edición exitosa de una regla de temporada
**Given** existe una regla de temporada registrada  
**When** se actualizan sus condiciones con valores válidos  
**Then** el sistema conserva la regla con la nueva configuración  
**And** la versión actualizada es la considerada para nuevas evaluaciones

#### Scenario: Rechazo de edición por incumplir rangos
**Given** existen límites de descuento definidos  
**When** la actualización propuesta excede los límites permitidos  
**Then** el sistema rechaza la modificación  
**And** mantiene la última configuración válida

#### Scenario: Eliminación exitosa de una regla de temporada
**Given** existe una regla de temporada registrada  
**When** se confirma su eliminación  
**Then** la regla deja de participar en evaluaciones futuras

#### Scenario: Rechazo de eliminación de regla inexistente
**Given** no existe una regla asociada al identificador solicitado  
**When** se solicita su eliminación  
**Then** el sistema rechaza la operación  
**And** informa que no existe una regla para el identificador solicitado

#### Scenario: Rechazo de edición de regla inexistente
**Given** no existe una regla asociada al identificador solicitado  
**When** se solicita su edición  
**Then** el sistema rechaza la operación  
**And** informa que no existe una regla para el identificador solicitado

### Notas Técnicas (Constraints)

- **Stack:** Java 21 + Spring Boot 3.x.
- **Auth:** JWT para gestión de reglas.
- **Almacenamiento:** PostgreSQL con tablas para `seasonal_rules`.
- **Performance:** Las reglas deben evaluarse en memoria para no impactar la latencia del /calculate.
- **Sync:** Sincronización de reglas vía RabbitMQ al Engine Service.
- Las reglas de temporada tienen: nombre, fecha inicio, fecha fin, porcentaje de descuento, tipo de descuento.
- No puede haber superposición de fechas para el mismo ecommerce.
- Los límites de descuento (mín/máx) deben validarse contra la configuración global.
- Las reglas eliminadas no participan en evaluaciones futuras.

---

## HU-07: Gestión de Reglas por Tipo de Producto

Como usuario de LOYALTY, quiero crear, editar y eliminar reglas por tipo de producto, para automatizar las promociones en base a inventario.

### Criterios de Aceptación

#### Scenario: Creación exitosa de una regla por tipo de producto
**Given** no hay una regla activa para ese tipo de producto  
**When** se define una nueva regla de descuento con parámetros válidos  
**Then** la regla queda registrada  
**And** la regla puede ser aplicada por el motor

#### Scenario: Rechazo de creación por duplicidad
**Given** existe una regla para el mismo tipo de producto  
**When** se registra una regla  
**Then** el sistema rechaza la creación  
**And** reporta conflicto de reglas para ese tipo de producto

#### Scenario: Edición exitosa de una regla por tipo de producto
**Given** existe una regla por tipo de producto registrada  
**When** se modifican sus parámetros dentro de los límites permitidos  
**Then** el sistema guarda la nueva versión  
**And** las nuevas evaluaciones consideran la configuración actualizada

#### Scenario: Rechazo de edición por datos incompletos
**Given** la regla requiere tipo de producto y beneficio para ser válida  
**When** la actualización omite uno o más datos obligatorios  
**Then** el sistema rechaza la modificación  
**And** mantiene la versión previamente válida

#### Scenario: Eliminación exitosa de una regla por tipo de producto
**Given** existe una regla por tipo de producto  
**When** se solicita su eliminación  
**Then** la regla deja de estar disponible para nuevas transacciones

#### Scenario: Rechazo de eliminación por regla no encontrada
**Given** no existe una regla para el tipo de producto indicado  
**When** se intenta eliminar la regla  
**Then** el sistema rechaza la operación  
**And** mantiene sin cambios la configuración

#### Scenario: Rechazo de edición de regla inexistente
**Given** no existe una regla asociada al tipo de producto indicado  
**When** se solicita su edición  
**Then** el sistema rechaza la operación  
**And** informa que no existe una regla para el identificador solicitado

### Notas Técnicas (Constraints)

- **Stack:** Java 21 + Spring Boot 3.x.
- **Auth:** JWT para gestión de reglas.
- **Almacenamiento:** PostgreSQL con tablas para `product_rules`.
- **Performance:** Las reglas deben evaluarse en memoria para no impactar la latencia del /calculate.
- **Sync:** Sincronización de reglas vía RabbitMQ al Engine Service.
- Las reglas por tipo de producto tienen: nombre, tipo de producto, porcentaje de descuento, beneficio.
- Solo puede existir una regla activa por tipo de producto.
- Los límites de descuento (mín/máx) deben validarse contra la configuración global.
- Las reglas eliminadas no participan en evaluaciones futuras.

---

## HU-08: Rangos de Clasificación de Fidelidad

Como usuario de LOYALTY, quiero definir los rangos de clasificación de fidelidad, para segmentar a los clientes y sus beneficios.

### Criterios de Aceptación

#### Scenario: Configuración exitosa de rangos de fidelidad
**Given** los rangos propuestos son completos y no se superponen  
**When** se registran los nuevos umbrales de clasificación  
**Then** el sistema guarda la configuración de rangos  
**And** la segmentación de clientes utiliza los nuevos umbrales

#### Scenario: Rechazo por superposición de rangos
**Given** existen reglas de clasificación que requieren exclusividad entre niveles  
**When** se define una configuración con rangos superpuestos  
**Then** el sistema rechaza la configuración  
**And** reporta inconsistencia en la definición de niveles

#### Scenario: Rechazo por discontinuidad o vacíos en rangos
**Given** la clasificación exige cobertura continua del dominio definido  
**When** se registra una configuración con vacíos entre rangos  
**Then** el sistema rechaza la configuración  
**And** mantiene la última segmentación válida

#### Scenario: Rechazo por orden inválido de niveles
**Given** los niveles deben mantener progresión ascendente de exigencia  
**When** se define un nivel superior con umbral menor o igual que uno inferior  
**Then** el sistema rechaza la configuración  
**And** notifica incumplimiento de jerarquía de fidelidad

#### Scenario: Aplicación efectiva de la nueva segmentación
**Given** la configuración de rangos fue aceptada  
**When** el motor clasifica a un cliente con métricas vigentes  
**Then** el cliente queda asignado al nivel correspondiente a su rango  
**And** los beneficios aplicables se determinan según ese nivel

### Notas Técnicas (Constraints)

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

---

## HU-09: Límite y Prioridad de Descuentos

Como usuario de LOYALTY, quiero definir el tope máximo y la prioridad de descuentos, para proteger la rentabilidad del negocio.

### Criterios de Aceptación

#### Scenario: Configuración válida de tope y prioridad
**Given** existen tipos de descuento disponibles en el ecommerce  
**And** el tope máximo propuesto es un valor positivo  
**And** la prioridad define un orden único entre descuentos  
**When** se registra la configuración de topes y prioridad  
**Then** la configuración queda vigente para el ecommerce  
**And** el motor utiliza ese orden para resolver descuentos concurrentes

#### Scenario: Rechazo por tope máximo inválido
**Given** existen límites de negocio para proteger la rentabilidad  
**When** se intenta registrar un tope máximo menor o igual a cero  
**Then** la configuración es rechazada  
**And** se mantiene la última configuración válida

#### Scenario: Rechazo por prioridad ambigua
**Given** la prioridad de descuentos debe ser determinística  
**When** se registra una prioridad con empates o niveles duplicados  
**Then** la configuración es rechazada  
**And** no se altera la prioridad vigente

#### Scenario: Aplicación del tope ante acumulación de descuentos
**Given** existe una configuración vigente de tope y prioridad  
**And** una transacción califica para múltiples descuentos  
**When** el descuento total calculado supera el tope máximo  
**Then** el descuento final aplicado se limita al tope máximo configurado

### Notas Técnicas (Constraints)

- **Stack:** Java 21 + Spring Boot 3.x.
- **Auth:** JWT para gestión de configuración.
- **Almacenamiento:** PostgreSQL con tablas para `discount_config`.
- **Performance:** La validación de límites debe ejecutarse en memoria para no impactar la latencia del `/calculate`.
- **Sync:** Sincronización de configuración vía RabbitMQ si es necesario.
- El tope máximo debe ser un número positivo mayor a cero.
- Cada tipo de descuento debe tener un valor de prioridad único.
- La resolución de descuentos debe ser atómica para evitar condiciones de carrera.
- Se debe definir una prioridad por defecto si no existe configuración.

---

## HU-10: Clasificación Dinámica de Clientes (Loyalty Tiers)

Como Engine Service, quiero clasificar al cliente en un nivel de fidelidad (Bronce, Plata, Oro, Platino) según el payload recibido del e-commerce, para determinar qué reglas de descuento aplicarle.

### Criterios de Aceptación

#### Scenario: Clasificación exitosa
**Given** existe una matriz de reglas en la caché de Caffeine (Engine)  
**And** el payload del e-commerce es válido (ej: total_spent, order_count)  
**When** el motor evalúa el payload contra la matriz  
**Then** el cliente queda asignado a un único nivel de fidelidad (Tier)

#### Scenario: Rechazo por datos inválidos o incompletos
**When** el payload no tiene los atributos obligatorios o tiene valores negativos  
**Then** el sistema rechaza la clasificación y devuelve un error descriptivo  
**And** no se aplica ningún descuento de fidelidad

#### Scenario: Sincronización de Matriz (Admin -> Engine)
**When** el Admin actualiza la matriz de clasificación en loyalty-admin  
**Then** se publica un evento en RabbitMQ  
**And** el loyalty-engine actualiza su caché local inmediatamente

#### Scenario: Consistencia y Determinismo
**When** se evalúa el mismo payload dos veces bajo la misma configuración  
**Then** el resultado de la clasificación debe ser idéntico

### Notas Técnicas (Constraints)

- **Stack:** Java 21 (Records para el payload) + Spring Boot 3.x.
- **Auth:** Gestión de Matriz (Admin): JWT. Ejecución de Clasificación (Engine - /calculate): API Key.
- **Performance:** La clasificación debe ejecutarse en memoria (Caffeine) para no impactar la latencia del /calculate.
- **Sync:** Comunicación asíncrona vía RabbitMQ (Fanout).
- **No X-User-ID:** La identidad del e-commerce se extrae del SecurityContext tras validar la API Key.
- La matriz de clasificación debe tener al menos un nivel definido.
- Cada cliente debe tener exactamente un nivel de fidelidad asignado.
- Los atributos obligatorios deben estar documentados.
- El cache debe tener TTL configurado para evitar stale data.
- La clasificación debe ser determinística (mismo payload = mismo resultado).

---

## HU-11: Cálculo de Carrito con Descuentos Aplicados

Como ecommerce, quiero enviar el carrito y recibir el precio final con descuentos aplicados, para mostrarle al usuario final en el checkout.

### Criterios de Aceptación

#### Scenario: Cálculo exitoso de precio final
**Given** el ecommerce está autorizado para consumir el servicio  
**And** el carrito contiene ítems válidos con cantidades y precios válidos  
**And** existen reglas de descuento vigentes aplicables  
**And** existe una configuración vigente de topes y prioridad de descuentos  
**And** el carrito califica para múltiples descuentos  
**When** el ecommerce solicita el cálculo del carrito  
**Then** el servicio responde el precio final del carrito  
**And** el total incluye los descuentos aplicados según reglas vigentes y prioridades configuradas  
**And** el orden de aplicación de descuentos respeta la prioridad configurada  
**And** el descuento total no supera el tope máximo vigente  
**And** el precio final refleja ambas restricciones de negocio

#### Scenario: Carrito sin descuentos aplicables
**Given** el ecommerce está autorizado para consumir el servicio  
**And** el carrito es válido  
**And** no existen descuentos aplicables para ese carrito  
**When** el ecommerce solicita el cálculo del carrito  
**Then** el precio final es igual al subtotal del carrito  
**And** la respuesta indica que no hubo descuentos aplicados

#### Scenario: Rechazo por carrito inválido
**Given** el servicio requiere estructura mínima y datos válidos de carrito  
**When** el ecommerce envía un carrito con datos incompletos o inconsistentes  
**Then** la solicitud es rechazada  
**And** no se retorna precio final hasta contar con un carrito válido

### Notas Técnicas (Constraints)

- **Stack:** Java 21 + Spring Boot 3.x.
- **Auth:** API Key en header para consumo del Engine Service.
- **Endpoint:** POST /api/v1/engine/calculate
- **Input:** Carrito con items (product_id, quantity, unit_price), cliente payload (para clasificación).
- **Almacenamiento:** No almacenar el carrito, solo logs de la transacción.
- **Performance:** El cálculo debe ejecutarse en memoria (Caffeine cache) para mantener latencia baja.
- **Sync:** Las reglas se sincronizan desde Admin Service vía RabbitMQ.
- El descuento total se limita al tope máximo configurado.
- La prioridad determina el orden de aplicación de descuentos.
- La respuesta debe incluir: subtotal, descuento aplicado, precio final, detalle de reglas aplicadas.

---

## HU-12: Historial de Descuentos Aplicados

Como usuario de LOYALTY, quiero consultar los descuentos aplicados en las transacciones de los últimos siete días, para verificar que el motor de cálculo esté operando bajo las reglas y topes máximos configurados.

### Criterios de Aceptación

#### Scenario: Verificación exitosa de transacciones recientes
**Given** soy un usuario de LOYALTY con acceso al dashboard de mi ecommerce  
**When** accedo al módulo de "Historial de Descuentos" y filtro por los últimos 7 días  
**Then** el sistema debe mostrar una lista con: id de transacción, fecha/hora, reglas aplicadas, descuento calculado y descuento final  
**And** debe resaltar aquellas transacciones donde el descuento fue rechazado por alcanzar el tope máximo configurado

#### Scenario: Cumplimiento de privacidad de datos
**Given** que el sistema registra el uso del motor de descuentos  
**When** se visualiza el detalle de una transacción en el log  
**Then** el sistema no debe mostrar nombres, correos, ni datos de identidad del comprador final  
**And** solo se debe mostrar el payload técnico que justifica el descuento

### Notas Técnicas (Constraints)

- **Stack:** Java 21 + Spring Boot 3.x.
- **Auth:** JWT para acceso al dashboard.
- **Almacenamiento:** PostgreSQL con tablas para `transaction_logs`.
- **Retención:** Los logs se mantienen por 7 días (configurable).
- **Performance:** Consultas con paginación y filtros por fecha.
- **Privacidad:** No almacenar PII (Personally Identifiable Information) del cliente final.
- **Logs:** Incluyen: transaction_id, ecommerce_id, timestamp, payload_recibido, reglas_aplicadas, descuento_calculado, descuento_final, descuento_rechazado_por_tope.
- **UI:** Dashboard con tabla filtrable por rango de fechas.
- Las transacciones donde el descuento fue truncado por el tope deben marcarse visualmente.

---

## HU-13: Trazabilidad de Cambios de Reglas (Auditoría)

Como super admin, quiero ver el registro de los cambios de las reglas de descuento de todos los ecommerce, para identificar que usuarios realizaron modificaciones y en que momentos.

### Criterios de Aceptación

#### Scenario: Trazabilidad de modificaciones por el Super Admin
**Given** que soy un Super Admin y accedo al panel global de auditoría  
**When** consulto el historial de cambios de un ecommerce específico  
**Then** el sistema debe mostrar una tabla cronológica con: usuario que realizó el cambio, fecha/hora, regla afectada, valor anterior y valor nuevo  
**And** debe permitir filtrar por tipo de regla Temporada, Producto o Fidelidad

### Notas Técnicas (Constraints)

- **Stack:** Java 21 + Spring Boot 3.x.
- **Auth:** JWT con rol SUPER_ADMIN para acceso global.
- **Almacenamiento:** PostgreSQL con tablas para `audit_logs`.
- **Events:** Cada modificación de regla genera un evento de auditoría.
- **Retención:** Logs de auditoría se mantienen por tiempo indefinido (o configurable).
- **Sync:** Los logs se escriben de forma síncrona en Admin Service.
- **Estructura del log:** id, timestamp, user_id, ecommerce_id, rule_type, rule_id, action (CREATE/UPDATE/DELETE), old_value, new_value, ip_address.
- **Filtros:** Por ecommerce, por tipo de regla, por usuario, por rango de fechas.
- **UI:** Panel de auditoría con tabla filtrable y exportable.

---

## HU-14: Activar/Desactivar Reglas

Como usuario de LOYALTY, quiero poder activar o desactivar reglas con un solo click, para reaccionar rápidamente a cambios en el mercado sin tener que borrar la configuración.

### Criterios de Aceptación

#### Scenario: Desactivación inmediata de una regla activa
**Given** que existe una regla de descuento por Temporada en estado "activa"  
**When** el usuario hace click en el botón para cambiarlo a "Inactivo"  
**Then** el sistema debe actualizar el estado de la regla inmediatamente  
**And** el motor de cálculo debe ignorar esta regla en todas las peticiones S2S recibidas a partir de ese momento

#### Scenario: Reactivación de regla sin pérdida de datos
**Given** que una regla de fidelidad oro fue desactivada previamente  
**When** el usuario activa nuevamente  
**Then** la regla debe volver a participar en el cálculo del motor con sus parámetros originales sin necesidad de reconfigurarla

### Notas Técnicas (Constraints)

- **Stack:** Java 21 + Spring Boot 3.x.
- **Auth:** JWT para gestión de reglas.
- **Almacenamiento:** PostgreSQL con campo `status` (ACTIVE/INACTIVE) en tablas de reglas.
- **Performance:** Cambio de estado debe reflejarse inmediatamente en caché del Engine Service.
- **Sync:** Publicación de evento de cambio de estado vía RabbitMQ al Engine Service.
- **Estados:** ACTIVE, INACTIVE.
- La activación/desactivación es una operación de cambio de estado, no elimina los datos.
- Las reglas inactivas no participan en el cálculo de descuentos.
- El historial de cambios debe registrar estas activaciones/desactivaciones (HU-11).

---

## HU-15: Registro de Ecommerces (Onboarding)

Como Super Admin, quiero registrar y gestionar los ecommerces en la plataforma, para habilitar un entorno aislado (tenant) donde cada cliente pueda operar de forma segura.

### Criterios de Aceptación

#### Scenario: Registro exitoso de un nuevo ecommerce
**Given** soy un Super Admin autenticado  
**When** registro un nuevo ecommerce con un nombre (ej: "Tienda Nike") y un identificador único o slug (ej: "nike-store")  
**Then** el sistema genera un UUID único para ese ecommerce  
**And** el estado inicial es "Activo"  
**And** el ecommerce ya aparece disponible para vincularle usuarios en la HU 2

#### Scenario: Rechazo por identificador duplicado
**Given** ya existe un ecommerce con el slug "nike-store"  
**When** intento registrar otro con el mismo identificador  
**Then** el sistema rechaza el registro e informa que el nombre ya está en uso

#### Scenario: Desactivación de un ecommerce (Baja del servicio)
**Given** un ecommerce está registrado y activo  
**When** el Super Admin cambia su estado a "Inactivo"  
**Then** todos los Usuarios (HU 2) vinculados a ese ecommerce pierden el acceso al dashboard de inmediato  
**And** las API Keys (HU 3) asociadas dejan de validar transacciones en el motor

### Notas Técnicas (Constraints)

- **Identificador único:** El campo `slug` debe ser único a nivel de sistema.
- **UUID:** Cada ecommerce tiene un UUIDv4 generado automáticamente.
- **Estado inicial:** Al crear un ecommerce, el estado por defecto es "Activo".
- **Estados posibles:** "Activo", "Inactivo".
- **Aislamiento:** Cada ecommerce funciona como tenant independiente.
- **Cascada:** Al desactivar un ecommerce, se debe invalidar tokens de usuarios y revocar acceso a API Keys.
- **Stack:** Java 21 + Spring Boot 3.x.
- **Referencia HU 2:** Ver `ecommerce-users.md` para gestión de usuarios por ecommerce.
- **Referencia HU 3:** Ver `api-keys.md` para gestión de API Keys.

---

## HU-16: Acceso Total (Super Admin)

**Como** SUPER_ADMIN
**Quiero** tener acceso total a la plataforma  
**Para** gestionar todos los ecommerce registrados, crear/modificar/eliminar admins de cada marca y configurar el sistema global sin estar vinculado a un ecommerce específico.

### Criterios de Aceptación

#### Scenario: SUPER_ADMIN crea STORE_ADMIN para un ecommerce
**Given** existe un usuario con rol SUPER_ADMIN  
**And** un ecommerce contrató los servicios de LOYALTY  
**When** el SUPER_ADMIN crea un STORE_ADMIN para ese ecommerce  
**Then** el STORE_ADMIN queda vinculado al ecommerce  
**And** el STORE_ADMIN puede gestionar usuarios estándar de su ecommerce

#### Scenario: Listar todos los ecommerce
**Given** soy un SUPER_ADMIN con acceso al sistema  
**When** consulto todos los ecommerce registrados  
**Then** el sistema muestra la lista de todos los ecommerce  
**And** puedo gestionar los STORE_ADMIN de cada ecommerce

#### Scenario: Modificar STORE_ADMIN de un ecommerce
**Given** soy un SUPER_ADMIN  
**And** existe un STORE_ADMIN asociado a un ecommerce  
**When** modifico los datos del STORE_ADMIN   
**Then** el sistema actualiza la información del STORE_ADMIN  
**And** el STORE_ADMIN mantiene la vinculación con su ecommerce

#### Scenario: Cambiar usuario de ecommerce
**Given** soy un SUPER_ADMIN  
**And** existe un usuario (STORE_ADMIN o STORE_USER) asociado a un ecommerce  
**When** cambio el ecommerce asociado al usuario  
**Then** el sistema actualiza la vinculación  
**And** el usuario ahora pertenece al nuevo ecommerce

#### Scenario: Eliminar STORE_ADMIN de un ecommerce
**Given** soy un SUPER_ADMIN  
**And** existe un STORE_ADMIN asociado a un ecommerce  
**When** elimino el STORE_ADMIN  
**Then** el sistema elimina el perfil de forma permanente  
**And** el STORE_ADMIN ya no puede acceder al sistema

### Notas Técnicas (Constraints)

- **Acceso global:** SUPER_ADMIN no está vinculado a ningún ecommerce específico.
- **Aislamiento:** No aplica filtro por `ecommerce_id` para SUPER_ADMIN.
- **Auth:** JWT con claim `role: SUPER_ADMIN`.
- **Stack:** Java 21 + Spring Boot 3.x.
- **Validación:** SUPER_ADMIN puede gestionar todos los ecommerce sin restricciones.


