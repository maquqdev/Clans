package live.maquq.api

import live.maquq.api.clan.Clan
import java.util.UUID

interface DataSource {
    fun connect()
    fun disconnect()

    suspend fun loadUser(uuid: UUID): User?
    suspend fun saveUser(user: User)
    suspend fun removeUser(user: User)

    suspend fun loadClan(tag: String): Clan?
    suspend fun saveClan(clan: Clan)
    suspend fun deleteClan(tag: String)
    suspend fun getAllClans(): List<Clan>
}