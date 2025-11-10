package live.maquq.spigot.clans.configuration.impl

import live.maquq.spigot.clans.configuration.ConfigTemplate
import org.bukkit.Material

class GuiConfiguration : ConfigTemplate {

    var panel: Panel = Panel()
    var upgradePanel: Panel.UpgradePanel = Panel.UpgradePanel()
    var upgradeMenu: UpgradeMenu = UpgradeMenu()

    class Panel : ConfigTemplate {
        var title: String = "[gold]Menu klanów"

        class UpgradePanel : ConfigTemplate {
            var material: Material = Material.FURNACE
            var title: String = "[gold]Wejdź do menu ulepszania"
            var lore: List<String> = listOf(
                "",
                " Tutaj możesz zobaczyć menu ulepszania klanów",
                " "
            )
        }
    }

    class UpgradeMenu : ConfigTemplate {
        var title: String = "[gold]Ulepszenia klanu"
        var rows: Int = 3 // inventory rows (1-6)
        var background: Material = Material.GRAY_STAINED_GLASS_PANE

        var sizeItem: UpgradeItem = UpgradeItem(
            slot = 11,
            material = Material.CHEST,
            title = "[yellow]Powiększ rozmiar klanu",
            lore = listOf(
                "[gray]Aktualny: [CURRENT]",
                "[gray]Następny: [NEXT]",
                "[gray]Koszt: [COST]x [ITEM]",
                "",
                "[green]Kliknij, aby ulepszyć"
            ),
            disabledTitle = "[red]Maksymalny rozmiar",
            disabledLore = listOf(
                "[gray]Aktualny: [CURRENT]",
                "[gray]Maksymalny osiągnięty"
            )
        )

        var pointsItem: UpgradeItem = UpgradeItem(
            slot = 15,
            material = Material.NETHER_STAR,
            title = "[yellow]Mnożnik punktów",
            lore = listOf(
                "[gray]Aktualny: [CURRENT]x",
                "[gray]Następny: [NEXT]x ([STEP]x)",
                "[gray]Koszt: [COST]x [ITEM]",
                "",
                "[green]Kliknij, aby ulepszyć"
            ),
            disabledTitle = "[red]Maksymalny mnożnik",
            disabledLore = listOf(
                "[gray]Aktualny: [CURRENT]x",
                "[gray]Maksymalny osiągnięty"
            )
        )

        var backItem: SimpleItem = SimpleItem(
            slot = 22,
            material = Material.BARRIER,
            title = "[red]Wróć"
        )

        class UpgradeItem(
            var slot: Int = 0,
            var material: Material = Material.STONE,
            var title: String = "",
            var lore: List<String> = emptyList(),
            var disabledTitle: String = "",
            var disabledLore: List<String> = emptyList()
        ) : ConfigTemplate

        class SimpleItem(
            var slot: Int = 0,
            var material: Material = Material.BARRIER,
            var title: String = ""
        ) : ConfigTemplate
    }
}