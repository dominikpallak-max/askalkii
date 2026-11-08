package pl.fullbright;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Pomocnik do przycisków (zwykłe ButtonWidget z buildera - działa na 1.21.11). */
public class FlatButton {
    private static Text label(String s, boolean selected) {
        MutableText t = Text.literal(s);
        return selected ? t.formatted(Formatting.AQUA, Formatting.BOLD) : t;
    }

    public static ButtonWidget btn(int x, int y, int w, int h, String text, boolean selected,
                                   ButtonWidget.PressAction action) {
        return ButtonWidget.builder(label(text, selected), action).dimensions(x, y, w, h).build();
    }

    public static void mark(ButtonWidget b, String text, boolean selected) {
        b.setMessage(label(text, selected));
    }

    public static void border(DrawContext c, int x, int y, int w, int h, int color) {
        c.fill(x, y, x + w, y + 1, color);
        c.fill(x, y + h - 1, x + w, y + h, color);
        c.fill(x, y, x + 1, y + h, color);
        c.fill(x + w - 1, y, x + w, y + h, color);
    }
}
