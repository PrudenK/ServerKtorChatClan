import clases.Partida
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONObject
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

fun main() {
    embeddedServer(Netty, port = 8080) {
        install(WebSockets) {
            pingPeriod = Duration.ofSeconds(15)
        }

        routing {
            val salasPorClan = ConcurrentHashMap<Int, MutableSet<DefaultWebSocketServerSession>>()

            // WebSocket para el chat del clan
            webSocket("/clan-chat/{clanId}") {
                val clanId = call.parameters["clanId"]?.toIntOrNull()

                if (clanId == null) {
                    close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Clan ID inválido"))
                    return@webSocket
                }

                val sesiones = salasPorClan.getOrPut(clanId) { mutableSetOf() }
                sesiones.add(this)

                println("🟢 Jugador conectado al clan $clanId")

                try {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val mensaje = frame.readText()
                            val json = JSONObject(mensaje)

                            // Lógica para manejar los mensajes del chat
                            val nombre = json.getString("nombre")
                            val texto = json.getString("mensaje")

                            guardarMensajeEnSymfony(clanId, nombre, texto)

                            val mensajeFinal = JSONObject()
                                .put("nombre", nombre)
                                .put("mensaje", texto)
                                .toString()

                            println("💬 [$clanId] $mensajeFinal")
                            sesiones.forEach {
                                it.send(mensajeFinal)
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("❌ Error en conexión con el clan $clanId: ${e.message}")
                } finally {
                    sesiones.remove(this)
                    println("🔴 Jugador desconectado del clan $clanId")
                }
            }



            val partidasEsperandoJugador = ConcurrentHashMap<Int, Partida>()
            webSocket("/crear-partida/{jugadorId}") {
                val jugadorId = call.parameters["jugadorId"]?.toIntOrNull()

                if (jugadorId == null) {
                    close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Jugador ID inválido"))
                    return@webSocket
                }

                try {
                    // Recibir el mensaje con el modo de la partida
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val mensaje = frame.readText()
                            val json = JSONObject(mensaje)

                            // Obtener el modo de la partida y crear el objeto partida
                            val modo = json.getString("modo") // El modo debe ser enviado en el JSON
                            val partida = Partida(creadorId = jugadorId, modo = modo)

                            // Guardar la partida esperando
                            partidasEsperandoJugador[jugadorId] = partida
                            println("🟢 Jugador $jugadorId ha creado una partida en modo $modo y está esperando oponente")

                            // Enviar confirmación de que la partida fue creada correctamente
                            val confirmacion = JSONObject()
                                .put("creadorId", jugadorId)
                                .put("modo", modo)
                                .toString()
                            send(confirmacion)
                        }
                    }
                } catch (e: Exception) {
                    println("❌ Error en conexión con el jugador $jugadorId: ${e.message}")
                } finally {
                    partidasEsperandoJugador.remove(jugadorId)
                    println("🔴 Jugador $jugadorId desconectado de la creación de partida")
                }
            }




            /*
            // WebSocket para aceptar una partida
            webSocket("/aceptar-partida/{jugadorId}") {
                val jugadorId = call.parameters["jugadorId"]?.toIntOrNull()

                if (jugadorId == null) {
                    close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Jugador ID inválido"))
                    return@webSocket
                }

                println("🟢 Jugador $jugadorId buscando aceptar una partida")

                try {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val mensaje = frame.readText()
                            val json = JSONObject(mensaje)

                            // Obtener el ID del jugador creador
                            val jugadorCreadorId = json.getInt("jugadorCreadorId")
                            val creadorSesion = partidasEsperandoJugador[jugadorCreadorId]

                            if (creadorSesion != null) {
                                // Emparejar a los jugadores
                                val mensajeInicio = JSONObject()
                                    .put("accion", "inicio")
                                    .toString()

                                // Enviar mensaje de inicio a ambos jugadores
                                this.send(mensajeInicio)
                                creadorSesion.send(mensajeInicio)
                                println("🟢 Jugadores emparejados: $jugadorCreadorId y $jugadorId")
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("❌ Error en conexión con el jugador $jugadorId: ${e.message}")
                }
            }

             */

        }
    }.start(wait = true)
}

// Método para guardar el mensaje del chat en Symfony
fun guardarMensajeEnSymfony(clanId: Int, remitente: String, mensaje: String) {
    val url = ApiCustom.PATH_CUSTOM + ApiCustom.GUARDAR_MENSAJE_CLAN
    val json = """
        {
            "clanId": $clanId,
            "remitente": "$remitente",
            "mensaje": "$mensaje"
        }
    """.trimIndent()

    val client = HttpClient.newHttpClient()
    val request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(json))
        .build()

    client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        .thenAccept { response ->
            println("📨 Guardado en Symfony: ${response.statusCode()} - ${response.body()}")
        }
        .exceptionally { e ->
            println("❌ Error al guardar en Symfony: ${e.message}")
            null
        }
}
