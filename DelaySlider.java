package com.askalki;

import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class DelaySlider extends SliderWidget {
    private static final int MAX_MS = 10000;

    public DelaySlider(int x, int y, int w, int h) {
        super(x, y, w, h, Text.empty(), Settings.delayMs / (double) MAX_MS);
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        setMessage(Text.literal(String.format("Opóźnienie: %.1f s", Settings.delayMs / 1000.0)));
    }

    @Override
    protected void applyValue() {
        Settings.delayMs = (int) Math.round(this.value * MAX_MS);
    }
}
