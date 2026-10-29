import kotlinx.serialization.json.Json
import repro.models.Board

fun main() {
    val decoded = runCatching { Json.decodeFromString(Board.serializer(), """{"column":[null],"grid":[[null]]}""") }
    println("decoded: " + decoded.fold({ it.toString() }, { "${it::class.simpleName}: ${it.message?.lineSequence()?.first()}" }))
}
