package live.maquq.storage.impl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import live.maquq.api.common.ClanRole
import live.maquq.api.data.DataSource
import live.maquq.api.user.User
import live.maquq.api.user.clan.Clan
import java.io.File
import java.util.*

@Serializer(forClass = UUID::class)
object UUIDSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): UUID {
        return UUID.fromString(decoder.decodeString())
    }
}


object UUIDClanRoleMapSerializer : KSerializer<MutableMap<UUID, ClanRole>> {
    private val delegateSerializer = MapSerializer(UUIDSerializer, ClanRole.serializer())
    override val descriptor: SerialDescriptor = delegateSerializer.descriptor

    override fun serialize(encoder: Encoder, value: MutableMap<UUID, ClanRole>) {
        encoder.encodeSerializableValue(delegateSerializer, value)
    }

    override fun deserialize(decoder: Decoder): MutableMap<UUID, ClanRole> {
        return decoder.decodeSerializableValue(delegateSerializer).toMutableMap()
    }
}


@OptIn(ExperimentalSerializationApi::class)
class FlatDataSource(
    dataFolder: File
) : DataSource {

    private val userFolder = File(dataFolder, "users")
    private val clanFolder = File(dataFolder, "clans")

    private val protoBuf = ProtoBuf {
        serializersModule = SerializersModule {
            contextual(UUIDSerializer)
            contextual(UUIDClanRoleMapSerializer)
        }
    }

    override fun connect() {
        userFolder.mkdirs()
        clanFolder.mkdirs()
    }

    override fun disconnect() { }

    override suspend fun loadUser(uuid: UUID): User? = withContext(Dispatchers.IO) {
        val userFile = File(userFolder, "$uuid.dat")
        if (!userFile.exists()) return@withContext null

        runCatching {
            val bytes = userFile.readBytes()
            protoBuf.decodeFromByteArray<User>(bytes)
        }.getOrNull()
    }

    override suspend fun saveUser(user: User): Int = withContext(Dispatchers.IO) {
        runCatching {
            val userFile = File(userFolder, "${user.uuid}.dat")
            val bytes = protoBuf.encodeToByteArray(user)
            userFile.writeBytes(bytes)
            0
        }.getOrElse {
            it.printStackTrace()
            -1
        }
    }

    override suspend fun removeUser(user: User): Int = withContext(Dispatchers.IO) {
        runCatching {
            File(userFolder, "${user.uuid}.dat").delete()
            0
        }.getOrElse {
            it.printStackTrace()
            -1
        }
    }

    override suspend fun loadClan(tag: String): Clan? = withContext(Dispatchers.IO) {
        val clanFile = File(clanFolder, "$tag.dat")
        if (!clanFile.exists()) return@withContext null

        runCatching {
            val bytes = clanFile.readBytes()
            protoBuf.decodeFromByteArray<Clan>(bytes)
        }.getOrNull()
    }

    override suspend fun saveClan(clan: Clan) {
        withContext(Dispatchers.IO) {
            runCatching {
                val clanFile = File(clanFolder, "${clan.tag}.dat")
                val bytes = protoBuf.encodeToByteArray(clan)
                clanFile.writeBytes(bytes)
            }.onFailure { it.printStackTrace() }
        }
    }

    override suspend fun deleteClan(tag: String): Int = withContext(Dispatchers.IO) {
        runCatching {
            File(clanFolder, "$tag.dat").delete()

            userFolder.listFiles { _, name -> name.endsWith(".dat") }
                ?.asSequence()
                ?.mapNotNull { file ->
                    runCatching {
                        val bytes = file.readBytes()
                        protoBuf.decodeFromByteArray<User>(bytes)
                    }.getOrNull()
                }
                ?.filter { it.clanTag == tag }
                ?.forEach { user ->
                    val updatedUser = user.copy(clanTag = null)
                    saveUser(updatedUser)
                }
            0
        }.getOrElse {
            it.printStackTrace()
            -1
        }
    }

    override suspend fun getAllClans(): List<Clan> = withContext(Dispatchers.IO) {
        clanFolder.listFiles { _, name -> name.endsWith(".dat") }
            ?.asSequence()
            ?.mapNotNull { file ->
                runCatching {
                    val bytes = file.readBytes()
                    protoBuf.decodeFromByteArray<Clan>(bytes)
                }.getOrNull()
            }
            ?.toList()
            ?: emptyList()
    }

    override suspend fun getTopUsers(limit: Int): List<User> = withContext(Dispatchers.IO) {
        userFolder.listFiles { _, name -> name.endsWith(".dat") }
            ?.asSequence()
            ?.mapNotNull { file ->
                runCatching {
                    val bytes = file.readBytes()
                    protoBuf.decodeFromByteArray<User>(bytes)
                }.getOrNull()
            }
            ?.sortedByDescending { it.points }
            ?.take(limit)
            ?.toList()
            ?: emptyList()
    }
}