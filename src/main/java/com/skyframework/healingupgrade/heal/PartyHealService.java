package com.skyframework.healingupgrade.heal;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.CobblemonSounds;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.block.entity.HealingMachineBlockEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.util.PlayerExtensionsKt;
import com.skyframework.healingupgrade.HealingUpgradeMod;
import com.skyframework.healingupgrade.config.HealingUpgradeConfig;
import com.skyframework.healingupgrade.network.CooldownSyncPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.skyframework.healingupgrade.upgrade.HealingUpgradeContainer;
import com.skyframework.healingupgrade.util.TimeFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Logica de curacion. Solo se ejecuta en el servidor, desde {@link HealingUpgradeContainer#handlePacket}. */
public final class PartyHealService {
	/** UUID del jugador -> tick del servidor a partir del cual puede volver a curar. En memoria: se reinicia al reiniciar el servidor. */
	private static final Map<UUID, Integer> COOLDOWN_UNTIL = new ConcurrentHashMap<>();

	private PartyHealService() {
	}

	public static void clearCooldowns() {
		COOLDOWN_UNTIL.clear();
	}

	public static int remainingTicks(ServerPlayer player) {
		Integer until = COOLDOWN_UNTIL.get(player.getUUID());
		return until == null ? 0 : Math.max(0, until - player.server.getTickCount());
	}

	/** Envia al cliente el tiempo de espera restante para que el boton lo muestre. */
	public static void syncCooldown(ServerPlayer player) {
		if (ServerPlayNetworking.canSend(player, CooldownSyncPayload.TYPE)) {
			ServerPlayNetworking.send(player, new CooldownSyncPayload(remainingTicks(player)));
		}
	}

	public static void tryHeal(ServerPlayer player, HealingUpgradeContainer container) {
		HealingUpgradeConfig config = HealingUpgradeConfig.get();

		if (!isOwnerOfOpenBackpack(player, container, config)) {
			fail(player, "not_owner");
			return;
		}
		if (!config.allowInBattle && PlayerExtensionsKt.isInBattle(player)) {
			fail(player, "in_battle");
			return;
		}
		if (HealingMachineBlockEntity.Companion.isUsingHealer(player)) {
			fail(player, "machine_in_use");
			return;
		}

		PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
		if (party.occupied() == 0) {
			fail(player, "no_pokemon");
			return;
		}
		if (!needsHealing(party)) {
			fail(player, "already_healed");
			return;
		}

		int now = player.server.getTickCount();
		int ticksLeft = remainingTicks(player);
		if (ticksLeft > 0) {
			fail(player, "cooldown", TimeFormat.minutesSeconds((ticksLeft + 19) / 20));
			return;
		}

		InventoryHandler inventory = container.getUpgradeWrapper().getStorageWrapper().getInventoryHandler();
		Item costItem = null;
		if (config.cost.enabled) {
			Optional<Item> resolved = resolveCostItem(config.cost.item);
			if (resolved.isEmpty()) {
				HealingUpgradeMod.LOGGER.warn("cost.item '{}' no es un item valido en config/healingupgrade.json", config.cost.item);
				fail(player, "bad_config");
				return;
			}
			costItem = resolved.get();
			int available = InventoryHelper.extractFromInventory(costItem, config.cost.count, inventory, true).getCount();
			if (available < config.cost.count) {
				fail(player, "missing_cost", config.cost.count, costItem.getDescription());
				return;
			}
		}

		// Todas las comprobaciones pasadas: a partir de aqui se cobra y se cura.
		if (costItem != null) {
			InventoryHelper.extractFromInventory(costItem, config.cost.count, inventory, false);
		}
		party.heal();
		if (config.cooldownSeconds > 0) {
			COOLDOWN_UNTIL.put(player.getUUID(), now + config.cooldownSeconds * 20);
		}
		if (config.playEffects) {
			playEffects(player);
		}
		player.sendSystemMessage(Component.translatable("healingupgrade.message.healed").withStyle(ChatFormatting.GREEN));
	}

	/**
	 * El jugador debe tener abierta una mochila suya (no la de otro jugador) y este contenedor de mejora debe
	 * pertenecer a ese mismo menu.
	 */
	private static boolean isOwnerOfOpenBackpack(ServerPlayer player, HealingUpgradeContainer container, HealingUpgradeConfig config) {
		if (!(player.containerMenu instanceof BackpackContainer backpackMenu)) {
			return false;
		}
		if (!backpackMenu.getUpgradeContainers().containsValue(container)) {
			return false;
		}
		return switch (backpackMenu.getBackpackContext().getType()) {
			case ITEM_BACKPACK, ITEM_SUB_BACKPACK -> true;
			case BLOCK_BACKPACK, BLOCK_SUB_BACKPACK -> config.allowPlacedBackpacks;
			case ANOTHER_PLAYER_BACKPACK, ANOTHER_PLAYER_SUB_BACKPACK -> false;
		};
	}

	private static boolean needsHealing(PlayerPartyStore party) {
		for (Pokemon pokemon : party) {
			if (pokemon.canBeHealed()) {
				return true;
			}
		}
		return false;
	}

	private static Optional<Item> resolveCostItem(String id) {
		ResourceLocation location = ResourceLocation.tryParse(id);
		if (location == null) {
			return Optional.empty();
		}
		return BuiltInRegistries.ITEM.getOptional(location).filter(item -> item != Items.AIR);
	}

	private static void playEffects(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				CobblemonSounds.HEALING_MACHINE_ACTIVE, SoundSource.PLAYERS, 1.0F, 1.0F);
		level.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.2, player.getZ(), 6, 0.5, 0.4, 0.5, 0.05);
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 12, 0.6, 0.6, 0.6, 0.1);
	}

	private static void fail(ServerPlayer player, String reason, Object... args) {
		player.sendSystemMessage(Component.translatable("healingupgrade.message." + reason, args).withStyle(ChatFormatting.RED));
	}
}
