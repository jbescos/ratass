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
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Manifold;
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
import java.util.Comparator;

public class RatassGame extends ApplicationAdapter {
    private static final float WORLD_WIDTH = 32f;
    private static final float WORLD_HEIGHT = 18f;
    private static final float EDGE_FALLOFF_MARGIN = 0.35f;
    private static final float PHYSICS_STEP = 1f / 60f;
    private static final int VELOCITY_ITERATIONS = 6;
    private static final int POSITION_ITERATIONS = 2;
    private static final float ARENA_EDGE_INSET = 0.7f;
    private static final float ARENA_CENTER_INSET = 1.15f;
    private static final float AUTO_ADVANCE_DELAY = 3f;
    private static final float GROWTH_PICKUP_RADIUS = 0.42f;
    private static final float GROWTH_PICKUP_SPAWN_MARGIN = 1.05f;
    private static final float GROWTH_PICKUP_MIN_MOVE_DISTANCE = 3.4f;
    private static final int GROWTH_PICKUP_SPAWN_ATTEMPTS = 96;
    private static final float GROWTH_DURATION = 5f;
    private static final float DESTRUCTION_EFFECT_DURATION = 0.65f;
    private static final int TOTAL_CARS = 20;
    private static final float ROUND_START_COUNTDOWN = 3f;
    private static final float ROUND_TIME_LIMIT = 30f;
    private static final float ROUND_TIMEOUT_LIMIT = 60f;
    private static final float CAMERA_HORIZONTAL_PADDING = 4f;
    private static final float CAMERA_VERTICAL_PADDING = 3f;
    private static final int ROUND_SPAWN_ATTEMPTS = 3200;
    private static final float ROUND_SPAWN_SAFE_MARGIN = 1.15f;
    private static final float ROUND_SPAWN_MIN_DISTANCE = 1.95f;
    private static final float SUDDEN_DEATH_TIE_SPEED_MARGIN = 0.08f;
    private static final String[] ENEMY_NAMES = new String[] {
            "Cinder",
            "Frost",
            "Moss",
            "Volt",
            "Riot",
            "Slate",
            "Tango",
            "Brick",
            "Blitz",
            "Orbit",
            "Knurl",
            "Viper",
            "Torque",
            "Crush",
            "Rivet",
            "Dune",
            "Glitch",
            "Piston",
            "Grit"
    };

    private static final AiDrivingPersonality[] ENEMY_PERSONALITIES =
            new AiDrivingPersonality[] {
                    AiDrivingPersonalities.BRAWLER,
                    AiDrivingPersonalities.INTERCEPTOR,
                    AiDrivingPersonalities.SURVIVOR,
                    AiDrivingPersonalities.BALANCED
            };

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
    private final Array<CarTemplate> leaderboardEntries = new Array<CarTemplate>();
    private final Array<Car> pendingCollisionEliminations = new Array<Car>();
    private final Array<DestructionEffect> destructionEffects = new Array<DestructionEffect>();
    private final GlyphLayout glyphLayout = new GlyphLayout();
    private final Color tint = new Color();
    private final Rectangle mapBounds = new Rectangle();
    private final Vector2 focusPoint = new Vector2();
    private final Vector2 growthPickupPosition = new Vector2();
    private final Vector2 pickupCandidate = new Vector2();
    private final Vector2 lastGrowthPickupPosition = new Vector2();
    private final Vector2 spawnCandidate = new Vector2();
    private final Array<SpawnPoint> roundSpawns = new Array<SpawnPoint>();
    private final Rectangle steerPadBounds = new Rectangle();
    private final Rectangle throttlePadBounds = new Rectangle();
    private final Rectangle reversePadBounds = new Rectangle();
    private final Rectangle restartButtonBounds = new Rectangle();
    private final Vector3 hudTouchPoint = new Vector3();
    private final Vector3 carLabelProjection = new Vector3();
    private final ImpactContactListener impactContactListener = new ImpactContactListener();
    private final Comparator<CarTemplate> leaderboardComparator = new Comparator<CarTemplate>() {
        @Override
        public int compare(CarTemplate left, CarTemplate right) {
            if (left.totalPoints != right.totalPoints) {
                return right.totalPoints - left.totalPoints;
            }
            if (left.roundFinishPosition != right.roundFinishPosition) {
                return right.roundFinishPosition - left.roundFinishPosition;
            }
            if (left.playerControlled != right.playerControlled) {
                return left.playerControlled ? -1 : 1;
            }
            return left.name.compareTo(right.name);
        }
    };

    private OrthographicCamera worldCamera;
    private FitViewport worldViewport;
    private OrthographicCamera hudCamera;
    private ScreenViewport hudViewport;
    private ShapeRenderer shapeRenderer;
    private SpriteBatch spriteBatch;
    private BitmapFont hudFont;
    private BitmapFont titleFont;
    private BitmapFont leaderboardFont;
    private BitmapFont labelFont;
    private World world;

    private MapProgression mapProgression;
    private ArenaMap currentMap;
    private float accumulator;
    private float roundOverTimer;
    private float effectClock;
    private float frameThrottleInput;
    private float frameTurnInput;
    private float preRoundCountdownTimer;
    private float roundTimer;
    private float touchThrottleInput;
    private float touchTurnInput;
    private float growthBoostTimer;
    private boolean touchRestartPressed;
    private boolean touchRestartJustPressed;
    private boolean touchControlsEnabled;
    private boolean growthPickupActive;
    private boolean hasLastGrowthPickupPosition;
    private boolean roundOver;
    private boolean roundTimedOut;
    private boolean suddenDeathActive;
    private int roundNumber;
    private int playerWins;
    private int finishPositionCounter;
    private int timeoutSharedPoints;
    private int timeoutSurvivorCount;
    private Car boostedCar;
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

        leaderboardFont = new BitmapFont();
        leaderboardFont.setUseIntegerPositions(false);
        leaderboardFont.getData().setScale(0.82f);

        labelFont = new BitmapFont();
        labelFont.setUseIntegerPositions(false);
        labelFont.getData().setScale(0.74f);

        Gdx.input.setCatchKey(Input.Keys.BACK, true);
        createRoster();
        mapProgression = new MapProgression(ArenaMaps.createDefaultSet());

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        resetRound(false);
    }

    private void createRoster() {
        roster.clear();
        roster.add(new CarTemplate("You", true, new Color(0.90f, 0.25f, 0.20f, 1f), null));

        for (int i = 0; i < ENEMY_NAMES.length && roster.size < TOTAL_CARS; i++) {
            roster.add(new CarTemplate(
                    ENEMY_NAMES[i],
                    false,
                    createEnemyColor(i),
                    ENEMY_PERSONALITIES[i % ENEMY_PERSONALITIES.length]));
        }

        while (roster.size < TOTAL_CARS) {
            int enemyIndex = roster.size - 1;
            roster.add(new CarTemplate(
                    "Rival " + roster.size,
                    false,
                    createEnemyColor(enemyIndex),
                    ENEMY_PERSONALITIES[enemyIndex % ENEMY_PERSONALITIES.length]));
        }
    }

    @Override
    public void resize(int width, int height) {
        worldViewport.update(width, height);
        updateWorldCamera();

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

        update(delta);
        renderWorld();
        renderHud();
    }

    private void update(float delta) {
        effectClock += delta;
        updateDestructionEffects(delta);

        if (roundOver) {
            roundOverTimer += delta;
            if (roundOverTimer >= AUTO_ADVANCE_DELAY) {
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
        if (!roundOver && preRoundCountdownTimer > 0f) {
            preRoundCountdownTimer = Math.max(0f, preRoundCountdownTimer - delta);
        }

        boolean allowControl = !roundOver && preRoundCountdownTimer <= 0f;
        for (int i = 0; i < cars.size; i++) {
            Car car = cars.get(i);
            car.step(
                    delta,
                    currentMap,
                    cars,
                    allowControl,
                    frameThrottleInput,
                    frameTurnInput,
                    growthPickupActive,
                    growthPickupPosition);
        }

        if (!allowControl && !roundOver) {
            freezeCarsForCountdown();
        }

        world.step(delta, VELOCITY_ITERATIONS, POSITION_ITERATIONS);
        processPendingCollisionEliminations();
        checkForEliminations();

        if (!roundOver) {
            updateRoundState();
            if (!roundOver && allowControl) {
                updateRoundTimer(delta);
                if (!roundOver) {
                    updateGrowthPickup(delta);
                }
            }
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
                eliminateCar(car);
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

        finishRound(lastAlive);
    }

    private void finishRound(Car roundWinner) {
        roundOver = true;
        roundOverTimer = 0f;
        winner = roundWinner;
        finalizeRoundResults();
        stopCars();
    }

    private void stopCars() {
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
            mapProgression.advanceIfPlayerWon(true);
        }

        currentMap = mapProgression.getCurrentMap();
        updateWorldCamera();

        if (world != null) {
            world.dispose();
        }

        world = new World(new Vector2(0f, 0f), true);
        world.setContactListener(impactContactListener);
        cars.clear();
        pendingCollisionEliminations.clear();
        destructionEffects.clear();
        accumulator = 0f;
        effectClock = 0f;
        growthPickupActive = false;
        hasLastGrowthPickupPosition = false;
        growthBoostTimer = 0f;
        boostedCar = null;
        preRoundCountdownTimer = ROUND_START_COUNTDOWN;
        roundTimer = 0f;
        roundOver = false;
        roundOverTimer = 0f;
        roundTimedOut = false;
        suddenDeathActive = false;
        winner = null;
        timeoutSharedPoints = 0;
        timeoutSurvivorCount = 0;
        playerCar = null;
        finishPositionCounter = 0;
        roundNumber++;
        buildRoundSpawns(roster.size, roundSpawns);

        for (int i = 0; i < roster.size; i++) {
            CarTemplate template = roster.get(i);
            template.roundFinishPosition = 0;
            template.lastRoundAwardedPoints = 0;
            template.roundPickupPoints = 0;
            SpawnPoint spawnPoint = roundSpawns.get(i);
            Car car = createCar(template, spawnPoint);
            template.currentCar = car;
            if (template.playerControlled) {
                playerCar = car;
            }
        }

        spawnGrowthPickup();
    }

    private void freezeCarsForCountdown() {
        for (int i = 0; i < cars.size; i++) {
            Car car = cars.get(i);
            if (!car.active || car.body == null) {
                continue;
            }
            car.body.setLinearVelocity(0f, 0f);
            car.body.setAngularVelocity(0f);
        }
    }

    private void processPendingCollisionEliminations() {
        if (pendingCollisionEliminations.size == 0) {
            return;
        }

        for (int i = 0; i < pendingCollisionEliminations.size; i++) {
            eliminateCar(pendingCollisionEliminations.get(i));
        }
        pendingCollisionEliminations.clear();
    }

    private void updateRoundTimer(float delta) {
        roundTimer = Math.min(ROUND_TIMEOUT_LIMIT, roundTimer + delta);
        if (!suddenDeathActive && roundTimer >= ROUND_TIME_LIMIT) {
            suddenDeathActive = true;
        }
        if (roundTimer >= ROUND_TIMEOUT_LIMIT) {
            triggerRoundTimeout();
        }
    }

    private void triggerRoundTimeout() {
        Array<Car> survivors = new Array<Car>();
        for (int i = 0; i < cars.size; i++) {
            Car car = cars.get(i);
            if (car.active && car.body != null) {
                survivors.add(car);
            }
        }

        roundTimedOut = true;
        timeoutSurvivorCount = survivors.size;
        if (survivors.size > 0) {
            timeoutSharedPoints = roster.size - survivors.size + 1;
            for (int i = 0; i < survivors.size; i++) {
                survivors.get(i).template.roundFinishPosition = timeoutSharedPoints;
            }
            for (int i = 0; i < survivors.size; i++) {
                eliminateCar(survivors.get(i));
            }
        } else {
            timeoutSharedPoints = 0;
        }

        finishRound(null);
    }

    private void finalizeRoundResults() {
        for (int i = 0; i < roster.size; i++) {
            CarTemplate template = roster.get(i);
            if (template.roundFinishPosition == 0) {
                template.roundFinishPosition = ++finishPositionCounter;
            }
            template.lastRoundAwardedPoints = template.roundFinishPosition + template.roundPickupPoints;
            template.totalPoints += template.roundFinishPosition;
        }

        if (winner != null && winner.playerControlled) {
            playerWins++;
        }
    }

    private void eliminateCar(Car car) {
        if (car == null || !car.active || car.body == null) {
            return;
        }

        spawnDestructionEffect(car);
        if (boostedCar == car) {
            clearGrowthBoost();
        }
        if (car.template.roundFinishPosition == 0) {
            car.template.roundFinishPosition = ++finishPositionCounter;
        }
        car.pendingElimination = false;
        car.eliminate(world);
    }

    private void queueCollisionElimination(Car car) {
        if (car == null || !car.active || car.pendingElimination) {
            return;
        }

        car.pendingElimination = true;
        pendingCollisionEliminations.add(car);
    }

    private void updateDestructionEffects(float delta) {
        for (int i = destructionEffects.size - 1; i >= 0; i--) {
            DestructionEffect effect = destructionEffects.get(i);
            effect.timer -= delta;
            if (effect.timer <= 0f) {
                destructionEffects.removeIndex(i);
            }
        }
    }

    private void spawnDestructionEffect(Car car) {
        DestructionEffect effect = new DestructionEffect();
        effect.position.set(car.body.getPosition());
        effect.color.set(car.color);
        effect.timer = DESTRUCTION_EFFECT_DURATION;
        effect.rotationDeg = MathUtils.random(360f);
        effect.scale = 0.92f + MathUtils.random(0.55f) + car.getSizeScale() * 0.12f;
        destructionEffects.add(effect);
    }

    private Color createEnemyColor(int index) {
        float hue = (28f + index * 31f) % 360f;
        float saturation = 0.66f + (index % 3) * 0.06f;
        float value = 0.86f + (index % 4) * 0.03f;
        return hsvColor(hue, saturation, Math.min(1f, value));
    }

    private Color hsvColor(float hue, float saturation, float value) {
        float normalizedHue = ((hue % 360f) + 360f) % 360f;
        float chroma = value * saturation;
        float segment = normalizedHue / 60f;
        float x = chroma * (1f - Math.abs(segment % 2f - 1f));

        float r = 0f;
        float g = 0f;
        float b = 0f;

        if (segment < 1f) {
            r = chroma;
            g = x;
        } else if (segment < 2f) {
            r = x;
            g = chroma;
        } else if (segment < 3f) {
            g = chroma;
            b = x;
        } else if (segment < 4f) {
            g = x;
            b = chroma;
        } else if (segment < 5f) {
            r = x;
            b = chroma;
        } else {
            r = chroma;
            b = x;
        }

        float match = value - chroma;
        return new Color(r + match, g + match, b + match, 1f);
    }

    private void updateWorldCamera() {
        if (worldCamera == null) {
            return;
        }

        if (currentMap == null) {
            worldCamera.zoom = 1f;
            worldCamera.position.set(0f, 0f, 0f);
            worldCamera.update();
            return;
        }

        currentMap.getBounds(mapBounds);
        currentMap.getFocusPoint(focusPoint);

        float visibleWidth = Math.max(1f, WORLD_WIDTH - CAMERA_HORIZONTAL_PADDING);
        float visibleHeight = Math.max(1f, WORLD_HEIGHT - CAMERA_VERTICAL_PADDING);
        float zoomX = mapBounds.width / visibleWidth;
        float zoomY = mapBounds.height / visibleHeight;

        worldCamera.zoom = Math.max(1f, Math.max(zoomX, zoomY));
        worldCamera.position.set(focusPoint.x, focusPoint.y, 0f);
        worldCamera.update();
    }

    private void buildRoundSpawns(int count, Array<SpawnPoint> out) {
        out.clear();
        currentMap.getFocusPoint(focusPoint);
        currentMap.getBounds(mapBounds);

        for (int i = 0; i < currentMap.getSpawnCount() && out.size < count; i++) {
            SpawnPoint seed = currentMap.getSpawn(i);
            spawnCandidate.set(seed.x, seed.y);
            if (currentMap.distanceToHazard(spawnCandidate) < ROUND_SPAWN_SAFE_MARGIN) {
                continue;
            }
            if (isSpawnLocationClear(spawnCandidate, out, ROUND_SPAWN_MIN_DISTANCE)) {
                out.add(seed);
            }
        }

        float minX = mapBounds.x + ROUND_SPAWN_SAFE_MARGIN;
        float maxX = mapBounds.x + mapBounds.width - ROUND_SPAWN_SAFE_MARGIN;
        float minY = mapBounds.y + ROUND_SPAWN_SAFE_MARGIN;
        float maxY = mapBounds.y + mapBounds.height - ROUND_SPAWN_SAFE_MARGIN;

        for (int attempt = 0; attempt < ROUND_SPAWN_ATTEMPTS && out.size < count; attempt++) {
            spawnCandidate.set(MathUtils.random(minX, maxX), MathUtils.random(minY, maxY));
            currentMap.clampToPlayable(spawnCandidate, ROUND_SPAWN_SAFE_MARGIN);

            if (currentMap.distanceToHazard(spawnCandidate) < ROUND_SPAWN_SAFE_MARGIN) {
                continue;
            }
            if (!isSpawnLocationClear(spawnCandidate, out, ROUND_SPAWN_MIN_DISTANCE)) {
                continue;
            }

            out.add(SpawnPoint.facingPoint(
                    spawnCandidate.x,
                    spawnCandidate.y,
                    focusPoint.x,
                    focusPoint.y));
        }

        if (out.size < count) {
            throw new IllegalStateException("Could not generate enough safe spawn points for the current map.");
        }
    }

    private boolean isSpawnLocationClear(Vector2 candidate, Array<SpawnPoint> spawns, float minDistance) {
        float minDistanceSq = minDistance * minDistance;
        for (int i = 0; i < spawns.size; i++) {
            SpawnPoint spawnPoint = spawns.get(i);
            if (Vector2.dst2(candidate.x, candidate.y, spawnPoint.x, spawnPoint.y) < minDistanceSq) {
                return false;
            }
        }
        return true;
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
            }
        }

        touchRestartJustPressed = false;
        touchRestartPressed = false;
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
    }

    private Car createCar(CarTemplate template, SpawnPoint spawnPoint) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(spawnPoint.x, spawnPoint.y);
        bodyDef.angle = spawnPoint.angleRad;
        bodyDef.linearDamping = 0.18f;
        bodyDef.angularDamping = 1.45f;
        bodyDef.bullet = true;

        Body body = world.createBody(bodyDef);

        Car car = new Car(body, template);
        body.setUserData(car);
        cars.add(car);
        return car;
    }

    private void updateGrowthPickup(float delta) {
        if (boostedCar != null) {
            if (!boostedCar.active || boostedCar.body == null) {
                clearGrowthBoost();
                spawnGrowthPickup();
                return;
            }

            growthBoostTimer -= delta;
            if (growthBoostTimer <= 0f) {
                clearGrowthBoost();
                spawnGrowthPickup();
            }
            return;
        }

        if (!growthPickupActive) {
            spawnGrowthPickup();
            return;
        }

        for (int i = 0; i < cars.size; i++) {
            Car car = cars.get(i);
            if (!car.active || car.body == null) {
                continue;
            }

            float collectRadius = GROWTH_PICKUP_RADIUS + car.getGrowthPickupReach();
            if (car.body.getPosition().dst2(growthPickupPosition) <= collectRadius * collectRadius) {
                collectGrowthPickup(car);
                return;
            }
        }
    }

    private void collectGrowthPickup(Car car) {
        if (car == null || !growthPickupActive || !car.active) {
            return;
        }

        lastGrowthPickupPosition.set(growthPickupPosition);
        hasLastGrowthPickupPosition = true;
        growthPickupActive = false;
        boostedCar = car;
        growthBoostTimer = GROWTH_DURATION;
        car.template.totalPoints++;
        car.template.roundPickupPoints++;
        car.setGrowthBoost(true);
    }

    private void clearGrowthBoost() {
        if (boostedCar != null) {
            boostedCar.setGrowthBoost(false);
            boostedCar = null;
        }
        growthBoostTimer = 0f;
    }

    private void spawnGrowthPickup() {
        if (currentMap == null) {
            return;
        }

        growthPickupActive = false;
        currentMap.getBounds(mapBounds);

        if (trySpawnGrowthPickup(true, true)
                || trySpawnGrowthPickup(true, false)
                || trySpawnGrowthPickup(false, false)) {
            return;
        }

        pickupCandidate.set(0f, 0f);
        currentMap.findRecoveryPoint(pickupCandidate, growthPickupPosition);
        growthPickupActive = true;
    }

    private boolean trySpawnGrowthPickup(boolean avoidCars, boolean requireNewSpot) {
        float minX = mapBounds.x + GROWTH_PICKUP_SPAWN_MARGIN;
        float maxX = mapBounds.x + mapBounds.width - GROWTH_PICKUP_SPAWN_MARGIN;
        float minY = mapBounds.y + GROWTH_PICKUP_SPAWN_MARGIN;
        float maxY = mapBounds.y + mapBounds.height - GROWTH_PICKUP_SPAWN_MARGIN;

        if (minX >= maxX || minY >= maxY) {
            return false;
        }

        for (int attempt = 0; attempt < GROWTH_PICKUP_SPAWN_ATTEMPTS; attempt++) {
            pickupCandidate.set(MathUtils.random(minX, maxX), MathUtils.random(minY, maxY));

            if (currentMap.distanceToHazard(pickupCandidate) < GROWTH_PICKUP_SPAWN_MARGIN) {
                continue;
            }
            if (requireNewSpot
                    && hasLastGrowthPickupPosition
                    && pickupCandidate.dst2(lastGrowthPickupPosition)
                    < GROWTH_PICKUP_MIN_MOVE_DISTANCE * GROWTH_PICKUP_MIN_MOVE_DISTANCE) {
                continue;
            }
            if (avoidCars && !isGrowthPickupFarFromCars(pickupCandidate)) {
                continue;
            }

            growthPickupPosition.set(pickupCandidate);
            growthPickupActive = true;
            return true;
        }

        return false;
    }

    private boolean isGrowthPickupFarFromCars(Vector2 candidate) {
        for (int i = 0; i < cars.size; i++) {
            Car car = cars.get(i);
            if (!car.active || car.body == null) {
                continue;
            }

            float minDistance = car.getGrowthPickupReach() + GROWTH_PICKUP_RADIUS + 0.75f;
            if (car.body.getPosition().dst2(candidate) < minDistance * minDistance) {
                return false;
            }
        }

        return true;
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
        drawGrowthPickup();
        drawCars();
        drawDestructionEffects();
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

    private void drawGrowthPickup() {
        if (!growthPickupActive) {
            return;
        }

        float pulse = 0.5f + 0.5f * MathUtils.sin(effectClock * 6.5f);
        float spinDeg = effectClock * 135f;

        shapeRenderer.setColor(1f, 0.84f, 0.22f, 0.16f + pulse * 0.10f);
        shapeRenderer.circle(
                growthPickupPosition.x,
                growthPickupPosition.y,
                GROWTH_PICKUP_RADIUS * (1.85f + pulse * 0.45f),
                28);

        drawRotatedRect(
                growthPickupPosition.x,
                growthPickupPosition.y,
                GROWTH_PICKUP_RADIUS * 2.15f,
                GROWTH_PICKUP_RADIUS * 2.15f,
                spinDeg,
                0.96f,
                0.70f,
                0.18f,
                0.92f);

        drawRotatedRect(
                growthPickupPosition.x,
                growthPickupPosition.y,
                GROWTH_PICKUP_RADIUS * 1.30f,
                GROWTH_PICKUP_RADIUS * 1.30f,
                -spinDeg * 1.25f,
                1f,
                0.93f,
                0.46f,
                0.96f);

        shapeRenderer.setColor(1f, 0.99f, 0.86f, 1f);
        shapeRenderer.circle(
                growthPickupPosition.x,
                growthPickupPosition.y,
                GROWTH_PICKUP_RADIUS * 0.34f,
                20);
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
            float carWidth = car.getWidth();
            float carHeight = car.getHeight();
            float carScale = car.getSizeScale();

            drawRotatedRect(
                    centerX + 0.10f * carScale,
                    centerY - 0.12f * carScale,
                    carWidth,
                    carHeight,
                    angleDeg,
                    0.04f,
                    0.05f,
                    0.07f,
                    0.32f);

            if (car.hasGrowthBoost()) {
                float boostPulse = 0.5f + 0.5f * MathUtils.sin(effectClock * 9f);
                drawRotatedRect(
                        centerX,
                        centerY,
                        carWidth + (0.30f + boostPulse * 0.12f) * carScale,
                        carHeight + (0.30f + boostPulse * 0.12f) * carScale,
                        angleDeg,
                        1f,
                        0.82f,
                        0.22f,
                        0.18f + boostPulse * 0.07f);
            }

            if (car.playerControlled) {
                drawRotatedRect(
                        centerX,
                        centerY,
                        carWidth + 0.42f * carScale,
                        carHeight + 0.42f * carScale,
                        angleDeg,
                        0.36f,
                        0.82f,
                        1f,
                        0.22f);

                drawRotatedRect(
                        centerX,
                        centerY,
                        carWidth + 0.20f * carScale,
                        carHeight + 0.20f * carScale,
                        angleDeg,
                        0.96f,
                        0.94f,
                        0.88f,
                        0.26f);
            }

            drawRotatedRect(
                    centerX,
                    centerY,
                    carWidth,
                    carHeight,
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
                    -0.08f * carScale,
                    carWidth * 0.68f,
                    carHeight * 0.50f,
                    angleDeg,
                    tint.r,
                    tint.g,
                    tint.b,
                    1f);

            drawOffsetRotatedRect(
                    centerX,
                    centerY,
                    0f,
                    0.40f * carScale,
                    carWidth * 0.62f,
                    carHeight * 0.16f,
                    angleDeg,
                    0.12f,
                    0.13f,
                    0.15f,
                    1f);

            drawOffsetRotatedRect(
                    centerX,
                    centerY,
                    0f,
                    0.84f * carScale,
                    carWidth * 0.55f,
                    carHeight * 0.09f,
                    angleDeg,
                    0.96f,
                    0.92f,
                    0.74f,
                    1f);
        }
    }

    private void drawDestructionEffects() {
        for (int i = 0; i < destructionEffects.size; i++) {
            DestructionEffect effect = destructionEffects.get(i);
            float progress = 1f - effect.timer / DESTRUCTION_EFFECT_DURATION;
            float alpha = MathUtils.clamp(1f - progress, 0f, 1f);
            float burstRadius = (0.72f + progress * 1.65f) * effect.scale;
            float flashRadius = (0.28f + progress * 0.42f) * effect.scale;

            shapeRenderer.setColor(effect.color.r, effect.color.g, effect.color.b, 0.18f * alpha);
            shapeRenderer.circle(effect.position.x, effect.position.y, burstRadius, 28);

            shapeRenderer.setColor(1f, 0.96f, 0.78f, 0.68f * alpha);
            shapeRenderer.circle(effect.position.x, effect.position.y, flashRadius, 20);

            for (int shard = 0; shard < 6; shard++) {
                float angleDeg = effect.rotationDeg + shard * 60f + progress * 95f;
                float shardDistance = burstRadius * (0.58f + shard * 0.04f);
                float shardX = effect.position.x + MathUtils.cosDeg(angleDeg) * shardDistance;
                float shardY = effect.position.y + MathUtils.sinDeg(angleDeg) * shardDistance;
                float shardWidth = (0.46f - progress * 0.10f) * effect.scale;
                float shardHeight = (0.16f + progress * 0.18f) * effect.scale;

                drawRotatedRect(
                        shardX,
                        shardY,
                        shardWidth,
                        shardHeight,
                        angleDeg,
                        effect.color.r,
                        effect.color.g,
                        effect.color.b,
                        0.74f * alpha);
            }
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
                hudWidth - glyphLayout.width - 22f,
                hudHeight - 20f);

        hudFont.setColor(0.93f, 0.84f, 0.49f, 1f);
        hudFont.draw(
                spriteBatch,
                buildObjectiveText(),
                22f,
                44f);

        drawLeaderboard(hudWidth, hudHeight);
        drawCarLabels();

        if (preRoundCountdownTimer > 0f && !roundOver) {
            drawCenteredOverlay(
                    String.valueOf(MathUtils.ceil(preRoundCountdownTimer)),
                    "GET READY",
                    hudWidth,
                    hudHeight,
                    new Color(0.98f, 0.94f, 0.84f, 1f),
                    0.62f);
        } else if (roundOver || (playerCar != null && !playerCar.active)) {
            String headline = buildHeadline();
            String subline = buildSubline();

            drawCenteredOverlay(
                    headline,
                    subline,
                    hudWidth,
                    hudHeight,
                    winner != null && winner.playerControlled
                            ? new Color(0.98f, 0.96f, 0.80f, 1f)
                            : new Color(0.97f, 0.90f, 0.78f, 1f),
                    0.60f);
        } else if (suddenDeathActive) {
            drawCenteredOverlay(
                    "SUDDEN DEATH",
                    buildSuddenDeathOverlayText(),
                    hudWidth,
                    hudHeight,
                    new Color(1f, 0.42f, 0.30f, 1f),
                    0.60f);
        }

        spriteBatch.end();

        if (touchControlsEnabled) {
            drawTouchControls();
        }
    }

    private void drawCenteredOverlay(
            String headline,
            String subline,
            float hudWidth,
            float hudHeight,
            Color headlineColor,
            float headlineYFactor) {
        titleFont.setColor(headlineColor);
        glyphLayout.setText(titleFont, headline);
        titleFont.draw(
                spriteBatch,
                headline,
                (hudWidth - glyphLayout.width) * 0.5f,
                hudHeight * headlineYFactor);

        hudFont.setColor(0.87f, 0.91f, 0.95f, 1f);
        glyphLayout.setText(hudFont, subline);
        hudFont.draw(
                spriteBatch,
                subline,
                (hudWidth - glyphLayout.width) * 0.5f,
                hudHeight * headlineYFactor - 34f);
    }

    private void drawLeaderboard(float hudWidth, float hudHeight) {
        leaderboardEntries.clear();
        leaderboardEntries.addAll(roster);
        leaderboardEntries.sort(leaderboardComparator);

        float x = hudWidth - 300f;
        float y = hudHeight - 52f;

        leaderboardFont.setColor(0.98f, 0.95f, 0.84f, 1f);
        leaderboardFont.draw(spriteBatch, "Leaderboard", x, y);
        y -= 18f;

        for (int i = 0; i < leaderboardEntries.size; i++) {
            CarTemplate template = leaderboardEntries.get(i);
            boolean active = template.currentCar != null && template.currentCar.active;
            String name = template.playerControlled ? "YOU" : template.name;
            StringBuilder row = new StringBuilder();
            row.append(i + 1).append(". ").append(name).append("  ")
                    .append(template.totalPoints).append(" pts");

            if (roundOver) {
                row.append("  +").append(template.lastRoundAwardedPoints);
            } else if (template.roundFinishPosition > 0) {
                row.append("  OUT #").append(template.roundFinishPosition);
            } else if (active && template.currentCar.hasGrowthBoost()) {
                row.append("  BIG");
            } else if (active) {
                row.append("  IN");
            }

            if (template.playerControlled) {
                leaderboardFont.setColor(1f, 0.94f, 0.54f, 1f);
            } else if (!active && !roundOver) {
                leaderboardFont.setColor(0.66f, 0.72f, 0.78f, 1f);
            } else {
                leaderboardFont.setColor(0.84f, 0.90f, 0.94f, 1f);
            }

            leaderboardFont.draw(spriteBatch, row, x, y);
            y -= 16f;
        }
    }

    private void drawCarLabels() {
        for (int i = 0; i < cars.size; i++) {
            Car car = cars.get(i);
            if (!car.active || car.body == null) {
                continue;
            }

            carLabelProjection.set(
                    car.body.getPosition().x,
                    car.body.getPosition().y + car.getHeight() * 0.82f,
                    0f);
            worldViewport.project(carLabelProjection);
            carLabelProjection.y = Gdx.graphics.getHeight() - carLabelProjection.y;
            hudViewport.unproject(carLabelProjection);

            String label = car.playerControlled ? "YOU" : car.name;
            if (car.hasGrowthBoost()) {
                label += " BIG";
            }

            glyphLayout.setText(labelFont, label);
            if (car.playerControlled) {
                labelFont.setColor(1f, 0.96f, 0.68f, 1f);
            } else {
                labelFont.setColor(0.95f, 0.96f, 0.98f, 0.94f);
            }
            labelFont.draw(
                    spriteBatch,
                    label,
                    carLabelProjection.x - glyphLayout.width * 0.5f,
                    carLabelProjection.y);
        }
    }

    private String buildControlsText() {
        if (touchControlsEnabled) {
            return "Touch: steer left, gas top-right, reverse bottom-right.";
        }
        return "WASD or Arrow Keys: drive and steer   Esc: quit";
    }

    private String buildStatusText() {
        int aliveCars = 0;
        for (int i = 0; i < cars.size; i++) {
            if (cars.get(i).active) {
                aliveCars++;
            }
        }

        StringBuilder builder = new StringBuilder();
        builder.append("You ").append(roster.first().totalPoints).append(" pts")
                .append("  |  Round ").append(roundNumber)
                .append("  |  Cars left: ").append(aliveCars).append("/").append(cars.size)
                .append("  |  Arenas ").append(mapProgression.getCurrentMapNumber()).append("/")
                .append(mapProgression.getMapCount());

        if (preRoundCountdownTimer > 0f) {
            builder.append("  |  Starts in ").append(MathUtils.ceil(preRoundCountdownTimer)).append("s");
        } else if (roundTimedOut) {
            builder.append("  |  TIME UP");
        } else if (roundOver) {
            builder.append("  |  ROUND OVER");
        } else if (suddenDeathActive) {
            builder.append("  |  SUDDEN DEATH  |  Wipe ").append(getRoundTimeoutSecondsLeft()).append("s");
        } else {
            builder.append("  |  Sudden death ").append(getSuddenDeathSecondsLeft()).append("s");
        }

        return builder.toString();
    }

    private String buildObjectiveText() {
        if (preRoundCountdownTimer > 0f) {
            return "Prepare for the horn. First car out gets 1 point. Sudden death starts at 30s and the arena wipes at 60s.";
        }

        if (roundOver) {
            if (roundTimedOut) {
                return buildTimeoutAwardText();
            }
            return "Standings updated. Next arena in a moment.";
        }

        if (suddenDeathActive) {
            return "Sudden death is live: " + buildSuddenDeathRuleText()
                    + " Arena wipe in " + getRoundTimeoutSecondsLeft() + "s.";
        }

        if (boostedCar != null && boostedCar.active) {
            return (boostedCar.playerControlled
                    ? "Mass core active: you are huge for "
                    : boostedCar.name + " has the mass core for ")
                    + MathUtils.ceil(growthBoostTimer)
                    + " more seconds. Sudden death in "
                    + getSuddenDeathSecondsLeft()
                    + "s, wipe in "
                    + getRoundTimeoutSecondsLeft()
                    + "s.";
        }

        if (growthPickupActive) {
            return currentMap.getName()
                    + ": mass core live. Sudden death in "
                    + getSuddenDeathSecondsLeft()
                    + "s, wipe in "
                    + getRoundTimeoutSecondsLeft()
                    + "s.";
        }

        return currentMap.getName() + ": stay alive. Sudden death in "
                + getSuddenDeathSecondsLeft()
                + "s, wipe in "
                + getRoundTimeoutSecondsLeft()
                + "s.";
    }

    private String buildSuddenDeathRuleText() {
        if (boostedCar != null && boostedCar.active) {
            return (boostedCar.playerControlled ? "The big YOU" : boostedCar.name + " in big form")
                    + " destroys every car it hits.";
        }
        return "The slower car in every hit is destroyed.";
    }

    private String buildSuddenDeathOverlayText() {
        return buildSuddenDeathRuleText() + " Arena wipe in " + getRoundTimeoutSecondsLeft() + "s.";
    }

    private String buildTimeoutAwardText() {
        if (timeoutSurvivorCount <= 0) {
            return "Time is up. The arena wipes everyone out.";
        }
        String pointLabel = timeoutSharedPoints == 1 ? " point" : " points";
        if (timeoutSurvivorCount == 1) {
            return "Time is up. The last survivor still banks " + timeoutSharedPoints + pointLabel + ".";
        }
        return "Time is up. " + timeoutSurvivorCount + " survivors each bank "
                + timeoutSharedPoints + pointLabel + ".";
    }

    private int getSuddenDeathSecondsLeft() {
        return MathUtils.ceil(Math.max(0f, ROUND_TIME_LIMIT - roundTimer));
    }

    private int getRoundTimeoutSecondsLeft() {
        return MathUtils.ceil(Math.max(0f, ROUND_TIMEOUT_LIMIT - roundTimer));
    }

    private String buildHeadline() {
        if (!roundOver) {
            return "OUT";
        }

        if (roundTimedOut) {
            return "TIME UP";
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
            return "You are out. Watch the finish and the leaderboard.";
        }

        if (roundTimedOut) {
            return buildTimeoutAwardText() + " Next arena in a moment.";
        }

        if (winner == null) {
            return "Standings updated. Next arena in a moment.";
        }

        if (winner.playerControlled) {
            return "You take " + currentMap.getName() + ". Next arena in a moment.";
        }

        return winner.name + " wins " + currentMap.getName() + ". Next arena in a moment.";
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
        leaderboardFont.dispose();
        labelFont.dispose();
    }

    private final class ImpactContactListener implements ContactListener {
        @Override
        public void beginContact(Contact contact) {
        }

        @Override
        public void endContact(Contact contact) {
        }

        @Override
        public void preSolve(Contact contact, Manifold oldManifold) {
        }

        @Override
        public void postSolve(Contact contact, ContactImpulse impulse) {
            Object userDataA = contact.getFixtureA().getBody().getUserData();
            Object userDataB = contact.getFixtureB().getBody().getUserData();
            if (!(userDataA instanceof Car) || !(userDataB instanceof Car)) {
                return;
            }

            Car carA = (Car) userDataA;
            Car carB = (Car) userDataB;
            if (!carA.active || !carB.active || carA.body == null || carB.body == null) {
                return;
            }

            Vector2 normal = contact.getWorldManifold().getNormal();
            if (normal.isZero(0.0001f)) {
                return;
            }

            float totalNormalImpulse = 0f;
            float[] normalImpulses = impulse.getNormalImpulses();
            for (int i = 0; i < normalImpulses.length; i++) {
                totalNormalImpulse += Math.abs(normalImpulses[i]);
            }

            if (totalNormalImpulse < Car.MIN_COLLISION_RESPONSE_IMPULSE) {
                return;
            }

            Vector2 velocityA = carA.body.getLinearVelocity();
            Vector2 velocityB = carB.body.getLinearVelocity();
            float closingSpeed = Math.max(
                    0f,
                    -((velocityB.x - velocityA.x) * normal.x
                            + (velocityB.y - velocityA.y) * normal.y));
            if (closingSpeed < Car.MIN_COLLISION_RESPONSE_SPEED) {
                return;
            }

            float impactStrength = totalNormalImpulse + closingSpeed * 2.8f;

            if (suddenDeathActive) {
                if (carA.hasGrowthBoost() != carB.hasGrowthBoost()) {
                    queueCollisionElimination(carA.hasGrowthBoost() ? carB : carA);
                } else {
                    float speedA = Math.abs(velocityA.dot(normal));
                    float speedB = Math.abs(velocityB.dot(normal));
                    if (Math.abs(speedA - speedB) <= SUDDEN_DEATH_TIE_SPEED_MARGIN) {
                        speedA = velocityA.len();
                        speedB = velocityB.len();
                    }

                    queueCollisionElimination(speedA >= speedB ? carB : carA);
                }
            }

            carA.absorbCollision(normal, -1f, impactStrength);
            carB.absorbCollision(normal, 1f, impactStrength);
        }
    }

    private static final class Car implements AiVehicleView {
        private static final float WIDTH = 1.12f;
        private static final float HEIGHT = 1.94f;
        private static final float HALF_WIDTH = WIDTH * 0.5f;
        private static final float HALF_HEIGHT = HEIGHT * 0.5f;
        private static final float FIXTURE_DENSITY = 1.3f;
        private static final float FIXTURE_FRICTION = 0.32f;
        private static final float FIXTURE_RESTITUTION = 0.12f;
        private static final float GROWTH_SCALE = 1.40f;
        private static final float GROWTH_MASS_MULTIPLIER = 10f;
        private static final float DRIVE_FORCE = 96f;
        private static final float REVERSE_FORCE = 74f;
        private static final float PLAYER_TURN_TORQUE = 48f;
        private static final float AI_TURN_TORQUE = 38f;
        private static final float LATERAL_GRIP = 0.72f;
        private static final float ANGULAR_GRIP = 0.23f;
        private static final float FORWARD_DRAG = 1.55f;
        private static final float MAX_SPEED = 15.2f;
        private static final float GROWTH_TURN_MULTIPLIER = 0.90f;
        private static final float MAX_GROWTH_SPEED_MULTIPLIER = 1.06f;
        private static final float MIN_COLLISION_RESPONSE_IMPULSE = 4.5f;
        private static final float MIN_COLLISION_RESPONSE_SPEED = 1.1f;
        private static final float COLLISION_FORCE_FACTOR = 4.8f;
        private static final float MAX_COLLISION_FORCE = 74f;
        private static final float COLLISION_FORWARD_TRANSFER = 1f;
        private static final float COLLISION_SIDEWAYS_TRANSFER = 0.44f;
        private static final float MAX_STORED_COLLISION_FORCE = 96f;
        private static final float IMPACT_FORCE_DURATION = 0.18f;
        private static final float IMPACT_SLIDE_DURATION = 0.34f;
        private static final float IMPACT_SLIDE_REFERENCE = 26f;

        private final CarTemplate template;
        private final String name;
        private final boolean playerControlled;
        private final Color color;
        private final CarAiController aiController;
        private final Vector2 forwardAxis = new Vector2();
        private final Vector2 sidewaysAxis = new Vector2();
        private final Vector2 working = new Vector2();
        private final Vector2 pendingImpactForce = new Vector2();

        private Body body;
        private boolean active = true;
        private boolean growthBoosted;
        private boolean pendingElimination;
        private float sizeScale = 1f;
        private float impactForceTimer;
        private float impactSlideTimer;
        private float impactSlideStrength;

        private Car(Body body, CarTemplate template) {
            this.body = body;
            this.template = template;
            name = template.name;
            playerControlled = template.playerControlled;
            color = template.color;
            aiController = playerControlled ? null : new CarAiController(template.personality);
            rebuildCollisionFixture();
        }

        private void step(
                float delta,
                ArenaMap arenaMap,
                Array<Car> cars,
                boolean allowControl,
                float playerThrottle,
                float playerTurn,
                boolean growthPickupActive,
                Vector2 growthPickupPosition) {
            if (!active || body == null) {
                return;
            }

            applyPendingImpactForce(delta);
            float impactSlideFactor = advanceImpactSlide(delta);
            applyGrip(impactSlideFactor);

            if (!allowControl) {
                return;
            }

            if (playerControlled) {
                drive(playerThrottle, playerTurn);
                return;
            }

            AiControlDecision decision =
                    aiController.plan(delta, this, arenaMap, cars, growthPickupActive, growthPickupPosition);
            drive(decision.throttle, decision.turn);
        }

        private void updateAxes() {
            forwardAxis.set(body.getWorldVector(working.set(0f, 1f)));
            sidewaysAxis.set(body.getWorldVector(working.set(1f, 0f)));
        }

        private void applyPendingImpactForce(float delta) {
            if (impactForceTimer <= 0f || pendingImpactForce.isZero(0.0001f)) {
                return;
            }

            float forceScale = MathUtils.clamp(impactForceTimer / IMPACT_FORCE_DURATION, 0.35f, 1f);
            working.set(pendingImpactForce).scl(forceScale);
            body.applyForceToCenter(working, true);

            impactForceTimer = Math.max(0f, impactForceTimer - delta);
            if (impactForceTimer == 0f) {
                pendingImpactForce.setZero();
            }
        }

        private float advanceImpactSlide(float delta) {
            if (impactSlideTimer <= 0f) {
                return 0f;
            }

            float slideFactor = impactSlideStrength * (impactSlideTimer / IMPACT_SLIDE_DURATION);
            impactSlideTimer = Math.max(0f, impactSlideTimer - delta);
            if (impactSlideTimer == 0f) {
                impactSlideStrength = 0f;
            }
            return slideFactor;
        }

        private void applyGrip(float impactSlideFactor) {
            updateAxes();

            float gripMultiplier = MathUtils.clamp(1f - 0.62f * impactSlideFactor, 0.24f, 1f);
            float dragMultiplier = MathUtils.clamp(1f - 0.52f * impactSlideFactor, 0.30f, 1f);
            float longitudinalForceMultiplier = getMassMultiplier();

            float lateralSpeed = sidewaysAxis.dot(body.getLinearVelocity());
            working.set(sidewaysAxis).scl(-lateralSpeed * body.getMass() * LATERAL_GRIP * gripMultiplier);
            body.applyLinearImpulse(working, body.getWorldCenter(), true);

            body.applyAngularImpulse(
                    -body.getAngularVelocity() * body.getInertia() * ANGULAR_GRIP * gripMultiplier,
                    true);

            float forwardSpeed = forwardAxis.dot(body.getLinearVelocity());
            working.set(forwardAxis).scl(-forwardSpeed * FORWARD_DRAG * dragMultiplier * longitudinalForceMultiplier);
            body.applyForceToCenter(working, true);
        }

        private void drive(float throttle, float turn) {
            updateAxes();
            float signedForwardSpeed = forwardAxis.dot(body.getLinearVelocity());
            float longitudinalForceMultiplier = getMassMultiplier();

            float engineForce = 0f;
            if (throttle > 0f) {
                engineForce = throttle * DRIVE_FORCE * longitudinalForceMultiplier;
            } else if (throttle < 0f) {
                engineForce = throttle * REVERSE_FORCE * longitudinalForceMultiplier;
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

            if (growthBoosted) {
                steeringStrength = MathUtils.clamp(steeringStrength * 0.96f, 0.30f, 1.3f);
                turnTorque *= GROWTH_TURN_MULTIPLIER;
            }
            turnTorque *= getSteeringInertiaCompensation();

            float steeringDirection = 1f;
            if (signedForwardSpeed < -0.25f) {
                steeringDirection = -1f;
            } else if (signedForwardSpeed < 0.25f && throttle < -0.1f) {
                steeringDirection = -1f;
            }
            body.applyTorque(turn * steeringDirection * turnTorque * steeringStrength, true);

            float currentSpeed = body.getLinearVelocity().len();
            float maxSpeed = MAX_SPEED * (growthBoosted ? MAX_GROWTH_SPEED_MULTIPLIER : 1f);
            if (currentSpeed > maxSpeed) {
                working.set(body.getLinearVelocity()).scl(maxSpeed / currentSpeed);
                body.setLinearVelocity(working);
            }
        }

        private void absorbCollision(Vector2 normal, float direction, float impactStrength) {
            if (!active || body == null) {
                return;
            }

            float collisionForce = MathUtils.clamp(
                    (impactStrength - MIN_COLLISION_RESPONSE_IMPULSE) * COLLISION_FORCE_FACTOR,
                    0f,
                    MAX_COLLISION_FORCE);
            if (collisionForce <= 0f) {
                return;
            }

            updateAxes();

            float forwardShare = forwardAxis.dot(normal) * direction;
            float sidewaysShare = sidewaysAxis.dot(normal) * direction;
            working.set(forwardAxis).scl(forwardShare * collisionForce * COLLISION_FORWARD_TRANSFER);
            working.mulAdd(sidewaysAxis, sidewaysShare * collisionForce * COLLISION_SIDEWAYS_TRANSFER);
            pendingImpactForce.add(working);
            if (pendingImpactForce.len2() > MAX_STORED_COLLISION_FORCE * MAX_STORED_COLLISION_FORCE) {
                pendingImpactForce.setLength(MAX_STORED_COLLISION_FORCE);
            }
            impactForceTimer = Math.max(impactForceTimer, IMPACT_FORCE_DURATION);

            impactSlideStrength = Math.max(
                    impactSlideStrength,
                    MathUtils.clamp(Math.abs(sidewaysShare) * impactStrength / IMPACT_SLIDE_REFERENCE, 0f, 1f));
            impactSlideTimer = Math.max(impactSlideTimer, IMPACT_SLIDE_DURATION);
        }

        private void setGrowthBoost(boolean growthBoosted) {
            this.growthBoosted = growthBoosted;
            sizeScale = growthBoosted ? GROWTH_SCALE : 1f;
            if (body != null) {
                rebuildCollisionFixture();
            }
        }

        private void rebuildCollisionFixture() {
            if (body == null) {
                return;
            }

            Array<Fixture> fixtures = body.getFixtureList();
            while (fixtures.size > 0) {
                body.destroyFixture(fixtures.first());
            }

            PolygonShape shape = new PolygonShape();
            shape.setAsBox(HALF_WIDTH * sizeScale, HALF_HEIGHT * sizeScale);

            FixtureDef fixtureDef = new FixtureDef();
            fixtureDef.shape = shape;
            fixtureDef.density = FIXTURE_DENSITY * getMassMultiplier() / (sizeScale * sizeScale);
            fixtureDef.friction = FIXTURE_FRICTION;
            fixtureDef.restitution = FIXTURE_RESTITUTION;
            body.createFixture(fixtureDef);
            shape.dispose();
            body.resetMassData();
        }

        private float getMassMultiplier() {
            return growthBoosted ? GROWTH_MASS_MULTIPLIER : 1f;
        }

        private float getSteeringInertiaCompensation() {
            return getMassMultiplier() * sizeScale * sizeScale;
        }

        private float getWidth() {
            return WIDTH * sizeScale;
        }

        private float getHeight() {
            return HEIGHT * sizeScale;
        }

        private float getSizeScale() {
            return sizeScale;
        }

        private float getGrowthPickupReach() {
            return Math.max(getWidth(), getHeight()) * 0.42f;
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

        @Override
        public boolean hasGrowthBoost() {
            return growthBoosted;
        }

        private void eliminate(World world) {
            if (!active || body == null) {
                return;
            }

            active = false;
            growthBoosted = false;
            pendingElimination = false;
            sizeScale = 1f;
            pendingImpactForce.setZero();
            impactForceTimer = 0f;
            impactSlideTimer = 0f;
            impactSlideStrength = 0f;
            world.destroyBody(body);
            body = null;
        }
    }

    private static final class CarTemplate {
        private final String name;
        private final boolean playerControlled;
        private final Color color;
        private final AiDrivingPersonality personality;
        private int totalPoints;
        private int roundFinishPosition;
        private int lastRoundAwardedPoints;
        private int roundPickupPoints;
        private Car currentCar;

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

    private static final class DestructionEffect {
        private final Vector2 position = new Vector2();
        private final Color color = new Color();
        private float timer;
        private float rotationDeg;
        private float scale;
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
