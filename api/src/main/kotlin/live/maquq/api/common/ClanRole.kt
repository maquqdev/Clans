package live.maquq.api.common

import kotlinx.serialization.Serializable

@Serializable
enum class ClanRole {
    LEADER,
    COLEADER,
    MEMBER
}