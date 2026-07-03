package io.github.thebusybiscuit.souljars;

import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Material;
import io.github.thebusybiscuit.slimefun5.libraries.keys.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import io.github.thebusybiscuit.slimefun5.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun5.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun5.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun5.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun5.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun5.implementation.items.blocks.BrokenSpawner;
import io.github.thebusybiscuit.slimefun5.implementation.items.blocks.UnplaceableBlock;
import io.github.thebusybiscuit.slimefun5.libraries.dough.config.Config;
import io.github.thebusybiscuit.slimefun5.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun5.utils.ChatUtils;

public class SoulJars extends JavaPlugin implements Listener, SlimefunAddon {

    private static final String JAR_TEXTURE = "bd1c777ee166c47cae698ae6b769da4e2b67f468855330ad7bddd751c5293f";
    private final Map<EntityType, Integer> mobs = new EnumMap<>(EntityType.class);

    private Config cfg;
    private ItemGroup itemGroup;
    private RecipeType recipeType;
    private SlimefunItemStack emptyJar;

    @Override
    public void onEnable() {
        cfg = new Config(this);

        emptyJar = new SlimefunItemStack("SOUL_JAR", JAR_TEXTURE, "&bSoul Jar &7(Empty)", "", "&rKill a Mob while having this", "&rItem in your Inventory to bind", "&rtheir Soul to this Jar");
        itemGroup = new ItemGroup(new NamespacedKey(this, "soul_jars"), createCustomItem(Material.GLASS_BOTTLE, "&bSoul Jars", "", "&a> Click to open"));
        recipeType = new RecipeType(new NamespacedKey(this, "mob_killing"), createCustomItem(Material.DIAMOND_SWORD, "&cKill the specified Mob", "&cwhile having an empty Soul Jar", "&cin your Inventory"));

        new SlimefunItem(itemGroup, emptyJar, RecipeType.ANCIENT_ALTAR, new ItemStack[] { SlimefunItems.EARTH_RUNE.asOne(), new ItemStack(Material.SOUL_SAND), SlimefunItems.WATER_RUNE.asOne(), new ItemStack(Material.SOUL_SAND), SlimefunItems.NECROTIC_SKULL.asOne(), new ItemStack(Material.SOUL_SAND), SlimefunItems.AIR_RUNE.asOne(), new ItemStack(Material.SOUL_SAND), SlimefunItems.FIRE_RUNE.asOne() }, emptyJar.asQuantity(3)).register(this);
        new JarsListener(this);

        for (String mob : cfg.getStringList("mobs")) {
            try {
                EntityType type = EntityType.valueOf(mob);
                registerSoul(type);
            } catch (Exception x) {
                getLogger().log(Level.WARNING, "{0}: Possibly invalid mob type: {1}", new Object[] { x.getClass().getSimpleName(), mob });
            }
        }

        cfg.save();
    }

    private void registerSoul(EntityType type) {
        String name = ChatUtils.humanize(type.name());

        int souls = cfg.getOrSetDefault("souls-required." + type.toString(), 128);
        mobs.put(type, souls);

        Material mobEgg = Material.getMaterial(type.toString() + "_SPAWN_EGG");

        if (mobEgg == null) {
            mobEgg = Material.ZOMBIE_SPAWN_EGG;
        }

        // @formatter:off
        SlimefunItemStack jarItem = new SlimefunItemStack(type.name() + "_SOUL_JAR", JAR_TEXTURE, "&cSoul Jar &7(" + name + ")", "", "&7Infused Souls: &e1");
        SlimefunItem jar = new UnplaceableBlock(itemGroup, jarItem, recipeType,
                new ItemStack[] { null, null, null, emptyJar.asOne(), null, createCustomItem(mobEgg, "&rKill " + souls + "x " + name), null, null, null });
        jar.register(this);

        SlimefunItemStack filledJarItem = new SlimefunItemStack("FILLED_" + type.name() + "_SOUL_JAR", JAR_TEXTURE, "&cFilled Soul Jar &7(" + name + ")", "", "&7Infused Souls: &e" + souls);
        SlimefunItem filledJar = new FilledJar(itemGroup, filledJarItem, recipeType,
                new ItemStack[] { null, null, null, emptyJar.asOne(), null, createCustomItem(mobEgg, "&rKill " + souls + "x " + name), null, null, null });
        filledJar.register(this);

        BrokenSpawner brokenSpawner = SlimefunItems.BROKEN_SPAWNER.getItem(BrokenSpawner.class);

        SlimefunItemStack spawnerItem = new SlimefunItemStack(type.toString() + "_BROKEN_SPAWNER", Material.SPAWNER, "&cBroken Spawner &7(" + name + ")");
        new SlimefunItem(itemGroup, spawnerItem, RecipeType.ANCIENT_ALTAR,
                new ItemStack[] { new ItemStack(Material.IRON_BARS), SlimefunItems.EARTH_RUNE.asOne(), new ItemStack(Material.IRON_BARS), SlimefunItems.EARTH_RUNE.asOne(), filledJarItem.asOne(), SlimefunItems.EARTH_RUNE.asOne(), new ItemStack(Material.IRON_BARS), SlimefunItems.EARTH_RUNE.asOne(), new ItemStack(Material.IRON_BARS) },
        brokenSpawner.getItemForEntityType(type)).register(this);
        // @formatter:on
    }

    public Map<EntityType, Integer> getRequiredSouls() {
        return mobs;
    }

    @Override
    public JavaPlugin getJavaPlugin() {
        return this;
    }

    @Override
    public String getBugTrackerURL() {
        return "https://github.com/kaiquebaccas/SoulJars-Slimefun5/issues";
    }

    private ItemStack createCustomItem(ItemStack baseItem, String name, String... lore) {
        ItemStack item = baseItem.clone();
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', name));
        java.util.List<String> translatedLore = new java.util.ArrayList<>();
        for (String line : lore) {
            translatedLore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', line));
        }
        meta.setLore(translatedLore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCustomItem(Material material, String name, String... lore) {
        return createCustomItem(new ItemStack(material), name, lore);
    }

    private ItemStack createCustomItem(ItemStack baseItem, int amount) {
        ItemStack item = baseItem.clone();
        item.setAmount(amount);
        return item;
    }
}