package imperatrix.wish.util;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemBuilder {
    private final ItemStack itemStack;

    public ItemBuilder(Material material) {
        this.itemStack = new ItemStack(material);
    }

    public ItemBuilder(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public ItemStack build() {
        return this.itemStack;
    }

    public ItemBuilder addEnchantment(Enchantment enchantment, int level) {
        this.itemStack.addUnsafeEnchantment(enchantment, level - 1);
        return this;
    }

    public ItemBuilder addEnchantments(HashMap<Enchantment, Integer> enchantments) {
        this.itemStack.addUnsafeEnchantments(enchantments);
        return this;
    }

    public ItemBuilder setAmount(int amount) {
        this.itemStack.setAmount(amount);
        return this;
    }

    public ItemBuilder setDisplayName(String name) {
        ItemMeta itemMeta = this.itemStack.getItemMeta();
        if (itemMeta == null)
            return this;
        itemMeta.setDisplayName(Utils.formatString(name));
        this.itemStack.setItemMeta(itemMeta);
        return this;
    }

    public ItemBuilder setLore(List<String> lore) {
        ItemMeta itemMeta = this.itemStack.getItemMeta();
        if (itemMeta == null)
            return this;
        List<String> newLore = new ArrayList<>();
        lore.forEach(s -> newLore.add(Utils.formatString(s)));
        itemMeta.setLore(newLore);
        this.itemStack.setItemMeta(itemMeta);
        return this;
    }

    public ItemBuilder setCustomModelData(int data) {
        ItemMeta itemMeta = this.itemStack.getItemMeta();
        if (itemMeta != null) {
            itemMeta.setCustomModelData(data);
            this.itemStack.setItemMeta(itemMeta);
        }
        return this;
    }

    public ItemBuilder setVariables(HashMap<String, String> variableMap) {
        ItemMeta itemMeta = this.itemStack.getItemMeta();
        if (itemMeta == null)
            return this;
        for (Map.Entry<String, String> variableEntry : variableMap.entrySet()) {
            List<String> lore = itemMeta.getLore();
            List<String> newLore = new ArrayList<>();
            if (lore != null) {
                for (String loreLine : lore)
                    newLore.add(loreLine.replace(variableEntry.getKey(), variableEntry.getValue()));
                itemMeta.setLore(newLore);
            }
            itemMeta.setDisplayName(itemMeta.getDisplayName().replace(variableEntry.getKey(), variableEntry.getValue()));
            this.itemStack.setItemMeta(itemMeta);
        }
        return this;
    }
}