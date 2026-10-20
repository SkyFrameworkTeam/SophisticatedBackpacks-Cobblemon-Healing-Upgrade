package com.skyframework.healingupgrade.client;

import com.skyframework.healingupgrade.upgrade.HealingUpgradeContainer;
import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeSettingsTab;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;

public class HealingUpgradeTab extends UpgradeSettingsTab<HealingUpgradeContainer> {
	public HealingUpgradeTab(HealingUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen) {
		super(upgradeContainer, position, screen,
				Component.translatable("gui.healingupgrade.upgrades.healing"),
				Component.translatable("gui.healingupgrade.upgrades.healing.tooltip"));
		openTabDimension = new Dimension(HealButton.WIDTH + 12, 48);
		addHideableChild(new HealButton(new Position(x + 6, y + 24),
				Component.translatable("gui.healingupgrade.button.heal"),
				Component.translatable("gui.healingupgrade.button.heal.tooltip"),
				button -> getContainer().requestHeal()));
		// Al abrir la mochila se pide el tiempo restante al servidor para pintarlo en el boton.
		upgradeContainer.requestCooldown();
	}

	@Override
	protected void moveSlotsToTab() {
		// Sin ranuras propias.
	}
}
