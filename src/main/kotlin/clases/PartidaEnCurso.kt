package clases

import io.ktor.server.websocket.*

data class PartidaEnCurso(
    val creadorId: Int,
    val unidorId: Int,
    val bolsa: List<String>,
    val sesionCreador: DefaultWebSocketServerSession,
    val sesionUnidor: DefaultWebSocketServerSession,
    var puntosCreador: Int = 0,
    var puntosUnidor: Int = 0,
    var nivelCreador: Int = 1,
    var nivelUnidor: Int = 1,
    var lineasCreador: Int = 0,
    var lineasUnidor: Int = 0,
)