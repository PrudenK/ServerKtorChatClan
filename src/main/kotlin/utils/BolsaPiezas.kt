package utils

fun generarBolsaIndices(piezasPermitidas: List<Int>, repeticiones: Int = 150): List<Int> {
    val bolsa = mutableListOf<Int>()
    repeat(repeticiones) {
        val copia = piezasPermitidas.toMutableList()
        copia.shuffle()
        bolsa.addAll(copia)
    }
    return bolsa
}
