package live.maquq.api.user

import org.bson.codecs.pojo.annotations.BsonId
import java.util.UUID

data class User(
    @BsonId val uuid: UUID,
    var kills: Int = 0,
    var deaths: Int = 0,
    var points: Int,
    var clanTag: String? = null
)