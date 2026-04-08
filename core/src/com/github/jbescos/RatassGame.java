package com.github.jbescos;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Box2D;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.github.jbescos.ai.AiControlDecision;
import com.github.jbescos.ai.AiDrivingPersonality;
import com.github.jbescos.ai.AiDrivingPersonalities;
import com.github.jbescos.ai.AiVehicleView;
import com.github.jbescos.ai.CarAiController;
import com.github.jbescos.gameplay.ArenaMap;
import com.github.jbescos.gameplay.ArenaMaps;
import com.github.jbescos.gameplay.MapProgression;
import com.github.jbescos.gameplay.SpawnPoint;

public class RatassGame extends ApplicationAdapter {
    private static final float WORLD_WIDTH = 32f;
    private static final float WORLD_HEIGHT = 18f;
    private static final float EDGE_FALLOFF_MARGIN = 0.35f;
    private static final float PHYSICS_STEP = 1f / 60f;
    private static final int VELOCITY_ITERATIONS = 6;
    private static final int POSITION_ITERATIONS = 2;
    private static final float ARENA_EDGE_INSET = 0.7f;
    private static final float ARENA_CENTER_INSET = 1.15f;
    private static final float AUTO_ADVANCE_DELAY = 1.8f;

    private static final Color SKYLINE = new Color(0.05f, 0.07f, 0.09f, 1f);
    private static final Color VOID = new Color(0.08f, 0.10f, 0.13f, 1f);
    private static final Color PLATFORM_SHADOW = new Color(0.04f, 0.05f, 0.07f, 1f);
    private static final Color WARNING_BRIGHT = new Color(0.95f, 0.78f, 0.18f, 1f);
    private static final Color WARNING_DARK = new Color(0.14f, 0.14f, 0.15f, 1f);

    private static final MapTheme[] MAP_THEMES = new MapTheme[] {
            new MapTheme(
                    new Color(0.38f, 0.34f, 0.24f, 1f),
                    new Color(0.78f, 0.72f, 0.54f, 1f),
                    new Color(0.69f, 0.63f, 0.46f, 1f)),
            new MapTheme(
                    new Color(0.23f, 0.36f, 0.44f, 1f),
                    new Color(0.48f, 0.70f, 0.78f, 1f),
                    new Color(0.31f, 0.53f, 0.60f, 1f)),
            new MapTheme(
                    new Color(0.43f, 0.25f, 0.20f, 1f),
                    new Color(0.82f, 0.58f, 0.40f, 1f),
                    new Color(0.68f, 0.41f, 0.28f, 1f)),
            new MapTheme(
                    new Color(0.25f, 0.33f, 0.22f, 1f),
                    new Color(0.56f, 0.74f, 0.46f, 1f),
                    new Color(0.38f, 0.58f, 0.31f, 1f))
    };

    private final Array<Car> cars = new Array<Car>();
    private final Array<CarTemplate> roster = new Array<CarTemplate>();
    private final GlyphLayout glyphLayout = new GlyphLayout();
    private final Color tint = new Color();
    private final Rectangle mapBounds = new Rectangle();
    private final Vector2 focusPoint = new Vector2();
    private final Rectangle steerPadBounds = new Rectangle();
    private final Rectangle throttlePadBounds = new Rectangle();
    private final Rectangle reversePadBounds = new Rectangle();
    private final Rectangle restartButtonBounds = new Rectangle();
    private final Vector3 hudTouchPoint = new Vector3();

    private OrthographicCamera worldCamera;
    private FitViewport worldViewport;
    private OrthographicCamera hudCamera;
    private ScreenViewport hudViewport;
    private ShapeRenderer shapeRenderer;
    private SpriteBatch spriteBatch;
    private BitmapFont hudFont;
    private BitmapFont titleFont;
    private World world;

    private MapProgression mapProgression;
    private ArenaMap currentMap;
    private float accumulator;
    private float roundOverTimer;
    private float frameThrottleInput;
    private float frameTurnInput;
    private float touchThrottleInput;
    private float touchTurnInput;
    private boolean touchRestartPressed;
    private boolean touchRestartJustPressed;
    private boolean touchControlsEnabled;
    private boolean roundOver;
    private int roundNumber;
    private int playerWins;
    private Car playerCar;
    private Car winner;

    @Override
    public void create() {
        Box2D.init();

        worldCamera = new OrthographicCamera();
        worldViewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, worldCamera);

        hudCamera = new OrthographicCamera();
        hudViewport = new ScreenViewport(hudCamera);

        shapeRenderer = new ShapeRenderer();
        spriteBatch = new SpriteBatch();

        hudFont = new BitmapFont();
        hudFont.setUseIntegerPositions(false);

        titleFont = new BitmapFont();
        titleFont.setUseIntegerPositions(false);
        titleFont.getData().setScale(1.9f);

        Gdx.input.setCatchKey(Input.Keys.BACK, true);
        createRoster();
        mapProgression = new MapProgression(ArenaMaps.createDefaultSet());

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        resetRound(false);
    }

    private void createRoster() {
        roster.clear();
        roster.add(new CarTemplate("You", true, new Color(0.90f, 0.25f, 0.20f, 1f), null));
        roster.add(new CarTemplate(
                "Cinder",
                false,
                new Color(0.92f, 0.54f, 0.13f, 1f),
                AiDrivingPersonalities.BRAWLER));
        roster.add(new CarTemplate(
                "Frost",
                false,
                new Color(0.16f, 0.57f, 0.84f, 1f),
                AiDrivingPersonalities.INTERCEPTOR));
        roster.add(new CarTemplate(
                "Moss",
                false,
                new Color(0.24f, 0.70f, 0.42f, 1f),
                AiDrivingPersonalities.SURVIVOR));
    }

    @Override
    public void resize(int width, int height) {
        worldViewport.update(width, height);
        worldCamera.position.set(0f, 0f, 0f);
        worldCamera.update();

        hudViewport.update(width, height, true);
    }

    @Override
    public void render() {
        float delta = Math.min(Gdx.graphics.getDeltaTime(), 1f / 30f);

        updateTouchState();
        frameThrottleInput = readPlayerThrottle();
        frameTurnInput = readPlayerTurn();

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
                || Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
            Gdx.app.exit();
            return;
        }

        if (isRestartRequested()) {
            resetRound(shouldAdvanceMap());
        }

        update(delta);
        renderWorld();
        renderHud();
    }

    private void update(float delta) {
        if (roundOver) {
            roundOverTimer += delta;
            if (winner != null && winner.playerControlled && roundOverTimer >= AUTO_ADVANCE_DELAY) {
                resetRound(true);
                return;
            }
        }

        accumulator += delta;
        while (accumulator >= PHYSICS_STEP) {
            stepSimulation(PHYSICS_STEP);
            accumulator -= PHYSICS_STEP;
            if (roundOver) {
                accumulator = 0f;
            }
        }
    }

    private void stepSimulation(float delta) {
        for (int i = 0; i < cars.size; i++) {
            Car car = cars.get(i);
            car.step(delta, currentMap, cars, !roundOver, frameThrottleInput, frameTurnInput);
        }

        world.step(delta, VELOCITY_ITERATIONS, POSITION_ITERATIONS);
        checkForEliminations();

        if (!roundOver) {
            updateRoundState();
        }
    }

    private void checkForEliminations() {
        for (int i = 0; i < cars.size; i++) {
            Car car = cars.get(i);
            if (!car.active || car.body == null) {
                continue;
            }

            Vector2 position = car.body.getPosition();
            if (!currentMap.supports(position)
                    && currentMap.distanceToSafety(position) > EDGE_FALLOFF_MARGIN) {
                car.eliminate(world);
            }
        }
    }

    private void updateRoundState() {
        int aliveCars = 0;
        Car lastAlive = null;

        for (int i = 0; i < cars.size; i++) {
            Car car = cars.get(i);
            if (car.active) {
                aliveCars++;
                lastAlive = car;
            }
        }

        if (aliveCars > 1) {
            return;
        }

        roundOver = true;
        roundOverTimer = 0f;
        winner = lastAlive;

        for (int i = 0; i < cars.size; i++) {
            Car car = cars.get(i);
            if (car.body != null) {
                car.body.setLinearVelocity(0f, 0f);
                car.body.setAngularVelocity(0f);
            }
        }
    }

    private void resetRound(boolean advanceMap) {
        if (advanceMap) {
            playerWins++;
            mapProgression.advanceIfPlayerWon(true);
        }

        currentMap = mapProgression.getCurrentMap();

        if (world != null) {
            world.dispose();
        }

        world = new World(new Vector2(0f, 0f), true);
        cars.clear();
        accumulator = 0f;
        roundOver = false;
        roundOverTimer = 0f;
        winner = null;
        playerCar = null;
        roundNumber++;

        if (currentMap.getSpawnCount() < roster.size) {
            throw new IllegalStateException("Current map does not have enough spawn points.");
        }

        for (int i = 0; i < roster.size; i++) {
            CarTemplate template = roster.get(i);
            SpawnPoint spawnPoint = currentMap.getSpawn(i);
            Car car = createCar(template, spawnPoint);
            if (template.playerControlled) {
                playerCar = car;
            }
        }
    }

    private boolean shouldAdvanceMap() {
        return roundOver && winner != null && winner.playerControlled;
    }

    private boolean isRestartRequested() {
        return Gdx.input.isKeyJustPressed(Input.Keys.R)
                || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                || touchRestartJustPressed;
    }

    private float readPlayerThrottle() {
        float throttle = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
            throttle += 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            throttle -= 1f;
        }

        if (Math.abs(touchThrottleInput) > Math.abs(throttle)) {
            throttle = touchThrottleInput;
        }

        return MathUtils.clamp(throttle, -1f, 1f);
    }

    private float readPlayerTurn() {
        float turn = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            turn += 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            turn -= 1f;
        }

        if (Math.abs(touchTurnInput) > Math.abs(turn)) {
            turn = touchTurnInput;
        }

        return MathUtils.clamp(turn, -1f, 1f);
    }

    private void updateTouchState() {
        touchControlsEnabled = shouldEnableTouchControls();
        if (!touchControlsEnabled) {
            touchThrottleInput = 0f;
            touchTurnInput = 0f;
            touchRestartJustPressed = false;
            touchRestartPressed = false;
            return;
        }

        updateTouchBounds(hudViewport.getWorldWidth(), hudViewport.getWorldHeight());

        float nextThrottle = 0f;
        float nextTurn = 0f;
        boolean restartPressed = false;
        int maxPointers = Math.min(5, Gdx.input.getMaxPointers());

        for (int pointer = 0; pointer < maxPointers; pointer++) {
            if (!Gdx.input.isTouched(pointer)) {
                continue;
            }

            hudTouchPoint.set(Gdx.input.getX(pointer), Gdx.input.getY(pointer), 0f);
            hudViewport.unproject(hudTouchPoint);

            if (steerPadBounds.contains(hudTouchPoint.x, hudTouchPoint.y)) {
                float centerX = steerPadBounds.x + steerPadBounds.width * 0.5f;
                float normalized =
                        (centerX - hudTouchPoint.x) / (steerPadBounds.width * 0.5f);
                nextTurn = MathUtils.clamp(normalized, -1f, 1f);
            } else if (throttlePadBounds.contains(hudTouchPoint.x, hudTouchPoint.y)) {
                nextThrottle = 1f;
            } else if (reversePadBounds.contains(hudTouchPoint.x, hudTouchPoint.y)) {
                nextThrottle = -1f;
            } else if (restartButtonBounds.contains(hudTouchPoint.x, hudTouchPoint.y)) {
                restartPressed = true;
            }
        }

        touchRestartJustPressed = restartPressed && !touchRestartPressed;
        touchRestartPressed = restartPressed;
        touchThrottleInput = nextThrottle;
        touchTurnInput = nextTurn;
    }

    private boolean shouldEnableTouchControls() {
        ApplicationType appType = Gdx.app.getType();
        return appType == ApplicationType.Android
                || appType == ApplicationType.iOS
                || (!Gdx.input.isPeripheralAvailable(Input.Peripheral.HardwareKeyboard)
                && appType != ApplicationType.Desktop);
    }

    private void updateTouchBounds(float hudWidth, float hudHeight) {
        float padSize = Math.min(180f, hudHeight * 0.26f);
        steerPadBounds.set(18f, 18f, padSize, padSize);

        float buttonWidth = padSize * 0.82f;
        throttlePadBounds.set(hudWidth - buttonWidth - 18f, 18f + padSize * 0.52f, buttonWidth, padSize * 0.42f);
        reversePadBounds.set(hudWidth - buttonWidth - 18f, 18f, buttonWidth, padSize * 0.32f);
        restartButtonBounds.set(hudWidth - 154f, hudHeight - 68f, 132f, 38f);
    }

    private Car createCar(CarTemplate template, SpawnPoint spawnPoint) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(spawnPoint.x, spawnPoint.y);
        bodyDef.angle = spawnPoint.angleRad;
        bodyDef.linearDamping = 0.25f;
        bodyDef.angularDamping = 1.8f;
        bodyDef.bullet = true;

        Body body = world.createBody(bodyDef);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(Car.HALF_WIDTH, Car.HALF_HEIGHT);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = 1.3f;
        fixtureDef.friction = 0.55f;
        fixtureDef.restitution = 0.08f;
        body.createFixture(fixtureDef);
        shape.dispose();

        Car car = new Car(body, template.name, template.playerControlled, template.color, template.personality);
        body.setUserData(car);
        cars.add(car);
        return car;
    }

    private void renderWorld() {
        ScreenUtils.clear(SKYLINE.r, SKYLINE.g, SKYLINE.b, 1f);

        worldViewport.apply();
        shapeRenderer.setProjectionMatrix(worldCamera.combined);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        drawBackdrop();
        drawArena();
        drawCars();
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawBackdrop() {
        shapeRenderer.setColor(VOID);
        shapeRenderer.rect(-WORLD_WIDTH, -WORLD_HEIGHT, WORLD_WIDTH * 2f, WORLD_HEIGHT * 2f);

        shapeRenderer.setColor(0.11f, 0.14f, 0.17f, 1f);
        for (float x = -WORLD_WIDTH; x <= WORLD_WIDTH; x += 2.8f) {
            shapeRenderer.rect(x, -WORLD_HEIGHT, 0.15f, WORLD_HEIGHT * 2f);
        }
    }

    private void drawArena() {
        MapTheme theme = currentTheme();
        currentMap.getBounds(mapBounds);

        shapeRenderer.setColor(PLATFORM_SHADOW);
        for (int i = 0; i < currentMap.getSolidZoneCount(); i++) {
            currentMap.getSolidZone(i).draw(shapeRenderer, 0.18f, -0.22f, 0f);
        }

        shapeRenderer.setColor(theme.edge);
        for (int i = 0; i < currentMap.getSolidZoneCount(); i++) {
            currentMap.getSolidZone(i).draw(shapeRenderer, 0f, 0f, ARENA_EDGE_INSET);
        }

        drawWarningStripes();

        shapeRenderer.setColor(theme.surface);
        for (int i = 0; i < currentMap.getSolidZoneCount(); i++) {
            currentMap.getSolidZone(i).draw(shapeRenderer, 0f, 0f, 0f);
        }

        shapeRenderer.setColor(theme.center);
        for (int i = 0; i < currentMap.getSolidZoneCount(); i++) {
            currentMap.getSolidZone(i).draw(shapeRenderer, 0f, 0f, -ARENA_CENTER_INSET);
        }

        if (currentMap.getHoleZoneCount() > 0) {
            shapeRenderer.setColor(theme.edge);
            for (int i = 0; i < currentMap.getHoleZoneCount(); i++) {
                currentMap.getHoleZone(i).draw(shapeRenderer, 0f, 0f, 0.48f);
            }

            shapeRenderer.setColor(VOID);
            for (int i = 0; i < currentMap.getHoleZoneCount(); i++) {
                currentMap.getHoleZone(i).draw(shapeRenderer, 0f, 0f, 0f);
            }
        } else {
            currentMap.getFocusPoint(focusPoint);
            shapeRenderer.setColor(theme.edge);
            shapeRenderer.circle(focusPoint.x, focusPoint.y, 1.65f, 28);

            shapeRenderer.setColor(theme.center);
            shapeRenderer.circle(focusPoint.x, focusPoint.y, 0.82f, 20);
        }
    }

    private void drawWarningStripes() {
        float stripeDepth = 0.55f;
        float stripeLength = 1.25f;
        boolean bright = true;

        for (float x = mapBounds.x - ARENA_EDGE_INSET;
                x < mapBounds.x + mapBounds.width + ARENA_EDGE_INSET;
                x += stripeLength) {
            shapeRenderer.setColor(bright ? WARNING_BRIGHT : WARNING_DARK);
            shapeRenderer.rect(x, mapBounds.y + mapBounds.height, stripeLength, stripeDepth);
            shapeRenderer.rect(x, mapBounds.y - stripeDepth, stripeLength, stripeDepth);
            bright = !bright;
        }

        bright = false;
        for (float y = mapBounds.y - ARENA_EDGE_INSET;
                y < mapBounds.y + mapBounds.height + ARENA_EDGE_INSET;
                y += stripeLength) {
            shapeRenderer.setColor(bright ? WARNING_BRIGHT : WARNING_DARK);
            shapeRenderer.rect(mapBounds.x - stripeDepth, y, stripeDepth, stripeLength);
            shapeRenderer.rect(mapBounds.x + mapBounds.width, y, stripeDepth, stripeLength);
            bright = !bright;
        }
    }

    private void drawCars() {
        for (int i = 0; i < cars.size; i++) {
            Car car = cars.get(i);
            if (!car.active || car.body == null) {
                continue;
            }

            float angleDeg = car.body.getAngle() * MathUtils.radiansToDegrees;
            float centerX = car.body.getPosition().x;
            float centerY = car.body.getPosition().y;

            drawRotatedRect(
                    centerX + 0.10f,
                    centerY - 0.12f,
                    Car.WIDTH,
                    Car.HEIGHT,
                    angleDeg,
                    0.04f,
                    0.05f,
                    0.07f,
                    0.32f);

            if (car.playerControlled) {
                drawRotatedRect(
                        centerX,
                        centerY,
                        Car.WIDTH + 0.20f,
                        Car.HEIGHT + 0.20f,
                        angleDeg,
                        0.96f,
                        0.94f,
                        0.88f,
                        0.26f);
            }

            drawRotatedRect(
                    centerX,
                    centerY,
                    Car.WIDTH,
                    Car.HEIGHT,
                    angleDeg,
                    car.color.r,
                    car.color.g,
                    car.color.b,
                    1f);

            tint.set(car.color).lerp(Color.WHITE, 0.18f);
            drawOffsetRotatedRect(
                    centerX,
                    centerY,
                    0f,
                    -0.08f,
                    Car.WIDTH * 0.68f,
                    Car.HEIGHT * 0.50f,
                    angleDeg,
                    tint.r,
                    tint.g,
                    tint.b,
                    1f);

            drawOffsetRotatedRect(
                    centerX,
                    centerY,
                    0f,
                    0.40f,
                    Car.WIDTH * 0.62f,
                    Car.HEIGHT * 0.16f,
                    angleDeg,
                    0.12f,
                    0.13f,
                    0.15f,
                    1f);

            drawOffsetRotatedRect(
                    centerX,
                    centerY,
                    0f,
                    0.84f,
                    Car.WIDTH * 0.55f,
                    Car.HEIGHT * 0.09f,
                    angleDeg,
                    0.96f,
                    0.92f,
                    0.74f,
                    1f);
        }
    }

    private void drawOffsetRotatedRect(
            float centerX,
            float centerY,
            float offsetX,
            float offsetY,
            float width,
            float height,
            float angleDeg,
            float r,
            float g,
            float b,
            float a) {
        float cos = MathUtils.cosDeg(angleDeg);
        float sin = MathUtils.sinDeg(angleDeg);
        float rotatedX = centerX + offsetX * cos - offsetY * sin;
        float rotatedY = centerY + offsetX * sin + offsetY * cos;
        drawRotatedRect(rotatedX, rotatedY, width, height, angleDeg, r, g, b, a);
    }

    private void drawRotatedRect(
            float centerX,
            float centerY,
            float width,
            float height,
            float angleDeg,
            float r,
            float g,
            float b,
            float a) {
        shapeRenderer.setColor(r, g, b, a);
        shapeRenderer.rect(
                centerX - width * 0.5f,
                centerY - height * 0.5f,
                width * 0.5f,
                height * 0.5f,
                width,
                height,
                1f,
                1f,
                angleDeg);
    }

    private void renderHud() {
        hudViewport.apply();
        spriteBatch.setProjectionMatrix(hudCamera.combined);

        float hudWidth = hudViewport.getWorldWidth();
        float hudHeight = hudViewport.getWorldHeight();
        float statusRightInset = touchControlsEnabled ? 166f : 22f;

        spriteBatch.begin();

        hudFont.setColor(0.96f, 0.92f, 0.82f, 1f);
        hudFont.draw(spriteBatch, "RATASS  |  Roof Sumo", 22f, hudHeight - 20f);

        hudFont.setColor(0.82f, 0.88f, 0.93f, 1f);
        hudFont.draw(
                spriteBatch,
                "Map " + mapProgression.getCurrentMapNumber() + ": " + currentMap.getName(),
                22f,
                hudHeight - 44f);

        hudFont.setColor(0.74f, 0.82f, 0.88f, 1f);
        hudFont.draw(spriteBatch, buildControlsText(), 22f, hudHeight - 68f);

        String status = buildStatusText();
        glyphLayout.setText(hudFont, status);
        hudFont.setColor(0.96f, 0.92f, 0.82f, 1f);
        hudFont.draw(
                spriteBatch,
                status,
                hudWidth - glyphLayout.width - statusRightInset,
                hudHeight - 20f);

        hudFont.setColor(0.93f, 0.84f, 0.49f, 1f);
        hudFont.draw(
                spriteBatch,
                buildObjectiveText(),
                22f,
                44f);

        hudFont.setColor(0.84f, 0.90f, 0.94f, 1f);
        hudFont.draw(spriteBatch, buildRivalText(), 22f, 22f);

        if (roundOver || (playerCar != null && !playerCar.active)) {
            String headline = buildHeadline();
            String subline = buildSubline();

            titleFont.setColor(0.97f, 0.94f, 0.85f, 1f);
            glyphLayout.setText(titleFont, headline);
            titleFont.draw(
                    spriteBatch,
                    headline,
                    (hudWidth - glyphLayout.width) * 0.5f,
                    hudHeight * 0.60f);

            hudFont.setColor(0.87f, 0.91f, 0.95f, 1f);
            glyphLayout.setText(hudFont, subline);
            hudFont.draw(
                    spriteBatch,
                    subline,
                    (hudWidth - glyphLayout.width) * 0.5f,
                    hudHeight * 0.60f - 34f);
        }

        spriteBatch.end();

        if (touchControlsEnabled) {
            drawTouchControls();
        }
    }

    private String buildControlsText() {
        if (touchControlsEnabled) {
            return "Touch: steer left, gas top-right, reverse bottom-right, restart top-right.";
        }
        return "WASD or Arrow Keys: drive and steer   R / Enter: restart   Esc: quit";
    }

    private String buildStatusText() {
        int aliveCars = 0;
        for (int i = 0; i < cars.size; i++) {
            if (cars.get(i).active) {
                aliveCars++;
            }
        }

        return "Wins " + playerWins
                + "  |  Round " + roundNumber
                + "  |  Cars left: " + aliveCars + "/" + cars.size
                + "  |  Arenas " + mapProgression.getCurrentMapNumber() + "/" + mapProgression.getMapCount();
    }

    private String buildObjectiveText() {
        return currentMap.getName() + ": win the arena to unlock " + mapProgression.getNextMap().getName() + ".";
    }

    private String buildRivalText() {
        StringBuilder builder = new StringBuilder("Rivals: ");
        boolean first = true;

        for (int i = 0; i < cars.size; i++) {
            Car car = cars.get(i);
            if (car.playerControlled) {
                continue;
            }

            if (!first) {
                builder.append("   |   ");
            }

            builder.append(car.name)
                    .append(" / ")
                    .append(car.getPersonalityDisplayName());
            if (!car.active) {
                builder.append(" out");
            }
            first = false;
        }

        return builder.toString();
    }

    private String buildHeadline() {
        if (!roundOver) {
            return "OUT";
        }

        if (winner == null) {
            return "DRAW";
        }

        if (winner.playerControlled) {
            return "MAP CLEAR";
        }

        return winner.name.toUpperCase() + " WINS";
    }

    private String buildSubline() {
        if (!roundOver) {
            return touchControlsEnabled
                    ? "You are out. Tap Restart to retry or watch the pile-up."
                    : "You are out. Watch the chaos or restart.";
        }

        if (winner == null) {
            return touchControlsEnabled
                    ? "Draw. Tap Restart to replay the arena."
                    : "Draw. Press Enter or R to replay the arena.";
        }

        if (winner.playerControlled) {
            return "Next arena: " + mapProgression.getNextMap().getName()
                    + ". Press restart now or wait a moment.";
        }

        return touchControlsEnabled
                ? "Retry " + currentMap.getName() + " with the Restart button."
                : "Press Enter or R to retry " + currentMap.getName() + ".";
    }

    private void drawTouchControls() {
        shapeRenderer.setProjectionMatrix(hudCamera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        shapeRenderer.setColor(0.10f, 0.12f, 0.16f, 0.28f);
        shapeRenderer.rect(steerPadBounds.x, steerPadBounds.y, steerPadBounds.width, steerPadBounds.height);
        shapeRenderer.rect(throttlePadBounds.x, throttlePadBounds.y, throttlePadBounds.width, throttlePadBounds.height);
        shapeRenderer.rect(reversePadBounds.x, reversePadBounds.y, reversePadBounds.width, reversePadBounds.height);
        shapeRenderer.rect(restartButtonBounds.x, restartButtonBounds.y, restartButtonBounds.width, restartButtonBounds.height);

        shapeRenderer.setColor(1f, 1f, 1f, 0.08f);
        float steerCenterX = steerPadBounds.x + steerPadBounds.width * 0.5f;
        shapeRenderer.rect(steerCenterX - 2f, steerPadBounds.y + 12f, 4f, steerPadBounds.height - 24f);

        float knobX = steerCenterX - touchTurnInput * steerPadBounds.width * 0.26f;
        float knobY = steerPadBounds.y + steerPadBounds.height * 0.5f;
        shapeRenderer.setColor(0.95f, 0.78f, 0.18f, 0.42f);
        shapeRenderer.circle(knobX, knobY, steerPadBounds.width * 0.12f, 28);

        shapeRenderer.setColor(0.73f, 0.88f, 0.97f, touchThrottleInput > 0f ? 0.40f : 0.16f);
        shapeRenderer.rect(throttlePadBounds.x + 8f, throttlePadBounds.y + 8f, throttlePadBounds.width - 16f, throttlePadBounds.height - 16f);

        shapeRenderer.setColor(0.95f, 0.64f, 0.36f, touchThrottleInput < 0f ? 0.40f : 0.16f);
        shapeRenderer.rect(reversePadBounds.x + 8f, reversePadBounds.y + 8f, reversePadBounds.width - 16f, reversePadBounds.height - 16f);

        shapeRenderer.setColor(0.97f, 0.86f, 0.52f, touchRestartPressed ? 0.40f : 0.18f);
        shapeRenderer.rect(restartButtonBounds.x + 8f, restartButtonBounds.y + 8f, restartButtonBounds.width - 16f, restartButtonBounds.height - 16f);

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private MapTheme currentTheme() {
        return MAP_THEMES[(mapProgression.getCurrentMapNumber() - 1) % MAP_THEMES.length];
    }

    @Override
    public void dispose() {
        if (world != null) {
            world.dispose();
        }
        shapeRenderer.dispose();
        spriteBatch.dispose();
        hudFont.dispose();
        titleFont.dispose();
    }

    private static final class Car implements AiVehicleView {
        private static final float WIDTH = 1.35f;
        private static final float HEIGHT = 2.35f;
        private static final float HALF_WIDTH = WIDTH * 0.5f;
        private static final float HALF_HEIGHT = HEIGHT * 0.5f;
        private static final float DRIVE_FORCE = 105f;
        private static final float REVERSE_FORCE = 82f;
        private static final float PLAYER_TURN_TORQUE = 48f;
        private static final float AI_TURN_TORQUE = 38f;
        private static final float LATERAL_GRIP = 0.82f;
        private static final float ANGULAR_GRIP = 0.25f;
        private static final float FORWARD_DRAG = 2.0f;
        private static final float MAX_SPEED = 14f;

        private final String name;
        private final boolean playerControlled;
        private final Color color;
        private final CarAiController aiController;
        private final Vector2 forwardAxis = new Vector2();
        private final Vector2 sidewaysAxis = new Vector2();
        private final Vector2 working = new Vector2();

        private Body body;
        private boolean active = true;

        private Car(
                Body body,
                String name,
                boolean playerControlled,
                Color color,
                AiDrivingPersonality aiPersonality) {
            this.body = body;
            this.name = name;
            this.playerControlled = playerControlled;
            this.color = color;
            aiController = playerControlled ? null : new CarAiController(aiPersonality);
        }

        private void step(
                float delta,
                ArenaMap arenaMap,
                Array<Car> cars,
                boolean allowControl,
                float playerThrottle,
                float playerTurn) {
            if (!active || body == null) {
                return;
            }

            applyGrip();

            if (!allowControl) {
                return;
            }

            if (playerControlled) {
                drive(playerThrottle, playerTurn);
                return;
            }

            AiControlDecision decision = aiController.plan(delta, this, arenaMap, cars);
            drive(decision.throttle, decision.turn);
        }

        private void applyGrip() {
            forwardAxis.set(body.getWorldVector(working.set(0f, 1f)));
            sidewaysAxis.set(body.getWorldVector(working.set(1f, 0f)));

            float lateralSpeed = sidewaysAxis.dot(body.getLinearVelocity());
            working.set(sidewaysAxis).scl(-lateralSpeed * body.getMass() * LATERAL_GRIP);
            body.applyLinearImpulse(working, body.getWorldCenter(), true);

            body.applyAngularImpulse(
                    -body.getAngularVelocity() * body.getInertia() * ANGULAR_GRIP,
                    true);

            float forwardSpeed = forwardAxis.dot(body.getLinearVelocity());
            working.set(forwardAxis).scl(-forwardSpeed * FORWARD_DRAG);
            body.applyForceToCenter(working, true);
        }

        private void drive(float throttle, float turn) {
            forwardAxis.set(body.getWorldVector(working.set(0f, 1f)));
            float signedForwardSpeed = forwardAxis.dot(body.getLinearVelocity());

            float engineForce = 0f;
            if (throttle > 0f) {
                engineForce = throttle * DRIVE_FORCE;
            } else if (throttle < 0f) {
                engineForce = throttle * REVERSE_FORCE;
            }

            if (engineForce != 0f) {
                working.set(forwardAxis).scl(engineForce);
                body.applyForceToCenter(working, true);
            }

            float steeringStrength;
            float turnTorque;
            if (playerControlled) {
                steeringStrength = MathUtils.clamp(body.getLinearVelocity().len() / 4.3f, 0.62f, 1.3f);
                turnTorque = PLAYER_TURN_TORQUE;
            } else {
                steeringStrength = MathUtils.clamp(body.getLinearVelocity().len() / 5.2f, 0.34f, 1.1f);
                turnTorque = AI_TURN_TORQUE;
            }

            float steeringDirection = 1f;
            if (signedForwardSpeed < -0.25f) {
                steeringDirection = -1f;
            } else if (signedForwardSpeed < 0.25f && throttle < -0.1f) {
                steeringDirection = -1f;
            }
            body.applyTorque(turn * steeringDirection * turnTorque * steeringStrength, true);

            float currentSpeed = body.getLinearVelocity().len();
            if (currentSpeed > MAX_SPEED) {
                working.set(body.getLinearVelocity()).scl(MAX_SPEED / currentSpeed);
                body.setLinearVelocity(working);
            }
        }

        private String getPersonalityDisplayName() {
            return aiController == null ? "Player" : aiController.getPersonality().displayName;
        }

        @Override
        public Body getBody() {
            return body;
        }

        @Override
        public boolean isActive() {
            return active;
        }

        @Override
        public boolean isPlayerControlled() {
            return playerControlled;
        }

        private void eliminate(World world) {
            if (!active || body == null) {
                return;
            }

            active = false;
            world.destroyBody(body);
            body = null;
        }
    }

    private static final class CarTemplate {
        private final String name;
        private final boolean playerControlled;
        private final Color color;
        private final AiDrivingPersonality personality;

        private CarTemplate(
                String name,
                boolean playerControlled,
                Color color,
                AiDrivingPersonality personality) {
            this.name = name;
            this.playerControlled = playerControlled;
            this.color = color;
            this.personality = personality;
        }
    }

    private static final class MapTheme {
        private final Color edge;
        private final Color surface;
        private final Color center;

        private MapTheme(Color edge, Color surface, Color center) {
            this.edge = edge;
            this.surface = surface;
            this.center = center;
        }
    }
}
