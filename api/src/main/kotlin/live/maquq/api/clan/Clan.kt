package live.maquq.api.clan

import live.maquq.api.LazyReference
import live.maquq.api.User
import org.bson.codecs.pojo.annotations.BsonId
import java.util.*

enum class ClanRole {
    LEADER,
    VLEADER,
    MEMBER
}

data class Clan(
    @BsonId val tag: String,
    var ownerUuid: UUID,
    var members: MutableMap<UUID, ClanRole>
) {
    @Transient
    private lateinit var ownerLoader: suspend (UUID?) -> User?

    @delegate:Transient
    val owner: User? by LazyReference { ownerLoader(ownerUuid) }

    fun init(ownerLoader: suspend (UUID?) -> User?) {
        this.ownerLoader = ownerLoader
    }
}