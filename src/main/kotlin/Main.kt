import clases.Partida
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import org.json.JSONArray
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
            val sesionesDePartidas = ConcurrentHashMap<Int, DefaultWebSocketServerSession>()
            val partidasEsperandoJugador = ConcurrentHashMap<Int, Partida>()


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


            webSocket("/crear-partida/{jugadorId}") {
                val jugadorId = call.parameters["jugadorId"]?.toIntOrNull() ?: return@webSocket

                sesionesDePartidas[jugadorId] = this

                var modo = "Clásico"
                var nombre = ""
                var nivel = -1
                var foto = ""

                try {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val mensaje = frame.readText()
                            val json = JSONObject(mensaje)

                            if (json.has("modo")) {
                                modo = json.getString("modo")
                                nombre = json.getString("nombre")
                                nivel = json.getInt("nivel")
                                foto = json.getString("foto")

                                partidasEsperandoJugador[jugadorId] = Partida(jugadorId, nombre, modo, nivel, foto)
                                println("🟢 $nombre ($jugadorId) ha creado partida en modo $modo")

                                send(JSONObject().put("creadorId", jugadorId)
                                    .put("modo", modo)
                                    .put("nombre", nombre)
                                    .put("nivel", nivel)
                                    .put("foto", foto)
                                    .toString())
                            }

                            // ⛔️ Si no hay más mensajes, este for termina y CIERRA el WebSocket.
                            // Así que dejamos el bucle sin break ni return para mantenerlo abierto.
                        }
                    }
                } catch (e: Exception) {
                    println("❌ Error con jugador $jugadorId: ${e.message}")
                } finally {
                    println("🔴 Jugador $jugadorId desconectado")
                    partidasEsperandoJugador.remove(jugadorId)
                    sesionesDePartidas.remove(jugadorId)
                }
            }






            webSocket("/buscar-partida/{jugadorId}") {
                val jugadorId = call.parameters["jugadorId"]?.toIntOrNull()

                if (jugadorId == null) {
                    close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Jugador ID inválido"))
                    return@webSocket
                }

                try {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            // Puedes usar este ciclo para enviar las partidas esperando
                            val partidasJson = JSONArray()
                            partidasEsperandoJugador.values.forEach { partida ->
                                val json = JSONObject()
                                    .put("creadorId", partida.creadorId)
                                    .put("modo", partida.modo)
                                    .put("nombre", partida.nombre)
                                    .put("foto", partida.foto)
                                    .put("nivel", partida.nivel)
                                partidasJson.put(json)
                            }

                            // Enviar la lista de partidas disponibles
                            send(partidasJson.toString())

                            // Mantener la conexión abierta para actualizaciones
                        }
                    }
                } catch (e: Exception) {
                    println("❌ Error en búsqueda de partidas para el jugador $jugadorId: ${e.message}")
                }
            }



            webSocket("/unirse-partida/{creadorId}/{unidorId}") {
                val creadorId = call.parameters["creadorId"]?.toIntOrNull()
                val unidorId = call.parameters["unidorId"]?.toIntOrNull()

                if (creadorId == null || unidorId == null) {
                    close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "IDs inválidos"))
                    return@webSocket
                }

                val sesionCreador = sesionesDePartidas[creadorId]
                if (sesionCreador == null) {
                    close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "El creador no está disponible"))
                    return@webSocket
                }

                // Enviar datos de inicio de partida a ambos
                val jsonInicio = JSONObject()
                    .put("creadorId", creadorId)
                    .put("unidorId", unidorId)
                    .put("mensaje", "iniciarPartida")
                    .put("modo", partidasEsperandoJugador[creadorId]!!.modo)

                sesionCreador.send(jsonInicio.toString())
                send(jsonInicio.toString()) // al unidor

                println("🟢 Partida iniciada entre $creadorId y $unidorId")

                // Limpiar de listas
                partidasEsperandoJugador.remove(creadorId)
                sesionesDePartidas.remove(creadorId)
            }




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
