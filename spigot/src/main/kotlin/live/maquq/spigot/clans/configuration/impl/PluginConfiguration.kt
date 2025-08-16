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
    var timeToTimeoutInvite: Int = 120
}

class MessageSettings : ConfigTemplate {
    var noPermission: String = "<red>Nie masz uprawnień do wykonania tej komendy. <dark_red>([PERMISSION])"
    var correctUsage: String = "<red>Poprawne uzycie z komendy: [CORRECT]"
    var correctUsages: String = "<red>Poprawne uzycie z komendy:"

    var clanNotFound: String = "<red>Klan o podanej nazwie nie istnieje."
    var clanAlreadyExists: String = "<red>Klan o podanym tagu już istnieje."
    var createdClan: String = "<green>Stworzono klan!"
    var alreadyInClan: String = "<red>Jestes juz w jakims klanie!"
    var targetAlreadyInClan: String = "<red>Gracz jest juz w jakim klanie!"
    var notInAnyClan: String = "<red>Nie jestes w zadnym klanie!"
    var msgToInviter: String = "<green>Zaproszono gracza <yellow>[INVITED]</yellow> do klanu."
    var invitedToClan: String = "<gold>Zostałeś zaproszony do klanu <yellow>[CLAN-TAG]</yellow> przez <yellow>[INVITER]</yellow>!\n" +
                                "<gray>Wpisz <click:run_command:/clan join>[CLAN-TAG]</click> aby dołączyć, lub <click:run_command:/clan deny>[CLAN-TAG]</click> aby odrzucić."
}