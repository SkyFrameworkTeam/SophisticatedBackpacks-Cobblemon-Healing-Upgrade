package com.skyframework.healingupgrade.upgrade;

import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;

import java.util.function.Consumer;

public class HealingUpgradeWrapper extends UpgradeWrapperBase<HealingUpgradeWrapper, HealingUpgradeItem> {
	protected HealingUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
	}

	public IStorageWrapper getStorageWrapper() {
		return storageWrapper;
	}

	@Override
	public boolean canBeDisabled() {
		return false;
	}
}
