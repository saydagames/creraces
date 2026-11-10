package mc.sayda.creraces.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.function.IntConsumer;

/**
 * A saturation/brightness field with a hue strip under it. Clicking or dragging in either hands the
 * picked color (0xRRGGBB) to the listener on every move; when to save it is up to the owning screen.
 */
@SuppressWarnings("null")
public class ColorPickerWidget extends AbstractWidget {
    private static final int HUE_STRIP_HEIGHT = 6;
    private static final int STRIP_GAP = 3;
    /** Mth.hsvToRgb only handles hues below 1. */
    private static final float MAX_HUE = 0.9999f;

    private final IntConsumer onPick;
    private float hue;
    private float saturation;
    private float brightness;
    /** Set by where a drag starts, so it keeps steering the same part after the cursor leaves it. */
    private boolean draggingHue;

    public ColorPickerWidget(int x, int y, int width, int height, int rgb, Component message, IntConsumer onPick) {
        super(x, y, width, height, message);
        this.onPick = onPick;
        setRgb(rgb);
    }

    public int rgb() {
        return Mth.hsvToRgb(hue, saturation, brightness) & 0xFFFFFF;
    }

    /** Moves the markers to this color without telling the listener. */
    public void setRgb(int rgb) {
        float r = (rgb >> 16 & 0xFF) / 255f;
        float g = (rgb >> 8 & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float delta = max - Math.min(r, Math.min(g, b));
        brightness = max;
        saturation = max > 0 ? delta / max : 0;
        // A grey has no hue of its own, so the strip stays where it was
        if (delta > 0) {
            float sector = max == r ? (g - b) / delta : max == g ? (b - r) / delta + 2 : (r - g) / delta + 4;
            hue = Math.min(Mth.positiveModulo(sector / 6f, 1f), MAX_HUE);
        }
    }

    /** "#RRGGBB" for a color, ignoring any alpha. */
    public static String toHex(int rgb) {
        return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
    }

    /** Reads "#RRGGBB" or "RRGGBB"; null for anything else, including a half-typed value. */
    @Nullable
    public static Integer parseHex(String text) {
        String digits = text.startsWith("#") ? text.substring(1) : text;
        if (digits.length() != 6) {
            return null;
        }
        try {
            return Integer.parseInt(digits, 16);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int fieldBottom() {
        return getY() + height - HUE_STRIP_HEIGHT - STRIP_GAP;
    }

    private int stripTop() {
        return getY() + height - HUE_STRIP_HEIGHT;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int left = getX();
        int top = getY();
        int fieldBottom = fieldBottom();
        int stripTop = stripTop();

        // Saturation runs left to right and brightness top to bottom, drawn a column at a time
        for (int i = 0; i < width; i++) {
            float s = i / (float) (width - 1);
            g.fillGradient(left + i, top, left + i + 1, fieldBottom,
                    0xFF000000 | (Mth.hsvToRgb(hue, s, 1.0f) & 0xFFFFFF), 0xFF000000);
            g.fill(left + i, stripTop, left + i + 1, stripTop + HUE_STRIP_HEIGHT,
                    0xFF000000 | (Mth.hsvToRgb(i / (float) width, 1.0f, 1.0f) & 0xFFFFFF));
        }

        int fieldX = left + Math.round(saturation * (width - 1));
        int fieldY = top + Math.round((1.0f - brightness) * (fieldBottom - top - 1));
        g.renderOutline(fieldX - 3, fieldY - 3, 7, 7, 0xFF000000);
        g.renderOutline(fieldX - 2, fieldY - 2, 5, 5, 0xFFFFFFFF);

        int stripX = left + Math.round(hue * (width - 1));
        g.fill(stripX - 1, stripTop - 1, stripX + 2, stripTop + HUE_STRIP_HEIGHT + 1, 0xFF000000);
        g.fill(stripX, stripTop - 1, stripX + 1, stripTop + HUE_STRIP_HEIGHT + 1, 0xFFFFFFFF);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        // A click in the gap goes to whichever part is nearer
        draggingHue = mouseY >= fieldBottom() + STRIP_GAP / 2.0;
        pickAt(mouseX, mouseY);
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        pickAt(mouseX, mouseY);
    }

    private void pickAt(double mouseX, double mouseY) {
        float alongX = (float) Mth.clamp((mouseX - getX()) / (width - 1), 0.0, 1.0);
        if (draggingHue) {
            hue = Math.min(alongX, MAX_HUE);
        } else {
            saturation = alongX;
            brightness = 1.0f - (float) Mth.clamp((mouseY - getY()) / (fieldBottom() - getY() - 1), 0.0, 1.0);
        }
        onPick.accept(rgb());
    }

    // A color field doesn't click like a button
    @Override
    public void playDownSound(SoundManager soundManager) {
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Component.empty().append(getMessage()).append(" " + toHex(rgb())));
    }
}
