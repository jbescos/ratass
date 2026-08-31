package com.github.jbescos.presentation;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

/** Owns the shared high-resolution texture and independently scaled UI fonts. */
public final class UiFontFamily implements Disposable {
    private static final String FONT_DEFINITION_PATH = "fonts/ui-semibold.fnt";
    private static final String FONT_TEXTURE_PATH = "fonts/ui-semibold.png";
    private static final float LOGICAL_BASE_LINE_HEIGHT = 18f;

    private final Texture texture;
    private final BitmapFont hud;
    private final BitmapFont title;
    private final BitmapFont leaderboard;
    private final BitmapFont label;

    public UiFontFamily(
            float hudScale,
            float titleScale,
            float leaderboardScale,
            float labelScale) {
        texture = new Texture(Gdx.files.internal(FONT_TEXTURE_PATH));
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        TextureRegion region = new TextureRegion(texture);
        hud = createFont(region);
        title = createFont(region);
        leaderboard = createFont(region);
        label = createFont(region);
        applyScale(hud, hudScale);
        applyScale(title, titleScale);
        applyScale(leaderboard, leaderboardScale);
        applyScale(label, labelScale);
    }

    public BitmapFont getHud() {
        return hud;
    }

    public BitmapFont getTitle() {
        return title;
    }

    public BitmapFont getLeaderboard() {
        return leaderboard;
    }

    public BitmapFont getLabel() {
        return label;
    }

    public void setReadabilityScale(
            float readabilityScale,
            float hudScale,
            float titleScale,
            float leaderboardScale,
            float labelScale) {
        float safeScale = Math.max(1f, readabilityScale);
        applyScale(hud, hudScale * safeScale);
        applyScale(title, titleScale * safeScale);
        applyScale(leaderboard, leaderboardScale * safeScale);
        applyScale(label, labelScale * safeScale);
    }

    @Override
    public void dispose() {
        hud.dispose();
        title.dispose();
        leaderboard.dispose();
        label.dispose();
        texture.dispose();
    }

    private static BitmapFont createFont(TextureRegion region) {
        BitmapFont font = new BitmapFont(
                Gdx.files.internal(FONT_DEFINITION_PATH),
                region,
                false);
        font.setUseIntegerPositions(true);
        return font;
    }

    private static void applyScale(BitmapFont font, float styleScale) {
        font.getData().setScale(1f);
        float sourceLineHeight = Math.max(1f, font.getLineHeight());
        font.getData().setScale(
                LOGICAL_BASE_LINE_HEIGHT * styleScale / sourceLineHeight);
    }
}
