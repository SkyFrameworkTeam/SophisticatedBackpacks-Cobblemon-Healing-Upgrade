package com.skyframework.healingupgrade.recipe;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.skyframework.healingupgrade.HealingUpgradeMod;
import com.skyframework.healingupgrade.config.HealingUpgradeConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Tipo de receta {@code healingupgrade:config_shaped}: ignora el contenido del JSON y construye una receta con forma
 * vanilla ({@link ShapedRecipe}) a partir de la seccion {@code recipe} de config/healingupgrade.json.
 * <p>
 * La receta resultante usa el serializador vanilla, asi que el cliente, REI y JEI la ven como una receta normal.
 * La config se vuelve a leer cada vez que se cargan los datapacks (arranque del servidor y /reload).
 */
public final class ConfigShapedRecipeSerializer implements RecipeSerializer<ShapedRecipe> {
	public static final ConfigShapedRecipeSerializer INSTANCE = new ConfigShapedRecipeSerializer();

	private static final MapCodec<ShapedRecipe> CODEC = new MapCodec<>() {
		@Override
		public <T> Stream<T> keys(DynamicOps<T> ops) {
			return Stream.empty();
		}

		@Override
		public <T> DataResult<ShapedRecipe> decode(DynamicOps<T> ops, MapLike<T> input) {
			HealingUpgradeConfig.load();
			return build(HealingUpgradeConfig.get().recipe);
		}

		@Override
		public <T> RecordBuilder<T> encode(ShapedRecipe input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
			return prefix;
		}
	};

	private ConfigShapedRecipeSerializer() {
	}

	@Override
	public MapCodec<ShapedRecipe> codec() {
		return CODEC;
	}

	@Override
	public StreamCodec<RegistryFriendlyByteBuf, ShapedRecipe> streamCodec() {
		// No se usa para enviar (ShapedRecipe#getSerializer devuelve el vanilla), pero el contrato lo exige.
		return RecipeSerializer.SHAPED_RECIPE.streamCodec();
	}

	private static DataResult<ShapedRecipe> build(HealingUpgradeConfig.Recipe config) {
		Map<Character, Ingredient> key = new HashMap<>();
		for (Map.Entry<String, String> entry : config.key.entrySet()) {
			if (entry.getKey() == null || entry.getKey().length() != 1 || entry.getKey().equals(" ")) {
				return error("cada clave de recipe.key debe ser un solo caracter distinto de espacio: '" + entry.getKey() + "'");
			}
			Optional<Ingredient> ingredient = parseIngredient(entry.getValue());
			if (ingredient.isEmpty()) {
				return error("ingrediente no valido para '" + entry.getKey() + "': '" + entry.getValue() + "'");
			}
			key.put(entry.getKey().charAt(0), ingredient.get());
		}

		ShapedRecipePattern pattern;
		try {
			pattern = ShapedRecipePattern.of(key, config.pattern);
		} catch (IllegalArgumentException | IllegalStateException e) {
			return error("recipe.pattern no valido: " + e.getMessage());
		}
		ItemStack result = new ItemStack(HealingUpgradeMod.HEALING_UPGRADE, config.count);
		return DataResult.success(new ShapedRecipe("", CraftingBookCategory.MISC, pattern, result));
	}

	/** "namespace:item" o "#namespace:tag". */
	private static Optional<Ingredient> parseIngredient(String value) {
		if (value == null || value.isBlank()) {
			return Optional.empty();
		}
		boolean isTag = value.startsWith("#");
		ResourceLocation id = ResourceLocation.tryParse(isTag ? value.substring(1) : value);
		if (id == null) {
			return Optional.empty();
		}
		if (isTag) {
			return Optional.of(Ingredient.of(TagKey.create(Registries.ITEM, id)));
		}
		Optional<Item> item = BuiltInRegistries.ITEM.getOptional(id).filter(i -> i != Items.AIR);
		return item.map(Ingredient::of);
	}

	private static DataResult<ShapedRecipe> error(String message) {
		HealingUpgradeMod.LOGGER.error("Receta de config/healingupgrade.json: {}", message);
		return DataResult.error(() -> "config/healingupgrade.json: " + message);
	}
}
