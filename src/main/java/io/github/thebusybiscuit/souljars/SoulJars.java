package io.github.thebusybiscuit.souljars;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import io.github.thebusybiscuit.slimefun5.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun5.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun5.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun5.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun5.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun5.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun5.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun5.implementation.items.blocks.BrokenSpawner;
import io.github.thebusybiscuit.slimefun5.implementation.items.blocks.UnplaceableBlock;
import io.github.thebusybiscuit.slimefun5.libraries.dough.config.Config;
import io.github.thebusybiscuit.slimefun5.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun5.libraries.keys.NamespacedKey;
import io.github.thebusybiscuit.slimefun5.libraries.xseries.XMaterial;
import io.github.thebusybiscuit.slimefun5.utils.ChatUtils;
import io.github.thebusybiscuit.slimefun5.utils.compatibility.MaterialCompat;

public class SoulJars extends JavaPlugin implements Listener, SlimefunAddon {

    private static final String JAR_TEXTURE = "bd1c777ee166c47cae698ae6b769da4e2b67f468855330ad7bddd751c5293f";
    private final Map<EntityType, Integer> mobs = new EnumMap<>(EntityType.class);

    private Config cfg;
    private ItemGroup itemGroup;
    private RecipeType recipeType;
    private SlimefunItemStack emptyJar;

    private String recipeHintTemplate;
    private String soulsLineTemplate;
    private String soulsLineMarker;

    @Override
    public void onEnable() {
        cfg = new Config(this);

        // Display templates live in config.yml (admin-editable / translatable) rather than hardcoded here.
        recipeHintTemplate = cfg.getOrSetDefault("messages.recipe-hint", "&rKill %count%x %mob%");
        soulsLineTemplate = cfg.getOrSetDefault("messages.infused-souls", "&7Infused Souls: &e%souls%");
        // The literal text before %souls% is how JarsListener finds the counter line to update it.
        soulsLineMarker = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&',
                soulsLineTemplate.split("%souls%", 2)[0])).trim().toLowerCase(Locale.ROOT);

        // id-only stacks: names/lore come from languages/<lang>/items.yml (see the SOUL_JAR entry and the
        // "%MOB%_SOUL_JAR" families), so nothing is hardcoded in English here.
        emptyJar = new SlimefunItemStack("SOUL_JAR", JAR_TEXTURE);
        itemGroup = new ItemGroup(new NamespacedKey(this, "soul_jars"), CustomItemStack.create(emptyJar.item(), "&bSoul Jars", "", "&a> Click to open"));
        itemGroup.setTheme("magic"); // soul/altar magic -> Magic theme in the themed guide

        recipeType = new RecipeType(new NamespacedKey(this, "mob_killing"), CustomItemStack.create(Material.DIAMOND_SWORD, "&cKill the specified Mob", "&cwhile having an empty Soul Jar", "&cin your Inventory"));

        SlimefunItem emptyJarItem = new SlimefunItem(itemGroup, emptyJar, RecipeType.ANCIENT_ALTAR, new ItemStack[] {
                SlimefunItems.EARTH_RUNE.asOne(), MaterialCompat.stack(XMaterial.SOUL_SAND), SlimefunItems.WATER_RUNE.asOne(),
                MaterialCompat.stack(XMaterial.SOUL_SAND), SlimefunItems.NECROTIC_SKULL.asOne(), MaterialCompat.stack(XMaterial.SOUL_SAND),
                SlimefunItems.AIR_RUNE.asOne(), MaterialCompat.stack(XMaterial.SOUL_SAND), SlimefunItems.FIRE_RUNE.asOne() }, emptyJar.asQuantity(3));
        emptyJarItem.setGuideType("magic");
        emptyJarItem.register(this);
        new JarsListener(this);

        for (String mob : cfg.getStringList("mobs")) {
            try {
                EntityType type = EntityType.valueOf(mob);
                registerSoul(type);
            } catch (IllegalArgumentException x) {
                // Mob doesn't exist on this MC version (e.g. DROWNED/EVOKER/FOX on 1.8) - silently
                // skip it, no per-mob warning spam; its soul jar simply isn't registered here.
            }
        }

        // Contribute this addon's item translations (+ families) so its display resolves per player language.
        Slimefun.getItemTranslationService().registerTranslations(this);

        cfg.save();
    }

    private void registerSoul(EntityType type) {
        String name = ChatUtils.humanize(type.name());

        int souls = cfg.getOrSetDefault("souls-required." + type.toString(), 128);
        mobs.put(type, souls);

        // Spawn-egg material differs by version (and pre-1.13 has no per-mob eggs); resolve via XMaterial
        // and fall back to a zombie egg so the recipe hint always has a valid icon.
        ItemStack mobEgg = XMaterial.matchXMaterial(type.name() + "_SPAWN_EGG").orElse(XMaterial.ZOMBIE_SPAWN_EGG).parseItem();

        if (mobEgg == null) {
            mobEgg = MaterialCompat.stack(XMaterial.ZOMBIE_SPAWN_EGG);
        }

        String recipeHint = recipeHintTemplate.replace("%count%", String.valueOf(souls)).replace("%mob%", name);

        // @formatter:off
        SlimefunItemStack jarItem = new SlimefunItemStack(type.name() + "_SOUL_JAR", JAR_TEXTURE);
        SlimefunItem jar = new UnplaceableBlock(itemGroup, jarItem, recipeType,
                new ItemStack[] { null, null, null, emptyJar.asOne(), null, CustomItemStack.create(mobEgg, recipeHint), null, null, null });
        jar.setGuideType("magic");
        jar.register(this);

        SlimefunItemStack filledJarItem = new SlimefunItemStack("FILLED_" + type.name() + "_SOUL_JAR", JAR_TEXTURE);
        SlimefunItem filledJar = new FilledJar(itemGroup, filledJarItem, recipeType,
                new ItemStack[] { null, null, null, emptyJar.asOne(), null, CustomItemStack.create(mobEgg, recipeHint), null, null, null });
        filledJar.setGuideType("magic");
        filledJar.register(this);

        BrokenSpawner brokenSpawner = SlimefunItems.BROKEN_SPAWNER.getItem(BrokenSpawner.class);

        SlimefunItemStack spawnerItem = new SlimefunItemStack(type.toString() + "_BROKEN_SPAWNER", XMaterial.SPAWNER.parseMaterial());
        SlimefunItem brokenSpawnerItem = new SlimefunItem(itemGroup, spawnerItem, RecipeType.ANCIENT_ALTAR,
                new ItemStack[] { MaterialCompat.stack(XMaterial.IRON_BARS), SlimefunItems.EARTH_RUNE.asOne(), MaterialCompat.stack(XMaterial.IRON_BARS), SlimefunItems.EARTH_RUNE.asOne(), filledJarItem.asOne(), SlimefunItems.EARTH_RUNE.asOne(), MaterialCompat.stack(XMaterial.IRON_BARS), SlimefunItems.EARTH_RUNE.asOne(), MaterialCompat.stack(XMaterial.IRON_BARS) },
        brokenSpawner.getItemForEntityType(type));
        brokenSpawnerItem.setGuideType("magic");
        brokenSpawnerItem.register(this);
        // @formatter:on
    }

    public Map<EntityType, Integer> getRequiredSouls() {
        return mobs;
    }

    /** The configured soul-counter lore template (contains %souls%). */
    public String getSoulsLineTemplate() {
        return soulsLineTemplate;
    }

    /** The lowercased, colour-stripped literal that identifies the soul-counter line for updating. */
    public String getSoulsLineMarker() {
        return soulsLineMarker;
    }

    @Override
    public JavaPlugin getJavaPlugin() {
        return this;
    }

    @Override
    public String getBugTrackerURL() {
        return "https://github.com/Slimefun5/SoulJars/issues";
    }

}
