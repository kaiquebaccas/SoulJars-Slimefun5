package io.github.thebusybiscuit.souljars;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.github.thebusybiscuit.slimefun5.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun5.libraries.dough.items.ItemUtils;
import io.github.thebusybiscuit.slimefun5.libraries.keys.NamespacedKey;
import io.github.thebusybiscuit.slimefun5.utils.compatibility.PdcCompat;

public class JarsListener implements Listener {

    private final SoulJars plugin;
    private final SlimefunItem emptyJar;

    // Cross-version PDC (dough NamespacedKey + PdcCompat) so this loads on 1.8 - 26.x; the modern
    // ItemMeta#getPersistentDataContainer API does not exist on legacy servers.
    private final NamespacedKey soulsKey;
    private final NamespacedKey mobKey;

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

            if (!PdcCompat.has(im, soulsKey, "INTEGER")) {
                continue;
            }

            String jarMob = PdcCompat.getString(im, mobKey);
            if (!e.getEntityType().name().equals(jarMob)) {
                continue;
            }

            int souls = PdcCompat.getInt(im, soulsKey, 0) + 1;
            int requiredSouls = mobs.get(e.getEntityType());

            if (souls >= requiredSouls && filledJar != null) {
                if (stack.getAmount() > 1) {
                    stack.setAmount(stack.getAmount() - 1);
                    killer.getInventory().addItem(filledJar.getItem().clone());
                } else {
                    killer.getInventory().setItem(slot, filledJar.getItem().clone());
                }
            } else {
                PdcCompat.setInt(im, soulsKey, souls);
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

            PdcCompat.setInt(im, soulsKey, 1);
            PdcCompat.setString(im, mobKey, e.getEntityType().name());
            updateSoulsLore(im, 1);

            newJar.setItemMeta(im);

            killer.getWorld().dropItemNaturally(e.getEntity().getLocation(), newJar);
            return;
        }
    }

    /**
     * Sets the "Infused Souls" display line to the given count, replacing the existing one if present
     * (identified by the "Infused Souls" marker JarsListener itself writes) or appending it otherwise.
     * The authoritative count lives in persistent data; this line is purely cosmetic.
     */
    private void updateSoulsLore(ItemMeta im, int souls) {
        List<String> lore = im.getLore() != null ? im.getLore() : new ArrayList<>();
        String line = ChatColor.translateAlternateColorCodes('&', "&7Infused Souls: &e" + souls);

        for (int i = 0; i < lore.size(); i++) {
            if (ChatColor.stripColor(lore.get(i)).toLowerCase(Locale.ROOT).contains("infused souls")) {
                lore.set(i, line);
                im.setLore(lore);
                return;
            }
        }

        lore.add(line);
        im.setLore(lore);
    }

    @EventHandler
    public void onJarPlace(BlockPlaceEvent e) {
        ItemStack item = e.getItemInHand();
        if (item != null && item.hasItemMeta() && PdcCompat.has(item.getItemMeta(), soulsKey, "INTEGER")) {
            e.setCancelled(true);
        }
    }

}
