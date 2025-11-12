package live.maquq.storage.impl

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import live.maquq.api.data.DataSource
import live.maquq.api.user.User
import live.maquq.api.user.clan.Clan
import live.maquq.api.user.clan.ClanRole
import java.sql.Connection
import java.sql.ResultSet
import java.util.*

class MySqlDataSource(
    private val settings: Map<String, Any?>,
    private val maxClanSize: Int
) : DataSource {

    private lateinit var hikari: HikariDataSource

    override fun connect() {
        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:mysql://${settings["host"]}:${settings["port"]}/${settings["database"]}" +
                    "?autoReconnect=true&useServerPrepStmts=true&cachePrepStmts=true&rewriteBatchedStatements=true"
            username = settings["username"] as String
            password = settings["password"] as String

            maximumPoolSize = 10
            minimumIdle = 2
            connectionTimeout = 30000
            idleTimeout = 600000
            maxLifetime = 1800000

            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
            addDataSourceProperty("useServerPrepStmts", "true")
        }
        hikari = HikariDataSource(config)
        createTables()
    }

    private fun createTables() {
        val userTableSql = """
            CREATE TABLE IF NOT EXISTS users (
                uuid VARCHAR(36) PRIMARY KEY,
                kills INT NOT NULL DEFAULT 0,
                deaths INT NOT NULL DEFAULT 0,
                points INT NOT NULL DEFAULT 0,
                clanTag VARCHAR(16),
                INDEX idx_clan (clanTag)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """.trimIndent()

        val clanTableSql = """
            CREATE TABLE IF NOT EXISTS clans (
                tag VARCHAR(16) PRIMARY KEY,
                ownerUuid VARCHAR(36) NOT NULL,
                pvpEnabled TINYINT(1) NOT NULL DEFAULT 1,
                pvpEditMinRole VARCHAR(16) NOT NULL DEFAULT 'COLEADER',
                INDEX idx_owner (ownerUuid)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """.trimIndent()

        val membersTableSql = """
            CREATE TABLE IF NOT EXISTS clanMembers (
                clanTag VARCHAR(16) NOT NULL,
                userUuid VARCHAR(36) NOT NULL,
                role VARCHAR(16) NOT NULL,
                PRIMARY KEY (clanTag, userUuid),
                INDEX idx_user (userUuid)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """.trimIndent()

        hikari.connection.use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute(userTableSql)
                stmt.execute(clanTableSql)
                stmt.execute(membersTableSql)
            }
        }
    }

    override fun disconnect() {
        if (this::hikari.isInitialized && !hikari.isClosed) {
            hikari.close()
        }
    }

    private suspend fun <T> query(block: (Connection) -> T): T = withContext(Dispatchers.IO) {
        hikari.connection.use(block)
    }

    private suspend fun <T> transaction(block: (Connection) -> T): T = withContext(Dispatchers.IO) {
        hikari.connection.use { conn ->
            conn.autoCommit = false
            try {
                block(conn).also { conn.commit() }
            } catch (e: Exception) {
                conn.rollback()
                throw e
            } finally {
                conn.autoCommit = true
            }
        }
    }

    private fun ResultSet.toUser(): User = User(
        uuid = UUID.fromString(getString("uuid")),
        kills = getInt("kills"),
        deaths = getInt("deaths"),
        points = getInt("points"),
        clanTag = getString("clanTag")
    )

    override suspend fun loadUser(uuid: UUID): User? = query { conn ->
        conn.prepareStatement("SELECT * FROM users WHERE uuid = ?").use { stmt ->
            stmt.setString(1, uuid.toString())
            stmt.executeQuery().use { rs ->
                if (rs.next()) rs.toUser() else null
            }
        }
    }

    override suspend fun saveUser(user: User) = query { conn ->
        val sql = """
            INSERT INTO users (uuid, kills, deaths, points, clanTag) 
            VALUES (?, ?, ?, ?, ?) 
            ON DUPLICATE KEY UPDATE 
                kills = VALUES(kills), 
                deaths = VALUES(deaths), 
                points = VALUES(points), 
                clanTag = VALUES(clanTag)
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, user.uuid.toString())
            stmt.setInt(2, user.kills)
            stmt.setInt(3, user.deaths)
            stmt.setInt(4, user.points)
            stmt.setString(5, user.clanTag)
            stmt.executeUpdate()
        }
    }

    override suspend fun removeUser(user: User) = query { conn ->
        conn.prepareStatement("DELETE FROM users WHERE uuid = ?").use { stmt ->
            stmt.setString(1, user.uuid.toString())
            stmt.executeUpdate()
        }
    }

    override suspend fun loadClan(tag: String): Clan? = query { conn ->
        val sql = """
            SELECT 
                c.tag, c.ownerUuid, c.pvpEnabled, c.pvpEditMinRole,
                cm.userUuid, cm.role
            FROM clans c
            LEFT JOIN clanMembers cm ON c.tag = cm.clanTag
            WHERE c.tag = ?
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, tag)
            stmt.executeQuery().use { rs ->
                if (!rs.next()) return@query null

                val clanTag = rs.getString("tag")
                val ownerUuid = UUID.fromString(rs.getString("ownerUuid"))
                val pvpEnabled = rs.getBoolean("pvpEnabled")
                val pvpEditMinRole = ClanRole.valueOf(rs.getString("pvpEditMinRole"))
                val members = mutableMapOf<UUID, ClanRole>()

                rs.getString("userUuid")?.let {
                    members[UUID.fromString(it)] = ClanRole.valueOf(rs.getString("role"))
                }

                while (rs.next()) {
                    rs.getString("userUuid")?.let {
                        members[UUID.fromString(it)] = ClanRole.valueOf(rs.getString("role"))
                    }
                }

                Clan(clanTag, ownerUuid, members, this.maxClanSize, 1.0, pvpEnabled, pvpEditMinRole)
            }
        }
    }

    override suspend fun saveClan(clan: Clan) = transaction { conn ->
        val clanSql = """
            INSERT INTO clans (tag, ownerUuid, pvpEnabled, pvpEditMinRole) 
            VALUES (?, ?, ?, ?) 
            ON DUPLICATE KEY UPDATE 
                ownerUuid = VALUES(ownerUuid),
                pvpEnabled = VALUES(pvpEnabled),
                pvpEditMinRole = VALUES(pvpEditMinRole)
        """.trimIndent()

        conn.prepareStatement(clanSql).use { stmt ->
            stmt.setString(1, clan.tag)
            stmt.setString(2, clan.ownerUuid.toString())
            stmt.setBoolean(3, clan.pvpEnabled)
            stmt.setString(4, clan.pvpEditMinRole.name)
            stmt.executeUpdate()
        }

        conn.prepareStatement("DELETE FROM clanMembers WHERE clanTag = ?").use { stmt ->
            stmt.setString(1, clan.tag)
            stmt.executeUpdate()
        }

        if (clan.members.isNotEmpty()) {
            val memberSql = "INSERT INTO clanMembers (clanTag, userUuid, role) VALUES (?, ?, ?)"
            conn.prepareStatement(memberSql).use { stmt ->
                clan.members.forEach { (uuid, role) ->
                    stmt.setString(1, clan.tag)
                    stmt.setString(2, uuid.toString())
                    stmt.setString(3, role.name)
                    stmt.addBatch()
                }
                stmt.executeBatch()
            }
        }
    }

    override suspend fun deleteClan(tag: String) = transaction { conn ->
        conn.prepareStatement("DELETE FROM clanMembers WHERE clanTag = ?").use { stmt ->
            stmt.setString(1, tag)
            stmt.executeUpdate()
        }

        conn.prepareStatement("DELETE FROM clans WHERE tag = ?").use { stmt ->
            stmt.setString(1, tag)
            stmt.executeUpdate()
        }

        conn.prepareStatement("UPDATE users SET clanTag = NULL WHERE clanTag = ?").use { stmt ->
            stmt.setString(1, tag)
            stmt.executeUpdate()
        }
    }

    override suspend fun getAllClans(): List<Clan> = query { conn ->
        val sql = """
            SELECT 
                c.tag, c.ownerUuid, c.pvpEnabled, c.pvpEditMinRole,
                cm.userUuid, cm.role
            FROM clans c
            LEFT JOIN clanMembers cm ON c.tag = cm.clanTag
            ORDER BY c.tag
        """.trimIndent()

        data class ClanRowAgg(
            val tag: String,
            val owner: UUID,
            val pvpEnabled: Boolean,
            val pvpRole: ClanRole,
            val members: MutableMap<UUID, ClanRole>
        )

        val clansMap = mutableMapOf<String, ClanRowAgg>()

        conn.prepareStatement(sql).use { stmt ->
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    val tag = rs.getString("tag")

                    val agg = clansMap.getOrPut(tag) {
                        val ownerUuid = UUID.fromString(rs.getString("ownerUuid"))
                        val pvpEnabled = rs.getBoolean("pvpEnabled")
                        val pvpRole = ClanRole.valueOf(rs.getString("pvpEditMinRole"))
                        ClanRowAgg(tag, ownerUuid, pvpEnabled, pvpRole, mutableMapOf())
                    }

                    rs.getString("userUuid")?.let { uuidStr ->
                        val uuid = UUID.fromString(uuidStr)
                        val role = ClanRole.valueOf(rs.getString("role"))
                        agg.members[uuid] = role
                    }
                }
            }
        }

        clansMap.values.map { agg ->
            Clan(agg.tag, agg.owner, agg.members, 3, 1.0, agg.pvpEnabled, agg.pvpRole) // TODO: change default size
        }
    }

    override suspend fun getTopUsers(limit: Int): List<User> = query { conn ->
        conn.prepareStatement("SELECT * FROM users ORDER BY points DESC LIMIT ?").use { stmt ->
            stmt.setInt(1, limit)
            stmt.executeQuery().use { rs ->
                val list = mutableListOf<User>()
                while (rs.next()) {
                    list.add(rs.toUser())
                }
                list
            }
        }
    }
}