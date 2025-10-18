package live.maquq.spigot.clans.configuration.impl

import live.maquq.spigot.clans.configuration.ConfigTemplate

enum class StorageType {
    FLAT, MYSQL, MONGODB
}

class PluginConfiguration : ConfigTemplate {
    var storage: StorageType = StorageType.FLAT

    var broker: BrokerSettings = BrokerSettings()
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
    var connectionString: String = "mongodb://localhost:27017/testng"
}

class BrokerSettings : ConfigTemplate {
    var enabled: Boolean = false

    var url: String = "ws://localhost:8080/ws"
    var username: String = "user"
    var password: String = "password"
}

class ClanSettings : ConfigTemplate {
    var timeToTimeoutInvite: Int = 120
    var separator: String = ", "
    var defaultPoints: Int = 500
    var defaultSize: Int = 5
    var maxSize: Int = 7
    var priceForUpgrade: Int = 5

    var pointsConfiguration: PointsConfiguration = PointsConfiguration()

    var skillBasedPointsConfiguration: SkillBasedPointsConfiguration = SkillBasedPointsConfiguration()
    var proportionalPointsConfiguration: ProportionalPointsConfiguration = ProportionalPointsConfiguration()
}

enum class PointsType {
    COMPOSITE,
    SKILL_BASED
}

class PointsConfiguration : ConfigTemplate {
    var pointsType: PointsType = PointsType.SKILL_BASED
}

class SkillBasedPointsConfiguration : ConfigTemplate {
    var basePointExchange: Int = 20
    var underdogDivisor: Double = 400.0
    var favouriteDivisor: Double = 500.0
}

class ProportionalPointsConfiguration : ConfigTemplate {
   var gainPercent: Double = 0.05
   var lossPercent: Double = 0.03
   var minimumChange: Int = 5
   var maximumGain: Int = 75
}

class MessageSettings : ConfigTemplate {
    var noPermission: String = "[red]Nie masz uprawnień do wykonania tej komendy. [dark_red]([PERMISSION])"
    var correctUsage: String = "[red]Poprawne uzycie z komendy: [CORRECT]"
    var correctUsages: String = "[red]Poprawne uzycie z komendy:"

    var clanNotFound: String = "[red]Klan o podanej nazwie nie istnieje."
    var clanAlreadyExists: String = "[red]Klan o podanym tagu już istnieje."
    var createdClan: String = "[green]Stworzono klan!"
    var alreadyInClan: String = "[red]Jestes juz w jakims klanie!"
    var targetAlreadyInClan: String = "[red]Gracz jest juz w jakim klanie!"
    var notInAnyClan: String = "[red]Nie jestes w zadnym klanie!"
    var leaderCantLeave: String = "[red]Nie jestes w zadnym klanie!"
    var targetIsAlreadyColeader: String = "[red]Ten gracz jest już coleaderem"
    var cantPromote: String = "[red]Tylko lider moze awansowac na zastepce"
    var kickedSuccess: String = "[red]Wyrzucono [PLAYER] z klanu!"
    var youWerekicked: String = "[red]Zostales wyrzucony z klanu [TAG]"
    var promotedToColeaderSuccess: String = "[green]Awansowałeś [PLAYER] na coleadera!"
    var youWerePromotedToColeader: String = "[green]Zostałeś awansowany do coleadera"
    var notLeader: String = "[red]Nie jesteś liderem klanu"
    var msgToInviter: String = "[green]Zaproszono gracza [yellow][INVITED][/yellow] do klanu."
    var notSameClan: String = "[red]Nie jesteście w tym samym klanie"
    var selfKick: String = "[red]Nie możesz wyrzucić samego siebie"
    var selfPromotion: String = "[red]Nie możesz awansowwać samego siebie na zastepca"
    var cantKick: String = "[red]Nie możesz wyrzucić lidera"
    var notEnoughPermission: String = "[red]Nie masz wystarczająco uprawnien, aby wyrzucić gracza z klanu"
    var cantInvite: String = "[red]Nie możesz zaprosić, ponieważ jesteś członkiem"
    var invitedToClan: String = "[gold]Zostałeś zaproszony do klanu [yellow][CLAN-TAG][/yellow] przez [yellow][INVITER][/yellow]!\n" +
                                "[gray]Wpisz [click:run_command:/clan join][aqua]/clan join[/aqua][/click] aby dołączyć"
    var joinedToClan: String = "[green]Pomyślnie dołączono do klanu!"
    var cantJoin: String = "[red]Nie masz żadnych oczekujących zaproszeń lub zaproszenie wygasło."
    var clanInfo: String = """
        Informacje o klanie
        Tag: [TAG]
        Lider: [LEADER]
        COLEADER: [COLEADER]
        Członkowie: [MEMBERS]
        Punkty: [POINTS]
    """.trimIndent()
    var playerInfo: String = """
        Informacje o graczu [PLAYER]
        Klan: [TAG]
        Punkty: [POINTS]
        Smierci: [DEATHS]
        Zabojstwa: [KILLS]
        KD: [KD]
    """.trimIndent()
    var maxSize: String = "[red]Twój klan ma maksymalną ilość członków"
    var leftClan: String = "[red]Opuszczono klan!"
    var requestDelete: String = "[red]Musisz wpisać ponownie ta komende, aby usunąć klan!"

    var playerDeath: PlayerDeathConfiguration = PlayerDeathConfiguration()

}

class PlayerDeathConfiguration : ConfigTemplate {
    var title: String = "ZABOJSTWO"
    var subtitle: String = "+[POINTS]"

    var victimTitle: String = "ZGINALES"
    var victimSubtitle: String = "-[POINTS]"
    var broadcast: String = "Gracz [VICTIM] (-[REMOVED-POINTS]) zginal od [KILLER] (+[ADDED-POINTS]) "
}