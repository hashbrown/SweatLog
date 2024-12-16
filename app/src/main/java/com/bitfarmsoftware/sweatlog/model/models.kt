package com.bitfarmsoftware.sweatlog.model

data class SweatLog(
    val id: String,
    val title: String
)

data class Device(
    val name: String,
    val address: String,
    val uuid: String, // likely not stable, this can constantly change for a device
    val isConnected: Boolean = false,
    val autoConnect: Boolean = false) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Device

        if (name != other.name) return false
        if (address != other.address) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + address.hashCode()
        return result
    }
}

data class UserData(
    val savedDevices: Set<Device>
)