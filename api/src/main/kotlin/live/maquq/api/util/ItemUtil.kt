package live.maquq.api.util

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object ItemUtil {

    fun takeItems(player: Player, item: ItemStack, amount: Int): Boolean {
        if (!hasItems(player, item, amount)) return false

        var remaining = amount
        val inventory = player.inventory.contents

        for (i in inventory.indices) {
            val slot = inventory[i] ?: continue
            if (!slot.isSimilar(item)) continue

            val slotAmount = slot.amount
            if (slotAmount >= remaining) {
                slot.amount = slotAmount - remaining
                return true
            }

            remaining -= slotAmount
            inventory[i] = null
        }

        return true
    }

    private fun hasItems(player: Player, item: ItemStack, amount: Int): Boolean {
        var count = 0

        for (slot in player.inventory.contents) {
            if (slot != null && slot.isSimilar(item)) {
                count += slot.amount
                if (count >= amount) return true
            }
        }

        return false
    }
}