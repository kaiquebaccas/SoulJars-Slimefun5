package io.github.thebusybiscuit.souljars;

import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.github.thebusybiscuit.slimefun5.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun5.libraries.dough.items.ItemUtils;

public class JarsListener implements Listener {

    private final SoulJars plugin;
    private final SlimefunItem emptyJar;

    public JarsListener(SoulJars plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        this.plugin = plugin;
        this.emptyJar = SlimefunItem.getById("SOUL_JAR");
    }

    @EventHandler
    public void onKill(EntityDeathEvent e) {
        Map<EntityType, Integer> mobs = plugin.getRequiredSouls();
        if (!mobs.containsKey(e.getEntityType()) || e.getEntity().getKiller() == null) {
            return;
        }

        Player killer = e.getEntity().getKiller();

        SlimefunItem jar = SlimefunItem.getById(e.getEntityType().name() + "_SOUL_JAR");
        SlimefunItem filledJar = SlimefunItem.getById("FILLED_" + e.getEntityType().name() + "_SOUL_JAR");

        org.bukkit.NamespacedKey soulsKey = new org.bukkit.NamespacedKey(plugin, "captured_souls");
        org.bukkit.NamespacedKey mobKey = new org.bukkit.NamespacedKey(plugin, "jar_mob_type");
        org.bukkit.NamespacedKey sfIdKey = org.bukkit.NamespacedKey.fromString("slimefun:slimefun_item");

        for (int slot = 0; slot < killer.getInventory().getSize(); slot++) {
            ItemStack stack = killer.getInventory().getItem(slot);

            if (stack == null || !stack.hasItemMeta()) continue;
            ItemMeta im = stack.getItemMeta();

            if (im.getPersistentDataContainer().has(soulsKey, org.bukkit.persistence.PersistentDataType.INTEGER)) {
                String jarMob = im.getPersistentDataContainer().get(mobKey, org.bukkit.persistence.PersistentDataType.STRING);

                if (e.getEntityType().name().equals(jarMob)) {
                    int souls = im.getPersistentDataContainer().getOrDefault(soulsKey, org.bukkit.persistence.PersistentDataType.INTEGER, 0);
                    souls++;

                    int requiredSouls = mobs.get(e.getEntityType());

                    if (souls >= requiredSouls) {
                        if (stack.getAmount() > 1) {
                            stack.setAmount(stack.getAmount() - 1);
                            killer.getInventory().addItem(filledJar.getItem().clone());
                        } else {
                            killer.getInventory().setItem(slot, filledJar.getItem().clone());
                        }
                    } else {
                        im.getPersistentDataContainer().set(soulsKey, org.bukkit.persistence.PersistentDataType.INTEGER, souls);

                        List<String> lore = im.getLore();
                        if (lore != null && lore.size() > 1) {
                            String originalLine = ChatColor.stripColor(lore.get(1));
                            String prefix = originalLine.contains(":") ? originalLine.split(":")[0] : "Souls";
                            lore.set(1, ChatColor.translateAlternateColorCodes('&', "&7" + prefix + ": &e" + souls));
                            im.setLore(lore);
                        }

                        if (stack.getAmount() > 1) {
                            stack.setAmount(stack.getAmount() - 1);
                            ItemStack updatedJar = stack.clone();
                            updatedJar.setAmount(1);
                            updatedJar.setItemMeta(im);
                            killer.getInventory().addItem(updatedJar);
                        } else {
                            stack.setItemMeta(im);
                        }
                    }
                    return;
                }
            }
        }

        for (int slot = 0; slot < killer.getInventory().getSize(); slot++) {
            ItemStack stack = killer.getInventory().getItem(slot);

            if (emptyJar != null && jar != null && emptyJar.isItem(stack)) {
                ItemUtils.consumeItem(stack, false);

                ItemStack newJar = jar.getItem().clone();
                ItemMeta im = newJar.getItemMeta();

                if (sfIdKey != null) {
                    // Remove the Slimefun item ID so the jar becomes a regular Bukkit item.
                    // This prevents Slimefun5 from rebuilding the ItemMeta and resetting the
                    // captured soul count on newer Slimefun versions.
                    im.getPersistentDataContainer().remove(sfIdKey);
                }

                im.getPersistentDataContainer().set(soulsKey, org.bukkit.persistence.PersistentDataType.INTEGER, 1);
                im.getPersistentDataContainer().set(mobKey, org.bukkit.persistence.PersistentDataType.STRING, e.getEntityType().name());

                List<String> lore = im.getLore();
                if (lore != null && lore.size() > 1) {
                    String originalLine = ChatColor.stripColor(lore.get(1));
                    String prefix = originalLine.contains(":") ? originalLine.split(":")[0] : "Souls";
                    lore.set(1, ChatColor.translateAlternateColorCodes('&', "&7" + prefix + ": &e1"));
                    im.setLore(lore);
                }
                newJar.setItemMeta(im);

                killer.getWorld().dropItemNaturally(e.getEntity().getLocation(), newJar);
                return;
            }
        }
    }

    @EventHandler
    public void onJarPlace(org.bukkit.event.block.BlockPlaceEvent e) {
        ItemStack item = e.getItemInHand();
        if (item != null && item.hasItemMeta()) {
            if (item.getItemMeta().getPersistentDataContainer().has(new org.bukkit.NamespacedKey(plugin, "captured_souls"), org.bukkit.persistence.PersistentDataType.INTEGER)) {
                e.setCancelled(true);
            }
        }
    }

}
