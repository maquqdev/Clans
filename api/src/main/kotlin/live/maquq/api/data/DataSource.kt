package live.maquq.api.data

import live.maquq.api.user.User
import live.maquq.api.user.clan.Clan
import java.util.*

interface DataSource {
    fun connect()
    fun disconnect()

    suspend fun loadUser(uuid: UUID): User?
    suspend fun saveUser(user: User): Int
    suspend fun removeUser(user: User): Int
    suspend fun getTopUsers(limit: Int = 50): List<User>

    suspend fun loadClan(tag: String): Clan?
    suspend fun saveClan(clan: Clan)
    suspend fun deleteClan(tag: String): Int
    suspend fun getAllClans(): List<Clan>
}