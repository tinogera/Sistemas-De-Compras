# Sistema de Solicitudes de Compra

**TP · Guía para el grupo**

Qué hace, cómo funciona y cómo lo vamos a armar.

---

## ¿De qué se trata?

Hoy en la empresa, cuando alguien necesita comprar algo o contratar un servicio, lo pide como puede: un mail, un mensaje, un Excel. Nadie sabe bien en qué quedó cada pedido, a quién se le compró ni cuánto se pagó.

La idea del sistema es ordenar eso. Cualquier empleado carga lo que necesita en un solo lugar, una persona de compras se encarga de conseguirlo al mejor precio, quien corresponda aprueba la compra según las reglas de la empresa, y todo queda registrado.

En una frase: alguien pide, el Encargado de compras cotiza y negocia, las personas que indican las reglas aprueban (o rechazan), el Encargado compra, y el sistema genera la orden de pedido.

## Los usuarios del sistema

Hay cuatro tipos de usuario:

| Quién | Qué hace |
|---|---|
| **Solicitante** | Cualquier empleado. Carga lo que necesita, puede corregirlo mientras nadie lo haya tocado todavía, y se entera del resultado. |
| **Encargado de compras** | Recibe todos los pedidos, busca proveedores, pide y negocia precios, carga el precio, puede rechazar lo que no corresponde y, cuando está aprobado, hace la compra. Ya no aprueba: eso lo deciden las reglas (ver más abajo). Puede haber más de uno (por ejemplo, el titular y un suplente para cuando se va de vacaciones). |
| **Supervisor** | Es el rol de una persona que puede variar con el tiempo. Aprueba o rechaza compras cuando una regla lo pide (hoy, el primer paso de toda compra). |
| **Administrador** | Da de alta usuarios y sectores, decide quién es Encargado de compras o Supervisor, y **define las reglas de aprobación**. |

Además, un **sector** entero puede ser aprobador. Por ejemplo, "Gerencia General" no es un rol: cualquier persona de ese sector puede aprobar los pasos que le corresponden.

## Cómo funciona, con un ejemplo

Laura, de Recursos Humanos, organiza el día de la familia y necesita **un pelotero** y **200 globos**.

1. **Laura carga la solicitud.** Pone los dos ítems (el pelotero es un servicio, los globos son un producto), marca urgencia Alta y la fecha para cuándo lo necesita. Para cada ítem pone un **precio estimado**: ya lo habló con su jefe y averiguó precios. Si quiere, adjunta una foto o una especificación.
2. **Si se equivocó, lo puede arreglar.** Mientras ningún Encargado haya tomado el pedido, Laura puede editar o borrar ítems, o cancelar toda la solicitud. Si edita un ítem que ya empezó a aprobarse, la aprobación vuelve a empezar.
3. **El Encargado toma cada ítem.** Lo ve en su bandeja, ordenada por urgencia. Al tomarlo queda asignado a él, así otro Encargado no hace lo mismo dos veces.
4. **Busca proveedores.** El sistema le muestra los proveedores que ya conocemos para esa categoría. Si no hay ninguno (nunca alquilamos un pelotero), busca uno por su cuenta y lo da de alta.
5. **Pide precios y los carga.** Habla con los proveedores por fuera del sistema (teléfono, mail) y carga cada cotización: precio, moneda (pesos o dólares), hasta cuándo vale ese precio y el presupuesto en PDF.
6. **Fija el precio.** Elige una cotización o carga a mano el precio total que negoció. Ese precio se ve **en tiempo real** para todos los que miran el ítem. Si el pedido no corresponde (por ejemplo, "hay globos en el depósito"), lo rechaza explicando el motivo.
7. **Se aprueba.** Desde que Laura cargó el pedido, el sistema aplica las reglas de aprobación según el monto (el precio total si ya está cargado; si no, el estimado) y pide las aprobaciones que correspondan: por ejemplo, primero el Supervisor y después Gerencia General. Mientras tanto el Encargado puede seguir buscando precios y hablando con proveedores, pero **no puede comprar**. Cualquier aprobador puede rechazar el ítem, con motivo.
8. **Se compra y se genera la orden.** Cuando el ítem tiene todas las aprobaciones, el Encargado lo ve como "para comprar", hace la compra y la registra: ahí el sistema genera la orden de pedido de ese ítem, sin esperar al resto. El Encargado se la manda al proveedor y la marca como enviada. Si después hay cambios en el precio, o hay un cargo de envío, los carga y queda en el historial.
9. **Laura se entera.** Cuando todos sus ítems están resueltos, le llega un aviso con qué se compró (y a qué proveedor) y qué se rechazó (quién lo rechazó y por qué).

Lo importante: cada ítem se maneja por separado. En el ejemplo, el pelotero puede comprarse y los globos rechazarse.

## Qué guarda el sistema

### Solicitud

Es el pedido completo de una persona. Adentro tiene uno o varios ítems.

| Dato | ¿Obligatorio? | Comentario |
|---|---|---|
| Quién la pidió | Sí | Se completa solo |
| Sector | Sí | El sector de quien pide |
| Urgencia | Sí | Baja, Media o Alta |
| Fecha necesaria | Sí | Para cuándo se necesita |
| Observaciones | No | Cualquier aclaración general |

Tiene que tener al menos un ítem.

### Ítem

Cada cosa que se pide dentro de la solicitud. Es lo que realmente se cotiza, se aprueba, se compra o se rechaza.

| Dato | ¿Obligatorio? | Comentario |
|---|---|---|
| Tipo | Sí | Producto o Servicio |
| Categoría | Sí | Por ejemplo "Plomería" o "Resmas de papel" |
| Detalle | No | Marca, medida, lo que haga falta aclarar |
| Cantidad | Sí | Tiene que ser mayor a 0 |
| Encargado asignado | Se completa solo | Cuando un Encargado lo toma |
| Precio estimado | Sí | Lo carga el solicitante: el total estimado del ítem, en pesos o dólares |
| Precio total | No | Lo carga el Encargado al negociar (o sale de la cotización elegida). Se puede corregir después de comprar |
| Monto de envío | No | Lo carga el Encargado después de la compra, si el proveedor cobra el envío |
| Estado de aprobación | Se completa solo | En curso o Aprobada, según las reglas |
| Cotización elegida | No | Qué proveedor y a qué precio, si se eligió una |
| Motivo de rechazo | Si se rechaza | Para que el solicitante sepa por qué y quién lo rechazó |

### Categoría

Es el rubro de lo que se pide: "Gasista", "Alquiler de pelotero", "Notebooks", etc.

Si alguien no encuentra la categoría, la escribe a mano y queda marcada como **Nueva**. Después el Encargado la confirma para que quede en la lista (o la junta con una que ya existía, si era la misma escrita distinto). Así la lista va creciendo sola sin llenarse de repetidos.

### Proveedor

Una empresa o persona que nos vende productos o nos presta servicios.

- Guarda nombre, contacto, CUIT y si está activo.
- Puede ofrecer **varias categorías a la vez**, tanto productos como servicios.
- **No guarda precios**: el precio siempre sale de una cotización, porque cambia todo el tiempo.

### Cotización

El precio que un proveedor nos pasó para un ítem.

| Dato | Comentario |
|---|---|
| Proveedor | Quién cotizó |
| Moneda | Pesos (ARS) o dólares (USD) |
| Precio unitario | Mayor a 0 |
| Precio total | Se calcula solo: precio × cantidad |
| Válida hasta | Después de esa fecha el precio ya no sirve |
| Presupuesto | El PDF que manda el proveedor (opcional pero recomendado) |

Un ítem puede tener varias cotizaciones para comparar. El Encargado elige una y esa fija el precio total del ítem (también puede cargar el precio a mano si lo negoció por teléfono). Si una cotización ya venció, no se puede elegir ni usar para comprar: hay que pedir precio de nuevo.

La cotización no es lo que se aprueba: lo que se aprueba es la compra del ítem, al precio que el Encargado fijó.

### Adjuntos

Se pueden subir archivos PDF o imágenes (hasta 10 MB):

- El solicitante, en el ítem: fotos, fichas técnicas, especificaciones.
- El Encargado, en la cotización: el presupuesto del proveedor, que queda como respaldo del precio.

### Orden de pedido

El documento que se le manda al proveedor. Se genera **una por cada ítem comprado** (es decir, aprobado y con la compra registrada), aunque varios ítems vayan al mismo proveedor. Tiene número, proveedor, cantidad, moneda, precio y, si lo hay, el monto de envío, y puede estar Generada o Enviada. Si después de comprar se corrige el precio o se agrega el envío, la orden se actualiza.

### Reglas de aprobación

Dicen quién tiene que aprobar una compra según su monto. Las define el Administrador y se pueden cambiar sin tocar el sistema.

| Dato | Comentario |
|---|---|
| Nombre | Por ejemplo "Compras grandes" |
| Condición | Hoy, un monto: "mayor a 500.000 pesos", "hasta 50.000 pesos". El sistema queda preparado para sumar otras (categoría, sector, urgencia) |
| Pasos | Quién aprueba y en qué orden. Cada paso lo cumple un **rol** (Supervisor) o un **sector** (Gerencia General). Pasos con el mismo número se aprueban a la vez; el siguiente se activa cuando todos aprobaron |
| Prioridad | Si dos reglas se cumplen, gana la de menor número |
| Activa | Se puede desactivar sin borrarla |

- **Sin pasos, la regla aprueba sola.** Sirve para "las compras de menos de X se aceptan directo".
- **Siempre hay una regla por defecto**, sin condiciones, que se aplica cuando ninguna otra corresponde. No se puede desactivar ni dejar sin pasos. Hoy: `Supervisor`.
- **Ejemplo.** "Compras grandes": monto mayor a 500.000 pesos → primero Supervisor, después Gerencia General. "Compras chicas": hasta 50.000 pesos → se aprueba sola. Una compra de 200.000 cae en la regla por defecto (solo Supervisor).
- **Mañana** la empresa decide que todo pase también por Gerencia General: el Administrador edita los pasos de la regla por defecto (`Supervisor → Gerencia General`) y listo.
- Los montos en pesos y en dólares no se convierten: una regla en pesos solo mira importes en pesos. Para dólares se crea otra regla.
- Si el Encargado cambia el precio y el nuevo monto exige otra aprobación, se piden las que faltan (las que ya aprobaron se conservan). Si cambia el ítem el solicitante, la aprobación vuelve a empezar.
- Los pedidos que ya están en curso conservan los pasos que tenían, aunque después se cambie la regla.
- Nadie aprueba su propio pedido.

### Historial

Todo lo que pasa queda anotado: quién cambió qué, cuándo y por qué. Sirve para saber siempre en qué quedó cada pedido.

## Los estados

### Estados de un ítem

```
Pendiente ──► En cotización ──► Comprado
    │               │
    │               └──────────► Rechazado
    ├──────────────────────────► Rechazado
    └──────────────────────────► Cancelado (lo borró el solicitante)
```

- **Pendiente**: recién cargado, nadie lo tomó. El solicitante todavía lo puede editar.
- **En cotización**: un Encargado lo tomó y está pidiendo precios. Ya no se puede editar.
- **Comprado**: el ítem tenía todas las aprobaciones y el Encargado registró la compra. Se genera la orden.
- **Rechazado**: tiene motivo y se sabe quién lo rechazó (el Encargado o un aprobador). Se puede rechazar sin haber cotizado.
- **Cancelado**: el solicitante lo sacó antes de que lo tomaran.

Comprado, Rechazado y Cancelado son finales: de ahí no se vuelve.

Además, cada ítem tiene un **estado de aprobación** que corre en paralelo: **En curso** (faltan aprobaciones) o **Aprobada** (todas las que pide la regla). "Aprobado" ya no es un estado del ítem: un ítem En cotización con la aprobación Aprobada está **listo para comprar**. Aunque ya esté aprobado, el Encargado tiene que tomarlo antes de comprarlo.

### Estados de una solicitud

La solicitud no se cambia a mano: su estado depende de sus ítems.

- **En revisión**: todavía queda algún ítem sin resolver.
- **Lista**: todos los ítems están resueltos (comprados, rechazados o cancelados). Un ítem aprobado que todavía no se compró no está resuelto. Acá se avisa al solicitante.
- **Cerrada**: está Lista y todas sus órdenes ya se mandaron a los proveedores.
- **Cancelada**: el solicitante la canceló entera antes de que la tomaran.

## Urgencia y recordatorios

La urgencia sirve para dos cosas:

| Urgencia | Lugar en la bandeja del Encargado | Recordatorio si sigue sin resolver |
|---|---|---|
| Alta | Arriba de todo | Cada 3 días |
| Media | En el medio | Cada 3 semanas |
| Baja | Abajo | Cada 3 semanas |

- Dentro de la misma urgencia, primero aparece lo que se necesita antes.
- No hay un plazo máximo para resolver: los recordatorios siguen hasta que se resuelva.
- El recordatorio le llega al Encargado que tiene el ítem. Si nadie lo tomó, les llega a todos.

## Reportes

Además de las reglas de aprobación, los reportes son la forma de controlar cuánto se gasta. Cuentan lo **comprado** (lo que tiene orden), no lo estimado ni lo que solo está aprobado:

- Gasto **por sector**.
- Gasto **por categoría**.
- Gasto **por proveedor**.
- **Tiempo promedio** que tarda en resolverse un ítem, en general y por urgencia.

Los montos en pesos y en dólares se muestran siempre por separado, nunca sumados. El monto de envío se muestra aparte del precio de lo comprado. El tiempo de resolución incluye la espera de las aprobaciones.

## Qué NO hace el sistema (por ahora)

- No se negocia con el proveedor adentro del sistema: eso se hace por teléfono o mail (el Encargado carga acá el resultado).
- No controla stock, ni la entrega de la mercadería, ni pagos o facturas.
- No hay conversión de pesos a dólares: las reglas por monto miran cada moneda por separado.
- Los proveedores no entran al sistema.

## Cómo lo vamos a armar

### La idea general

Hacemos **una sola aplicación en Java**, pero bien dividida por dentro en partes (módulos), cada una con su tarea. A esto se le llama **monolito modular**.

¿Por qué no microservicios? Porque con unos cientos de pedidos por mes no hace falta y complica mucho (varios deploys, comunicación por red, errores entre servicios). Igual lo armamos prolijo: si algún día una parte necesita crecer sola, se puede separar sin rehacer todo.

Las reglas para que quede prolijo:

- Cada módulo tiene sus propias tablas y no toca las de otro.
- Los módulos no se llaman directamente entre sí: se avisan cosas con **eventos**. Por ejemplo, cuando el Encargado registra la compra de un ítem aprobado, Compras "grita" `CompraRegistrada` y el módulo de Órdenes, que está escuchando, genera la orden.

### Los módulos

| Módulo | De qué se encarga | Avisa | Escucha |
|---|---|---|---|
| **Solicitudes** | Cargar, editar y cancelar solicitudes e ítems. Calcula el estado de la solicitud. | `SolicitudCreada`, `SolicitudLista`, `SolicitudCerrada` | `ItemResuelto`, `OrdenEnviada`, `AprobacionCompletada` |
| **Catálogo** | Proveedores, categorías y qué vende cada proveedor. | `CategoriaCreada` | — |
| **Compras** | Todo el trabajo del Encargado: tomar ítems, cargar cotizaciones, fijar el precio, rechazar y registrar la compra (y corregirla después). | `ItemAsignado`, `PrecioTotalActualizado`, `CompraRegistrada`, `CompraActualizada`, `ItemResuelto` | `SolicitudCreada`, `AprobacionCompletada`, `AprobacionReabierta`, `AprobacionRechazada` |
| **Aprobaciones** | Las reglas del Administrador y la cadena de pasos de cada ítem: quién tiene que aprobar y en qué orden. | `PasoAprobacionActivado`, `PasoDecidido`, `AprobacionCompletada`, `AprobacionReabierta`, `AprobacionRechazada`, `ReglaModificada` | `SolicitudCreada`, `PrecioTotalActualizado`, `ItemEditado`, `ItemResuelto` |
| **Órdenes** | Genera la orden cuando se registra la compra de un ítem, la actualiza si se corrige, y registra cuando se envía. | `OrdenGenerada`, `OrdenEnviada` | `CompraRegistrada`, `CompraActualizada` |
| **Reportes** | Arma los números de gasto y tiempos con su propia copia de los datos. | — | `ItemResuelto`, `OrdenGenerada`, `CompraActualizada` |
| **Integraciones** | Le pasa a n8n lo que tiene que avisar, y avisa a las pantallas abiertas cuando cambia un precio o una aprobación (tiempo real). No tiene lógica de negocio. | — | `SolicitudCreada`, `SolicitudLista`, `PasoAprobacionActivado` y los eventos de precio y aprobación |

### El recorrido completo

```
Solicitudes ──SolicitudCreada──┬─► Compras ──CompraRegistrada──► Órdenes
     ▲                         │      ▲  │                          │
     │                         │      │  └─PrecioTotalActualizado─┐  │ OrdenEnviada
     │                         └─► Aprobaciones ◄────────────────┘  │
     │                                │  (AprobacionCompletada /     │
     │                                │   AprobacionRechazada)       │
     └──────── ItemResuelto ──────────┴─ vuelve a Compras ───────────┘
```

Compras y Aprobaciones trabajan **a la vez** sobre el mismo ítem: Compras negocia el precio, Aprobaciones junta las aprobaciones. Compras solo puede registrar la compra cuando Aprobaciones avisó que está todo aprobado.

Cuando todos los ítems están resueltos:

```
Solicitudes ──SolicitudLista──► Integraciones ──► n8n ──► mail al solicitante
```

### ¿Y n8n para qué?

n8n es una herramienta aparte para automatizar avisos. No guarda nada del negocio: los datos siempre están en nuestra base. Lo usamos para:

- Avisarle a los Encargados que entró una solicitud nueva.
- Avisarle a cada aprobador que le toca decidir (cuando se activa su paso, no antes).
- Mandarle el mail al solicitante cuando su solicitud está Lista.
- Mandar los recordatorios (revisa una vez por día qué ítems siguen sin resolver).

## Tecnologías

| Para qué | Qué usamos |
|---|---|
| Backend | Java + Spring Boot |
| Base de datos | PostgreSQL (una sola base, con las tablas de cada módulo separadas) |
| Acceso a datos | Spring Data JPA |
| Login y permisos | Spring Security |
| Avisos y recordatorios | n8n |
| Archivos adjuntos | Carpeta en el servidor o un almacenamiento tipo S3 |
| Tiempo real | Server-Sent Events (SSE) para que el precio y las aprobaciones se actualicen sin recargar |
| Frontend | React |
| Comunicación | API REST con JSON |

## Reglas para no olvidarse

1. Una solicitud tiene al menos un ítem, y cada ítem tiene cantidad mayor a 0.
2. El solicitante solo puede editar o borrar ítems que estén Pendientes.
3. Cada ítem tiene un solo Encargado a la vez; si otro se lo pasa, queda registrado.
4. Solo los Encargados cargan cotizaciones, fijan el precio, compran y mandan órdenes. Aprueban solo los pasos que indican las reglas. Rechazar lo puede hacer el Encargado o un aprobador.
5. Toda cotización tiene moneda y fecha de validez; si venció, no se puede elegir.
6. Comprado = tiene precio total, proveedor y todas las aprobaciones. Rechazado = tiene motivo.
7. La orden se genera en el momento en que se registra la compra de un ítem aprobado, una por ítem.
8. La solicitud pasa a Lista sola cuando todos sus ítems están resueltos, y ahí se avisa.
9. Los proveedores no guardan precios.
10. Los reportes nunca suman pesos con dólares.
11. Todo cambio queda en el historial (incluidos los cambios de reglas y las correcciones de precio después de comprar).
12. No se puede comprar un ítem hasta que estén todas las aprobaciones que pide su regla.
13. Rechazar es por ítem, con motivo; nunca por orden ni por solicitud entera.
14. Nadie aprueba su propio pedido.
15. Siempre existe una regla por defecto; no se puede desactivar ni quedar sin pasos.
16. Los pedidos en curso conservan sus pasos aunque se cambie una regla; si cambia el monto o el ítem, se vuelven a evaluar.
