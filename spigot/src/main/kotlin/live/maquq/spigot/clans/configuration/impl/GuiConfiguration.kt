package live.maquq.spigot.clans.configuration.impl

import live.maquq.spigot.clans.configuration.ConfigTemplate
import org.bukkit.Material

class GuiConfiguration : ConfigTemplate {

    var panel: Panel = Panel()
    var upgradePanel: Panel.UpgradePanel = Panel.UpgradePanel()

    class Panel : ConfigTemplate {
        var title: String = "[gold]Menu klanów"

        class UpgradePanel : ConfigTemplate {
            var  material: Material = Material.FURNACE
            var title: String = "[gold]Wejdź do menu ulepszania"
            var lore: List<String> = listOf(
                "",
                " Tutaj możesz zobaczyć menu ulepszania klanów",
                " "
            )
        }
    }
}