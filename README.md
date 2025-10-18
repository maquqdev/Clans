# Clans Plugin

A Minecraft plugin that adds clan functionality to Spigot/Paper servers, allowing players to form clans, compete for points, and manage clan hierarchies.

## Features

- **Clan Management**: Create, join, and manage clans with customizable roles (Leader, Co-Leader, Member).
- **Points System**: Earn points through player kills with configurable multipliers and calculation strategies.
- **Event-Driven Architecture**: Comprehensive event system for clan and user actions (join, quit, create, delete, upgrade).
- **Data Persistence**: Support for multiple storage backends (MongoDB, MySQL, FlatFile).
- **Modular Design**: Clean API for developers to extend functionality.

## Installation

1. Download the latest release JAR from the [releases page](https://github.com/maquqdev/Clans/releases).
2. Place the JAR file in your server's `plugins` directory.
3. Restart your server.
4. Configure the plugin in `plugins/Clans/config.json`.

Also, you can check logs folder

## Permissions

- `clans.create` - Create clans
- `clans.invite` - Invite players to clans
- `clans.kick` - Kick members from clan
- `clans.upgrade` - Upgrade clan properties
- `clans.admin` - Administrative commands

## Commands

- `/clan create <tag>` - Create a new clan
- `/clan invite <player>` - Invite a player to your clan
- `/clan join <tag>` - Join a clan (if invited)
- `/clan leave` - Leave your current clan
- `/clan info [tag]` - View clan information
- `/clan list` - List all clans
- `/clan top` - View clan leaderboard

## Configuration

The plugin supports various configuration options including:
- Database connection settings
- Points calculation parameters
- Clan size limits and upgrade costs
- GUI customization

## Modules

- **API**: Core interfaces, data classes, and events.
- **Spigot**: Main plugin implementation for Minecraft servers.
- **Storage**: Data persistence implementations.
- **GUI**: User interface components.

## Developer API

The plugin provides a comprehensive API for developers to integrate with clan functionality.

### Core Classes

#### User
```kotlin
data class User(
    val uuid: UUID,
    var kills: Int = 0,
    var deaths: Int = 0,
    var points: Int,
    var clanTag: String? = null
)
```

#### Clan
```kotlin
data class Clan(
    val tag: String,
    var ownerUuid: UUID,
    var members: MutableMap<UUID, ClanRole>,
    var maxSize: Int,
    var pointsMultiplier: Double = 1.0
)
```

#### ClanRole
```kotlin
enum class ClanRole {
    LEADER,
    COLEADER,
    MEMBER
}
```

### Events

The plugin fires various events that developers can listen to:

- `UserJoinClanEvent` - Fired when a user joins a clan
- `UserQuitClanEvent` - Fired when a user leaves or is kicked from a clan
- `ClanCreateEvent` - Fired when a new clan is created
- `ClanDeleteEvent` - Fired when a clan is deleted
- `ClanUpgradeEvent` - Fired when a clan is upgraded
- `UserPointsChangedEvent` - Fired when a user's points change

### DataSource Interface

Implement custom storage backends:

```kotlin
interface DataSource {
    fun connect()
    fun disconnect()
    suspend fun loadUser(uuid: UUID): User?
    suspend fun saveUser(user: User)
    suspend fun removeUser(user: User)
    suspend fun loadClan(tag: String): Clan?
    suspend fun saveClan(clan: Clan)
    suspend fun deleteClan(tag: String)
    suspend fun getAllClans(): List<Clan>
}
```

### Points Calculation

Implement custom points strategies:

```kotlin
interface Points {
    fun calculate(winner: User, loser: User): Pair<Int, Int>
}
```

## Building from Source

1. Clone the repository
2. Run `./gradlew build`
3. Find the built JAR in `spigot/build/libs/`

## Building from Source

1. Clone the repository
2. Run `./gradlew build`
3. Find the built JAR in `spigot/build/libs/`

## Dependencies

- Kotlin 2.0.10
- Paper/Spigot API 1.20.1
- MongoDB BSON 4.9.0
- Kotlinx Coroutines 1.7.1

## Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues for bugs and feature requests.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Version

Current version: 0.1