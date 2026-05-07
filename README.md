# room-service

## Alcance de eventos

`room-service` (Persona A) no consume `paper.evaluated` directamente.
El contrato RabbitMQ de `paper.evaluated` se centraliza en `scheduler-service/contracts/`.
