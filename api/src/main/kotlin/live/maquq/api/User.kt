package live.maquq.api

import live.maquq.api.clan.Clan
import org.bson.codecs.pojo.annotations.BsonId
import java.util.*

data class User(
    @BsonId val uuid: UUID,
    var kills: Int = 0,
    var deaths: Int = 0,
    var points: Int = 0,
    var clanTag: String? = null
) {
    @Transient
    private lateinit var clanLoader: suspend (String?) -> Clan?

    @delegate:Transient
    val clan: Clan? by LazyReference { clanLoader(clanTag) }

    fun init(clanLoader: suspend (String?) -> Clan?) {
        this.clanLoader = clanLoader
    }
}