# n8n: workflows de avisos y recordatorios

Los workflows de esta carpeta mandan los mails del sistema. n8n los recibe del backend por webhook (`solicitud-creada`, `solicitud-lista`, `encargado-desasignado`, `aprobacion-pendiente`) o los dispara solo (recordatorios diarios, 9:00 hs de Argentina). Los mails de prueba se ven en Mailpit.

| Archivo | Disparador | Qué hace |
|---|---|---|
| `aviso-solicitud-nueva.json` | webhooks `solicitud-creada` y `encargado-desasignado` | Avisa a los Encargados |
| `resultado-solicitante.json` | webhook `solicitud-lista` | Mail al solicitante con el resultado de cada ítem |
| `aviso-aprobacion-pendiente.json` | webhook `aprobacion-pendiente` | Mail al aprobador al que le toca decidir |
| `recordatorios-diarios.json` | Lunes a viernes 9:00 | Pide los recordatorios al backend, manda los mails y avisa que los envió |

## Puesta en marcha

1. Completar `back/.env` (tomar `back/.env.example` de modelo). Para n8n y Mailpit: `PORT_N8N`, `PORT_WEB_N8N`, `SMTP`, `N8N_WEBHOOK_URL`, `N8N_API_KEY` y, si se quiere firmar los webhooks, `N8N_WEBHOOK_SECRET`.
2. Desde `back/`: `docker compose up -d n8n mailpit`.
   - n8n: http://localhost:5678 (la primera vez pide crear el usuario dueño).
   - Mailpit: http://localhost:8025 (acá se ven los mails).
3. En n8n, **Import from File** con cada JSON de esta carpeta.
4. Crear las dos credenciales (Credentials → Create) y asignarlas en los nodos que las piden:
   - **SMTP**: host `mailpit`, puerto `1025`, sin usuario ni contraseña, SSL/TLS apagado. Se usa en los nodos Send Email.
   - **Header Auth**: nombre `X-API-Key`, valor el mismo `N8N_API_KEY` del `.env`. Se usa en los dos nodos HTTP Request de `recordatorios-diarios`.
5. Activar los 4 workflows (switch **Active**). Con el workflow inactivo, `/webhook/...` no responde.

## Cosas a tener en cuenta

- El backend corre fuera de Docker, así que n8n lo llama como `http://host.docker.internal:8080` (ya configurado en los nodos).
- Los links a la aplicación en los mails apuntan a `http://localhost:5173`; cambiarlo en `aviso-aprobacion-pendiente` si el front corre en otro puerto.
- Si n8n está caído, el backend deja el evento pendiente y lo reintenta (no se pierden avisos).
- Nunca escribir la API key dentro de un nodo: se exporta con el workflow. Antes de commitear, revisar: `grep -i -E "password|apikey|x-api-key" n8n/workflows/*.json`.
- Si alguien cambia un workflow en n8n, tiene que volver a exportarlo (menú **…** → **Download**) a esta carpeta, con el mismo nombre de archivo, y subirlo en su PR.
