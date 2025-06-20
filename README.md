# 🛠️ Servidor Ktor - Chat y Partidas

Este es el servidor backend desarrollado con **Ktor** para gestionar **chat en tiempo real** y **partidas online** en el juego **CustomBlocks**. El servidor maneja conexiones WebSocket para facilitar la comunicación entre jugadores, la creación de partidas y la gestión de clanes.

---

## 🧩 Endpoints de WebSocket

### 1. **/clan-chat/{clanId}**
- **Método**: WebSocket
- **Descripción**: Permite a los jugadores unirse a un chat de clan y enviar/recibir mensajes en tiempo real.
- **Parámetros**:
    - `clanId`: Identificador del clan.
- **Acciones**:
    - Los jugadores se conectan a este endpoint para interactuar en el chat del clan especificado.

### 2. **/crear-partida/{jugadorId}**
- **Método**: WebSocket
- **Descripción**: Permite a un jugador crear una nueva partida con un modo específico y detalles de la partida.
- **Parámetros**:
    - `jugadorId`: Identificador del jugador que crea la partida.
- **Acciones**:
    - El jugador puede crear una partida con un modo (ej. clásico, álgebra, etc.) y esperar que otro jugador se una.
    - El jugador puede cancelar la partida en cualquier momento.

### 3. **/buscar-partida/{jugadorId}**
- **Método**: WebSocket
- **Descripción**: Permite a los jugadores buscar partidas disponibles para unirse.
- **Parámetros**:
    - `jugadorId`: Identificador del jugador que está buscando una partida.
- **Acciones**:
    - Devuelve una lista de partidas disponibles que están esperando jugadores.
    - Mantiene la conexión abierta para actualizaciones en tiempo real de las partidas.

### 4. **/unirse-partida/{creadorId}/{unidorId}**
- **Método**: WebSocket
- **Descripción**: Permite a un jugador unirse a una partida creada por otro jugador.
- **Parámetros**:
    - `creadorId`: Identificador del jugador que creó la partida.
    - `unidorId`: Identificador del jugador que se une a la partida.
- **Acciones**:
    - El jugador puede unirse a una partida esperando y la partida comienza con los datos correspondientes.
    - La bolsa de piezas y la configuración de la partida se envían a ambos jugadores.

### 5. **/partida-en-curso/{jugadorId}**
- **Método**: WebSocket
- **Descripción**: Permite a los jugadores en una partida en curso intercambiar información en tiempo real.
- **Parámetros**:
    - `jugadorId`: Identificador del jugador que participa en la partida.
- **Acciones**:
    - Envía y recibe mensajes relacionados con el estado de la partida.
    - Maneja la sincronización de acciones entre los jugadores (ej. movimientos, puntuaciones, etc.).

---

## 🚀 Requisitos

Antes de comenzar, asegúrate de tener lo siguiente instalado:

- **JDK 11 o superior**: El proyecto está basado en Kotlin y requiere un JDK compatible.
- **Kotlin 1.5 o superior**: Para ejecutar el código Kotlin.
- **Gradle**: Para manejar las dependencias del proyecto.

---

## 📂 Estructura del Proyecto

```text
src/
├── clases/            # Clases principales para la gestión de las partidas y el chat
├── constantes/        # Constantes y configuraciones globales (API, modos de juego)
├── utils/             # Funciones utilitarias (ej. generación de bolsas)
├── main.kt            # Entrada principal para iniciar el servidor
