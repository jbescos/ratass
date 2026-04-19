package com.github.jbescos;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
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
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.github.jbescos.ai.AiControlDecision;
import com.github.jbescos.ai.AiDrivingPersonality;
import com.github.jbescos.ai.AiDrivingPersonalities;
import com.github.jbescos.ai.AiVehicleView;
import com.github.jbescos.ai.CarAiController;
import com.github.jbescos.gameplay.ArenaMap;
import com.github.jbescos.gameplay.MapProgression;
import com.github.jbescos.gameplay.SpawnPoint;
import com.github.jbescos.gameplay.maps.ArenaMaps;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

public class RatassGame extends ApplicationAdapter {
    private static final String GAME_PROPERTIES_RESOURCE = "game.properties";
    private static final String GAME_PROPERTIES_FILE = "assets/game.properties";
    private static final String THEME_PROPERTY = "theme";
    private static final String DEFAULT_THEME_NAME = "infernal";
    private static final String CAMERA_FOLLOW_BEHIND_PROPERTY = "camera.follow.behind";
    private static final String THEME_DIRECTORY = "theme";
    private static final String THEME_CAR_SHEET_PATH = "cars/cars.png";
    private static final String THEME_ENEMY_NAMES_PATH = "enemy-names.txt";
    private static final int THEME_CAR_SHEET_COLUMNS = 12;
    private static final int THEME_CAR_SHEET_ROWS = 5;
    private static final int THEME_CAR_TEXTURE_PADDING = 2;
    private static final int BACKGROUND_ALPHA_CUTOUT_THRESHOLD = 5;
    private static final int BACKGROUND_ALPHA_OPAQUE_THRESHOLD = 16;
    private static final float ARENA_PIXEL_ART_PIXELS_PER_WORLD_UNIT = 12f;
    private static final int ARENA_PIXEL_ART_MIN_TEXTURE_SIZE = 128;
    private static final int ARENA_PIXEL_ART_MAX_TEXTURE_SIZE = 512;
    private static final float ARENA_PIXEL_ART_DECOR_GRID = 6f;
    private static final float WORLD_WIDTH = 32f;
    private static final float WORLD_HEIGHT = 18f;
    private static final float EDGE_FALLOFF_MARGIN = 0.35f;
    private static final float PHYSICS_STEP = 1f / 60f;
    private static final int VELOCITY_ITERATIONS = 6;
    private static final int POSITION_ITERATIONS = 2;
    private static final float ARENA_EDGE_INSET = 0.7f;
    private static final float ARENA_CENTER_INSET = 1.15f;
    private static final float AUTO_ADVANCE_DELAY = 2f;
    private static final float GROWTH_PICKUP_RADIUS = 0.42f;
    private static final float GROWTH_PICKUP_SPAWN_MARGIN = 1.05f;
    private static final float GROWTH_PICKUP_MIN_MOVE_DISTANCE = 3.4f;
    private static final int GROWTH_PICKUP_SPAWN_ATTEMPTS = 96;
    private static final float POINT_PICKUP_RADIUS = 0.34f;
    private static final float POINT_PICKUP_SPAWN_MARGIN = 0.92f;
    private static final float POINT_PICKUP_MIN_MOVE_DISTANCE = 2.6f;
    private static final int POINT_PICKUP_SPAWN_ATTEMPTS = 96;
    private static final float PICKUP_MIN_SEPARATION = 1.95f;
    private static final float GROWTH_DURATION = 10f;
    private static final float RAM_CHARGE_DURATION = 8f;
    private static final float DESTRUCTION_EFFECT_DURATION = 0.65f;
    private static final float ROUND_START_COUNTDOWN = 3f;
    private static final float ROUND_TIME_LIMIT = 20f;
    private static final float ROUND_TIMEOUT_LIMIT = 35f;
    private static final float CAMERA_HORIZONTAL_PADDING = 4f;
    private static final float CAMERA_VERTICAL_PADDING = 3f;
    private static final float MIN_WORLD_CAMERA_ZOOM = 0.90f;
    private static final float PLAYER_CAMERA_ZOOM = 1.04f;
    private static final float PLAYER_CAMERA_SPEED_ZOOM_OUT = 0.46f;
    private static final float PLAYER_CAMERA_GROWTH_ZOOM_OUT = 0.14f;
    private static final float PLAYER_CAMERA_MAX_ZOOM = 1.58f;
    private static final float PLAYER_CAMERA_FOLLOW_LEAD_DISTANCE = 0.10f;
    private static final float PLAYER_CAMERA_FOLLOW_LEAD_SPEED_BONUS = 0.28f;
    private static final float PLAYER_CAMERA_DIRECTION_LERP_SPEED = 3.8f;
    private static final float PLAYER_CAMERA_DIRECTION_MIN_SPEED = 2.2f;
    private static final float PLAYER_CAMERA_DIRECTION_SPIN_DAMPING = 0.22f;
    private static final float PLAYER_CAMERA_DIRECTION_IMPACT_DAMPING = 0.48f;
    private static final float PLAYER_CAMERA_DIRECTION_RETURN_SPEED = 1.9f;
    private static final float PLAYER_CAMERA_LOOK_AHEAD_TIME = 0.15f;
    private static final float PLAYER_CAMERA_MAX_LOOK_AHEAD = 2.35f;
    private static final float PLAYER_CAMERA_FOLLOW_LERP_SPEED = 7.5f;
    private static final int ROUND_SPAWN_ATTEMPTS = 3200;
    private static final float ROUND_SPAWN_SAFE_MARGIN = 1.15f;
    private static final float ROUND_SPAWN_MIN_DISTANCE = 1.95f;
    private static final float EVENT_CALLOUT_DURATION = 1.35f;
    private static final float SUDDEN_DEATH_TIE_SPEED_MARGIN = 0.08f;
    private static final float HUD_SIDEBAR_RATIO = 0.29f;
    private static final float HUD_SIDEBAR_MIN_WIDTH = 200f;
    private static final float HUD_SIDEBAR_PREFERRED_MIN_WIDTH = 260f;
    private static final float HUD_SIDEBAR_MAX_WIDTH = 420f;
    private static final float HUD_MIN_PLAYFIELD_WIDTH = 320f;
    private static final float SIDEBAR_CARD_MARGIN = 12f;
    private static final float SIDEBAR_CONTENT_MARGIN = 18f;
    private static final float SIDEBAR_MINIMAP_GAP = 10f;
    private static final float SIDEBAR_MINIMAP_MIN_HEIGHT = 64f;
    private static final float SIDEBAR_MINIMAP_LABEL_HEIGHT = 18f;
    private static final float SIDEBAR_MINIMAP_PADDING = 12f;
    private static final float SIDEBAR_MINIMAP_MARKER_RADIUS = 3.2f;
    private static final float SIDEBAR_MINIMAP_PLAYER_RING_RADIUS = 6.2f;
    private static final float SIDEBAR_MINIMAP_BIG_CAR_RADIUS_BOOST = 1.4f;
    private static final float SIDEBAR_LEADERBOARD_COLUMN_GAP = 10f;
    private static final float SIDEBAR_LEADERBOARD_COMPACT_ROW_STEP = 11.5f;
    private static final float SIDEBAR_LEADERBOARD_POINTS_MIN_WIDTH = 112f;
    private static final int SIDEBAR_LEADERBOARD_MAX_COLUMNS = 8;
    private static final float HUD_FONT_SCALE = 1.18f;
    private static final float TITLE_FONT_SCALE = 2.25f;
    private static final float LEADERBOARD_FONT_SCALE = 0.96f;
    private static final float LABEL_FONT_SCALE = 0.86f;
    private static final float CAR_SPRITE_WIDTH_SCALE = 1.16f;
    private static final float CAR_SPRITE_HEIGHT_SCALE = 1.14f;
    private static final float CAR_SPRITE_ROTATION_OFFSET_DEG = 180f;
    private static final float IMPACT_SOUND_COOLDOWN = 0.06f;
    private static final float DESTRUCTION_SOUND_COOLDOWN = 0.08f;
    private static final String[] DEFAULT_ENEMY_NAMES = new String[] {
            "Brim",
            "Cinderskull",
            "Hellion",
            "Malice",
            "Ashfang",
            "Dreadcoil",
            "Embermaw",
            "Ruin",
            "Hex",
            "Scorch",
            "Nightfire",
            "Ravager",
            "Obsidian",
            "Vex",
            "Doomspark",
            "Sableclaw",
            "Torment",
            "Pyre",
            "Wrath"
    };

    private static final AiDrivingPersonality[] ENEMY_PERSONALITIES =
            new AiDrivingPersonality[] {
                    AiDrivingPersonalities.BRAWLER,
                    AiDrivingPersonalities.INTERCEPTOR,
                    AiDrivingPersonalities.SURVIVOR,
                    AiDrivingPersonalities.BALANCED
            };

    private static final Color VOID = new Color(0.08f, 0.10f, 0.13f, 1f);
    private static final Color PLATFORM_SHADOW = new Color(0.04f, 0.05f, 0.07f, 1f);
    private static final Color WARNING_BRIGHT = new Color(0.95f, 0.78f, 0.18f, 1f);
    private static final MapTheme DEFAULT_MAP_THEME =
            mapTheme(
                    MapDecorStyle.GRID,
                    "081018",
                    "10232d",
                    "1d4b59",
                    "173642",
                    "6b8790",
                    "4f6972",
                    "efbf57",
                    "8ecad4");
    private static final MapTheme CROSSWIND_THEME =
            mapTheme(
                    MapDecorStyle.CROSS,
                    "06141c",
                    "12303c",
                    "2d7a8d",
                    "183742",
                    "74a1ad",
                    "52717d",
                    "efc565",
                    "8bdbe7");
    private static final MapTheme SPLIT_SHIFT_THEME =
            mapTheme(
                    MapDecorStyle.DIAGONAL,
                    "120d11",
                    "33212a",
                    "9e5037",
                    "3a2327",
                    "88707a",
                    "66525c",
                    "f08b42",
                    "d7b199");
    private static final MapTheme DONUT_BOWL_THEME =
            mapTheme(
                    MapDecorStyle.ORBIT,
                    "140f0b",
                    "342417",
                    "94552d",
                    "4a3222",
                    "a48462",
                    "7d6448",
                    "f5bf5c",
                    "ddb585");
    private static final MapTheme TWIN_CRATER_THEME =
            mapTheme(
                    MapDecorStyle.ORBIT,
                    "0d0f13",
                    "1f222d",
                    "7f3a33",
                    "2d313d",
                    "7c7e89",
                    "616572",
                    "e46a4b",
                    "a1a8bc");
    private static final MapTheme FRAME_RING_THEME =
            mapTheme(
                    MapDecorStyle.GRID,
                    "07110d",
                    "10261f",
                    "2a8b6a",
                    "183a31",
                    "6f9d88",
                    "50725f",
                    "e9d36d",
                    "8ad5bb");
    private static final MapTheme CAUSEWAY_THEME =
            mapTheme(
                    MapDecorStyle.RUNWAY,
                    "07111a",
                    "12273a",
                    "2e6285",
                    "163447",
                    "5f7e99",
                    "456071",
                    "e6b05a",
                    "8ab5d5");
    private static final MapTheme CORE_BREACH_THEME =
            mapTheme(
                    MapDecorStyle.CROSS,
                    "12080a",
                    "351519",
                    "a63228",
                    "431d23",
                    "8d6461",
                    "6a4a49",
                    "f05d3a",
                    "e4a591");
    private static final MapTheme PILLBOX_THEME =
            mapTheme(
                    MapDecorStyle.RUNWAY,
                    "081316",
                    "133039",
                    "188791",
                    "163940",
                    "79a4a4",
                    "5a7d7d",
                    "f4df83",
                    "9ae2d8");
    private static final MapTheme SATELLITE_THEME =
            mapTheme(
                    MapDecorStyle.ORBIT,
                    "07101c",
                    "0f2642",
                    "376ab6",
                    "18355a",
                    "6d88bf",
                    "4d6390",
                    "f5c85e",
                    "98baf3");
    private static final MapTheme KNIFE_EDGE_THEME =
            mapTheme(
                    MapDecorStyle.RUNWAY,
                    "0f1114",
                    "2a2f35",
                    "b24a37",
                    "3c4348",
                    "a49b8d",
                    "7c7569",
                    "f06b4c",
                    "d9d3cc");
    private static final MapTheme DEADFALL_THEME =
            mapTheme(
                    MapDecorStyle.ORBIT,
                    "130d0a",
                    "362314",
                    "8a5c2c",
                    "4a2f1e",
                    "a88662",
                    "7d6247",
                    "eea550",
                    "d7c1a2");
    private static final MapTheme SWITCHBACK_THEME =
            mapTheme(
                    MapDecorStyle.DIAGONAL,
                    "120c0b",
                    "321e19",
                    "a24b31",
                    "452922",
                    "9c7a66",
                    "745848",
                    "f0a064",
                    "dbaf96");
    private static final MapTheme LAST_STAND_THEME =
            mapTheme(
                    MapDecorStyle.FORTRESS,
                    "0a110d",
                    "183223",
                    "46663a",
                    "224231",
                    "7d9364",
                    "5b6d47",
                    "e5bf5f",
                    "a9c986");
    private static final MapTheme BOILER_DECK_THEME =
            mapTheme(
                    MapDecorStyle.RUNWAY,
                    "110c0b",
                    "30201a",
                    "a5662d",
                    "462d24",
                    "a28063",
                    "7d604b",
                    "f0a352",
                    "d8b89b");

    // Fallback visuals for themes that provide individual car files instead of a sheet.
    private static final CarVisual[] NORMAL_CAR_VISUALS = new CarVisual[] {
            new CarVisual("cars/car11.png", Color.valueOf("f42934")),
            new CarVisual("cars/car01.png", Color.valueOf("fde14b")),
            new CarVisual("cars/car02.png", Color.valueOf("f2ecea")),
            new CarVisual("cars/car03.png", Color.valueOf("fcfafa")),
            new CarVisual("cars/car04.png", Color.valueOf("f6732a")),
            new CarVisual("cars/car05.png", Color.valueOf("72ab33")),
            new CarVisual("cars/car06.png", Color.valueOf("86d9eb")),
            new CarVisual("cars/car07.png", Color.valueOf("fa64a9")),
            new CarVisual("cars/car08.png", Color.valueOf("624d5b")),
            new CarVisual("cars/car09.png", Color.valueOf("fcfaf8")),
            new CarVisual("cars/car10.png", Color.valueOf("2b3741")),
            new CarVisual("cars/car12.png", Color.valueOf("2ca6f5")),
            new CarVisual("cars/car13.png", Color.valueOf("cba268")),
            new CarVisual("cars/car14.png", Color.valueOf("f9f5f4")),
            new CarVisual("cars/car15.png", Color.valueOf("fcfbfb")),
            new CarVisual("cars/car16.png", Color.valueOf("682a46")),
            new CarVisual("cars/car17.png", Color.valueOf("f3ebe4")),
            new CarVisual("cars/car18.png", Color.valueOf("f46925")),
            new CarVisual("cars/car20.png", Color.valueOf("87c538")),
            new CarVisual("cars/car33.png", Color.valueOf("363c49"))
    };
    private final Array<Car> cars = new Array<Car>();
    private final Array<CarTemplate> roster = new Array<CarTemplate>();
    private final Array<CarTemplate> leaderboardEntries = new Array<CarTemplate>();
    private final Array<Car> pendingCollisionEliminations = new Array<Car>();
    private final Array<DestructionEffect> destructionEffects = new Array<DestructionEffect>();
    private final Array<CarVisual> themeCarVisuals = new Array<CarVisual>();
    private final Array<String> themeEnemyNames = new Array<String>();
    private final GlyphLayout glyphLayout = new GlyphLayout();
    private final Color tint = new Color();
    private final Rectangle mapBounds = new Rectangle();
    private final Vector2 focusPoint = new Vector2();
    private final Vector2 growthPickupPosition = new Vector2();
    private final Vector2 pointPickupPosition = new Vector2();
    private final Vector2 pickupCandidate = new Vector2();
    private final Vector2 lastGrowthPickupPosition = new Vector2();
    private final Vector2 lastPointPickupPosition = new Vector2();
    private final Vector2 spawnCandidate = new Vector2();
    private final Array<SpawnPoint> roundSpawns = new Array<SpawnPoint>();
    private final Rectangle steerPadBounds = new Rectangle();
    private final Rectangle throttlePadBounds = new Rectangle();
    private final Rectangle reversePadBounds = new Rectangle();
    private final Rectangle restartButtonBounds = new Rectangle();
    private final Rectangle sidebarMinimapBounds = new Rectangle();
    private final Matrix4 minimapTransform = new Matrix4();
    private final Matrix4 hudTransform = new Matrix4();
    private final Vector2 cameraTargetPosition = new Vector2();
    private final Vector2 cameraSmoothedPosition = new Vector2();
    private final Vector2 cameraLookAhead = new Vector2();
    private final Vector2 cameraForwardDirection = new Vector2();
    private final Vector2 cameraSmoothedForwardDirection = new Vector2(0f, 1f);
    private final Vector2 cameraChaseOffset = new Vector2();
    private final Vector2 cameraRightDirection = new Vector2(1f, 0f);
    private final Color eventCalloutColor = new Color();
    private final Vector3 hudTouchPoint = new Vector3();
    private final Vector3 carLabelProjection = new Vector3();
    private final ImpactContactListener impactContactListener = new ImpactContactListener();
    private final String configuredThemeName = loadConfiguredThemeName();
    private final boolean followCameraBehind =
            loadConfiguredBooleanProperty(CAMERA_FOLLOW_BEHIND_PROPERTY, false);
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
    private Sound impactSound;
    private Sound pickupSound;
    private Sound destructionSound;
    private Sound countdownSound;
    private Sound roundStartSound;
    private Sound suddenDeathSound;
    private Sound timeoutSound;
    private World world;
    private Texture arenaSurfaceTexture;
    private Texture themeCarsTexture;

    private MapProgression mapProgression;
    private ArenaMap currentMap;
    private float accumulator;
    private float roundOverTimer;
    private float effectClock;
    private float frameThrottleInput;
    private float frameTurnInput;
    private boolean frameHandbrakeInput;
    private float preRoundCountdownTimer;
    private float roundTimer;
    private float touchThrottleInput;
    private float touchTurnInput;
    private float growthBoostTimer;
    private float playfieldHudWidth;
    private float sidebarHudWidth;
    private float impactSoundCooldown;
    private float destructionSoundCooldown;
    private float smoothedCameraZoom = 1f;
    private float eventCalloutTimer;
    private boolean touchRestartPressed;
    private boolean touchRestartJustPressed;
    private boolean touchControlsEnabled;
    private boolean cameraInitialized;
    private boolean growthPickupActive;
    private boolean pointPickupActive;
    private boolean hasLastGrowthPickupPosition;
    private boolean hasLastPointPickupPosition;
    private boolean roundOver;
    private boolean roundStartSoundPlayed;
    private boolean roundTimedOut;
    private boolean suddenDeathActive;
    private int countdownCueSecond;
    private int roundNumber;
    private int playerWins;
    private int finishPositionCounter;
    private int timeoutSharedPoints;
    private int timeoutSurvivorCount;
    private Car boostedCar;
    private Car playerCar;
    private Car winner;
    private String eventCalloutTitle = "";
    private String eventCalloutSubline = "";

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
        hudFont.getData().setScale(HUD_FONT_SCALE);

        titleFont = new BitmapFont();
        titleFont.setUseIntegerPositions(false);
        titleFont.getData().setScale(TITLE_FONT_SCALE);

        leaderboardFont = new BitmapFont();
        leaderboardFont.setUseIntegerPositions(false);
        leaderboardFont.getData().setScale(LEADERBOARD_FONT_SCALE);

        labelFont = new BitmapFont();
        labelFont.setUseIntegerPositions(false);
        labelFont.getData().setScale(LABEL_FONT_SCALE);

        loadThemeEnemyNames();
        loadThemeTextures();
        loadSounds();
        Gdx.input.setCatchKey(Input.Keys.BACK, true);
        createRoster();
        loadCarSprites();
        mapProgression = new MapProgression(ArenaMaps.createDefaultSet());

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        resetRound(false);
    }

    private void createRoster() {
        roster.clear();
        CarVisual playerVisual = getCarVisual(0);
        addRosterTemplate(
                "You",
                true,
                new Color(playerVisual.color),
                null,
                playerVisual,
                "player");

        int enemyCount = getConfiguredEnemyCount();
        for (int enemyIndex = 0; enemyIndex < enemyCount; enemyIndex++) {
            CarVisual visual = getEnemyCarVisual(enemyIndex);
            AiDrivingPersonality personality = ENEMY_PERSONALITIES[enemyIndex % ENEMY_PERSONALITIES.length];
            addRosterTemplate(
                    getEnemyName(enemyIndex),
                    false,
                    new Color(visual.color),
                    personality,
                    visual,
                    personality.id);
        }
    }

    private int getConfiguredEnemyCount() {
        return themeEnemyNames.size > 0 ? themeEnemyNames.size : DEFAULT_ENEMY_NAMES.length;
    }

    private String getEnemyName(int enemyIndex) {
        if (enemyIndex >= 0 && enemyIndex < themeEnemyNames.size) {
            return themeEnemyNames.get(enemyIndex);
        }
        if (enemyIndex >= 0 && enemyIndex < DEFAULT_ENEMY_NAMES.length) {
            return DEFAULT_ENEMY_NAMES[enemyIndex];
        }
        return "Rival " + (enemyIndex + 1);
    }

    private CarVisual getEnemyCarVisual(int enemyIndex) {
        int carVisualCount = getAvailableCarVisualCount();
        if (carVisualCount > 0 && getConfiguredEnemyCount() >= carVisualCount) {
            return getCarVisual(enemyIndex);
        }
        return getCarVisual(enemyIndex + 1);
    }

    private void createSimulationRoster(Array<AiTournamentParticipant> participants) {
        roster.clear();
        for (int i = 0; i < participants.size; i++) {
            AiTournamentParticipant participant = participants.get(i);
            CarVisual visual = getCarVisual(i);
            String displayName =
                    participant.displayName == null || participant.displayName.length() == 0
                            ? "Bot " + (i + 1)
                            : participant.displayName;
            addRosterTemplate(
                    displayName,
                    false,
                    new Color(visual.color),
                    participant.personality,
                    visual,
                    participant.label);
        }
    }

    private void addRosterTemplate(
            String name,
            boolean playerControlled,
            Color color,
            AiDrivingPersonality personality,
            CarVisual visual,
            String statsLabel) {
        roster.add(new CarTemplate(
                roster.size,
                name,
                playerControlled,
                color,
                personality,
                visual,
                statsLabel));
    }

    public static AiTournamentResult runAiTournament(AiTournamentConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Ai tournament config is required.");
        }
        if (config.participants.size == 0) {
            throw new IllegalArgumentException("Ai tournament config requires at least one participant.");
        }
        if (config.rounds <= 0) {
            throw new IllegalArgumentException("Ai tournament rounds must be positive.");
        }

        RatassGame game = new RatassGame();
        try {
            return game.runAiTournamentInternal(config);
        } finally {
            game.dispose();
        }
    }

    private AiTournamentResult runAiTournamentInternal(AiTournamentConfig config) {
        MathUtils.random.setSeed(config.seed);
        Box2D.init();

        Array<ArenaMap> maps = config.maps.size == 0 ? ArenaMaps.createDefaultSet() : config.maps;
        createSimulationRoster(config.participants);
        mapProgression = new MapProgression(maps, new Random(config.seed ^ 0x9E3779B97F4A7C15L));
        frameThrottleInput = 0f;
        frameTurnInput = 0f;
        frameHandbrakeInput = false;
        roundNumber = 0;
        playerWins = 0;

        LinkedHashMap<String, AiTournamentEntry> statsByLabel = new LinkedHashMap<String, AiTournamentEntry>();
        for (int i = 0; i < config.participants.size; i++) {
            AiTournamentParticipant participant = config.participants.get(i);
            AiTournamentEntry entry = statsByLabel.get(participant.label);
            if (entry == null) {
                entry = new AiTournamentEntry(participant.label, participant.displayName);
                statsByLabel.put(participant.label, entry);
            }
            entry.entrants++;
        }

        resetRound(false);
        if (config.skipCountdown) {
            preRoundCountdownTimer = 0f;
            countdownCueSecond = 0;
        }

        int maxStepsPerRound = Math.max(1, MathUtils.ceil(ROUND_TIMEOUT_LIMIT / PHYSICS_STEP) + 300);
        for (int round = 0; round < config.rounds; round++) {
            int steps = 0;
            while (!roundOver && steps < maxStepsPerRound) {
                stepSimulation(PHYSICS_STEP);
                steps++;
            }
            if (!roundOver) {
                triggerRoundTimeout();
            }

            collectTournamentRound(statsByLabel);

            if (round + 1 < config.rounds) {
                resetRound(true);
                if (config.skipCountdown) {
                    preRoundCountdownTimer = 0f;
                    countdownCueSecond = 0;
                }
            }
        }

        AiTournamentResult result = new AiTournamentResult(config.seed, config.rounds, config.participants.size);
        for (Map.Entry<String, AiTournamentEntry> entry : statsByLabel.entrySet()) {
            result.entries.add(entry.getValue());
        }
        return result;
    }

    private void collectTournamentRound(Map<String, AiTournamentEntry> statsByLabel) {
        for (int i = 0; i < roster.size; i++) {
            CarTemplate template = roster.get(i);
            AiTournamentEntry entry = statsByLabel.get(template.statsLabel);
            if (entry == null) {
                continue;
            }

            entry.samples++;
            entry.totalPlacementPoints += template.roundFinishPosition;
            entry.totalAwardedPoints += template.lastRoundAwardedPoints;
            entry.totalPickupPoints += template.roundPickupPoints;
            if (winner != null && winner.template.vehicleId == template.vehicleId) {
                entry.wins++;
            }
        }
    }

    private CarVisual getCarVisual(int rosterIndex) {
        if (themeCarVisuals.size > 0) {
            return themeCarVisuals.get(rosterIndex % themeCarVisuals.size);
        }
        return NORMAL_CAR_VISUALS[rosterIndex % NORMAL_CAR_VISUALS.length];
    }

    private int getAvailableCarVisualCount() {
        return themeCarVisuals.size > 0 ? themeCarVisuals.size : NORMAL_CAR_VISUALS.length;
    }

    private void loadCarSprites() {
        for (int i = 0; i < roster.size; i++) {
            CarTemplate template = roster.get(i);
            if (template.ownsSpriteTexture) {
                disposeTexture(template.spriteTexture);
            }
            template.spriteTexture = null;
            template.ownsSpriteTexture = false;
            template.spriteSourceX = 0;
            template.spriteSourceY = 0;
            template.spriteSourceWidth = 0;
            template.spriteSourceHeight = 0;

            if (template.visual == null) {
                continue;
            }

            if (template.visual.texture != null) {
                template.spriteTexture = template.visual.texture;
            } else if (template.visual.sharedTexture) {
                template.spriteTexture = themeCarsTexture;
            } else {
                template.spriteTexture = loadTexture(template.visual.spritePath, template.visual.stripDarkBackground);
                template.ownsSpriteTexture = template.spriteTexture != null;
            }

            if (template.spriteTexture == null) {
                continue;
            }

            if (template.visual.hasSourceRegion()) {
                template.spriteSourceX = template.visual.spriteSourceX;
                template.spriteSourceY = template.visual.spriteSourceY;
                template.spriteSourceWidth = template.visual.spriteSourceWidth;
                template.spriteSourceHeight = template.visual.spriteSourceHeight;
            } else {
                template.spriteSourceWidth = template.spriteTexture.getWidth();
                template.spriteSourceHeight = template.spriteTexture.getHeight();
            }
        }
    }

    private void loadSounds() {
        impactSound = loadSound("audio/impact.wav");
        pickupSound = loadSound("audio/pickup.wav");
        destructionSound = loadSound("audio/destroy.wav");
        countdownSound = loadSound("audio/countdown.wav");
        roundStartSound = loadSound("audio/start.wav");
        suddenDeathSound = loadSound("audio/sudden_death.wav");
        timeoutSound = loadSound("audio/timeout.wav");
    }

    private Sound loadSound(String path) {
        FileHandle handle = resolveAssetHandle(path);
        if (handle == null || !handle.exists()) {
            return null;
        }
        try {
            return Gdx.audio.newSound(handle);
        } catch (RuntimeException exception) {
            Gdx.app.error("RatassGame", "Could not load sound " + handle.path(), exception);
            return null;
        }
    }

    private void loadThemeEnemyNames() {
        themeEnemyNames.clear();
        FileHandle handle = resolveThemedAssetHandle(THEME_ENEMY_NAMES_PATH);
        if (handle == null || !handle.exists()) {
            return;
        }

        try {
            String[] lines = handle.readString("UTF-8").split("\\r?\\n");
            for (int i = 0; i < lines.length; i++) {
                String name = lines[i].trim();
                if (name.length() == 0 || name.startsWith("#")) {
                    continue;
                }
                themeEnemyNames.add(name);
            }
        } catch (RuntimeException exception) {
            Gdx.app.error("RatassGame", "Could not load enemy names from " + handle.path(), exception);
            themeEnemyNames.clear();
        }
    }

    private void loadThemeTextures() {
        disposeThemeCarVisualTextures();
        disposeTexture(themeCarsTexture);
        themeCarsTexture = null;
        themeCarVisuals.clear();

        FileHandle carSheetHandle = resolveThemedAssetHandle(THEME_CAR_SHEET_PATH);
        if (carSheetHandle == null || !carSheetHandle.exists()) {
            return;
        }

        try {
            Pixmap source = new Pixmap(carSheetHandle);
            Pixmap masked = createMaskedPixmap(source);
            source.dispose();

            themeCarVisuals.addAll(extractCarVisualsFromSheet(masked));
            masked.dispose();
        } catch (RuntimeException exception) {
            Gdx.app.error("RatassGame", "Could not load themed car sheet " + carSheetHandle.path(), exception);
            disposeThemeCarVisualTextures();
            themeCarVisuals.clear();
            disposeTexture(themeCarsTexture);
            themeCarsTexture = null;
        }
    }

    private Texture loadTexture(String path) {
        return loadTexture(path, false);
    }

    private Texture loadTexture(String path, boolean stripDarkBackground) {
        FileHandle handle = resolveAssetHandle(path);
        if (handle == null || !handle.exists()) {
            return null;
        }
        try {
            Texture texture;
            if (stripDarkBackground) {
                Pixmap source = new Pixmap(handle);
                Pixmap masked = createMaskedPixmap(source);
                source.dispose();
                texture = new Texture(masked);
                masked.dispose();
            } else {
                texture = new Texture(handle);
            }
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            return texture;
        } catch (RuntimeException exception) {
            Gdx.app.error("RatassGame", "Could not load texture " + handle.path(), exception);
            return null;
        }
    }

    private String buildThemeAssetPath(String relativePath) {
        return THEME_DIRECTORY + "/" + configuredThemeName + "/" + relativePath;
    }

    private FileHandle resolveThemedAssetHandle(String relativePath) {
        FileHandle handle = Gdx.files.internal(buildThemeAssetPath(relativePath));
        return handle.exists() ? handle : null;
    }

    private FileHandle resolveAssetHandle(String relativePath) {
        FileHandle themed = resolveThemedAssetHandle(relativePath);
        if (themed != null) {
            return themed;
        }
        FileHandle fallback = Gdx.files.internal(relativePath);
        return fallback.exists() ? fallback : null;
    }

    private Pixmap createMaskedPixmap(Pixmap source) {
        Pixmap masked = new Pixmap(source.getWidth(), source.getHeight(), Pixmap.Format.RGBA8888);
        int background = source.getPixel(0, 0);
        int bgR = (background >>> 24) & 0xff;
        int bgG = (background >>> 16) & 0xff;
        int bgB = (background >>> 8) & 0xff;

        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int pixel = source.getPixel(x, y);
                int red = (pixel >>> 24) & 0xff;
                int green = (pixel >>> 16) & 0xff;
                int blue = (pixel >>> 8) & 0xff;
                int alpha = computeMaskedAlpha(red, green, blue, bgR, bgG, bgB);
                masked.drawPixel(x, y, (red << 24) | (green << 16) | (blue << 8) | alpha);
            }
        }
        return masked;
    }

    private Array<CarVisual> extractCarVisualsFromSheet(Pixmap masked) {
        Array<CarVisual> visuals = new Array<CarVisual>();
        List<ThemeCarComponent> components = detectThemeCarComponents(masked);
        for (int i = 0; i < components.size(); i++) {
            CarVisual visual = createThemeCarVisual(masked, components.get(i));
            if (visual != null) {
                visuals.add(visual);
            }
        }
        return visuals;
    }

    private List<ThemeCarComponent> detectThemeCarComponents(Pixmap masked) {
        int width = masked.getWidth();
        int height = masked.getHeight();
        boolean[] visited = new boolean[width * height];
        int[] queue = new int[width * height];
        List<ThemeCarComponent> components = new ArrayList<ThemeCarComponent>();
        int largestPixelCount = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                if (visited[index]) {
                    continue;
                }
                visited[index] = true;
                if (!isOpaquePixel(masked.getPixel(x, y))) {
                    continue;
                }

                IntArray pixels = new IntArray();
                int queueHead = 0;
                int queueTail = 0;
                int minX = x;
                int maxX = x;
                int minY = y;
                int maxY = y;
                queue[queueTail++] = index;

                while (queueHead < queueTail) {
                    int currentIndex = queue[queueHead++];
                    int currentY = currentIndex / width;
                    int currentX = currentIndex - currentY * width;
                    pixels.add(currentIndex);

                    minX = Math.min(minX, currentX);
                    maxX = Math.max(maxX, currentX);
                    minY = Math.min(minY, currentY);
                    maxY = Math.max(maxY, currentY);

                    for (int offsetY = -1; offsetY <= 1; offsetY++) {
                        for (int offsetX = -1; offsetX <= 1; offsetX++) {
                            if (offsetX == 0 && offsetY == 0) {
                                continue;
                            }

                            int neighborX = currentX + offsetX;
                            int neighborY = currentY + offsetY;
                            if (neighborX < 0 || neighborX >= width || neighborY < 0 || neighborY >= height) {
                                continue;
                            }

                            int neighborIndex = neighborY * width + neighborX;
                            if (visited[neighborIndex]) {
                                continue;
                            }
                            visited[neighborIndex] = true;
                            if (!isOpaquePixel(masked.getPixel(neighborX, neighborY))) {
                                continue;
                            }
                            queue[queueTail++] = neighborIndex;
                        }
                    }
                }

                ThemeCarComponent component = new ThemeCarComponent(pixels, minX, minY, maxX, maxY);
                largestPixelCount = Math.max(largestPixelCount, component.getPixelCount());
                components.add(component);
            }
        }

        if (components.isEmpty()) {
            return components;
        }

        int minPixelCount = Math.max(64, largestPixelCount / 24);
        List<ThemeCarComponent> filtered = new ArrayList<ThemeCarComponent>();
        for (int i = 0; i < components.size(); i++) {
            ThemeCarComponent component = components.get(i);
            if (component.getPixelCount() < minPixelCount
                    || component.getWidth() < 24
                    || component.getHeight() < 24) {
                continue;
            }
            filtered.add(component);
        }

        if (filtered.isEmpty()) {
            filtered.addAll(components);
        }

        sortThemeCarComponents(filtered);
        return filtered;
    }

    private void sortThemeCarComponents(List<ThemeCarComponent> components) {
        Collections.sort(
                components,
                new Comparator<ThemeCarComponent>() {
                    @Override
                    public int compare(ThemeCarComponent left, ThemeCarComponent right) {
                        return Float.compare(left.getCenterY(), right.getCenterY());
                    }
                });

        float averageHeight = 0f;
        for (int i = 0; i < components.size(); i++) {
            averageHeight += components.get(i).getHeight();
        }
        averageHeight /= components.size();
        float rowThreshold = Math.max(24f, averageHeight * 0.5f);
        List<List<ThemeCarComponent>> rows = new ArrayList<List<ThemeCarComponent>>();
        List<ThemeCarComponent> currentRow = null;
        float currentRowCenterY = 0f;
        int currentRowSize = 0;

        for (int i = 0; i < components.size(); i++) {
            ThemeCarComponent component = components.get(i);
            if (currentRow == null
                    || Math.abs(component.getCenterY() - currentRowCenterY) > rowThreshold) {
                currentRow = new ArrayList<ThemeCarComponent>();
                rows.add(currentRow);
                currentRowCenterY = component.getCenterY();
                currentRowSize = 0;
            }

            currentRow.add(component);
            currentRowCenterY =
                    (currentRowCenterY * currentRowSize + component.getCenterY())
                            / (currentRowSize + 1f);
            currentRowSize++;
        }

        components.clear();
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            List<ThemeCarComponent> row = rows.get(rowIndex);
            Collections.sort(
                    row,
                    new Comparator<ThemeCarComponent>() {
                        @Override
                        public int compare(ThemeCarComponent left, ThemeCarComponent right) {
                            return Float.compare(left.getCenterX(), right.getCenterX());
                        }
                    });
            components.addAll(row);
        }
    }

    private CarVisual createThemeCarVisual(Pixmap masked, ThemeCarComponent component) {
        int spriteWidth = component.getWidth();
        int spriteHeight = component.getHeight();
        Pixmap spritePixmap =
                new Pixmap(
                        spriteWidth + THEME_CAR_TEXTURE_PADDING * 2,
                        spriteHeight + THEME_CAR_TEXTURE_PADDING * 2,
                        Pixmap.Format.RGBA8888);
        int sheetWidth = masked.getWidth();
        for (int i = 0; i < component.pixels.size; i++) {
            int pixelIndex = component.pixels.get(i);
            int pixelY = pixelIndex / sheetWidth;
            int pixelX = pixelIndex - pixelY * sheetWidth;
            spritePixmap.drawPixel(
                    pixelX - component.minX + THEME_CAR_TEXTURE_PADDING,
                    pixelY - component.minY + THEME_CAR_TEXTURE_PADDING,
                    masked.getPixel(pixelX, pixelY));
        }

        Texture texture = new Texture(spritePixmap);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        Color color =
                sampleSpriteColor(
                        spritePixmap,
                        new Rectangle(0f, 0f, spritePixmap.getWidth(), spritePixmap.getHeight()));
        spritePixmap.dispose();
        return new CarVisual(texture, 0f, color);
    }

    private void disposeThemeCarVisualTextures() {
        for (int i = 0; i < themeCarVisuals.size; i++) {
            disposeTexture(themeCarVisuals.get(i).texture);
        }
    }

    private Color sampleSpriteColor(Pixmap masked, Rectangle bounds) {
        long red = 0L;
        long green = 0L;
        long blue = 0L;
        long samples = 0L;

        int minX = Math.round(bounds.x);
        int minY = Math.round(bounds.y);
        int maxX = minX + Math.round(bounds.width) - 1;
        int maxY = minY + Math.round(bounds.height) - 1;
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                int pixel = masked.getPixel(x, y);
                int alpha = pixel & 0xff;
                if (alpha < 64) {
                    continue;
                }
                red += (pixel >>> 24) & 0xff;
                green += (pixel >>> 16) & 0xff;
                blue += (pixel >>> 8) & 0xff;
                samples++;
            }
        }

        if (samples == 0L) {
            return new Color(0.82f, 0.82f, 0.82f, 1f);
        }

        float divisor = 255f * (float) samples;
        return new Color(red / divisor, green / divisor, blue / divisor, 1f);
    }

    private boolean isOpaquePixel(int pixel) {
        return (pixel & 0xff) > 0;
    }

    private int computeMaskedAlpha(int red, int green, int blue, int bgR, int bgG, int bgB) {
        int maxDifference = Math.max(
                Math.max(Math.abs(red - bgR), Math.abs(green - bgG)),
                Math.abs(blue - bgB));
        if (maxDifference <= BACKGROUND_ALPHA_CUTOUT_THRESHOLD) {
            return 0;
        }
        if (maxDifference >= BACKGROUND_ALPHA_OPAQUE_THRESHOLD) {
            return 255;
        }

        float normalized =
                (float) (maxDifference - BACKGROUND_ALPHA_CUTOUT_THRESHOLD)
                        / (BACKGROUND_ALPHA_OPAQUE_THRESHOLD - BACKGROUND_ALPHA_CUTOUT_THRESHOLD);
        return Math.round(normalized * 255f);
    }

    private int calculateSidebarWidth(int width) {
        float preferred = MathUtils.clamp(
                width * HUD_SIDEBAR_RATIO,
                HUD_SIDEBAR_PREFERRED_MIN_WIDTH,
                HUD_SIDEBAR_MAX_WIDTH);
        float sidebarWidth = Math.min(preferred, width - HUD_MIN_PLAYFIELD_WIDTH);
        if (sidebarWidth < HUD_SIDEBAR_MIN_WIDTH) {
            sidebarWidth = Math.min(HUD_SIDEBAR_MIN_WIDTH, width * 0.32f);
        }
        return Math.max(0, Math.round(MathUtils.clamp(sidebarWidth, 0f, HUD_SIDEBAR_MAX_WIDTH)));
    }

    private void updateAudioState(float delta) {
        impactSoundCooldown = Math.max(0f, impactSoundCooldown - delta);
        destructionSoundCooldown = Math.max(0f, destructionSoundCooldown - delta);

        if (roundOver) {
            return;
        }

        if (preRoundCountdownTimer > 0f) {
            int countdownSecond = MathUtils.ceil(preRoundCountdownTimer);
            if (countdownSecond < countdownCueSecond) {
                playSound(countdownSound, countdownSecond == 1 ? 0.62f : 0.48f);
                countdownCueSecond = countdownSecond;
            }
            return;
        }

        if (!roundStartSoundPlayed) {
            playSound(roundStartSound, 0.78f);
            roundStartSoundPlayed = true;
        }
    }

    private void playImpactSound(float impactStrength) {
        if (impactSoundCooldown > 0f) {
            return;
        }

        float volume = MathUtils.clamp(
                (impactStrength - Car.MIN_COLLISION_RESPONSE_IMPULSE) / 24f,
                0.18f,
                0.82f);
        playSound(impactSound, volume);
        impactSoundCooldown = IMPACT_SOUND_COOLDOWN;
    }

    private void playDestructionSound(float volume) {
        if (destructionSoundCooldown > 0f) {
            return;
        }

        playSound(destructionSound, volume);
        destructionSoundCooldown = DESTRUCTION_SOUND_COOLDOWN;
    }

    private void updatePresentationState(float delta) {
        eventCalloutTimer = Math.max(0f, eventCalloutTimer - delta);
        if (eventCalloutTimer == 0f) {
            eventCalloutTitle = "";
            eventCalloutSubline = "";
        }
    }

    private void announceEvent(String title, String subline, Color color) {
        eventCalloutTitle = title == null ? "" : title;
        eventCalloutSubline = subline == null ? "" : subline;
        eventCalloutColor.set(color == null ? Color.WHITE : color);
        eventCalloutTimer = EVENT_CALLOUT_DURATION;
    }

    private void clearEventCallout() {
        eventCalloutTimer = 0f;
        eventCalloutTitle = "";
        eventCalloutSubline = "";
    }

    private Car findActiveRamChargeCar() {
        for (int i = 0; i < cars.size; i++) {
            Car car = cars.get(i);
            if (car.active && car.body != null && car.hasRamCharge()) {
                return car;
            }
        }
        return null;
    }

    private CarTemplate findTemplateByVehicleId(int vehicleId) {
        for (int i = 0; i < roster.size; i++) {
            CarTemplate template = roster.get(i);
            if (template.vehicleId == vehicleId) {
                return template;
            }
        }
        return null;
    }

    private void playSound(Sound sound, float volume) {
        if (sound == null) {
            return;
        }
        sound.play(MathUtils.clamp(volume, 0f, 1f));
    }

    @Override
    public void resize(int width, int height) {
        sidebarHudWidth = calculateSidebarWidth(width);
        playfieldHudWidth = Math.max(1f, width - sidebarHudWidth);
        worldViewport.update(Math.max(1, Math.round(playfieldHudWidth)), height, true);
        updateWorldCamera();

        hudViewport.update(width, height, true);
    }

    @Override
    public void render() {
        float delta = Math.min(Gdx.graphics.getDeltaTime(), 1f / 30f);

        updateTouchState();
        frameThrottleInput = readPlayerThrottle();
        frameTurnInput = readPlayerTurn();
        frameHandbrakeInput = readPlayerHandbrake();

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
        updatePresentationState(delta);
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

        updateCarRenderTransforms(MathUtils.clamp(accumulator / PHYSICS_STEP, 0f, 1f));
        updateAudioState(delta);
    }

    private void updateCarRenderTransforms(float alpha) {
        for (int i = 0; i < cars.size; i++) {
            cars.get(i).updateRenderTransform(alpha);
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
                    frameHandbrakeInput,
                    growthPickupActive,
                    growthPickupPosition,
                    pointPickupActive,
                    pointPickupPosition);
        }

        if (!allowControl && !roundOver) {
            freezeCarsForCountdown();
        }

        for (int i = 0; i < cars.size; i++) {
            cars.get(i).capturePreviousTransform();
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
                car.clearImpactResponse();
                car.body.setLinearVelocity(0f, 0f);
                car.body.setAngularVelocity(0f);
            }
        }
    }

    private void resetRound(boolean advanceMap) {
        if (advanceMap) {
            mapProgression.advance();
        }

        currentMap = mapProgression.getCurrentMap();
        disposeTexture(arenaSurfaceTexture);
        arenaSurfaceTexture = null;
        cameraInitialized = false;
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
        pointPickupActive = false;
        hasLastGrowthPickupPosition = false;
        hasLastPointPickupPosition = false;
        clearEventCallout();
        growthBoostTimer = 0f;
        boostedCar = null;
        preRoundCountdownTimer = ROUND_START_COUNTDOWN;
        countdownCueSecond = MathUtils.ceil(ROUND_START_COUNTDOWN) + 1;
        roundStartSoundPlayed = false;
        roundTimer = 0f;
        impactSoundCooldown = 0f;
        destructionSoundCooldown = 0f;
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

        cameraInitialized = false;
        updateWorldCamera();
        spawnGrowthPickup();
    }

    private void freezeCarsForCountdown() {
        for (int i = 0; i < cars.size; i++) {
            Car car = cars.get(i);
            if (!car.active || car.body == null) {
                continue;
            }
            car.clearImpactResponse();
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
            playSound(suddenDeathSound, 0.72f);
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
        playSound(timeoutSound, 0.78f);
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
        playDestructionSound(0.42f + car.getSizeScale() * 0.10f);
        announceElimination(car);
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
        effect.scale = 0.62f + MathUtils.random(0.28f) + car.getSizeScale() * 0.07f;
        destructionEffects.add(effect);
    }

    private void updateWorldCamera() {
        if (worldCamera == null) {
            return;
        }

        if (currentMap == null) {
            cameraTargetPosition.set(0f, 0f);
            cameraSmoothedPosition.set(cameraTargetPosition);
            cameraSmoothedForwardDirection.set(0f, 1f);
            smoothedCameraZoom = 1f;
            cameraInitialized = true;
            applyWorldCamera(1f);
            return;
        }

        currentMap.getBounds(mapBounds);
        currentMap.getFocusPoint(focusPoint);

        float visibleWidth = Math.max(1f, WORLD_WIDTH - CAMERA_HORIZONTAL_PADDING);
        float visibleHeight = Math.max(1f, WORLD_HEIGHT - CAMERA_VERTICAL_PADDING);
        float zoomX = mapBounds.width / visibleWidth;
        float zoomY = mapBounds.height / visibleHeight;

        float targetZoom = Math.max(MIN_WORLD_CAMERA_ZOOM, Math.max(zoomX, zoomY));
        cameraTargetPosition.set(focusPoint);
        float delta = Math.min(Gdx.graphics.getDeltaTime(), 1f / 30f);

        boolean playerCameraActive = playerCar != null && playerCar.active && playerCar.body != null;

        if (playerCameraActive) {
            Vector2 playerPosition = playerCar.getRenderPosition();
            Vector2 playerVelocity = playerCar.body.getLinearVelocity();
            float playerSpeed = playerVelocity.len();
            float speedFactor = MathUtils.clamp(playerSpeed / Car.MAX_SPEED, 0f, 1f);
            targetZoom = MathUtils.clamp(
                    PLAYER_CAMERA_ZOOM
                            + speedFactor * PLAYER_CAMERA_SPEED_ZOOM_OUT
                            + MathUtils.clamp(playerCar.getSizeScale() - 1f, 0f, 1f)
                                    * PLAYER_CAMERA_GROWTH_ZOOM_OUT,
                    PLAYER_CAMERA_ZOOM,
                    PLAYER_CAMERA_MAX_ZOOM);
            if (followCameraBehind) {
                playerCar.getRenderForwardDirection(cameraForwardDirection);
                if (!cameraForwardDirection.isZero(0.0001f)) {
                    cameraForwardDirection.nor();
                } else {
                    cameraForwardDirection.set(cameraSmoothedForwardDirection);
                }

                if (!cameraInitialized || delta <= 0f) {
                    cameraSmoothedForwardDirection.set(cameraForwardDirection);
                } else if (playerSpeed >= PLAYER_CAMERA_DIRECTION_MIN_SPEED) {
                    float spinDamping =
                            1f
                                    / (1f
                                            + Math.abs(playerCar.body.getAngularVelocity())
                                                    * PLAYER_CAMERA_DIRECTION_SPIN_DAMPING);
                    float impactDamping =
                            playerCar.getRecentImpactTime() > 0f
                                    ? PLAYER_CAMERA_DIRECTION_IMPACT_DAMPING
                                    : 1f;
                    float directionAlpha =
                            1f
                                    - (float)
                                            Math.exp(
                                                    -PLAYER_CAMERA_DIRECTION_LERP_SPEED
                                                            * spinDamping
                                                            * impactDamping
                                                            * delta);
                    cameraSmoothedForwardDirection
                            .lerp(cameraForwardDirection, directionAlpha)
                            .nor();
                } else {
                    float slowDirectionAlpha =
                            1f
                                    - (float)
                                            Math.exp(
                                                    -PLAYER_CAMERA_DIRECTION_RETURN_SPEED
                                                            * PLAYER_CAMERA_DIRECTION_IMPACT_DAMPING
                                                            * delta);
                    cameraSmoothedForwardDirection
                            .lerp(cameraForwardDirection, slowDirectionAlpha)
                            .nor();
                }

                cameraChaseOffset
                        .set(cameraSmoothedForwardDirection)
                        .scl(PLAYER_CAMERA_FOLLOW_LEAD_DISTANCE
                                + speedFactor * PLAYER_CAMERA_FOLLOW_LEAD_SPEED_BONUS);
                cameraTargetPosition.set(playerPosition).add(cameraChaseOffset);
            } else {
                cameraLookAhead.set(playerVelocity).scl(PLAYER_CAMERA_LOOK_AHEAD_TIME);
                if (cameraLookAhead.len2() > PLAYER_CAMERA_MAX_LOOK_AHEAD * PLAYER_CAMERA_MAX_LOOK_AHEAD) {
                    cameraLookAhead.setLength(PLAYER_CAMERA_MAX_LOOK_AHEAD);
                }
                cameraSmoothedForwardDirection.set(0f, 1f);
                cameraTargetPosition.set(playerPosition).add(cameraLookAhead);
            }
        } else if (!cameraInitialized || delta <= 0f) {
            cameraSmoothedForwardDirection.set(0f, 1f);
        } else {
            float directionReturnAlpha =
                    1f
                            - (float)
                                    Math.exp(
                                            -PLAYER_CAMERA_DIRECTION_RETURN_SPEED
                                                    * delta);
            cameraSmoothedForwardDirection.lerp(cameraLookAhead.set(0f, 1f), directionReturnAlpha).nor();
        }

        if (!playerCameraActive || !followCameraBehind) {
            clampCameraToArena(cameraTargetPosition, targetZoom);
        }

        if (!cameraInitialized || delta <= 0f) {
            cameraSmoothedPosition.set(cameraTargetPosition);
            smoothedCameraZoom = targetZoom;
            cameraInitialized = true;
        } else {
            float alpha = 1f - (float) Math.exp(-PLAYER_CAMERA_FOLLOW_LERP_SPEED * delta);
            cameraSmoothedPosition.lerp(cameraTargetPosition, alpha);
            smoothedCameraZoom += (targetZoom - smoothedCameraZoom) * alpha;
        }

        if (!playerCameraActive || !followCameraBehind) {
            clampCameraToArena(cameraSmoothedPosition, smoothedCameraZoom);
        }
        applyWorldCamera(smoothedCameraZoom);
    }

    private void clampCameraToArena(Vector2 position, float zoom) {
        float halfViewportWidth = worldCamera.viewportWidth * zoom * 0.5f;
        float halfViewportHeight = worldCamera.viewportHeight * zoom * 0.5f;
        if (cameraSmoothedForwardDirection.isZero(0.0001f)) {
            cameraSmoothedForwardDirection.set(0f, 1f);
        }

        cameraRightDirection.set(
                cameraSmoothedForwardDirection.y,
                -cameraSmoothedForwardDirection.x);

        float halfWorldX =
                Math.abs(cameraRightDirection.x) * halfViewportWidth
                        + Math.abs(cameraSmoothedForwardDirection.x) * halfViewportHeight;
        float halfWorldY =
                Math.abs(cameraRightDirection.y) * halfViewportWidth
                        + Math.abs(cameraSmoothedForwardDirection.y) * halfViewportHeight;

        float minX = mapBounds.x + halfWorldX;
        float maxX = mapBounds.x + mapBounds.width - halfWorldX;
        float minY = mapBounds.y + halfWorldY;
        float maxY = mapBounds.y + mapBounds.height - halfWorldY;

        position.x = minX > maxX ? focusPoint.x : MathUtils.clamp(position.x, minX, maxX);
        position.y = minY > maxY ? focusPoint.y : MathUtils.clamp(position.y, minY, maxY);
    }

    private void applyWorldCamera(float zoom) {
        if (followCameraBehind && cameraSmoothedForwardDirection.isZero(0.0001f)) {
            cameraSmoothedForwardDirection.set(0f, 1f);
        }
        worldCamera.zoom = zoom;
        if (followCameraBehind) {
            worldCamera.up.set(
                    cameraSmoothedForwardDirection.x,
                    cameraSmoothedForwardDirection.y,
                    0f);
        } else {
            worldCamera.up.set(0f, 1f, 0f);
        }
        worldCamera.direction.set(0f, 0f, -1f);
        worldCamera.position.set(
                cameraSmoothedPosition.x,
                cameraSmoothedPosition.y,
                0f);
        worldCamera.update();
    }

    private void buildRoundSpawns(int count, Array<SpawnPoint> out) {
        out.clear();
        currentMap.getFocusPoint(focusPoint);
        currentMap.getBounds(mapBounds);
        float safeMargin = getRoundSpawnSafeMargin(count);
        float minDistance = getRoundSpawnMinDistance(count);
        int maxAttempts = getRoundSpawnAttempts(count);

        for (int i = 0; i < currentMap.getSpawnCount() && out.size < count; i++) {
            SpawnPoint seed = currentMap.getSpawn(i);
            spawnCandidate.set(seed.x, seed.y);
            if (currentMap.distanceToHazard(spawnCandidate) < safeMargin) {
                continue;
            }
            if (isSpawnLocationClear(spawnCandidate, out, minDistance)) {
                out.add(seed);
            }
        }

        float minX = mapBounds.x + safeMargin;
        float maxX = mapBounds.x + mapBounds.width - safeMargin;
        float minY = mapBounds.y + safeMargin;
        float maxY = mapBounds.y + mapBounds.height - safeMargin;

        for (int attempt = 0; attempt < maxAttempts && out.size < count; attempt++) {
            spawnCandidate.set(MathUtils.random(minX, maxX), MathUtils.random(minY, maxY));
            currentMap.clampToPlayable(spawnCandidate, safeMargin);

            if (currentMap.distanceToHazard(spawnCandidate) < safeMargin) {
                continue;
            }
            if (!isSpawnLocationClear(spawnCandidate, out, minDistance)) {
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

    private float getRoundSpawnSafeMargin(int count) {
        if (count <= DEFAULT_ENEMY_NAMES.length + 1) {
            return ROUND_SPAWN_SAFE_MARGIN;
        }
        float crowdedAlpha =
                MathUtils.clamp(
                        (float) (count - (DEFAULT_ENEMY_NAMES.length + 1)) / 42f,
                        0f,
                        1f);
        return MathUtils.lerp(ROUND_SPAWN_SAFE_MARGIN, 0.82f, crowdedAlpha);
    }

    private float getRoundSpawnMinDistance(int count) {
        if (count <= DEFAULT_ENEMY_NAMES.length + 1) {
            return ROUND_SPAWN_MIN_DISTANCE;
        }
        float crowdedAlpha =
                MathUtils.clamp(
                        (float) (count - (DEFAULT_ENEMY_NAMES.length + 1)) / 42f,
                        0f,
                        1f);
        return MathUtils.lerp(ROUND_SPAWN_MIN_DISTANCE, 1.08f, crowdedAlpha);
    }

    private int getRoundSpawnAttempts(int count) {
        return Math.max(ROUND_SPAWN_ATTEMPTS, count * 240);
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

    private boolean readPlayerHandbrake() {
        return Gdx.input.isKeyPressed(Input.Keys.SPACE);
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
        float controlWidth = playfieldHudWidth > 0f ? Math.min(playfieldHudWidth, hudWidth) : hudWidth;
        float rightEdge = Math.max(buttonWidth + 36f, controlWidth - 18f);
        throttlePadBounds.set(rightEdge - buttonWidth, 18f + padSize * 0.52f, buttonWidth, padSize * 0.42f);
        reversePadBounds.set(rightEdge - buttonWidth, 18f, buttonWidth, padSize * 0.32f);
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

    private void updatePointPickup() {
        if (findActiveRamChargeCar() != null) {
            pointPickupActive = false;
            return;
        }

        if (!pointPickupActive) {
            spawnPointPickup();
            return;
        }

        for (int i = 0; i < cars.size; i++) {
            Car car = cars.get(i);
            if (!car.active || car.body == null) {
                continue;
            }

            float collectRadius = POINT_PICKUP_RADIUS + car.getGrowthPickupReach();
            if (car.body.getPosition().dst2(pointPickupPosition) <= collectRadius * collectRadius) {
                collectPointPickup(car);
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
        playSound(pickupSound, 0.72f);
        if (car.playerControlled) {
            announceEvent("MASS CORE", "You are huge for 10 seconds.", new Color(0.99f, 0.85f, 0.28f, 1f));
        }
    }

    private void collectPointPickup(Car car) {
        if (car == null || !pointPickupActive || !car.active) {
            return;
        }

        lastPointPickupPosition.set(pointPickupPosition);
        hasLastPointPickupPosition = true;
        pointPickupActive = false;
        car.grantRamCharge();
        playSound(pickupSound, 0.64f);
        if (car.playerControlled) {
            announceEvent(
                    "RAM CORE",
                    "Your next clean hit lands much harder.",
                    new Color(1f, 0.48f, 0.22f, 1f));
        }
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
            if (avoidCars && !isPickupFarFromCars(pickupCandidate, GROWTH_PICKUP_RADIUS, 0.75f)) {
                continue;
            }
            if (!isPickupSeparated(pickupCandidate, pointPickupPosition, pointPickupActive, PICKUP_MIN_SEPARATION)) {
                continue;
            }

            growthPickupPosition.set(pickupCandidate);
            growthPickupActive = true;
            return true;
        }

        return false;
    }

    private void spawnPointPickup() {
        if (currentMap == null) {
            return;
        }

        pointPickupActive = false;
        currentMap.getBounds(mapBounds);

        if (trySpawnPointPickup(true, true)
                || trySpawnPointPickup(true, false)
                || trySpawnPointPickup(false, false)) {
            return;
        }

        pickupCandidate.set(0f, 0f);
        currentMap.findRecoveryPoint(pickupCandidate, pointPickupPosition);
        pointPickupActive = true;
    }

    private boolean trySpawnPointPickup(boolean avoidCars, boolean requireNewSpot) {
        float minX = mapBounds.x + POINT_PICKUP_SPAWN_MARGIN;
        float maxX = mapBounds.x + mapBounds.width - POINT_PICKUP_SPAWN_MARGIN;
        float minY = mapBounds.y + POINT_PICKUP_SPAWN_MARGIN;
        float maxY = mapBounds.y + mapBounds.height - POINT_PICKUP_SPAWN_MARGIN;

        if (minX >= maxX || minY >= maxY) {
            return false;
        }

        for (int attempt = 0; attempt < POINT_PICKUP_SPAWN_ATTEMPTS; attempt++) {
            pickupCandidate.set(MathUtils.random(minX, maxX), MathUtils.random(minY, maxY));

            if (currentMap.distanceToHazard(pickupCandidate) < POINT_PICKUP_SPAWN_MARGIN) {
                continue;
            }
            if (requireNewSpot
                    && hasLastPointPickupPosition
                    && pickupCandidate.dst2(lastPointPickupPosition)
                    < POINT_PICKUP_MIN_MOVE_DISTANCE * POINT_PICKUP_MIN_MOVE_DISTANCE) {
                continue;
            }
            if (avoidCars && !isPickupFarFromCars(pickupCandidate, POINT_PICKUP_RADIUS, 0.65f)) {
                continue;
            }
            if (!isPickupSeparated(pickupCandidate, growthPickupPosition, growthPickupActive, PICKUP_MIN_SEPARATION)) {
                continue;
            }

            pointPickupPosition.set(pickupCandidate);
            pointPickupActive = true;
            return true;
        }

        return false;
    }

    private boolean isPickupFarFromCars(Vector2 candidate, float pickupRadius, float extraMargin) {
        for (int i = 0; i < cars.size; i++) {
            Car car = cars.get(i);
            if (!car.active || car.body == null) {
                continue;
            }

            float minDistance = car.getGrowthPickupReach() + pickupRadius + extraMargin;
            if (car.body.getPosition().dst2(candidate) < minDistance * minDistance) {
                return false;
            }
        }

        return true;
    }

    private boolean isPickupSeparated(
            Vector2 candidate,
            Vector2 otherPickupPosition,
            boolean otherPickupActive,
            float minDistance) {
        return !otherPickupActive
                || candidate.dst2(otherPickupPosition) >= minDistance * minDistance;
    }

    private void renderWorld() {
        MapTheme theme = currentTheme();
        if (currentMap != null) {
            currentMap.getBounds(mapBounds);
            currentMap.getFocusPoint(focusPoint);
        } else {
            mapBounds.set(-WORLD_WIDTH * 0.32f, -WORLD_HEIGHT * 0.26f, WORLD_WIDTH * 0.64f, WORLD_HEIGHT * 0.52f);
            focusPoint.set(0f, 0f);
        }

        ScreenUtils.clear(theme.backdropBase.r, theme.backdropBase.g, theme.backdropBase.b, 1f);

        updateWorldCamera();
        worldViewport.apply();
        shapeRenderer.setProjectionMatrix(worldCamera.combined);
        spriteBatch.setProjectionMatrix(worldCamera.combined);
        ensureArenaSurfaceTexture(theme);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        drawBackdrop(theme);
        drawArenaUnderlay(theme);
        shapeRenderer.end();

        spriteBatch.begin();
        drawArenaSurface();
        spriteBatch.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        drawArenaOverlay(theme);
        drawGrowthPickup();
        drawCarEffects();
        shapeRenderer.end();

        spriteBatch.begin();
        drawCarSprites();
        spriteBatch.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        drawDestructionEffects();
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawBackdrop(MapTheme theme) {
        float backdropMarginX = Math.max(WORLD_WIDTH, mapBounds.width * 0.75f);
        float backdropMarginY = Math.max(WORLD_HEIGHT, mapBounds.height * 0.75f);
        float backdropMinX = mapBounds.x - backdropMarginX;
        float backdropMaxX = mapBounds.x + mapBounds.width + backdropMarginX;
        float backdropMinY = mapBounds.y - backdropMarginY;
        float backdropMaxY = mapBounds.y + mapBounds.height + backdropMarginY;
        float backdropWidth = backdropMaxX - backdropMinX;
        float backdropHeight = backdropMaxY - backdropMinY;

        shapeRenderer.setColor(theme.backdropBase);
        shapeRenderer.rect(backdropMinX, backdropMinY, backdropWidth, backdropHeight);

        for (float y = backdropMinY; y < backdropMaxY; y += 3.25f) {
            boolean majorBand = (((int) ((y - backdropMinY) / 3.25f)) & 1) == 0;
            setShapeColor(theme.backdropLine, majorBand ? 0.12f : 0.06f);
            shapeRenderer.rect(backdropMinX, y, backdropWidth, majorBand ? 1.35f : 0.72f);
        }

        for (float x = backdropMinX; x <= backdropMaxX; x += 3.1f) {
            float alpha = Math.abs(x - focusPoint.x) < mapBounds.width * 0.44f ? 0.15f : 0.08f;
            setShapeColor(theme.backdropLine, alpha);
            shapeRenderer.rect(x, backdropMinY, 0.16f, backdropHeight);
        }

        for (float y = backdropMinY; y <= backdropMaxY; y += 2.6f) {
            float alpha = Math.abs(y - focusPoint.y) < mapBounds.height * 0.42f ? 0.08f : 0.04f;
            setShapeColor(theme.backdropGlow, alpha);
            shapeRenderer.rect(backdropMinX, y, backdropWidth, 0.10f);
        }

        drawBackdropMotif(theme);
    }

    private void drawBackdropMotif(MapTheme theme) {
        float pulse = 0.5f + 0.5f * MathUtils.sin(effectClock * 1.35f);
        float centerX = focusPoint.x;
        float centerY = focusPoint.y;
        float span = Math.max(mapBounds.width, mapBounds.height);
        float longSpan = span + 10f;
        float laneAngle = mapBounds.width >= mapBounds.height ? 0f : 90f;

        switch (theme.decorStyle) {
            case ORBIT:
                setShapeColor(theme.backdropGlow, 0.05f + pulse * 0.04f);
                shapeRenderer.circle(centerX, centerY, span * 0.28f + 1.2f, 56);
                setShapeColor(theme.accentSoft, 0.04f);
                shapeRenderer.circle(centerX, centerY, span * 0.46f + 2.8f, 56);
                setShapeColor(theme.accent, 0.05f);
                shapeRenderer.circle(centerX, centerY, span * 0.64f + 3.8f, 56);
                break;
            case RUNWAY:
                drawRotatedRect(
                        centerX,
                        centerY,
                        longSpan + 8f,
                        0.92f,
                        laneAngle,
                        theme.backdropGlow.r,
                        theme.backdropGlow.g,
                        theme.backdropGlow.b,
                        0.09f + pulse * 0.03f);
                drawRotatedRect(
                        centerX,
                        centerY,
                        longSpan + 10f,
                        0.20f,
                        laneAngle,
                        theme.accent.r,
                        theme.accent.g,
                        theme.accent.b,
                        0.18f);
                drawOffsetRotatedRect(
                        centerX,
                        centerY,
                        0f,
                        2.4f,
                        longSpan + 6f,
                        0.26f,
                        laneAngle,
                        theme.accentSoft.r,
                        theme.accentSoft.g,
                        theme.accentSoft.b,
                        0.08f);
                drawOffsetRotatedRect(
                        centerX,
                        centerY,
                        0f,
                        -2.4f,
                        longSpan + 6f,
                        0.26f,
                        laneAngle,
                        theme.accentSoft.r,
                        theme.accentSoft.g,
                        theme.accentSoft.b,
                        0.08f);
                break;
            case CROSS:
                drawRotatedRect(
                        centerX,
                        centerY,
                        longSpan + 8f,
                        1.05f,
                        0f,
                        theme.backdropGlow.r,
                        theme.backdropGlow.g,
                        theme.backdropGlow.b,
                        0.08f + pulse * 0.03f);
                drawRotatedRect(
                        centerX,
                        centerY,
                        mapBounds.height + 11f,
                        1.05f,
                        90f,
                        theme.backdropGlow.r,
                        theme.backdropGlow.g,
                        theme.backdropGlow.b,
                        0.08f + pulse * 0.03f);
                drawRotatedRect(
                        centerX,
                        centerY,
                        longSpan + 12f,
                        0.18f,
                        0f,
                        theme.accent.r,
                        theme.accent.g,
                        theme.accent.b,
                        0.14f);
                drawRotatedRect(
                        centerX,
                        centerY,
                        mapBounds.height + 14f,
                        0.18f,
                        90f,
                        theme.accent.r,
                        theme.accent.g,
                        theme.accent.b,
                        0.14f);
                break;
            case FORTRESS:
                drawRotatedRect(
                        centerX,
                        centerY - mapBounds.height * 0.5f - 2.2f,
                        mapBounds.width + 7f,
                        0.42f,
                        0f,
                        theme.backdropGlow.r,
                        theme.backdropGlow.g,
                        theme.backdropGlow.b,
                        0.10f);
                drawRotatedRect(
                        centerX,
                        centerY + mapBounds.height * 0.5f + 2.2f,
                        mapBounds.width + 7f,
                        0.42f,
                        0f,
                        theme.backdropGlow.r,
                        theme.backdropGlow.g,
                        theme.backdropGlow.b,
                        0.10f);
                drawRotatedRect(
                        centerX - mapBounds.width * 0.5f - 2.2f,
                        centerY,
                        mapBounds.height + 7f,
                        0.42f,
                        90f,
                        theme.backdropGlow.r,
                        theme.backdropGlow.g,
                        theme.backdropGlow.b,
                        0.10f);
                drawRotatedRect(
                        centerX + mapBounds.width * 0.5f + 2.2f,
                        centerY,
                        mapBounds.height + 7f,
                        0.42f,
                        90f,
                        theme.backdropGlow.r,
                        theme.backdropGlow.g,
                        theme.backdropGlow.b,
                        0.10f);
                drawRotatedRect(
                        centerX,
                        centerY,
                        Math.min(mapBounds.width, mapBounds.height) + 4.2f,
                        0.18f,
                        45f,
                        theme.accent.r,
                        theme.accent.g,
                        theme.accent.b,
                        0.12f);
                drawRotatedRect(
                        centerX,
                        centerY,
                        Math.min(mapBounds.width, mapBounds.height) + 4.2f,
                        0.18f,
                        -45f,
                        theme.accent.r,
                        theme.accent.g,
                        theme.accent.b,
                        0.12f);
                break;
            case DIAGONAL:
                float diagonalSpan =
                        Math.max(mapBounds.width, mapBounds.height)
                                + Math.max(WORLD_WIDTH, WORLD_HEIGHT) * 2f;
                for (float offset = centerX - diagonalSpan; offset <= centerX + diagonalSpan; offset += 6.2f) {
                    drawRotatedRect(
                            offset,
                            centerY,
                            diagonalSpan * 1.8f,
                            0.64f,
                            28f,
                            theme.backdropGlow.r,
                            theme.backdropGlow.g,
                            theme.backdropGlow.b,
                            0.05f);
                }
                drawRotatedRect(
                        centerX,
                        centerY,
                        diagonalSpan * 1.7f,
                        0.22f,
                        -28f,
                        theme.accent.r,
                        theme.accent.g,
                        theme.accent.b,
                        0.12f);
                break;
            case GRID:
            default:
                float halfWidth = mapBounds.width * 0.55f + 2.4f;
                float halfHeight = mapBounds.height * 0.55f + 2.4f;
                setShapeColor(theme.backdropGlow, 0.08f + pulse * 0.03f);
                shapeRenderer.rect(centerX - halfWidth, centerY - 0.18f, halfWidth * 2f, 0.36f);
                shapeRenderer.rect(centerX - 0.18f, centerY - halfHeight, 0.36f, halfHeight * 2f);
                setShapeColor(theme.accentSoft, 0.05f);
                shapeRenderer.rect(centerX - halfWidth, centerY - halfHeight, halfWidth * 2f, 0.18f);
                shapeRenderer.rect(centerX - halfWidth, centerY + halfHeight - 0.18f, halfWidth * 2f, 0.18f);
                shapeRenderer.rect(centerX - halfWidth, centerY - halfHeight, 0.18f, halfHeight * 2f);
                shapeRenderer.rect(centerX + halfWidth - 0.18f, centerY - halfHeight, 0.18f, halfHeight * 2f);
                break;
        }
    }

    private void drawArenaUnderlay(MapTheme theme) {
        if (currentMap == null) {
            return;
        }

        float pulse = 0.5f + 0.5f * MathUtils.sin(effectClock * 2.2f);

        drawSolidZones(theme.backdropGlow, 0.14f + pulse * 0.05f, 0f, 0f, 0.44f);
        drawSolidZones(PLATFORM_SHADOW, 1f, 0.18f, -0.22f, 0f);
        drawSolidZones(theme.edge, 1f, 0f, 0f, ARENA_EDGE_INSET);

        if (currentMap.getHoleZoneCount() > 0) {
            drawHoleZones(theme.backdropGlow, 0.14f + pulse * 0.06f, 0f, 0f, 0.72f);
            drawHoleZones(theme.edge, 0.90f, 0f, 0f, 0.48f);
            drawHoleZones(VOID, 1f, 0f, 0f, 0f);
        }
    }

    private void drawArenaOverlay(MapTheme theme) {
        if (currentMap == null) {
            return;
        }

        drawArenaFrame(theme);
        drawWarningStripes(theme);
        drawSpawnPads(theme);
        drawFocusMotif(theme);
    }

    private void drawArenaSurface() {
        if (currentMap == null || arenaSurfaceTexture == null) {
            return;
        }

        spriteBatch.setColor(1f, 1f, 1f, 1f);
        spriteBatch.draw(
                arenaSurfaceTexture,
                mapBounds.x,
                mapBounds.y,
                mapBounds.width,
                mapBounds.height);
    }

    private void ensureArenaSurfaceTexture(MapTheme theme) {
        if (currentMap == null || arenaSurfaceTexture != null || Gdx.gl == null) {
            return;
        }

        int textureWidth =
                MathUtils.clamp(
                        MathUtils.ceil(mapBounds.width * ARENA_PIXEL_ART_PIXELS_PER_WORLD_UNIT),
                        ARENA_PIXEL_ART_MIN_TEXTURE_SIZE,
                        ARENA_PIXEL_ART_MAX_TEXTURE_SIZE);
        int textureHeight =
                MathUtils.clamp(
                        MathUtils.ceil(mapBounds.height * ARENA_PIXEL_ART_PIXELS_PER_WORLD_UNIT),
                        ARENA_PIXEL_ART_MIN_TEXTURE_SIZE,
                        ARENA_PIXEL_ART_MAX_TEXTURE_SIZE);

        Pixmap pixmap = new Pixmap(textureWidth, textureHeight, Pixmap.Format.RGBA8888);
        pixmap.setColor(0f, 0f, 0f, 0f);
        pixmap.fill();

        for (int pixelY = 0; pixelY < textureHeight; pixelY++) {
            float worldY = mapBounds.y + (pixelY + 0.5f) * mapBounds.height / textureHeight;
            for (int pixelX = 0; pixelX < textureWidth; pixelX++) {
                float worldX = mapBounds.x + (pixelX + 0.5f) * mapBounds.width / textureWidth;
                if (!currentMap.supports(worldX, worldY)) {
                    continue;
                }

                pixmap.drawPixel(
                        pixelX,
                        pixelY,
                        buildArenaSurfacePixel(theme, worldX, worldY, pixelX, pixelY));
            }
        }

        arenaSurfaceTexture = new Texture(pixmap);
        arenaSurfaceTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pixmap.dispose();
    }

    private int buildArenaSurfacePixel(
            MapTheme theme,
            float worldX,
            float worldY,
            int pixelX,
            int pixelY) {
        float hazardDepth = currentMap.distanceToHazard(worldX, worldY);
        float centerBlend = smooth01(MathUtils.clamp(hazardDepth / 1.9f, 0f, 1f));
        float rimBlend = 1f - smooth01(MathUtils.clamp(hazardDepth / 0.55f, 0f, 1f));
        float checker =
                (((pixelX >> 1) + (pixelY >> 1)) & 1) == 0
                        ? 0.038f
                        : -0.038f;
        float coarseNoise = hash01(pixelX / 3, pixelY / 3, 17);
        float plateNoise = hash01(pixelX / 8, pixelY / 8, 37);
        float microNoise = hash01(pixelX, pixelY, 59);

        float red = MathUtils.lerp(theme.surface.r, theme.center.r, 0.14f + centerBlend * 0.32f);
        float green = MathUtils.lerp(theme.surface.g, theme.center.g, 0.14f + centerBlend * 0.32f);
        float blue = MathUtils.lerp(theme.surface.b, theme.center.b, 0.14f + centerBlend * 0.32f);

        float darkMix = rimBlend * 0.26f;
        if (coarseNoise < 0.16f) {
            darkMix += 0.07f;
        }
        if (checker < 0f) {
            darkMix += 0.05f;
        }

        float softMix = 0.05f + Math.max(0f, checker) * 0.7f;
        if (plateNoise > 0.78f) {
            softMix += 0.09f;
        }
        if (microNoise > 0.88f) {
            softMix += 0.04f;
        }

        float accentMix = computeArenaSurfaceAccentMix(theme.decorStyle, worldX, worldY, pixelX, pixelY);
        float edgeHighlightMix = rimBlend * 0.12f;

        red = MathUtils.lerp(red, theme.edge.r, darkMix);
        green = MathUtils.lerp(green, theme.edge.g, darkMix);
        blue = MathUtils.lerp(blue, theme.edge.b, darkMix);

        red = MathUtils.lerp(red, theme.accentSoft.r, softMix);
        green = MathUtils.lerp(green, theme.accentSoft.g, softMix);
        blue = MathUtils.lerp(blue, theme.accentSoft.b, softMix);

        red = MathUtils.lerp(red, theme.accent.r, accentMix * 0.42f);
        green = MathUtils.lerp(green, theme.accent.g, accentMix * 0.42f);
        blue = MathUtils.lerp(blue, theme.accent.b, accentMix * 0.42f);

        red = MathUtils.lerp(red, theme.backdropGlow.r, edgeHighlightMix);
        green = MathUtils.lerp(green, theme.backdropGlow.g, edgeHighlightMix);
        blue = MathUtils.lerp(blue, theme.backdropGlow.b, edgeHighlightMix);

        return Color.rgba8888(
                MathUtils.clamp(red, 0f, 1f),
                MathUtils.clamp(green, 0f, 1f),
                MathUtils.clamp(blue, 0f, 1f),
                1f);
    }

    private float computeArenaSurfaceAccentMix(
            MapDecorStyle decorStyle,
            float worldX,
            float worldY,
            int pixelX,
            int pixelY) {
        float accentMix = 0f;
        switch (decorStyle) {
            case RUNWAY:
                boolean horizontalRunway = mapBounds.width >= mapBounds.height;
                float runwayCross = horizontalRunway ? worldY - focusPoint.y : worldX - focusPoint.x;
                float runwayAlong = horizontalRunway ? worldX - mapBounds.x : worldY - mapBounds.y;
                if (Math.abs(runwayCross) < 0.18f
                        && ((int) Math.floor(runwayAlong * 3.5f) & 3) < 2) {
                    accentMix += 0.26f;
                }
                if (Math.abs(runwayCross - 0.95f) < 0.10f || Math.abs(runwayCross + 0.95f) < 0.10f) {
                    accentMix += 0.12f;
                }
                break;
            case ORBIT:
                float orbitDistance = Vector2.dst(worldX, worldY, focusPoint.x, focusPoint.y);
                if (isNearRepeatingLine(orbitDistance, 1.65f, 0.07f)) {
                    accentMix += 0.16f;
                }
                if (isNearRepeatingLine(orbitDistance + 0.82f, 3.25f, 0.09f)) {
                    accentMix += 0.08f;
                }
                break;
            case CROSS:
                if (Math.abs(worldX - focusPoint.x) < 0.14f || Math.abs(worldY - focusPoint.y) < 0.14f) {
                    accentMix += 0.20f;
                }
                if (Math.abs(worldX - focusPoint.x) < 0.52f || Math.abs(worldY - focusPoint.y) < 0.52f) {
                    accentMix += 0.07f;
                }
                break;
            case DIAGONAL:
                if (isNearRepeatingLine(worldX + worldY, 1.95f, 0.08f)) {
                    accentMix += 0.16f;
                }
                if (isNearRepeatingLine(worldX - worldY + 0.9f, 3.8f, 0.11f)) {
                    accentMix += 0.08f;
                }
                break;
            case FORTRESS:
                float insetX = Math.min(worldX - mapBounds.x, mapBounds.x + mapBounds.width - worldX);
                float insetY = Math.min(worldY - mapBounds.y, mapBounds.y + mapBounds.height - worldY);
                float inset = Math.min(insetX, insetY);
                if (Math.abs(inset - 0.55f) < 0.06f || Math.abs(inset - 1.15f) < 0.06f) {
                    accentMix += 0.15f;
                }
                if (insetX < 1.55f
                        && insetY < 1.55f
                        && (((pixelX / 3) + (pixelY / 3)) & 1) == 0) {
                    accentMix += 0.08f;
                }
                break;
            case GRID:
            default:
                if (isNearRepeatingLine(worldX - mapBounds.x, 0.82f, 0.05f)
                        || isNearRepeatingLine(worldY - mapBounds.y, 0.82f, 0.05f)) {
                    accentMix += 0.10f;
                }
                if (pixelX % (int) ARENA_PIXEL_ART_DECOR_GRID == 0
                        && pixelY % (int) ARENA_PIXEL_ART_DECOR_GRID == 0) {
                    accentMix += 0.08f;
                }
                break;
        }
        return MathUtils.clamp(accentMix, 0f, 0.36f);
    }

    private boolean isNearRepeatingLine(float value, float spacing, float halfWidth) {
        if (spacing <= 0f) {
            return false;
        }
        float mod = value % spacing;
        if (mod < 0f) {
            mod += spacing;
        }
        return Math.min(mod, spacing - mod) <= halfWidth;
    }

    private float smooth01(float value) {
        float clamped = MathUtils.clamp(value, 0f, 1f);
        return clamped * clamped * (3f - 2f * clamped);
    }

    private float hash01(int x, int y, int seed) {
        int hash = x * 0x1f1f1f1f ^ y * 0x45d9f3b ^ seed * 0x27d4eb2d;
        hash ^= hash >>> 16;
        hash *= 0x7feb352d;
        hash ^= hash >>> 15;
        hash *= 0x846ca68b;
        hash ^= hash >>> 16;
        return (hash & 0x7fffffff) / (float) 0x7fffffff;
    }

    private void drawArenaFrame(MapTheme theme) {
        float inset = 1.08f;
        float segment = Math.min(3.1f, Math.min(mapBounds.width, mapBounds.height) * 0.26f + 0.85f);
        float thickness = 0.18f;
        float left = mapBounds.x - inset;
        float right = mapBounds.x + mapBounds.width + inset - thickness;
        float bottom = mapBounds.y - inset;
        float top = mapBounds.y + mapBounds.height + inset - thickness;
        float centerX = mapBounds.x + mapBounds.width * 0.5f;
        float centerY = mapBounds.y + mapBounds.height * 0.5f;

        setShapeColor(theme.accentSoft, 0.24f);
        shapeRenderer.rect(left, top, segment, thickness);
        shapeRenderer.rect(left, top - segment + thickness, thickness, segment);
        shapeRenderer.rect(right - segment + thickness, top, segment, thickness);
        shapeRenderer.rect(right, top - segment + thickness, thickness, segment);
        shapeRenderer.rect(left, bottom, segment, thickness);
        shapeRenderer.rect(left, bottom, thickness, segment);
        shapeRenderer.rect(right - segment + thickness, bottom, segment, thickness);
        shapeRenderer.rect(right, bottom, thickness, segment);

        setShapeColor(theme.accent, 0.22f);
        shapeRenderer.rect(centerX - 1.05f, top, 2.1f, thickness);
        shapeRenderer.rect(centerX - 1.05f, bottom, 2.1f, thickness);
        shapeRenderer.rect(left, centerY - 1.05f, thickness, 2.1f);
        shapeRenderer.rect(right, centerY - 1.05f, thickness, 2.1f);
    }

    private void drawWarningStripes(MapTheme theme) {
        float stripeDepth = 0.55f;
        float stripeLength = 1.25f;
        boolean bright = true;

        for (float x = mapBounds.x - ARENA_EDGE_INSET;
                x < mapBounds.x + mapBounds.width + ARENA_EDGE_INSET;
                x += stripeLength) {
            if (bright) {
                setShapeColor(WARNING_BRIGHT, 0.92f);
            } else {
                setShapeColor(theme.edge, 0.96f);
            }
            shapeRenderer.rect(x, mapBounds.y + mapBounds.height, stripeLength, stripeDepth);
            shapeRenderer.rect(x, mapBounds.y - stripeDepth, stripeLength, stripeDepth);
            bright = !bright;
        }

        bright = false;
        for (float y = mapBounds.y - ARENA_EDGE_INSET;
                y < mapBounds.y + mapBounds.height + ARENA_EDGE_INSET;
                y += stripeLength) {
            if (bright) {
                setShapeColor(WARNING_BRIGHT, 0.92f);
            } else {
                setShapeColor(theme.edge, 0.96f);
            }
            shapeRenderer.rect(mapBounds.x - stripeDepth, y, stripeDepth, stripeLength);
            shapeRenderer.rect(mapBounds.x + mapBounds.width, y, stripeDepth, stripeLength);
            bright = !bright;
        }
    }

    private void drawSpawnPads(MapTheme theme) {
        if (currentMap == null) {
            return;
        }

        for (int i = 0; i < currentMap.getSpawnCount(); i++) {
            SpawnPoint spawnPoint = currentMap.getSpawn(i);
            float angleDeg = spawnPoint.angleRad * MathUtils.radiansToDegrees + 90f;

            setShapeColor(theme.backdropGlow, 0.16f);
            shapeRenderer.circle(spawnPoint.x, spawnPoint.y, 0.56f, 20);

            drawRotatedRect(
                    spawnPoint.x,
                    spawnPoint.y,
                    1.42f,
                    0.46f,
                    angleDeg,
                    theme.edge.r,
                    theme.edge.g,
                    theme.edge.b,
                    0.34f);
            drawRotatedRect(
                    spawnPoint.x,
                    spawnPoint.y,
                    1.10f,
                    0.18f,
                    angleDeg,
                    theme.accentSoft.r,
                    theme.accentSoft.g,
                    theme.accentSoft.b,
                    0.28f);
            drawOffsetRotatedRect(
                    spawnPoint.x,
                    spawnPoint.y,
                    0.34f,
                    0f,
                    0.34f,
                    0.10f,
                    angleDeg,
                    theme.accent.r,
                    theme.accent.g,
                    theme.accent.b,
                    0.44f);

            setShapeColor(theme.accent, 0.34f);
            shapeRenderer.circle(spawnPoint.x, spawnPoint.y, 0.12f, 14);
        }
    }

    private void drawFocusMotif(MapTheme theme) {
        if (currentMap == null || !currentMap.supports(focusPoint)) {
            return;
        }

        float pulse = 0.5f + 0.5f * MathUtils.sin(effectClock * 3.1f);

        switch (theme.decorStyle) {
            case ORBIT:
                setShapeColor(theme.accentSoft, 0.15f);
                shapeRenderer.circle(focusPoint.x, focusPoint.y, 0.92f + pulse * 0.10f, 28);
                setShapeColor(theme.accent, 0.12f);
                shapeRenderer.circle(focusPoint.x, focusPoint.y, 1.55f + pulse * 0.16f, 28);
                break;
            case RUNWAY:
                float laneAngle = mapBounds.width >= mapBounds.height ? 0f : 90f;
                drawRotatedRect(
                        focusPoint.x,
                        focusPoint.y,
                        2.6f,
                        0.16f,
                        laneAngle,
                        theme.accent.r,
                        theme.accent.g,
                        theme.accent.b,
                        0.30f);
                drawRotatedRect(
                        focusPoint.x,
                        focusPoint.y,
                        0.80f,
                        0.16f,
                        laneAngle + 90f,
                        theme.accentSoft.r,
                        theme.accentSoft.g,
                        theme.accentSoft.b,
                        0.22f);
                break;
            case CROSS:
                drawRotatedRect(
                        focusPoint.x,
                        focusPoint.y,
                        2.8f,
                        0.16f,
                        0f,
                        theme.accent.r,
                        theme.accent.g,
                        theme.accent.b,
                        0.28f);
                drawRotatedRect(
                        focusPoint.x,
                        focusPoint.y,
                        2.8f,
                        0.16f,
                        90f,
                        theme.accent.r,
                        theme.accent.g,
                        theme.accent.b,
                        0.28f);
                break;
            case FORTRESS:
                drawRotatedRect(
                        focusPoint.x,
                        focusPoint.y,
                        2.4f,
                        0.16f,
                        0f,
                        theme.accentSoft.r,
                        theme.accentSoft.g,
                        theme.accentSoft.b,
                        0.24f);
                drawRotatedRect(
                        focusPoint.x,
                        focusPoint.y,
                        2.4f,
                        0.16f,
                        90f,
                        theme.accentSoft.r,
                        theme.accentSoft.g,
                        theme.accentSoft.b,
                        0.24f);
                drawRotatedRect(
                        focusPoint.x,
                        focusPoint.y,
                        1.65f,
                        0.12f,
                        45f,
                        theme.accent.r,
                        theme.accent.g,
                        theme.accent.b,
                        0.24f);
                drawRotatedRect(
                        focusPoint.x,
                        focusPoint.y,
                        1.65f,
                        0.12f,
                        -45f,
                        theme.accent.r,
                        theme.accent.g,
                        theme.accent.b,
                        0.24f);
                break;
            case DIAGONAL:
                drawRotatedRect(
                        focusPoint.x,
                        focusPoint.y,
                        2.5f,
                        0.18f,
                        28f,
                        theme.accent.r,
                        theme.accent.g,
                        theme.accent.b,
                        0.28f);
                drawRotatedRect(
                        focusPoint.x,
                        focusPoint.y,
                        2.5f,
                        0.18f,
                        -28f,
                        theme.accentSoft.r,
                        theme.accentSoft.g,
                        theme.accentSoft.b,
                        0.22f);
                break;
            case GRID:
            default:
                drawRotatedRect(
                        focusPoint.x,
                        focusPoint.y,
                        2.2f,
                        0.16f,
                        0f,
                        theme.accentSoft.r,
                        theme.accentSoft.g,
                        theme.accentSoft.b,
                        0.26f);
                drawRotatedRect(
                        focusPoint.x,
                        focusPoint.y,
                        2.2f,
                        0.16f,
                        90f,
                        theme.accentSoft.r,
                        theme.accentSoft.g,
                        theme.accentSoft.b,
                        0.26f);
                break;
        }

        setShapeColor(theme.accent, 0.38f + pulse * 0.06f);
        shapeRenderer.circle(focusPoint.x, focusPoint.y, 0.13f + pulse * 0.02f, 16);
    }

    private void drawSolidZones(Color color, float alpha, float offsetX, float offsetY, float expansion) {
        if (currentMap == null) {
            return;
        }

        setShapeColor(color, alpha);
        for (int i = 0; i < currentMap.getSolidZoneCount(); i++) {
            currentMap.getSolidZone(i).draw(shapeRenderer, offsetX, offsetY, expansion);
        }
    }

    private void drawHoleZones(Color color, float alpha, float offsetX, float offsetY, float expansion) {
        if (currentMap == null) {
            return;
        }

        setShapeColor(color, alpha);
        for (int i = 0; i < currentMap.getHoleZoneCount(); i++) {
            currentMap.getHoleZone(i).draw(shapeRenderer, offsetX, offsetY, expansion);
        }
    }

    private void setShapeColor(Color color, float alpha) {
        shapeRenderer.setColor(color.r, color.g, color.b, alpha);
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

    private void drawPointPickup() {
        if (!pointPickupActive) {
            return;
        }

        float pulse = 0.5f + 0.5f * MathUtils.sin(effectClock * 8.2f + 0.6f);
        float spinDeg = effectClock * 190f;

        shapeRenderer.setColor(1f, 0.36f, 0.12f, 0.15f + pulse * 0.10f);
        shapeRenderer.circle(
                pointPickupPosition.x,
                pointPickupPosition.y,
                POINT_PICKUP_RADIUS * (2.20f + pulse * 0.30f),
                24);

        drawRotatedRect(
                pointPickupPosition.x,
                pointPickupPosition.y,
                POINT_PICKUP_RADIUS * 3.00f,
                POINT_PICKUP_RADIUS * 0.90f,
                spinDeg,
                1f,
                0.30f,
                0.10f,
                0.92f);

        drawRotatedRect(
                pointPickupPosition.x,
                pointPickupPosition.y,
                POINT_PICKUP_RADIUS * 0.90f,
                POINT_PICKUP_RADIUS * 3.00f,
                -spinDeg * 1.25f,
                1f,
                0.68f,
                0.22f,
                0.98f);

        drawRotatedRect(
                pointPickupPosition.x,
                pointPickupPosition.y,
                POINT_PICKUP_RADIUS * 1.55f,
                POINT_PICKUP_RADIUS * 1.55f,
                -spinDeg * 1.65f,
                1f,
                0.90f,
                0.42f,
                0.98f);

        shapeRenderer.setColor(1f, 0.98f, 0.86f, 1f);
        shapeRenderer.circle(
                pointPickupPosition.x,
                pointPickupPosition.y,
                POINT_PICKUP_RADIUS * 0.28f,
                16);
    }

    private void drawCarEffects() {
        for (int i = 0; i < cars.size; i++) {
            Car car = cars.get(i);
            if (!car.active || car.body == null) {
                continue;
            }

            float angleDeg = car.getRenderAngleDeg();
            Vector2 renderPosition = car.getRenderPosition();
            float centerX = renderPosition.x;
            float centerY = renderPosition.y;
            float carWidth = car.getWidth();
            float carHeight = car.getHeight();
            float carScale = car.getSizeScale();

            if (car.hasGrowthBoost()) {
                float pulse = 0.5f + 0.5f * MathUtils.sin(effectClock * 6f + i * 0.4f);
                shapeRenderer.setColor(1f, 0.84f, 0.24f, 0.12f + pulse * 0.08f);
                shapeRenderer.circle(
                        centerX,
                        centerY,
                        Math.max(carWidth, carHeight) * (0.78f + pulse * 0.08f),
                        24);
            }

            if (car.hasRamCharge()) {
                float pulse = 0.5f + 0.5f * MathUtils.sin(effectClock * 10.2f + i * 0.7f);
                drawRotatedRect(
                        centerX,
                        centerY,
                        carWidth * (1.65f + pulse * 0.06f),
                        carHeight * 0.34f,
                        angleDeg + effectClock * 230f,
                        1f,
                        0.36f,
                        0.12f,
                        0.74f);
                drawRotatedRect(
                        centerX,
                        centerY,
                        carWidth * 0.34f,
                        carHeight * (1.65f + pulse * 0.06f),
                        angleDeg - effectClock * 190f,
                        1f,
                        0.78f,
                        0.22f,
                        0.82f);
            }

            if (car.template.spriteTexture == null) {
                drawFallbackCarBody(car, centerX, centerY, carWidth, carHeight, carScale, angleDeg);
            }
        }
    }

    private void drawCarSprites() {
        spriteBatch.setColor(1f, 1f, 1f, 1f);

        for (int i = 0; i < cars.size; i++) {
            Car car = cars.get(i);
            if (!car.active || car.body == null || car.template.spriteTexture == null) {
                continue;
            }

            Texture sprite = car.template.spriteTexture;
            float angleDeg = car.getRenderAngleDeg();
            float spriteAngleDeg = angleDeg + car.template.visual.spriteRotationOffsetDeg;
            Vector2 renderPosition = car.getRenderPosition();
            float centerX = renderPosition.x;
            float centerY = renderPosition.y;
            float spriteWidth = car.getWidth() * CAR_SPRITE_WIDTH_SCALE;
            float spriteHeight = car.getHeight() * CAR_SPRITE_HEIGHT_SCALE;

            spriteBatch.draw(
                    sprite,
                    centerX - spriteWidth * 0.5f,
                    centerY - spriteHeight * 0.5f,
                    spriteWidth * 0.5f,
                    spriteHeight * 0.5f,
                    spriteWidth,
                    spriteHeight,
                    1f,
                    1f,
                    spriteAngleDeg,
                    car.template.spriteSourceX,
                    car.template.spriteSourceY,
                    car.template.spriteSourceWidth,
                    car.template.spriteSourceHeight,
                    false,
                    false);
        }
    }

    private void drawFallbackCarBody(
            Car car,
            float centerX,
            float centerY,
            float carWidth,
            float carHeight,
            float carScale,
            float angleDeg) {
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

    private void drawDestructionEffects() {
        for (int i = 0; i < destructionEffects.size; i++) {
            DestructionEffect effect = destructionEffects.get(i);
            float progress = 1f - effect.timer / DESTRUCTION_EFFECT_DURATION;
            float alpha = MathUtils.clamp(1f - progress, 0f, 1f);
            float burstRadius = (0.50f + progress * 1.05f) * effect.scale;
            float flashRadius = (0.20f + progress * 0.24f) * effect.scale;

            shapeRenderer.setColor(effect.color.r, effect.color.g, effect.color.b, 0.18f * alpha);
            shapeRenderer.circle(effect.position.x, effect.position.y, burstRadius, 28);

            shapeRenderer.setColor(1f, 0.96f, 0.78f, 0.68f * alpha);
            shapeRenderer.circle(effect.position.x, effect.position.y, flashRadius, 20);

            for (int shard = 0; shard < 6; shard++) {
                float angleDeg = effect.rotationDeg + shard * 60f + progress * 95f;
                float shardDistance = burstRadius * (0.44f + shard * 0.03f);
                float shardX = effect.position.x + MathUtils.cosDeg(angleDeg) * shardDistance;
                float shardY = effect.position.y + MathUtils.sinDeg(angleDeg) * shardDistance;
                float shardWidth = (0.32f - progress * 0.08f) * effect.scale;
                float shardHeight = (0.12f + progress * 0.12f) * effect.scale;

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

    private void drawOffsetCircle(
            float centerX,
            float centerY,
            float offsetX,
            float offsetY,
            float angleDeg,
            float radius,
            float r,
            float g,
            float b,
            float a,
            int segments) {
        float cos = MathUtils.cosDeg(angleDeg);
        float sin = MathUtils.sinDeg(angleDeg);
        float rotatedX = centerX + offsetX * cos - offsetY * sin;
        float rotatedY = centerY + offsetX * sin + offsetY * cos;
        shapeRenderer.setColor(r, g, b, a);
        shapeRenderer.circle(rotatedX, rotatedY, radius, segments);
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
        float playfieldWidth = playfieldHudWidth > 0f ? playfieldHudWidth : hudWidth;
        float sidebarX = playfieldWidth;
        float sidebarWidth = Math.max(0f, hudWidth - playfieldWidth);

        if (sidebarWidth > 0f) {
            drawSidebarPanel(sidebarX, sidebarWidth, hudHeight);
            drawSidebarMinimap(sidebarX, sidebarWidth, hudHeight, currentTheme());
        }

        spriteBatch.begin();

        float topHudLineY = hudHeight - 20f;
        float topHudLineStep = Math.max(26f, hudFont.getLineHeight() + 4f);

        hudFont.setColor(0.96f, 0.92f, 0.82f, 1f);
        hudFont.draw(spriteBatch, "RATASS  |  Roof Sumo", 22f, topHudLineY);

        hudFont.setColor(0.82f, 0.88f, 0.93f, 1f);
        hudFont.draw(
                spriteBatch,
                "Map " + mapProgression.getCurrentMapNumber() + ": " + currentMap.getName(),
                22f,
                topHudLineY - topHudLineStep);

        hudFont.setColor(0.74f, 0.82f, 0.88f, 1f);
        hudFont.draw(spriteBatch, buildControlsText(), 22f, topHudLineY - topHudLineStep * 2f);

        drawSidebarSummary(sidebarX, sidebarWidth, hudHeight);
        drawLeaderboard(sidebarX, sidebarWidth, hudHeight);
        drawSidebarMinimapOverlay(sidebarX, sidebarWidth, hudHeight);
        drawSidebarFooter(sidebarX, sidebarWidth);
        drawCarLabels();

        if (eventCalloutTimer > 0f && !roundOver && preRoundCountdownTimer <= 0f) {
            drawEventCallout(playfieldWidth * 0.5f, hudHeight * 0.80f);
        }

        if (preRoundCountdownTimer > 0f && !roundOver) {
            drawCenteredOverlay(
                    String.valueOf(MathUtils.ceil(preRoundCountdownTimer)),
                    "GET READY",
                    playfieldWidth * 0.5f,
                    hudHeight,
                    new Color(0.98f, 0.94f, 0.84f, 1f),
                    0.62f);
        } else if (roundOver || (playerCar != null && !playerCar.active)) {
            String headline = buildHeadline();
            String subline = buildSubline();

            drawCenteredOverlay(
                    headline,
                    subline,
                    playfieldWidth * 0.5f,
                    hudHeight,
                    winner != null && winner.playerControlled
                            ? new Color(0.98f, 0.96f, 0.80f, 1f)
                            : new Color(0.97f, 0.90f, 0.78f, 1f),
                    0.60f);
        } else if (suddenDeathActive) {
            drawCenteredOverlay(
                    "SUDDEN DEATH",
                    buildSuddenDeathOverlayText(),
                    playfieldWidth * 0.5f,
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
            float centerX,
            float hudHeight,
            Color headlineColor,
            float headlineYFactor) {
        titleFont.setColor(headlineColor);
        glyphLayout.setText(titleFont, headline);
        titleFont.draw(
                spriteBatch,
                headline,
                centerX - glyphLayout.width * 0.5f,
                hudHeight * headlineYFactor);

        hudFont.setColor(0.87f, 0.91f, 0.95f, 1f);
        glyphLayout.setText(hudFont, subline);
        hudFont.draw(
                spriteBatch,
                subline,
                centerX - glyphLayout.width * 0.5f,
                hudHeight * headlineYFactor - Math.max(36f, titleFont.getLineHeight() * 0.52f));
    }

    private void drawEventCallout(float centerX, float y) {
        if (eventCalloutTitle.length() == 0) {
            return;
        }

        float alpha = MathUtils.clamp(eventCalloutTimer / EVENT_CALLOUT_DURATION, 0f, 1f);
        titleFont.setColor(eventCalloutColor.r, eventCalloutColor.g, eventCalloutColor.b, alpha);
        glyphLayout.setText(titleFont, eventCalloutTitle);
        titleFont.draw(
                spriteBatch,
                eventCalloutTitle,
                centerX - glyphLayout.width * 0.5f,
                y);

        if (eventCalloutSubline.length() == 0) {
            return;
        }

        hudFont.setColor(0.96f, 0.95f, 0.90f, alpha);
        glyphLayout.setText(hudFont, eventCalloutSubline);
        hudFont.draw(
                spriteBatch,
                eventCalloutSubline,
                centerX - glyphLayout.width * 0.5f,
                y - Math.max(30f, titleFont.getLineHeight() * 0.46f));
    }

    private void drawSidebarPanel(float sidebarX, float sidebarWidth, float hudHeight) {
        if (sidebarWidth <= 0f) {
            return;
        }

        shapeRenderer.setProjectionMatrix(hudCamera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.04f, 0.06f, 0.08f, 0.95f);
        shapeRenderer.rect(sidebarX, 0f, sidebarWidth, hudHeight);

        shapeRenderer.setColor(0.98f, 0.84f, 0.28f, 0.16f);
        shapeRenderer.rect(sidebarX, hudHeight - 4f, sidebarWidth, 4f);

        shapeRenderer.setColor(1f, 1f, 1f, 0.06f);
        shapeRenderer.rect(sidebarX, 0f, 2f, hudHeight);

        float summaryCardHeight = getSidebarSummaryCardHeight();
        float footerCardHeight = getSidebarFooterCardHeight();

        shapeRenderer.setColor(0.10f, 0.13f, 0.17f, 0.95f);
        shapeRenderer.rect(
                sidebarX + SIDEBAR_CARD_MARGIN,
                hudHeight - 22f - summaryCardHeight,
                sidebarWidth - SIDEBAR_CARD_MARGIN * 2f,
                summaryCardHeight);
        shapeRenderer.rect(
                sidebarX + SIDEBAR_CARD_MARGIN,
                10f,
                sidebarWidth - SIDEBAR_CARD_MARGIN * 2f,
                footerCardHeight);
        if (getSidebarMinimapBounds(sidebarX, sidebarWidth, hudHeight, sidebarMinimapBounds)) {
            shapeRenderer.rect(
                    sidebarMinimapBounds.x,
                    sidebarMinimapBounds.y,
                    sidebarMinimapBounds.width,
                    sidebarMinimapBounds.height);
        }
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawSidebarSummary(float sidebarX, float sidebarWidth, float hudHeight) {
        if (sidebarWidth <= 0f) {
            return;
        }

        float x = sidebarX + 18f;
        float contentWidth = sidebarWidth - 36f;
        float y = hudHeight - 24f;
        float hudLineStep = Math.max(24f, hudFont.getLineHeight() + 3f);
        float leaderboardLineStep = Math.max(17f, leaderboardFont.getLineHeight() + 2f);

        hudFont.setColor(0.98f, 0.95f, 0.84f, 1f);
        hudFont.draw(spriteBatch, "Classification", x, y);

        leaderboardFont.setColor(0.76f, 0.84f, 0.90f, 1f);
        y -= hudLineStep;
        leaderboardFont.draw(
                spriteBatch,
                "Map " + mapProgression.getCurrentMapNumber() + "/" + mapProgression.getMapCount()
                        + "  |  Round " + roundNumber,
                x,
                y);
        y -= leaderboardLineStep;
        leaderboardFont.draw(spriteBatch, currentMap.getName(), x, y);
        y -= leaderboardLineStep;
        leaderboardFont.draw(
                spriteBatch,
                "Cars " + getAliveCarCount() + "/" + cars.size + "  |  You " + roster.first().totalPoints + " pts",
                x,
                y);

        leaderboardFont.setColor(0.94f, 0.84f, 0.50f, 1f);
        y -= leaderboardLineStep;
        leaderboardFont.draw(
                spriteBatch,
                buildSidebarStateText(),
                x,
                y,
                contentWidth,
                Align.left,
                true);
    }

    private void drawLeaderboard(float sidebarX, float sidebarWidth, float hudHeight) {
        leaderboardEntries.clear();
        leaderboardEntries.addAll(roster);
        leaderboardEntries.sort(leaderboardComparator);

        SidebarLeaderboardLayout layout = getSidebarLeaderboardLayout(sidebarWidth, hudHeight);
        float x = sidebarX + SIDEBAR_CONTENT_MARGIN;
        float y = getSidebarLeaderboardStartY(hudHeight);
        BitmapFont font = layout.compact ? labelFont : leaderboardFont;

        if (layout.headingStep > 0f) {
            float rightX = sidebarX + sidebarWidth - SIDEBAR_CONTENT_MARGIN;
            font.setColor(0.98f, 0.95f, 0.84f, 1f);
            font.draw(spriteBatch, "Rank", x, y);
            glyphLayout.setText(font, "Points");
            font.draw(spriteBatch, "Points", rightX - glyphLayout.width, y);
            y -= layout.headingStep;
        }

        for (int i = 0; i < leaderboardEntries.size; i++) {
            CarTemplate template = leaderboardEntries.get(i);
            boolean active = template.currentCar != null && template.currentCar.active;
            int columnIndex = i / layout.rowsPerColumn;
            int rowIndex = i % layout.rowsPerColumn;
            float columnX = x + columnIndex * (layout.columnWidth + layout.columnGap);
            float rowY = y - rowIndex * layout.rowStep;

            String left = buildLeaderboardLeftLabel(template, i + 1);
            String right = buildLeaderboardRightLabel(template, active, layout);

            if (template.playerControlled) {
                font.setColor(1f, 0.94f, 0.54f, 1f);
            } else if (!active && !roundOver) {
                font.setColor(0.66f, 0.72f, 0.78f, 1f);
            } else {
                font.setColor(0.84f, 0.90f, 0.94f, 1f);
            }

            if (layout.showPoints) {
                glyphLayout.setText(font, right);
                float rightWidth = glyphLayout.width;
                float rightX = columnX + layout.columnWidth - rightWidth;
                float leftWidth = Math.max(1f, layout.columnWidth - rightWidth - 6f);
                left = truncateTextToWidth(font, left, leftWidth);
                font.draw(spriteBatch, left, columnX, rowY);
                font.draw(spriteBatch, right, rightX, rowY);
            } else {
                left = truncateTextToWidth(font, left, layout.columnWidth);
                font.draw(spriteBatch, left, columnX, rowY);
            }
        }
    }

    private void drawSidebarMinimap(float sidebarX, float sidebarWidth, float hudHeight, MapTheme theme) {
        if (currentMap == null
                || !getSidebarMinimapBounds(sidebarX, sidebarWidth, hudHeight, sidebarMinimapBounds)) {
            return;
        }

        currentMap.getBounds(mapBounds);

        float innerX = sidebarMinimapBounds.x + SIDEBAR_MINIMAP_PADDING;
        float innerY = sidebarMinimapBounds.y + SIDEBAR_MINIMAP_PADDING;
        float innerWidth = sidebarMinimapBounds.width - SIDEBAR_MINIMAP_PADDING * 2f;
        float innerHeight =
                sidebarMinimapBounds.height
                        - SIDEBAR_MINIMAP_PADDING * 2f
                        - SIDEBAR_MINIMAP_LABEL_HEIGHT;
        if (innerWidth <= 0f || innerHeight <= 0f || mapBounds.width <= 0f || mapBounds.height <= 0f) {
            return;
        }

        float minimapScale = Math.min(innerWidth / mapBounds.width, innerHeight / mapBounds.height);
        if (minimapScale <= 0f) {
            return;
        }

        float minimapWidth = mapBounds.width * minimapScale;
        float minimapHeight = mapBounds.height * minimapScale;
        float offsetX = innerX + (innerWidth - minimapWidth) * 0.5f - mapBounds.x * minimapScale;
        float offsetY = innerY + (innerHeight - minimapHeight) * 0.5f - mapBounds.y * minimapScale;

        shapeRenderer.setProjectionMatrix(hudCamera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        shapeRenderer.setColor(theme.backdropBase.r, theme.backdropBase.g, theme.backdropBase.b, 0.94f);
        shapeRenderer.rect(innerX, innerY, innerWidth, innerHeight);

        shapeRenderer.setColor(theme.backdropGlow.r, theme.backdropGlow.g, theme.backdropGlow.b, 0.16f);
        shapeRenderer.rect(
                innerX + (innerWidth - minimapWidth) * 0.5f,
                innerY + (innerHeight - minimapHeight) * 0.5f,
                minimapWidth,
                minimapHeight);

        shapeRenderer.setColor(theme.accent.r, theme.accent.g, theme.accent.b, 0.26f);
        shapeRenderer.rect(innerX, innerY, innerWidth, 2f);
        shapeRenderer.rect(innerX, innerY + innerHeight - 2f, innerWidth, 2f);
        shapeRenderer.rect(innerX, innerY, 2f, innerHeight);
        shapeRenderer.rect(innerX + innerWidth - 2f, innerY, 2f, innerHeight);
        shapeRenderer.end();

        minimapTransform
                .idt()
                .translate(offsetX, offsetY, 0f)
                .scale(minimapScale, minimapScale, 1f);
        shapeRenderer.setTransformMatrix(minimapTransform);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        setShapeColor(theme.edge, 0.92f);
        for (int i = 0; i < currentMap.getSolidZoneCount(); i++) {
            currentMap.getSolidZone(i).draw(shapeRenderer, 0f, 0f, 0.24f);
        }

        setShapeColor(theme.surface, 0.96f);
        for (int i = 0; i < currentMap.getSolidZoneCount(); i++) {
            currentMap.getSolidZone(i).draw(shapeRenderer, 0f, 0f, 0f);
        }

        if (currentMap.getHoleZoneCount() > 0) {
            setShapeColor(theme.edge, 0.80f);
            for (int i = 0; i < currentMap.getHoleZoneCount(); i++) {
                currentMap.getHoleZone(i).draw(shapeRenderer, 0f, 0f, 0.18f);
            }

            setShapeColor(VOID, 1f);
            for (int i = 0; i < currentMap.getHoleZoneCount(); i++) {
                currentMap.getHoleZone(i).draw(shapeRenderer, 0f, 0f, 0f);
            }
        }

        if (growthPickupActive) {
            float pickupRadius = 3.1f / minimapScale;
            shapeRenderer.setColor(1f, 0.88f, 0.28f, 0.94f);
            shapeRenderer.circle(growthPickupPosition.x, growthPickupPosition.y, pickupRadius, 18);
        }

        for (int i = 0; i < cars.size; i++) {
            Car car = cars.get(i);
            if (!car.active || car.body == null) {
                continue;
            }

            Vector2 position = car.getRenderPosition();
            float markerRadius =
                    (SIDEBAR_MINIMAP_MARKER_RADIUS
                                    + (car.hasGrowthBoost() ? SIDEBAR_MINIMAP_BIG_CAR_RADIUS_BOOST : 0f))
                            / minimapScale;

            shapeRenderer.setColor(0.03f, 0.04f, 0.05f, 0.90f);
            shapeRenderer.circle(position.x, position.y, markerRadius + 1.2f / minimapScale, 20);

            if (car.playerControlled) {
                shapeRenderer.setColor(1f, 0.95f, 0.62f, 0.92f);
                shapeRenderer.circle(
                        position.x,
                        position.y,
                        SIDEBAR_MINIMAP_PLAYER_RING_RADIUS / minimapScale,
                        24);
            }

            shapeRenderer.setColor(car.color.r, car.color.g, car.color.b, 0.98f);
            shapeRenderer.circle(position.x, position.y, markerRadius, 20);
        }

        shapeRenderer.end();
        shapeRenderer.setTransformMatrix(hudTransform.idt());
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawSidebarMinimapOverlay(float sidebarX, float sidebarWidth, float hudHeight) {
        if (!getSidebarMinimapBounds(sidebarX, sidebarWidth, hudHeight, sidebarMinimapBounds)) {
            return;
        }

        leaderboardFont.setColor(0.98f, 0.95f, 0.84f, 1f);
        leaderboardFont.draw(
                spriteBatch,
                "Arena Scan",
                sidebarMinimapBounds.x + SIDEBAR_CONTENT_MARGIN - SIDEBAR_CARD_MARGIN,
                sidebarMinimapBounds.y + sidebarMinimapBounds.height - SIDEBAR_MINIMAP_PADDING);
    }

    private String buildLeaderboardRowState(CarTemplate template, boolean active) {
        if (roundOver) {
            return "+" + template.lastRoundAwardedPoints;
        }
        if (template.roundFinishPosition > 0) {
            return "OUT#" + template.roundFinishPosition;
        }
        if (active && template.currentCar.hasGrowthBoost()) {
            return "BIG";
        }
        return active ? "IN" : "";
    }

    private void drawSidebarFooter(float sidebarX, float sidebarWidth) {
        if (sidebarWidth <= 0f) {
            return;
        }

        leaderboardFont.setColor(0.93f, 0.84f, 0.49f, 1f);
        leaderboardFont.draw(
                spriteBatch,
                buildObjectiveText(),
                sidebarX + 18f,
                10f + getSidebarFooterCardHeight() - 12f,
                sidebarWidth - 36f,
                Align.left,
                true);
    }

    private void drawCarLabels() {
        for (int i = 0; i < cars.size; i++) {
            Car car = cars.get(i);
            if (!car.active || car.body == null) {
                continue;
            }

            Vector2 renderPosition = car.getRenderPosition();
            carLabelProjection.set(
                    renderPosition.x,
                    renderPosition.y + car.getHeight() * 0.92f,
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
        return "WASD/Arrows: drive   Space: handbrake/drift   Space+W+A/D: spin   Esc: quit";
    }

    private float getSidebarSummaryCardHeight() {
        return Math.max(86f, hudFont.getLineHeight() + leaderboardFont.getLineHeight() * 5f);
    }

    private float getSidebarFooterCardHeight() {
        return Math.max(82f, leaderboardFont.getLineHeight() * 4.3f);
    }

    private SidebarLeaderboardLayout getSidebarLeaderboardLayout(float sidebarWidth, float hudHeight) {
        float availableHeight =
                getSidebarLeaderboardStartY(hudHeight)
                        - (10f
                                + getSidebarFooterCardHeight()
                                + SIDEBAR_MINIMAP_GAP * 2f
                                + SIDEBAR_MINIMAP_MIN_HEIGHT);
        float contentWidth = Math.max(1f, sidebarWidth - SIDEBAR_CONTENT_MARGIN * 2f);
        int entryCount = Math.max(0, roster.size);
        if (entryCount == 0) {
            return new SidebarLeaderboardLayout(1, 1, false, false, 0f, 0f, 0f, contentWidth, 0f);
        }

        float headingStep = getSidebarLeaderboardHeadingStep();
        float rowStep = getSidebarLeaderboardRowStep();
        float singleColumnHeight = headingStep + entryCount * rowStep;
        if (singleColumnHeight <= availableHeight) {
            return new SidebarLeaderboardLayout(
                    1,
                    entryCount,
                    false,
                    true,
                    headingStep,
                    rowStep,
                    0f,
                    contentWidth,
                    singleColumnHeight);
        }

        float compactRowStep = getSidebarCompactLeaderboardRowStep();
        int columns = 1;
        int rowsPerColumn = entryCount;
        int maxColumns = Math.min(SIDEBAR_LEADERBOARD_MAX_COLUMNS, Math.max(1, entryCount));
        for (int candidate = 2; candidate <= maxColumns; candidate++) {
            columns = candidate;
            rowsPerColumn = MathUtils.ceil((float) entryCount / candidate);
            if (rowsPerColumn * compactRowStep <= availableHeight) {
                break;
            }
        }

        float columnGap = columns > 1 ? SIDEBAR_LEADERBOARD_COLUMN_GAP : 0f;
        float columnWidth =
                Math.max(1f, (contentWidth - columnGap * (columns - 1)) / Math.max(1, columns));
        boolean showPoints = columnWidth >= SIDEBAR_LEADERBOARD_POINTS_MIN_WIDTH;
        return new SidebarLeaderboardLayout(
                columns,
                rowsPerColumn,
                true,
                showPoints,
                0f,
                compactRowStep,
                columnGap,
                columnWidth,
                rowsPerColumn * compactRowStep);
    }

    private float getSidebarLeaderboardStartY(float hudHeight) {
        return hudHeight - 22f - getSidebarSummaryCardHeight() - 16f;
    }

    private float getSidebarLeaderboardHeadingStep() {
        return Math.max(18f, leaderboardFont.getLineHeight() + 2f);
    }

    private float getSidebarLeaderboardRowStep() {
        return Math.max(16f, leaderboardFont.getLineHeight() + 1f);
    }

    private float getSidebarCompactLeaderboardRowStep() {
        return Math.max(SIDEBAR_LEADERBOARD_COMPACT_ROW_STEP, labelFont.getLineHeight() + 1f);
    }

    private float getSidebarLeaderboardBottomY(float sidebarWidth, float hudHeight) {
        SidebarLeaderboardLayout layout = getSidebarLeaderboardLayout(sidebarWidth, hudHeight);
        return getSidebarLeaderboardStartY(hudHeight) - layout.totalHeight;
    }

    private String buildLeaderboardLeftLabel(CarTemplate template, int rank) {
        return rank + ". " + (template.playerControlled ? "YOU" : template.name);
    }

    private String buildLeaderboardRightLabel(
            CarTemplate template,
            boolean active,
            SidebarLeaderboardLayout layout) {
        if (layout.compact) {
            return String.valueOf(template.totalPoints);
        }

        String right = template.totalPoints + " pts";
        String rowState = buildLeaderboardRowState(template, active);
        if (!rowState.isEmpty()) {
            right += " " + rowState;
        }
        return right;
    }

    private String truncateTextToWidth(BitmapFont font, String text, float maxWidth) {
        if (text == null || text.length() == 0 || maxWidth <= 0f) {
            return "";
        }

        glyphLayout.setText(font, text);
        if (glyphLayout.width <= maxWidth) {
            return text;
        }

        glyphLayout.setText(font, "...");
        if (glyphLayout.width > maxWidth) {
            return "";
        }

        for (int end = text.length() - 1; end > 0; end--) {
            String candidate = text.substring(0, end).trim() + "...";
            glyphLayout.setText(font, candidate);
            if (glyphLayout.width <= maxWidth) {
                return candidate;
            }
        }
        return "";
    }

    private boolean getSidebarMinimapBounds(
            float sidebarX,
            float sidebarWidth,
            float hudHeight,
            Rectangle out) {
        float top = getSidebarLeaderboardBottomY(sidebarWidth, hudHeight) - SIDEBAR_MINIMAP_GAP;
        float bottom = 10f + getSidebarFooterCardHeight() + SIDEBAR_MINIMAP_GAP;
        float height = top - bottom;
        if (sidebarWidth <= 0f
                || sidebarWidth <= SIDEBAR_CARD_MARGIN * 2f
                || height < SIDEBAR_MINIMAP_MIN_HEIGHT) {
            out.set(0f, 0f, 0f, 0f);
            return false;
        }

        out.set(
                sidebarX + SIDEBAR_CARD_MARGIN,
                bottom,
                sidebarWidth - SIDEBAR_CARD_MARGIN * 2f,
                height);
        return true;
    }

    private int getAliveCarCount() {
        int aliveCars = 0;
        for (int i = 0; i < cars.size; i++) {
            if (cars.get(i).active) {
                aliveCars++;
            }
        }
        return aliveCars;
    }

    private String buildStatusText() {
        StringBuilder builder = new StringBuilder();
        builder.append("You ").append(roster.first().totalPoints).append(" pts")
                .append("  |  Round ").append(roundNumber)
                .append("  |  Cars left: ").append(getAliveCarCount()).append("/").append(cars.size)
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

    private String buildSidebarStateText() {
        if (preRoundCountdownTimer > 0f) {
            return "Starts in " + MathUtils.ceil(preRoundCountdownTimer) + "s";
        }

        if (roundTimedOut) {
            return "Arena wipe resolved";
        }

        if (roundOver) {
            return "Round over";
        }

        if (boostedCar != null && boostedCar.active) {
            String owner = boostedCar.playerControlled ? "YOU" : boostedCar.name;
            if (suddenDeathActive) {
                return owner + " BIG  |  wipe " + getRoundTimeoutSecondsLeft() + "s";
            }
            return owner + " BIG " + MathUtils.ceil(growthBoostTimer)
                    + "s  |  SD " + getSuddenDeathSecondsLeft() + "s";
        }

        if (suddenDeathActive) {
            return "Sudden death  |  wipe " + getRoundTimeoutSecondsLeft() + "s";
        }

        if (growthPickupActive) {
            return "Mass core live  |  SD " + getSuddenDeathSecondsLeft() + "s";
        }

        return "Sudden death in " + getSuddenDeathSecondsLeft() + "s";
    }

    private String buildObjectiveText() {
        if (preRoundCountdownTimer > 0f) {
            return "Prepare for the horn. First car out gets 1 point, the mass core makes one car huge, sudden death starts at 20s, and the arena wipes at 35s.";
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
                    + " more seconds. "
                    + "Sudden death in "
                    + getSuddenDeathSecondsLeft()
                    + "s, wipe in "
                    + getRoundTimeoutSecondsLeft()
                    + "s.";
        }

        if (growthPickupActive) {
            return currentMap.getName() + ": mass core live. Sudden death in "
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
        if (currentMap == null) {
            return DEFAULT_MAP_THEME;
        }

        String mapId = currentMap.getId();
        if (mapId == null) {
            return DEFAULT_MAP_THEME;
        }

        if (mapId.startsWith("crosswind-junction")) {
            return CROSSWIND_THEME;
        }
        if (mapId.startsWith("split-shift")) {
            return SPLIT_SHIFT_THEME;
        }
        if (mapId.startsWith("donut-bowl")) {
            return DONUT_BOWL_THEME;
        }
        if (mapId.startsWith("twin-crater")) {
            return TWIN_CRATER_THEME;
        }
        if (mapId.startsWith("frame-ring")) {
            return FRAME_RING_THEME;
        }
        if (mapId.startsWith("causeway-clash")) {
            return CAUSEWAY_THEME;
        }
        if (mapId.startsWith("core-breach")) {
            return CORE_BREACH_THEME;
        }
        if (mapId.startsWith("pillbox-lanes")) {
            return PILLBOX_THEME;
        }
        if (mapId.startsWith("satellite-crown")) {
            return SATELLITE_THEME;
        }
        if (mapId.startsWith("boiler-deck")) {
            return BOILER_DECK_THEME;
        }
        if ("knife-edge".equals(mapId)) {
            return KNIFE_EDGE_THEME;
        }
        if ("deadfall".equals(mapId)) {
            return DEADFALL_THEME;
        }
        if ("switchback".equals(mapId)) {
            return SWITCHBACK_THEME;
        }
        if ("last-stand".equals(mapId)) {
            return LAST_STAND_THEME;
        }

        return DEFAULT_MAP_THEME;
    }

    private static MapTheme mapTheme(
            MapDecorStyle decorStyle,
            String backdropBase,
            String backdropLine,
            String backdropGlow,
            String edge,
            String surface,
            String center,
            String accent,
            String accentSoft) {
        return new MapTheme(
                decorStyle,
                parseThemeColor(backdropBase),
                parseThemeColor(backdropLine),
                parseThemeColor(backdropGlow),
                parseThemeColor(edge),
                parseThemeColor(surface),
                parseThemeColor(center),
                parseThemeColor(accent),
                parseThemeColor(accentSoft));
    }

    private static Color parseThemeColor(String value) {
        return Color.valueOf(value.length() == 6 ? value + "ff" : value);
    }

    private static String loadConfiguredThemeName() {
        String value = loadConfiguredProperty(THEME_PROPERTY);
        if (value == null) {
            return DEFAULT_THEME_NAME;
        }
        String normalized = value.trim();
        return normalized.length() == 0 ? DEFAULT_THEME_NAME : normalized;
    }

    private static boolean loadConfiguredBooleanProperty(String propertyName, boolean defaultValue) {
        String value = loadConfiguredProperty(propertyName);
        if (value == null) {
            return defaultValue;
        }

        String normalized = value.trim();
        if ("true".equalsIgnoreCase(normalized)
                || "1".equals(normalized)
                || "yes".equalsIgnoreCase(normalized)
                || "on".equalsIgnoreCase(normalized)) {
            return true;
        }
        if ("false".equalsIgnoreCase(normalized)
                || "0".equals(normalized)
                || "no".equalsIgnoreCase(normalized)
                || "off".equalsIgnoreCase(normalized)) {
            return false;
        }
        return defaultValue;
    }

    private static String loadConfiguredProperty(String propertyName) {
        Properties properties = new Properties();

        InputStream resourceStream = RatassGame.class.getClassLoader().getResourceAsStream(GAME_PROPERTIES_RESOURCE);
        if (resourceStream != null) {
            try (InputStream input = resourceStream) {
                properties.load(input);
                return properties.getProperty(propertyName);
            } catch (IOException exception) {
                // Fall through to the filesystem fallback.
            }
        }

        try (InputStream input = new FileInputStream(GAME_PROPERTIES_FILE)) {
            properties.clear();
            properties.load(input);
            return properties.getProperty(propertyName);
        } catch (IOException exception) {
            return null;
        }
    }

    private void disposeSound(Sound sound) {
        if (sound != null) {
            sound.dispose();
        }
    }

    private void disposeTexture(Texture texture) {
        if (texture != null) {
            texture.dispose();
        }
    }

    @Override
    public void dispose() {
        if (world != null) {
            world.dispose();
        }
        for (int i = 0; i < roster.size; i++) {
            CarTemplate template = roster.get(i);
            if (template.ownsSpriteTexture) {
                disposeTexture(template.spriteTexture);
            }
        }
        disposeTexture(arenaSurfaceTexture);
        disposeThemeCarVisualTextures();
        disposeTexture(themeCarsTexture);
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
        if (spriteBatch != null) {
            spriteBatch.dispose();
        }
        if (hudFont != null) {
            hudFont.dispose();
        }
        if (titleFont != null) {
            titleFont.dispose();
        }
        if (leaderboardFont != null) {
            leaderboardFont.dispose();
        }
        if (labelFont != null) {
            labelFont.dispose();
        }
        disposeSound(impactSound);
        disposeSound(pickupSound);
        disposeSound(destructionSound);
        disposeSound(countdownSound);
        disposeSound(roundStartSound);
        disposeSound(suddenDeathSound);
        disposeSound(timeoutSound);
    }

    private void announceRamImpact(Car attacker, Car victim) {
        if (attacker == null || victim == null) {
            return;
        }

        if (attacker.playerControlled) {
            announceEvent(
                    "SLAM",
                    victim.name + " got launched.",
                    new Color(1f, 0.56f, 0.18f, 1f));
            return;
        }

        if (victim.playerControlled) {
            announceEvent(
                    "HEAVY HIT",
                    attacker.name + " cracked your line.",
                    new Color(1f, 0.38f, 0.24f, 1f));
            return;
        }

        if (getAliveCarCount() <= 4) {
            announceEvent(
                    "BRAWL",
                    attacker.name + " smashed " + victim.name + ".",
                    new Color(1f, 0.62f, 0.20f, 1f));
        }
    }

    private void announceElimination(Car eliminated) {
        if (eliminated == null) {
            return;
        }

        CarTemplate attacker = findTemplateByVehicleId(eliminated.getLastAttackerId());
        if (eliminated.playerControlled) {
            announceEvent(
                    "YOU'RE OUT",
                    attacker != null
                            ? attacker.name + " rang you out."
                            : "You went over the edge.",
                    new Color(1f, 0.34f, 0.22f, 1f));
            return;
        }

        if (attacker != null && attacker.playerControlled) {
            announceEvent(
                    "RING OUT",
                    eliminated.name + " is gone.",
                    new Color(1f, 0.86f, 0.34f, 1f));
            return;
        }

        if (getAliveCarCount() <= 4) {
            announceEvent(
                    "KNOCKOUT",
                    eliminated.name + " dropped off the roof.",
                    new Color(0.95f, 0.88f, 0.70f, 1f));
        }
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

            float speedIntoCollisionA = Math.max(0f, velocityA.dot(normal));
            float speedIntoCollisionB = Math.max(0f, -velocityB.dot(normal));

            float impactStrength = totalNormalImpulse + closingSpeed * Car.IMPACT_STRENGTH_SPEED_FACTOR;
            if (!roundOver && preRoundCountdownTimer <= 0f) {
                playImpactSound(impactStrength);
            }

            if (suddenDeathActive) {
                if (carA.hasGrowthBoost() != carB.hasGrowthBoost()) {
                    queueCollisionElimination(carA.hasGrowthBoost() ? carB : carA);
                } else {
                    float speedA = Math.max(0f, velocityA.dot(normal));
                    float speedB = Math.max(0f, -velocityB.dot(normal));
                    if (Math.abs(speedA - speedB) <= SUDDEN_DEATH_TIE_SPEED_MARGIN) {
                        speedA = velocityA.len();
                        speedB = velocityB.len();
                    }

                    queueCollisionElimination(speedA >= speedB ? carB : carA);
                }
            }

            boolean carADominant = speedIntoCollisionA > speedIntoCollisionB + 0.24f;
            boolean carBDominant = speedIntoCollisionB > speedIntoCollisionA + 0.24f;
            boolean carARamHit = carADominant && carA.hasRamCharge();
            boolean carBRamHit = carBDominant && carB.hasRamCharge();
            boolean carAEmpoweredHit = carARamHit || (carADominant && carA.hasGrowthBoost());
            boolean carBEmpoweredHit = carBRamHit || (carBDominant && carB.hasGrowthBoost());

            carA.absorbCollision(
                    normal,
                    -1f,
                    impactStrength,
                    speedIntoCollisionB,
                    carA.getCollisionReboundMultiplierAgainst(carB));
            carB.absorbCollision(
                    normal,
                    1f,
                    impactStrength,
                    speedIntoCollisionA,
                    carB.getCollisionReboundMultiplierAgainst(carA));
            carA.receiveCombatHit(carB, currentMap, impactStrength, speedIntoCollisionB, carBEmpoweredHit);
            carB.receiveCombatHit(carA, currentMap, impactStrength, speedIntoCollisionA, carAEmpoweredHit);

            if (carARamHit) {
                carA.consumeRamCharge();
                announceRamImpact(carA, carB);
            }
            if (carBRamHit) {
                carB.consumeRamCharge();
                announceRamImpact(carB, carA);
            }

        }
    }

    private static final class Car implements AiVehicleView {
        private static final float WIDTH = 1.12f;
        private static final float HEIGHT = 1.94f;
        private static final float HALF_WIDTH = WIDTH * 0.5f;
        private static final float HALF_HEIGHT = HEIGHT * 0.5f;
        private static final float FIXTURE_DENSITY = 1.3f;
        private static final float FIXTURE_FRICTION = 0.10f;
        private static final float FIXTURE_RESTITUTION = 0.46f;
        private static final float GROWTH_SCALE = 1.40f;
        private static final float GROWTH_MASS_MULTIPLIER = 10f;
        private static final float DRIVE_FORCE = 192f;
        private static final float REVERSE_FORCE = 148f;
        private static final float BRAKE_FORCE = 276f;
        private static final float BRAKE_SPEED_THRESHOLD = 0.45f;
        private static final float PLAYER_TURN_TORQUE = 48f;
        private static final float AI_TURN_TORQUE = 38f;
        private static final float LATERAL_GRIP = 0.72f;
        private static final float ANGULAR_GRIP = 0.23f;
        private static final float FORWARD_DRAG = 1.55f;
        private static final float HANDBRAKE_LATERAL_GRIP_MULTIPLIER = 0.26f;
        private static final float HANDBRAKE_ANGULAR_GRIP_MULTIPLIER = 0.42f;
        private static final float HANDBRAKE_PIVOT_ANGULAR_GRIP_MULTIPLIER = 0.18f;
        private static final float HANDBRAKE_FORWARD_DRAG_MULTIPLIER = 1.18f;
        private static final float HANDBRAKE_DRIVE_FORCE_MULTIPLIER = 0.78f;
        private static final float HANDBRAKE_TURN_TORQUE_MULTIPLIER = 1.42f;
        private static final float HANDBRAKE_STEERING_FLOOR = 0.92f;
        private static final float HANDBRAKE_PIVOT_SPEED_SQ = 1.35f;
        private static final float HANDBRAKE_PIVOT_MIN_TURN = 0.20f;
        private static final float HANDBRAKE_PIVOT_DRIVE_FORCE_MULTIPLIER = 0.18f;
        private static final float HANDBRAKE_PIVOT_TURN_TORQUE_MULTIPLIER = 3.15f;
        private static final float HANDBRAKE_PIVOT_LINEAR_DAMPING = 0.74f;
        private static final float HANDBRAKE_PIVOT_STEERING_STRENGTH = 1.12f;
        private static final float MAX_SPEED = 30.4f;
        private static final float GROWTH_TURN_MULTIPLIER = 0.90f;
        private static final float MAX_GROWTH_SPEED_MULTIPLIER = 1.06f;
        private static final float MIN_COLLISION_RESPONSE_IMPULSE = 3.6f;
        private static final float MIN_COLLISION_RESPONSE_SPEED = 0.75f;
        private static final float COLLISION_BOUNCE_SCALE = 2f;
        private static final float IMPACT_STRENGTH_SPEED_FACTOR = 3.9f;
        private static final float COLLISION_REBOUND_IMPULSE_FACTOR = 0.96f;
        private static final float INCOMING_SPEED_REBOUND_BOOST_FACTOR = 0.11f;
        private static final float MAX_INCOMING_SPEED_REBOUND_BOOST = 1.10f;
        private static final float MAX_COLLISION_REBOUND_IMPULSE = 22f;
        private static final float MAX_STORED_COLLISION_IMPULSE = 30f;
        private static final float IMPACT_SLIDE_DURATION = 0.54f;
        private static final float IMPACT_SLIDE_REFERENCE = 18f;
        private static final float BOOSTED_VS_NORMAL_REBOUND_MULTIPLIER = 0.85f;
        private static final float NORMAL_VS_BOOSTED_REBOUND_MULTIPLIER = 31.5f;
        private static final float BOOSTED_VS_BOOSTED_REBOUND_MULTIPLIER = 1.70f;
        private static final float RAM_CHARGE_ATTACKER_REBOUND_MULTIPLIER = 0.70f;
        private static final float RAM_CHARGE_VICTIM_REBOUND_MULTIPLIER = 1.85f;
        private static final float RECENT_IMPACT_DURATION = 1.8f;
        private static final float CONTROL_LOCK_REFERENCE = 20f;
        private static final float MAX_CONTROL_LOCK_DURATION = 0.58f;
        private static final float EDGE_FINISH_DISTANCE = 2.5f;
        private static final float BASE_EDGE_FINISH_IMPULSE = 3.2f;
        private static final float EMPOWERED_EDGE_FINISH_MULTIPLIER = 1.75f;

        private final CarTemplate template;
        private final String name;
        private final boolean playerControlled;
        private final Color color;
        private final CarAiController aiController;
        private final Vector2 forwardAxis = new Vector2();
        private final Vector2 sidewaysAxis = new Vector2();
        private final Vector2 working = new Vector2();
        private final Vector2 pendingImpactImpulse = new Vector2();
        private final Vector2 impactRecoveryPoint = new Vector2();
        private final Vector2 impactOutward = new Vector2();
        private final Vector2 previousRenderPosition = new Vector2();
        private final Vector2 renderPosition = new Vector2();

        private Body body;
        private boolean active = true;
        private boolean growthBoosted;
        private boolean pendingElimination;
        private int lastAttackerId = -1;
        private float sizeScale = 1f;
        private float impactSlideTimer;
        private float impactSlideStrength;
        private float controlLockTimer;
        private float recentImpactTimer;
        private float ramChargeTimer;
        private float previousRenderAngleRad;
        private float renderAngleRad;

        private Car(Body body, CarTemplate template) {
            this.body = body;
            this.template = template;
            name = template.name;
            playerControlled = template.playerControlled;
            color = template.color;
            aiController = playerControlled ? null : new CarAiController(template.personality);
            rebuildCollisionFixture();
            syncRenderTransformToBody();
        }

        private void syncRenderTransformToBody() {
            if (body == null) {
                previousRenderPosition.setZero();
                renderPosition.setZero();
                previousRenderAngleRad = 0f;
                renderAngleRad = 0f;
                return;
            }

            previousRenderPosition.set(body.getPosition());
            renderPosition.set(previousRenderPosition);
            previousRenderAngleRad = body.getAngle();
            renderAngleRad = previousRenderAngleRad;
        }

        private void capturePreviousTransform() {
            if (body == null) {
                return;
            }

            previousRenderPosition.set(body.getPosition());
            previousRenderAngleRad = body.getAngle();
        }

        private void updateRenderTransform(float alpha) {
            if (body == null) {
                return;
            }

            float clampedAlpha = MathUtils.clamp(alpha, 0f, 1f);
            renderPosition.set(previousRenderPosition).lerp(body.getPosition(), clampedAlpha);

            float angleDelta =
                    MathUtils.atan2(
                            MathUtils.sin(body.getAngle() - previousRenderAngleRad),
                            MathUtils.cos(body.getAngle() - previousRenderAngleRad));
            renderAngleRad = previousRenderAngleRad + angleDelta * clampedAlpha;
        }

        private Vector2 getRenderPosition() {
            return renderPosition;
        }

        private float getRenderAngleDeg() {
            return renderAngleRad * MathUtils.radiansToDegrees;
        }

        private Vector2 getRenderForwardDirection(Vector2 out) {
            return out.set(-MathUtils.sin(renderAngleRad), MathUtils.cos(renderAngleRad));
        }

        private void step(
                float delta,
                ArenaMap arenaMap,
                Array<Car> cars,
                boolean allowControl,
                float playerThrottle,
                float playerTurn,
                boolean playerHandbrake,
                boolean growthPickupActive,
                Vector2 growthPickupPosition,
                boolean pointPickupActive,
                Vector2 pointPickupPosition) {
            if (!active || body == null) {
                return;
            }

            advanceCombatTimers(delta);
            float impactSlideFactor = advanceImpactSlide(delta);
            applyPendingImpactImpulse();

            float throttle = 0f;
            float turn = 0f;
            boolean handbrake = false;

            if (allowControl && controlLockTimer <= 0f) {
                if (playerControlled) {
                    throttle = playerThrottle;
                    turn = playerTurn;
                    handbrake = playerHandbrake;
                } else {
                    AiControlDecision decision =
                            aiController.plan(
                                    delta,
                                    this,
                                    arenaMap,
                                    cars,
                                    growthPickupActive,
                                    growthPickupPosition,
                                    pointPickupActive,
                                    pointPickupPosition);
                    throttle = decision.throttle;
                    turn = decision.turn;
                    handbrake = decision.handbrake;
                }
            }

            boolean handbrakePivot = shouldUseHandbrakePivot(handbrake, throttle, turn);
            applyGrip(impactSlideFactor, handbrake, handbrakePivot);

            if (!allowControl || controlLockTimer > 0f) {
                return;
            }

            drive(throttle, turn, handbrake, handbrakePivot);
        }

        private void updateAxes() {
            forwardAxis.set(body.getWorldVector(working.set(0f, 1f)));
            sidewaysAxis.set(body.getWorldVector(working.set(1f, 0f)));
        }

        private void applyPendingImpactImpulse() {
            if (pendingImpactImpulse.isZero(0.0001f)) {
                return;
            }

            body.applyLinearImpulse(pendingImpactImpulse, body.getWorldCenter(), true);
            pendingImpactImpulse.setZero();
        }

        private void advanceCombatTimers(float delta) {
            recentImpactTimer = Math.max(0f, recentImpactTimer - delta);
            if (recentImpactTimer == 0f) {
                lastAttackerId = -1;
            }
            controlLockTimer = Math.max(0f, controlLockTimer - delta);
            ramChargeTimer = Math.max(0f, ramChargeTimer - delta);
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

        private boolean shouldUseHandbrakePivot(boolean handbrake, float throttle, float turn) {
            return handbrake
                    && throttle > 0.1f
                    && Math.abs(turn) >= HANDBRAKE_PIVOT_MIN_TURN
                    && body.getLinearVelocity().len2() <= HANDBRAKE_PIVOT_SPEED_SQ;
        }

        private void applyGrip(float impactSlideFactor, boolean handbrake, boolean handbrakePivot) {
            updateAxes();

            float gripMultiplier = MathUtils.clamp(1f - 0.68f * impactSlideFactor, 0.18f, 1f);
            float dragMultiplier = MathUtils.clamp(1f - 0.58f * impactSlideFactor, 0.24f, 1f);
            if (controlLockTimer > 0f) {
                gripMultiplier *= 0.58f;
                dragMultiplier *= 0.76f;
            }
            float angularGripMultiplier = 1f;
            if (handbrake) {
                gripMultiplier *= HANDBRAKE_LATERAL_GRIP_MULTIPLIER;
                dragMultiplier *= HANDBRAKE_FORWARD_DRAG_MULTIPLIER;
                angularGripMultiplier =
                        handbrakePivot
                                ? HANDBRAKE_PIVOT_ANGULAR_GRIP_MULTIPLIER
                                : HANDBRAKE_ANGULAR_GRIP_MULTIPLIER;
            }
            float longitudinalForceMultiplier = getMassMultiplier();

            float lateralSpeed = sidewaysAxis.dot(body.getLinearVelocity());
            working.set(sidewaysAxis).scl(-lateralSpeed * body.getMass() * LATERAL_GRIP * gripMultiplier);
            body.applyLinearImpulse(working, body.getWorldCenter(), true);

            body.applyAngularImpulse(
                    -body.getAngularVelocity()
                            * body.getInertia()
                            * ANGULAR_GRIP
                            * gripMultiplier
                            * angularGripMultiplier,
                    true);

            float forwardSpeed = forwardAxis.dot(body.getLinearVelocity());
            working.set(forwardAxis).scl(-forwardSpeed * FORWARD_DRAG * dragMultiplier * longitudinalForceMultiplier);
            body.applyForceToCenter(working, true);

            if (handbrakePivot) {
                working.set(body.getLinearVelocity()).scl(HANDBRAKE_PIVOT_LINEAR_DAMPING);
                if (working.len2() < 0.01f) {
                    working.setZero();
                }
                body.setLinearVelocity(working);
            }
        }

        private void drive(float throttle, float turn, boolean handbrake, boolean handbrakePivot) {
            updateAxes();
            float signedForwardSpeed = forwardAxis.dot(body.getLinearVelocity());
            float longitudinalForceMultiplier = getMassMultiplier();

            float engineForce = 0f;
            if (throttle != 0f) {
                boolean braking =
                        (throttle > 0f && signedForwardSpeed < -BRAKE_SPEED_THRESHOLD)
                                || (throttle < 0f && signedForwardSpeed > BRAKE_SPEED_THRESHOLD);
                float driveForce;
                if (handbrakePivot) {
                    driveForce = DRIVE_FORCE * HANDBRAKE_PIVOT_DRIVE_FORCE_MULTIPLIER;
                } else if (braking) {
                    driveForce = BRAKE_FORCE;
                } else if (throttle > 0f) {
                    driveForce = DRIVE_FORCE * (handbrake ? HANDBRAKE_DRIVE_FORCE_MULTIPLIER : 1f);
                } else {
                    driveForce = REVERSE_FORCE * (handbrake ? HANDBRAKE_DRIVE_FORCE_MULTIPLIER : 1f);
                }
                engineForce = throttle * driveForce * longitudinalForceMultiplier;
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

            if (handbrake) {
                steeringStrength = Math.max(steeringStrength, HANDBRAKE_STEERING_FLOOR);
                turnTorque *= HANDBRAKE_TURN_TORQUE_MULTIPLIER;
            }
            if (handbrakePivot) {
                steeringStrength = Math.max(steeringStrength, HANDBRAKE_PIVOT_STEERING_STRENGTH);
                turnTorque *= HANDBRAKE_PIVOT_TURN_TORQUE_MULTIPLIER;
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

        private void absorbCollision(
                Vector2 normal,
                float direction,
                float impactStrength,
                float incomingSpeed,
                float reboundMultiplier) {
            if (!active || body == null) {
                return;
            }

            float speedReboundBoost = 1f
                    + MathUtils.clamp(
                            incomingSpeed * INCOMING_SPEED_REBOUND_BOOST_FACTOR,
                            0f,
                            MAX_INCOMING_SPEED_REBOUND_BOOST);
            float speedReboundScale =
                    (float) Math.pow(speedReboundBoost, COLLISION_BOUNCE_SCALE);
            float collisionImpulse = MathUtils.clamp(
                    (impactStrength - MIN_COLLISION_RESPONSE_IMPULSE)
                            * COLLISION_REBOUND_IMPULSE_FACTOR
                            * reboundMultiplier
                            * speedReboundScale,
                    0f,
                    MAX_COLLISION_REBOUND_IMPULSE
                            * Math.max(1f, reboundMultiplier)
                            * speedReboundScale);
            if (collisionImpulse <= 0f) {
                return;
            }

            pendingImpactImpulse.mulAdd(normal, direction * collisionImpulse);
            clampPendingImpactImpulse();

            updateAxes();
            float slideShare = 0.28f + 0.72f * Math.abs(sidewaysAxis.dot(normal));
            impactSlideStrength = Math.max(
                    impactSlideStrength,
                    MathUtils.clamp(
                            slideShare
                                    * impactStrength
                                    * reboundMultiplier
                                    * speedReboundScale
                                    / IMPACT_SLIDE_REFERENCE,
                            0f,
                            1f));
            impactSlideTimer = Math.max(impactSlideTimer, IMPACT_SLIDE_DURATION);
        }

        private void clampPendingImpactImpulse() {
            float maxStoredImpulse = MAX_STORED_COLLISION_IMPULSE * COLLISION_BOUNCE_SCALE;
            if (pendingImpactImpulse.len2() > maxStoredImpulse * maxStoredImpulse) {
                pendingImpactImpulse.setLength(maxStoredImpulse);
            }
        }

        private void receiveCombatHit(
                Car attacker,
                ArenaMap arenaMap,
                float impactStrength,
                float incomingSpeed,
                boolean attackerEmpowered) {
            if (!active || body == null) {
                return;
            }

            lastAttackerId = attacker == null ? -1 : attacker.template.vehicleId;
            recentImpactTimer = RECENT_IMPACT_DURATION;
            controlLockTimer = Math.max(
                    controlLockTimer,
                    MathUtils.clamp(
                            (impactStrength - MIN_COLLISION_RESPONSE_IMPULSE)
                                    / CONTROL_LOCK_REFERENCE
                                    + (attackerEmpowered ? 0.14f : 0.04f),
                            0.06f,
                            attackerEmpowered ? MAX_CONTROL_LOCK_DURATION : 0.34f));

            if (arenaMap == null) {
                return;
            }

            float edgeDistance = arenaMap.distanceToHazard(body.getPosition());
            if (edgeDistance >= EDGE_FINISH_DISTANCE) {
                return;
            }

            arenaMap.findRecoveryPoint(body.getPosition(), impactRecoveryPoint);
            impactOutward.set(body.getPosition()).sub(impactRecoveryPoint);
            if (impactOutward.isZero(0.0001f)) {
                return;
            }

            float danger = 1f - MathUtils.clamp(edgeDistance / EDGE_FINISH_DISTANCE, 0f, 1f);
            float finishImpulse = (BASE_EDGE_FINISH_IMPULSE + incomingSpeed * 1.55f) * danger;
            if (attackerEmpowered) {
                finishImpulse *= EMPOWERED_EDGE_FINISH_MULTIPLIER;
            }

            pendingImpactImpulse.mulAdd(impactOutward.nor(), finishImpulse);
            clampPendingImpactImpulse();

            impactSlideStrength = Math.max(
                    impactSlideStrength,
                    MathUtils.clamp(
                            0.40f + danger * 0.26f + (attackerEmpowered ? 0.18f : 0f),
                            0f,
                            1f));
            impactSlideTimer = Math.max(impactSlideTimer, IMPACT_SLIDE_DURATION + danger * 0.18f);
        }

        private float getCollisionReboundMultiplierAgainst(Car other) {
            float multiplier = 1f;
            if (other == null) {
                return multiplier;
            }

            if (growthBoosted && other.growthBoosted) {
                multiplier *= BOOSTED_VS_BOOSTED_REBOUND_MULTIPLIER;
            } else if (growthBoosted) {
                multiplier *= BOOSTED_VS_NORMAL_REBOUND_MULTIPLIER;
            } else if (other.growthBoosted) {
                multiplier *= NORMAL_VS_BOOSTED_REBOUND_MULTIPLIER;
            }

            if (hasRamCharge() && !other.hasRamCharge()) {
                multiplier *= RAM_CHARGE_ATTACKER_REBOUND_MULTIPLIER;
            } else if (other.hasRamCharge() && !hasRamCharge()) {
                multiplier *= RAM_CHARGE_VICTIM_REBOUND_MULTIPLIER;
            }

            return multiplier;
        }

        private void grantRamCharge() {
            ramChargeTimer = RAM_CHARGE_DURATION;
        }

        private void consumeRamCharge() {
            ramChargeTimer = 0f;
        }

        private float getRamChargeTimeLeft() {
            return ramChargeTimer;
        }

        private void setGrowthBoost(boolean growthBoosted) {
            this.growthBoosted = growthBoosted;
            sizeScale = growthBoosted ? GROWTH_SCALE : 1f;
            if (body != null) {
                rebuildCollisionFixture();
            }
        }

        private void clearImpactResponse() {
            pendingImpactImpulse.setZero();
            impactSlideTimer = 0f;
            impactSlideStrength = 0f;
            controlLockTimer = 0f;
            recentImpactTimer = 0f;
            lastAttackerId = -1;
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

        @Override
        public boolean hasRamCharge() {
            return ramChargeTimer > 0f;
        }

        @Override
        public int getVehicleId() {
            return template.vehicleId;
        }

        @Override
        public int getScore() {
            return template.totalPoints;
        }

        @Override
        public int getLastAttackerId() {
            return lastAttackerId;
        }

        @Override
        public float getRecentImpactTime() {
            return recentImpactTimer;
        }

        private void eliminate(World world) {
            if (!active || body == null) {
                return;
            }

            active = false;
            growthBoosted = false;
            pendingElimination = false;
            ramChargeTimer = 0f;
            sizeScale = 1f;
            clearImpactResponse();
            world.destroyBody(body);
            body = null;
        }
    }

    public static final class AiTournamentParticipant {
        public final String label;
        public final String displayName;
        public final AiDrivingPersonality personality;

        public AiTournamentParticipant(String label, String displayName, AiDrivingPersonality personality) {
            if (label == null || label.length() == 0) {
                throw new IllegalArgumentException("Ai tournament participant label is required.");
            }
            if (personality == null) {
                throw new IllegalArgumentException("Ai tournament participant personality is required.");
            }
            this.label = label;
            this.displayName = displayName == null || displayName.length() == 0 ? label : displayName;
            this.personality = personality;
        }
    }

    public static final class AiTournamentConfig {
        public final Array<AiTournamentParticipant> participants = new Array<AiTournamentParticipant>();
        public final Array<ArenaMap> maps = new Array<ArenaMap>();
        public int rounds = 12;
        public long seed = 1L;
        public boolean skipCountdown = true;

        public AiTournamentConfig addParticipant(AiTournamentParticipant participant) {
            participants.add(participant);
            return this;
        }

        public AiTournamentConfig addParticipant(
                String label,
                String displayName,
                AiDrivingPersonality personality) {
            participants.add(new AiTournamentParticipant(label, displayName, personality));
            return this;
        }

        public AiTournamentConfig addMap(ArenaMap map) {
            if (map != null) {
                maps.add(map);
            }
            return this;
        }

        public AiTournamentConfig withRounds(int rounds) {
            this.rounds = rounds;
            return this;
        }

        public AiTournamentConfig withSeed(long seed) {
            this.seed = seed;
            return this;
        }

        public AiTournamentConfig withSkipCountdown(boolean skipCountdown) {
            this.skipCountdown = skipCountdown;
            return this;
        }
    }

    public static final class AiTournamentEntry {
        public final String label;
        public final String displayName;
        public int entrants;
        public int samples;
        public int wins;
        public int totalPlacementPoints;
        public int totalAwardedPoints;
        public int totalPickupPoints;

        private AiTournamentEntry(String label, String displayName) {
            this.label = label;
            this.displayName = displayName == null || displayName.length() == 0 ? label : displayName;
        }

        public float getAveragePlacementPoints() {
            return samples == 0 ? 0f : (float) totalPlacementPoints / samples;
        }

        public float getAverageAwardedPoints() {
            return samples == 0 ? 0f : (float) totalAwardedPoints / samples;
        }

        public float getAveragePickupPoints() {
            return samples == 0 ? 0f : (float) totalPickupPoints / samples;
        }

        public float getWinRate() {
            return samples == 0 ? 0f : (float) wins / samples;
        }
    }

    public static final class AiTournamentResult {
        public final long seed;
        public final int rounds;
        public final int participantCount;
        public final Array<AiTournamentEntry> entries = new Array<AiTournamentEntry>();

        private AiTournamentResult(long seed, int rounds, int participantCount) {
            this.seed = seed;
            this.rounds = rounds;
            this.participantCount = participantCount;
        }

        public AiTournamentEntry getEntry(String label) {
            for (int i = 0; i < entries.size; i++) {
                AiTournamentEntry entry = entries.get(i);
                if (entry.label.equals(label)) {
                    return entry;
                }
            }
            return null;
        }
    }

    private static final class CarTemplate {
        private final int vehicleId;
        private final String name;
        private final boolean playerControlled;
        private final Color color;
        private final AiDrivingPersonality personality;
        private final CarVisual visual;
        private final String statsLabel;
        private Texture spriteTexture;
        private boolean ownsSpriteTexture;
        private int spriteSourceX;
        private int spriteSourceY;
        private int spriteSourceWidth;
        private int spriteSourceHeight;
        private int totalPoints;
        private int roundFinishPosition;
        private int lastRoundAwardedPoints;
        private int roundPickupPoints;
        private Car currentCar;

        private CarTemplate(
                int vehicleId,
                String name,
                boolean playerControlled,
                Color color,
                AiDrivingPersonality personality,
                CarVisual visual,
                String statsLabel) {
            this.vehicleId = vehicleId;
            this.name = name;
            this.playerControlled = playerControlled;
            this.color = color;
            this.personality = personality;
            this.visual = visual;
            this.statsLabel = statsLabel == null || statsLabel.length() == 0 ? name : statsLabel;
        }
    }

    private static final class DestructionEffect {
        private final Vector2 position = new Vector2();
        private final Color color = new Color();
        private float timer;
        private float rotationDeg;
        private float scale;
    }

    private enum MapDecorStyle {
        GRID,
        RUNWAY,
        ORBIT,
        CROSS,
        DIAGONAL,
        FORTRESS
    }

    private static final class MapTheme {
        private final MapDecorStyle decorStyle;
        private final Color backdropBase;
        private final Color backdropLine;
        private final Color backdropGlow;
        private final Color edge;
        private final Color surface;
        private final Color center;
        private final Color accent;
        private final Color accentSoft;

        private MapTheme(
                MapDecorStyle decorStyle,
                Color backdropBase,
                Color backdropLine,
                Color backdropGlow,
                Color edge,
                Color surface,
                Color center,
                Color accent,
                Color accentSoft) {
            this.decorStyle = decorStyle;
            this.backdropBase = backdropBase;
            this.backdropLine = backdropLine;
            this.backdropGlow = backdropGlow;
            this.edge = edge;
            this.surface = surface;
            this.center = center;
            this.accent = accent;
            this.accentSoft = accentSoft;
        }
    }

    private static final class ThemeCarComponent {
        private final IntArray pixels;
        private final int minX;
        private final int minY;
        private final int maxX;
        private final int maxY;

        private ThemeCarComponent(IntArray pixels, int minX, int minY, int maxX, int maxY) {
            this.pixels = pixels;
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
        }

        private int getPixelCount() {
            return pixels.size;
        }

        private int getWidth() {
            return maxX - minX + 1;
        }

        private int getHeight() {
            return maxY - minY + 1;
        }

        private float getCenterX() {
            return (minX + maxX) * 0.5f;
        }

        private float getCenterY() {
            return (minY + maxY) * 0.5f;
        }
    }

    private static final class SidebarLeaderboardLayout {
        private final int columns;
        private final int rowsPerColumn;
        private final boolean compact;
        private final boolean showPoints;
        private final float headingStep;
        private final float rowStep;
        private final float columnGap;
        private final float columnWidth;
        private final float totalHeight;

        private SidebarLeaderboardLayout(
                int columns,
                int rowsPerColumn,
                boolean compact,
                boolean showPoints,
                float headingStep,
                float rowStep,
                float columnGap,
                float columnWidth,
                float totalHeight) {
            this.columns = columns;
            this.rowsPerColumn = rowsPerColumn;
            this.compact = compact;
            this.showPoints = showPoints;
            this.headingStep = headingStep;
            this.rowStep = rowStep;
            this.columnGap = columnGap;
            this.columnWidth = columnWidth;
            this.totalHeight = totalHeight;
        }
    }

    private static final class CarVisual {
        private final String spritePath;
        private final Texture texture;
        private final Color color;
        private final int spriteSourceX;
        private final int spriteSourceY;
        private final int spriteSourceWidth;
        private final int spriteSourceHeight;
        private final boolean stripDarkBackground;
        private final boolean sharedTexture;
        private final float spriteRotationOffsetDeg;

        private CarVisual(String spritePath, Color color) {
            this(
                    spritePath,
                    null,
                    0,
                    0,
                    0,
                    0,
                    false,
                    false,
                    CAR_SPRITE_ROTATION_OFFSET_DEG,
                    color);
        }

        private CarVisual(Texture texture, float spriteRotationOffsetDeg, Color color) {
            this(
                    null,
                    texture,
                    0,
                    0,
                    0,
                    0,
                    false,
                    false,
                    spriteRotationOffsetDeg,
                    color);
        }

        private CarVisual(
                String spritePath,
                Texture texture,
                int spriteSourceX,
                int spriteSourceY,
                int spriteSourceWidth,
                int spriteSourceHeight,
                boolean stripDarkBackground,
                boolean sharedTexture,
                float spriteRotationOffsetDeg,
                Color color) {
            this.spritePath = spritePath;
            this.texture = texture;
            this.color = color;
            this.spriteSourceX = spriteSourceX;
            this.spriteSourceY = spriteSourceY;
            this.spriteSourceWidth = spriteSourceWidth;
            this.spriteSourceHeight = spriteSourceHeight;
            this.stripDarkBackground = stripDarkBackground;
            this.sharedTexture = sharedTexture;
            this.spriteRotationOffsetDeg = spriteRotationOffsetDeg;
        }

        private boolean hasSourceRegion() {
            return spriteSourceWidth > 0 && spriteSourceHeight > 0;
        }
    }
}
