package com.skyframework.healingupgrade.upgrade;

import com.skyframework.healingupgrade.heal.PartyHealService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;

public class HealingUpgradeContainer extends UpgradeContainerBase<HealingUpgradeWrapper, HealingUpgradeContainer> {
	private static final String DATA_HEAL = "healParty";
	private static final String DATA_REQUEST_COOLDOWN = "requestCooldown";

	public HealingUpgradeContainer(Player player, int upgradeContainerId, HealingUpgradeWrapper upgradeWrapper,
			UpgradeContainerType<HealingUpgradeWrapper, HealingUpgradeContainer> type) {
		super(player, upgradeContainerId, upgradeWrapper, type);
	}

	/** Cliente: pide al servidor que cure. El servidor decide si se puede. */
	public void requestHeal() {
		sendFlagToServer(DATA_HEAL);
	}

	/** Cliente: pide el tiempo de espera restante para mostrarlo en el boton. */
	public void requestCooldown() {
		sendFlagToServer(DATA_REQUEST_COOLDOWN);
	}

	private void sendFlagToServer(String flag) {
		sendDataToServer(() -> {
			CompoundTag tag = new CompoundTag();
			tag.putBoolean(flag, true);
			return tag;
		});
	}

	/**
	 * Servidor: Sophisticated Core solo llega aqui a traves del menu que tiene abierto el jugador que envio el
	 * paquete (SyncContainerClientDataPayload -> player.containerMenu -> upgradeContainers[containerId]).
	 */
	@Override
	public void handlePacket(CompoundTag data) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}
		if (data.contains(DATA_HEAL)) {
			PartyHealService.tryHeal(serverPlayer, this);
			PartyHealService.syncCooldown(serverPlayer);
		} else if (data.contains(DATA_REQUEST_COOLDOWN)) {
			PartyHealService.syncCooldown(serverPlayer);
		}
	}
}
