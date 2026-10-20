package com.skyframework.healingupgrade.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.skyframework.healingupgrade.HealingUpgradeMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * config/healingupgrade.json. Se crea con valores por defecto si no existe y se vuelve a escribir al cargar,
 * para que aparezcan los campos nuevos tras actualizar el mod. Recargable con /healingupgrade reload.
 */
public final class HealingUpgradeConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve(HealingUpgradeMod.MOD_ID + ".json");

	private static volatile HealingUpgradeConfig instance = new HealingUpgradeConfig();

	/** Segundos entre curaciones por jugador (10 min por defecto). 0 = sin cooldown. Se muestra en el boton. */
	public int cooldownSeconds = 600;
	public Cost cost = new Cost();
	/** Sonido de la maquina de curacion de Cobblemon y particulas alrededor del jugador. */
	public boolean playEffects = true;
	/** Igual que la maquina de curacion: no se puede curar en mitad de un combate. */
	public boolean allowInBattle = false;
	/**
	 * Mochilas colocadas como bloque no tienen dueno; por defecto solo cura si la mochila la lleva encima
	 * el propio jugador (inventario, accesorios o mochila dentro de otra mochila suya).
	 */
	public boolean allowPlacedBackpacks = false;

	public static final class Cost {
		/** Si es true, cada curacion consume {@code count} unidades de {@code item} del inventario de la mochila. */
		public boolean enabled = false;
		public String item = "cobblemon:super_potion";
		public int count = 1;
	}

	/**
	 * Receta de crafteo (mesa de crafteo, con forma). Se aplica al arrancar y con /reload.
	 * Cada valor de {@code key} es un id de item ("cobblemon:revive") o un tag con # ("#c:ingots/iron").
	 */
	public Recipe recipe = new Recipe();

	public static final class Recipe {
		public List<String> pattern = new ArrayList<>(List.of("OMO", "HBC", " D "));
		public Map<String, String> key = defaultKey();
		public int count = 1;

		private static Map<String, String> defaultKey() {
			Map<String, String> key = new LinkedHashMap<>();
			key.put("O", "#c:ingots/gold");
			key.put("M", "cobblemon:healing_machine");
			key.put("H", "cobblemon:hyper_potion");
			key.put("B", "sophisticatedbackpacks:upgrade_base");
			key.put("C", "cobblemon:full_heal");
			key.put("D", "#c:gems/diamond");
			return key;
		}
	}

	public static HealingUpgradeConfig get() {
		return instance;
	}

	public static void load() {
		HealingUpgradeConfig loaded = null;
		if (Files.exists(PATH)) {
			try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
				loaded = GSON.fromJson(reader, HealingUpgradeConfig.class);
			} catch (IOException | JsonParseException e) {
				HealingUpgradeMod.LOGGER.error("No se pudo leer {}, se usan los valores por defecto", PATH, e);
			}
		}
		if (loaded == null) {
			loaded = new HealingUpgradeConfig();
		}
		if (loaded.cost == null) {
			loaded.cost = new Cost();
		}
		if (loaded.recipe == null) {
			loaded.recipe = new Recipe();
		}
		if (loaded.recipe.pattern == null || loaded.recipe.key == null) {
			Recipe defaults = new Recipe();
			loaded.recipe.pattern = loaded.recipe.pattern == null ? defaults.pattern : loaded.recipe.pattern;
			loaded.recipe.key = loaded.recipe.key == null ? defaults.key : loaded.recipe.key;
		}
		loaded.recipe.count = Math.max(1, loaded.recipe.count);
		loaded.cooldownSeconds = Math.max(0, loaded.cooldownSeconds);
		loaded.cost.count = Math.max(1, loaded.cost.count);
		instance = loaded;
		save();
	}

	private static void save() {
		try {
			Files.createDirectories(PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
				GSON.toJson(instance, writer);
			}
		} catch (IOException e) {
			HealingUpgradeMod.LOGGER.error("No se pudo escribir {}", PATH, e);
		}
	}
}
