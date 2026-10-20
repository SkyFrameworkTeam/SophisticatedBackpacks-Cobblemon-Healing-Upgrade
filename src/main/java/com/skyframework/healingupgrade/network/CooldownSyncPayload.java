package com.skyframework.healingupgrade.network;

import com.skyframework.healingupgrade.HealingUpgradeMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Servidor -> cliente: ticks que le quedan al jugador hasta poder volver a curar (0 = ya puede). */
public record CooldownSyncPayload(int remainingTicks) implements CustomPacketPayload {
	public static final Type<CooldownSyncPayload> TYPE = new Type<>(HealingUpgradeMod.id("cooldown_sync"));
	public static final StreamCodec<ByteBuf, CooldownSyncPayload> STREAM_CODEC =
			ByteBufCodecs.VAR_INT.map(CooldownSyncPayload::new, CooldownSyncPayload::remainingTicks);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
