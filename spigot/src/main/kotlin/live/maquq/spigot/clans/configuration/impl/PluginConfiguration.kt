package live.maquq.spigot.clans.configuration.impl

import live.maquq.spigot.clans.configuration.ConfigTemplate

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

}

class MessageSettings : ConfigTemplate {
    var noPermission: String = "<red>Nie masz uprawnień do wykonania tej komendy. <dark_red>([PERMISSION])"
    var correctUsage: String = "<red>Poprawne uzycie z komendy: [CORRECT]"
    var correctUsages: String = "<red>Poprawne uzycie z komendy:"
}