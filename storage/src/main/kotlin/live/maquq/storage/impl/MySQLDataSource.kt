package live.maquq.storage.impl

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import live.maquq.api.common.ClanRole
import live.maquq.api.data.DataSource
import live.maquq.api.user.User
import live.maquq.api.user.clan.Clan
import org.jooq.*
import org.jooq.impl.DSL
import java.util.*

class MySqlDataSource(
    private val settings: Map<String, Any?>,
    private val maxClanSize: Int
) : DataSource {

    private lateinit var hikari: HikariDataSource
    private lateinit var dsl: DSLContext

    private val USERS = DSL.table("users")
    private val USERS_UUID = DSL.field("uuid", String::class.java)
    private val USERS_KILLS = DSL.field("kills", Int::class.java)
    private val USERS_DEATHS = DSL.field("deaths", Int::class.java)
    private val USERS_POINTS = DSL.field("points", Int::class.java)
    private val USERS_ASSISTS = DSL.field("assists", Int::class.java)
    private val USERS_CLAN_TAG = DSL.field("clanTag", String::class.java)

    private val CLANS = DSL.table("clans")
    private val CLANS_TAG = DSL.field("tag", String::class.java)
    private val CLANS_OWNER = DSL.field("ownerUuid", String::class.java)
    private val CLANS_PVP = DSL.field("pvpEnabled", Boolean::class.java)
    private val CLANS_PVP_ROLE = DSL.field("pvpEditMinRole", String::class.java)

    private val MEMBERS = DSL.table("clanMembers")
    private val MEMBERS_CLAN = DSL.field("clanTag", String::class.java)
    private val MEMBERS_UUID = DSL.field("userUuid", String::class.java)
    private val MEMBERS_ROLE = DSL.field("role", String::class.java)

    override fun connect() {
        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:mysql://${settings["host"]}:${settings["port"]}/${settings["database"]}" +
                    "?useServerPrepStmts=true&cachePrepStmts=true&prepStmtCacheSize=500&prepStmtCacheSqlLimit=2048" +
                    "&rewriteBatchedStatements=true&useCompression=true&maintainTimeStats=false"
            username = settings["username"] as String
            password = settings["password"] as String

            maximumPoolSize = 15
            minimumIdle = 5
            connectionTimeout = 20000
            idleTimeout = 300000
            maxLifetime = 1800000
            leakDetectionThreshold = 60000

            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "500")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
            addDataSourceProperty("useServerPrepStmts", "true")
            addDataSourceProperty("useLocalSessionState", "true")
            addDataSourceProperty("useLocalTransactionState", "true")
            addDataSourceProperty("rewriteBatchedStatements", "true")
            addDataSourceProperty("cacheResultSetMetadata", "true")
            addDataSourceProperty("cacheServerConfiguration", "true")
            addDataSourceProperty("maintainTimeStats", "false")
        }

        hikari = HikariDataSource(config)
        dsl = DSL.using(hikari, SQLDialect.MYSQL)

        createTables()
    }

    private fun createTables() {
        dsl.transaction { config ->
            val ctx = DSL.using(config)

            ctx.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    uuid VARCHAR(36) PRIMARY KEY,
                    kills INT NOT NULL DEFAULT 0,
                    deaths INT NOT NULL DEFAULT 0,
                    points INT NOT NULL DEFAULT 0,
                    assists INT NOT NULL DEFAULT 0,
                    clanTag VARCHAR(16),
                    INDEX idx_clan (clanTag),
                    INDEX idx_points (points DESC)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                ROW_FORMAT=DYNAMIC;
            """.trimIndent())

            ctx.execute("""
                CREATE TABLE IF NOT EXISTS clans (
                    tag VARCHAR(16) PRIMARY KEY,
                    ownerUuid VARCHAR(36) NOT NULL,
                    pvpEnabled TINYINT(1) NOT NULL DEFAULT 1,
                    pvpEditMinRole VARCHAR(16) NOT NULL DEFAULT 'COLEADER',
                    INDEX idx_owner (ownerUuid)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                ROW_FORMAT=DYNAMIC;
            """.trimIndent())

            ctx.execute("""
                CREATE TABLE IF NOT EXISTS clanMembers (
                    clanTag VARCHAR(16) NOT NULL,
                    userUuid VARCHAR(36) NOT NULL,
                    role VARCHAR(16) NOT NULL,
                    PRIMARY KEY (clanTag, userUuid),
                    INDEX idx_user (userUuid),
                    FOREIGN KEY (clanTag) REFERENCES clans(tag) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                ROW_FORMAT=DYNAMIC;
            """.trimIndent())
        }
    }

    override fun disconnect() {
        if (::hikari.isInitialized && !hikari.isClosed) {
            hikari.close()
        }
    }

    private suspend fun <T> query(block: (DSLContext) -> T): T =
        withContext(Dispatchers.IO) { block(dsl) }

    override suspend fun loadUser(uuid: UUID): User? = query { ctx ->
        ctx.selectFrom(USERS)
            .where(USERS_UUID.eq(uuid.toString()))
            .fetchOne()
            ?.let { record ->
                User(
                    uuid = UUID.fromString(record.get(USERS_UUID)),
                    kills = record.get(USERS_KILLS),
                    deaths = record.get(USERS_DEATHS),
                    points = record.get(USERS_POINTS),
                    assists = record.get(USERS_ASSISTS),
                    clanTag = record.get(USERS_CLAN_TAG)
                )
            }
    }

    override suspend fun saveUser(user: User): Int = query { ctx ->
        ctx.insertInto(USERS)
            .set(USERS_UUID, user.uuid.toString())
            .set(USERS_KILLS, user.kills)
            .set(USERS_DEATHS, user.deaths)
            .set(USERS_POINTS, user.points)
            .set(USERS_ASSISTS, user.assists)
            .set(USERS_CLAN_TAG, user.clanTag)
            .onDuplicateKeyUpdate()
            .set(USERS_KILLS, user.kills)
            .set(USERS_DEATHS, user.deaths)
            .set(USERS_POINTS, user.points)
            .set(USERS_ASSISTS, user.assists)
            .set(USERS_CLAN_TAG, user.clanTag)
            .execute()
        0
    }

    override suspend fun removeUser(user: User): Int = query { ctx ->
        ctx.deleteFrom(USERS)
            .where(USERS_UUID.eq(user.uuid.toString()))
            .execute()
        0
    }

    override suspend fun loadClan(tag: String): Clan? = query { ctx ->
        val records = ctx.select(
            CLANS_TAG, CLANS_OWNER, CLANS_PVP, CLANS_PVP_ROLE,
            MEMBERS_UUID, MEMBERS_ROLE
        )
            .from(CLANS)
            .leftJoin(MEMBERS).on(CLANS_TAG.eq(MEMBERS_CLAN))
            .where(CLANS_TAG.eq(tag))
            .fetch()

        if (records.isEmpty()) return@query null

        val first = records.first()
        val members = records
            .mapNotNull { record ->
                record.get(MEMBERS_UUID)?.let { uuid ->
                    UUID.fromString(uuid) to ClanRole.valueOf(record.get(MEMBERS_ROLE))
                }
            }
            .toMap()
            .toMutableMap()

        Clan(
            tag = first.get(CLANS_TAG),
            ownerUuid = UUID.fromString(first.get(CLANS_OWNER)),
            members = members,
            maxSize = maxClanSize,
            pointsMultiplier = 1.0,
            pvpEnabled = first.get(CLANS_PVP),
            pvpEditMinRole = ClanRole.valueOf(first.get(CLANS_PVP_ROLE))
        )
    }

    override suspend fun saveClan(clan: Clan) {
        query { ctx ->
            ctx.transaction { config ->
                val txCtx = DSL.using(config)

                txCtx.insertInto(CLANS)
                    .set(CLANS_TAG, clan.tag)
                    .set(CLANS_OWNER, clan.ownerUuid.toString())
                    .set(CLANS_PVP, clan.pvpEnabled)
                    .set(CLANS_PVP_ROLE, clan.pvpEditMinRole.name)
                    .onDuplicateKeyUpdate()
                    .set(CLANS_OWNER, clan.ownerUuid.toString())
                    .set(CLANS_PVP, clan.pvpEnabled)
                    .set(CLANS_PVP_ROLE, clan.pvpEditMinRole.name)
                    .execute()

                txCtx.deleteFrom(MEMBERS)
                    .where(MEMBERS_CLAN.eq(clan.tag))
                    .execute()

                if (clan.members.isNotEmpty()) {
                    val insert = txCtx.insertInto(
                        MEMBERS,
                        MEMBERS_CLAN, MEMBERS_UUID, MEMBERS_ROLE
                    )

                    clan.members.forEach { (uuid, role) ->
                        insert.values(clan.tag, uuid.toString(), role.name)
                    }

                    insert.execute()
                }
            }
        }
    }

    override suspend fun deleteClan(tag: String): Int = query { ctx ->
        ctx.transaction { config ->
            val txCtx = DSL.using(config)

            txCtx.deleteFrom(CLANS)
                .where(CLANS_TAG.eq(tag))
                .execute()

            txCtx.update(USERS)
                .setNull(USERS_CLAN_TAG)
                .where(USERS_CLAN_TAG.eq(tag))
                .execute()
        }
        0
    }

    override suspend fun getAllClans(): List<Clan> = query { ctx ->
        val records = ctx.select(
            CLANS_TAG, CLANS_OWNER, CLANS_PVP, CLANS_PVP_ROLE,
            MEMBERS_UUID, MEMBERS_ROLE
        )
            .from(CLANS)
            .leftJoin(MEMBERS).on(CLANS_TAG.eq(MEMBERS_CLAN))
            .orderBy(CLANS_TAG)
            .fetch()

        records
            .groupBy { it.get(CLANS_TAG) }
            .map { (tag, groupRecords) ->
                val first = groupRecords.first()
                val members = groupRecords
                    .mapNotNull { record ->
                        record.get(MEMBERS_UUID)?.let { uuid ->
                            UUID.fromString(uuid) to ClanRole.valueOf(record.get(MEMBERS_ROLE))
                        }
                    }
                    .toMap()
                    .toMutableMap()

                Clan(
                    tag = tag,
                    ownerUuid = UUID.fromString(first.get(CLANS_OWNER)),
                    members = members,
                    maxSize = maxClanSize,
                    pointsMultiplier = 1.0,
                    pvpEnabled = first.get(CLANS_PVP),
                    pvpEditMinRole = ClanRole.valueOf(first.get(CLANS_PVP_ROLE))
                )
            }
    }

    override suspend fun getTopUsers(limit: Int): List<User> = query { ctx ->
        ctx.selectFrom(USERS)
            .orderBy(USERS_POINTS.desc())
            .limit(limit)
            .fetch()
            .map { record ->
                User(
                    uuid = UUID.fromString(record.get(USERS_UUID)),
                    kills = record.get(USERS_KILLS),
                    deaths = record.get(USERS_DEATHS),
                    points = record.get(USERS_POINTS),
                    assists = record.get(USERS_ASSISTS),
                    clanTag = record.get(USERS_CLAN_TAG)
                )
            }
    }
}