package com.bitfarmsoftware.sweatlog.data.ble

fun ByteArray.toInt(): Int {
    var result = 0
    for (i in this.indices) {
        result = result or ((this[i].toInt() and 0xFF) shl (8 * i))
    }
    return result
}

fun ByteArray.toHexString(): String {
    val stringBuilder = StringBuilder(this.size * 2)
    for (byte in this) {
        val hex = Integer.toHexString(byte.toInt() and 0xFF)
        if (hex.length == 1) {
            stringBuilder.append('0')
        }
        stringBuilder.append(hex)
    }
    return stringBuilder.toString().uppercase()
}

fun String.asciiToHex(): String {
    val hexStringBuilder = StringBuilder()

    for (char in this) {
        val asciiValue = char.toInt()
        val hexValue = Integer.toHexString(asciiValue)
        hexStringBuilder.append(hexValue)
    }

    return hexStringBuilder.toString()
}

fun ByteArray.toAsciiString(): String {
    return String(this, Charsets.US_ASCII)
}

fun String.hexStringToAscii(): String {
    val result = StringBuilder()
    var i = 0
    while (i < length) {
        val hex = substring(i, i + 2)
        val decimal = hex.toInt(16)
        result.append(decimal.toChar())
        i += 2
    }
    return result.toString()
}


fun String.hexStringToByteArray(): ByteArray {
    var bytesString = this.removePrefix("0x").filter { it != ':' }
    val len = bytesString.length
    val data = ByteArray(len / 2)
    var i = 0
    while (i < len) {
        data[i / 2] = ((Character.digit(bytesString[i], 16) shl 4) +
                Character.digit(bytesString[i + 1], 16)).toByte()
        i += 2
    }
    return data
}

fun String.hexStringToInt(): Int {
    return this.toLong(16).toInt()
}

fun Int.toHexString(): String {
    return Integer.toHexString(this)
}


fun String.isHexString(): Boolean {
    return this.matches("[0-9A-Fa-f]+".toRegex())
}

fun String.isAsciiString(): Boolean {
    for (char in this) {
        val asciiValue = char.toInt()
        if (asciiValue < 0 || asciiValue > 127) {
            return false
        }
    }
    return true
}


fun ByteArray.isAsciiString(): Boolean {
    for (byte in this) {
        if (!byte.isAsciiPrintable()) {
            return false
        }
    }
    return true
}

fun Byte.isAsciiPrintable(): Boolean {
    return this in 0x20..0x7E
}


fun ByteArray.parsedValue(): Any {
    if (this.size == 1) {
        try {
            return this.toInt()
        } catch (ex: Exception) {

        }
    }
    if (this.isAsciiString()) {
        return this.toAsciiString()
    } else {
        return this.toHexString()

    }
}