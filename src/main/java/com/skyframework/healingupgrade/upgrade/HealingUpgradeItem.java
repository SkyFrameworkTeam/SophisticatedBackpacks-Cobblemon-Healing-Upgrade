package com.skyframework.healingupgrade.upgrade;

import net.minecraft.resources.ResourceLocation;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeCountLimitConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeGroup;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HealingUpgradeItem extends UpgradeItemBase<HealingUpgradeWrapper> {
	private static final UpgradeType<HealingUpgradeWrapper> TYPE = new UpgradeType<>(HealingUpgradeWrapper::new);

	/** Una por mochila y ninguna en otros almacenes de Sophisticated (cofres de Sophisticated Storage, etc.). */
	private static final IUpgradeCountLimitConfig LIMITS = new IUpgradeCountLimitConfig() {
		@Override
		public int getMaxUpgradesPerStorage(String storageType, @Nullable ResourceLocation upgradeRegistryName) {
			return "backpack".equals(storageType) ? 1 : 0;
		}

		@Override
		public int getMaxUpgradesInGroupPerStorage(String storageType, UpgradeGroup upgradeGroup) {
			return Integer.MAX_VALUE;
		}
	};

	public HealingUpgradeItem() {
		super(LIMITS);
	}

	@Override
	public UpgradeType<HealingUpgradeWrapper> getType() {
		return TYPE;
	}

	@Override
	public List<UpgradeConflictDefinition> getUpgradeConflicts() {
		return List.of();
	}
}
