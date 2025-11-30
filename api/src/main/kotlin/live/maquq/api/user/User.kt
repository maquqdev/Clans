package live.maquq.api.user

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import java.util.UUID

@Serializable
data class User(
    @BsonId @Contextual val uuid: UUID,
    var kills: Int = 0,
    var deaths: Int = 0,
    var assists: Int = 0,
    var points: Int,
    var clanTag: String? = null
)