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
        try {
            val config = HikariConfig().apply {
                jdbcUrl = "jdbc:mysql://${settings["host"]}:${settings["port"]}/${settings["database"]}?autoReconnect=true"
                username = settings["username"] as String
                password = settings["password"] as String
                maximumPoolSize = 10
            }
            hikari = HikariDataSource(config)
            createTables()
        } catch (exception: Exception) {
            throw RuntimeException("Failed to connect to MySQL database with settings: $settings", exception)
        }
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
            CREATE TABLE IF NOT EXISTS clanMembers (
                clanTag VARCHAR(16) NOT NULL,
                userUuid VARCHAR(36) NOT NULL,
                role VARCHAR(16) NOT NULL,
                PRIMARY KEY (clanTag, userUuid)
            );
        """.trimIndent()

        try {
            hikari.connection.use { conn ->
                conn.createStatement().use {
                    it.execute(userTableSql)
                    it.execute(clanTableSql)
                    it.execute(membersTableSql)
                }
            }
        } catch (exception: Exception) {
            throw RuntimeException("Failed to create database tables", exception)
        }
    }

    override fun disconnect() {
        if (this::hikari.isInitialized && !hikari.isClosed) {
            hikari.close()
        }
    }

    private suspend fun <T> transaction(block: suspend (Connection) -> T): T = withContext(Dispatchers.IO) {
        hikari.connection.use { conn ->
            val oldAutoCommit = conn.autoCommit
            conn.autoCommit = false
            try {
                val result = block(conn)
                conn.commit()
                result
            } catch (exception: Exception) {
                conn.rollback()
                throw RuntimeException("Failed to execute database transaction", exception)
            } finally {
                conn.autoCommit = oldAutoCommit
            }
        }
    }

    override suspend fun loadUser(uuid: UUID): User? = withContext(Dispatchers.IO) {
        try {
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
        } catch (exception: Exception) {
            throw RuntimeException("Failed to load user by UUID: $uuid", exception)
        }
    }

    override suspend fun saveUser(user: User) {
        try {
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
        } catch (exception: Exception) {
            throw RuntimeException("Failed to save user with UUID: ${user.uuid}", exception)
        }
    }

    override suspend fun removeUser(user: User) {
        withContext(Dispatchers.IO) {
            try {
                hikari.connection.use { conn ->
                    conn.prepareStatement("DELETE FROM users WHERE uuid = ?").use { stmt ->
                        stmt.setString(1, user.uuid.toString())
                        stmt.executeUpdate()
                    }
                }
            } catch (exception: Exception) {
                throw RuntimeException("Failed to remove user with UUID: ${user.uuid}", exception)
            }
        }
    }

    override suspend fun loadClan(tag: String): Clan? = try {
        transaction { conn ->
            val clanData = conn.prepareStatement("SELECT * FROM clans WHERE tag = ?").use { stmt ->
                stmt.setString(1, tag)
                stmt.executeQuery().use { rs ->
                    if (!rs.next()) return@transaction null
                    rs.getString("tag") to UUID.fromString(rs.getString("ownerUuid"))
                }
            }

            val members = conn.prepareStatement("SELECT * FROM clanMembers WHERE clanTag = ?").use { stmt ->
                stmt.setString(1, tag)
                val membersMap = mutableMapOf<UUID, ClanRole>()
                stmt.executeQuery().use { rs ->
                    while(rs.next()) {
                        val memberUuid = UUID.fromString(rs.getString("userUuid"))
                        val role = ClanRole.valueOf(rs.getString("role"))
                        membersMap[memberUuid] = role
                    }
                }
                membersMap
            }
            Clan(clanData.first, clanData.second, members)
        }
    } catch (exception: Exception) {
        throw RuntimeException("Failed to load clan with tag: $tag", exception)
    }

    override suspend fun saveClan(clan: Clan) {
        try {
            transaction { conn ->
                val clanSql = "INSERT INTO clans (tag, ownerUuid) VALUES (?, ?) ON DUPLICATE KEY UPDATE ownerUuid=VALUES(ownerUuid);"
                conn.prepareStatement(clanSql).use { stmt ->
                    stmt.setString(1, clan.tag)
                    stmt.setString(2, clan.ownerUuid.toString())
                    stmt.executeUpdate()
                }

                conn.prepareStatement("DELETE FROM clanMembers WHERE clanTag = ?").use { it.setString(1, clan.tag); it.execute() }

                val memberSql = "INSERT INTO clanMembers (clanTag, userUuid, role) VALUES (?, ?, ?);"
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
        } catch (exception: Exception) {
            throw RuntimeException("Failed to save clan with tag: ${clan.tag}", exception)
        }
    }

    override suspend fun deleteClan(tag: String) {
        try {
            transaction { conn ->
                conn.prepareStatement("DELETE FROM clans WHERE tag = ?").use { it.setString(1, tag); it.execute() }
                conn.prepareStatement("DELETE FROM clanMembers WHERE clanTag = ?").use { it.setString(1, tag); it.execute() }
                conn.prepareStatement("UPDATE users SET clanTag = NULL WHERE clanTag = ?").use { it.setString(1, tag); it.executeUpdate() }
            }
        } catch (exception: Exception) {
            throw RuntimeException("Failed to delete clan with tag: $tag", exception)
        }
    }

    override suspend fun getAllClans(): List<Clan> {
        return try {
            val tags = transaction { conn ->
                conn.prepareStatement("SELECT tag FROM clans").use { stmt ->
                    stmt.executeQuery().use { rs ->
                        val list = mutableListOf<String>()
                        while (rs.next()) list.add(rs.getString("tag"))
                        list
                    }
                }
            }
            tags.mapNotNull {
                try {
                    loadClan(it)
                } catch (ignored: Exception) {
                    null 
                }
            }
        } catch (exception: Exception) {
            throw RuntimeException("Failed to load all clans from database", exception)
        }
    }
}