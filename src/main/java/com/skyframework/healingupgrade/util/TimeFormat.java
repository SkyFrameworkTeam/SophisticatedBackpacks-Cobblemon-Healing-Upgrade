package com.skyframework.healingupgrade.util;

public final class TimeFormat {
	private TimeFormat() {
	}

	/** 754 -> "12:34". */
	public static String minutesSeconds(int totalSeconds) {
		return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
	}
}
