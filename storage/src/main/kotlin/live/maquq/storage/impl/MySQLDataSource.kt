package live.maquq.storage.impl

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import live.maquq.api.DataSource
import live.maquq.api.clan.Clan
import live.maquq.api.clan.ClanRole
import live.maquq.api.User
import java.sql.Connection
import java.util.*

class MySqlDataSource(private val settings: Map<String, Any?>) : DataSource {

    private lateinit var hikari: HikariDataSource

    override fun connect() {
        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:mysql://${settings["host"]}:${settings["port"]}/${settings["database"]}?autoReconnect=true"
            username = settings["username"] as String
            password = settings["password"] as String
            maximumPoolSize = 10
        }
        hikari = HikariDataSource(config)
        createTables()
    }

    private fun createTables() {
        val userTableSql = """
            CREATE TABLE IF NOT EXISTS users (
                uuid VARCHAR(36) PRIMARY KEY,
                kills INT DEFAULT 0,
                deaths INT DEFAULT 0,
                points INT DEFAULT 0,
                clanTag VARCHAR(16)
            );
        """.trimIndent()
        val clanTableSql = """
            CREATE TABLE IF NOT EXISTS clans (
                tag VARCHAR(16) PRIMARY KEY,
                ownerUuid VARCHAR(36) NOT NULL
            );
        """.trimIndent()
        val membersTableSql = """
            CREATE TABLE IF NOT EXISTS clan_members (
                clan_tag VARCHAR(16) NOT NULL,
                user_uuid VARCHAR(36) NOT NULL,
                role VARCHAR(16) NOT NULL,
                PRIMARY KEY (clan_tag, user_uuid)
            );
        """.trimIndent()

        hikari.connection.use { conn ->
            conn.createStatement().use {
                it.execute(userTableSql)
                it.execute(clanTableSql)
                it.execute(membersTableSql)
            }
        }
    }

    override fun disconnect() {
        if (this::hikari.isInitialized && !hikari.isClosed) {
            hikari.close()
        }
    }

    private suspend fun <T> transaction(block: suspend (Connection) -> T): T = withContext(Dispatchers.IO) {
        hikari.connection.use { conn ->
            try {
                conn.autoCommit = false
                val result = block(conn)
                conn.commit()
                result
            } catch (e: Exception) {
                conn.rollback()
                throw e
            }
        }
    }

    override suspend fun loadUser(uuid: UUID): User? = withContext(Dispatchers.IO) {
        hikari.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM users WHERE uuid = ?").use { stmt ->
                stmt.setString(1, uuid.toString())
                stmt.executeQuery().use { rs ->
                    if (!rs.next()) return@withContext null
                    User(
                        uuid = UUID.fromString(rs.getString("uuid")),
                        kills = rs.getInt("kills"),
                        deaths = rs.getInt("deaths"),
                        points = rs.getInt("points"),
                        clanTag = rs.getString("clanTag")
                    )
                }
            }
        }
    }

    override suspend fun saveUser(user: User) {
        withContext(Dispatchers.IO) {
            val sql = "INSERT INTO users (uuid, kills, deaths, points, clanTag) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE kills=VALUES(kills), deaths=VALUES(deaths), points=VALUES(points), clanTag=VALUES(clanTag);"
            hikari.connection.use { conn ->
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, user.uuid.toString())
                    stmt.setInt(2, user.kills)
                    stmt.setInt(3, user.deaths)
                    stmt.setInt(4, user.points)
                    stmt.setString(5, user.clanTag)
                    stmt.executeUpdate()
                }
            }
        }
    }

    override suspend fun loadClan(tag: String): Clan? = transaction { conn ->
        val clanData = conn.prepareStatement("SELECT * FROM clans WHERE tag = ?").use { stmt ->
            stmt.setString(1, tag)
            stmt.executeQuery().use { rs ->
                if (!rs.next()) return@transaction null
                rs.getString("tag") to UUID.fromString(rs.getString("ownerUuid"))
            }
        }

        val members = conn.prepareStatement("SELECT * FROM clan_members WHERE clan_tag = ?").use { stmt ->
            stmt.setString(1, tag)
            val membersMap = mutableMapOf<UUID, ClanRole>()
            stmt.executeQuery().use { rs ->
                while(rs.next()) {
                    val memberUuid = UUID.fromString(rs.getString("user_uuid"))
                    val role = ClanRole.valueOf(rs.getString("role"))
                    membersMap[memberUuid] = role
                }
            }
            membersMap
        }
        Clan(clanData.first, clanData.second, members)
    }

    override suspend fun saveClan(clan: Clan) {
        transaction { conn ->
            val clanSql = "INSERT INTO clans (tag, ownerUuid) VALUES (?, ?) ON DUPLICATE KEY UPDATE ownerUuid=VALUES(ownerUuid);"
            conn.prepareStatement(clanSql).use { stmt ->
                stmt.setString(1, clan.tag)
                stmt.setString(2, clan.ownerUuid.toString())
                stmt.executeUpdate()
            }

            conn.prepareStatement("DELETE FROM clan_members WHERE clan_tag = ?").use { it.setString(1, clan.tag); it.execute() }

            val memberSql = "INSERT INTO clan_members (clan_tag, user_uuid, role) VALUES (?, ?, ?);"
            conn.prepareStatement(memberSql).use { stmt ->
                for ((uuid, role) in clan.members) {
                    stmt.setString(1, clan.tag)
                    stmt.setString(2, uuid.toString())
                    stmt.setString(3, role.name)
                    stmt.addBatch()
                }
                stmt.executeBatch()
            }
        }
    }

    override suspend fun deleteClan(tag: String) {
        transaction { conn ->
            conn.prepareStatement("DELETE FROM clans WHERE tag = ?").use { it.setString(1, tag); it.execute() }
            conn.prepareStatement("DELETE FROM clan_members WHERE clan_tag = ?").use { it.setString(1, tag); it.execute() }
            conn.prepareStatement("UPDATE users SET clanTag = NULL WHERE clanTag = ?").use { it.setString(1, tag); it.executeUpdate() }
        }
    }

    override suspend fun getAllClans(): List<Clan> {
        val tags = transaction { conn ->
            conn.prepareStatement("SELECT tag FROM clans").use { stmt ->
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<String>()
                    while (rs.next()) list.add(rs.getString("tag"))
                    list
                }
            }
        }
        return tags.mapNotNull { loadClan(it) }
    }
}