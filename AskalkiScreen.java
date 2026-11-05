package com.askalki;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.function.Supplier;

public class AskalkiScreen extends Screen {

    public AskalkiScreen() {
        super(Text.literal("Askalki"));
    }

    @Override
    protected void init() {
        int cx = width / 2 - 100;
        int y = height / 2 - 80;

        addDrawableChild(toggle(cx, y, "Wszystko",
                () -> Settings.masterEnabled, v -> Settings.masterEnabled = v));

        addDrawableChild(toggle(cx, y + 30, "Szybkie podnoszenie",
                () -> Settings.fastPickup, v -> Settings.fastPickup = v));

        addDrawableChild(new DelaySlider(cx, y + 60, 200, 20));

        addDrawableChild(toggle(cx, y + 90, "Złap od razu po wylądowaniu",
                () -> Settings.instantOnLand, v -> Settings.instantOnLand = v));

        addDrawableChild(toggle(cx, y + 120, "Biegnij do itemu (sprint)",
                () -> Settings.sprintToItem, v -> Settings.sprintToItem = v));
    }

    private ButtonWidget toggle(int x, int y, String name,
                                Supplier<Boolean> getter, java.util.function.Consumer<Boolean> setter) {
        return ButtonWidget.builder(label(name, getter.get()), button -> {
                    boolean next = !getter.get();
                    setter.accept(next);
                    button.setMessage(label(name, next));
                })
                .dimensions(x, y, 200, 20)
                .build();
    }

    private static Text label(String name, boolean on) {
        return Text.literal(name + ": " + (on ? "ON" : "OFF"));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Askalki"), width / 2, height / 2 - 100, 0xFFFFFF);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
