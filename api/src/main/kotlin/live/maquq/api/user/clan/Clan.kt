package live.maquq.api.user.clan

import org.bson.codecs.pojo.annotations.BsonId
import java.util.*

data class Clan(
    @param:BsonId val tag: String,
    var ownerUuid: UUID,
    var members: MutableMap<UUID, ClanRole>,
    var maxSize: Int,
    var pointsMultiplier: Double = 1.0,
    var pvpEnabled: Boolean = true,
    var pvpEditMinRole: ClanRole = ClanRole.COLEADER
)