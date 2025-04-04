package utils

fun generarBolsaIndices(piezasPermitidas: List<Int>, repeticiones: Int = 50): List<Int> {
    val bolsa = mutableListOf<Int>()
    repeat(repeticiones) {
        val copia = piezasPermitidas.toMutableList()
        copia.shuffle()
        bolsa.addAll(copia)
    }
    return bolsa
}
