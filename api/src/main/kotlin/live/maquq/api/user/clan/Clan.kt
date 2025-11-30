package live.maquq.api.user.clan

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import live.maquq.api.common.ClanRole
import org.bson.codecs.pojo.annotations.BsonId
import java.util.*

@Serializable
data class Clan(
    @param:BsonId val tag: String,
    @Contextual var ownerUuid: UUID,
    @Contextual var members: MutableMap<@Contextual UUID, ClanRole>,
    var maxSize: Int,
    var pointsMultiplier: Double = 1.0,
    var pvpEnabled: Boolean = true,
    var pvpEditMinRole: ClanRole = ClanRole.COLEADER
)