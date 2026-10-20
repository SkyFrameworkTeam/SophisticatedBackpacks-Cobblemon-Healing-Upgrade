package com.skyframework.healingupgrade.client;

/**
 * Copia en el cliente del cooldown del jugador local (el servidor es quien manda; esto solo sirve para mostrarlo).
 * Se guarda como instante de fin en tiempo real, para que la cuenta atras avance sola entre sincronizaciones.
 */
public final class ClientCooldown {
	private static long endMillis = 0L;

	private ClientCooldown() {
	}

	public static void set(int remainingTicks) {
		endMillis = remainingTicks <= 0 ? 0L : System.currentTimeMillis() + remainingTicks * 50L;
	}

	/** Segundos restantes, redondeando hacia arriba; 0 si ya se puede curar. */
	public static int remainingSeconds() {
		long left = endMillis - System.currentTimeMillis();
		return left <= 0 ? 0 : (int) ((left + 999) / 1000);
	}

	public static void reset() {
		endMillis = 0L;
	}
}
