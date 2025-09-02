package live.maquq.api.clan

import live.maquq.api.LazyReference
import live.maquq.api.User
import org.bson.codecs.pojo.annotations.BsonId
import java.util.*

enum class ClanRole {
    LEADER,
    COLEADER,
    MEMBER
}

data class Clan(
    @param:BsonId val tag: String,
    var ownerUuid: UUID,
    var members: MutableMap<UUID, ClanRole>,
    var maxSize: Int,
    var pointsMultiplier: Double
)