package com.HUY.JumpUltimateStars;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;

public class Man_Hinh_Chinh implements Screen {

    private final MainGame game;

    private Texture backgroundTexture;
    private SpriteBatch batch;
    private OrthographicCamera camera;

    public Man_Hinh_Chinh(MainGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        backgroundTexture = new Texture("man_hinh_chinh.jpg");
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0f, 0f, 0f, 1f);
        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        batch.draw(backgroundTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.end();
    }

    @Override
    public void resize(int width, int height) {}
    @Override
    public void pause() {}
    @Override
    public void hide() {}
    @Override
    public void resume() {}
    @Override
    public void dispose() {}
}
