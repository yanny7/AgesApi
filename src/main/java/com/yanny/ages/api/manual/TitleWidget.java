package com.yanny.ages.api.manual;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.RenderHelper;

import static com.yanny.ages.api.manual.ConfigHolder.*;

public class TitleWidget extends ConfigurableWidget {
    public static final String TYPE = "title";

    protected final String text;
    protected final int color;
    protected final float scale;
    protected final int tmpWidth;
    protected final int tmpHeight;
    protected final int margin_top;
    protected final int margin_left;
    protected final int margin_bottom;
    protected final int margin_right;
    protected final Align align;

    public TitleWidget(JsonObject object, IManual manual) {
        super(object, manual, TEXT, SCALE, COLOR, WIDTH, HEIGHT, MARGIN_TOP, MARGIN_LEFT, MARGIN_BOTTOM, MARGIN_RIGHT, ALIGN_CENTER);

        text = configHolder.getValue(TEXT);
        color = configHolder.getValue(COLOR);
        scale = configHolder.getValue(SCALE);
        tmpWidth = configHolder.getValue(WIDTH);
        tmpHeight = configHolder.getValue(HEIGHT);
        margin_top = configHolder.getValue(MARGIN_TOP);
        margin_left = configHolder.getValue(MARGIN_LEFT);
        margin_bottom = configHolder.getValue(MARGIN_BOTTOM);
        margin_right = configHolder.getValue(MARGIN_RIGHT);
        align = configHolder.getValue(ALIGN_CENTER);
    }

    @Override
    public int getMinWidth(int height) {
        return Math.max(tmpWidth, DYNAMIC);
    }

    @Override
    public int getMinHeight(int width) {
        return Math.max(tmpHeight, Math.round(mc.fontRenderer.FONT_HEIGHT * scale) + margin_top + margin_bottom);
    }

    @Override
    public void drawBackgroundLayer(Screen screen, int mx, int my) {
        RenderSystem.pushMatrix();
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.translatef(getX() + margin_left, getY() + margin_top, 0.0f);
        RenderSystem.scalef(scale, scale, scale);
        RenderHelper.setupGuiFlatDiffuseLighting();
        switch (align) {
            case LEFT:
                screen.drawString(mc.fontRenderer, text, 0, 0, color);
                break;
            case RIGHT:
                screen.drawRightAlignedString(mc.fontRenderer, text, Math.round((getWidth() - margin_left - margin_right) / scale), 0, color);
                break;
            case CENTER:
                screen.drawCenteredString(mc.fontRenderer, text, Math.round(((getWidth() - margin_left - margin_right) / scale) / 2f), 0, color);
                break;
        }
        RenderSystem.popMatrix();
    }
}
