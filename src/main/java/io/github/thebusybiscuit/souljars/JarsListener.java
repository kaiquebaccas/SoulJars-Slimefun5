package io.github.thebusybiscuit.souljars;

import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import io.github.thebusybiscuit.slimefun5.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun5.libraries.dough.items.ItemUtils;

public class JarsListener implements Listener {

    private final SoulJars plugin;
    private final SlimefunItem emptyJar;

    private final NamespacedKey soulsKey;
    private final NamespacedKey mobKey;
    private final NamespacedKey sfIdKey = NamespacedKey.fromString("slimefun:slimefun_item");

    public JarsListener(SoulJars plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        this.plugin = plugin;
        this.emptyJar = SlimefunItem.getById("SOUL_JAR");

        this.soulsKey = new NamespacedKey(plugin, "captured_souls");
        this.mobKey = new NamespacedKey(plugin, "jar_mob_type");
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

        if (tryProgressExistingJar(e, killer, mobs, filledJar)) {
            return;
        }

        tryCreateNewJar(e, killer, jar);
    }

    private boolean tryProgressExistingJar(EntityDeathEvent e, Player killer, Map<EntityType, Integer> mobs, SlimefunItem filledJar) {
        for (int slot = 0; slot < killer.getInventory().getSize(); slot++) {
            ItemStack stack = killer.getInventory().getItem(slot);

            if (stack == null || !stack.hasItemMeta()) {
                continue;
            }

            ItemMeta im = stack.getItemMeta();

            if (!im.getPersistentDataContainer().has(soulsKey, PersistentDataType.INTEGER)) {
                continue;
            }

            String jarMob = im.getPersistentDataContainer().get(mobKey, PersistentDataType.STRING);
            if (!e.getEntityType().name().equals(jarMob)) {
                continue;
            }

            int souls = im.getPersistentDataContainer().getOrDefault(soulsKey, PersistentDataType.INTEGER, 0) + 1;
            int requiredSouls = mobs.get(e.getEntityType());

            if (souls >= requiredSouls) {
                if (stack.getAmount() > 1) {
                    stack.setAmount(stack.getAmount() - 1);
                    killer.getInventory().addItem(filledJar.getItem().clone());
                } else {
                    killer.getInventory().setItem(slot, filledJar.getItem().clone());
                }
            } else {
                im.getPersistentDataContainer().set(soulsKey, PersistentDataType.INTEGER, souls);
                updateSoulsLore(im, souls);

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

            return true;
        }

        return false;
    }

    private void tryCreateNewJar(EntityDeathEvent e, Player killer, SlimefunItem jar) {
        for (int slot = 0; slot < killer.getInventory().getSize(); slot++) {
            ItemStack stack = killer.getInventory().getItem(slot);

            if (emptyJar == null || jar == null || !emptyJar.isItem(stack)) {
                continue;
            }

            ItemUtils.consumeItem(stack, false);

            ItemStack newJar = jar.getItem().clone();
            ItemMeta im = newJar.getItemMeta();

            if (sfIdKey != null) {
                // Remove the Slimefun item ID so the jar becomes a regular Bukkit item.
                // This prevents Slimefun5 from rebuilding the ItemMeta and resetting the
                // captured soul count on newer Slimefun versions.
                im.getPersistentDataContainer().remove(sfIdKey);
            }

            im.getPersistentDataContainer().set(soulsKey, PersistentDataType.INTEGER, 1);
            im.getPersistentDataContainer().set(mobKey, PersistentDataType.STRING, e.getEntityType().name());
            updateSoulsLore(im, 1);

            newJar.setItemMeta(im);

            killer.getWorld().dropItemNaturally(e.getEntity().getLocation(), newJar);
            return;
        }
    }

    private void updateSoulsLore(ItemMeta im, int souls) {
        List<String> lore = im.getLore();
        if (lore == null || lore.size() <= 1) {
            return;
        }

        String originalLine = ChatColor.stripColor(lore.get(1));
        String prefix = originalLine.contains(":") ? originalLine.split(":")[0] : "Souls";
        lore.set(1, ChatColor.translateAlternateColorCodes('&', "&7" + prefix + ": &e" + souls));
        im.setLore(lore);
    }

    @EventHandler
    public void onJarPlace(BlockPlaceEvent e) {
        ItemStack item = e.getItemInHand();
        if (item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(soulsKey, PersistentDataType.INTEGER)) {
            e.setCancelled(true);
        }
    }

}