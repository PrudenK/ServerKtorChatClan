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
                    println("❌ Error en conexión: ${e.message}")
                } finally {
                    sesiones.remove(this)
                    println("🔴 Jugador desconectado del clan $clanId")
                }
            }
        }
    }.start(wait = true)
}

fun guardarMensajeEnSymfony(clanId: Int, remitente: String, mensaje: String) {
    val url = ApiCustom.PATH_CUSTOM+ApiCustom.GUARDAR_MENSAJE_CLAN
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
