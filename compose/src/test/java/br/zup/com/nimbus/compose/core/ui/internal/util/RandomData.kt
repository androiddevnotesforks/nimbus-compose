package br.zup.com.nimbus.compose.core.ui.internal.util

import kotlin.random.Random

object RandomData {

    fun boolean(): Boolean = Random.nextBoolean()

    fun int(): Int = Random.nextInt(1, 10000)

    fun double(): Double = Random.nextDouble(1.0, 10000.0)

    fun float(): Float = Random.nextFloat()

    fun string(size: Int = 20): String {
        val alphabet: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return List(size) { alphabet.random() }.joinToString("")
    }

    fun email(): String = "${string(5)}@${string(3)}.com"

    fun httpUrl(): String = "http://${string(5)}.com"

    fun httpsUrl(): String = "https://${string(5)}.com"
}