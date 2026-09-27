# Sistema de Solicitudes de Compra

**TP · Guía para el grupo**

Qué hace, cómo funciona y cómo lo vamos a armar.

---

## ¿De qué se trata?

Hoy en la empresa, cuando alguien necesita comprar algo o contratar un servicio, lo pide como puede: un mail, un mensaje, un Excel. Nadie sabe bien en qué quedó cada pedido, a quién se le compró ni cuánto se pagó.

La idea del sistema es ordenar eso. Cualquier empleado carga lo que necesita en un solo lugar, una persona de compras se encarga de conseguirlo al mejor precio, y todo queda registrado.

En una frase: alguien pide, el Encargado de compras cotiza y decide, y el sistema genera la orden de pedido.

## Los usuarios del sistema

Hay tres tipos de usuario:

| Quién | Qué hace |
|---|---|
| **Solicitante** | Cualquier empleado. Carga lo que necesita, puede corregirlo mientras nadie lo haya tocado todavía, y se entera del resultado. |
| **Encargado de compras** | Recibe los pedidos, busca proveedores, pide precios, elige uno y aprueba o rechaza cada cosa. Puede haber más de uno (por ejemplo, el titular y un suplente para cuando se va de vacaciones). |
| **Administrador** | Da de alta usuarios y sectores, y decide quién es Encargado de compras. |

## Cómo funciona, con un ejemplo

Laura, de Recursos Humanos, organiza el día de la familia y necesita **un pelotero** y **200 globos**.

1. **Laura carga la solicitud.** Pone los dos ítems (el pelotero es un servicio, los globos son un producto), marca urgencia Alta y la fecha para cuándo lo necesita. Si quiere, adjunta una foto o una especificación.
2. **Si se equivocó, lo puede arreglar.** Mientras ningún Encargado haya tomado el pedido, Laura puede editar o borrar ítems, o cancelar toda la solicitud.
3. **El Encargado toma cada ítem.** Lo ve en su bandeja, ordenada por urgencia. Al tomarlo queda asignado a él, así otro Encargado no hace lo mismo dos veces.
4. **Busca proveedores.** El sistema le muestra los proveedores que ya conocemos para esa categoría. Si no hay ninguno (nunca alquilamos un pelotero), busca uno por su cuenta y lo da de alta.
5. **Pide precios y los carga.** Habla con los proveedores por fuera del sistema (teléfono, mail) y carga cada cotización: precio, moneda (pesos o dólares), hasta cuándo vale ese precio y el presupuesto en PDF.
6. **Decide.** Aprueba el ítem eligiendo una cotización, o lo rechaza explicando el motivo (por ejemplo, "hay globos en el depósito").
7. **Se genera la orden.** Apenas se aprueba un ítem, el sistema genera su orden de pedido, sin esperar al resto. El Encargado se la manda al proveedor y la marca como enviada.
8. **Laura se entera.** Cuando todos sus ítems están resueltos, le llega un aviso con qué se aprobó (y a qué proveedor) y qué se rechazó (y por qué).

Lo importante: cada ítem se maneja por separado. En el ejemplo, el pelotero puede aprobarse y los globos rechazarse.

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

Cada cosa que se pide dentro de la solicitud. Es lo que realmente se cotiza, se aprueba o se rechaza.

| Dato | ¿Obligatorio? | Comentario |
|---|---|---|
| Tipo | Sí | Producto o Servicio |
| Categoría | Sí | Por ejemplo "Plomería" o "Resmas de papel" |
| Detalle | No | Marca, medida, lo que haga falta aclarar |
| Cantidad | Sí | Tiene que ser mayor a 0 |
| Encargado asignado | Se completa solo | Cuando un Encargado lo toma |
| Cotización elegida | Si se aprueba | Qué proveedor y a qué precio |
| Motivo de rechazo | Si se rechaza | Para que el solicitante sepa por qué |

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

Un ítem puede tener varias cotizaciones para comparar. Al aprobar se elige una. Si una cotización ya venció, no se puede elegir: hay que pedir precio de nuevo.

### Adjuntos

Se pueden subir archivos PDF o imágenes (hasta 10 MB):

- El solicitante, en el ítem: fotos, fichas técnicas, especificaciones.
- El Encargado, en la cotización: el presupuesto del proveedor, que queda como respaldo del precio.

### Orden de pedido

El documento que se le manda al proveedor. Se genera **una por cada ítem aprobado**, aunque varios ítems vayan al mismo proveedor. Tiene número, proveedor, cantidad, moneda y precio, y puede estar Generada o Enviada.

### Historial

Todo lo que pasa queda anotado: quién cambió qué, cuándo y por qué. Sirve para saber siempre en qué quedó cada pedido.

## Los estados

### Estados de un ítem

```
Pendiente ──► En cotización ──► Aprobado
    │               │
    │               └──────────► Rechazado
    ├──────────────────────────► Rechazado
    └──────────────────────────► Cancelado (lo borró el solicitante)
```

- **Pendiente**: recién cargado, nadie lo tomó. El solicitante todavía lo puede editar.
- **En cotización**: un Encargado lo tomó y está pidiendo precios. Ya no se puede editar.
- **Aprobado**: tiene cotización elegida. Se genera la orden.
- **Rechazado**: tiene motivo. Se puede rechazar sin haber cotizado.
- **Cancelado**: el solicitante lo sacó antes de que lo tomaran.

Aprobado, Rechazado y Cancelado son finales: de ahí no se vuelve.

### Estados de una solicitud

La solicitud no se cambia a mano: su estado depende de sus ítems.

- **En revisión**: todavía queda algún ítem sin resolver.
- **Lista**: todos los ítems están resueltos. Acá se avisa al solicitante.
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

Como no hay aprobaciones por monto, los reportes son la forma de controlar cuánto se gasta:

- Gasto **por sector**.
- Gasto **por categoría**.
- Gasto **por proveedor**.
- **Tiempo promedio** que tarda en resolverse un ítem, en general y por urgencia.

Los montos en pesos y en dólares se muestran siempre por separado, nunca sumados.

## Qué NO hace el sistema (por ahora)

- No se negocia con el proveedor adentro del sistema: eso se hace por teléfono o mail.
- No controla stock, ni la entrega de la mercadería, ni pagos o facturas.
- No hay aprobaciones en cadena por monto o por sector.
- Los proveedores no entran al sistema.

## Cómo lo vamos a armar

### La idea general

Hacemos **una sola aplicación en Java**, pero bien dividida por dentro en partes (módulos), cada una con su tarea. A esto se le llama **monolito modular**.

¿Por qué no microservicios? Porque con unos cientos de pedidos por mes no hace falta y complica mucho (varios deploys, comunicación por red, errores entre servicios). Igual lo armamos prolijo: si algún día una parte necesita crecer sola, se puede separar sin rehacer todo.

Las reglas para que quede prolijo:

- Cada módulo tiene sus propias tablas y no toca las de otro.
- Los módulos no se llaman directamente entre sí: se avisan cosas con **eventos**. Por ejemplo, cuando se aprueba un ítem, Compras "grita" `ItemAprobado` y el módulo de Órdenes, que está escuchando, genera la orden.

### Los módulos

| Módulo | De qué se encarga | Avisa | Escucha |
|---|---|---|---|
| **Solicitudes** | Cargar, editar y cancelar solicitudes e ítems. Calcula el estado de la solicitud. | `SolicitudCreada`, `SolicitudLista`, `SolicitudCerrada` | `ItemResuelto`, `OrdenEnviada` |
| **Catálogo** | Proveedores, categorías y qué vende cada proveedor. | `CategoriaCreada` | — |
| **Compras** | Todo el trabajo del Encargado: tomar ítems, cargar cotizaciones, aprobar y rechazar. | `ItemAsignado`, `ItemAprobado`, `ItemResuelto` | `SolicitudCreada` |
| **Órdenes** | Genera la orden cuando se aprueba un ítem y registra cuando se envía. | `OrdenGenerada`, `OrdenEnviada` | `ItemAprobado` |
| **Reportes** | Arma los números de gasto y tiempos con su propia copia de los datos. | — | `SolicitudCreada`, `ItemResuelto`, `OrdenGenerada` |
| **Integraciones** | Le pasa a n8n lo que tiene que avisar. No tiene lógica de negocio. | — | `SolicitudCreada`, `SolicitudLista` |

### El recorrido completo

```
Solicitudes ──SolicitudCreada──► Compras ──ItemAprobado──► Órdenes
     ▲                              │                         │
     └──────── ItemResuelto ────────┘                         │ OrdenEnviada
     ▲                                                        │
     └────────────────────────────────────────────────────────┘
```

Cuando todos los ítems están resueltos:

```
Solicitudes ──SolicitudLista──► Integraciones ──► n8n ──► mail al solicitante
```

### ¿Y n8n para qué?

n8n es una herramienta aparte para automatizar avisos. No guarda nada del negocio: los datos siempre están en nuestra base. Lo usamos para:

- Avisarle a los Encargados que entró una solicitud nueva.
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
| Frontend | React |
| Comunicación | API REST con JSON |

## Reglas para no olvidarse

1. Una solicitud tiene al menos un ítem, y cada ítem tiene cantidad mayor a 0.
2. El solicitante solo puede editar o borrar ítems que estén Pendientes.
3. Cada ítem tiene un solo Encargado a la vez; si otro se lo pasa, queda registrado.
4. Solo los Encargados cargan cotizaciones, aprueban, rechazan y mandan órdenes.
5. Toda cotización tiene moneda y fecha de validez; si venció, no se puede elegir.
6. Aprobado = tiene cotización elegida. Rechazado = tiene motivo.
7. La orden se genera en el momento en que se aprueba el ítem, una por ítem.
8. La solicitud pasa a Lista sola cuando todos sus ítems están resueltos, y ahí se avisa.
9. Los proveedores no guardan precios.
10. Los reportes nunca suman pesos con dólares.
11. Todo cambio queda en el historial.
