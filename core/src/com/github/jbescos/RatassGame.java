package com.github.jbescos;

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

public class RatassGame extends ApplicationAdapter {
    private static final float WORLD_WIDTH = 32f;
    private static final float WORLD_HEIGHT = 18f;
    private static final float ARENA_WIDTH = 22f;
    private static final float ARENA_HEIGHT = 12f;
    private static final float EDGE_FALLOFF_MARGIN = 0.35f;
    private static final float PHYSICS_STEP = 1f / 60f;
    private static final int VELOCITY_ITERATIONS = 6;
    private static final int POSITION_ITERATIONS = 2;

    private static final Color SKYLINE = new Color(0.05f, 0.07f, 0.09f, 1f);
    private static final Color VOID = new Color(0.08f, 0.10f, 0.13f, 1f);
    private static final Color PLATFORM_SHADOW = new Color(0.04f, 0.05f, 0.07f, 1f);
    private static final Color PLATFORM_EDGE = new Color(0.38f, 0.34f, 0.24f, 1f);
    private static final Color PLATFORM_SURFACE = new Color(0.78f, 0.72f, 0.54f, 1f);
    private static final Color PLATFORM_CENTER = new Color(0.69f, 0.63f, 0.46f, 1f);
    private static final Color WARNING_BRIGHT = new Color(0.95f, 0.78f, 0.18f, 1f);
    private static final Color WARNING_DARK = new Color(0.14f, 0.14f, 0.15f, 1f);

    private final Rectangle arenaBounds =
            new Rectangle(-ARENA_WIDTH * 0.5f, -ARENA_HEIGHT * 0.5f, ARENA_WIDTH, ARENA_HEIGHT);
    private final Array<Car> cars = new Array<Car>();
    private final GlyphLayout glyphLayout = new GlyphLayout();
    private final Color tint = new Color();
    private final Vector2 arenaCenter = new Vector2();

    private OrthographicCamera worldCamera;
    private FitViewport worldViewport;
    private OrthographicCamera hudCamera;
    private ScreenViewport hudViewport;
    private ShapeRenderer shapeRenderer;
    private SpriteBatch spriteBatch;
    private BitmapFont hudFont;
    private BitmapFont titleFont;
    private World world;

    private float accumulator;
    private boolean roundOver;
    private int roundNumber;
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

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        resetRound();
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

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.R)
                || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            resetRound();
        }

        update(delta);
        renderWorld();
        renderHud();
    }

    private void update(float delta) {
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
            car.step(delta, arenaBounds, arenaCenter, cars, !roundOver);
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
            boolean outOfBounds = position.x < arenaBounds.x - EDGE_FALLOFF_MARGIN
                    || position.x > arenaBounds.x + arenaBounds.width + EDGE_FALLOFF_MARGIN
                    || position.y < arenaBounds.y - EDGE_FALLOFF_MARGIN
                    || position.y > arenaBounds.y + arenaBounds.height + EDGE_FALLOFF_MARGIN;

            if (outOfBounds) {
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
        winner = lastAlive;

        for (int i = 0; i < cars.size; i++) {
            Car car = cars.get(i);
            if (car.body != null) {
                car.body.setLinearVelocity(0f, 0f);
                car.body.setAngularVelocity(0f);
            }
        }
    }

    private void resetRound() {
        if (world != null) {
            world.dispose();
        }

        world = new World(new Vector2(0f, 0f), true);
        cars.clear();
        accumulator = 0f;
        roundOver = false;
        winner = null;
        playerCar = null;
        roundNumber++;

        playerCar = createCar(
                "You",
                true,
                new Color(0.90f, 0.25f, 0.20f, 1f),
                0f,
                -3.8f,
                facingAngle(0f, -3.8f, 0f, 0f));

        createCar(
                "Cinder",
                false,
                new Color(0.92f, 0.54f, 0.13f, 1f),
                -6.4f,
                3.4f,
                facingAngle(-6.4f, 3.4f, 0f, 0f));

        createCar(
                "Frost",
                false,
                new Color(0.16f, 0.57f, 0.84f, 1f),
                6.2f,
                3.0f,
                facingAngle(6.2f, 3.0f, 0f, 0f));

        createCar(
                "Moss",
                false,
                new Color(0.24f, 0.70f, 0.42f, 1f),
                0f,
                4.4f,
                facingAngle(0f, 4.4f, 0f, 0f));
    }

    private Car createCar(
            String name,
            boolean playerControlled,
            Color color,
            float x,
            float y,
            float angleRad) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(x, y);
        bodyDef.angle = angleRad;
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

        Car car = new Car(body, name, playerControlled, color);
        body.setUserData(car);
        cars.add(car);
        return car;
    }

    private float facingAngle(float fromX, float fromY, float toX, float toY) {
        return MathUtils.atan2(toY - fromY, toX - fromX) - MathUtils.HALF_PI;
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
        float edgeInset = 0.7f;
        float centerInset = 1.2f;

        shapeRenderer.setColor(PLATFORM_SHADOW);
        shapeRenderer.rect(
                arenaBounds.x + 0.18f,
                arenaBounds.y - 0.22f,
                arenaBounds.width,
                arenaBounds.height);

        shapeRenderer.setColor(PLATFORM_EDGE);
        shapeRenderer.rect(arenaBounds.x - edgeInset, arenaBounds.y - edgeInset,
                arenaBounds.width + edgeInset * 2f, arenaBounds.height + edgeInset * 2f);

        drawWarningStripes();

        shapeRenderer.setColor(PLATFORM_SURFACE);
        shapeRenderer.rect(arenaBounds.x, arenaBounds.y, arenaBounds.width, arenaBounds.height);

        shapeRenderer.setColor(PLATFORM_CENTER);
        shapeRenderer.rect(
                arenaBounds.x + centerInset,
                arenaBounds.y + centerInset,
                arenaBounds.width - centerInset * 2f,
                arenaBounds.height - centerInset * 2f);

        shapeRenderer.setColor(0.61f, 0.56f, 0.40f, 1f);
        shapeRenderer.circle(0f, 0f, 1.7f, 28);

        shapeRenderer.setColor(0.57f, 0.52f, 0.37f, 1f);
        shapeRenderer.circle(0f, 0f, 0.8f, 20);
    }

    private void drawWarningStripes() {
        float stripeDepth = 0.55f;
        float stripeLength = 1.25f;
        boolean bright = true;

        for (float x = arenaBounds.x - 0.7f; x < arenaBounds.x + arenaBounds.width + 0.7f;
                x += stripeLength) {
            shapeRenderer.setColor(bright ? WARNING_BRIGHT : WARNING_DARK);
            shapeRenderer.rect(x, arenaBounds.y + arenaBounds.height, stripeLength, stripeDepth);
            shapeRenderer.rect(x, arenaBounds.y - stripeDepth, stripeLength, stripeDepth);
            bright = !bright;
        }

        bright = false;
        for (float y = arenaBounds.y - 0.7f; y < arenaBounds.y + arenaBounds.height + 0.7f;
                y += stripeLength) {
            shapeRenderer.setColor(bright ? WARNING_BRIGHT : WARNING_DARK);
            shapeRenderer.rect(arenaBounds.x - stripeDepth, y, stripeDepth, stripeLength);
            shapeRenderer.rect(arenaBounds.x + arenaBounds.width, y, stripeDepth, stripeLength);
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

            drawRotatedRect(centerX + 0.10f, centerY - 0.12f,
                    Car.WIDTH, Car.HEIGHT, angleDeg, 0.04f, 0.05f, 0.07f, 0.32f);

            if (car.playerControlled) {
                drawRotatedRect(centerX, centerY,
                        Car.WIDTH + 0.20f, Car.HEIGHT + 0.20f, angleDeg,
                        0.96f, 0.94f, 0.88f, 0.26f);
            }

            drawRotatedRect(centerX, centerY,
                    Car.WIDTH, Car.HEIGHT, angleDeg,
                    car.color.r, car.color.g, car.color.b, 1f);

            tint.set(car.color).lerp(Color.WHITE, 0.18f);
            drawOffsetRotatedRect(centerX, centerY, 0f, -0.08f,
                    Car.WIDTH * 0.68f, Car.HEIGHT * 0.50f, angleDeg,
                    tint.r, tint.g, tint.b, 1f);

            drawOffsetRotatedRect(centerX, centerY, 0f, 0.40f,
                    Car.WIDTH * 0.62f, Car.HEIGHT * 0.16f, angleDeg,
                    0.12f, 0.13f, 0.15f, 1f);

            drawOffsetRotatedRect(centerX, centerY, 0f, 0.84f,
                    Car.WIDTH * 0.55f, Car.HEIGHT * 0.09f, angleDeg,
                    0.96f, 0.92f, 0.74f, 1f);
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
                "WASD or Arrow Keys: drive and steer   R / Enter: restart   Esc: quit",
                22f,
                hudHeight - 44f);

        String status = buildStatusText();
        glyphLayout.setText(hudFont, status);
        hudFont.draw(spriteBatch, status, hudWidth - glyphLayout.width - 22f, hudHeight - 20f);

        hudFont.setColor(0.93f, 0.84f, 0.49f, 1f);
        hudFont.draw(
                spriteBatch,
                "Last car standing wins. Push rivals clean off the platform.",
                22f,
                30f);

        if (roundOver || !playerCar.active) {
            String headline = buildHeadline();
            String subline = roundOver
                    ? "Press Enter or R to throw another round."
                    : "You are out. Watch the chaos or restart.";

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
    }

    private String buildStatusText() {
        int aliveCars = 0;
        for (int i = 0; i < cars.size; i++) {
            Car car = cars.get(i);
            if (car.active) {
                aliveCars++;
            }
        }

        return "Round " + roundNumber + "   |   Cars left: " + aliveCars + "/" + cars.size;
    }

    private String buildHeadline() {
        if (!roundOver) {
            return "OUT";
        }

        if (winner == null) {
            return "DRAW";
        }

        if (winner.playerControlled) {
            return "YOU WIN";
        }

        return winner.name.toUpperCase() + " WINS";
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

    private static final class Car {
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
        private static final float AI_RECOVERY_EDGE = 1.8f;
        private static final float AI_CAUTION_EDGE = 2.7f;
        private static final float AI_TARGET_LEAD_MAX = 0.45f;
        private static final float AI_STAGING_DISTANCE = 1.85f;

        private final String name;
        private final boolean playerControlled;
        private final Color color;
        private final Vector2 forwardAxis = new Vector2();
        private final Vector2 sidewaysAxis = new Vector2();
        private final Vector2 desiredVector = new Vector2();
        private final Vector2 centerBias = new Vector2();
        private final Vector2 working = new Vector2();
        private final Vector2 targetLead = new Vector2();
        private final Vector2 attackVector = new Vector2();
        private final Vector2 interceptPoint = new Vector2();

        private Body body;
        private boolean active = true;
        private float stuckTimer;

        private Car(Body body, String name, boolean playerControlled, Color color) {
            this.body = body;
            this.name = name;
            this.playerControlled = playerControlled;
            this.color = color;
        }

        private void step(
                float delta,
                Rectangle arenaBounds,
                Vector2 arenaCenter,
                Array<Car> cars,
                boolean allowControl) {
            if (!active || body == null) {
                return;
            }

            applyGrip();

            if (!allowControl) {
                return;
            }

            if (playerControlled) {
                drive(getPlayerThrottle(), getPlayerTurn());
                return;
            }

            driveAi(delta, arenaBounds, arenaCenter, cars);
        }

        private void driveAi(
                float delta,
                Rectangle arenaBounds,
                Vector2 arenaCenter,
                Array<Car> cars) {
            Car target = findTarget(cars, arenaBounds, arenaCenter);
            if (target == null || target.body == null) {
                drive(0f, 0f);
                return;
            }

            Vector2 position = body.getPosition();
            float nearestEdge = distanceToNearestEdge(position, arenaBounds);
            float outwardVelocity = 0f;

            centerBias.set(position).sub(arenaCenter);
            if (!centerBias.isZero(0.001f)) {
                centerBias.nor();
                outwardVelocity = centerBias.dot(body.getLinearVelocity());
            }

            boolean recovering =
                    nearestEdge < AI_RECOVERY_EDGE
                            || (nearestEdge < AI_CAUTION_EDGE && outwardVelocity > 1.15f);

            if (recovering) {
                desiredVector.set(arenaCenter).sub(position);
                if (desiredVector.isZero(0.001f)) {
                    desiredVector.set(0f, 1f);
                }
            } else {
                Vector2 targetPosition = target.body.getPosition();
                float leadTime = MathUtils.clamp(
                        position.dst(targetPosition) / 10f,
                        0.14f,
                        AI_TARGET_LEAD_MAX);
                interceptPoint.set(targetPosition).mulAdd(target.body.getLinearVelocity(), leadTime);
                clampInsideArena(interceptPoint, arenaBounds, 0.7f);

                attackVector.set(targetPosition).sub(arenaCenter);
                if (attackVector.isZero(0.001f)) {
                    attackVector.set(interceptPoint).sub(position);
                }
                if (attackVector.isZero(0.001f)) {
                    attackVector.set(0f, 1f);
                }
                attackVector.nor();

                targetLead.set(interceptPoint).sub(position);
                float pushAlignment = 0f;
                if (!targetLead.isZero(0.001f)) {
                    targetLead.nor();
                    pushAlignment = targetLead.dot(attackVector);
                }

                float targetEdge = distanceToNearestEdge(targetPosition, arenaBounds);
                boolean commitPush =
                        targetEdge < AI_RECOVERY_EDGE
                                || (pushAlignment > 0.42f && position.dst2(targetPosition) < 20f);

                if (commitPush) {
                    desiredVector.set(interceptPoint).mulAdd(attackVector, 1.15f).sub(position);
                } else {
                    float stagingDistance = MathUtils.clamp(
                            AI_STAGING_DISTANCE + target.body.getLinearVelocity().len() * 0.18f,
                            1.45f,
                            2.55f);
                    desiredVector.set(interceptPoint).mulAdd(attackVector, -stagingDistance).sub(position);

                    if (targetEdge > 3.4f && nearestEdge > 3f) {
                        centerBias.set(arenaCenter).sub(interceptPoint);
                        if (!centerBias.isZero(0.001f)) {
                            centerBias.nor().scl(0.75f);
                            desiredVector.add(centerBias);
                        }
                    }
                }
            }

            float currentHeading = body.getAngle() + MathUtils.HALF_PI;
            float desiredHeading = desiredVector.angleRad();
            float angleError = desiredHeading - currentHeading;
            angleError = MathUtils.atan2(MathUtils.sin(angleError), MathUtils.cos(angleError));

            float turn = MathUtils.clamp(angleError * (recovering ? 3.8f : 3.1f), -1f, 1f);
            float throttle;

            if (recovering) {
                if (Math.abs(angleError) > 1.35f) {
                    throttle = -0.58f;
                } else if (nearestEdge < 1.1f) {
                    throttle = 1f;
                } else {
                    throttle = 0.85f;
                }
            } else if (Math.abs(angleError) > 1.55f) {
                throttle = -0.42f;
            } else if (Math.abs(angleError) > 0.85f) {
                throttle = 0.55f;
            } else if (desiredVector.len2() < 2f) {
                throttle = 0.78f;
            } else {
                throttle = 1f;
            }

            if (body.getLinearVelocity().len2() < 1f) {
                stuckTimer += delta;
            } else {
                stuckTimer = 0f;
            }

            if (stuckTimer > 0.65f) {
                throttle = -0.72f;
                turn = turn >= 0f ? 1f : -1f;
                if (stuckTimer > 1.1f) {
                    stuckTimer = 0f;
                }
            }

            drive(throttle, turn);
        }

        private float getPlayerThrottle() {
            float throttle = 0f;
            if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
                throttle += 1f;
            }
            if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
                throttle -= 1f;
            }
            return throttle;
        }

        private float getPlayerTurn() {
            float turn = 0f;
            if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
                turn += 1f;
            }
            if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
                turn -= 1f;
            }
            return turn;
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

        private Car findTarget(Array<Car> cars, Rectangle arenaBounds, Vector2 arenaCenter) {
            Car best = null;
            float bestScore = -Float.MAX_VALUE;
            Vector2 myPosition = body.getPosition();
            forwardAxis.set(body.getWorldVector(working.set(0f, 1f)));

            for (int i = 0; i < cars.size; i++) {
                Car candidate = cars.get(i);
                if (candidate == this || !candidate.active || candidate.body == null) {
                    continue;
                }

                Vector2 candidatePosition = candidate.body.getPosition();
                float distance = myPosition.dst(candidatePosition);
                float edgeThreat = 4.8f - distanceToNearestEdge(candidatePosition, arenaBounds);

                targetLead.set(candidatePosition).sub(arenaCenter);
                float centerDistanceScore = targetLead.len() * 0.28f;

                targetLead.set(candidatePosition).sub(myPosition);
                float approachAlignment = 0f;
                if (!targetLead.isZero(0.001f)) {
                    targetLead.nor();
                    approachAlignment = Math.max(0f, forwardAxis.dot(targetLead));
                }

                float score =
                        edgeThreat * 2.3f
                                + centerDistanceScore
                                + approachAlignment * 1.8f
                                - distance * 0.24f;

                if (candidate.playerControlled) {
                    score += 0.45f;
                }

                if (score > bestScore) {
                    bestScore = score;
                    best = candidate;
                }
            }

            return best;
        }

        private float distanceToNearestEdge(Vector2 position, Rectangle arenaBounds) {
            float leftGap = position.x - arenaBounds.x;
            float rightGap = arenaBounds.x + arenaBounds.width - position.x;
            float bottomGap = position.y - arenaBounds.y;
            float topGap = arenaBounds.y + arenaBounds.height - position.y;
            return Math.min(Math.min(leftGap, rightGap), Math.min(bottomGap, topGap));
        }

        private void clampInsideArena(Vector2 point, Rectangle arenaBounds, float margin) {
            point.x = MathUtils.clamp(
                    point.x,
                    arenaBounds.x + margin,
                    arenaBounds.x + arenaBounds.width - margin);
            point.y = MathUtils.clamp(
                    point.y,
                    arenaBounds.y + margin,
                    arenaBounds.y + arenaBounds.height - margin);
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
}
