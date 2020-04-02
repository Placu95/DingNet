package it.unibo.acdingnet.protelis.physicalnetwork

enum class HostType { CLOUD, EDGE, LEAF }

data class Host(
    val id: String,
    val type: HostType,
    val bandWidth: Int,
    private var devicesNumber: Int = 0
) {

    override fun equals(other: Any?): Boolean = when (other) {
        is Host -> id == other.id
        else -> false
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }
}
