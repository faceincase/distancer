package dev.faceincase.distancer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.ContextualBar;
import net.minecraft.world.entity.HumanoidArm;

import java.text.NumberFormat;
import java.util.Locale;

/** Draws the small distance box next to the locator bar. */
final class DistanceBox {
	private static final int PAD_X = 5;
	private static final int PAD_Y = 2;
	private static final int ACCENT = 2;
	/** Gap between the end of the locator bar and the box. */
	private static final int GAP = 5;

	private static final int BACKGROUND = 0xC0101014;
	private static final int BORDER = 0xFF000000;
	private static final int TEXT_SURE = 0xFFFFFFFF;
	private static final int TEXT_UNSURE = 0xFF9A9AA0;

	private static final NumberFormat FORMAT = NumberFormat.getIntegerInstance(Locale.US);

	private DistanceBox() {}

	static void render(GuiGraphicsExtractor graphics, DistancerClient mod) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		DistancerClient.Target target = mod.facedTarget(mc);
		if (target == null) {
			return;
		}

		Font font = mc.font;
		String text = format(target);
		int boxW = ACCENT + PAD_X + font.width(text) + PAD_X;
		int boxH = font.lineHeight + PAD_Y * 2;

		int width = graphics.guiWidth();
		int height = graphics.guiHeight();
		int barTop = height - ContextualBar.MARGIN_BOTTOM - ContextualBar.HEIGHT;
		int barLeft = (width - ContextualBar.WIDTH) / 2;
		int barRight = barLeft + ContextualBar.WIDTH;

		// Sits in the free space beside the bar, on the main-hand side (the offhand slot is on the other side)
		boolean right = mc.player.getMainArm() == HumanoidArm.RIGHT;
		int x = right ? barRight + GAP : barLeft - GAP - boxW;
		x = Math.max(1, Math.min(width - boxW - 1, x));
		int y = barTop + ContextualBar.HEIGHT / 2 - boxH / 2;

		graphics.fill(x - 1, y - 1, x + boxW + 1, y + boxH + 1, BORDER);
		graphics.fill(x, y, x + boxW, y + boxH, BACKGROUND);
		graphics.fill(x, y, x + ACCENT, y + boxH, target.color);
		graphics.text(font, text, x + ACCENT + PAD_X, y + PAD_Y + 1, target.confident ? TEXT_SURE : TEXT_UNSURE, true);
	}

	private static String format(DistancerClient.Target t) {
		if (Double.isNaN(t.distance)) {
			return "?";
		}
		long blocks = Math.round(t.distance);
		if (t.estimated && blocks >= 100) {
			blocks = Math.round(t.distance / 10.0) * 10; // don't pretend to be exact
		}
		return FORMAT.format(blocks);
	}
}
