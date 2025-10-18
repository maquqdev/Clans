package live.maquq.spigot.gui.builder

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import com.bruhdows.minitext.MiniText

class ItemBuilder(
    private val material: Material,
    private val miniText: MiniText
) {
    private var displayName: Component? = null
    private var lore: List<Component>? = null
    private val enchantments: MutableMap<Enchantment, Int> = mutableMapOf()
    private val itemFlags: MutableSet<ItemFlag> = mutableSetOf()
    private var amount: Int = 1
    private var customModelData: Int? = null
    private var unbreakable: Boolean = false

    fun name(text: String): ItemBuilder {
        this.displayName = miniText.deserialize(text).component()
        return this
    }

    fun lore(vararg lines: String): ItemBuilder {
        this.lore = lines.map { miniText.deserialize(it).component() }
        return this
    }

    fun lore(lines: List<String>): ItemBuilder {
        this.lore = lines.map { miniText.deserialize(it).component() }
        return this
    }

    fun enchant(enchantment: Enchantment, level: Int): ItemBuilder {
        this.enchantments[enchantment] = level
        return this
    }

    fun flag(vararg flags: ItemFlag): ItemBuilder {
        this.itemFlags.addAll(flags)
        return this
    }

    fun amount(amount: Int): ItemBuilder {
        this.amount = amount
        return this
    }

    fun customModelData(data: Int): ItemBuilder {
        this.customModelData = data
        return this
    }

    fun unbreakable(unbreakable: Boolean = true): ItemBuilder {
        this.unbreakable = unbreakable
        return this
    }

    fun build(): ItemStack {
        val item = ItemStack(material, amount)
        val meta = item.itemMeta ?: return item

        displayName?.let { meta.displayName(it) }
        lore?.let { meta.lore(it) }

        customModelData?.let { meta.setCustomModelData(it) }

        enchantments.forEach { (enchant, level) ->
            meta.addEnchant(enchant, level, true)
        }

        itemFlags.forEach { meta.addItemFlags(it) }

        meta.isUnbreakable = unbreakable

        item.itemMeta = meta
        return item
    }
}

fun ItemStack.displayName(component: Component): ItemStack {
    itemMeta = itemMeta?.apply {
        displayName(component)
    }
    return this
}

fun ItemStack.displayName(text: String, miniMessage: MiniMessage): ItemStack {
    itemMeta = itemMeta?.apply {
        displayName(miniMessage.deserialize(text))
    }
    return this
}

fun ItemStack.lore(lines: List<Component>): ItemStack {
    itemMeta = itemMeta?.apply {
        lore(lines)
    }
    return this
}

fun ItemStack.lore(lines: List<String>, miniMessage: MiniMessage): ItemStack {
    itemMeta = itemMeta?.apply {
        lore(lines.map { miniMessage.deserialize(it) })
    }
    return this
}