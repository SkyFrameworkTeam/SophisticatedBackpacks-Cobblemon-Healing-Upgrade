package com.skyframework.healingupgrade.client;

import com.skyframework.healingupgrade.HealingUpgradeMod;
import com.skyframework.healingupgrade.util.TimeFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TextureBlitData;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.UV;

import java.util.List;
import java.util.function.IntConsumer;

/**
 * Boton con texto para la pestana. Mientras dura el cooldown muestra la cuenta atras ("Espera 9:42"), se ve
 * desactivado y no envia nada al servidor. Textura propia: assets/healingupgrade/textures/gui/heal_button.png (128x64).
 */
public class HealButton extends ButtonBase {
	public static final int WIDTH = 90;
	public static final int HEIGHT = 16;
	private static final ResourceLocation TEXTURE = HealingUpgradeMod.id("textures/gui/heal_button.png");
	private static final Dimension TEXTURE_SIZE = new Dimension(128, 64);
	private static final TextureBlitData BACKGROUND = new TextureBlitData(TEXTURE, TEXTURE_SIZE, new UV(0, 0), new Dimension(WIDTH, HEIGHT));
	private static final TextureBlitData BACKGROUND_HOVERED = new TextureBlitData(TEXTURE, TEXTURE_SIZE, new UV(0, 16), new Dimension(WIDTH, HEIGHT));
	private static final TextureBlitData BACKGROUND_DISABLED = new TextureBlitData(TEXTURE, TEXTURE_SIZE, new UV(0, 32), new Dimension(WIDTH, HEIGHT));
	private static final TextureBlitData ICON = new TextureBlitData(TEXTURE, TEXTURE_SIZE, new UV(96, 0), new Dimension(9, 9));
	private static final TextureBlitData ICON_DISABLED = new TextureBlitData(TEXTURE, TEXTURE_SIZE, new UV(106, 0), new Dimension(9, 9));

	private final Component label;
	private final List<Component> tooltip;
	private final List<Component> cooldownTooltip = List.of(Component.translatable("gui.healingupgrade.button.cooldown.tooltip"));

	public HealButton(Position position, Component label, Component tooltip, IntConsumer onClick) {
		super(position, new Dimension(WIDTH, HEIGHT), onClick);
		this.label = label;
		this.tooltip = List.of(tooltip);
	}

	private static boolean onCooldown() {
		return ClientCooldown.remainingSeconds() > 0;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (onCooldown()) {
			// Se consume el clic para que no llegue a lo que hay debajo, pero no se pide curar.
			return isMouseOver(mouseX, mouseY);
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, Minecraft minecraft, int mouseX, int mouseY) {
		TextureBlitData background;
		if (onCooldown()) {
			background = BACKGROUND_DISABLED;
		} else {
			background = isMouseOver(mouseX, mouseY) ? BACKGROUND_HOVERED : BACKGROUND;
		}
		GuiHelper.blit(guiGraphics, x, y, background);
	}

	@Override
	protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		int secondsLeft = ClientCooldown.remainingSeconds();
		boolean cooldown = secondsLeft > 0;
		GuiHelper.blit(guiGraphics, x + 4, y + 4, cooldown ? ICON_DISABLED : ICON);
		Component text = cooldown
				? Component.translatable("gui.healingupgrade.button.cooldown", TimeFormat.minutesSeconds(secondsLeft))
				: label;
		int textAreaX = x + 15;
		int textAreaWidth = WIDTH - 17;
		int textX = textAreaX + (textAreaWidth - font.width(text)) / 2;
		guiGraphics.drawString(font, text, textX, y + 4, cooldown ? 0xBBBBBB : 0xFFFFFF, true);
	}

	@Override
	public void renderTooltip(Screen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
		super.renderTooltip(screen, guiGraphics, mouseX, mouseY);
		if (visible && isMouseOver(mouseX, mouseY)) {
			GuiHelper.renderTooltip(screen, guiGraphics, onCooldown() ? cooldownTooltip : tooltip, mouseX, mouseY);
		}
	}
}
