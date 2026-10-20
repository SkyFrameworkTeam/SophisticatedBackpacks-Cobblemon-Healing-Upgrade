package com.skyframework.healingupgrade;

import com.mojang.brigadier.Command;
import com.skyframework.healingupgrade.config.HealingUpgradeConfig;
import com.skyframework.healingupgrade.heal.PartyHealService;
import com.skyframework.healingupgrade.network.CooldownSyncPayload;
import com.skyframework.healingupgrade.recipe.ConfigShapedRecipeSerializer;
import com.skyframework.healingupgrade.upgrade.HealingUpgradeContainer;
import com.skyframework.healingupgrade.upgrade.HealingUpgradeItem;
import com.skyframework.healingupgrade.upgrade.HealingUpgradeWrapper;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerRegistry;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HealingUpgradeMod implements ModInitializer {
	public static final String MOD_ID = "healingupgrade";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final ResourceLocation HEALING_UPGRADE_ID = id("healing_upgrade");
	/** Pestaña creativa de Sophisticated Backpacks (ModItems.CREATIVE_MODE_TABS.register("main", ...)). */
	private static final ResourceKey<CreativeModeTab> SB_CREATIVE_TAB =
			ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath("sophisticatedbackpacks", "main"));

	public static HealingUpgradeItem HEALING_UPGRADE;
	public static final UpgradeContainerType<HealingUpgradeWrapper, HealingUpgradeContainer> HEALING_TYPE =
			new UpgradeContainerType<>(HealingUpgradeContainer::new);

	@Override
	public void onInitialize() {
		HealingUpgradeConfig.load();

		HEALING_UPGRADE = Registry.register(BuiltInRegistries.ITEM, HEALING_UPGRADE_ID, new HealingUpgradeItem());
		// data/healingupgrade/recipe/healing_upgrade.json usa este tipo; los ingredientes salen de la config.
		Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, id("config_shaped"), ConfigShapedRecipeSerializer.INSTANCE);
		PayloadTypeRegistry.playS2C().register(CooldownSyncPayload.TYPE, CooldownSyncPayload.STREAM_CODEC);
		// Misma API publica que usa Sophisticated Backpacks en ModItems.registerContainers().
		UpgradeContainerRegistry.register(HEALING_UPGRADE_ID, HEALING_TYPE);

		// Junto a las demas mejoras de la mochila (justo despues de la de herreria). Si ese item no esta en la
		// pestana (desactivado en la config de SB), Fabric lo anade al final.
		ItemGroupEvents.modifyEntriesEvent(SB_CREATIVE_TAB).register(entries -> entries.addAfter(
				BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("sophisticatedbackpacks", "smithing_upgrade")),
				HEALING_UPGRADE));

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> PartyHealService.clearCooldowns());

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				Commands.literal(MOD_ID)
						.requires(source -> source.hasPermission(2))
						.then(Commands.literal("reload").executes(ctx -> {
							HealingUpgradeConfig.load();
							ctx.getSource().sendSuccess(() -> Component.translatable("healingupgrade.command.reloaded"), true);
							return Command.SINGLE_SUCCESS;
						}))));
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
