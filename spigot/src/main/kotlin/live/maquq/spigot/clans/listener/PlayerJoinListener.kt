package live.maquq.spigot.clans.listener

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import live.maquq.spigot.clans.manager.UserManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class PlayerJoinListener(
    private val userManager: UserManager,
    private val scope: CoroutineScope
) : Listener {
    @EventHandler
    fun handlePlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        this.scope.launch {
            userManager.getUser(player.uniqueId) ?: userManager.createNewUser(player)
        }

//        this.scope.launch {
//            runCatching {
//                // --- Krok 1: Wczytaj lub stwórz użytkownika ---
//                var user = userManager.getUser(player.uniqueId)
//
//                if (user == null) {
//                    logger.info("Gracz ${player.name} dołącza po raz pierwszy. Tworzenie profilu...")
//                    user = userManager.createNewUser(player)
//                    player.sendMessage("${ChatColor.GREEN}Witaj po raz pierwszy! Tworzymy Twój profil...")
//                } else {
//                    logger.info("Gracz ${player.name} dołączył ponownie. Wczytywanie profilu...")
//                    player.sendMessage("${ChatColor.GREEN}Witaj ponownie!")
//                }
//
//                // --- Krok 2: Logika "Testu Jednostkowego" ---
//                // Zapisujemy stare wartości, aby pokazać zmianę.
//                val oldKills = user.kills
//                val oldDeaths = user.deaths
//                val oldPoints = user.points
//
//                // Generujemy nowe, losowe wartości.
//                val newKills = Random.nextInt(0, 100)
//                val newDeaths = Random.nextInt(0, 50)
//                val newPoints = Random.nextInt(0, 1000)
//
//                logger.debug("Test dla ${player.name}: Zmieniam statystyki z (K:$oldKills, D:$oldDeaths, P:$oldPoints) na (K:$newKills, D:$newDeaths, P:$newPoints)")
//
//                // Modyfikujemy obiekt użytkownika.
//                user.kills = newKills
//                user.deaths = newDeaths
//                user.points = newPoints
//
//                // --- Krok 3: Zapisz zmiany w bazie danych ---
//                userManager.saveUser(user)
//                logger.debug("Zaktualizowany profil dla ${player.name} został zapisany w bazie danych.")
//
//                // --- Krok 4: Wyślij wiadomość zwrotną do gracza ---
//                player.sendMessage("${ChatColor.GRAY}=======================================")
//                player.sendMessage("${ChatColor.YELLOW}Test Danych (Każde dołączenie generuje nowe):")
//                player.sendMessage("${ChatColor.AQUA}Poprzednie statystyki: ${ChatColor.WHITE}K: $oldKills, D: $oldDeaths, P: $oldPoints")
//                player.sendMessage("${ChatColor.AQUA}Nowe, zapisane statystyki: ${ChatColor.WHITE}K: $newKills, D: $newDeaths, P: $newPoints")
//                player.sendMessage("${ChatColor.GRAY}=======================================")
//                player.sendMessage("${ChatColor.GOLD}Jeśli przy następnym dołączeniu 'Poprzednie statystyki' będą się zgadzać z 'Nowymi', to system działa!")
//
//            }.onFailure { exception ->
//                logger.error("Wystąpił krytyczny błąd podczas obsługi dołączenia gracza ${player.name}!", exception)
//                player.sendMessage("${ChatColor.RED}Wystąpił wewnętrzny błąd podczas ładowania Twojego profilu. Zgłoś to administracji.")
//            }
//        }
    }
}