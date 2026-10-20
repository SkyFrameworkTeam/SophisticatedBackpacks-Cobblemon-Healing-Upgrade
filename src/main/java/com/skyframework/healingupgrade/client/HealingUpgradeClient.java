package com.skyframework.healingupgrade.client;

import com.skyframework.healingupgrade.HealingUpgradeMod;
import com.skyframework.healingupgrade.network.CooldownSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeGuiManager;

public class HealingUpgradeClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Misma API que usa Sophisticated Backpacks en ModItemsClient.registerScreens().
		UpgradeGuiManager.registerTab(HealingUpgradeMod.HEALING_TYPE, HealingUpgradeTab::new);

		ClientPlayNetworking.registerGlobalReceiver(CooldownSyncPayload.TYPE,
				(payload, context) -> ClientCooldown.set(payload.remainingTicks()));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientCooldown.reset());
	}
}
