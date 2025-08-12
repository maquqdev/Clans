package live.maquq.spigot.clans.configuration.impl

import live.maquq.spigot.clans.configuration.ConfigTemplate
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack

enum class StorageType {
    FLAT, MYSQL, MONGODB
}

class PluginConfiguration : ConfigTemplate {

    var storage: StorageType = StorageType.FLAT

    var mysql: MysqlSettings = MysqlSettings()
    var mongo: MongoSettings = MongoSettings()
    var clanSettings: ClanSettings = ClanSettings()
    var messages: MessageSettings = MessageSettings()
}


class MysqlSettings : ConfigTemplate {
    var host: String = "localhost"
    var port: Int = 3306
    var database: String = "clans"
    var username: String = "user"
    var password: String = "password"
}

class MongoSettings : ConfigTemplate {
    var connectionString: String = "mongodb://localhost:27017/clans"
}

class ClanSettings : ConfigTemplate {
    var maxMembers: Int = 20
    var tagMinLength: Int = 3
    var tagMaxLength: Int = 5
    var creationCostItem: ItemStack = ItemStack(Material.NETHER_STAR, 1).apply {
        itemMeta = itemMeta.also {
            it.displayName(Component.text("Zakorzeniona Gwiazda", NamedTextColor.LIGHT_PURPLE))
            it.lore(listOf(
                Component.text("Przedmiot wymagany do", NamedTextColor.GRAY),
                Component.text("stworzenia nowego klanu.", NamedTextColor.GRAY)
            ))
            it.addEnchant(Enchantment.PROTECTION_FALL, 1, true)
        }
    }
}

class MessageSettings : ConfigTemplate {
    var noPermission: String = "<red>Nie masz uprawnień do wykonania tej komendy."
    var playerOnlyCommand: String = "<red>Ta komenda może być wykonana tylko przez gracza."
    var clanCreated: String = "<green>Klan <gold><tag></gold> został pomyślnie stworzony!"
    var playerJoinedClan: String = "<aqua><player_name></aqua> dołączył do Twojego klanu!"
}