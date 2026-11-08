package pl.fullbright;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class FlatButton extends ButtonWidget {
    private boolean selected;

    public FlatButton(int x, int y, int w, int h, Text text, PressAction action) {
        super(x, y, w, h, text, action, DEFAULT_NARRATION_SUPPLIER);
    }

    public FlatButton selected(boolean s) { this.selected = s; return this; }

    @Override
    protected void renderWidget(DrawContext c, int mx, int my, float delta) {
        int bg = selected ? 0xFF2F6FD6 : isHovered() ? 0xFF3A4050 : 0xFF262A36;
        c.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bg);
        c.drawBorder(getX(), getY(), getWidth(), getHeight(), selected ? 0xFF7DB3FF : 0xFF3A4050);
        var tr = MinecraftClient.getInstance().textRenderer;
        c.drawCenteredTextWithShadow(tr, getMessage(), getX() + getWidth() / 2,
                getY() + (getHeight() - 8) / 2, 0xFFFFFFFF);
    }
}
