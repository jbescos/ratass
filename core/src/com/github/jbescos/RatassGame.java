package com.github.jbescos;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Music;
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
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.github.jbescos.ai.AiControlDecision;
import com.github.jbescos.ai.rl.RlPolicy;
import com.github.jbescos.gameplay.ArenaMap;
import com.github.jbescos.gameplay.MapProgression;
import com.github.jbescos.gameplay.SpawnPoint;
import com.github.jbescos.gameplay.maps.ArenaMaps;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class RatassGame extends ApplicationAdapter {
    private static final String GAME_PROPERTIES_RESOURCE = "game.properties";
    private static final String GAME_PROPERTIES_FILE = "assets/game.properties";
    private static final String SETTINGS_PREFS_NAME = "ratass-settings";
    private static final String THEME_PROPERTY = "theme";
    private static final String THEME_PREF_KEY = THEME_PROPERTY;
    private static final String DEFAULT_THEME_NAME = "infernal";
    private static final String CAMERA_FOLLOW_BEHIND_PROPERTY = "camera.follow.behind";
    private static final String CAMERA_FOLLOW_BEHIND_PREF_KEY = CAMERA_FOLLOW_BEHIND_PROPERTY;
    private static final String CAMERA_ZOOM_PROPERTY = "camera.zoom";
    private static final String CAMERA_ZOOM_PREF_KEY = CAMERA_ZOOM_PROPERTY;
    private static final String CAR_COUNT_PROPERTY = "cars.count";
    private static final String CAR_COUNT_PREF_KEY = CAR_COUNT_PROPERTY;
    private static final String PLAYER_CAR_PROPERTY = "cars.player.index";
    private static final String PLAYER_CAR_PREF_KEY = PLAYER_CAR_PROPERTY;
    private static final String RACE_LAPS_PROPERTY = "race.laps";
    private static final String RACE_LAPS_PREF_KEY = RACE_LAPS_PROPERTY;
    private static final String SANDBOX_MAP_PREF_KEY = "sandbox.map.index";
    private static final String THEME_DIRECTORY = "theme";
    private static final String THEME_MANIFEST_PATH = "themes.txt";
    private static final String THEME_CAR_SHEET_PATH = "cars/cars.png";
    private static final String THEME_FLAT_CAR_SHEET_PATH = "cars.png";
    private static final String THEME_ENEMY_NAMES_PATH = "enemy-names.txt";
    private static final String RL_ENEMY_POLICY_PATH = "ai/rl_enemy_policy.json";
    private static final ThemeChoice[] FALLBACK_THEME_CHOICES = new ThemeChoice[] {
            new ThemeChoice("infernal", "Infernal"),
            new ThemeChoice("sport", "Sport Cars"),
            new ThemeChoice("classical", "Classical Cars"),
            new ThemeChoice("circus", "Circus Cars"),
            new ThemeChoice("animals", "Animal Cars"),
            new ThemeChoice("monsters", "Monster Cars")
    };
    private static final int THEME_CAR_COLUMNS = 10;
    private static final int THEME_CAR_SHEET_ROWS = 5;
    private static final int MAX_CAR_VISUAL_COUNT = THEME_CAR_COLUMNS * THEME_CAR_SHEET_ROWS;
    private static final int DEFAULT_CAR_COUNT = 20;
    private static final int DEFAULT_PLAYER_CAR_INDEX = 0;
    private static final int DEFAULT_RACE_LAPS = 3;
    private static final int MIN_RACE_LAPS = 1;
    private static final int MAX_RACE_LAPS = 10;
    private static final int MIN_CAR_COUNT = 1;
    private static final int MAX_CAR_COUNT = 20;
    private static final int OPTIONS_THEME_SELECTION = 0;
    private static final int OPTIONS_CARS_SELECTION = 1;
    private static final int OPTIONS_PLAYER_CAR_SELECTION = 2;
    private static final int OPTIONS_LAPS_SELECTION = 3;
    private static final int OPTIONS_CAMERA_SELECTION = 4;
    private static final int OPTIONS_ZOOM_SELECTION = 5;
    private static final int OPTIONS_BACK_SELECTION = 6;
    private static final int MAIN_MENU_NEW_GAME_SELECTION = 0;
    private static final int MAIN_MENU_SANDBOX_SELECTION = 1;
    private static final int MAIN_MENU_OPTIONS_SELECTION = 2;
    private static final int MAIN_MENU_EXIT_SELECTION = 3;
    private static final int PAUSE_MENU_RESTART_SELECTION = 0;
    private static final int PAUSE_MENU_OPTIONS_SELECTION = 1;
    private static final int PAUSE_MENU_MAIN_MENU_SELECTION = 2;
    private static final int PAUSE_MENU_EXIT_SELECTION = 3;
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
    private static final boolean GROWTH_PICKUP_ENABLED = false;
    private static final float GROWTH_PICKUP_RADIUS = 0.68f;
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
    private static final float RACE_CHECKPOINT_DEFAULT_DEADLINE = 10f;
    private static final float RACE_CHECKPOINT_MIN_RADIUS = 0.58f;
    private static final int[] RACE_POSITION_POINTS = {25, 18, 15, 12, 10, 8, 6, 4, 2, 1};
    private static final float RACE_FINISH_TIMEOUT = 15f;
    private static final float RACE_CHECKPOINT_RADIUS = 3.0f;
    private static final float RACE_CHECKPOINT_GATE_MARGIN = 2.40f;
    private static final float RACE_CHECKPOINT_MIN_FORWARD_CROSS_SPEED = 0.30f;
    private static final float MIN_WORLD_CAMERA_ZOOM = 0.90f;
    private static final float PLAYER_CAMERA_ZOOM = 1.04f;
    private static final float PLAYER_CAMERA_SPEED_ZOOM_OUT = 0.46f;
    private static final float PLAYER_CAMERA_GROWTH_ZOOM_OUT = 0.14f;
    private static final float PLAYER_CAMERA_MAX_ZOOM = 1.58f;
    private static final float DEFAULT_CAMERA_ZOOM = 1.00f;
    private static final float MIN_CAMERA_ZOOM = 0.70f;
    private static final float MAX_CAMERA_ZOOM = 1.50f;
    private static final float CAMERA_ZOOM_STEP = 0.10f;
    private static final float DEFAULT_MAP_SCALE = 8.00f;
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
    private static final float ROUND_SPAWN_CROWDED_SAFE_MARGIN = 1.0f;
    private static final float ROUND_SPAWN_MIN_DISTANCE = 1.95f;
    private static final float EVENT_CALLOUT_DURATION = 1.35f;
    public static final int RL_OBSERVATION_SIZE = 68;
    public static final int RL_ACTION_SIZE = 2;
    public static final int RL_REWARD_BREAKDOWN_SIZE = 8;
    private static final int RL_REWARD_PROGRESS = 0;
    private static final int RL_REWARD_CHECKPOINT = 1;
    private static final int RL_REWARD_STEP_COST = 2;
    private static final int RL_REWARD_OFF_ROAD = 3;
    private static final int RL_REWARD_STEERING = 4;
    private static final int RL_REWARD_REVERSE_SPEED = 5;
    private static final int RL_REWARD_ELIMINATION = 6;
    private static final int RL_REWARD_CAR_PUSH = 7;
    private static final String[] RL_REWARD_BREAKDOWN_NAMES = {
            "checkpoint_progress",
            "checkpoint",
            "step_cost",
            "off_road",
            "steering",
            "reverse_speed",
            "elimination",
            "car_push"
    };
    private static final int RL_DEBUG_TRACE_SIZE = 40;
    private static final String[] RL_DEBUG_TRACE_NAMES = {
            "active",
            "car_x",
            "car_y",
            "car_angle",
            "speed",
            "forward_speed",
            "lateral_speed",
            "target_checkpoint_index",
            "target_checkpoint_sequence",
            "target_x",
            "target_y",
            "target_has_gate",
            "target_gate_start_x",
            "target_gate_start_y",
            "target_gate_end_x",
            "target_gate_end_y",
            "target_supported",
            "target_edge_distance",
            "target_route_distance",
            "progress_toward_target",
            "route_target_x",
            "route_target_y",
            "route_target_supported",
            "route_target_edge_distance",
            "route_clearance",
            "second_checkpoint_index",
            "second_checkpoint_x",
            "second_checkpoint_y",
            "second_checkpoint_supported",
            "off_road",
            "off_road_distance",
            "edge_distance",
            "effective_throttle",
            "effective_turn",
            "checkpoint_deadline_remaining_seconds",
            "checkpoint_deadline_duration_seconds",
            "checkpoints_reached",
            "max_checkpoints",
            "checkpoint_timeout",
            "episode_done"
    };
    private static final int RL_DEFAULT_CONTROLLED_AGENTS = 1;
    private static final int RL_DEFAULT_FIELD_SIZE = 1;
    private static final int RL_DEFAULT_ACTION_REPEAT = 4;
    private static final int RL_DEFAULT_MAX_ACTION_STEPS = 6400;
    private static final int RL_DEFAULT_MAX_CHECKPOINTS = 6;
    private static final float RL_LIVE_DECISION_INTERVAL = 0.08f;
    private static final float RL_INITIAL_DECISION_STAGGER = 0.24f;
    private static final float RL_POSITION_NORMALIZER_MIN = 1f;
    private static final float RL_ANGULAR_VELOCITY_NORMALIZER = 8f;
    private static final float RL_HAZARD_DISTANCE_NORMALIZER = 12f;
    private static final float RL_DEFAULT_CHECKPOINT_RADIUS = RACE_CHECKPOINT_RADIUS;
    private static final float RL_DEFAULT_CHECKPOINT_DEADLINE_SECONDS = 0f;
    private static final float RL_ROUTE_MARGIN = 0.75f;
    private static final float RL_RAYCAST_DISTANCE = 22f;
    private static final float RL_SHORT_RAYCAST_DISTANCE = 7.5f;
    private static final float RL_RAYCAST_STEP = 0.40f;
    private static final float RL_CAR_SENSOR_DISTANCE = 18f;
    private static final float RL_CAR_SENSOR_RADIUS = 1.2f;
    private static final float RL_BRAKING_DISTANCE_NORMALIZER = 24f;
    private static final float RL_CONTROL_DEADZONE = 0.06f;
    private static final float RL_ACTION_FLIP_DEADZONE = 0.18f;
    private static final float RL_STEP_PENALTY = 0.006f;
    private static final float RL_PROGRESS_REWARD = 1.60f;
    private static final float RL_STEERING_PENALTY = 0.010f;
    private static final float RL_REVERSE_SPEED_FREE_EPSILON = 0.20f;
    private static final float RL_REVERSE_SPEED_PENALTY_PER_UNIT = 0.08f;
    private static final float RL_REVERSE_SPEED_MAX_PENALTY = 0.90f;
    private static final float RL_CHECKPOINT_ROUTE_DEADLINE_BASE_SECONDS = 8f;
    private static final float RL_CHECKPOINT_ROUTE_DEADLINE_SECONDS_PER_UNIT = 0.33f;
    private static final float RL_CHECKPOINT_ROUTE_DEADLINE_MAX_SECONDS = 120f;
    private static final float RL_RANDOM_SPAWN_BACKWARD_CHECKPOINT_PENALTY = 85f;
    private static final float RL_RANDOM_SPAWN_CHECKPOINT_CLEARANCE = 5f;
    private static final float RL_RANDOM_SPAWN_NEAR_CHECKPOINT_PENALTY = 22f;
    private static final float RL_RANDOM_SPAWN_MIN_ROUTE_DISTANCE = 16f;
    private static final float RL_RANDOM_SPAWN_ROUTE_TARGET_EPSILON = 0.35f;
    private static final float RL_CHECKPOINT_REWARD = 30.0f;
    private static final float RL_ELIMINATION_PENALTY = 128.0f;
    private static final float RL_CAR_PUSH_PENALTY = 3.0f;
    private static final float RL_CAR_PUSH_MAX_STEP_PENALTY = 8.0f;
    private static final float RL_OFF_ROAD_PENALTY = 0.80f;
    private static final float RL_OFF_ROAD_DISTANCE_PENALTY = 0.22f;
    private static final float RL_OFF_ROAD_MAX_PENALTY = 5.0f;
    private static final float RL_EDGE_DANGER_DISTANCE = 2.35f;
    private static final float RL_EDGE_WARNING_DISTANCE = 9.0f;
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
    private static final float SIDEBAR_LEADERBOARD_COMPACT_ROW_STEP = 11.5f;
    private static final float HUD_FONT_SCALE = 1.18f;
    private static final float TITLE_FONT_SCALE = 2.25f;
    private static final float LEADERBOARD_FONT_SCALE = 0.96f;
    private static final float LABEL_FONT_SCALE = 0.86f;
    private static final float MENU_BUTTON_WIDTH = 340f;
    private static final float MENU_BUTTON_HEIGHT = 56f;
    private static final float MENU_BUTTON_GAP = 18f;
    private static final float MENU_OPTION_ROW_WIDTH = 560f;
    private static final float MENU_OPTION_ROW_HEIGHT = 54f;
    private static final float MENU_OPTION_STEP_BUTTON_WIDTH = 58f;
    private static final float MENU_MIN_SIDE_MARGIN = 24f;
    private static final float MENU_OPTIONS_COLUMN_GAP = 24f;
    private static final float MENU_CAR_PREVIEW_MAX_WIDTH = 620f;
    private static final float MENU_CAR_PREVIEW_MIN_WIDTH = 240f;
    private static final float MENU_CAR_SHEET_ASPECT = 3f / 2f;
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
    private final Array<DestructionEffect> destructionEffects = new Array<DestructionEffect>();
    private final Array<CarVisual> themeCarVisuals = new Array<CarVisual>();
    private final Array<Integer> sessionEnemyVisualIndices = new Array<Integer>();
    private final Array<Integer> sessionEnemyVisualPool = new Array<Integer>();
    private final Array<Rectangle> menuCarSheetSourceBounds = new Array<Rectangle>();
    private final Array<ThemeChoice> themeChoices = new Array<ThemeChoice>();
    private final Array<String> themeEnemyNames = new Array<String>();
    private final Array<ArenaMap> sandboxMenuMaps = new Array<ArenaMap>();
    private final LinkedHashMap<String, Texture> arenaSurfaceTextureCache =
            new LinkedHashMap<String, Texture>();
    private final Random sessionEnemyVisualRandom = new Random();
    private final GlyphLayout glyphLayout = new GlyphLayout();
    private int sessionEnemyVisualPaletteSize = -1;
    private final Color tint = new Color();
    private final Rectangle mapBounds = new Rectangle();
    private final Vector2 focusPoint = new Vector2();
    private final Vector2 growthPickupPosition = new Vector2();
    private final Vector2 pointPickupPosition = new Vector2();
    private final Vector2 checkpointTargetPosition = new Vector2();
    private final Vector2 raceTargetPosition = new Vector2();
    private final Vector2 raceSecondTargetPosition = new Vector2();
    private final Vector2 pickupCandidate = new Vector2();
    private final Vector2 lastGrowthPickupPosition = new Vector2();
    private final Vector2 lastPointPickupPosition = new Vector2();
    private final Vector2 spawnCandidate = new Vector2();
    private final Array<SpawnPoint> roundSpawns = new Array<SpawnPoint>();
    private final Rectangle menuNewGameBounds = new Rectangle();
    private final Rectangle menuSandboxBounds = new Rectangle();
    private final Rectangle menuSandboxPrevBounds = new Rectangle();
    private final Rectangle menuSandboxNextBounds = new Rectangle();
    private final Rectangle menuOptionsBounds = new Rectangle();
    private final Rectangle menuExitBounds = new Rectangle();
    private final Rectangle pauseRestartBounds = new Rectangle();
    private final Rectangle pauseOptionsBounds = new Rectangle();
    private final Rectangle pauseMainMenuBounds = new Rectangle();
    private final Rectangle pauseExitBounds = new Rectangle();
    private final Rectangle optionsThemeBounds = new Rectangle();
    private final Rectangle optionsThemePrevBounds = new Rectangle();
    private final Rectangle optionsThemeNextBounds = new Rectangle();
    private final Rectangle optionsCarsBounds = new Rectangle();
    private final Rectangle optionsCarsPrevBounds = new Rectangle();
    private final Rectangle optionsCarsNextBounds = new Rectangle();
    private final Rectangle optionsPlayerCarBounds = new Rectangle();
    private final Rectangle optionsPlayerCarPrevBounds = new Rectangle();
    private final Rectangle optionsPlayerCarNextBounds = new Rectangle();
    private final Rectangle optionsLapsBounds = new Rectangle();
    private final Rectangle optionsLapsPrevBounds = new Rectangle();
    private final Rectangle optionsLapsNextBounds = new Rectangle();
    private final Rectangle optionsCameraBounds = new Rectangle();
    private final Rectangle optionsZoomBounds = new Rectangle();
    private final Rectangle optionsZoomOutBounds = new Rectangle();
    private final Rectangle optionsZoomInBounds = new Rectangle();
    private final Rectangle optionsBackBounds = new Rectangle();
    private final Rectangle optionsCarSheetBounds = new Rectangle();
    private final Rectangle steerPadBounds = new Rectangle();
    private final Rectangle throttlePadBounds = new Rectangle();
    private final Rectangle reversePadBounds = new Rectangle();
    private final Rectangle restartButtonBounds = new Rectangle();
    private final Rectangle sidebarMinimapBounds = new Rectangle();
    private final Rectangle sidebarTablesViewportBounds = new Rectangle();
    private final Rectangle sidebarTablesScrollbarBounds = new Rectangle();
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
    private final Comparator<CarTemplate> leaderboardComparator = new Comparator<CarTemplate>() {
        @Override
        public int compare(CarTemplate left, CarTemplate right) {
            if (isLiveLapRaceMode() && !roundOver) {
                return compareRaceStanding(left, right);
            }
            if (left.totalPoints != right.totalPoints) {
                return right.totalPoints - left.totalPoints;
            }
            if (left.roundFinishPosition != right.roundFinishPosition) {
                return compareFinishPosition(left.roundFinishPosition, right.roundFinishPosition);
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
    private Music themeMusic;
    private World world;
    private Texture arenaSurfaceTexture;
    private Texture themeCarsTexture;
    private Texture menuCarSheetTexture;
    private String preloadedNextArenaSurfaceKey;

    private GameMode gameMode = GameMode.MAIN_MENU;
    private MapProgression mapProgression;
    private ArenaMap currentMap;
    private float accumulator;
    private float roundOverTimer;
    private float effectClock;
    private float frameThrottleInput;
    private float frameTurnInput;
    private float preRoundCountdownTimer;
    private float roundTimer;
    private float checkpointDeadlineTimer;
    private float checkpointTargetRadius;
    private float touchThrottleInput;
    private float touchTurnInput;
    private float growthBoostTimer;
    private float playfieldHudWidth;
    private float sidebarHudWidth;
    private float impactSoundCooldown;
    private float destructionSoundCooldown;
    private float smoothedCameraZoom = 1f;
    private float eventCalloutTimer;
    private float raceFinishTimer;
    private float sidebarTablesScrollOffset;
    private float sidebarTablesScrollbarGrabOffsetY;
    private boolean touchRestartPressed;
    private boolean touchRestartJustPressed;
    private boolean touchControlsEnabled;
    private boolean cameraInitialized;
    private boolean growthPickupActive;
    private boolean pointPickupActive;
    private boolean checkpointTargetActive;
    private boolean hasLastGrowthPickupPosition;
    private boolean hasLastPointPickupPosition;
    private boolean leaderboardDirty = true;
    private boolean roundOver;
    private boolean roundStartSoundPlayed;
    private boolean sidebarTablesScrollbarDragging;
    private boolean sandboxMode;
    private boolean rlTrainingAllowSoloRound;
    private boolean rlTrainingDisablePickups;
    private boolean rlTrainingRandomSpawnLocations;
    private boolean rlTrainingMode;
    private boolean rlTrainingRaceMode;
    private int countdownCueSecond;
    private int roundNumber;
    private int playerWins;
    private int finishPositionCounter;
    private int checkpointTargetSequence;
    private int selectedThemeIndex;
    private int selectedCarCount = DEFAULT_CAR_COUNT;
    private int selectedPlayerCarIndex = DEFAULT_PLAYER_CAR_INDEX;
    private int selectedRaceLapCount = DEFAULT_RACE_LAPS;
    private int selectedSandboxMapIndex;
    private int mainMenuSelection;
    private int pauseMenuSelection;
    private int optionsMenuSelection;
    private Car boostedCar;
    private Car playerCar;
    private Car winner;
    private RlPolicy rlEnemyPolicy;
    private String eventCalloutTitle = "";
    private String eventCalloutSubline = "";
    private String configuredThemeName = DEFAULT_THEME_NAME;
    private String menuCarSheetThemeName = "";
    private boolean followCameraBehind;
    private boolean optionsOpenedFromPause;
    private float cameraZoom = DEFAULT_CAMERA_ZOOM;
    private float mapScale = DEFAULT_MAP_SCALE;

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

        loadMenuSettings();
        Gdx.input.setCatchKey(Input.Keys.BACK, true);
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean scrolled(float amountX, float amountY) {
                return handleHudScrolled(amountY);
            }
        });
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private void startNewGame() {
        startNewGame(false);
    }

    private void startSandboxGame() {
        startNewGame(true);
    }

    private void startNewGame(boolean sandbox) {
        sandboxMode = sandbox;
        disposeRosterSpriteTextures();
        disposeArenaSurfaceTextures();
        disposeMenuCarPreview();
        loadThemeEnemyNames();
        loadThemeTextures();
        int clampedPlayerCarIndex =
                clampPlayerCarIndex(selectedPlayerCarIndex, getAvailableCarVisualCount());
        if (clampedPlayerCarIndex != selectedPlayerCarIndex) {
            selectedPlayerCarIndex = clampedPlayerCarIndex;
            saveMenuSettings();
        }
        loadSounds();
        rlEnemyPolicy = loadRlEnemyPolicy();
        createRoster();
        loadCarSprites();
        mapProgression = new MapProgression(createGameplayMapSet(sandbox));

        roundNumber = 0;
        playerWins = 0;
        accumulator = 0f;
        roundOverTimer = 0f;
        effectClock = 0f;
        eventCalloutTimer = 0f;
        cameraInitialized = false;
        gameMode = GameMode.PLAYING;
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        resetRound(false);
    }

    private Array<ArenaMap> createGameplayMapSet(boolean sandbox) {
        Array<ArenaMap> maps = ArenaMaps.createDefaultSet(mapScale);
        if (!sandbox) {
            return maps;
        }

        Array<ArenaMap> sandboxMaps = new Array<ArenaMap>();
        if (maps.size > 0) {
            selectedSandboxMapIndex = clampSandboxMapIndex(selectedSandboxMapIndex, maps.size);
            sandboxMaps.add(maps.get(selectedSandboxMapIndex));
            return sandboxMaps;
        }

        Gdx.app.log("RatassGame", "No sandbox maps were found; using default maps.");
        return maps;
    }

    private void refreshSandboxMenuMaps() {
        sandboxMenuMaps.clear();
        try {
            sandboxMenuMaps.addAll(ArenaMaps.createDefaultSet(mapScale));
        } catch (RuntimeException exception) {
            if (Gdx.app != null) {
                Gdx.app.error("RatassGame", "Could not load sandbox map list.", exception);
            }
        }
        selectedSandboxMapIndex = clampSandboxMapIndex(selectedSandboxMapIndex);
    }

    private int clampSandboxMapIndex(int index) {
        return clampSandboxMapIndex(index, sandboxMenuMaps.size);
    }

    private int clampSandboxMapIndex(int index, int mapCount) {
        if (mapCount <= 0) {
            return 0;
        }
        return MathUtils.clamp(index, 0, mapCount - 1);
    }

    private ArenaMap getSelectedSandboxMenuMap() {
        if (sandboxMenuMaps.size == 0) {
            refreshSandboxMenuMaps();
        }
        if (sandboxMenuMaps.size == 0) {
            return null;
        }
        selectedSandboxMapIndex = clampSandboxMapIndex(selectedSandboxMapIndex);
        return sandboxMenuMaps.get(selectedSandboxMapIndex);
    }

    private void changeSandboxMapSelection(int direction) {
        if (sandboxMenuMaps.size == 0) {
            refreshSandboxMenuMaps();
        }
        if (sandboxMenuMaps.size == 0) {
            selectedSandboxMapIndex = 0;
            return;
        }
        selectedSandboxMapIndex =
                (selectedSandboxMapIndex + direction + sandboxMenuMaps.size) % sandboxMenuMaps.size;
        saveMenuSettings();
    }

    private String buildSandboxMapMenuValue() {
        ArenaMap map = getSelectedSandboxMenuMap();
        if (map == null) {
            return "No maps";
        }
        return map.getId() + "  " + map.getName();
    }

    private RlPolicy loadRlEnemyPolicy() {
        if (Gdx.files == null) {
            return null;
        }
        try {
            FileHandle policyFile = Gdx.files.internal(RL_ENEMY_POLICY_PATH);
            if (!policyFile.exists()) {
                return null;
            }
            RlPolicy policy = RlPolicy.fromJson(policyFile.readString("UTF-8"));
            if (policy.getObservationSize() > RL_OBSERVATION_SIZE
                    || policy.getActionSize() != RL_ACTION_SIZE) {
                Gdx.app.log(
                        "RatassGame",
                        "Ignoring RL enemy policy with observation/action size "
                                + policy.getObservationSize()
                                + "/"
                                + policy.getActionSize()
                                + "; expected "
                                + RL_OBSERVATION_SIZE
                                + "/"
                                + RL_ACTION_SIZE
                                + ". Retrain the policy for the current observation contract.");
                return null;
            }
            if (policy.getObservationSize() < RL_OBSERVATION_SIZE) {
                Gdx.app.log(
                        "RatassGame",
                        "Using older RL enemy policy with "
                                + policy.getObservationSize()
                                + " observations; new car sensors are ignored until retraining.");
            }
            return policy;
        } catch (RuntimeException exception) {
            Gdx.app.error("RatassGame", "Could not load RL enemy policy.", exception);
            return null;
        }
    }

    private void loadThemeChoices() {
        themeChoices.clear();
        addFallbackThemeChoices();
        addThemeChoicesFromManifest();
        addThemeChoicesFromDirectory();
        if (themeChoices.size == 0) {
            for (int i = 0; i < FALLBACK_THEME_CHOICES.length; i++) {
                addThemeChoiceIfMissing(FALLBACK_THEME_CHOICES[i]);
            }
        }
    }

    private void addFallbackThemeChoices() {
        for (int i = 0; i < FALLBACK_THEME_CHOICES.length; i++) {
            addThemeChoiceIfUsable(FALLBACK_THEME_CHOICES[i]);
        }
    }

    private void addThemeChoicesFromManifest() {
        if (Gdx.files == null) {
            return;
        }
        FileHandle manifest =
                Gdx.files.internal(THEME_DIRECTORY + "/" + THEME_MANIFEST_PATH);
        if (!manifest.exists()) {
            return;
        }
        try {
            String[] lines = manifest.readString("UTF-8").split("\\r?\\n");
            for (int i = 0; i < lines.length; i++) {
                addThemeChoiceIfUsable(parseThemeChoice(lines[i]));
            }
        } catch (RuntimeException exception) {
            Gdx.app.error("RatassGame", "Could not load theme manifest " + manifest.path(), exception);
        }
    }

    private void addThemeChoicesFromDirectory() {
        if (Gdx.files == null) {
            return;
        }
        try {
            FileHandle root = Gdx.files.internal(THEME_DIRECTORY);
            if (!root.exists()) {
                return;
            }
            FileHandle[] listedChildren = root.list();
            Array<FileHandle> directories = new Array<FileHandle>(listedChildren.length);
            for (int i = 0; i < listedChildren.length; i++) {
                FileHandle child = listedChildren[i];
                if (child.isDirectory()) {
                    directories.add(child);
                }
            }
            directories.sort(
                    new Comparator<FileHandle>() {
                        @Override
                        public int compare(FileHandle left, FileHandle right) {
                            return left.name().compareTo(right.name());
                        }
                    });
            for (int i = 0; i < directories.size; i++) {
                FileHandle directory = directories.get(i);
                addThemeChoiceIfUsable(
                        new ThemeChoice(directory.name(), buildThemeDisplayName(directory.name())));
            }
        } catch (RuntimeException exception) {
            Gdx.app.debug("RatassGame", "Could not list theme directory for auto-discovery.", exception);
        }
    }

    private ThemeChoice parseThemeChoice(String line) {
        String trimmed = line.trim();
        if (trimmed.length() == 0 || trimmed.startsWith("#")) {
            return null;
        }
        int separator = trimmed.indexOf('|');
        if (separator < 0) {
            separator = trimmed.indexOf('=');
        }
        String name = separator < 0 ? trimmed : trimmed.substring(0, separator).trim();
        if (name.length() == 0) {
            return null;
        }
        String displayName =
                separator < 0 ? "" : trimmed.substring(separator + 1).trim();
        if (displayName.length() == 0) {
            displayName = buildThemeDisplayName(name);
        }
        return new ThemeChoice(name, displayName);
    }

    private void addThemeChoiceIfUsable(ThemeChoice choice) {
        if (choice == null || !themeHasCarSheet(choice.name)) {
            return;
        }
        addThemeChoiceIfMissing(choice);
    }

    private void addThemeChoiceIfMissing(ThemeChoice choice) {
        if (choice == null || choice.name.length() == 0) {
            return;
        }
        if (findThemeChoiceIndex(choice.name) >= 0) {
            return;
        }
        themeChoices.add(choice);
    }

    private boolean themeHasCarSheet(String themeName) {
        return themeAssetExists(themeName, THEME_CAR_SHEET_PATH)
                || themeAssetExists(themeName, THEME_FLAT_CAR_SHEET_PATH);
    }

    private boolean themeAssetExists(String themeName, String relativePath) {
        if (Gdx.files == null || themeName == null || themeName.trim().length() == 0) {
            return false;
        }
        FileHandle handle =
                Gdx.files.internal(THEME_DIRECTORY + "/" + themeName.trim() + "/" + relativePath);
        return handle.exists();
    }

    private void loadMenuSettings() {
        loadThemeChoices();
        configuredThemeName = normalizeThemeName(loadConfiguredThemeName());
        followCameraBehind = loadConfiguredBooleanProperty(CAMERA_FOLLOW_BEHIND_PROPERTY, false);
        cameraZoom = loadConfiguredFloatProperty(CAMERA_ZOOM_PROPERTY, DEFAULT_CAMERA_ZOOM);
        mapScale = DEFAULT_MAP_SCALE;
        selectedCarCount = loadConfiguredIntProperty(CAR_COUNT_PROPERTY, DEFAULT_CAR_COUNT);
        selectedPlayerCarIndex =
                loadConfiguredIntProperty(PLAYER_CAR_PROPERTY, DEFAULT_PLAYER_CAR_INDEX);
        selectedRaceLapCount = loadConfiguredIntProperty(RACE_LAPS_PROPERTY, DEFAULT_RACE_LAPS);
        selectedSandboxMapIndex = 0;

        Preferences preferences = loadPreferences();
        if (preferences != null) {
            configuredThemeName =
                    normalizeThemeName(preferences.getString(THEME_PREF_KEY, configuredThemeName));
            followCameraBehind =
                    preferences.getBoolean(CAMERA_FOLLOW_BEHIND_PREF_KEY, followCameraBehind);
            cameraZoom = preferences.getFloat(CAMERA_ZOOM_PREF_KEY, cameraZoom);
            selectedCarCount = preferences.getInteger(CAR_COUNT_PREF_KEY, selectedCarCount);
            selectedPlayerCarIndex =
                    preferences.getInteger(PLAYER_CAR_PREF_KEY, selectedPlayerCarIndex);
            selectedRaceLapCount = preferences.getInteger(RACE_LAPS_PREF_KEY, selectedRaceLapCount);
            selectedSandboxMapIndex =
                    preferences.getInteger(SANDBOX_MAP_PREF_KEY, selectedSandboxMapIndex);
        }

        refreshSandboxMenuMaps();
        selectedThemeIndex = findThemeIndex(configuredThemeName);
        configuredThemeName = getCurrentTheme().name;
        cameraZoom = clampCameraZoom(cameraZoom);
        selectedCarCount = clampCarCount(selectedCarCount);
        selectedRaceLapCount = clampRaceLapCount(selectedRaceLapCount);
        selectedPlayerCarIndex = Math.max(0, selectedPlayerCarIndex);
        selectedSandboxMapIndex = clampSandboxMapIndex(selectedSandboxMapIndex);
    }

    private Preferences loadPreferences() {
        if (Gdx.app == null) {
            return null;
        }
        try {
            return Gdx.app.getPreferences(SETTINGS_PREFS_NAME);
        } catch (RuntimeException exception) {
            Gdx.app.error("RatassGame", "Could not load game preferences.", exception);
            return null;
        }
    }

    private void saveMenuSettings() {
        Preferences preferences = loadPreferences();
        if (preferences == null) {
            return;
        }

        preferences.putString(THEME_PREF_KEY, configuredThemeName);
        preferences.putBoolean(CAMERA_FOLLOW_BEHIND_PREF_KEY, followCameraBehind);
        preferences.putFloat(CAMERA_ZOOM_PREF_KEY, cameraZoom);
        preferences.putInteger(CAR_COUNT_PREF_KEY, selectedCarCount);
        preferences.putInteger(PLAYER_CAR_PREF_KEY, selectedPlayerCarIndex);
        preferences.putInteger(RACE_LAPS_PREF_KEY, selectedRaceLapCount);
        preferences.putInteger(SANDBOX_MAP_PREF_KEY, selectedSandboxMapIndex);
        preferences.flush();
    }

    private void createRoster() {
        roster.clear();
        CarVisual playerVisual = getPlayerCarVisual();
        boolean playerAutopilot = rlEnemyPolicy != null;
        addRosterTemplate(
                "You",
                true,
                new Color(playerVisual.color),
                playerVisual,
                "player",
                false,
                playerAutopilot);

        int enemyCount = getConfiguredEnemyCount();
        boolean modelControlledEnemies = rlEnemyPolicy != null;
        for (int enemyIndex = 0; enemyIndex < enemyCount; enemyIndex++) {
            CarVisual visual = getEnemyCarVisual(enemyIndex);
            addRosterTemplate(
                    getEnemyName(enemyIndex),
                    false,
                    new Color(visual.color),
                    visual,
                    "rl-" + enemyIndex,
                    !modelControlledEnemies,
                    modelControlledEnemies);
        }
        invalidateLeaderboard();
    }

    private int getConfiguredEnemyCount() {
        return Math.max(0, selectedCarCount - 1);
    }

    private int getRaceLapsToWin() {
        return clampRaceLapCount(selectedRaceLapCount);
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
        ensureSessionEnemyVisualIndices(enemyIndex + 1);
        if (enemyIndex >= 0 && enemyIndex < sessionEnemyVisualIndices.size) {
            return getCarVisual(sessionEnemyVisualIndices.get(enemyIndex).intValue());
        }
        return getCarVisual(enemyIndex + 1);
    }

    private void ensureSessionEnemyVisualIndices(int requiredCount) {
        int carVisualCount = getAvailableCarVisualCount();
        if (sessionEnemyVisualPaletteSize != carVisualCount) {
            sessionEnemyVisualPaletteSize = carVisualCount;
            sessionEnemyVisualIndices.clear();
            sessionEnemyVisualPool.clear();
        }

        while (sessionEnemyVisualIndices.size < requiredCount) {
            if (sessionEnemyVisualPool.size == 0) {
                refillSessionEnemyVisualPool(carVisualCount);
            }
            sessionEnemyVisualIndices.add(sessionEnemyVisualPool.pop());
        }
    }

    private void refillSessionEnemyVisualPool(int carVisualCount) {
        sessionEnemyVisualPool.clear();
        if (carVisualCount <= 0) {
            sessionEnemyVisualPool.add(Integer.valueOf(0));
            return;
        }

        int playerCarIndex = clampPlayerCarIndex(selectedPlayerCarIndex, carVisualCount);
        for (int i = 0; i < carVisualCount; i++) {
            if (carVisualCount > 1 && i == playerCarIndex) {
                continue;
            }
            sessionEnemyVisualPool.add(Integer.valueOf(i));
        }
        if (sessionEnemyVisualPool.size == 0) {
            sessionEnemyVisualPool.add(Integer.valueOf(0));
        }

        for (int i = sessionEnemyVisualPool.size - 1; i > 0; i--) {
            sessionEnemyVisualPool.swap(i, sessionEnemyVisualRandom.nextInt(i + 1));
        }
    }

    private void addRosterTemplate(
            String name,
            boolean playerControlled,
            Color color,
            CarVisual visual,
            String statsLabel) {
        addRosterTemplate(
                name,
                playerControlled,
                color,
                visual,
                statsLabel,
                false,
                false);
    }

    private void addRosterTemplate(
            String name,
            boolean playerControlled,
            Color color,
            CarVisual visual,
            String statsLabel,
            boolean externallyControlled) {
        addRosterTemplate(
                name,
                playerControlled,
                color,
                visual,
                statsLabel,
                externallyControlled,
                false);
    }

    private void addRosterTemplate(
            String name,
            boolean playerControlled,
            Color color,
            CarVisual visual,
            String statsLabel,
            boolean externallyControlled,
            boolean modelControlled) {
        roster.add(new CarTemplate(
                roster.size,
                name,
                playerControlled,
                color,
                visual,
                statsLabel,
                externallyControlled,
                modelControlled,
                CarPhysics.DEFAULT));
    }

    private CarVisual getCarVisual(int rosterIndex) {
        if (themeCarVisuals.size > 0) {
            int carVisualCount = Math.min(themeCarVisuals.size, MAX_CAR_VISUAL_COUNT);
            return themeCarVisuals.get(rosterIndex % carVisualCount);
        }
        return NORMAL_CAR_VISUALS[rosterIndex % NORMAL_CAR_VISUALS.length];
    }

    private CarVisual getPlayerCarVisual() {
        return getCarVisual(selectedPlayerCarIndex);
    }

    private int getAvailableCarVisualCount() {
        return themeCarVisuals.size > 0
                ? Math.min(themeCarVisuals.size, MAX_CAR_VISUAL_COUNT)
                : NORMAL_CAR_VISUALS.length;
    }

    private void disposeRosterSpriteTextures() {
        for (int i = 0; i < roster.size; i++) {
            CarTemplate template = roster.get(i);
            if (template.ownsSpriteTexture) {
                disposeTexture(template.spriteTexture);
            }
            template.spriteTexture = null;
            template.ownsSpriteTexture = false;
        }
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

            if (template.visual.sharedTexture) {
                template.spriteTexture = themeCarsTexture;
            } else {
                template.spriteTexture = loadTexture(template.visual.spritePath);
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
        disposeGameSounds();
        impactSound = loadSound("audio/impact.wav");
        pickupSound = loadSound("audio/pickup.wav");
        destructionSound = loadSound("audio/destroy.wav");
        countdownSound = loadSound("audio/countdown.wav");
        roundStartSound = loadSound("audio/start.wav");
        themeMusic = loadMusic("audio/music.wav");
        playThemeMusic();
    }

    private void disposeGameSounds() {
        disposeMusic(themeMusic);
        disposeSound(impactSound);
        disposeSound(pickupSound);
        disposeSound(destructionSound);
        disposeSound(countdownSound);
        disposeSound(roundStartSound);
        themeMusic = null;
        impactSound = null;
        pickupSound = null;
        destructionSound = null;
        countdownSound = null;
        roundStartSound = null;
    }

    private void playThemeMusic() {
        if (themeMusic == null) {
            return;
        }
        themeMusic.setLooping(true);
        themeMusic.setVolume(0.32f);
        themeMusic.play();
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

    private Music loadMusic(String path) {
        FileHandle handle = resolveAssetHandle(path);
        if (handle == null || !handle.exists()) {
            return null;
        }
        try {
            return Gdx.audio.newMusic(handle);
        } catch (RuntimeException exception) {
            Gdx.app.error("RatassGame", "Could not load music " + handle.path(), exception);
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
        disposeTexture(themeCarsTexture);
        themeCarsTexture = null;
        themeCarVisuals.clear();

        FileHandle carSheetHandle = resolveThemeCarSheetHandle();
        if (carSheetHandle == null || !carSheetHandle.exists()) {
            return;
        }

        Pixmap source = null;
        try {
            source = new Pixmap(carSheetHandle);
            themeCarsTexture = new Texture(carSheetHandle);
            themeCarsTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            themeCarVisuals.addAll(createThemeCarVisualsFromFixedGrid(source));
        } catch (RuntimeException exception) {
            Gdx.app.error("RatassGame", "Could not load themed car sheet " + carSheetHandle.path(), exception);
            themeCarVisuals.clear();
            disposeTexture(themeCarsTexture);
            themeCarsTexture = null;
        } finally {
            if (source != null) {
                source.dispose();
            }
        }
    }

    private void ensureMenuCarPreviewLoaded() {
        if (configuredThemeName.equals(menuCarSheetThemeName)) {
            return;
        }

        disposeMenuCarPreview();
        menuCarSheetThemeName = configuredThemeName;

        FileHandle carSheetHandle = resolveThemeCarSheetHandle();
        if (carSheetHandle == null || !carSheetHandle.exists()) {
            return;
        }

        try {
            menuCarSheetTexture = new Texture(carSheetHandle);
            menuCarSheetTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            addThemeCarGridBounds(
                    menuCarSheetTexture.getWidth(),
                    menuCarSheetTexture.getHeight(),
                    menuCarSheetSourceBounds);
            clampSelectedPlayerCarToMenuPreview();
        } catch (RuntimeException exception) {
            Gdx.app.error("RatassGame", "Could not load menu car sheet " + carSheetHandle.path(), exception);
            disposeTexture(menuCarSheetTexture);
            menuCarSheetTexture = null;
            menuCarSheetSourceBounds.clear();
        }
    }

    private void disposeMenuCarPreview() {
        disposeTexture(menuCarSheetTexture);
        menuCarSheetTexture = null;
        menuCarSheetThemeName = "";
        menuCarSheetSourceBounds.clear();
    }

    private void clampSelectedPlayerCarToMenuPreview() {
        int carCount = getMenuSelectableCarCount();
        int clampedPlayerCarIndex = clampPlayerCarIndex(selectedPlayerCarIndex, carCount);
        if (clampedPlayerCarIndex != selectedPlayerCarIndex) {
            selectedPlayerCarIndex = clampedPlayerCarIndex;
            saveMenuSettings();
        }
    }

    private int getMenuSelectableCarCount() {
        if (menuCarSheetSourceBounds.size > 0) {
            return menuCarSheetSourceBounds.size;
        }
        return getAvailableCarVisualCount();
    }

    private float getMenuCarSheetAspect() {
        if (menuCarSheetTexture != null && menuCarSheetTexture.getHeight() > 0) {
            return menuCarSheetTexture.getWidth() / (float) menuCarSheetTexture.getHeight();
        }
        return MENU_CAR_SHEET_ASPECT;
    }

    private Array<CarVisual> createThemeCarVisualsFromFixedGrid(Pixmap source) {
        Array<CarVisual> visuals = new Array<CarVisual>();
        for (int i = 0; i < MAX_CAR_VISUAL_COUNT; i++) {
            Rectangle sourceBounds = getThemeCarCellBounds(i, source.getWidth(), source.getHeight());
            visuals.add(
                    new CarVisual(
                            null,
                            Math.round(sourceBounds.x),
                            Math.round(sourceBounds.y),
                            Math.round(sourceBounds.width),
                            Math.round(sourceBounds.height),
                            true,
                            CAR_SPRITE_ROTATION_OFFSET_DEG,
                            sampleSpriteColor(source, sourceBounds)));
        }
        return visuals;
    }

    private void addThemeCarGridBounds(
            int sheetWidth, int sheetHeight, Array<Rectangle> targetBounds) {
        for (int i = 0; i < MAX_CAR_VISUAL_COUNT; i++) {
            targetBounds.add(getThemeCarCellBounds(i, sheetWidth, sheetHeight));
        }
    }

    private Rectangle getThemeCarCellBounds(int index, int sheetWidth, int sheetHeight) {
        int column = index % THEME_CAR_COLUMNS;
        int row = index / THEME_CAR_COLUMNS;
        int x0 = Math.round(column * sheetWidth / (float) THEME_CAR_COLUMNS);
        int x1 = Math.round((column + 1) * sheetWidth / (float) THEME_CAR_COLUMNS);
        int y0 = Math.round(row * sheetHeight / (float) THEME_CAR_SHEET_ROWS);
        int y1 = Math.round((row + 1) * sheetHeight / (float) THEME_CAR_SHEET_ROWS);
        return new Rectangle(x0, y0, x1 - x0, y1 - y0);
    }

    private Texture loadTexture(String path) {
        FileHandle handle = resolveAssetHandle(path);
        if (handle == null || !handle.exists()) {
            return null;
        }
        try {
            Texture texture = new Texture(handle);
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

    private FileHandle resolveThemeCarSheetHandle() {
        FileHandle nested = resolveThemedAssetHandle(THEME_CAR_SHEET_PATH);
        if (nested != null) {
            return nested;
        }
        return resolveThemedAssetHandle(THEME_FLAT_CAR_SHEET_PATH);
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
        if (!DEFAULT_THEME_NAME.equals(configuredThemeName)) {
            FileHandle defaultThemed =
                    Gdx.files.internal(THEME_DIRECTORY + "/" + DEFAULT_THEME_NAME + "/" + relativePath);
            if (defaultThemed.exists()) {
                return defaultThemed;
            }
        }
        FileHandle fallback = Gdx.files.internal(relativePath);
        return fallback.exists() ? fallback : null;
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

        if (isInGameMenuOpen()) {
            effectClock += delta;
            updateMenuLayout(hudViewport.getWorldWidth(), hudViewport.getWorldHeight());
            handleMenuInput();
            if (!isInGameMenuOpen()) {
                if (gameMode == GameMode.PLAYING) {
                    renderWorld();
                    renderHud();
                } else {
                    updateMenuLayout(hudViewport.getWorldWidth(), hudViewport.getWorldHeight());
                    renderMenu();
                }
                return;
            }
            renderWorld();
            renderHud();
            renderInGameMenuOverlay();
            return;
        }

        if (gameMode != GameMode.PLAYING) {
            effectClock += delta;
            updateMenuLayout(hudViewport.getWorldWidth(), hudViewport.getWorldHeight());
            handleMenuInput();
            renderMenu();
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
                || Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
            openPauseMenu();
            updateMenuLayout(hudViewport.getWorldWidth(), hudViewport.getWorldHeight());
            renderWorld();
            renderHud();
            renderInGameMenuOverlay();
            return;
        }

        updateTouchState();
        handleSidebarTablesPointerInput();
        frameThrottleInput = readPlayerThrottle();
        frameTurnInput = readPlayerTurn();

        update(delta);
        renderWorld();
        renderHud();
    }

    private boolean isInGameMenuOpen() {
        return gameMode == GameMode.PAUSE_MENU
                || (gameMode == GameMode.OPTIONS_MENU && optionsOpenedFromPause);
    }

    private boolean handleHudScrolled(float amountY) {
        if (gameMode != GameMode.PLAYING || Math.abs(amountY) <= 0.0001f) {
            return false;
        }

        float hudWidth = hudViewport.getWorldWidth();
        float hudHeight = hudViewport.getWorldHeight();
        float playfieldWidth = playfieldHudWidth > 0f ? playfieldHudWidth : hudWidth;
        float sidebarX = playfieldWidth;
        float sidebarWidth = Math.max(0f, hudWidth - playfieldWidth);
        if (!getSidebarTablesViewportBounds(sidebarX, sidebarWidth, hudHeight, sidebarTablesViewportBounds)) {
            return false;
        }

        hudTouchPoint.set(Gdx.input.getX(), Gdx.input.getY(), 0f);
        hudViewport.unproject(hudTouchPoint);
        if (!sidebarTablesViewportBounds.contains(hudTouchPoint.x, hudTouchPoint.y)) {
            return false;
        }

        scrollSidebarTables(
                amountY * getSidebarCompactLeaderboardRowStep() * 3f,
                sidebarX,
                sidebarWidth,
                hudHeight);
        return true;
    }

    private void handleSidebarTablesPointerInput() {
        float hudWidth = hudViewport.getWorldWidth();
        float hudHeight = hudViewport.getWorldHeight();
        float playfieldWidth = playfieldHudWidth > 0f ? playfieldHudWidth : hudWidth;
        float sidebarX = playfieldWidth;
        float sidebarWidth = Math.max(0f, hudWidth - playfieldWidth);

        updateSidebarTablesScrollbarBounds(sidebarX, sidebarWidth, hudHeight);
        if (sidebarTablesScrollbarBounds.width <= 0f) {
            sidebarTablesScrollbarDragging = false;
            return;
        }

        if (Gdx.input.justTouched()) {
            hudTouchPoint.set(Gdx.input.getX(), Gdx.input.getY(), 0f);
            hudViewport.unproject(hudTouchPoint);
            if (sidebarTablesScrollbarBounds.contains(hudTouchPoint.x, hudTouchPoint.y)) {
                sidebarTablesScrollbarDragging = true;
                sidebarTablesScrollbarGrabOffsetY = hudTouchPoint.y - sidebarTablesScrollbarBounds.y;
            }
        }

        if (!Gdx.input.isTouched()) {
            sidebarTablesScrollbarDragging = false;
            return;
        }

        if (!sidebarTablesScrollbarDragging) {
            return;
        }

        hudTouchPoint.set(Gdx.input.getX(), Gdx.input.getY(), 0f);
        hudViewport.unproject(hudTouchPoint);
        setSidebarTablesScrollFromThumb(
                hudTouchPoint.y - sidebarTablesScrollbarGrabOffsetY,
                sidebarX,
                sidebarWidth,
                hudHeight);
    }

    private void handleMenuInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
                || Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
            if (gameMode == GameMode.OPTIONS_MENU) {
                closeOptionsMenu();
                return;
            }
            if (gameMode == GameMode.PAUSE_MENU) {
                resumeGame();
                return;
            }
            Gdx.app.exit();
            return;
        }

        if (gameMode == GameMode.MAIN_MENU) {
            handleMainMenuInput();
        } else if (gameMode == GameMode.PAUSE_MENU) {
            handlePauseMenuInput();
        } else if (gameMode == GameMode.OPTIONS_MENU) {
            handleOptionsMenuInput();
        }

        if (Gdx.input.justTouched()) {
            hudTouchPoint.set(Gdx.input.getX(), Gdx.input.getY(), 0f);
            hudViewport.unproject(hudTouchPoint);
            handleMenuClick(hudTouchPoint.x, hudTouchPoint.y);
        }
    }

    private void handleMainMenuInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)
                || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            mainMenuSelection = Math.max(0, mainMenuSelection - 1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)
                || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            mainMenuSelection = Math.min(MAIN_MENU_EXIT_SELECTION, mainMenuSelection + 1);
        }
        if (mainMenuSelection == MAIN_MENU_SANDBOX_SELECTION
                && (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)
                || Gdx.input.isKeyJustPressed(Input.Keys.A))) {
            changeSandboxMapSelection(-1);
        }
        if (mainMenuSelection == MAIN_MENU_SANDBOX_SELECTION
                && (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)
                || Gdx.input.isKeyJustPressed(Input.Keys.D))) {
            changeSandboxMapSelection(1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.O)) {
            openOptionsMenu(false);
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            if (mainMenuSelection == MAIN_MENU_NEW_GAME_SELECTION) {
                startNewGame();
            } else if (mainMenuSelection == MAIN_MENU_SANDBOX_SELECTION) {
                startSandboxGame();
            } else if (mainMenuSelection == MAIN_MENU_OPTIONS_SELECTION) {
                openOptionsMenu(false);
            } else {
                Gdx.app.exit();
            }
        }
    }

    private void handlePauseMenuInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)
                || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            pauseMenuSelection = Math.max(0, pauseMenuSelection - 1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)
                || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            pauseMenuSelection = Math.min(PAUSE_MENU_EXIT_SELECTION, pauseMenuSelection + 1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.O)) {
            openOptionsMenu(true);
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            if (pauseMenuSelection == PAUSE_MENU_RESTART_SELECTION) {
                restartCurrentGame();
            } else if (pauseMenuSelection == PAUSE_MENU_OPTIONS_SELECTION) {
                openOptionsMenu(true);
            } else if (pauseMenuSelection == PAUSE_MENU_MAIN_MENU_SELECTION) {
                returnToMainMenu();
            } else {
                Gdx.app.exit();
            }
        }
    }

    private void handleOptionsMenuInput() {
        ensureEnabledOptionsSelection();
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)
                || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            changeOptionsMenuSelection(-1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)
                || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            changeOptionsMenuSelection(1);
        }

        if (optionsMenuSelection == OPTIONS_THEME_SELECTION && canChangeCarSetupOptions()) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)
                    || Gdx.input.isKeyJustPressed(Input.Keys.A)) {
                cycleTheme(-1);
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)
                    || Gdx.input.isKeyJustPressed(Input.Keys.D)) {
                cycleTheme(1);
            }
        } else if (optionsMenuSelection == OPTIONS_CARS_SELECTION && canChangeCarSetupOptions()) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)
                    || Gdx.input.isKeyJustPressed(Input.Keys.A)) {
                changeCarCount(-1);
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)
                    || Gdx.input.isKeyJustPressed(Input.Keys.D)) {
                changeCarCount(1);
            }
        } else if (optionsMenuSelection == OPTIONS_PLAYER_CAR_SELECTION && canChangeCarSetupOptions()) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)
                    || Gdx.input.isKeyJustPressed(Input.Keys.A)) {
                changePlayerCar(-1);
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)
                    || Gdx.input.isKeyJustPressed(Input.Keys.D)) {
                changePlayerCar(1);
            }
        } else if (optionsMenuSelection == OPTIONS_LAPS_SELECTION && canChangeCarSetupOptions()) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)
                    || Gdx.input.isKeyJustPressed(Input.Keys.A)) {
                changeRaceLapCount(-1);
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)
                    || Gdx.input.isKeyJustPressed(Input.Keys.D)) {
                changeRaceLapCount(1);
            }
        } else if (optionsMenuSelection == OPTIONS_CAMERA_SELECTION) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)
                    || Gdx.input.isKeyJustPressed(Input.Keys.A)
                    || Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)
                    || Gdx.input.isKeyJustPressed(Input.Keys.D)) {
                toggleCameraMode();
            }
        } else if (optionsMenuSelection == OPTIONS_ZOOM_SELECTION) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)
                    || Gdx.input.isKeyJustPressed(Input.Keys.A)) {
                changeCameraZoom(-1);
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)
                    || Gdx.input.isKeyJustPressed(Input.Keys.D)) {
                changeCameraZoom(1);
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            if (optionsMenuSelection == OPTIONS_THEME_SELECTION && canChangeCarSetupOptions()) {
                cycleTheme(1);
            } else if (optionsMenuSelection == OPTIONS_CARS_SELECTION && canChangeCarSetupOptions()) {
                changeCarCount(1);
            } else if (optionsMenuSelection == OPTIONS_PLAYER_CAR_SELECTION && canChangeCarSetupOptions()) {
                changePlayerCar(1);
            } else if (optionsMenuSelection == OPTIONS_LAPS_SELECTION && canChangeCarSetupOptions()) {
                changeRaceLapCount(1);
            } else if (optionsMenuSelection == OPTIONS_CAMERA_SELECTION) {
                toggleCameraMode();
            } else if (optionsMenuSelection == OPTIONS_ZOOM_SELECTION) {
                changeCameraZoom(1);
            } else {
                closeOptionsMenu();
            }
        }
    }

    private void openPauseMenu() {
        clearPlayerInput();
        pauseMenuSelection = PAUSE_MENU_RESTART_SELECTION;
        optionsOpenedFromPause = false;
        gameMode = GameMode.PAUSE_MENU;
    }

    private void resumeGame() {
        clearPlayerInput();
        optionsOpenedFromPause = false;
        gameMode = GameMode.PLAYING;
    }

    private void restartCurrentGame() {
        clearPlayerInput();
        optionsOpenedFromPause = false;
        startNewGame(sandboxMode);
    }

    private void returnToMainMenu() {
        clearPlayerInput();
        optionsOpenedFromPause = false;
        mainMenuSelection = MAIN_MENU_NEW_GAME_SELECTION;
        disposeGameSounds();
        gameMode = GameMode.MAIN_MENU;
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private void openOptionsMenu(boolean fromPause) {
        optionsOpenedFromPause = fromPause;
        gameMode = GameMode.OPTIONS_MENU;
        optionsMenuSelection = fromPause ? OPTIONS_CAMERA_SELECTION : OPTIONS_THEME_SELECTION;
        ensureEnabledOptionsSelection();
    }

    private void closeOptionsMenu() {
        if (optionsOpenedFromPause) {
            optionsOpenedFromPause = false;
            gameMode = GameMode.PAUSE_MENU;
            pauseMenuSelection = PAUSE_MENU_OPTIONS_SELECTION;
        } else {
            gameMode = GameMode.MAIN_MENU;
            mainMenuSelection = MAIN_MENU_OPTIONS_SELECTION;
        }
    }

    private void clearPlayerInput() {
        frameThrottleInput = 0f;
        frameTurnInput = 0f;
        touchThrottleInput = 0f;
        touchTurnInput = 0f;
        touchRestartPressed = false;
        touchRestartJustPressed = false;
    }

    private void ensureEnabledOptionsSelection() {
        if (isOptionSelectionEnabled(optionsMenuSelection)) {
            return;
        }

        for (int selection = 0; selection <= OPTIONS_BACK_SELECTION; selection++) {
            if (isOptionSelectionEnabled(selection)) {
                optionsMenuSelection = selection;
                return;
            }
        }
        optionsMenuSelection = OPTIONS_BACK_SELECTION;
    }

    private void changeOptionsMenuSelection(int direction) {
        int selection = optionsMenuSelection;
        for (int i = 0; i <= OPTIONS_BACK_SELECTION; i++) {
            selection += direction;
            if (selection < 0) {
                selection = OPTIONS_BACK_SELECTION;
            } else if (selection > OPTIONS_BACK_SELECTION) {
                selection = 0;
            }

            if (isOptionSelectionEnabled(selection)) {
                optionsMenuSelection = selection;
                return;
            }
        }
    }

    private boolean isOptionSelectionEnabled(int selection) {
        return canChangeCarSetupOptions()
                || selection == OPTIONS_CAMERA_SELECTION
                || selection == OPTIONS_ZOOM_SELECTION
                || selection == OPTIONS_BACK_SELECTION;
    }

    private boolean canChangeCarSetupOptions() {
        return !optionsOpenedFromPause;
    }

    private void handleMenuClick(float x, float y) {
        if (gameMode == GameMode.MAIN_MENU) {
            if (menuNewGameBounds.contains(x, y)) {
                mainMenuSelection = MAIN_MENU_NEW_GAME_SELECTION;
                startNewGame();
            } else if (menuSandboxPrevBounds.contains(x, y)) {
                mainMenuSelection = MAIN_MENU_SANDBOX_SELECTION;
                changeSandboxMapSelection(-1);
            } else if (menuSandboxNextBounds.contains(x, y)) {
                mainMenuSelection = MAIN_MENU_SANDBOX_SELECTION;
                changeSandboxMapSelection(1);
            } else if (menuSandboxBounds.contains(x, y)) {
                mainMenuSelection = MAIN_MENU_SANDBOX_SELECTION;
                startSandboxGame();
            } else if (menuOptionsBounds.contains(x, y)) {
                mainMenuSelection = MAIN_MENU_OPTIONS_SELECTION;
                openOptionsMenu(false);
            } else if (menuExitBounds.contains(x, y)) {
                mainMenuSelection = MAIN_MENU_EXIT_SELECTION;
                Gdx.app.exit();
            }
            return;
        }

        if (gameMode == GameMode.PAUSE_MENU) {
            if (pauseRestartBounds.contains(x, y)) {
                pauseMenuSelection = PAUSE_MENU_RESTART_SELECTION;
                restartCurrentGame();
            } else if (pauseOptionsBounds.contains(x, y)) {
                pauseMenuSelection = PAUSE_MENU_OPTIONS_SELECTION;
                openOptionsMenu(true);
            } else if (pauseMainMenuBounds.contains(x, y)) {
                pauseMenuSelection = PAUSE_MENU_MAIN_MENU_SELECTION;
                returnToMainMenu();
            } else if (pauseExitBounds.contains(x, y)) {
                pauseMenuSelection = PAUSE_MENU_EXIT_SELECTION;
                Gdx.app.exit();
            }
            return;
        }

        boolean carSetupEnabled = canChangeCarSetupOptions();
        if (carSetupEnabled && optionsThemePrevBounds.contains(x, y)) {
            optionsMenuSelection = OPTIONS_THEME_SELECTION;
            cycleTheme(-1);
        } else if (carSetupEnabled
                && (optionsThemeNextBounds.contains(x, y) || optionsThemeBounds.contains(x, y))) {
            optionsMenuSelection = OPTIONS_THEME_SELECTION;
            cycleTheme(1);
        } else if (carSetupEnabled && optionsCarsPrevBounds.contains(x, y)) {
            optionsMenuSelection = OPTIONS_CARS_SELECTION;
            changeCarCount(-1);
        } else if (carSetupEnabled
                && (optionsCarsNextBounds.contains(x, y) || optionsCarsBounds.contains(x, y))) {
            optionsMenuSelection = OPTIONS_CARS_SELECTION;
            changeCarCount(1);
        } else if (carSetupEnabled && optionsPlayerCarPrevBounds.contains(x, y)) {
            optionsMenuSelection = OPTIONS_PLAYER_CAR_SELECTION;
            changePlayerCar(-1);
        } else if (carSetupEnabled
                && (optionsPlayerCarNextBounds.contains(x, y)
                        || optionsPlayerCarBounds.contains(x, y))) {
            optionsMenuSelection = OPTIONS_PLAYER_CAR_SELECTION;
            changePlayerCar(1);
        } else if (carSetupEnabled && selectPlayerCarFromPreview(x, y)) {
            optionsMenuSelection = OPTIONS_PLAYER_CAR_SELECTION;
        } else if (carSetupEnabled && optionsLapsPrevBounds.contains(x, y)) {
            optionsMenuSelection = OPTIONS_LAPS_SELECTION;
            changeRaceLapCount(-1);
        } else if (carSetupEnabled && (optionsLapsNextBounds.contains(x, y) || optionsLapsBounds.contains(x, y))) {
            optionsMenuSelection = OPTIONS_LAPS_SELECTION;
            changeRaceLapCount(1);
        } else if (optionsCameraBounds.contains(x, y)) {
            optionsMenuSelection = OPTIONS_CAMERA_SELECTION;
            toggleCameraMode();
        } else if (optionsZoomOutBounds.contains(x, y)) {
            optionsMenuSelection = OPTIONS_ZOOM_SELECTION;
            changeCameraZoom(-1);
        } else if (optionsZoomInBounds.contains(x, y) || optionsZoomBounds.contains(x, y)) {
            optionsMenuSelection = OPTIONS_ZOOM_SELECTION;
            changeCameraZoom(1);
        } else if (optionsBackBounds.contains(x, y)) {
            optionsMenuSelection = OPTIONS_BACK_SELECTION;
            closeOptionsMenu();
        }
    }

    private void updateMenuLayout(float hudWidth, float hudHeight) {
        float width = Math.max(1f, hudWidth);
        float height = Math.max(1f, hudHeight);
        float centerX = width * 0.5f;
        float availableWidth = Math.max(1f, width - MENU_MIN_SIDE_MARGIN * 2f);
        float buttonWidth = Math.min(MENU_BUTTON_WIDTH, availableWidth);
        float menuButtonHeight = Math.min(MENU_BUTTON_HEIGHT, Math.max(42f, height * 0.11f));
        float menuButtonGap = Math.min(MENU_BUTTON_GAP, Math.max(8f, height * 0.025f));
        float buttonX = centerX - buttonWidth * 0.5f;
        float firstButtonY =
                Math.max(
                        height * 0.48f,
                        MENU_MIN_SIDE_MARGIN + (menuButtonHeight + menuButtonGap) * 3f);

        menuNewGameBounds.set(buttonX, firstButtonY, buttonWidth, menuButtonHeight);
        menuSandboxBounds.set(
                buttonX,
                firstButtonY - menuButtonHeight - menuButtonGap,
                buttonWidth,
                menuButtonHeight);
        float sandboxStepSize = Math.min(menuButtonHeight, Math.max(36f, menuButtonHeight * 0.78f));
        float sandboxStepGap = Math.min(menuButtonGap, 10f);
        if (availableWidth - buttonWidth >= (sandboxStepSize + sandboxStepGap) * 2f) {
            float sandboxStepY = menuSandboxBounds.y + (menuSandboxBounds.height - sandboxStepSize) * 0.5f;
            menuSandboxPrevBounds.set(
                    menuSandboxBounds.x - sandboxStepSize - sandboxStepGap,
                    sandboxStepY,
                    sandboxStepSize,
                    sandboxStepSize);
            menuSandboxNextBounds.set(
                    menuSandboxBounds.x + menuSandboxBounds.width + sandboxStepGap,
                    sandboxStepY,
                    sandboxStepSize,
                    sandboxStepSize);
        } else {
            menuSandboxPrevBounds.set(0f, 0f, 0f, 0f);
            menuSandboxNextBounds.set(0f, 0f, 0f, 0f);
        }
        menuOptionsBounds.set(
                buttonX,
                firstButtonY - (menuButtonHeight + menuButtonGap) * 2f,
                buttonWidth,
                menuButtonHeight);
        menuExitBounds.set(
                buttonX,
                firstButtonY - (menuButtonHeight + menuButtonGap) * 3f,
                buttonWidth,
                menuButtonHeight);

        float pauseFirstButtonY =
                Math.max(
                        height * 0.49f,
                        MENU_MIN_SIDE_MARGIN + (menuButtonHeight + menuButtonGap) * 3f);
        pauseRestartBounds.set(buttonX, pauseFirstButtonY, buttonWidth, menuButtonHeight);
        pauseOptionsBounds.set(
                buttonX,
                pauseFirstButtonY - menuButtonHeight - menuButtonGap,
                buttonWidth,
                menuButtonHeight);
        pauseMainMenuBounds.set(
                buttonX,
                pauseFirstButtonY - (menuButtonHeight + menuButtonGap) * 2f,
                buttonWidth,
                menuButtonHeight);
        pauseExitBounds.set(
                buttonX,
                pauseFirstButtonY - (menuButtonHeight + menuButtonGap) * 3f,
                buttonWidth,
                menuButtonHeight);

        boolean showCarPreview =
                gameMode == GameMode.OPTIONS_MENU && canChangeCarSetupOptions();
        if (showCarPreview) {
            ensureMenuCarPreviewLoaded();
        }

        float rowGap = Math.min(MENU_BUTTON_GAP, Math.max(8f, height * 0.025f));
        float rowHeight = Math.min(MENU_OPTION_ROW_HEIGHT, Math.max(40f, height * 0.095f));
        float rowsHeight = rowHeight * (OPTIONS_BACK_SELECTION + 1)
                + rowGap * OPTIONS_BACK_SELECTION;
        float rowWidth = Math.min(MENU_OPTION_ROW_WIDTH, availableWidth);
        float rowX = centerX - rowWidth * 0.5f;
        float rowTopY = Math.min(height * 0.67f, height - Math.max(74f, height * 0.14f));
        rowTopY = Math.max(rowTopY, MENU_MIN_SIDE_MARGIN + rowsHeight);
        float previewWidth = 0f;
        float previewHeight = 0f;
        float previewX = 0f;
        float previewY = 0f;

        if (showCarPreview) {
            float previewAspect = getMenuCarSheetAspect();
            boolean sideBySide = availableWidth >= 820f && height >= 520f;
            if (sideBySide) {
                rowWidth = Math.min(420f, Math.max(320f, availableWidth * 0.42f));
                previewWidth =
                        Math.min(
                                MENU_CAR_PREVIEW_MAX_WIDTH,
                                availableWidth - rowWidth - MENU_OPTIONS_COLUMN_GAP);
                sideBySide = previewWidth >= MENU_CAR_PREVIEW_MIN_WIDTH;
            }

            if (sideBySide) {
                float layoutWidth = rowWidth + MENU_OPTIONS_COLUMN_GAP + previewWidth;
                rowX = centerX - layoutWidth * 0.5f;
                previewX = rowX + rowWidth + MENU_OPTIONS_COLUMN_GAP;
                previewHeight = previewWidth / previewAspect;
                previewY = rowTopY - previewHeight;
            } else {
                float titleBand = Math.max(72f, height * 0.16f);
                float availablePreviewHeight =
                        Math.max(
                                72f,
                                height - titleBand - rowGap - rowsHeight - MENU_MIN_SIDE_MARGIN);
                previewWidth = Math.min(rowWidth, availableWidth);
                previewHeight = Math.min(previewWidth / previewAspect, availablePreviewHeight);
                previewWidth = Math.min(previewWidth, previewHeight * previewAspect);
                previewX = centerX - previewWidth * 0.5f;
                previewY = height - titleBand - previewHeight;
                rowTopY = previewY - rowGap;
                rowTopY = Math.max(rowTopY, MENU_MIN_SIDE_MARGIN + rowsHeight);
            }
        }

        optionsCarSheetBounds.set(previewX, previewY, previewWidth, previewHeight);

        float rowY = rowTopY - rowHeight;
        float stepButtonWidth =
                Math.min(MENU_OPTION_STEP_BUTTON_WIDTH, Math.max(32f, rowWidth * 0.18f));
        stepButtonWidth = Math.min(stepButtonWidth, rowWidth * 0.26f);

        optionsThemeBounds.set(rowX, rowY, rowWidth, rowHeight);
        optionsThemePrevBounds.set(rowX, rowY, stepButtonWidth, rowHeight);
        optionsThemeNextBounds.set(
                rowX + rowWidth - stepButtonWidth,
                rowY,
                stepButtonWidth,
                rowHeight);
        optionsCarsBounds.set(
                rowX,
                rowY - rowHeight - rowGap,
                rowWidth,
                rowHeight);
        optionsCarsPrevBounds.set(
                rowX,
                rowY - rowHeight - rowGap,
                stepButtonWidth,
                rowHeight);
        optionsCarsNextBounds.set(
                rowX + rowWidth - stepButtonWidth,
                rowY - rowHeight - rowGap,
                stepButtonWidth,
                rowHeight);
        optionsPlayerCarBounds.set(
                rowX,
                rowY - (rowHeight + rowGap) * 2f,
                rowWidth,
                rowHeight);
        optionsPlayerCarPrevBounds.set(
                rowX,
                rowY - (rowHeight + rowGap) * 2f,
                stepButtonWidth,
                rowHeight);
        optionsPlayerCarNextBounds.set(
                rowX + rowWidth - stepButtonWidth,
                rowY - (rowHeight + rowGap) * 2f,
                stepButtonWidth,
                rowHeight);
        optionsLapsBounds.set(
                rowX,
                rowY - (rowHeight + rowGap) * 3f,
                rowWidth,
                rowHeight);
        optionsLapsPrevBounds.set(
                rowX,
                rowY - (rowHeight + rowGap) * 3f,
                stepButtonWidth,
                rowHeight);
        optionsLapsNextBounds.set(
                rowX + rowWidth - stepButtonWidth,
                rowY - (rowHeight + rowGap) * 3f,
                stepButtonWidth,
                rowHeight);
        optionsCameraBounds.set(
                rowX,
                rowY - (rowHeight + rowGap) * 4f,
                rowWidth,
                rowHeight);
        optionsZoomBounds.set(
                rowX,
                rowY - (rowHeight + rowGap) * 5f,
                rowWidth,
                rowHeight);
        optionsZoomOutBounds.set(
                rowX,
                rowY - (rowHeight + rowGap) * 5f,
                stepButtonWidth,
                rowHeight);
        optionsZoomInBounds.set(
                rowX + rowWidth - stepButtonWidth,
                rowY - (rowHeight + rowGap) * 5f,
                stepButtonWidth,
                rowHeight);
        optionsBackBounds.set(
                rowX,
                rowY - (rowHeight + rowGap) * 6f,
                rowWidth,
                rowHeight);
    }

    private void cycleTheme(int direction) {
        if (themeChoices.size == 0) {
            loadThemeChoices();
        }
        if (themeChoices.size == 0) {
            return;
        }
        selectedThemeIndex = selectedThemeIndex + direction;
        if (selectedThemeIndex < 0) {
            selectedThemeIndex = themeChoices.size - 1;
        } else if (selectedThemeIndex >= themeChoices.size) {
            selectedThemeIndex = 0;
        }
        configuredThemeName = themeChoices.get(selectedThemeIndex).name;
        disposeMenuCarPreview();
        saveMenuSettings();
    }

    private void toggleCameraMode() {
        followCameraBehind = !followCameraBehind;
        saveMenuSettings();
        cameraInitialized = false;
        updateWorldCamera();
    }

    private void changeCameraZoom(int direction) {
        float nextZoom = clampCameraZoom(cameraZoom + direction * CAMERA_ZOOM_STEP);
        if (Math.abs(nextZoom - cameraZoom) < 0.001f) {
            return;
        }
        cameraZoom = nextZoom;
        saveMenuSettings();
        cameraInitialized = false;
        updateWorldCamera();
    }

    private void changeCarCount(int delta) {
        selectedCarCount = clampCarCount(selectedCarCount + delta);
        saveMenuSettings();
    }

    private void changeRaceLapCount(int delta) {
        selectedRaceLapCount = clampRaceLapCount(selectedRaceLapCount + delta);
        saveMenuSettings();
    }

    private void changePlayerCar(int delta) {
        ensureMenuCarPreviewLoaded();
        int carCount = getMenuSelectableCarCount();
        if (carCount <= 0) {
            return;
        }

        selectedPlayerCarIndex += delta;
        while (selectedPlayerCarIndex < 0) {
            selectedPlayerCarIndex += carCount;
        }
        while (selectedPlayerCarIndex >= carCount) {
            selectedPlayerCarIndex -= carCount;
        }
        saveMenuSettings();
    }

    private boolean selectPlayerCarFromPreview(float x, float y) {
        if (menuCarSheetTexture == null
                || menuCarSheetSourceBounds.size == 0
                || !optionsCarSheetBounds.contains(x, y)) {
            return false;
        }

        float sourceX =
                (x - optionsCarSheetBounds.x)
                        / optionsCarSheetBounds.width
                        * menuCarSheetTexture.getWidth();
        float sourceY =
                (optionsCarSheetBounds.y + optionsCarSheetBounds.height - y)
                        / optionsCarSheetBounds.height
                        * menuCarSheetTexture.getHeight();
        int selectedIndex = findMenuCarIndexAtSourcePosition(sourceX, sourceY);
        if (selectedIndex < 0) {
            selectedIndex = findNearestMenuCarIndex(sourceX, sourceY);
        }
        if (selectedIndex < 0) {
            return true;
        }

        selectedPlayerCarIndex = selectedIndex;
        saveMenuSettings();
        return true;
    }

    private int findMenuCarIndexAtSourcePosition(float sourceX, float sourceY) {
        for (int i = 0; i < menuCarSheetSourceBounds.size; i++) {
            if (menuCarSheetSourceBounds.get(i).contains(sourceX, sourceY)) {
                return i;
            }
        }
        return -1;
    }

    private int findNearestMenuCarIndex(float sourceX, float sourceY) {
        int nearestIndex = -1;
        float nearestDistanceSquared = Float.MAX_VALUE;
        for (int i = 0; i < menuCarSheetSourceBounds.size; i++) {
            Rectangle sourceBounds = menuCarSheetSourceBounds.get(i);
            float centerX = sourceBounds.x + sourceBounds.width * 0.5f;
            float centerY = sourceBounds.y + sourceBounds.height * 0.5f;
            float deltaX = sourceX - centerX;
            float deltaY = sourceY - centerY;
            float distanceSquared = deltaX * deltaX + deltaY * deltaY;
            if (distanceSquared < nearestDistanceSquared) {
                nearestDistanceSquared = distanceSquared;
                nearestIndex = i;
            }
        }
        return nearestIndex;
    }

    private void renderMenu() {
        hudViewport.apply();
        ScreenUtils.clear(0.045f, 0.055f, 0.065f, 1f);
        shapeRenderer.setProjectionMatrix(hudCamera.combined);
        spriteBatch.setProjectionMatrix(hudCamera.combined);

        float hudWidth = hudViewport.getWorldWidth();
        float hudHeight = hudViewport.getWorldHeight();
        drawMenuBackdrop(hudWidth, hudHeight);

        if (gameMode == GameMode.MAIN_MENU) {
            drawMainMenu(hudWidth, hudHeight);
        } else {
            drawOptionsMenu(hudWidth, hudHeight);
        }
    }

    private void renderInGameMenuOverlay() {
        hudViewport.apply();
        shapeRenderer.setProjectionMatrix(hudCamera.combined);
        spriteBatch.setProjectionMatrix(hudCamera.combined);

        float hudWidth = hudViewport.getWorldWidth();
        float hudHeight = hudViewport.getWorldHeight();
        drawMenuDimOverlay(hudWidth, hudHeight);

        if (gameMode == GameMode.PAUSE_MENU) {
            drawPauseMenu(hudWidth, hudHeight);
        } else {
            drawOptionsMenu(hudWidth, hudHeight);
        }
    }

    private void drawMenuDimOverlay(float hudWidth, float hudHeight) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.02f, 0.025f, 0.03f, 0.72f);
        shapeRenderer.rect(0f, 0f, hudWidth, hudHeight);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawMenuBackdrop(float hudWidth, float hudHeight) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        float pulse = 0.5f + 0.5f * MathUtils.sin(effectClock * 1.5f);
        shapeRenderer.setColor(0.08f, 0.10f, 0.12f, 1f);
        shapeRenderer.rect(0f, 0f, hudWidth, hudHeight);

        shapeRenderer.setColor(0.55f, 0.40f, 0.14f, 0.13f + pulse * 0.04f);
        shapeRenderer.rect(0f, hudHeight * 0.66f, hudWidth, 3f);
        shapeRenderer.rect(0f, hudHeight * 0.31f, hudWidth, 2f);
        for (int i = 0; i < 9; i++) {
            float x = (i + 0.5f) * hudWidth / 9f;
            shapeRenderer.rect(x - 1f, hudHeight * 0.15f, 2f, hudHeight * 0.72f);
        }

        shapeRenderer.setColor(0.96f, 0.78f, 0.22f, 0.10f);
        shapeRenderer.triangle(
                hudWidth * 0.18f,
                hudHeight * 0.18f,
                hudWidth * 0.50f,
                hudHeight * 0.74f,
                hudWidth * 0.82f,
                hudHeight * 0.18f);

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawMainMenu(float hudWidth, float hudHeight) {
        drawMenuButton(menuNewGameBounds, "New Game", mainMenuSelection == MAIN_MENU_NEW_GAME_SELECTION);
        drawMenuButton(menuSandboxBounds, "Sandbox", mainMenuSelection == MAIN_MENU_SANDBOX_SELECTION);
        if (sandboxMenuMaps.size > 1 && menuSandboxPrevBounds.width > 0f) {
            drawMenuStepButton(menuSandboxPrevBounds, "<", mainMenuSelection == MAIN_MENU_SANDBOX_SELECTION);
            drawMenuStepButton(menuSandboxNextBounds, ">", mainMenuSelection == MAIN_MENU_SANDBOX_SELECTION);
        }
        drawMenuButton(menuOptionsBounds, "Options", mainMenuSelection == MAIN_MENU_OPTIONS_SELECTION);
        drawMenuButton(menuExitBounds, "Exit", mainMenuSelection == MAIN_MENU_EXIT_SELECTION);

        spriteBatch.begin();
        titleFont.setColor(0.98f, 0.92f, 0.76f, 1f);
        drawTextCentered(titleFont, "RATASS", hudWidth * 0.5f, hudHeight * 0.73f);

        hudFont.setColor(0.76f, 0.84f, 0.88f, 1f);
        drawTextCentered(hudFont, getCurrentTheme().displayName, hudWidth * 0.5f, hudHeight * 0.65f);
        hudFont.setColor(0.98f, 0.92f, 0.76f, 1f);
        drawTextCentered(
                hudFont,
                truncateTextToWidth(hudFont, buildSandboxMapMenuValue(), menuSandboxBounds.width - 18f),
                hudWidth * 0.5f,
                menuSandboxBounds.y + menuSandboxBounds.height * 0.27f);
        spriteBatch.end();
    }

    private void drawPauseMenu(float hudWidth, float hudHeight) {
        drawMenuButton(
                pauseRestartBounds,
                "Restart",
                pauseMenuSelection == PAUSE_MENU_RESTART_SELECTION);
        drawMenuButton(
                pauseOptionsBounds,
                "Options",
                pauseMenuSelection == PAUSE_MENU_OPTIONS_SELECTION);
        drawMenuButton(
                pauseMainMenuBounds,
                "Main Menu",
                pauseMenuSelection == PAUSE_MENU_MAIN_MENU_SELECTION);
        drawMenuButton(
                pauseExitBounds,
                "Exit",
                pauseMenuSelection == PAUSE_MENU_EXIT_SELECTION);

        spriteBatch.begin();
        titleFont.setColor(0.98f, 0.92f, 0.76f, 1f);
        float titleBaseline =
                Math.min(
                        hudHeight - 30f,
                        Math.max(
                                hudHeight * 0.73f,
                                pauseRestartBounds.y
                                        + pauseRestartBounds.height
                                        + Math.max(34f, hudHeight * 0.045f)));
        drawTextCentered(titleFont, "Paused", hudWidth * 0.5f, titleBaseline);
        spriteBatch.end();
    }

    private void drawOptionsMenu(float hudWidth, float hudHeight) {
        boolean carSetupEnabled = canChangeCarSetupOptions();
        drawOptionRow(
                optionsThemeBounds,
                optionsMenuSelection == OPTIONS_THEME_SELECTION,
                carSetupEnabled);
        drawOptionRow(
                optionsCarsBounds,
                optionsMenuSelection == OPTIONS_CARS_SELECTION,
                carSetupEnabled);
        drawOptionRow(
                optionsPlayerCarBounds,
                optionsMenuSelection == OPTIONS_PLAYER_CAR_SELECTION,
                carSetupEnabled);
        drawOptionRow(
                optionsLapsBounds,
                optionsMenuSelection == OPTIONS_LAPS_SELECTION,
                carSetupEnabled);
        drawOptionRow(optionsCameraBounds, optionsMenuSelection == OPTIONS_CAMERA_SELECTION, true);
        drawOptionRow(optionsZoomBounds, optionsMenuSelection == OPTIONS_ZOOM_SELECTION, true);
        drawMenuButton(optionsBackBounds, "Back", optionsMenuSelection == OPTIONS_BACK_SELECTION);

        if (carSetupEnabled) {
            drawCarSheetPreview();

            drawMenuStepButton(optionsThemePrevBounds, "<", optionsMenuSelection == OPTIONS_THEME_SELECTION);
            drawMenuStepButton(optionsThemeNextBounds, ">", optionsMenuSelection == OPTIONS_THEME_SELECTION);
            drawMenuStepButton(optionsCarsPrevBounds, "<", optionsMenuSelection == OPTIONS_CARS_SELECTION);
            drawMenuStepButton(optionsCarsNextBounds, ">", optionsMenuSelection == OPTIONS_CARS_SELECTION);
            drawMenuStepButton(
                    optionsPlayerCarPrevBounds,
                    "<",
                    optionsMenuSelection == OPTIONS_PLAYER_CAR_SELECTION);
            drawMenuStepButton(
                    optionsPlayerCarNextBounds,
                    ">",
                    optionsMenuSelection == OPTIONS_PLAYER_CAR_SELECTION);
            drawMenuStepButton(optionsLapsPrevBounds, "<", optionsMenuSelection == OPTIONS_LAPS_SELECTION);
            drawMenuStepButton(optionsLapsNextBounds, ">", optionsMenuSelection == OPTIONS_LAPS_SELECTION);
        }
        drawMenuStepButton(optionsZoomOutBounds, "-", optionsMenuSelection == OPTIONS_ZOOM_SELECTION);
        drawMenuStepButton(optionsZoomInBounds, "+", optionsMenuSelection == OPTIONS_ZOOM_SELECTION);

        spriteBatch.begin();
        titleFont.setColor(0.98f, 0.92f, 0.76f, 1f);
        drawTextCentered(titleFont, "Options", hudWidth * 0.5f, getOptionsTitleBaseline(hudHeight));

        setOptionLabelColor(carSetupEnabled);
        hudFont.draw(
                spriteBatch,
                "Theme",
                optionsThemeBounds.x + optionsThemePrevBounds.width + 18f,
                optionsThemeBounds.y + optionsThemeBounds.height * 0.62f);
        setOptionValueColor(carSetupEnabled);
        drawTextRight(
                hudFont,
                getCurrentTheme().displayName,
                optionsThemeBounds.x + optionsThemeBounds.width - optionsThemeNextBounds.width - 18f,
                optionsThemeBounds.y + optionsThemeBounds.height * 0.62f);

        setOptionLabelColor(carSetupEnabled);
        hudFont.draw(
                spriteBatch,
                "Cars",
                optionsCarsBounds.x + optionsCarsPrevBounds.width + 18f,
                optionsCarsBounds.y + optionsCarsBounds.height * 0.62f);
        setOptionValueColor(carSetupEnabled);
        drawTextRight(
                hudFont,
                String.valueOf(selectedCarCount),
                optionsCarsBounds.x + optionsCarsBounds.width - optionsCarsNextBounds.width - 18f,
                optionsCarsBounds.y + optionsCarsBounds.height * 0.62f);

        setOptionLabelColor(carSetupEnabled);
        hudFont.draw(
                spriteBatch,
                "My Car",
                optionsPlayerCarBounds.x + optionsPlayerCarPrevBounds.width + 18f,
                optionsPlayerCarBounds.y + optionsPlayerCarBounds.height * 0.62f);
        setOptionValueColor(carSetupEnabled);
        drawTextRight(
                hudFont,
                buildPlayerCarMenuValue(),
                optionsPlayerCarBounds.x
                        + optionsPlayerCarBounds.width
                        - optionsPlayerCarNextBounds.width
                        - 18f,
                optionsPlayerCarBounds.y + optionsPlayerCarBounds.height * 0.62f);

        setOptionLabelColor(carSetupEnabled);
        hudFont.draw(
                spriteBatch,
                "Laps",
                optionsLapsBounds.x + optionsLapsPrevBounds.width + 18f,
                optionsLapsBounds.y + optionsLapsBounds.height * 0.62f);
        setOptionValueColor(carSetupEnabled);
        drawTextRight(
                hudFont,
                buildRaceLapsMenuValue(),
                optionsLapsBounds.x + optionsLapsBounds.width - optionsLapsNextBounds.width - 18f,
                optionsLapsBounds.y + optionsLapsBounds.height * 0.62f);

        setOptionLabelColor(true);
        hudFont.draw(
                spriteBatch,
                "Camera",
                optionsCameraBounds.x + 22f,
                optionsCameraBounds.y + optionsCameraBounds.height * 0.62f);
        setOptionValueColor(true);
        drawTextRight(
                hudFont,
                followCameraBehind ? "Chase" : "Top Down",
                optionsCameraBounds.x + optionsCameraBounds.width - 22f,
                optionsCameraBounds.y + optionsCameraBounds.height * 0.62f);

        setOptionLabelColor(true);
        hudFont.draw(
                spriteBatch,
                "Zoom",
                optionsZoomBounds.x + optionsZoomOutBounds.width + 18f,
                optionsZoomBounds.y + optionsZoomBounds.height * 0.62f);
        setOptionValueColor(true);
        drawTextRight(
                hudFont,
                buildCameraZoomMenuValue(),
                optionsZoomBounds.x + optionsZoomBounds.width - optionsZoomInBounds.width - 18f,
                optionsZoomBounds.y + optionsZoomBounds.height * 0.62f);
        spriteBatch.end();
    }

    private float getOptionsTitleBaseline(float hudHeight) {
        float contentTop =
                Math.max(
                        optionsThemeBounds.y + optionsThemeBounds.height,
                        optionsCarSheetBounds.y + optionsCarSheetBounds.height);
        return Math.min(hudHeight - 30f, contentTop + Math.max(34f, hudHeight * 0.045f));
    }

    private String buildPlayerCarMenuValue() {
        int carCount = getMenuSelectableCarCount();
        if (carCount <= 0) {
            return String.valueOf(selectedPlayerCarIndex + 1);
        }
        int displayIndex = clampPlayerCarIndex(selectedPlayerCarIndex, carCount) + 1;
        return displayIndex + "/" + carCount;
    }

    private String buildRaceLapsMenuValue() {
        int laps = getRaceLapsToWin();
        return laps == 1 ? "1 lap" : laps + " laps";
    }

    private String buildCameraZoomMenuValue() {
        return Math.round(cameraZoom * 100f) + "%";
    }

    private void drawCarSheetPreview() {
        if (menuCarSheetTexture == null
                || optionsCarSheetBounds.width <= 0f
                || optionsCarSheetBounds.height <= 0f) {
            return;
        }

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.035f, 0.042f, 0.050f, 0.92f);
        shapeRenderer.rect(
                optionsCarSheetBounds.x,
                optionsCarSheetBounds.y,
                optionsCarSheetBounds.width,
                optionsCarSheetBounds.height);
        shapeRenderer.end();

        spriteBatch.begin();
        spriteBatch.setColor(1f, 1f, 1f, 1f);
        spriteBatch.draw(
                menuCarSheetTexture,
                optionsCarSheetBounds.x,
                optionsCarSheetBounds.y,
                optionsCarSheetBounds.width,
                optionsCarSheetBounds.height);
        spriteBatch.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        drawSelectedPlayerCarFrame();
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.62f, 0.70f, 0.75f, 0.58f);
        shapeRenderer.rect(
                optionsCarSheetBounds.x,
                optionsCarSheetBounds.y,
                optionsCarSheetBounds.width,
                optionsCarSheetBounds.height);
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawSelectedPlayerCarFrame() {
        if (menuCarSheetSourceBounds.size == 0 || menuCarSheetTexture == null) {
            return;
        }

        int selectedIndex = clampPlayerCarIndex(selectedPlayerCarIndex, menuCarSheetSourceBounds.size);
        Rectangle sourceBounds = menuCarSheetSourceBounds.get(selectedIndex);
        float scaleX = optionsCarSheetBounds.width / menuCarSheetTexture.getWidth();
        float scaleY = optionsCarSheetBounds.height / menuCarSheetTexture.getHeight();
        float frameX = optionsCarSheetBounds.x + sourceBounds.x * scaleX;
        float frameY =
                optionsCarSheetBounds.y
                        + optionsCarSheetBounds.height
                        - (sourceBounds.y + sourceBounds.height) * scaleY;
        float frameWidth = sourceBounds.width * scaleX;
        float frameHeight = sourceBounds.height * scaleY;
        float padding = Math.max(2f, Math.min(frameWidth, frameHeight) * 0.06f);
        frameX = Math.max(optionsCarSheetBounds.x, frameX - padding);
        frameY = Math.max(optionsCarSheetBounds.y, frameY - padding);
        frameWidth =
                Math.min(
                        optionsCarSheetBounds.x + optionsCarSheetBounds.width - frameX,
                        frameWidth + padding * 2f);
        frameHeight =
                Math.min(
                        optionsCarSheetBounds.y + optionsCarSheetBounds.height - frameY,
                        frameHeight + padding * 2f);

        float thickness = Math.max(2f, Math.min(frameWidth, frameHeight) * 0.055f);
        shapeRenderer.setColor(0.01f, 0.012f, 0.015f, 0.96f);
        drawFrameEdges(frameX, frameY, frameWidth, frameHeight, thickness * 2.4f);
        shapeRenderer.setColor(0.00f, 0.95f, 1.00f, 0.98f);
        drawFrameEdges(
                frameX + thickness * 0.55f,
                frameY + thickness * 0.55f,
                Math.max(0f, frameWidth - thickness * 1.1f),
                Math.max(0f, frameHeight - thickness * 1.1f),
                thickness);
        shapeRenderer.setColor(1f, 1f, 1f, 0.92f);
        drawFrameEdges(
                frameX + thickness * 1.7f,
                frameY + thickness * 1.7f,
                Math.max(0f, frameWidth - thickness * 3.4f),
                Math.max(0f, frameHeight - thickness * 3.4f),
                Math.max(1f, thickness * 0.42f));
    }

    private void drawFrameEdges(float x, float y, float width, float height, float thickness) {
        if (width <= 0f || height <= 0f || thickness <= 0f) {
            return;
        }
        shapeRenderer.rect(x, y, width, thickness);
        shapeRenderer.rect(x, y + height - thickness, width, thickness);
        shapeRenderer.rect(x, y, thickness, height);
        shapeRenderer.rect(x + width - thickness, y, thickness, height);
    }

    private void drawMenuButton(Rectangle bounds, String label, boolean selected) {
        drawButtonBox(bounds, selected, 0.12f, 0.15f, 0.18f);
        spriteBatch.begin();
        tint.set(selected ? 1f : 0.92f, selected ? 0.90f : 0.96f, selected ? 0.58f : 0.98f, 1f);
        hudFont.setColor(tint);
        drawTextCentered(hudFont, label, bounds.x + bounds.width * 0.5f, bounds.y + bounds.height * 0.60f);
        spriteBatch.end();
    }

    private void drawMenuStepButton(Rectangle bounds, String label, boolean selected) {
        drawButtonBox(bounds, selected, 0.10f, 0.13f, 0.16f);
        spriteBatch.begin();
        tint.set(selected ? 1f : 0.82f, selected ? 0.90f : 0.88f, selected ? 0.58f : 0.91f, 1f);
        hudFont.setColor(tint);
        drawTextCentered(hudFont, label, bounds.x + bounds.width * 0.5f, bounds.y + bounds.height * 0.60f);
        spriteBatch.end();
    }

    private void drawOptionRow(Rectangle bounds, boolean selected, boolean enabled) {
        if (enabled) {
            drawButtonBox(bounds, selected, 0.08f, 0.10f, 0.13f);
        } else {
            drawButtonBox(bounds, false, 0.05f, 0.06f, 0.07f);
        }
    }

    private void drawButtonBox(Rectangle bounds, boolean selected, float red, float green, float blue) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        if (selected) {
            shapeRenderer.setColor(0.72f, 0.51f, 0.18f, 0.42f);
        } else {
            shapeRenderer.setColor(red, green, blue, 0.86f);
        }
        shapeRenderer.rect(bounds.x, bounds.y, bounds.width, bounds.height);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        if (selected) {
            shapeRenderer.setColor(1f, 0.82f, 0.34f, 0.86f);
        } else {
            shapeRenderer.setColor(0.62f, 0.70f, 0.75f, 0.34f);
        }
        shapeRenderer.rect(bounds.x, bounds.y, bounds.width, bounds.height);
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void setOptionLabelColor(boolean enabled) {
        if (enabled) {
            hudFont.setColor(0.82f, 0.88f, 0.91f, 1f);
        } else {
            hudFont.setColor(0.48f, 0.54f, 0.58f, 1f);
        }
    }

    private void setOptionValueColor(boolean enabled) {
        if (enabled) {
            hudFont.setColor(0.98f, 0.92f, 0.76f, 1f);
        } else {
            hudFont.setColor(0.56f, 0.58f, 0.60f, 1f);
        }
    }

    private void drawTextCentered(BitmapFont font, String text, float centerX, float baselineY) {
        glyphLayout.setText(font, text);
        font.draw(spriteBatch, text, centerX - glyphLayout.width * 0.5f, baselineY);
    }

    private void drawTextRight(BitmapFont font, String text, float rightX, float baselineY) {
        glyphLayout.setText(font, text);
        font.draw(spriteBatch, text, rightX - glyphLayout.width, baselineY);
    }

    private ThemeChoice getCurrentTheme() {
        if (themeChoices.size == 0) {
            return FALLBACK_THEME_CHOICES[0];
        }
        int clampedThemeIndex =
                Math.max(0, Math.min(selectedThemeIndex, themeChoices.size - 1));
        return themeChoices.get(clampedThemeIndex);
    }

    private void update(float delta) {
        effectClock += delta;
        updatePresentationState(delta);
        updateDestructionEffects(delta);

        if (roundOver) {
            roundOverTimer += delta;
            if (roundOverTimer >= 0.15f) {
                warmNextArenaSurfaceTexture();
            }
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
        boolean liveLapRace = isLiveLapRaceMode();
        for (int i = 0; i < cars.size; i++) {
            Car car = cars.get(i);
            boolean targetActive = checkpointTargetActive;
            Vector2 targetPosition = checkpointTargetPosition;
            Vector2 secondTargetPosition = null;
            float checkpointRadius = checkpointTargetRadius;
            float targetTimeRemaining =
                    checkpointTargetActive
                            ? MathUtils.clamp(
                                    checkpointDeadlineTimer / Math.max(0.001f, RACE_CHECKPOINT_DEFAULT_DEADLINE),
                                    0f,
                                    1f)
                            : 1f;
            if (liveLapRace && setRaceTargetsForCar(car, raceTargetPosition, raceSecondTargetPosition)) {
                targetActive = true;
                targetPosition = raceTargetPosition;
                secondTargetPosition = raceSecondTargetPosition;
                checkpointRadius = RACE_CHECKPOINT_RADIUS;
                targetTimeRemaining =
                        raceFinishTimer >= 0f
                                ? MathUtils.clamp(raceFinishTimer / RACE_FINISH_TIMEOUT, 0f, 1f)
                                : 1f;
            }
            car.step(
                    delta,
                    currentMap,
                    allowControl,
                    frameThrottleInput,
                    frameTurnInput,
                    targetActive,
                    targetPosition,
                    secondTargetPosition,
                    checkpointRadius,
                    targetTimeRemaining,
                    cars,
                    rlEnemyPolicy);
        }
        if (!allowControl && !roundOver) {
            freezeCarsForCountdown();
        }

        for (int i = 0; i < cars.size; i++) {
            cars.get(i).capturePreviousTransform();
        }
        world.step(delta, VELOCITY_ITERATIONS, POSITION_ITERATIONS);
        resolveArenaWallCollisions();

        if (!roundOver && allowControl && liveLapRace) {
            updateLapRace(delta);
        }
        if (!roundOver) {
            updateRoundState();
            if (!roundOver && allowControl) {
                updateRoundTimer(delta);
                if (GROWTH_PICKUP_ENABLED && !roundOver && !rlTrainingDisablePickups) {
                    updateGrowthPickup(delta);
                }
            }
        }
    }

    private void resolveArenaWallCollisions() {
        for (int i = 0; i < cars.size; i++) {
            Car car = cars.get(i);
            if (!car.active || car.body == null) {
                continue;
            }
            car.resolveArenaWallCollision(currentMap);
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

        if (isLiveLapRaceMode()) {
            if (aliveCars == 0) {
                finishRound(findRaceLeader());
            }
            return;
        }

        if (aliveCars > 1 || (rlTrainingAllowSoloRound && aliveCars == 1)) {
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
        arenaSurfaceTexture = null;
        preloadedNextArenaSurfaceKey = null;
        cameraInitialized = false;
        updateWorldCamera();

        if (world != null) {
            world.dispose();
        }

        world = new World(new Vector2(0f, 0f), true);
        world.setContactListener(impactContactListener);
        cars.clear();
        destructionEffects.clear();
        accumulator = 0f;
        effectClock = 0f;
        growthPickupActive = false;
        pointPickupActive = false;
        checkpointTargetActive = false;
        hasLastGrowthPickupPosition = false;
        hasLastPointPickupPosition = false;
        clearEventCallout();
        growthBoostTimer = 0f;
        boostedCar = null;
        preRoundCountdownTimer = ROUND_START_COUNTDOWN;
        countdownCueSecond = MathUtils.ceil(ROUND_START_COUNTDOWN) + 1;
        roundStartSoundPlayed = false;
        roundTimer = 0f;
        raceFinishTimer = -1f;
        checkpointDeadlineTimer = 0f;
        checkpointTargetRadius = 0f;
        impactSoundCooldown = 0f;
        destructionSoundCooldown = 0f;
        roundOver = false;
        roundOverTimer = 0f;
        winner = null;
        checkpointTargetSequence = 0;
        playerCar = null;
        finishPositionCounter = 0;
        roundNumber++;
        buildRoundSpawns(roster.size, roundSpawns);
        int fixedRaceStartCheckpointIndex =
                shouldUseFixedRaceStartCheckpoint()
                        ? findFixedRaceStartCheckpointIndex(roundSpawns)
                        : -1;

        for (int i = 0; i < roster.size; i++) {
            CarTemplate template = roster.get(i);
            template.roundFinishPosition = 0;
            template.lastRoundAwardedPoints = 0;
            template.roundPickupPoints = 0;
            template.roundRaceLap = 0;
            template.roundRaceCheckpointsCompleted = 0;
            template.roundRaceFinishTime = 0f;
            template.roundRaceCurrentLapStartTime = 0f;
            template.roundRaceBestLapTime = 0f;
            template.roundRaceLapCounterCrossed = false;
            template.roundRaceLapTimes.clear();
            template.roundRaceFinished = false;
            SpawnPoint spawnPoint = roundSpawns.get(i);
            int initialCheckpointIndex =
                    fixedRaceStartCheckpointIndex >= 0
                            ? fixedRaceStartCheckpointIndex
                            : findRoundRaceCheckpointIndexForSpawn(spawnPoint);
            template.roundRaceStartCheckpointIndex = initialCheckpointIndex;
            template.roundRaceNextCheckpointIndex = initialCheckpointIndex;
            Car car = createCar(template, spawnPoint);
            template.currentCar = car;
            if (template.playerControlled) {
                playerCar = car;
            }
        }

        cameraInitialized = false;
        updateWorldCamera();
        warmArenaSurfaceTextures();
        invalidateLeaderboard();
        if (!rlTrainingMode && currentMap.getCheckpointCount() <= 1) {
            throw new IllegalStateException("Race maps require at least two ordered checkpoints.");
        }
    }

    private boolean shouldUseFixedRaceStartCheckpoint() {
        return currentMap != null
                && currentMap.getCheckpointCount() > 1
                && !rlTrainingRandomSpawnLocations
                && (rlTrainingRaceMode || isLiveLapRaceMode());
    }

    private int findFixedRaceStartCheckpointIndex(Array<SpawnPoint> spawnPoints) {
        if (currentMap == null || currentMap.getCheckpointCount() <= 1 || spawnPoints.size == 0) {
            return -1;
        }

        float forwardX = 0f;
        float forwardY = 0f;
        for (int i = 0; i < spawnPoints.size; i++) {
            SpawnPoint spawnPoint = spawnPoints.get(i);
            forwardX += -MathUtils.sin(spawnPoint.angleRad);
            forwardY += MathUtils.cos(spawnPoint.angleRad);
        }
        float forwardLength = (float) Math.sqrt(forwardX * forwardX + forwardY * forwardY);
        if (forwardLength <= 0.0001f) {
            return -1;
        }
        forwardX /= forwardLength;
        forwardY /= forwardLength;

        SpawnPoint frontSpawn = null;
        float bestProjection = -Float.MAX_VALUE;
        for (int i = 0; i < spawnPoints.size; i++) {
            SpawnPoint spawnPoint = spawnPoints.get(i);
            float projection = spawnPoint.x * forwardX + spawnPoint.y * forwardY;
            if (frontSpawn == null || projection > bestProjection) {
                frontSpawn = spawnPoint;
                bestProjection = projection;
            }
        }
        if (frontSpawn == null) {
            return -1;
        }

        spawnCandidate.set(frontSpawn.x, frontSpawn.y);
        int checkpointCount = currentMap.getCheckpointCount();
        int bestIndex = -1;
        float bestScore = Float.MAX_VALUE;
        for (int i = 0; i < checkpointCount; i++) {
            SpawnPoint checkpoint = currentMap.getCheckpoint(i);
            float dx = checkpoint.x - frontSpawn.x;
            float dy = checkpoint.y - frontSpawn.y;
            float directDistance = (float) Math.sqrt(dx * dx + dy * dy);
            if (directDistance <= 0.0001f) {
                continue;
            }

            float forwardProjection = dx * forwardX + dy * forwardY;
            if (forwardProjection <= 0.25f) {
                continue;
            }

            raceTargetPosition.set(checkpoint.x, checkpoint.y);
            float routeDistance =
                    currentMap.estimateDriveDistance(spawnCandidate, raceTargetPosition, RL_ROUTE_MARGIN);
            currentMap.findDriveTarget(
                    spawnCandidate,
                    raceTargetPosition,
                    RL_ROUTE_MARGIN,
                    raceSecondTargetPosition);
            float routeDx = raceSecondTargetPosition.x - frontSpawn.x;
            float routeDy = raceSecondTargetPosition.y - frontSpawn.y;
            float routeTargetDistance = (float) Math.sqrt(routeDx * routeDx + routeDy * routeDy);
            if (routeTargetDistance <= 0.0001f) {
                continue;
            }

            float routeAlignment = (routeDx * forwardX + routeDy * forwardY) / routeTargetDistance;
            if (routeAlignment <= -0.05f) {
                continue;
            }

            float sideDistance = Math.abs(dx * forwardY - dy * forwardX);
            float score = routeDistance + sideDistance * 0.32f - forwardProjection * 0.08f;
            if (score < bestScore) {
                bestScore = score;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private int findRoundRaceCheckpointIndexForSpawn(SpawnPoint spawnPoint) {
        if (currentMap == null || currentMap.getCheckpointCount() <= 1 || spawnPoint == null) {
            return 0;
        }
        if (rlTrainingRandomSpawnLocations && (rlTrainingRaceMode || isLiveLapRaceMode())) {
            int checkpointIndex = findRandomRaceSpawnCheckpointIndex(spawnPoint.x, spawnPoint.y);
            if (checkpointIndex >= 0) {
                return checkpointIndex;
            }
        }
        return findInitialRaceCheckpointIndex(spawnPoint);
    }

    private int findInitialRaceCheckpointIndex(SpawnPoint spawnPoint) {
        if (currentMap == null || currentMap.getCheckpointCount() <= 1 || spawnPoint == null) {
            return 0;
        }

        int checkpointCount = currentMap.getCheckpointCount();
        float forwardX = -MathUtils.sin(spawnPoint.angleRad);
        float forwardY = MathUtils.cos(spawnPoint.angleRad);
        spawnCandidate.set(spawnPoint.x, spawnPoint.y);

        int bestIndex = -1;
        float bestRouteDistance = Float.MAX_VALUE;
        int bestDirectIndex = -1;
        float bestDirectRouteDistance = Float.MAX_VALUE;
        for (int i = 0; i < checkpointCount; i++) {
            SpawnPoint checkpoint = currentMap.getCheckpoint(i);
            float dx = checkpoint.x - spawnPoint.x;
            float dy = checkpoint.y - spawnPoint.y;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            if (distance <= 0.0001f) {
                continue;
            }

            float alignment = (dx * forwardX + dy * forwardY) / distance;
            if (alignment <= 0.05f) {
                continue;
            }

            float checkpointForwardX = -MathUtils.sin(checkpoint.angleRad);
            float checkpointForwardY = MathUtils.cos(checkpoint.angleRad);
            float checkpointCrossAlignment =
                    (dx * checkpointForwardX + dy * checkpointForwardY) / distance;
            if (checkpointCrossAlignment <= 0.05f) {
                continue;
            }

            raceTargetPosition.set(checkpoint.x, checkpoint.y);
            float routeDistance =
                    currentMap.estimateDriveDistance(spawnCandidate, raceTargetPosition, RL_ROUTE_MARGIN);
            if (routeDistance < bestDirectRouteDistance) {
                bestDirectRouteDistance = routeDistance;
                bestDirectIndex = i;
            }

            currentMap.findDriveTarget(
                    spawnCandidate,
                    raceTargetPosition,
                    RL_ROUTE_MARGIN,
                    raceSecondTargetPosition);
            float routeDx = raceSecondTargetPosition.x - spawnPoint.x;
            float routeDy = raceSecondTargetPosition.y - spawnPoint.y;
            float routeTargetDistance = (float) Math.sqrt(routeDx * routeDx + routeDy * routeDy);
            if (routeTargetDistance <= 0.0001f) {
                continue;
            }

            float routeAlignment = (routeDx * forwardX + routeDy * forwardY) / routeTargetDistance;
            if (routeAlignment > 0.05f && routeDistance < bestRouteDistance) {
                bestRouteDistance = routeDistance;
                bestIndex = i;
            }
        }

        if (bestIndex >= 0) {
            return bestIndex;
        }
        return bestDirectIndex >= 0 ? bestDirectIndex : 1;
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

    private void updateRoundTimer(float delta) {
        roundTimer += delta;
    }

    private boolean isLiveLapRaceMode() {
        return !rlTrainingMode && currentMap != null && currentMap.getCheckpointCount() > 1;
    }

    private boolean setRaceTargetsForCar(Car car, Vector2 targetOut, Vector2 secondTargetOut) {
        if (car == null || car.template == null || currentMap == null || currentMap.getCheckpointCount() <= 1) {
            return false;
        }
        int checkpointCount = currentMap.getCheckpointCount();
        int checkpointIndex =
                MathUtils.clamp(car.template.roundRaceNextCheckpointIndex, 0, checkpointCount - 1);
        SpawnPoint checkpoint = currentMap.getCheckpoint(checkpointIndex);
        targetOut.set(checkpoint.x, checkpoint.y);

        int secondCheckpointIndex = (checkpointIndex + 1) % checkpointCount;
        SpawnPoint secondCheckpoint = currentMap.getCheckpoint(secondCheckpointIndex);
        secondTargetOut.set(secondCheckpoint.x, secondCheckpoint.y);
        return true;
    }

    private void updateLapRace(float delta) {
        for (int i = 0; i < cars.size; i++) {
            updateCarLapProgress(cars.get(i));
        }

        if (raceFinishTimer >= 0f) {
            raceFinishTimer = Math.max(0f, raceFinishTimer - delta);
            if (raceFinishTimer <= 0f || haveAllActiveCarsFinishedRace()) {
                finishRound(winner != null ? winner : findRaceLeader());
            }
        }
    }

    private void updateCarLapProgress(Car car) {
        if (car == null
                || !car.active
                || car.body == null
                || car.template.roundRaceFinished
                || currentMap == null
                || currentMap.getCheckpointCount() <= 1) {
            return;
        }

        int checkpointCount = currentMap.getCheckpointCount();
        int checkpointIndex =
                MathUtils.clamp(car.template.roundRaceNextCheckpointIndex, 0, checkpointCount - 1);
        SpawnPoint checkpoint = currentMap.getCheckpoint(checkpointIndex);
        if (!hasPassedRaceCheckpoint(car, checkpoint)) {
            return;
        }

        int crossedCheckpointIndex = checkpointIndex;
        car.template.roundRaceCheckpointsCompleted++;
        checkpointIndex = (checkpointIndex + 1) % checkpointCount;
        if (crossedCheckpointIndex == car.template.roundRaceStartCheckpointIndex) {
            if (car.template.roundRaceLapCounterCrossed) {
                recordCompletedRaceLap(car.template);
                car.template.roundRaceLap++;
            } else {
                car.template.roundRaceLapCounterCrossed = true;
                car.template.roundRaceCurrentLapStartTime = roundTimer;
            }
        }
        car.template.roundRaceNextCheckpointIndex = checkpointIndex;
        invalidateLeaderboard();

        if (car.template.roundRaceLap >= getRaceLapsToWin()) {
            markRaceFinished(car);
        }
    }

    private void recordCompletedRaceLap(CarTemplate template) {
        if (template == null) {
            return;
        }
        float lapTime = Math.max(0f, roundTimer - template.roundRaceCurrentLapStartTime);
        if (lapTime > 0.05f
                && (template.roundRaceBestLapTime <= 0f || lapTime < template.roundRaceBestLapTime)) {
            template.roundRaceBestLapTime = lapTime;
        }
        template.roundRaceLapTimes.add(lapTime);
        template.roundRaceCurrentLapStartTime = roundTimer;
    }

    private boolean hasPassedRaceCheckpoint(Car car, SpawnPoint checkpoint) {
        if (car == null || car.body == null || checkpoint == null) {
            return false;
        }
        if (!isMovingForwardThroughCheckpoint(car)) {
            return false;
        }
        if (!checkpoint.hasGate) {
            return car.body.getPosition().dst2(checkpoint.x, checkpoint.y)
                    <= RACE_CHECKPOINT_RADIUS * RACE_CHECKPOINT_RADIUS;
        }
        return hasCrossedRaceCheckpointGate(car, checkpoint);
    }

    private boolean isMovingForwardThroughCheckpoint(Car car) {
        return car != null
                && car.body != null
                && car.getSignedForwardSpeed() >= RACE_CHECKPOINT_MIN_FORWARD_CROSS_SPEED;
    }

    private boolean hasCrossedRaceCheckpointGate(Car car, SpawnPoint checkpoint) {
        Vector2 current = car.body.getPosition();
        return isNearRaceCheckpointGate(current, checkpoint, RACE_CHECKPOINT_GATE_MARGIN);
    }

    private boolean isNearRaceCheckpointGate(Vector2 position, SpawnPoint checkpoint, float margin) {
        if (position == null || checkpoint == null || !checkpoint.hasGate) {
            return false;
        }
        float segmentX = checkpoint.gateEndX - checkpoint.gateStartX;
        float segmentY = checkpoint.gateEndY - checkpoint.gateStartY;
        float lengthSquared = segmentX * segmentX + segmentY * segmentY;
        if (lengthSquared <= 0.0001f) {
            return position.dst2(checkpoint.x, checkpoint.y) <= margin * margin;
        }
        float projection =
                ((position.x - checkpoint.gateStartX) * segmentX
                                + (position.y - checkpoint.gateStartY) * segmentY)
                        / lengthSquared;
        projection = MathUtils.clamp(projection, 0f, 1f);
        float closestX = checkpoint.gateStartX + segmentX * projection;
        float closestY = checkpoint.gateStartY + segmentY * projection;
        return position.dst2(closestX, closestY) <= margin * margin;
    }

    private void markRaceFinished(Car car) {
        if (car == null || car.template.roundRaceFinished) {
            return;
        }

        car.template.roundFinishPosition = ++finishPositionCounter;
        car.template.roundRaceFinishTime = roundTimer;
        car.template.roundRaceFinished = true;
        if (winner == null) {
            winner = car;
            raceFinishTimer = RACE_FINISH_TIMEOUT;
            announceEvent(
                    "FINISH",
                    (car.playerControlled ? "You" : car.name)
                            + " completed "
                            + getRaceLapsToWin()
                            + " laps. "
                            + MathUtils.ceil(RACE_FINISH_TIMEOUT)
                            + "s left.",
                    new Color(0.36f, 0.92f, 1f, 1f));
        }
        invalidateLeaderboard();
    }

    private boolean haveAllActiveCarsFinishedRace() {
        boolean activeFound = false;
        for (int i = 0; i < cars.size; i++) {
            Car car = cars.get(i);
            if (car == null || !car.active) {
                continue;
            }
            activeFound = true;
            if (!car.template.roundRaceFinished) {
                return false;
            }
        }
        return activeFound;
    }

    private Car findRaceLeader() {
        Car best = null;
        for (int i = 0; i < cars.size; i++) {
            Car car = cars.get(i);
            if (car == null || car.template == null) {
                continue;
            }
            if (best == null || compareRaceStanding(car.template, best.template) < 0) {
                best = car;
            }
        }
        return best;
    }

    private int compareRaceStanding(CarTemplate left, CarTemplate right) {
        if (left.roundRaceFinished || right.roundRaceFinished) {
            if (!left.roundRaceFinished) {
                return 1;
            }
            if (!right.roundRaceFinished) {
                return -1;
            }
            return left.roundFinishPosition - right.roundFinishPosition;
        }
        if (left.roundRaceCheckpointsCompleted != right.roundRaceCheckpointsCompleted) {
            return right.roundRaceCheckpointsCompleted - left.roundRaceCheckpointsCompleted;
        }
        boolean leftActive = left.currentCar != null && left.currentCar.active;
        boolean rightActive = right.currentCar != null && right.currentCar.active;
        if (leftActive != rightActive) {
            return leftActive ? -1 : 1;
        }
        if (left.playerControlled != right.playerControlled) {
            return left.playerControlled ? -1 : 1;
        }
        return left.name.compareTo(right.name);
    }

    private int compareFinishPosition(int leftPosition, int rightPosition) {
        if (leftPosition <= 0 || rightPosition <= 0) {
            if (leftPosition <= 0 && rightPosition <= 0) {
                return 0;
            }
            return leftPosition <= 0 ? 1 : -1;
        }
        return leftPosition - rightPosition;
    }

    private void assignRemainingRaceFinishPositions() {
        Array<CarTemplate> standings = new Array<CarTemplate>();
        standings.addAll(roster);
        standings.sort(new Comparator<CarTemplate>() {
            @Override
            public int compare(CarTemplate left, CarTemplate right) {
                return compareRaceStanding(left, right);
            }
        });
        for (int i = 0; i < standings.size; i++) {
            CarTemplate template = standings.get(i);
            if (template.roundFinishPosition == 0) {
                template.roundFinishPosition = ++finishPositionCounter;
            }
        }
    }

    private void finalizeRoundResults() {
        if (isLiveLapRaceMode()) {
            assignRemainingRaceFinishPositions();
        }
        for (int i = 0; i < roster.size; i++) {
            CarTemplate template = roster.get(i);
            if (template.roundFinishPosition == 0) {
                template.roundFinishPosition = ++finishPositionCounter;
            }
            template.lastRoundAwardedPoints = getRacePointsForPosition(template.roundFinishPosition);
            template.totalPoints += template.lastRoundAwardedPoints;
        }

        if (winner != null && winner.playerControlled) {
            playerWins++;
        }

        invalidateLeaderboard();
    }

    private int getRacePointsForPosition(int finishPosition) {
        if (finishPosition <= 0 || finishPosition > RACE_POSITION_POINTS.length) {
            return 0;
        }
        return RACE_POSITION_POINTS[finishPosition - 1];
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
        if (!isLiveLapRaceMode() && car.template.roundFinishPosition == 0) {
            car.template.roundFinishPosition = ++finishPositionCounter;
        }
        car.eliminatedByAttackerId = car.lastAttackerId;
        car.eliminate(world);
        invalidateLeaderboard();
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

        float visibleWidth = Math.max(1f, worldCamera.viewportWidth);
        float visibleHeight = Math.max(1f, worldCamera.viewportHeight);
        float zoomX = mapBounds.width / visibleWidth;
        float zoomY = mapBounds.height / visibleHeight;

        float mapFitZoom = Math.max(0.01f, Math.max(zoomX, zoomY));
        float targetZoom = mapFitZoom;
        cameraTargetPosition.set(
                mapBounds.x + mapBounds.width * 0.5f,
                mapBounds.y + mapBounds.height * 0.5f);
        float delta = Math.min(Gdx.graphics.getDeltaTime(), 1f / 30f);

        boolean playerCameraActive = playerCar != null && playerCar.active && playerCar.body != null;

        if (playerCameraActive) {
            Vector2 playerPosition = playerCar.getRenderPosition();
            Vector2 playerVelocity = playerCar.body.getLinearVelocity();
            float playerSpeed = playerVelocity.len();
            float speedFactor = MathUtils.clamp(playerSpeed / playerCar.getForwardMaxSpeed(), 0f, 1f);
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
        } else {
            cameraSmoothedForwardDirection.set(0f, 1f);
        }

        if (playerCameraActive) {
            targetZoom = applyConfiguredCameraZoom(targetZoom);
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

    private float applyConfiguredCameraZoom(float targetZoom) {
        return MathUtils.clamp(
                targetZoom / cameraZoom,
                MIN_WORLD_CAMERA_ZOOM / MAX_CAMERA_ZOOM,
                PLAYER_CAMERA_MAX_ZOOM / MIN_CAMERA_ZOOM);
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

        float mapCenterX = mapBounds.x + mapBounds.width * 0.5f;
        float mapCenterY = mapBounds.y + mapBounds.height * 0.5f;
        position.x = minX > maxX ? mapCenterX : MathUtils.clamp(position.x, minX, maxX);
        position.y = minY > maxY ? mapCenterY : MathUtils.clamp(position.y, minY, maxY);
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

        if (rlTrainingRandomSpawnLocations) {
            if (buildRandomRoundSpawns(count, out, safeMargin, minDistance, maxAttempts)) {
                return;
            }
        }
        if ((rlTrainingRaceMode || isLiveLapRaceMode()) && buildRaceGridSpawns(count, out)) {
            return;
        }

        int mapSpawnCount = currentMap.getSpawnCount();
        for (int i = 0; i < mapSpawnCount && out.size < count; i++) {
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
            if ((rlTrainingRaceMode || isLiveLapRaceMode())
                    && isRandomRaceSpawnTooCloseToCheckpoint(spawnCandidate)) {
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

    private boolean buildRaceGridSpawns(int count, Array<SpawnPoint> out) {
        if (currentMap == null || currentMap.getSpawnCount() < count) {
            return false;
        }

        for (int i = 0; i < count; i++) {
            out.add(currentMap.getSpawn(i));
        }
        return out.size == count;
    }

    private boolean buildRandomRoundSpawns(
            int count,
            Array<SpawnPoint> out,
            float safeMargin,
            float minDistance,
            int maxAttempts) {
        float minX = mapBounds.x + safeMargin;
        float maxX = mapBounds.x + mapBounds.width - safeMargin;
        float minY = mapBounds.y + safeMargin;
        float maxY = mapBounds.y + mapBounds.height - safeMargin;
        int attempts = Math.max(maxAttempts, count * 256);

        for (int attempt = 0; attempt < attempts && out.size < count; attempt++) {
            spawnCandidate.set(MathUtils.random(minX, maxX), MathUtils.random(minY, maxY));
            currentMap.clampToPlayable(spawnCandidate, safeMargin);

            if (currentMap.distanceToHazard(spawnCandidate) < safeMargin) {
                continue;
            }
            if ((rlTrainingRaceMode || isLiveLapRaceMode())
                    && isRandomRaceSpawnTooCloseToCheckpoint(spawnCandidate)) {
                continue;
            }
            if (!isSpawnLocationClear(spawnCandidate, out, minDistance)) {
                continue;
            }

            out.add(
                    rlTrainingRaceMode || isLiveLapRaceMode()
                            ? randomRaceSpawnPoint(spawnCandidate.x, spawnCandidate.y)
                            : randomSpawnPoint(spawnCandidate.x, spawnCandidate.y));
        }

        if (out.size == count) {
            return true;
        }

        out.clear();
        return false;
    }

    private SpawnPoint randomSpawnPoint(float x, float y) {
        return new SpawnPoint(x, y, MathUtils.random(-MathUtils.PI, MathUtils.PI));
    }

    private SpawnPoint randomRaceSpawnPoint(float x, float y) {
        if (currentMap == null || currentMap.getCheckpointCount() <= 1) {
            return randomSpawnPoint(x, y);
        }

        int checkpointIndex = findRandomRaceSpawnCheckpointIndex(x, y);
        if (checkpointIndex < 0) {
            return randomSpawnPoint(x, y);
        }

        SpawnPoint checkpoint = currentMap.getCheckpoint(checkpointIndex);
        spawnCandidate.set(x, y);
        raceTargetPosition.set(checkpoint.x, checkpoint.y);
        currentMap.findDriveTarget(
                spawnCandidate,
                raceTargetPosition,
                RL_ROUTE_MARGIN,
                raceSecondTargetPosition);
        if (raceSecondTargetPosition.dst2(x, y)
                <= RL_RANDOM_SPAWN_ROUTE_TARGET_EPSILON * RL_RANDOM_SPAWN_ROUTE_TARGET_EPSILON) {
            SpawnPoint secondCheckpoint =
                    currentMap.getCheckpoint((checkpointIndex + 1) % currentMap.getCheckpointCount());
            raceSecondTargetPosition.set(secondCheckpoint.x, secondCheckpoint.y);
        }

        return SpawnPoint.facingPoint(
                x,
                y,
                raceSecondTargetPosition.x,
                raceSecondTargetPosition.y);
    }

    private int findRandomRaceSpawnCheckpointIndex(float x, float y) {
        int checkpointCount = currentMap == null ? 0 : currentMap.getCheckpointCount();
        if (checkpointCount <= 0) {
            return -1;
        }

        spawnCandidate.set(x, y);
        int bestIndex = -1;
        float bestScore = Float.MAX_VALUE;
        for (int i = 0; i < checkpointCount; i++) {
            SpawnPoint checkpoint = currentMap.getCheckpoint(i);
            float dx = checkpoint.x - x;
            float dy = checkpoint.y - y;
            float directDistance = (float) Math.sqrt(dx * dx + dy * dy);
            if (directDistance <= 0.0001f) {
                continue;
            }

            raceTargetPosition.set(checkpoint.x, checkpoint.y);
            float routeDistance =
                    currentMap.estimateDriveDistance(spawnCandidate, raceTargetPosition, RL_ROUTE_MARGIN);
            float checkpointForwardX = -MathUtils.sin(checkpoint.angleRad);
            float checkpointForwardY = MathUtils.cos(checkpoint.angleRad);
            float checkpointForwardAlignment =
                    (dx * checkpointForwardX + dy * checkpointForwardY) / directDistance;
            float backwardPenalty =
                    checkpointForwardAlignment >= -0.05f
                            ? 0f
                            : RL_RANDOM_SPAWN_BACKWARD_CHECKPOINT_PENALTY
                                    * (-checkpointForwardAlignment);
            float nearCheckpointPenalty =
                    routeDistance >= RL_RANDOM_SPAWN_MIN_ROUTE_DISTANCE
                            ? 0f
                            : (RL_RANDOM_SPAWN_MIN_ROUTE_DISTANCE - routeDistance)
                                    * RL_RANDOM_SPAWN_NEAR_CHECKPOINT_PENALTY;
            float score = routeDistance + backwardPenalty + nearCheckpointPenalty;
            if (score < bestScore) {
                bestScore = score;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private boolean isRandomRaceSpawnTooCloseToCheckpoint(Vector2 candidate) {
        int checkpointCount = currentMap == null ? 0 : currentMap.getCheckpointCount();
        if (candidate == null || checkpointCount <= 0) {
            return false;
        }

        float minDistanceSq =
                RL_RANDOM_SPAWN_CHECKPOINT_CLEARANCE * RL_RANDOM_SPAWN_CHECKPOINT_CLEARANCE;
        for (int i = 0; i < checkpointCount; i++) {
            SpawnPoint checkpoint = currentMap.getCheckpoint(i);
            if (checkpoint.hasGate) {
                if (distanceToRaceCheckpointGateSquared(candidate, checkpoint) < minDistanceSq) {
                    return true;
                }
            } else if (candidate.dst2(checkpoint.x, checkpoint.y) < minDistanceSq) {
                return true;
            }
        }
        return false;
    }

    private float distanceToRaceCheckpointGateSquared(Vector2 position, SpawnPoint checkpoint) {
        float segmentX = checkpoint.gateEndX - checkpoint.gateStartX;
        float segmentY = checkpoint.gateEndY - checkpoint.gateStartY;
        float lengthSquared = segmentX * segmentX + segmentY * segmentY;
        if (lengthSquared <= 0.0001f) {
            return position.dst2(checkpoint.x, checkpoint.y);
        }
        float projection =
                ((position.x - checkpoint.gateStartX) * segmentX
                                + (position.y - checkpoint.gateStartY) * segmentY)
                        / lengthSquared;
        projection = MathUtils.clamp(projection, 0f, 1f);
        float closestX = checkpoint.gateStartX + segmentX * projection;
        float closestY = checkpoint.gateStartY + segmentY * projection;
        return position.dst2(closestX, closestY);
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
        return MathUtils.lerp(
                ROUND_SPAWN_SAFE_MARGIN,
                ROUND_SPAWN_CROWDED_SAFE_MARGIN,
                crowdedAlpha);
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
        bodyDef.linearDamping = template.physics.linearDamping;
        bodyDef.angularDamping = template.physics.angularDamping;
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
        invalidateLeaderboard();
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
    }

    private boolean trySpawnGrowthPickup(boolean avoidCars, boolean requireNewSpot) {
        for (int attempt = 0; attempt < GROWTH_PICKUP_SPAWN_ATTEMPTS; attempt++) {
            pickupCandidate.set(
                    MathUtils.random(mapBounds.x, mapBounds.x + mapBounds.width),
                    MathUtils.random(mapBounds.y, mapBounds.y + mapBounds.height));

            if (!isGrowthPickupCandidate(pickupCandidate, avoidCars, requireNewSpot)) {
                continue;
            }

            growthPickupPosition.set(pickupCandidate);
            growthPickupActive = true;
            return true;
        }

        return false;
    }

    private boolean isGrowthPickupCandidate(
            Vector2 candidate,
            boolean avoidCars,
            boolean requireNewSpot) {
        if (!currentMap.supports(candidate)) {
            return false;
        }

        if (currentMap.distanceToHazard(candidate) < GROWTH_PICKUP_SPAWN_MARGIN) {
            return false;
        }
        if (requireNewSpot
                && hasLastGrowthPickupPosition
                && candidate.dst2(lastGrowthPickupPosition)
                < GROWTH_PICKUP_MIN_MOVE_DISTANCE * GROWTH_PICKUP_MIN_MOVE_DISTANCE) {
            return false;
        }
        if (avoidCars && !isPickupFarFromCars(candidate, GROWTH_PICKUP_RADIUS, 0.75f)) {
            return false;
        }
        return isPickupSeparated(candidate, pointPickupPosition, pointPickupActive, PICKUP_MIN_SEPARATION);
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
        ensureArenaSurfaceTexture();

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
        if (GROWTH_PICKUP_ENABLED) {
            drawGrowthPickup();
        }
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
        if (currentMap.hasSurfaceImage()) {
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
        if (currentMap.hasSurfaceImage()) {
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

        float drawX = mapBounds.x;
        float drawY = mapBounds.y;
        float drawWidth = mapBounds.width;
        float drawHeight = mapBounds.height;
        float textureAspect = arenaSurfaceTexture.getWidth() / (float) arenaSurfaceTexture.getHeight();
        float mapAspect = mapBounds.width / mapBounds.height;
        if (textureAspect > mapAspect) {
            drawHeight = drawWidth / textureAspect;
            drawY += (mapBounds.height - drawHeight) * 0.5f;
        } else if (textureAspect < mapAspect) {
            drawWidth = drawHeight * textureAspect;
            drawX += (mapBounds.width - drawWidth) * 0.5f;
        }

        spriteBatch.setColor(1f, 1f, 1f, 1f);
        spriteBatch.draw(
                arenaSurfaceTexture,
                drawX,
                drawY,
                drawWidth,
                drawHeight);
    }

    private void ensureArenaSurfaceTexture() {
        if (currentMap == null || arenaSurfaceTexture != null || Gdx.gl == null) {
            return;
        }

        arenaSurfaceTexture = arenaSurfaceTextureCache.get(buildArenaSurfaceCacheKey(currentMap));
    }

    private void warmArenaSurfaceTextures() {
        if (currentMap == null || Gdx.gl == null) {
            return;
        }

        arenaSurfaceTexture = getOrCreateArenaSurfaceTexture(currentMap, currentTheme());
    }

    private void warmNextArenaSurfaceTexture() {
        if (mapProgression == null || currentMap == null || Gdx.gl == null) {
            return;
        }
        if (mapProgression.getMapCount() <= 1
                || mapProgression.getCurrentMapNumber() >= mapProgression.getMapCount()) {
            return;
        }

        ArenaMap nextMap = mapProgression.getNextMap();
        if (nextMap == null) {
            return;
        }

        String cacheKey = buildArenaSurfaceCacheKey(nextMap);
        if (cacheKey.equals(preloadedNextArenaSurfaceKey)
                || arenaSurfaceTextureCache.containsKey(cacheKey)) {
            preloadedNextArenaSurfaceKey = cacheKey;
            return;
        }

        if (getOrCreateArenaSurfaceTexture(nextMap, currentTheme()) != null) {
            preloadedNextArenaSurfaceKey = cacheKey;
        }
    }

    private Texture getOrCreateArenaSurfaceTexture(ArenaMap map, MapTheme theme) {
        if (map == null || Gdx.gl == null) {
            return null;
        }

        String cacheKey = buildArenaSurfaceCacheKey(map);
        Texture texture = arenaSurfaceTextureCache.get(cacheKey);
        if (texture != null) {
            return texture;
        }

        texture = buildArenaSurfaceTexture(map, theme);
        if (texture != null) {
            arenaSurfaceTextureCache.put(cacheKey, texture);
        }
        return texture;
    }

    private String buildArenaSurfaceCacheKey(ArenaMap map) {
        String mapId = map.getId();
        return configuredThemeName + ":" + (mapId == null ? Integer.toHexString(map.hashCode()) : mapId);
    }

    private Texture buildArenaSurfaceTexture(ArenaMap map, MapTheme theme) {
        if (map.hasSurfaceImage()) {
            Texture texture = loadTexture(map.getSurfaceImagePath());
            if (texture != null) {
                texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            }
            return texture;
        }

        Rectangle arenaBounds = new Rectangle();
        Vector2 arenaFocus = new Vector2();
        map.getBounds(arenaBounds);
        map.getFocusPoint(arenaFocus);

        int textureWidth =
                MathUtils.clamp(
                        MathUtils.ceil(arenaBounds.width * ARENA_PIXEL_ART_PIXELS_PER_WORLD_UNIT),
                        ARENA_PIXEL_ART_MIN_TEXTURE_SIZE,
                        ARENA_PIXEL_ART_MAX_TEXTURE_SIZE);
        int textureHeight =
                MathUtils.clamp(
                        MathUtils.ceil(arenaBounds.height * ARENA_PIXEL_ART_PIXELS_PER_WORLD_UNIT),
                        ARENA_PIXEL_ART_MIN_TEXTURE_SIZE,
                        ARENA_PIXEL_ART_MAX_TEXTURE_SIZE);

        Pixmap pixmap = new Pixmap(textureWidth, textureHeight, Pixmap.Format.RGBA8888);
        pixmap.setColor(0f, 0f, 0f, 0f);
        pixmap.fill();

        for (int pixelY = 0; pixelY < textureHeight; pixelY++) {
            float worldY =
                    arenaBounds.y
                            + (textureHeight - pixelY - 0.5f) * arenaBounds.height / textureHeight;
            for (int pixelX = 0; pixelX < textureWidth; pixelX++) {
                float worldX = arenaBounds.x + (pixelX + 0.5f) * arenaBounds.width / textureWidth;
                if (!map.approximateSupports(worldX, worldY)) {
                    continue;
                }

                float hazardDepth = map.approximateDistanceToHazard(worldX, worldY);
                pixmap.drawPixel(
                        pixelX,
                        pixelY,
                        buildArenaSurfacePixel(
                                theme,
                                arenaBounds,
                                arenaFocus,
                                hazardDepth,
                                worldX,
                                worldY,
                                pixelX,
                                pixelY));
            }
        }

        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pixmap.dispose();
        return texture;
    }

    private int buildArenaSurfacePixel(
            MapTheme theme,
            Rectangle arenaBounds,
            Vector2 arenaFocus,
            float hazardDepth,
            float worldX,
            float worldY,
            int pixelX,
            int pixelY) {
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

        float accentMix =
                computeArenaSurfaceAccentMix(
                        theme.decorStyle,
                        arenaBounds,
                        arenaFocus,
                        worldX,
                        worldY,
                        pixelX,
                        pixelY);
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
            Rectangle arenaBounds,
            Vector2 arenaFocus,
            float worldX,
            float worldY,
            int pixelX,
            int pixelY) {
        float accentMix = 0f;
        switch (decorStyle) {
            case RUNWAY:
                boolean horizontalRunway = arenaBounds.width >= arenaBounds.height;
                float runwayCross = horizontalRunway ? worldY - arenaFocus.y : worldX - arenaFocus.x;
                float runwayAlong = horizontalRunway ? worldX - arenaBounds.x : worldY - arenaBounds.y;
                if (Math.abs(runwayCross) < 0.18f
                        && ((int) Math.floor(runwayAlong * 3.5f) & 3) < 2) {
                    accentMix += 0.26f;
                }
                if (Math.abs(runwayCross - 0.95f) < 0.10f || Math.abs(runwayCross + 0.95f) < 0.10f) {
                    accentMix += 0.12f;
                }
                break;
            case ORBIT:
                float orbitDistance = Vector2.dst(worldX, worldY, arenaFocus.x, arenaFocus.y);
                if (isNearRepeatingLine(orbitDistance, 1.65f, 0.07f)) {
                    accentMix += 0.16f;
                }
                if (isNearRepeatingLine(orbitDistance + 0.82f, 3.25f, 0.09f)) {
                    accentMix += 0.08f;
                }
                break;
            case CROSS:
                if (Math.abs(worldX - arenaFocus.x) < 0.14f || Math.abs(worldY - arenaFocus.y) < 0.14f) {
                    accentMix += 0.20f;
                }
                if (Math.abs(worldX - arenaFocus.x) < 0.52f || Math.abs(worldY - arenaFocus.y) < 0.52f) {
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
                float insetX = Math.min(worldX - arenaBounds.x, arenaBounds.x + arenaBounds.width - worldX);
                float insetY = Math.min(worldY - arenaBounds.y, arenaBounds.y + arenaBounds.height - worldY);
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
                if (isNearRepeatingLine(worldX - arenaBounds.x, 0.82f, 0.05f)
                        || isNearRepeatingLine(worldY - arenaBounds.y, 0.82f, 0.05f)) {
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
        hudFont.draw(spriteBatch, "RATASS GP", 22f, topHudLineY);

        hudFont.setColor(0.82f, 0.88f, 0.93f, 1f);
        hudFont.draw(
                spriteBatch,
                "Circuit " + mapProgression.getCurrentMapNumber() + ": " + currentMap.getName(),
                22f,
                topHudLineY - topHudLineStep);

        drawSidebarSummary(sidebarX, sidebarWidth, hudHeight);
        drawSidebarTables(sidebarX, sidebarWidth, hudHeight);
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
        boolean hasFooterCard = buildObjectiveText().length() > 0;

        shapeRenderer.setColor(0.10f, 0.13f, 0.17f, 0.95f);
        shapeRenderer.rect(
                sidebarX + SIDEBAR_CARD_MARGIN,
                hudHeight - 22f - summaryCardHeight,
                sidebarWidth - SIDEBAR_CARD_MARGIN * 2f,
                summaryCardHeight);
        if (hasFooterCard) {
            shapeRenderer.rect(
                    sidebarX + SIDEBAR_CARD_MARGIN,
                    10f,
                    sidebarWidth - SIDEBAR_CARD_MARGIN * 2f,
                    getSidebarFooterCardHeight());
        }
        if (getSidebarMinimapBounds(sidebarX, sidebarWidth, hudHeight, sidebarMinimapBounds)) {
            shapeRenderer.rect(
                    sidebarMinimapBounds.x,
                    sidebarMinimapBounds.y,
                    sidebarMinimapBounds.width,
                    sidebarMinimapBounds.height);
        }
        drawSidebarTablesScrollbar(sidebarX, sidebarWidth, hudHeight);
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
        hudFont.draw(spriteBatch, "Championship", x, y);

        leaderboardFont.setColor(0.76f, 0.84f, 0.90f, 1f);
        y -= hudLineStep;
        leaderboardFont.draw(
                spriteBatch,
                "Circuit " + mapProgression.getCurrentMapNumber() + "/" + mapProgression.getMapCount(),
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

    private void drawSidebarTables(float sidebarX, float sidebarWidth, float hudHeight) {
        refreshLeaderboardEntries();

        if (!getSidebarTablesViewportBounds(sidebarX, sidebarWidth, hudHeight, sidebarTablesViewportBounds)) {
            return;
        }

        clampSidebarTablesScrollOffset(sidebarX, sidebarWidth, hudHeight);

        float x = sidebarX + SIDEBAR_CONTENT_MARGIN;
        float viewportTop = sidebarTablesViewportBounds.y + sidebarTablesViewportBounds.height - 2f;
        float viewportBottom = sidebarTablesViewportBounds.y + 2f;
        float rowStep = getSidebarCompactLeaderboardRowStep();
        float headerStep = Math.max(16f, rowStep + 1f);
        boolean overflow = getSidebarTablesMaxScroll(sidebarX, sidebarWidth, hudHeight) > 0.5f;
        float contentWidth =
                Math.max(1f, sidebarWidth - SIDEBAR_CONTENT_MARGIN * 2f - (overflow ? 12f : 0f));
        BitmapFont font = labelFont;
        float pointsWidth = 34f;
        float currentLapWidth = 62f;
        float bestLapWidth = 54f;
        float gap = 7f;
        float pointsX = x + contentWidth;
        float currentLapX = pointsX - pointsWidth - gap;
        float bestLapX = currentLapX - currentLapWidth - gap;
        float nameWidth = Math.max(36f, bestLapX - x - bestLapWidth - gap);
        float contentY = 0f;

        float y = viewportTop - contentY - sidebarTablesScrollOffset;
        font.setColor(0.98f, 0.95f, 0.84f, 1f);
        drawSidebarTableText(font, "Player", x, y, viewportBottom, viewportTop);
        drawSidebarTableRightAlignedText(font, "Best", bestLapX, y, viewportBottom, viewportTop);
        drawSidebarTableRightAlignedText(font, "Current", currentLapX, y, viewportBottom, viewportTop);
        drawSidebarTableRightAlignedText(font, "Pts", pointsX, y, viewportBottom, viewportTop);
        contentY += headerStep;

        for (int i = 0; i < leaderboardEntries.size; i++) {
            CarTemplate template = leaderboardEntries.get(i);
            boolean active = template.currentCar != null && template.currentCar.active;
            y = viewportTop - contentY - sidebarTablesScrollOffset;

            if (template.playerControlled) {
                font.setColor(1f, 0.94f, 0.54f, 1f);
            } else if (!active && !roundOver) {
                font.setColor(0.66f, 0.72f, 0.78f, 1f);
            } else {
                font.setColor(0.84f, 0.90f, 0.94f, 1f);
            }

            String player = truncateTextToWidth(font, buildLeaderboardPlayerLabel(template), nameWidth);
            drawSidebarTableText(font, player, x, y, viewportBottom, viewportTop);
            drawSidebarTableRightAlignedText(font, buildBestLapText(template), bestLapX, y, viewportBottom, viewportTop);
            drawSidebarTableRightAlignedText(
                    font,
                    buildCurrentLapText(template, active),
                    currentLapX,
                    y,
                    viewportBottom,
                    viewportTop);
            drawSidebarTableRightAlignedText(
                    font,
                    String.valueOf(template.totalPoints),
                    pointsX,
                    y,
                    viewportBottom,
                    viewportTop);
            contentY += rowStep;
        }

        contentY += SIDEBAR_MINIMAP_GAP;
        CarTemplate player = roster.size == 0 ? null : roster.first();
        float rightX = x + contentWidth;

        y = viewportTop - contentY - sidebarTablesScrollOffset;
        font.setColor(0.98f, 0.95f, 0.84f, 1f);
        drawSidebarTableText(font, "Player Laps", x, y, viewportBottom, viewportTop);
        contentY += headerStep;

        y = viewportTop - contentY - sidebarTablesScrollOffset;
        font.setColor(0.76f, 0.84f, 0.90f, 1f);
        drawSidebarTableText(font, "Lap", x, y, viewportBottom, viewportTop);
        drawSidebarTableRightAlignedText(font, "Time", rightX, y, viewportBottom, viewportTop);
        contentY += rowStep;

        int raceLaps = getRaceLapsToWin();
        for (int lapIndex = 0; lapIndex < raceLaps; lapIndex++) {
            boolean currentLap =
                    player != null
                            && !player.roundRaceFinished
                            && lapIndex == MathUtils.clamp(player.roundRaceLap, 0, raceLaps - 1);
            if (currentLap) {
                font.setColor(1f, 0.94f, 0.54f, 1f);
            } else {
                font.setColor(0.84f, 0.90f, 0.94f, 1f);
            }

            y = viewportTop - contentY - sidebarTablesScrollOffset;
            drawSidebarTableText(font, String.valueOf(lapIndex + 1), x, y, viewportBottom, viewportTop);
            drawSidebarTableRightAlignedText(
                    font,
                    buildPlayerLapTimeText(player, lapIndex),
                    rightX,
                    y,
                    viewportBottom,
                    viewportTop);
            contentY += rowStep;
        }
    }

    private void drawSidebarTableText(
            BitmapFont font,
            String text,
            float x,
            float y,
            float viewportBottom,
            float viewportTop) {
        if (y < viewportBottom || y > viewportTop) {
            return;
        }
        font.draw(spriteBatch, text, x, y);
    }

    private void drawSidebarTableRightAlignedText(
            BitmapFont font,
            String text,
            float rightX,
            float y,
            float viewportBottom,
            float viewportTop) {
        if (y < viewportBottom || y > viewportTop) {
            return;
        }
        drawRightAlignedText(font, text, rightX, y);
    }

    private void drawRightAlignedText(BitmapFont font, String text, float rightX, float y) {
        glyphLayout.setText(font, text);
        font.draw(spriteBatch, text, rightX - glyphLayout.width, y);
    }

    private void refreshLeaderboardEntries() {
        if (!leaderboardDirty) {
            return;
        }

        leaderboardEntries.clear();
        leaderboardEntries.addAll(roster);
        leaderboardEntries.sort(leaderboardComparator);
        leaderboardDirty = false;
    }

    private void invalidateLeaderboard() {
        leaderboardDirty = true;
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

        if (GROWTH_PICKUP_ENABLED && growthPickupActive) {
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
                "Circuit Map",
                sidebarMinimapBounds.x + SIDEBAR_CONTENT_MARGIN - SIDEBAR_CARD_MARGIN,
                sidebarMinimapBounds.y + sidebarMinimapBounds.height - SIDEBAR_MINIMAP_PADDING);
    }

    private void drawSidebarFooter(float sidebarX, float sidebarWidth) {
        if (sidebarWidth <= 0f) {
            return;
        }

        String objectiveText = buildObjectiveText();
        if (objectiveText.length() == 0) {
            return;
        }

        leaderboardFont.setColor(0.93f, 0.84f, 0.49f, 1f);
        leaderboardFont.draw(
                spriteBatch,
                objectiveText,
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

    private float getSidebarSummaryCardHeight() {
        return Math.max(86f, hudFont.getLineHeight() + leaderboardFont.getLineHeight() * 5f);
    }

    private float getSidebarFooterCardHeight() {
        return Math.max(82f, leaderboardFont.getLineHeight() * 4.3f);
    }

    private float getSidebarLeaderboardStartY(float hudHeight) {
        return hudHeight - 22f - getSidebarSummaryCardHeight() - 16f;
    }

    private float getSidebarCompactLeaderboardRowStep() {
        return Math.max(SIDEBAR_LEADERBOARD_COMPACT_ROW_STEP, labelFont.getLineHeight() + 1f);
    }

    private boolean getSidebarTablesViewportBounds(
            float sidebarX,
            float sidebarWidth,
            float hudHeight,
            Rectangle out) {
        if (sidebarWidth <= 0f || sidebarWidth <= SIDEBAR_CONTENT_MARGIN * 2f) {
            out.set(0f, 0f, 0f, 0f);
            return false;
        }

        if (!getSidebarMinimapBounds(sidebarX, sidebarWidth, hudHeight, sidebarMinimapBounds)) {
            out.set(0f, 0f, 0f, 0f);
            return false;
        }

        float top = getSidebarLeaderboardStartY(hudHeight) + 2f;
        float bottom = sidebarMinimapBounds.y + sidebarMinimapBounds.height + SIDEBAR_MINIMAP_GAP;
        float height = top - bottom;
        if (height < Math.max(24f, getSidebarCompactLeaderboardRowStep() * 2f)) {
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

    private float getSidebarTablesContentHeight(float sidebarWidth, float hudHeight) {
        refreshLeaderboardEntries();
        float rowStep = getSidebarCompactLeaderboardRowStep();
        float headerStep = Math.max(16f, rowStep + 1f);
        return headerStep
                + leaderboardEntries.size * rowStep
                + SIDEBAR_MINIMAP_GAP
                + headerStep
                + rowStep
                + getRaceLapsToWin() * rowStep
                + 4f;
    }

    private float getSidebarTablesMaxScroll(float sidebarX, float sidebarWidth, float hudHeight) {
        if (!getSidebarTablesViewportBounds(sidebarX, sidebarWidth, hudHeight, sidebarTablesViewportBounds)) {
            return 0f;
        }
        return Math.max(0f, getSidebarTablesContentHeight(sidebarWidth, hudHeight)
                - sidebarTablesViewportBounds.height);
    }

    private void clampSidebarTablesScrollOffset(float sidebarX, float sidebarWidth, float hudHeight) {
        float maxScroll = getSidebarTablesMaxScroll(sidebarX, sidebarWidth, hudHeight);
        sidebarTablesScrollOffset = MathUtils.clamp(sidebarTablesScrollOffset, 0f, maxScroll);
        if (maxScroll <= 0.0001f) {
            sidebarTablesScrollOffset = 0f;
        }
    }

    private void scrollSidebarTables(float amount, float sidebarX, float sidebarWidth, float hudHeight) {
        sidebarTablesScrollOffset += amount;
        clampSidebarTablesScrollOffset(sidebarX, sidebarWidth, hudHeight);
        updateSidebarTablesScrollbarBounds(sidebarX, sidebarWidth, hudHeight);
    }

    private void updateSidebarTablesScrollbarBounds(float sidebarX, float sidebarWidth, float hudHeight) {
        if (!getSidebarTablesViewportBounds(sidebarX, sidebarWidth, hudHeight, sidebarTablesViewportBounds)) {
            sidebarTablesScrollbarBounds.set(0f, 0f, 0f, 0f);
            return;
        }

        float contentHeight = getSidebarTablesContentHeight(sidebarWidth, hudHeight);
        float viewportHeight = sidebarTablesViewportBounds.height;
        if (contentHeight <= viewportHeight + 0.5f) {
            sidebarTablesScrollbarBounds.set(0f, 0f, 0f, 0f);
            sidebarTablesScrollOffset = 0f;
            return;
        }

        float scrollbarWidth = 5f;
        float thumbHeight = Math.max(24f, viewportHeight * viewportHeight / contentHeight);
        float maxThumbTravel = Math.max(1f, viewportHeight - thumbHeight);
        float maxScroll = Math.max(1f, contentHeight - viewportHeight);
        float scrollRatio = MathUtils.clamp(sidebarTablesScrollOffset / maxScroll, 0f, 1f);
        float thumbY = sidebarTablesViewportBounds.y + viewportHeight - thumbHeight - scrollRatio * maxThumbTravel;
        sidebarTablesScrollbarBounds.set(
                sidebarTablesViewportBounds.x + sidebarTablesViewportBounds.width - scrollbarWidth - 3f,
                thumbY,
                scrollbarWidth,
                thumbHeight);
    }

    private void setSidebarTablesScrollFromThumb(
            float thumbY,
            float sidebarX,
            float sidebarWidth,
            float hudHeight) {
        if (!getSidebarTablesViewportBounds(sidebarX, sidebarWidth, hudHeight, sidebarTablesViewportBounds)) {
            return;
        }

        float contentHeight = getSidebarTablesContentHeight(sidebarWidth, hudHeight);
        float viewportHeight = sidebarTablesViewportBounds.height;
        float maxScroll = Math.max(0f, contentHeight - viewportHeight);
        if (maxScroll <= 0.0001f) {
            sidebarTablesScrollOffset = 0f;
            updateSidebarTablesScrollbarBounds(sidebarX, sidebarWidth, hudHeight);
            return;
        }

        float thumbHeight = Math.max(24f, viewportHeight * viewportHeight / contentHeight);
        float maxThumbTravel = Math.max(1f, viewportHeight - thumbHeight);
        float clampedThumbY = MathUtils.clamp(
                thumbY,
                sidebarTablesViewportBounds.y,
                sidebarTablesViewportBounds.y + viewportHeight - thumbHeight);
        float scrollRatio =
                (sidebarTablesViewportBounds.y + viewportHeight - thumbHeight - clampedThumbY)
                        / maxThumbTravel;
        sidebarTablesScrollOffset = MathUtils.clamp(scrollRatio * maxScroll, 0f, maxScroll);
        updateSidebarTablesScrollbarBounds(sidebarX, sidebarWidth, hudHeight);
    }

    private void drawSidebarTablesScrollbar(float sidebarX, float sidebarWidth, float hudHeight) {
        updateSidebarTablesScrollbarBounds(sidebarX, sidebarWidth, hudHeight);
        if (sidebarTablesScrollbarBounds.width <= 0f) {
            return;
        }

        shapeRenderer.setColor(1f, 1f, 1f, 0.08f);
        shapeRenderer.rect(
                sidebarTablesScrollbarBounds.x,
                sidebarTablesViewportBounds.y,
                sidebarTablesScrollbarBounds.width,
                sidebarTablesViewportBounds.height);

        shapeRenderer.setColor(0.98f, 0.84f, 0.28f, 0.46f);
        shapeRenderer.rect(
                sidebarTablesScrollbarBounds.x,
                sidebarTablesScrollbarBounds.y,
                sidebarTablesScrollbarBounds.width,
                sidebarTablesScrollbarBounds.height);
    }

    private String buildLeaderboardPlayerLabel(CarTemplate template) {
        return template.playerControlled ? "YOU" : template.name;
    }

    private String buildBestLapText(CarTemplate template) {
        if (template == null || template.roundRaceBestLapTime <= 0f) {
            return "--";
        }
        return formatRaceTime(template.roundRaceBestLapTime);
    }

    private String buildCurrentLapText(CarTemplate template, boolean active) {
        if (template == null) {
            return "--";
        }
        if (template.roundRaceFinished) {
            return "FIN";
        }
        if (!active && !roundOver) {
            return "OUT";
        }
        if (roundOver) {
            return template.roundFinishPosition > 0 ? "P" + template.roundFinishPosition : "--";
        }
        return formatRaceTime(Math.max(0f, roundTimer - template.roundRaceCurrentLapStartTime));
    }

    private String buildPlayerLapTimeText(CarTemplate template, int lapIndex) {
        if (template == null || lapIndex < 0) {
            return "--";
        }
        if (lapIndex < template.roundRaceLapTimes.size) {
            return formatRaceTime(template.roundRaceLapTimes.get(lapIndex));
        }
        if (!template.roundRaceFinished
                && lapIndex == MathUtils.clamp(template.roundRaceLap, 0, getRaceLapsToWin() - 1)) {
            return formatRaceTime(Math.max(0f, roundTimer - template.roundRaceCurrentLapStartTime));
        }
        return "--";
    }

    private String formatRaceTime(float seconds) {
        float clampedSeconds = Math.max(0f, seconds);
        int minutes = (int) (clampedSeconds / 60f);
        float secondPart = clampedSeconds - minutes * 60f;
        if (minutes > 0) {
            return String.format(Locale.ROOT, "%d:%04.1f", minutes, secondPart);
        }
        return String.format(Locale.ROOT, "%.1f", secondPart);
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
        float footerReserve = buildObjectiveText().length() > 0
                ? getSidebarFooterCardHeight() + SIDEBAR_MINIMAP_GAP
                : 0f;
        float bottom = 10f + footerReserve;
        float summaryBottom = hudHeight - 22f - getSidebarSummaryCardHeight();
        float availableHeight = summaryBottom - SIDEBAR_MINIMAP_GAP - bottom;
        if (sidebarWidth <= 0f
                || sidebarWidth <= SIDEBAR_CARD_MARGIN * 2f
                || availableHeight < SIDEBAR_MINIMAP_MIN_HEIGHT) {
            out.set(0f, 0f, 0f, 0f);
            return false;
        }

        float preferredHeight = Math.min(220f, Math.max(SIDEBAR_MINIMAP_MIN_HEIGHT, sidebarWidth * 0.66f));
        float tableReserve = Math.max(28f, getSidebarCompactLeaderboardRowStep() * 4f);
        float height = Math.min(preferredHeight, availableHeight);
        if (availableHeight - height < tableReserve && height > SIDEBAR_MINIMAP_MIN_HEIGHT) {
            height = Math.max(SIDEBAR_MINIMAP_MIN_HEIGHT, availableHeight - tableReserve);
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

    private Car findLastAliveCar() {
        for (int i = 0; i < cars.size; i++) {
            Car car = cars.get(i);
            if (car.active) {
                return car;
            }
        }
        return null;
    }

    private String buildStatusText() {
        StringBuilder builder = new StringBuilder();
        builder.append("You ").append(roster.first().totalPoints).append(" pts")
                .append("  |  Cars left: ").append(getAliveCarCount()).append("/").append(cars.size)
                .append("  |  Circuits ").append(mapProgression.getCurrentMapNumber()).append("/")
                .append(mapProgression.getMapCount());

        if (preRoundCountdownTimer > 0f) {
            builder.append("  |  Starts in ").append(MathUtils.ceil(preRoundCountdownTimer)).append("s");
        } else if (roundOver) {
            builder.append("  |  RACE OVER");
        } else if (isLiveLapRaceMode()) {
            builder.append("  |  ").append(buildRaceProgressText(roster.first()));
            if (raceFinishTimer >= 0f) {
                builder.append("  |  Finish closes in ").append(getRaceFinishSecondsLeft()).append("s");
            }
        } else {
            builder.append("  |  Race checkpoints unavailable");
        }

        return builder.toString();
    }

    private String buildSidebarStateText() {
        if (preRoundCountdownTimer > 0f) {
            return "Starts in " + MathUtils.ceil(preRoundCountdownTimer) + "s";
        }

        if (roundOver) {
            return "Race complete";
        }

        if (isLiveLapRaceMode()) {
            if (raceFinishTimer >= 0f) {
                return "Finish closes in " + getRaceFinishSecondsLeft() + "s";
            }
            return buildRaceProgressText(roster.first());
        }

        return "No checkpoint route";
    }

    private String buildObjectiveText() {
        if (preRoundCountdownTimer > 0f) {
            return isLiveLapRaceMode()
                    ? "Prepare for the horn. Follow the checkpoints and complete "
                            + getRaceLapsToWin()
                            + " laps."
                    : "This map needs ordered checkpoints before it can be raced.";
        }

        if (roundOver) {
            return "Standings updated. Next circuit in a moment.";
        }

        if (isLiveLapRaceMode()) {
            if (raceFinishTimer >= 0f) {
                return (winner != null && winner.playerControlled ? "You finished first. " : "Leader finished. ")
                        + getRaceFinishSecondsLeft()
                        + "s left for the rest to finish.";
            }
            return "";
        }

        return "Checkpoint route missing.";
    }

    private int getRaceFinishSecondsLeft() {
        return MathUtils.ceil(Math.max(0f, raceFinishTimer));
    }

    private String buildRaceProgressText(CarTemplate template) {
        if (template == null) {
            return "Lap 1/" + getRaceLapsToWin();
        }
        if (template.roundRaceFinished) {
            return "Finished #" + template.roundFinishPosition;
        }
        int raceLaps = getRaceLapsToWin();
        int lap = MathUtils.clamp(template.roundRaceLap + 1, 1, raceLaps);
        int checkpointCount = currentMap == null ? 0 : currentMap.getCheckpointCount();
        int checkpoint = checkpointCount <= 0 ? 0 : MathUtils.clamp(
                template.roundRaceNextCheckpointIndex,
                0,
                checkpointCount - 1);
        return "Lap " + lap + "/" + raceLaps
                + (checkpointCount > 0 ? " CP " + (checkpoint + 1) + "/" + checkpointCount : "");
    }

    private String buildHeadline() {
        if (!roundOver) {
            return "OUT";
        }

        if (winner == null) {
            return "WIPEOUT";
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

        if (winner == null) {
            return "No cars finished the race. Standings updated.";
        }

        if (winner.playerControlled) {
            return "You win " + currentMap.getName() + ". Next circuit in a moment.";
        }

        return winner.name + " wins " + currentMap.getName() + ". Next circuit in a moment.";
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
        return themeForMap(currentMap);
    }

    private MapTheme themeForMap(ArenaMap map) {
        if (map == null) {
            return DEFAULT_MAP_THEME;
        }

        String mapId = map.getId();
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

    private int findThemeIndex(String themeName) {
        int index = findThemeChoiceIndex(themeName);
        return index >= 0 ? index : 0;
    }

    private int findThemeChoiceIndex(String themeName) {
        String lookup = toThemeLookupKey(themeName);
        if (lookup.length() == 0) {
            return -1;
        }
        for (int i = 0; i < themeChoices.size; i++) {
            ThemeChoice choice = themeChoices.get(i);
            if (toThemeLookupKey(choice.name).equals(lookup)
                    || toThemeLookupKey(choice.displayName).equals(lookup)) {
                return i;
            }
        }
        return -1;
    }

    private String normalizeThemeName(String themeName) {
        int index = findThemeChoiceIndex(themeName);
        if (index >= 0) {
            return themeChoices.get(index).name;
        }
        int defaultIndex = findThemeChoiceIndex(DEFAULT_THEME_NAME);
        if (defaultIndex >= 0) {
            return themeChoices.get(defaultIndex).name;
        }
        return themeChoices.size > 0 ? themeChoices.get(0).name : DEFAULT_THEME_NAME;
    }

    private String buildThemeDisplayName(String themeName) {
        ThemeChoice fallback = findFallbackThemeChoice(themeName);
        if (fallback != null) {
            return fallback.displayName;
        }
        String normalized = themeName == null ? "" : themeName.trim().replace('-', ' ').replace('_', ' ');
        StringBuilder displayName = new StringBuilder();
        boolean capitalizeNext = true;
        for (int i = 0; i < normalized.length(); i++) {
            char character = normalized.charAt(i);
            if (Character.isWhitespace(character)) {
                if (displayName.length() > 0
                        && displayName.charAt(displayName.length() - 1) != ' ') {
                    displayName.append(' ');
                }
                capitalizeNext = true;
            } else if (capitalizeNext) {
                displayName.append(Character.toUpperCase(character));
                capitalizeNext = false;
            } else {
                displayName.append(Character.toLowerCase(character));
            }
        }
        return displayName.length() == 0 ? DEFAULT_THEME_NAME : displayName.toString();
    }

    private static ThemeChoice findFallbackThemeChoice(String themeName) {
        String lookup = toThemeLookupKey(themeName);
        for (int i = 0; i < FALLBACK_THEME_CHOICES.length; i++) {
            ThemeChoice choice = FALLBACK_THEME_CHOICES[i];
            if (toThemeLookupKey(choice.name).equals(lookup)
                    || toThemeLookupKey(choice.displayName).equals(lookup)) {
                return choice;
            }
        }
        return null;
    }

    private static String toThemeLookupKey(String value) {
        return value == null ? "" : value.trim().toLowerCase();
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

    private static int loadConfiguredIntProperty(String propertyName, int defaultValue) {
        String value = loadConfiguredProperty(propertyName);
        if (value == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private static float loadConfiguredFloatProperty(String propertyName, float defaultValue) {
        String value = loadConfiguredProperty(propertyName);
        if (value == null) {
            return defaultValue;
        }

        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private static int clampCarCount(int carCount) {
        return MathUtils.clamp(carCount, MIN_CAR_COUNT, MAX_CAR_COUNT);
    }

    private static int clampRaceLapCount(int lapCount) {
        return MathUtils.clamp(lapCount, MIN_RACE_LAPS, MAX_RACE_LAPS);
    }

    private static float clampCameraZoom(float zoom) {
        return MathUtils.clamp(zoom, MIN_CAMERA_ZOOM, MAX_CAMERA_ZOOM);
    }

    private static int clampPlayerCarIndex(int playerCarIndex, int carCount) {
        if (carCount <= 0) {
            return Math.max(0, playerCarIndex);
        }
        return MathUtils.clamp(playerCarIndex, 0, carCount - 1);
    }

    private static String loadConfiguredProperty(String propertyName) {
        Map<String, String> properties = loadConfiguredProperties();
        return properties.get(propertyName);
    }

    private static Map<String, String> loadConfiguredProperties() {
        Map<String, String> properties = new LinkedHashMap<String, String>();

        loadPropertiesFromInternalFile(properties, GAME_PROPERTIES_RESOURCE);
        loadPropertiesFromLocalFile(properties, GAME_PROPERTIES_FILE);
        return properties;
    }

    private static void loadPropertiesFromInternalFile(Map<String, String> properties, String path) {
        if (Gdx.files == null) {
            return;
        }

        try {
            FileHandle handle = Gdx.files.internal(path);
            if (handle == null || !handle.exists()) {
                return;
            }
            parseProperties(properties, handle.readString("UTF-8"));
        } catch (RuntimeException exception) {
            // Internal files can be unavailable in headless tooling.
        }
    }

    private static void loadPropertiesFromLocalFile(Map<String, String> properties, String path) {
        if (Gdx.files == null) {
            return;
        }

        try {
            FileHandle handle = Gdx.files.local(path);
            if (handle == null || !handle.exists()) {
                return;
            }
            parseProperties(properties, handle.readString("UTF-8"));
        } catch (RuntimeException exception) {
            // Local overrides are optional.
        }
    }

    private static void parseProperties(Map<String, String> properties, String content) {
        if (content == null) {
            return;
        }

        String[] lines = content.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.length() == 0 || line.startsWith("#") || line.startsWith("!")) {
                continue;
            }

            int separator = findPropertySeparator(line);
            if (separator <= 0) {
                continue;
            }

            String key = line.substring(0, separator).trim();
            String value = line.substring(separator + 1).trim();
            if (key.length() > 0) {
                properties.put(key, value);
            }
        }
    }

    private static int findPropertySeparator(String line) {
        int equalsIndex = line.indexOf('=');
        int colonIndex = line.indexOf(':');
        if (equalsIndex < 0) {
            return colonIndex;
        }
        if (colonIndex < 0) {
            return equalsIndex;
        }
        return Math.min(equalsIndex, colonIndex);
    }

    private void disposeSound(Sound sound) {
        if (sound != null) {
            sound.dispose();
        }
    }

    private void disposeMusic(Music music) {
        if (music != null) {
            music.stop();
            music.dispose();
        }
    }

    private void disposeTexture(Texture texture) {
        if (texture != null) {
            texture.dispose();
        }
    }

    private void disposeArenaSurfaceTextures() {
        for (Texture texture : arenaSurfaceTextureCache.values()) {
            disposeTexture(texture);
        }
        arenaSurfaceTextureCache.clear();
        arenaSurfaceTexture = null;
    }

    @Override
    public void dispose() {
        if (world != null) {
            world.dispose();
        }
        disposeRosterSpriteTextures();
        disposeArenaSurfaceTextures();
        disposeTexture(themeCarsTexture);
        disposeMenuCarPreview();
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
        disposeGameSounds();
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
        if (eliminated.eliminatedByCheckpointTimeout) {
            if (eliminated.playerControlled) {
                announceEvent(
                        "MISSED CHECKPOINT",
                        "You ran out of checkpoint time.",
                        new Color(0.30f, 0.82f, 1f, 1f));
                return;
            }
            if (getAliveCarCount() <= 6) {
                announceEvent(
                        "CHECKPOINT TIMEOUT",
                        eliminated.name + " missed the next gate.",
                        new Color(0.42f, 0.90f, 1f, 1f));
            }
            return;
        }
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
                    eliminated.name + " left the circuit.",
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
            carA.recordCarHit();
            carB.recordCarHit();

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

    private static final class CarPhysics {
        private static final CarPhysics DEFAULT =
                new CarPhysics(
                        1.00f,
                        220f,
                        0.56f,
                        345f,
                        0.50f,
                        38.5f,
                        1.08f,
                        0.36f,
                        0.84f,
                        5.2f,
                        2.7f,
                        0.115f,
                        0.055f,
                        6.4f,
                        52.0f,
                        38.0f,
                        0.22f,
                        0.70f,
                        0.42f,
                        0.18f,
                        1.45f,
                        1.30f,
                        0.42f,
                        0.025f,
                        0.12f,
                        4.8f);

        private static final float HORSEPOWER_TO_FORCE = 0.70f;

        private final float massMultiplier;
        private final float horsePower;
        private final float reversePowerMultiplier;
        private final float brakeForce;
        private final float reverseSpeedMultiplier;
        private final float steeringTorque;
        private final float lowSpeedSteeringAuthority;
        private final float highSpeedSteeringAuthority;
        private final float wheelGrip;
        private final float lateralGripPerSecond;
        private final float yawGripPerSecond;
        private final float rollingResistance;
        private final float aeroDrag;
        private final float driftSpeedScrubPerSecond;
        private final float maxForwardSpeed;
        private final float steeringReferenceSpeed;
        private final float trackLimitMaxSpeedMultiplier;
        private final float trackLimitVelocityDamping;
        private final float trackLimitAngularDamping;
        private final float linearDamping;
        private final float angularDamping;
        private final float fixtureDensity;
        private final float fixtureFriction;
        private final float fixtureRestitution;
        private final float collisionImpulseFactor;
        private final float maxCollisionImpulse;

        private CarPhysics(
                float massMultiplier,
                float horsePower,
                float reversePowerMultiplier,
                float brakeForce,
                float reverseSpeedMultiplier,
                float steeringTorque,
                float lowSpeedSteeringAuthority,
                float highSpeedSteeringAuthority,
                float wheelGrip,
                float lateralGripPerSecond,
                float yawGripPerSecond,
                float rollingResistance,
                float aeroDrag,
                float driftSpeedScrubPerSecond,
                float maxForwardSpeed,
                float steeringReferenceSpeed,
                float trackLimitMaxSpeedMultiplier,
                float trackLimitVelocityDamping,
                float trackLimitAngularDamping,
                float linearDamping,
                float angularDamping,
                float fixtureDensity,
                float fixtureFriction,
                float fixtureRestitution,
                float collisionImpulseFactor,
                float maxCollisionImpulse) {
            this.massMultiplier = massMultiplier;
            this.horsePower = horsePower;
            this.reversePowerMultiplier = reversePowerMultiplier;
            this.brakeForce = brakeForce;
            this.reverseSpeedMultiplier = reverseSpeedMultiplier;
            this.steeringTorque = steeringTorque;
            this.lowSpeedSteeringAuthority = lowSpeedSteeringAuthority;
            this.highSpeedSteeringAuthority = highSpeedSteeringAuthority;
            this.wheelGrip = wheelGrip;
            this.lateralGripPerSecond = lateralGripPerSecond;
            this.yawGripPerSecond = yawGripPerSecond;
            this.rollingResistance = rollingResistance;
            this.aeroDrag = aeroDrag;
            this.driftSpeedScrubPerSecond = driftSpeedScrubPerSecond;
            this.maxForwardSpeed = maxForwardSpeed;
            this.steeringReferenceSpeed = steeringReferenceSpeed;
            this.trackLimitMaxSpeedMultiplier = trackLimitMaxSpeedMultiplier;
            this.trackLimitVelocityDamping = trackLimitVelocityDamping;
            this.trackLimitAngularDamping = trackLimitAngularDamping;
            this.linearDamping = linearDamping;
            this.angularDamping = angularDamping;
            this.fixtureDensity = fixtureDensity;
            this.fixtureFriction = fixtureFriction;
            this.fixtureRestitution = fixtureRestitution;
            this.collisionImpulseFactor = collisionImpulseFactor;
            this.maxCollisionImpulse = maxCollisionImpulse;
        }

        private float engineForce() {
            return horsePower * HORSEPOWER_TO_FORCE;
        }
    }

    private static final class Car {
        private static final float WIDTH = 1.36f;
        private static final float HEIGHT = 1.58f;
        private static final float HALF_WIDTH = WIDTH * 0.5f;
        private static final float HALF_HEIGHT = HEIGHT * 0.5f;
        private static final float GROWTH_SCALE = 1.40f;
        private static final float GROWTH_MASS_MULTIPLIER = 10f;
        private static final float REVERSE_ENGAGE_SPEED = 0.08f;
        private static final float BRAKE_STOP_EPSILON = 0.015f;
        private static final float GROWTH_TURN_MULTIPLIER = 0.90f;
        private static final float MAX_GROWTH_SPEED_MULTIPLIER = 1.06f;
        private static final float MIN_COLLISION_RESPONSE_IMPULSE = 4.4f;
        private static final float MIN_COLLISION_RESPONSE_SPEED = 1.0f;
        private static final float IMPACT_STRENGTH_SPEED_FACTOR = 3.9f;
        private static final float INCOMING_SPEED_REBOUND_BOOST_FACTOR = 0.025f;
        private static final float MAX_INCOMING_SPEED_REBOUND_BOOST = 0.20f;
        private static final float IMPACT_SLIDE_DURATION = 0.42f;
        private static final float IMPACT_SLIDE_REFERENCE = 18f;
        private static final float BOOSTED_VS_NORMAL_REBOUND_MULTIPLIER = 0.72f;
        private static final float NORMAL_VS_BOOSTED_REBOUND_MULTIPLIER = 1.65f;
        private static final float BOOSTED_VS_BOOSTED_REBOUND_MULTIPLIER = 1.12f;
        private static final float RAM_CHARGE_ATTACKER_REBOUND_MULTIPLIER = 0.82f;
        private static final float RAM_CHARGE_VICTIM_REBOUND_MULTIPLIER = 1.45f;
        private static final float RECENT_IMPACT_DURATION = 1.8f;
        private static final float CONTROL_LOCK_REFERENCE = 20f;
        private static final float MAX_CONTROL_LOCK_DURATION = 0.58f;
        private static final float EDGE_FINISH_DISTANCE = 2.5f;
        private static final float BASE_EDGE_FINISH_IMPULSE = 1.1f;
        private static final float EMPOWERED_EDGE_FINISH_MULTIPLIER = 1.35f;
        private static final float TRACK_LIMIT_SLOW_MARGIN = 0.82f;
        private static final float ARENA_WALL_CONTACT_DURATION = 0.28f;

        private final CarTemplate template;
        private final String name;
        private final boolean playerControlled;
        private final boolean externallyControlled;
        private final boolean modelControlled;
        private final Color color;
        private final AiControlDecision externalControlDecision = new AiControlDecision();
        private final AiControlDecision rawExternalControlDecision = new AiControlDecision();
        private final float[] rlObservation = new float[RL_OBSERVATION_SIZE];
        private final Vector2 forwardAxis = new Vector2();
        private final Vector2 sidewaysAxis = new Vector2();
        private final Vector2 working = new Vector2();
        private final Vector2 pendingImpactImpulse = new Vector2();
        private final Vector2 impactRecoveryPoint = new Vector2();
        private final Vector2 impactOutward = new Vector2();
        private final Vector2 arenaWallPosition = new Vector2();
        private final Vector2 arenaWallCorrection = new Vector2();
        private final Vector2 arenaWallVelocity = new Vector2();
        private final Vector2 previousRenderPosition = new Vector2();
        private final Vector2 renderPosition = new Vector2();
        private final Rectangle rlObservationBounds = new Rectangle();
        private final Vector2 rlObservationFocus = new Vector2();
        private final Vector2 rlObservationForward = new Vector2();
        private final Vector2 rlObservationRecovery = new Vector2();
        private final Vector2 rlObservationRouteTarget = new Vector2();
        private final Vector2 rlObservationRouteLookahead = new Vector2();
        private final Vector2 rlObservationSide = new Vector2();
        private final float[] rlObservationRays = new float[6];
        private final float[] rlObservationCarRays = new float[6];

        private Body body;
        private boolean active = true;
        private boolean growthBoosted;
        private boolean eliminatedByCheckpointTimeout;
        private int lastAttackerId = -1;
        private int eliminatedByAttackerId = -1;
        private int carHitCount;
        private float sizeScale = 1f;
        private float impactSlideTimer;
        private float impactSlideStrength;
        private float controlLockTimer;
        private float recentImpactTimer;
        private float arenaWallContactTimer;
        private float ramChargeTimer;
        private float rlDecisionTimer;
        private float lastThrottleCommand;
        private float previousRenderAngleRad;
        private float renderAngleRad;
        private float[] rlScratchA;
        private float[] rlScratchB;

        private Car(Body body, CarTemplate template) {
            this.body = body;
            this.template = template;
            name = template.name;
            playerControlled = template.playerControlled;
            externallyControlled = template.externallyControlled;
            modelControlled = template.modelControlled;
            color = template.color;
            if (modelControlled) {
                rlDecisionTimer =
                        ((template.vehicleId % MAX_CAR_COUNT) + 1)
                                * (RL_INITIAL_DECISION_STAGGER / MAX_CAR_COUNT);
            }
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
                boolean allowControl,
                float playerThrottle,
                float playerTurn,
                boolean checkpointTargetActive,
                Vector2 checkpointTargetPosition,
                Vector2 secondCheckpointPosition,
                float checkpointTargetRadius,
                float targetTimeRemaining,
                Array<Car> cars,
                RlPolicy rlPolicy) {
            if (!active || body == null) {
                return;
            }

            advanceCombatTimers(delta);
            float impactSlideFactor = advanceImpactSlide(delta);
            applyPendingImpactImpulse();

            float throttle = 0f;
            float turn = 0f;

            if (allowControl && controlLockTimer <= 0f) {
                if (modelControlled && rlPolicy != null) {
                    AiControlDecision decision =
                            planWithRlPolicy(
                                    delta,
                                    rlPolicy,
                                    arenaMap,
                                    checkpointTargetActive,
                                    checkpointTargetPosition,
                                    secondCheckpointPosition,
                                    checkpointTargetRadius,
                                    targetTimeRemaining,
                                    cars);
                    throttle = decision.throttle;
                    turn = decision.turn;
                } else if (playerControlled) {
                    throttle = playerThrottle;
                    turn = playerTurn;
                } else if (externallyControlled) {
                    throttle = externalControlDecision.throttle;
                    turn = externalControlDecision.turn;
                }
            }
            lastThrottleCommand = allowControl && controlLockTimer <= 0f ? throttle : 0f;

            applyGrip(delta, impactSlideFactor);

            if (!allowControl || controlLockTimer > 0f) {
                return;
            }

            drive(throttle, turn);
        }

        private AiControlDecision planWithRlPolicy(
                float delta,
                RlPolicy policy,
                ArenaMap arenaMap,
                boolean checkpointTargetActive,
                Vector2 checkpointTargetPosition,
                Vector2 secondCheckpointPosition,
                float checkpointTargetRadius,
                float targetTimeRemaining,
                Array<Car> cars) {
            rlDecisionTimer -= delta;
            if (rlDecisionTimer <= 0f) {
                ensureRlScratch(policy);
                float positionNormalizer =
                        getRlPositionNormalizer(
                                arenaMap,
                                rlObservationBounds,
                                rlObservationFocus);
                fillRlObservation(
                        rlObservation,
                        0,
                        this,
                        arenaMap,
                        checkpointTargetActive,
                        checkpointTargetPosition,
                        secondCheckpointPosition,
                        checkpointTargetRadius,
                        0f,
                        targetTimeRemaining,
                        externalControlDecision.throttle,
                        externalControlDecision.turn,
                        positionNormalizer,
                        rlObservationForward,
                        rlObservationRecovery,
                        rlObservationRouteTarget,
                        rlObservationRouteLookahead,
                        rlObservationSide,
                        rlObservationRays,
                        rlObservationCarRays,
                        cars);
                policy.computeAction(
                        rlObservation,
                        rlScratchA,
                        rlScratchB,
                        rawExternalControlDecision);
                setDirectRlControl(
                        rawExternalControlDecision.throttle,
                        rawExternalControlDecision.turn);
                rlDecisionTimer =
                        RL_LIVE_DECISION_INTERVAL
                                + (template.vehicleId % 5) * RL_LIVE_DECISION_INTERVAL * 0.11f;
            }
            return externalControlDecision;
        }

        private void ensureRlScratch(RlPolicy policy) {
            int scratchSize = policy.getScratchSize();
            if (rlScratchA == null || rlScratchA.length < scratchSize) {
                rlScratchA = new float[scratchSize];
                rlScratchB = new float[scratchSize];
            }
        }

        private void setDirectRlControl(float throttle, float turn) {
            externalControlDecision.set(
                    applyRlControlDeadzone(MathUtils.clamp(throttle, -1f, 1f)),
                    applyRlControlDeadzone(MathUtils.clamp(turn, -1f, 1f)));
        }

        private float applyRlControlDeadzone(float value) {
            return Math.abs(value) < RL_CONTROL_DEADZONE ? 0f : value;
        }

        private void updateAxes() {
            forwardAxis.set(body.getWorldVector(working.set(0f, 1f)));
            sidewaysAxis.set(body.getWorldVector(working.set(1f, 0f)));
        }

        private float getSignedForwardSpeed() {
            updateAxes();
            return forwardAxis.dot(body.getLinearVelocity());
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
            arenaWallContactTimer = Math.max(0f, arenaWallContactTimer - delta);
            controlLockTimer = Math.max(0f, controlLockTimer - delta);
            ramChargeTimer = Math.max(0f, ramChargeTimer - delta);
        }

        private boolean hasRecentArenaWallContact() {
            return arenaWallContactTimer > 0f;
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

        private void applyGrip(float delta, float impactSlideFactor) {
            updateAxes();
            CarPhysics physics = physics();
            float clampedDelta = MathUtils.clamp(delta, 1f / 240f, 1f / 30f);

            float gripMultiplier = MathUtils.clamp(1f - 0.68f * impactSlideFactor, 0.18f, 1f);
            float dragMultiplier = MathUtils.clamp(1f - 0.58f * impactSlideFactor, 0.24f, 1f);
            if (controlLockTimer > 0f) {
                gripMultiplier *= 0.58f;
                dragMultiplier *= 0.76f;
            }

            float lateralSpeed = sidewaysAxis.dot(body.getLinearVelocity());
            float lateralImpulse =
                    -lateralSpeed * body.getMass() * physics.wheelGrip * gripMultiplier;
            float maxLateralImpulse =
                    body.getMass()
                            * physics.lateralGripPerSecond
                            * physics.wheelGrip
                            * gripMultiplier
                            * clampedDelta;
            lateralImpulse =
                    MathUtils.clamp(lateralImpulse, -maxLateralImpulse, maxLateralImpulse);
            working.set(sidewaysAxis).scl(lateralImpulse);
            body.applyLinearImpulse(working, body.getWorldCenter(), true);

            float yawImpulse =
                    -body.getAngularVelocity()
                            * body.getInertia()
                            * physics.yawGripPerSecond
                            * physics.wheelGrip
                            * gripMultiplier
                            * clampedDelta;
            body.applyAngularImpulse(yawImpulse, true);

            Vector2 velocity = body.getLinearVelocity();
            float speed = velocity.len();
            if (speed > 0.001f) {
                applyDriftSpeedScrub(velocity, speed, lateralSpeed, gripMultiplier);
                working.set(velocity).scl(-physics.aeroDrag * speed * dragMultiplier);
                body.applyForceToCenter(working, true);
            }
            float forwardSpeed = forwardAxis.dot(body.getLinearVelocity());
            if (Math.abs(forwardSpeed) > 0.04f) {
                working.set(forwardAxis)
                        .scl(-Math.signum(forwardSpeed)
                                * physics.rollingResistance
                                * body.getMass()
                                * dragMultiplier);
                body.applyForceToCenter(working, true);
            }
        }

        private void applyDriftSpeedScrub(
                Vector2 velocity,
                float speed,
                float lateralSpeed,
                float gripMultiplier) {
            float slipRatio =
                    MathUtils.clamp(Math.abs(lateralSpeed) / Math.max(1f, speed), 0f, 1f);
            if (slipRatio <= 0.035f) {
                return;
            }

            float scrubAcceleration =
                    physics().driftSpeedScrubPerSecond * slipRatio * slipRatio * speed * gripMultiplier;
            working.set(velocity).nor().scl(-scrubAcceleration * body.getMass());
            body.applyForceToCenter(working, true);
        }

        private void drive(float throttle, float turn) {
            updateAxes();
            float signedForwardSpeed = forwardAxis.dot(body.getLinearVelocity());
            CarPhysics physics = physics();

            float engineForce = 0f;
            if (throttle != 0f) {
                boolean braking =
                        (throttle > 0f && signedForwardSpeed < -REVERSE_ENGAGE_SPEED)
                                || (throttle < 0f && signedForwardSpeed > REVERSE_ENGAGE_SPEED);
                if (braking) {
                    applyServiceBrake(signedForwardSpeed, Math.abs(throttle), physics);
                } else if (throttle > 0f) {
                    float speedRatio =
                            MathUtils.clamp(
                                    Math.abs(signedForwardSpeed) / getForwardMaxSpeed(),
                                    0f,
                                    1f);
                    engineForce = throttle * physics.engineForce() * enginePowerCurve(speedRatio);
                } else {
                    float speedRatio =
                            MathUtils.clamp(
                                    Math.abs(signedForwardSpeed) / getReverseMaxSpeed(),
                                    0f,
                                    1f);
                    engineForce =
                            throttle
                                    * physics.engineForce()
                                    * physics.reversePowerMultiplier
                                    * enginePowerCurve(speedRatio);
                }
            }

            if (engineForce != 0f) {
                working.set(forwardAxis).scl(engineForce);
                body.applyForceToCenter(working, true);
            }

            float steeringStrength = getSteeringAuthority();
            float turnTorque = physics.steeringTorque;
            if (growthBoosted) {
                steeringStrength = MathUtils.clamp(steeringStrength * 0.96f, 0.30f, 1.3f);
                turnTorque *= GROWTH_TURN_MULTIPLIER;
            }
            turnTorque *= getSteeringInertiaCompensation();

            float steeringDirection = 1f;
            if (signedForwardSpeed < -0.25f) {
                steeringDirection = -1f;
            } else if (Math.abs(signedForwardSpeed) <= REVERSE_ENGAGE_SPEED && throttle < -0.1f) {
                steeringDirection = -1f;
            }
            body.applyTorque(turn * steeringDirection * turnTorque * steeringStrength, true);

            float currentSpeed = body.getLinearVelocity().len();
            float maxSpeed = getForwardMaxSpeed();
            if (signedForwardSpeed < 0f) {
                maxSpeed = getReverseMaxSpeed();
            }
            if (currentSpeed > maxSpeed) {
                working.set(body.getLinearVelocity()).scl(maxSpeed / currentSpeed);
                body.setLinearVelocity(working);
            }
        }

        private void applyServiceBrake(
                float signedForwardSpeed,
                float throttleMagnitude,
                CarPhysics physics) {
            float speedToRemove = Math.abs(signedForwardSpeed);
            if (speedToRemove <= 0f || throttleMagnitude <= 0f) {
                return;
            }

            float brakeAcceleration =
                    physics.brakeForce / Math.max(0.001f, body.getMass());
            float speedDelta = brakeAcceleration * PHYSICS_STEP * throttleMagnitude;
            float removedSpeed = Math.min(speedToRemove, speedDelta);
            float impulseDirection = signedForwardSpeed > 0f ? -1f : 1f;
            working.set(forwardAxis).scl(impulseDirection * removedSpeed * body.getMass());
            body.applyLinearImpulse(working, body.getWorldCenter(), true);

            if (speedToRemove <= speedDelta + BRAKE_STOP_EPSILON) {
                Vector2 velocity = body.getLinearVelocity();
                float lateralSpeed = sidewaysAxis.dot(velocity);
                body.setLinearVelocity(working.set(sidewaysAxis).scl(lateralSpeed));
            }
        }

        private CarPhysics physics() {
            return template.physics;
        }

        private float getForwardMaxSpeed() {
            return physics().maxForwardSpeed * (growthBoosted ? MAX_GROWTH_SPEED_MULTIPLIER : 1f);
        }

        private float getReverseMaxSpeed() {
            return getForwardMaxSpeed() * physics().reverseSpeedMultiplier;
        }

        private float enginePowerCurve(float speedRatio) {
            float clamped = MathUtils.clamp(speedRatio, 0f, 1f);
            return 0.16f + 0.84f * (1f - (float) Math.pow(clamped, 1.7f));
        }

        private float getSteeringAuthority() {
            float steeringReferenceSpeed =
                    Math.max(1f, Math.min(getForwardMaxSpeed(), physics().steeringReferenceSpeed));
            float speedRatio =
                    MathUtils.clamp(body.getLinearVelocity().len() / steeringReferenceSpeed, 0f, 1f);
            float highSpeedBlend = speedRatio * speedRatio;
            float authority =
                    MathUtils.lerp(
                            physics().lowSpeedSteeringAuthority,
                            physics().highSpeedSteeringAuthority,
                            highSpeedBlend);
            float slipRatio = getLateralSlipSignal();
            authority *= 1f - MathUtils.clamp(slipRatio * 0.58f, 0f, 0.50f);
            return MathUtils.clamp(authority, 0.18f, 1.18f);
        }

        private float getLateralSlipSignal() {
            if (body == null) {
                return 0f;
            }
            updateAxes();
            Vector2 velocity = body.getLinearVelocity();
            float speed = velocity.len();
            if (speed < 0.1f) {
                return 0f;
            }
            float lateralSpeed = Math.abs(sidewaysAxis.dot(velocity));
            return MathUtils.clamp(lateralSpeed / Math.max(1f, speed), 0f, 1f);
        }

        private float getEstimatedBrakingDistance() {
            if (body == null) {
                return 0f;
            }
            float speed = body.getLinearVelocity().len();
            if (speed <= 0.001f) {
                return 0f;
            }
            float deceleration = Math.max(0.001f, physics().brakeForce / Math.max(0.001f, body.getMass()));
            return speed * speed / (2f * deceleration);
        }

        private void resolveArenaWallCollision(ArenaMap arenaMap) {
            if (!active || body == null || arenaMap == null) {
                return;
            }

            Vector2 position = body.getPosition();
            float edgeDistance = arenaMap.distanceToHazard(position);
            if (arenaMap.supports(position) && edgeDistance >= TRACK_LIMIT_SLOW_MARGIN) {
                return;
            }

            float slowSeverity =
                    arenaMap.supports(position)
                            ? 1f - MathUtils.clamp(
                                    edgeDistance / TRACK_LIMIT_SLOW_MARGIN,
                                    0f,
                                    1f)
                            : 1f;
            CarPhysics physics = physics();
            float dampingPerSecond =
                    MathUtils.lerp(1f, physics.trackLimitVelocityDamping, slowSeverity);
            float angularDampingPerSecond =
                    MathUtils.lerp(1f, physics.trackLimitAngularDamping, slowSeverity);
            float damping = (float) Math.pow(dampingPerSecond, PHYSICS_STEP);
            float angularDamping = (float) Math.pow(angularDampingPerSecond, PHYSICS_STEP);
            arenaWallVelocity.set(body.getLinearVelocity()).scl(damping);
            float maxSpeed =
                    getForwardMaxSpeed()
                            * MathUtils.lerp(1f, physics.trackLimitMaxSpeedMultiplier, slowSeverity);
            if (arenaWallVelocity.len2() > maxSpeed * maxSpeed) {
                arenaWallVelocity.setLength(maxSpeed);
            }
            body.setLinearVelocity(arenaWallVelocity);
            body.setAngularVelocity(body.getAngularVelocity() * angularDamping);

            arenaWallContactTimer = ARENA_WALL_CONTACT_DURATION;
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
            float speedReboundScale = speedReboundBoost;
            float collisionImpulse = MathUtils.clamp(
                    (impactStrength - MIN_COLLISION_RESPONSE_IMPULSE)
                            * physics().collisionImpulseFactor
                            * reboundMultiplier
                            * speedReboundScale,
                    0f,
                    physics().maxCollisionImpulse
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
            float maxStoredImpulse = physics().maxCollisionImpulse * 1.35f;
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

        private void recordCarHit() {
            carHitCount++;
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
            arenaWallContactTimer = 0f;
            lastThrottleCommand = 0f;
            externalControlDecision.set(0f, 0f);
            rawExternalControlDecision.set(0f, 0f);
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
            fixtureDef.density =
                    physics().fixtureDensity * getMassMultiplier() / (sizeScale * sizeScale);
            fixtureDef.friction = physics().fixtureFriction;
            fixtureDef.restitution = physics().fixtureRestitution;
            body.createFixture(fixtureDef);
            shape.dispose();
            body.resetMassData();
        }

        private float getMassMultiplier() {
            return physics().massMultiplier * (growthBoosted ? GROWTH_MASS_MULTIPLIER : 1f);
        }

        private float getSteeringInertiaCompensation() {
            return MathUtils.lerp(1f, getMassMultiplier(), 0.45f) * sizeScale * sizeScale;
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
            if (modelControlled) {
                return "RL";
            }
            if (playerControlled) {
                return "Player";
            }
            return "Idle";
        }

        public Body getBody() {
            return body;
        }

        public boolean isActive() {
            return active;
        }

        public boolean isPlayerControlled() {
            return playerControlled;
        }

        public boolean hasGrowthBoost() {
            return growthBoosted;
        }

        public boolean hasRamCharge() {
            return ramChargeTimer > 0f;
        }

        public int getVehicleId() {
            return template.vehicleId;
        }

        public int getScore() {
            return template.totalPoints;
        }

        public int getLastAttackerId() {
            return lastAttackerId;
        }

        public float getRecentImpactTime() {
            return recentImpactTimer;
        }

        public int getCarHitCount() {
            return carHitCount;
        }

        private void eliminate(World world) {
            if (!active || body == null) {
                return;
            }

            active = false;
            growthBoosted = false;
            ramChargeTimer = 0f;
            sizeScale = 1f;
            clearImpactResponse();
            world.destroyBody(body);
            body = null;
        }
    }

    private static float getRlPositionNormalizer(
            ArenaMap arenaMap,
            Rectangle observationBounds,
            Vector2 observationFocus) {
        if (arenaMap == null) {
            observationBounds.set(0f, 0f, 1f, 1f);
            observationFocus.setZero();
            return RL_POSITION_NORMALIZER_MIN;
        }

        arenaMap.getBounds(observationBounds);
        arenaMap.getFocusPoint(observationFocus);
        return Math.max(
                RL_POSITION_NORMALIZER_MIN,
                Math.max(observationBounds.width, observationBounds.height) * 0.5f);
    }

    private static void fillRlObservation(
            float[] observations,
            int offset,
            Car car,
            ArenaMap arenaMap,
            boolean checkpointTargetActive,
            Vector2 checkpointTargetPosition,
            Vector2 secondCheckpointPosition,
            float checkpointTargetRadius,
            float checkpointProgress,
            float targetTimeRemaining,
            float previousThrottle,
            float previousTurn,
            float positionNormalizer,
            Vector2 observationForward,
            Vector2 observationRecovery,
            Vector2 observationRouteTarget,
            Vector2 observationRouteLookahead,
            Vector2 observationSide,
            float[] rayScratch,
            float[] carRayScratch,
            Array<Car> cars) {
        for (int i = 0; i < RL_OBSERVATION_SIZE; i++) {
            observations[offset + i] = 0f;
        }
        if (car == null || !car.active || car.body == null || arenaMap == null) {
            return;
        }

        Vector2 position = car.body.getPosition();
        Vector2 velocity = car.body.getLinearVelocity();
        observationForward.set(car.body.getWorldVector(observationForward.set(0f, 1f)));
        observationSide.set(-observationForward.y, observationForward.x);

        float targetDx = 0f;
        float targetDy = 0f;
        float targetDistance = 0f;
        float targetForwardAlignment = 0f;
        float targetSideAlignment = 0f;
        float targetClearance = 1f;
        float routeDx = 0f;
        float routeDy = 0f;
        float routeDistance = 0f;
        float routeDistanceToTarget = 0f;
        float routeForwardAlignment = 0f;
        float routeSideAlignment = 0f;
        float routeClearance = 1f;
        float secondCheckpointDx = 0f;
        float secondCheckpointDy = 0f;
        float secondCheckpointDistance = 0f;
        float secondCheckpointForwardAlignment = 0f;
        float secondCheckpointSideAlignment = 0f;
        boolean nearCheckpointTarget = false;
        boolean routeActive = false;

        if (checkpointTargetActive && checkpointTargetPosition != null && checkpointTargetRadius > 0f) {
            targetDx = checkpointTargetPosition.x - position.x;
            targetDy = checkpointTargetPosition.y - position.y;
            targetDistance = (float) Math.sqrt(targetDx * targetDx + targetDy * targetDy);
            nearCheckpointTarget = targetDistance <= checkpointTargetRadius;
            routeDistanceToTarget =
                    Math.max(
                            0f,
                            arenaMap.estimateDriveDistance(
                                            position,
                                            checkpointTargetPosition,
                                            RL_ROUTE_MARGIN)
                                    - checkpointTargetRadius);

            if (targetDistance > 0.0001f) {
                float targetUnitX = targetDx / targetDistance;
                float targetUnitY = targetDy / targetDistance;
                targetForwardAlignment =
                        MathUtils.clamp(
                                targetUnitX * observationForward.x
                                        + targetUnitY * observationForward.y,
                                -1f,
                                1f);
                targetSideAlignment =
                        MathUtils.clamp(
                                targetUnitX * observationSide.x + targetUnitY * observationSide.y,
                                -1f,
                                1f);
                targetClearance =
                        sampleRlRayClearance(
                                arenaMap,
                                position,
                                targetUnitX,
                                targetUnitY,
                                Math.max(0f, targetDistance - checkpointTargetRadius));
            }

            arenaMap.findDriveTarget(
                    position,
                    checkpointTargetPosition,
                    RL_ROUTE_MARGIN,
                    observationRouteTarget);
            routeDx = observationRouteTarget.x - position.x;
            routeDy = observationRouteTarget.y - position.y;
            routeDistance = (float) Math.sqrt(routeDx * routeDx + routeDy * routeDy);
            if (routeDistance > 0.0001f) {
                routeActive = true;
                float routeUnitX = routeDx / routeDistance;
                float routeUnitY = routeDy / routeDistance;
                routeForwardAlignment =
                        MathUtils.clamp(
                                routeUnitX * observationForward.x
                                        + routeUnitY * observationForward.y,
                                -1f,
                                1f);
                routeSideAlignment =
                        MathUtils.clamp(
                                routeUnitX * observationSide.x + routeUnitY * observationSide.y,
                                -1f,
                                1f);
                routeClearance =
                        sampleRlRayClearance(
                                arenaMap,
                                position,
                                routeUnitX,
                                routeUnitY,
                                routeDistance);
            }
        }

        if (secondCheckpointPosition != null) {
            secondCheckpointDx = secondCheckpointPosition.x - position.x;
            secondCheckpointDy = secondCheckpointPosition.y - position.y;
            secondCheckpointDistance =
                    (float)
                            Math.sqrt(
                                    secondCheckpointDx * secondCheckpointDx
                                            + secondCheckpointDy * secondCheckpointDy);
            if (secondCheckpointDistance > 0.0001f) {
                float unitX = secondCheckpointDx / secondCheckpointDistance;
                float unitY = secondCheckpointDy / secondCheckpointDistance;
                secondCheckpointForwardAlignment =
                        MathUtils.clamp(
                                unitX * observationForward.x + unitY * observationForward.y,
                                -1f,
                                1f);
                secondCheckpointSideAlignment =
                        MathUtils.clamp(
                                unitX * observationSide.x + unitY * observationSide.y,
                                -1f,
                                1f);
            }
        }

        float forwardSpeed = observationForward.dot(velocity);
        float lateralSpeed = observationSide.dot(velocity);
        float speed = velocity.len();
        float maxForwardSpeed = Math.max(0.001f, car.getForwardMaxSpeed());
        float speedSignal = MathUtils.clamp(speed / maxForwardSpeed, 0f, 1f);
        boolean offRoad = !arenaMap.supports(position);
        float offRoadDistance = offRoad ? arenaMap.distanceToSafety(position) : 0f;
        float edgeDistance = arenaMap.approximateDistanceToHazard(position);
        float edgeClearance =
                MathUtils.clamp(edgeDistance / RL_HAZARD_DISTANCE_NORMALIZER, 0f, 1f);

        fillRlCarRayObservations(carRayScratch, 0, cars, car, position, observationForward, observationSide);

        float nearestCarForward = 0f;
        float nearestCarSide = 0f;
        float nearestCarDistance = 1f;
        float nearestCarApproach = 0f;
        float nearestCarNearCheckpoint = 0f;
        if (cars != null) {
            float nearestDistanceSquared = Float.MAX_VALUE;
            Car nearestCar = null;
            Vector2 nearestPosition = null;
            for (int i = 0; i < cars.size; i++) {
                Car other = cars.get(i);
                if (other == null || other == car || !other.active || other.body == null) {
                    continue;
                }
                Vector2 otherPosition = other.body.getPosition();
                float dx = otherPosition.x - position.x;
                float dy = otherPosition.y - position.y;
                float distanceSquared = dx * dx + dy * dy;
                if (distanceSquared < nearestDistanceSquared) {
                    nearestDistanceSquared = distanceSquared;
                    nearestCar = other;
                    nearestPosition = otherPosition;
                }
            }
            if (nearestCar != null && nearestPosition != null) {
                float dx = nearestPosition.x - position.x;
                float dy = nearestPosition.y - position.y;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);
                nearestCarForward =
                        MathUtils.clamp(
                                (dx * observationForward.x + dy * observationForward.y)
                                        / RL_CAR_SENSOR_DISTANCE,
                                -1f,
                                1f);
                nearestCarSide =
                        MathUtils.clamp(
                                (dx * observationSide.x + dy * observationSide.y)
                                        / RL_CAR_SENSOR_DISTANCE,
                                -1f,
                                1f);
                nearestCarDistance =
                        MathUtils.clamp(
                                Math.max(0f, distance - RL_CAR_SENSOR_RADIUS)
                                        / RL_CAR_SENSOR_DISTANCE,
                                0f,
                                1f);
                if (distance > 0.0001f) {
                    Vector2 otherVelocity = nearestCar.body.getLinearVelocity();
                    float relativeVelocityX = otherVelocity.x - velocity.x;
                    float relativeVelocityY = otherVelocity.y - velocity.y;
                    float unitX = dx / distance;
                    float unitY = dy / distance;
                    float closingSpeed = -(relativeVelocityX * unitX + relativeVelocityY * unitY);
                    nearestCarApproach = normalizedRlValue(closingSpeed, maxForwardSpeed);
                }
                nearestCarNearCheckpoint =
                        checkpointTargetActive
                                        && checkpointTargetPosition != null
                                        && checkpointTargetRadius > 0f
                                        && nearestPosition.dst(checkpointTargetPosition) <= checkpointTargetRadius
                                ? 1f
                                : 0f;
            }
        }

        observations[offset] = 1f;
        observations[offset + 1] = normalizedRlValue(targetDx, positionNormalizer);
        observations[offset + 2] = normalizedRlValue(targetDy, positionNormalizer);
        observations[offset + 3] = MathUtils.clamp(targetDistance / positionNormalizer, 0f, 1f);
        observations[offset + 4] = normalizedRlValue(routeDx, positionNormalizer);
        observations[offset + 5] = normalizedRlValue(routeDy, positionNormalizer);
        observations[offset + 6] = MathUtils.clamp(routeDistance / positionNormalizer, 0f, 1f);
        observations[offset + 7] = routeForwardAlignment;
        observations[offset + 8] = routeSideAlignment;
        observations[offset + 9] = MathUtils.clamp(routeDistanceToTarget / positionNormalizer, 0f, 1f);
        observations[offset + 10] = targetForwardAlignment;
        observations[offset + 11] = targetSideAlignment;
        observations[offset + 12] = nearCheckpointTarget ? 1f : 0f;
        observations[offset + 13] = MathUtils.clamp(targetTimeRemaining, 0f, 1f);
        observations[offset + 14] = normalizedRlValue(secondCheckpointDx, positionNormalizer);
        observations[offset + 15] = normalizedRlValue(secondCheckpointDy, positionNormalizer);
        observations[offset + 16] =
                MathUtils.clamp(secondCheckpointDistance / positionNormalizer, 0f, 1f);
        observations[offset + 17] = speedSignal;
        observations[offset + 18] = edgeClearance;
        observations[offset + 19] = offRoad ? 1f : 0f;
        observations[offset + 20] = MathUtils.clamp(offRoadDistance / positionNormalizer, 0f, 1f);
        observations[offset + 21] = normalizedRlValue(forwardSpeed, maxForwardSpeed);
        observations[offset + 22] = normalizedRlValue(lateralSpeed, maxForwardSpeed);
        observations[offset + 23] =
                normalizedRlValue(car.body.getAngularVelocity(), RL_ANGULAR_VELOCITY_NORMALIZER);
        observations[offset + 24] = MathUtils.clamp(previousThrottle, -1f, 1f);
        observations[offset + 25] = MathUtils.clamp(previousTurn, -1f, 1f);
        for (int i = 0; i < 6; i++) {
            observations[offset + 26 + i] = carRayScratch[i];
        }
        observations[offset + 32] = nearestCarForward;
        observations[offset + 33] = nearestCarSide;
        observations[offset + 34] = nearestCarDistance;
        observations[offset + 35] = nearestCarApproach;
        observations[offset + 36] = nearestCarNearCheckpoint;
        observations[offset + 37] = routeClearance;
        observations[offset + 38] = targetClearance;
        observations[offset + 39] = routeActive ? 1f : 0f;
        observations[offset + 40] = secondCheckpointForwardAlignment;
        observations[offset + 41] = secondCheckpointSideAlignment;
    }

    private static void fillRlRayObservations(
            float[] observations,
            int offset,
            ArenaMap arenaMap,
            Vector2 carPosition,
            Vector2 forward,
            Vector2 side) {
        side.set(-forward.y, forward.x);
        observations[offset] = sampleRlRayClearance(arenaMap, carPosition, forward.x, forward.y);
        observations[offset + 1] =
                sampleRlRayClearance(
                        arenaMap,
                        carPosition,
                        forward.x + side.x,
                        forward.y + side.y);
        observations[offset + 2] =
                sampleRlRayClearance(
                        arenaMap,
                        carPosition,
                        forward.x - side.x,
                        forward.y - side.y);
        observations[offset + 3] = sampleRlRayClearance(arenaMap, carPosition, side.x, side.y);
        observations[offset + 4] = sampleRlRayClearance(arenaMap, carPosition, -side.x, -side.y);
        observations[offset + 5] = sampleRlRayClearance(arenaMap, carPosition, -forward.x, -forward.y);
    }

    private static float sampleRlRayClearance(
            ArenaMap arenaMap,
            Vector2 position,
            float directionX,
            float directionY) {
        return sampleRlRayClearance(arenaMap, position, directionX, directionY, RL_RAYCAST_DISTANCE);
    }

    private static float sampleRlRayClearance(
            ArenaMap arenaMap,
            Vector2 position,
            float directionX,
            float directionY,
            float maxDistance) {
        float length = (float) Math.sqrt(directionX * directionX + directionY * directionY);
        if (arenaMap == null || position == null || length <= 0.0001f) {
            return 0f;
        }

        float unitX = directionX / length;
        float unitY = directionY / length;
        float rayDistance = MathUtils.clamp(maxDistance, 0f, RL_RAYCAST_DISTANCE);
        if (rayDistance <= 0.0001f) {
            return 1f;
        }
        for (float distance = RL_RAYCAST_STEP;
                distance <= rayDistance;
                distance += RL_RAYCAST_STEP) {
            float sampleX = position.x + unitX * distance;
            float sampleY = position.y + unitY * distance;
            if (arenaMap.approximateDistanceToHazard(sampleX, sampleY) < RL_ROUTE_MARGIN) {
                return MathUtils.clamp(distance / rayDistance, 0f, 1f);
            }
        }
        return 1f;
    }

    private static void fillRlCarRayObservations(
            float[] observations,
            int offset,
            Array<Car> cars,
            Car self,
            Vector2 carPosition,
            Vector2 forward,
            Vector2 side) {
        observations[offset] = sampleRlCarRayClearance(cars, self, carPosition, forward.x, forward.y);
        observations[offset + 1] =
                sampleRlCarRayClearance(
                        cars,
                        self,
                        carPosition,
                        forward.x + side.x,
                        forward.y + side.y);
        observations[offset + 2] =
                sampleRlCarRayClearance(
                        cars,
                        self,
                        carPosition,
                        forward.x - side.x,
                        forward.y - side.y);
        observations[offset + 3] = sampleRlCarRayClearance(cars, self, carPosition, side.x, side.y);
        observations[offset + 4] = sampleRlCarRayClearance(cars, self, carPosition, -side.x, -side.y);
        observations[offset + 5] =
                sampleRlCarRayClearance(cars, self, carPosition, -forward.x, -forward.y);
    }

    private static float sampleRlCarRayClearance(
            Array<Car> cars,
            Car self,
            Vector2 position,
            float directionX,
            float directionY) {
        if (cars == null || position == null) {
            return 1f;
        }
        float length = (float) Math.sqrt(directionX * directionX + directionY * directionY);
        if (length <= 0.0001f) {
            return 1f;
        }
        float unitX = directionX / length;
        float unitY = directionY / length;
        float nearest = RL_CAR_SENSOR_DISTANCE;
        for (int i = 0; i < cars.size; i++) {
            Car other = cars.get(i);
            if (other == null || other == self || !other.active || other.body == null) {
                continue;
            }
            Vector2 otherPosition = other.body.getPosition();
            float dx = otherPosition.x - position.x;
            float dy = otherPosition.y - position.y;
            float along = dx * unitX + dy * unitY;
            if (along < 0f || along > RL_CAR_SENSOR_DISTANCE + RL_CAR_SENSOR_RADIUS) {
                continue;
            }
            float perpendicularX = dx - along * unitX;
            float perpendicularY = dy - along * unitY;
            float perpendicularSquared =
                    perpendicularX * perpendicularX + perpendicularY * perpendicularY;
            if (perpendicularSquared <= RL_CAR_SENSOR_RADIUS * RL_CAR_SENSOR_RADIUS) {
                nearest = Math.min(nearest, Math.max(0f, along - RL_CAR_SENSOR_RADIUS));
            }
        }
        return MathUtils.clamp(nearest / RL_CAR_SENSOR_DISTANCE, 0f, 1f);
    }

    private static float normalizedRlValue(float value, float normalizer) {
        return MathUtils.clamp(value / Math.max(0.0001f, normalizer), -1f, 1f);
    }

    public static final class RlTrainingConfig {
        public final Array<ArenaMap> maps = new Array<ArenaMap>();
        public int controlledAgentCount = RL_DEFAULT_CONTROLLED_AGENTS;
        public int fieldSize = RL_DEFAULT_FIELD_SIZE;
        public int actionRepeat = RL_DEFAULT_ACTION_REPEAT;
        public int maxActionSteps = RL_DEFAULT_MAX_ACTION_STEPS;
        public int maxCheckpoints = RL_DEFAULT_MAX_CHECKPOINTS;
        public float checkpointRadius = RL_DEFAULT_CHECKPOINT_RADIUS;
        public float checkpointDeadlineSeconds = RL_DEFAULT_CHECKPOINT_DEADLINE_SECONDS;
        public long seed = 1L;
        public boolean skipCountdown = true;
        public boolean raceMode = true;
        public boolean randomRaceSpawns;
        public boolean debugTraceEnabled;

        public RlTrainingConfig addMap(ArenaMap map) {
            if (map != null) {
                maps.add(map);
            }
            return this;
        }

        public RlTrainingConfig withControlledAgentCount(int controlledAgentCount) {
            this.controlledAgentCount = controlledAgentCount;
            return this;
        }

        public RlTrainingConfig withFieldSize(int fieldSize) {
            this.fieldSize = fieldSize;
            return this;
        }

        public RlTrainingConfig withActionRepeat(int actionRepeat) {
            this.actionRepeat = actionRepeat;
            return this;
        }

        public RlTrainingConfig withMaxActionSteps(int maxActionSteps) {
            this.maxActionSteps = maxActionSteps;
            return this;
        }

        public RlTrainingConfig withMaxCheckpoints(int maxCheckpoints) {
            this.maxCheckpoints = maxCheckpoints;
            return this;
        }

        public RlTrainingConfig withCheckpointRadius(float checkpointRadius) {
            this.checkpointRadius = checkpointRadius;
            return this;
        }

        public RlTrainingConfig withCheckpointDeadlineSeconds(float checkpointDeadlineSeconds) {
            this.checkpointDeadlineSeconds = checkpointDeadlineSeconds;
            return this;
        }

        public RlTrainingConfig withSeed(long seed) {
            this.seed = seed;
            return this;
        }

        public RlTrainingConfig withSkipCountdown(boolean skipCountdown) {
            this.skipCountdown = skipCountdown;
            return this;
        }

        public RlTrainingConfig withRaceMode(boolean raceMode) {
            this.raceMode = true;
            return this;
        }

        public RlTrainingConfig withRandomRaceSpawns(boolean randomRaceSpawns) {
            this.randomRaceSpawns = randomRaceSpawns;
            return this;
        }

        public RlTrainingConfig withDebugTraceEnabled(boolean debugTraceEnabled) {
            this.debugTraceEnabled = debugTraceEnabled;
            return this;
        }

    }

    public static final class RlStepResult {
        public final float[] observations;
        public final float[] rewards;
        public final float[] rewardBreakdown;
        public final String[] rewardBreakdownNames;
        public final float[] effectiveActions;
        public final boolean[] dones;
        public final boolean episodeDone;
        public final int actionStep;
        public final int winnerAgentIndex;
        public final String winnerLabel;
        public final String currentMapId;
        public final String currentMapName;
        public final int[] checkpointsReached;
        public final int[] eliminations;
        public final float[] progressTowardCheckpoint;
        public final float[] debugTrace;
        public final String[] debugTraceNames;

        private RlStepResult(
                float[] observations,
                float[] rewards,
                float[] rewardBreakdown,
                String[] rewardBreakdownNames,
                float[] effectiveActions,
                boolean[] dones,
                boolean episodeDone,
                int actionStep,
                int winnerAgentIndex,
                String winnerLabel,
                String currentMapId,
                String currentMapName,
                int[] checkpointsReached,
                int[] eliminations,
                float[] progressTowardCheckpoint,
                float[] debugTrace,
                String[] debugTraceNames) {
            this.observations = observations;
            this.rewards = rewards;
            this.rewardBreakdown = rewardBreakdown;
            this.rewardBreakdownNames = rewardBreakdownNames;
            this.effectiveActions = effectiveActions;
            this.dones = dones;
            this.episodeDone = episodeDone;
            this.actionStep = actionStep;
            this.winnerAgentIndex = winnerAgentIndex;
            this.winnerLabel = winnerLabel;
            this.currentMapId = currentMapId;
            this.currentMapName = currentMapName;
            this.checkpointsReached = checkpointsReached;
            this.eliminations = eliminations;
            this.progressTowardCheckpoint = progressTowardCheckpoint;
            this.debugTrace = debugTrace;
            this.debugTraceNames = debugTraceNames;
        }
    }

    public static final class RlTrainingEnvironment implements AutoCloseable {
        private final RlTrainingConfig config;
        private final RatassGame game = new RatassGame();
        private final Array<ArenaMap> trainingMaps;
        private final Array<Integer> controlledVehicleIds = new Array<Integer>();
        private final Rectangle observationBounds = new Rectangle();
        private final Vector2 observationFocus = new Vector2();
        private final Vector2 observationForward = new Vector2();
        private final Vector2 observationRecovery = new Vector2();
        private final Vector2 observationRouteTarget = new Vector2();
        private final Vector2 observationRouteLookahead = new Vector2();
        private final Vector2 raceProgressPreviousPosition = new Vector2();
        private final Vector2 raceProgressTargetPosition = new Vector2();
        private final Vector2 snapshotTargetPosition = new Vector2();
        private final Vector2 observationTargetPosition = new Vector2();
        private final Vector2 observationSecondCheckpoint = new Vector2();
        private final Vector2 observationSide = new Vector2();
        private final RlAgentSnapshot[] beforeSnapshots;
        private final RlAgentSnapshot[] afterSnapshots;
        private final float[] observations;
        private final float[] rewards;
        private final float[] rewardBreakdown;
        private final float[] effectiveActions;
        private final float[] currentActionThrottle;
        private final float[] previousActionThrottle;
        private final float[] currentActionTurn;
        private final float[] previousActionTurn;
        private final float[] checkpointDeadlineTimers;
        private final float[] checkpointDeadlineDurations;
        private final float[] progressTowardCheckpoint;
        private final float[] bestRaceRouteProgress;
        private final int[] raceNextCheckpointIndices;
        private final int[] raceTargetSequences;
        private final float[] observationRays = new float[6];
        private final float[] observationCarRays = new float[6];
        private final int[] checkpointsReached;
        private final int[] eliminations;
        private final int[] checkpointCrossRewardSequences;
        private final boolean[] checkpointCrossRewardEvents;
        private final boolean[] checkpointCompleteEvents;
        private final boolean[] checkpointTimeoutEvents;
        private final boolean[] raceCheckpointCrossEvents;
        private final boolean[] dones;
        private final int controlledAgentCount;
        private int episodeIndex;
        private int actionStep;
        private boolean episodeStarted;
        private boolean episodeDone;
        private boolean closed;

        public RlTrainingEnvironment(RlTrainingConfig config) {
            this.config = config == null ? new RlTrainingConfig() : config;
            this.config.raceMode = true;
            trainingMaps =
                    this.config.maps.size == 0
                            ? ArenaMaps.createHeadlessTrainingSet()
                            : this.config.maps;
            controlledAgentCount =
                    MathUtils.clamp(this.config.controlledAgentCount, 1, MAX_CAR_COUNT);
            beforeSnapshots = new RlAgentSnapshot[controlledAgentCount];
            afterSnapshots = new RlAgentSnapshot[controlledAgentCount];
            for (int i = 0; i < controlledAgentCount; i++) {
                beforeSnapshots[i] = new RlAgentSnapshot();
                afterSnapshots[i] = new RlAgentSnapshot();
            }
            observations = new float[controlledAgentCount * RL_OBSERVATION_SIZE];
            rewards = new float[controlledAgentCount];
            rewardBreakdown = new float[controlledAgentCount * RL_REWARD_BREAKDOWN_SIZE];
            effectiveActions = new float[controlledAgentCount * RL_ACTION_SIZE];
            currentActionThrottle = new float[controlledAgentCount];
            previousActionThrottle = new float[controlledAgentCount];
            currentActionTurn = new float[controlledAgentCount];
            previousActionTurn = new float[controlledAgentCount];
            checkpointDeadlineTimers = new float[controlledAgentCount];
            checkpointDeadlineDurations = new float[controlledAgentCount];
            progressTowardCheckpoint = new float[controlledAgentCount];
            bestRaceRouteProgress = new float[controlledAgentCount];
            raceNextCheckpointIndices = new int[controlledAgentCount];
            raceTargetSequences = new int[controlledAgentCount];
            checkpointsReached = new int[controlledAgentCount];
            eliminations = new int[controlledAgentCount];
            checkpointCrossRewardSequences = new int[controlledAgentCount];
            checkpointCrossRewardEvents = new boolean[controlledAgentCount];
            checkpointCompleteEvents = new boolean[controlledAgentCount];
            checkpointTimeoutEvents = new boolean[controlledAgentCount];
            raceCheckpointCrossEvents = new boolean[controlledAgentCount];
            dones = new boolean[controlledAgentCount];
        }

        public int getControlledAgentCount() {
            return controlledAgentCount;
        }

        public int getObservationSize() {
            return RL_OBSERVATION_SIZE;
        }

        public int getActionSize() {
            return RL_ACTION_SIZE;
        }

        public RlStepResult reset() {
            ensureOpen();
            long episodeSeed = config.seed + episodeIndex * 104729L;
            episodeIndex++;
            MathUtils.random.setSeed(episodeSeed);
            Box2D.init();

            createRoster();
            game.rlTrainingAllowSoloRound = game.roster.size <= 1;
            game.rlTrainingDisablePickups = true;
            game.rlTrainingRandomSpawnLocations = config.randomRaceSpawns;
            game.rlTrainingMode = true;
            game.rlTrainingRaceMode = true;
            game.mapProgression =
                    new MapProgression(
                            trainingMaps,
                            new Random(episodeSeed ^ 0x9E3779B97F4A7C15L));
            game.frameThrottleInput = 0f;
            game.frameTurnInput = 0f;
            game.roundNumber = 0;
            game.playerWins = 0;
            game.resetRound(false);
            if (config.skipCountdown) {
                game.preRoundCountdownTimer = 0f;
                game.countdownCueSecond = 0;
            }
            game.growthPickupActive = false;
            game.pointPickupActive = false;
            game.checkpointTargetActive = false;

            actionStep = 0;
            episodeStarted = true;
            episodeDone = false;
            clearEpisodeMetrics();
            clearRewards();
            activateRaceCheckpointTarget(0);
            resetCheckpointDeadlines();
            captureSnapshots(afterSnapshots);
            buildObservations();
            return createResult();
        }

        public RlStepResult step(float[] actions) {
            ensureOpen();
            if (!episodeStarted) {
                reset();
            }
            if (episodeDone) {
                return createResult();
            }

            clearStepEvents();
            captureSnapshots(beforeSnapshots);
            applyActions(actions);
            int repeats = Math.max(1, config.actionRepeat);
            for (int i = 0; i < repeats && !game.roundOver; i++) {
                game.stepSimulation(PHYSICS_STEP);
                if (config.raceMode) {
                    updateRaceCheckpointCrossEvents();
                }
            }
            actionStep++;

            boolean maxStepsReached = !game.roundOver && actionStep >= getMaxActionSteps();

            captureSnapshots(afterSnapshots);
            updateCheckpointDeadlines(repeats * PHYSICS_STEP);
            captureSnapshots(afterSnapshots);
            updateCheckpointProgress(repeats * PHYSICS_STEP);
            episodeDone =
                    maxStepsReached
                            || game.roundOver
                            || !hasActiveControlledAgent()
                            || hasCompletedTrainingRace();
            computeRewards();
            buildObservations();
            return createResult();
        }

        @Override
        public void close() {
            if (!closed) {
                game.dispose();
                closed = true;
            }
        }

        private void createRoster() {
            game.roster.clear();
            controlledVehicleIds.clear();

            int controlledAgentCount = getControlledAgentCount();
            for (int i = 0; i < controlledAgentCount; i++) {
                CarVisual visual = game.getCarVisual(i);
                game.addRosterTemplate(
                        "Learner " + (i + 1),
                        false,
                        new Color(visual.color),
                        visual,
                        "learner-" + i,
                        true);
                controlledVehicleIds.add(
                        Integer.valueOf(game.roster.get(game.roster.size - 1).vehicleId));
            }

            game.invalidateLeaderboard();
        }

        private int getMaxActionSteps() {
            return config.maxActionSteps <= 0
                    ? RL_DEFAULT_MAX_ACTION_STEPS
                    : config.maxActionSteps;
        }

        private int getMaxCheckpoints() {
            if (config.raceMode && config.maxCheckpoints < 0 && game.currentMap != null) {
                return Math.max(1, game.currentMap.getCheckpointCount());
            }
            if (config.raceMode && config.maxCheckpoints == 0 && game.currentMap != null) {
                return Math.max(1, game.currentMap.getCheckpointCount() * game.getRaceLapsToWin());
            }
            return config.maxCheckpoints <= 0 ? RL_DEFAULT_MAX_CHECKPOINTS : config.maxCheckpoints;
        }

        private float getBaseCheckpointDeadlineSeconds() {
            if (config.checkpointDeadlineSeconds > 0f) {
                return Math.max(PHYSICS_STEP, config.checkpointDeadlineSeconds);
            }
            float episodeSeconds =
                    getMaxActionSteps()
                            * Math.max(1, config.actionRepeat)
                            * PHYSICS_STEP;
            float perCheckpointSeconds = episodeSeconds / Math.max(1, getMaxCheckpoints());
            return Math.max(RACE_CHECKPOINT_DEFAULT_DEADLINE, perCheckpointSeconds * 0.85f);
        }

        private float getCheckpointDeadlineSeconds(int agentIndex) {
            float baseDeadline = getBaseCheckpointDeadlineSeconds();
            if (!config.raceMode || game.currentMap == null) {
                return baseDeadline;
            }

            Car car = getControlledCar(agentIndex);
            SpawnPoint checkpoint = getRaceCheckpoint(agentIndex);
            if (car == null || car.body == null || checkpoint == null) {
                return baseDeadline;
            }

            raceProgressTargetPosition.set(checkpoint.x, checkpoint.y);
            float routeDistance =
                    game.currentMap.estimateDriveDistance(
                            car.body.getPosition(),
                            raceProgressTargetPosition,
                            RL_ROUTE_MARGIN);
            float routeDeadline =
                    RL_CHECKPOINT_ROUTE_DEADLINE_BASE_SECONDS
                            + routeDistance * RL_CHECKPOINT_ROUTE_DEADLINE_SECONDS_PER_UNIT;
            float maxDeadline = Math.max(baseDeadline, RL_CHECKPOINT_ROUTE_DEADLINE_MAX_SECONDS);
            return MathUtils.clamp(Math.max(baseDeadline, routeDeadline), baseDeadline, maxDeadline);
        }

        private void resetCheckpointDeadlines() {
            for (int i = 0; i < getControlledAgentCount(); i++) {
                float deadline = getCheckpointDeadlineSeconds(i);
                checkpointDeadlineTimers[i] = deadline;
                checkpointDeadlineDurations[i] = deadline;
                checkpointTimeoutEvents[i] = false;
            }
            updateGameCheckpointDeadlineTimer();
        }

        private void resetCheckpointDeadline(int agentIndex) {
            if (agentIndex < 0 || agentIndex >= getControlledAgentCount()) {
                return;
            }
            float deadline = getCheckpointDeadlineSeconds(agentIndex);
            checkpointDeadlineTimers[agentIndex] = deadline;
            checkpointDeadlineDurations[agentIndex] = deadline;
            checkpointTimeoutEvents[agentIndex] = false;
            updateGameCheckpointDeadlineTimer();
        }

        private void updateGameCheckpointDeadlineTimer() {
            float maxRemaining = 0f;
            for (int i = 0; i < getControlledAgentCount(); i++) {
                maxRemaining = Math.max(maxRemaining, checkpointDeadlineTimers[i]);
            }
            game.checkpointDeadlineTimer = maxRemaining;
        }

        private void activateRaceCheckpointTarget(int agentIndex) {
            if (game.currentMap == null || game.currentMap.getCheckpointCount() == 0) {
                game.checkpointTargetActive = false;
                return;
            }
            if (!getRaceCheckpointTarget(agentIndex, snapshotTargetPosition)) {
                game.checkpointTargetActive = false;
                return;
            }
            game.checkpointTargetRadius = getRaceTargetRadius();
            game.checkpointTargetPosition.set(snapshotTargetPosition);
            updateGameCheckpointDeadlineTimer();
            game.checkpointTargetActive = true;
            game.checkpointTargetSequence++;
        }

        private void advanceRaceCheckpoint(int agentIndex) {
            if (game.currentMap == null || game.currentMap.getCheckpointCount() == 0) {
                raceNextCheckpointIndices[agentIndex] = 0;
                return;
            }
            raceNextCheckpointIndices[agentIndex] =
                    (raceNextCheckpointIndices[agentIndex] + 1) % game.currentMap.getCheckpointCount();
            raceTargetSequences[agentIndex]++;
            resetRaceRouteProgressBaseline(agentIndex);
        }

        private void resetRaceRouteProgressBaseline(int agentIndex) {
            Car car = getControlledCar(agentIndex);
            bestRaceRouteProgress[agentIndex] =
                    car != null && car.active && car.body != null
                            ? getOrderedRaceRouteProgress(agentIndex, car.body.getPosition())
                            : 0f;
        }

        private float getRaceTargetRadius() {
            return Math.max(RACE_CHECKPOINT_MIN_RADIUS, config.checkpointRadius);
        }

        private SpawnPoint getRaceCheckpoint(int agentIndex) {
            int checkpointIndex = getRaceCheckpointIndex(agentIndex);
            return checkpointIndex < 0 ? null : game.currentMap.getCheckpoint(checkpointIndex);
        }

        private int getRaceCheckpointIndex(int agentIndex) {
            if (game.currentMap == null || game.currentMap.getCheckpointCount() == 0) {
                return -1;
            }
            int clampedAgent = MathUtils.clamp(agentIndex, 0, getControlledAgentCount() - 1);
            return MathUtils.clamp(
                    raceNextCheckpointIndices[clampedAgent],
                    0,
                    game.currentMap.getCheckpointCount() - 1);
        }

        private int getSecondRaceCheckpointIndex(int agentIndex) {
            if (game.currentMap == null || game.currentMap.getCheckpointCount() <= 1) {
                return -1;
            }
            int clampedAgent = MathUtils.clamp(agentIndex, 0, getControlledAgentCount() - 1);
            return (raceNextCheckpointIndices[clampedAgent] + 1) % game.currentMap.getCheckpointCount();
        }

        private boolean getRaceCheckpointTarget(int agentIndex, Vector2 out) {
            SpawnPoint checkpoint = getRaceCheckpoint(agentIndex);
            if (checkpoint == null || out == null) {
                return false;
            }
            out.set(checkpoint.x, checkpoint.y);
            return true;
        }

        private boolean getSecondRaceCheckpointTarget(int agentIndex, Vector2 out) {
            if (game.currentMap == null || game.currentMap.getCheckpointCount() <= 1 || out == null) {
                return false;
            }
            int checkpointIndex = getSecondRaceCheckpointIndex(agentIndex);
            if (checkpointIndex < 0) {
                return false;
            }
            SpawnPoint checkpoint = game.currentMap.getCheckpoint(checkpointIndex);
            out.set(checkpoint.x, checkpoint.y);
            return true;
        }

        private void updateRaceCheckpointCrossEvents() {
            for (int agentIndex = 0; agentIndex < getControlledAgentCount(); agentIndex++) {
                if (raceCheckpointCrossEvents[agentIndex]) {
                    continue;
                }
                Car car = getControlledCar(agentIndex);
                SpawnPoint checkpoint = getRaceCheckpoint(agentIndex);
                if (car != null && checkpoint != null && game.hasPassedRaceCheckpoint(car, checkpoint)) {
                    raceCheckpointCrossEvents[agentIndex] = true;
                }
            }
        }

        private void applyActions(float[] actions) {
            for (int agentIndex = 0; agentIndex < getControlledAgentCount(); agentIndex++) {
                Car car = getControlledCar(agentIndex);
                if (car == null || !car.active) {
                    continue;
                }
                int actionOffset = agentIndex * RL_ACTION_SIZE;
                float throttle =
                        actions != null && actionOffset < actions.length
                                ? actions[actionOffset]
                                : 0f;
                float turn =
                        actions != null && actionOffset + 1 < actions.length
                                ? actions[actionOffset + 1]
                                : 0f;
                throttle = MathUtils.clamp(throttle, -1f, 1f);
                turn = MathUtils.clamp(turn, -1f, 1f);
                previousActionThrottle[agentIndex] = currentActionThrottle[agentIndex];
                previousActionTurn[agentIndex] = currentActionTurn[agentIndex];
                currentActionThrottle[agentIndex] = throttle;
                currentActionTurn[agentIndex] = turn;
                car.setDirectRlControl(throttle, turn);
            }
        }

        private void captureSnapshots(RlAgentSnapshot[] snapshots) {
            for (int agentIndex = 0; agentIndex < getControlledAgentCount(); agentIndex++) {
                captureSnapshot(agentIndex, snapshots[agentIndex]);
            }
        }

        private void captureSnapshot(int agentIndex, RlAgentSnapshot snapshot) {
            snapshot.clear();
            Car car = getControlledCar(agentIndex);
            if (car == null) {
                return;
            }

            snapshot.active = car.active && car.body != null;
            if (!snapshot.active || game.currentMap == null) {
                return;
            }

            Vector2 position = car.body.getPosition();
            snapshot.edgeDistance = game.currentMap.approximateDistanceToHazard(position);
            snapshot.offRoad = !game.currentMap.supports(position);
            snapshot.offRoadDistance = snapshot.offRoad ? game.currentMap.distanceToSafety(position) : 0f;
            Vector2 velocity = car.body.getLinearVelocity();
            snapshot.speed = velocity.len();
            snapshot.signedForwardSpeed = car.getSignedForwardSpeed();
            snapshot.nearestCarDistance = getNearestActiveCarDistance(car, position);
            snapshot.wallContact = car.hasRecentArenaWallContact();
            snapshot.carHitCount = car.getCarHitCount();
            snapshot.angularSpeed = Math.abs(car.body.getAngularVelocity());
            snapshot.effectiveThrottle = car.externalControlDecision.throttle;
            boolean targetActive = game.checkpointTargetActive;
            Vector2 targetPosition = game.checkpointTargetPosition;
            float checkpointRadius = game.checkpointTargetRadius;
            int targetSequence = game.checkpointTargetSequence;
            if (config.raceMode && getRaceCheckpointTarget(agentIndex, snapshotTargetPosition)) {
                targetActive = true;
                targetPosition = snapshotTargetPosition;
                checkpointRadius = getRaceTargetRadius();
                targetSequence = raceTargetSequences[agentIndex];
            }
            if (targetActive) {
                snapshot.checkpointTargetActive = true;
                snapshot.checkpointTargetSequence = targetSequence;
                snapshot.checkpointTargetRadius = checkpointRadius;
                float checkpointTargetDistance = position.dst(targetPosition);
                SpawnPoint raceCheckpoint = config.raceMode ? getRaceCheckpoint(agentIndex) : null;
                snapshot.checkpointTargetInside =
                        raceCheckpoint != null && raceCheckpoint.hasGate
                                ? game.isNearRaceCheckpointGate(
                                        position,
                                        raceCheckpoint,
                                        RACE_CHECKPOINT_GATE_MARGIN)
                                : checkpointRadius - checkpointTargetDistance >= 0f;
                if (checkpointTargetDistance > 0.0001f) {
                    snapshot.checkpointRouteDistance =
                            Math.max(
                                    0f,
                                    game.currentMap.estimateDriveDistance(
                                                    position,
                                                    targetPosition,
                                            RL_ROUTE_MARGIN)
                                            - checkpointRadius);
                }
                if (config.raceMode) {
                    snapshot.raceRouteProgress = getOrderedRaceRouteProgress(agentIndex, position);
                    snapshot.routeForwardSpeed =
                            getRouteForwardSpeed(position, targetPosition, velocity);
                }
            }
            observationForward.set(car.body.getWorldVector(observationForward.set(0f, 1f)));
            observationSide.set(-observationForward.y, observationForward.x);
            game.currentMap.findRecoveryPoint(position, observationRecovery);
            observationRecovery.sub(position);
            if (!observationRecovery.isZero(0.0001f)) {
                observationRecovery.nor();
                snapshot.recoverySpeed = observationRecovery.dot(velocity);
            }
        }

        private float getOrderedRaceRouteProgress(int agentIndex, Vector2 position) {
            if (game.currentMap == null
                    || position == null
                    || game.currentMap.getCheckpointCount() <= 1) {
                return 0f;
            }

            int checkpointIndex = getRaceCheckpointIndex(agentIndex);
            if (checkpointIndex < 0) {
                return 0f;
            }

            int checkpointCount = game.currentMap.getCheckpointCount();
            SpawnPoint target = game.currentMap.getCheckpoint(checkpointIndex);
            SpawnPoint previous =
                    game.currentMap.getCheckpoint((checkpointIndex + checkpointCount - 1) % checkpointCount);
            raceProgressPreviousPosition.set(previous.x, previous.y);
            raceProgressTargetPosition.set(target.x, target.y);
            float segmentDistance =
                    game.currentMap.estimateDriveDistance(
                            raceProgressPreviousPosition,
                            raceProgressTargetPosition,
                            RL_ROUTE_MARGIN);
            float remainingDistance =
                    game.currentMap.estimateDriveDistance(
                            position,
                            raceProgressTargetPosition,
                            RL_ROUTE_MARGIN);
            return segmentDistance - remainingDistance;
        }

        private float getRouteForwardSpeed(Vector2 position, Vector2 targetPosition, Vector2 velocity) {
            if (game.currentMap == null
                    || position == null
                    || targetPosition == null
                    || velocity == null) {
                return 0f;
            }
            game.currentMap.findDriveTarget(
                    position,
                    targetPosition,
                    RL_ROUTE_MARGIN,
                    observationRouteTarget);
            observationRouteTarget.sub(position);
            float routeDistance = observationRouteTarget.len();
            if (routeDistance <= 0.0001f) {
                return 0f;
            }
            observationRouteTarget.scl(1f / routeDistance);
            return observationRouteTarget.dot(velocity);
        }

        private float getNearestActiveCarDistance(Car self, Vector2 position) {
            float nearestDistance = Float.MAX_VALUE;
            for (int i = 0; i < game.cars.size; i++) {
                Car other = game.cars.get(i);
                if (other == null || other == self || !other.active || other.body == null) {
                    continue;
                }
                nearestDistance = Math.min(nearestDistance, position.dst(other.body.getPosition()));
            }
            return nearestDistance;
        }

        private void clearStepEvents() {
            for (int agentIndex = 0; agentIndex < getControlledAgentCount(); agentIndex++) {
                progressTowardCheckpoint[agentIndex] = 0f;
                checkpointCrossRewardEvents[agentIndex] = false;
                checkpointCompleteEvents[agentIndex] = false;
                checkpointTimeoutEvents[agentIndex] = false;
                raceCheckpointCrossEvents[agentIndex] = false;
            }
        }

        private void updateCheckpointDeadlines(float elapsedSeconds) {
            if (!game.checkpointTargetActive || game.checkpointTargetRadius <= 0f) {
                resetCheckpointDeadlines();
                return;
            }

            float maxRemaining = 0f;
            for (int agentIndex = 0; agentIndex < getControlledAgentCount(); agentIndex++) {
                RlAgentSnapshot after = afterSnapshots[agentIndex];
                if (!after.active) {
                    continue;
                }
                if (raceCheckpointCrossEvents[agentIndex]) {
                    maxRemaining = Math.max(maxRemaining, checkpointDeadlineTimers[agentIndex]);
                    continue;
                }

                checkpointDeadlineTimers[agentIndex] =
                        Math.max(0f, checkpointDeadlineTimers[agentIndex] - elapsedSeconds);
                maxRemaining = Math.max(maxRemaining, checkpointDeadlineTimers[agentIndex]);
                if (checkpointDeadlineTimers[agentIndex] <= 0f) {
                    checkpointTimeoutEvents[agentIndex] = true;
                }
            }
            game.checkpointDeadlineTimer = maxRemaining;
        }

        private void updateCheckpointProgress(float elapsedSeconds) {
            boolean raceTargetAdvanced = false;
            for (int agentIndex = 0; agentIndex < getControlledAgentCount(); agentIndex++) {
                RlAgentSnapshot before = beforeSnapshots[agentIndex];
                RlAgentSnapshot after = afterSnapshots[agentIndex];
                if (!before.active || !after.active || !after.checkpointTargetActive) {
                    continue;
                }
                boolean checkpointSatisfied = false;

                if (before.checkpointTargetActive
                        && before.checkpointTargetSequence == after.checkpointTargetSequence) {
                    float newProgress =
                            after.raceRouteProgress - bestRaceRouteProgress[agentIndex];
                    if (newProgress > 0f) {
                        progressTowardCheckpoint[agentIndex] = newProgress;
                        bestRaceRouteProgress[agentIndex] = after.raceRouteProgress;
                    }
                }

                if (raceCheckpointCrossEvents[agentIndex]) {
                    if (checkpointCrossRewardSequences[agentIndex] != after.checkpointTargetSequence) {
                        checkpointCrossRewardSequences[agentIndex] = after.checkpointTargetSequence;
                        checkpointCrossRewardEvents[agentIndex] = true;
                    }
                    checkpointSatisfied = true;
                }

                if (checkpointSatisfied) {
                    checkpointsReached[agentIndex]++;
                    checkpointCompleteEvents[agentIndex] = true;
                    advanceRaceCheckpoint(agentIndex);
                    resetCheckpointDeadline(agentIndex);
                    raceTargetAdvanced = true;
                }
            }

            if (raceTargetAdvanced && !hasCompletedTrainingRace()) {
                activateRaceCheckpointTarget(0);
                updateGameCheckpointDeadlineTimer();
            }
        }

        private void computeRewards() {
            for (int agentIndex = 0; agentIndex < getControlledAgentCount(); agentIndex++) {
                clearRewardBreakdown(agentIndex);
                RlAgentSnapshot before = beforeSnapshots[agentIndex];
                RlAgentSnapshot after = afterSnapshots[agentIndex];
                float reward = 0f;

                if (before.active) {
                    if (!after.active) {
                        eliminations[agentIndex]++;
                        reward += recordReward(
                                agentIndex,
                                RL_REWARD_ELIMINATION,
                                -RL_ELIMINATION_PENALTY);
                    } else {
                        reward += recordReward(agentIndex, RL_REWARD_STEP_COST, -RL_STEP_PENALTY);
                        reward += recordReward(
                                agentIndex,
                                RL_REWARD_STEERING,
                                -getSteeringPenalty(agentIndex));
                        if (after.offRoad) {
                            reward += recordReward(
                                    agentIndex,
                                    RL_REWARD_OFF_ROAD,
                                    -getOffRoadPenalty(after));
                        }
                        reward += recordReward(
                                agentIndex,
                                RL_REWARD_PROGRESS,
                                getProgressReward(agentIndex, after));
                        reward += recordReward(
                                agentIndex,
                                RL_REWARD_REVERSE_SPEED,
                                -getReverseSpeedPenalty(after));
                        reward += recordReward(
                                agentIndex,
                                RL_REWARD_CAR_PUSH,
                                -getCarPushPenalty(before, after));
                        if (checkpointCompleteEvents[agentIndex]) {
                            reward += recordReward(
                                    agentIndex,
                                    RL_REWARD_CHECKPOINT,
                                    RL_CHECKPOINT_REWARD);
                        }
                    }
                }

                rewards[agentIndex] = reward;
                dones[agentIndex] = episodeDone || !after.active;
            }
        }

        private void clearRewardBreakdown(int agentIndex) {
            int offset = agentIndex * RL_REWARD_BREAKDOWN_SIZE;
            for (int i = 0; i < RL_REWARD_BREAKDOWN_SIZE; i++) {
                rewardBreakdown[offset + i] = 0f;
            }
        }

        private float recordReward(int agentIndex, int bucket, float reward) {
            rewardBreakdown[agentIndex * RL_REWARD_BREAKDOWN_SIZE + bucket] += reward;
            return reward;
        }

        private float getOffRoadPenalty(RlAgentSnapshot snapshot) {
            return Math.min(
                    RL_OFF_ROAD_MAX_PENALTY,
                    RL_OFF_ROAD_PENALTY
                            + snapshot.offRoadDistance * RL_OFF_ROAD_DISTANCE_PENALTY);
        }

        private float getProgressReward(int agentIndex, RlAgentSnapshot after) {
            if (after.signedForwardSpeed < RACE_CHECKPOINT_MIN_FORWARD_CROSS_SPEED) {
                return 0f;
            }
            float progress = MathUtils.clamp(progressTowardCheckpoint[agentIndex], 0f, 1.50f);
            return progress * RL_PROGRESS_REWARD;
        }

        private float getReverseSpeedPenalty(RlAgentSnapshot snapshot) {
            float reverseSpeed =
                    Math.max(0f, -snapshot.signedForwardSpeed - RL_REVERSE_SPEED_FREE_EPSILON);
            return Math.min(
                    RL_REVERSE_SPEED_MAX_PENALTY,
                    reverseSpeed * RL_REVERSE_SPEED_PENALTY_PER_UNIT);
        }

        private float getSteeringPenalty(int agentIndex) {
            float turn = MathUtils.clamp(currentActionTurn[agentIndex], -1f, 1f);
            return turn * turn * RL_STEERING_PENALTY;
        }

        private float getCarPushPenalty(RlAgentSnapshot before, RlAgentSnapshot after) {
            int hits = Math.max(0, after.carHitCount - before.carHitCount);
            return Math.min(RL_CAR_PUSH_MAX_STEP_PENALTY, hits * RL_CAR_PUSH_PENALTY);
        }

        private void buildObservations() {
            for (int i = 0; i < observations.length; i++) {
                observations[i] = 0f;
            }
            if (game.currentMap == null) {
                return;
            }

            float positionNormalizer =
                    getRlPositionNormalizer(
                            game.currentMap,
                            observationBounds,
                            observationFocus);

            for (int agentIndex = 0; agentIndex < getControlledAgentCount(); agentIndex++) {
                float targetTimeRemaining =
                        MathUtils.clamp(
                                checkpointDeadlineTimers[agentIndex]
                                        / Math.max(0.001f, checkpointDeadlineDurations[agentIndex]),
                                0f,
                                1f);
                boolean targetActive = game.checkpointTargetActive;
                Vector2 targetPosition = game.checkpointTargetPosition;
                float checkpointRadius = game.checkpointTargetRadius;
                Vector2 secondCheckpoint = null;
                if (config.raceMode && getRaceCheckpointTarget(agentIndex, observationTargetPosition)) {
                    targetActive = true;
                    targetPosition = observationTargetPosition;
                    checkpointRadius = getRaceTargetRadius();
                }
                if (config.raceMode && getSecondRaceCheckpointTarget(agentIndex, observationSecondCheckpoint)) {
                    secondCheckpoint = observationSecondCheckpoint;
                }
                fillRlObservation(
                        observations,
                        agentIndex * RL_OBSERVATION_SIZE,
                        getControlledCar(agentIndex),
                        game.currentMap,
                        targetActive,
                        targetPosition,
                        secondCheckpoint,
                        checkpointRadius,
                        0f,
                        targetTimeRemaining,
                        currentActionThrottle[agentIndex],
                        currentActionTurn[agentIndex],
                        positionNormalizer,
                        observationForward,
                        observationRecovery,
                        observationRouteTarget,
                        observationRouteLookahead,
                        observationSide,
                        observationRays,
                        observationCarRays,
                        game.cars);
            }
        }

        private boolean hasActiveControlledAgent() {
            for (int i = 0; i < getControlledAgentCount(); i++) {
                Car car = getControlledCar(i);
                if (car != null && car.active) {
                    return true;
                }
            }
            return false;
        }

        private int getWinnerAgentIndex() {
            int bestAgentIndex = -1;
            int bestCheckpointsReached = 0;
            for (int i = 0; i < getControlledAgentCount(); i++) {
                if (checkpointsReached[i] > bestCheckpointsReached) {
                    bestCheckpointsReached = checkpointsReached[i];
                    bestAgentIndex = i;
                }
            }
            return bestCheckpointsReached >= getMaxCheckpoints() ? bestAgentIndex : -1;
        }

        private boolean hasCompletedTrainingRace() {
            for (int i = 0; i < getControlledAgentCount(); i++) {
                if (checkpointsReached[i] >= getMaxCheckpoints()) {
                    return true;
                }
            }
            return false;
        }

        private String getWinnerLabel() {
            int winnerAgentIndex = getWinnerAgentIndex();
            if (winnerAgentIndex < 0 || winnerAgentIndex >= controlledVehicleIds.size) {
                return "";
            }
            int winnerVehicleId = controlledVehicleIds.get(winnerAgentIndex).intValue();
            return winnerVehicleId >= 0 && winnerVehicleId < game.roster.size
                    ? game.roster.get(winnerVehicleId).statsLabel
                    : "";
        }

        private Car getControlledCar(int agentIndex) {
            if (agentIndex < 0 || agentIndex >= controlledVehicleIds.size) {
                return null;
            }
            int vehicleId = controlledVehicleIds.get(agentIndex).intValue();
            if (vehicleId < 0 || vehicleId >= game.roster.size) {
                return null;
            }
            return game.roster.get(vehicleId).currentCar;
        }

        private void clearRewards() {
            for (int i = 0; i < rewards.length; i++) {
                rewards[i] = 0f;
                dones[i] = false;
                currentActionThrottle[i] = 0f;
                previousActionThrottle[i] = 0f;
                currentActionTurn[i] = 0f;
                previousActionTurn[i] = 0f;
            }
            clearStepEvents();
            for (int i = 0; i < rewardBreakdown.length; i++) {
                rewardBreakdown[i] = 0f;
            }
        }

        private void clearEpisodeMetrics() {
            for (int i = 0; i < getControlledAgentCount(); i++) {
                Car car = getControlledCar(i);
                raceNextCheckpointIndices[i] =
                        config.raceMode
                                        && car != null
                                        && car.template != null
                                        && game.currentMap != null
                                        && game.currentMap.getCheckpointCount() > 1
                                ? car.template.roundRaceNextCheckpointIndex
                                : 0;
                float deadline = getCheckpointDeadlineSeconds(i);
                checkpointDeadlineTimers[i] = deadline;
                checkpointDeadlineDurations[i] = deadline;
                progressTowardCheckpoint[i] = 0f;
                raceTargetSequences[i] = 0;
                resetRaceRouteProgressBaseline(i);
                checkpointsReached[i] = 0;
                eliminations[i] = 0;
                checkpointCrossRewardSequences[i] = -1;
                checkpointCrossRewardEvents[i] = false;
                checkpointCompleteEvents[i] = false;
                checkpointTimeoutEvents[i] = false;
                raceCheckpointCrossEvents[i] = false;
            }
        }

        private RlStepResult createResult() {
            float[] observationCopy = new float[observations.length];
            float[] rewardCopy = new float[rewards.length];
            float[] rewardBreakdownCopy = new float[rewardBreakdown.length];
            float[] effectiveActionCopy = new float[effectiveActions.length];
            boolean[] doneCopy = new boolean[dones.length];
            int[] checkpointsReachedCopy = new int[checkpointsReached.length];
            int[] eliminationsCopy = new int[eliminations.length];
            float[] progressTowardCheckpointCopy = new float[progressTowardCheckpoint.length];
            float[] debugTraceCopy =
                    config.debugTraceEnabled
                            ? new float[getControlledAgentCount() * RL_DEBUG_TRACE_SIZE]
                            : new float[0];
            buildEffectiveActions();
            System.arraycopy(observations, 0, observationCopy, 0, observations.length);
            System.arraycopy(rewards, 0, rewardCopy, 0, rewards.length);
            System.arraycopy(
                    rewardBreakdown,
                    0,
                    rewardBreakdownCopy,
                    0,
                    rewardBreakdown.length);
            System.arraycopy(effectiveActions, 0, effectiveActionCopy, 0, effectiveActions.length);
            System.arraycopy(dones, 0, doneCopy, 0, dones.length);
            System.arraycopy(checkpointsReached, 0, checkpointsReachedCopy, 0, checkpointsReached.length);
            System.arraycopy(eliminations, 0, eliminationsCopy, 0, eliminations.length);
            System.arraycopy(
                    progressTowardCheckpoint,
                    0,
                    progressTowardCheckpointCopy,
                    0,
                    progressTowardCheckpoint.length);
            if (config.debugTraceEnabled) {
                buildDebugTrace(debugTraceCopy);
            }
            return new RlStepResult(
                    observationCopy,
                    rewardCopy,
                    rewardBreakdownCopy,
                    RL_REWARD_BREAKDOWN_NAMES,
                    effectiveActionCopy,
                    doneCopy,
                    episodeDone,
                    actionStep,
                    getWinnerAgentIndex(),
                    getWinnerLabel(),
                    getCurrentMapId(),
                    getCurrentMapName(),
                    checkpointsReachedCopy,
                    eliminationsCopy,
                    progressTowardCheckpointCopy,
                    debugTraceCopy,
                    config.debugTraceEnabled ? RL_DEBUG_TRACE_NAMES : new String[0]);
        }

        private void buildDebugTrace(float[] out) {
            if (out.length < getControlledAgentCount() * RL_DEBUG_TRACE_SIZE
                    || game.currentMap == null) {
                return;
            }

            Vector2 forward = new Vector2();
            Vector2 side = new Vector2();
            Vector2 routeTarget = new Vector2();
            Vector2 checkpointPosition = new Vector2();
            for (int agentIndex = 0; agentIndex < getControlledAgentCount(); agentIndex++) {
                int offset = agentIndex * RL_DEBUG_TRACE_SIZE;
                out[offset + 34] =
                        agentIndex < checkpointDeadlineTimers.length
                                ? checkpointDeadlineTimers[agentIndex]
                                : 0f;
                out[offset + 35] =
                        agentIndex < checkpointDeadlineDurations.length
                                ? checkpointDeadlineDurations[agentIndex]
                                : 0f;
                out[offset + 36] =
                        agentIndex < checkpointsReached.length ? checkpointsReached[agentIndex] : 0f;
                out[offset + 37] = getMaxCheckpoints();
                out[offset + 38] =
                        agentIndex < checkpointTimeoutEvents.length
                                        && checkpointTimeoutEvents[agentIndex]
                                ? 1f
                                : 0f;
                out[offset + 39] = episodeDone ? 1f : 0f;
                Car car = getControlledCar(agentIndex);
                if (car == null || !car.active || car.body == null) {
                    continue;
                }

                Vector2 position = car.body.getPosition();
                Vector2 velocity = car.body.getLinearVelocity();
                forward.set(car.body.getWorldVector(forward.set(0f, 1f)));
                side.set(-forward.y, forward.x);
                SpawnPoint checkpoint = getRaceCheckpoint(agentIndex);
                int checkpointIndex = getRaceCheckpointIndex(agentIndex);
                int secondCheckpointIndex = getSecondRaceCheckpointIndex(agentIndex);
                float targetRouteDistance = 0f;
                float routeClearance = 1f;
                if (checkpoint != null) {
                    checkpointPosition.set(checkpoint.x, checkpoint.y);
                    targetRouteDistance =
                            Math.max(
                                    0f,
                                    game.currentMap.estimateDriveDistance(
                                                    position,
                                                    checkpointPosition,
                                                    RL_ROUTE_MARGIN)
                                            - getRaceTargetRadius());
                    game.currentMap.findDriveTarget(
                            position,
                            checkpointPosition,
                            RL_ROUTE_MARGIN,
                            routeTarget);
                    float routeDx = routeTarget.x - position.x;
                    float routeDy = routeTarget.y - position.y;
                    float routeDistance = (float) Math.sqrt(routeDx * routeDx + routeDy * routeDy);
                    if (routeDistance > 0.0001f) {
                        routeClearance =
                                sampleRlRayClearance(
                                        game.currentMap,
                                        position,
                                        routeDx / routeDistance,
                                        routeDy / routeDistance,
                                        routeDistance);
                    }
                } else {
                    checkpointPosition.setZero();
                    routeTarget.set(position);
                }

                SpawnPoint secondCheckpoint =
                        secondCheckpointIndex >= 0
                                && game.currentMap.getCheckpointCount() > 0
                                ? game.currentMap.getCheckpoint(secondCheckpointIndex)
                                : null;
                out[offset] = 1f;
                out[offset + 1] = position.x;
                out[offset + 2] = position.y;
                out[offset + 3] = car.body.getAngle();
                out[offset + 4] = velocity.len();
                out[offset + 5] = forward.dot(velocity);
                out[offset + 6] = side.dot(velocity);
                out[offset + 7] = checkpointIndex;
                out[offset + 8] = raceTargetSequences[agentIndex];
                out[offset + 9] = checkpoint == null ? 0f : checkpoint.x;
                out[offset + 10] = checkpoint == null ? 0f : checkpoint.y;
                out[offset + 11] = checkpoint != null && checkpoint.hasGate ? 1f : 0f;
                out[offset + 12] = checkpoint == null ? 0f : checkpoint.gateStartX;
                out[offset + 13] = checkpoint == null ? 0f : checkpoint.gateStartY;
                out[offset + 14] = checkpoint == null ? 0f : checkpoint.gateEndX;
                out[offset + 15] = checkpoint == null ? 0f : checkpoint.gateEndY;
                out[offset + 16] =
                        checkpoint != null && game.currentMap.supports(checkpoint.x, checkpoint.y)
                                ? 1f
                                : 0f;
                out[offset + 17] =
                        checkpoint == null
                                ? 0f
                                : game.currentMap.approximateDistanceToHazard(checkpoint.x, checkpoint.y);
                out[offset + 18] = targetRouteDistance;
                out[offset + 19] = progressTowardCheckpoint[agentIndex];
                out[offset + 20] = routeTarget.x;
                out[offset + 21] = routeTarget.y;
                out[offset + 22] = game.currentMap.supports(routeTarget) ? 1f : 0f;
                out[offset + 23] = game.currentMap.approximateDistanceToHazard(routeTarget);
                out[offset + 24] = routeClearance;
                out[offset + 25] = secondCheckpointIndex;
                out[offset + 26] = secondCheckpoint == null ? 0f : secondCheckpoint.x;
                out[offset + 27] = secondCheckpoint == null ? 0f : secondCheckpoint.y;
                out[offset + 28] =
                        secondCheckpoint != null
                                        && game.currentMap.supports(
                                                secondCheckpoint.x,
                                                secondCheckpoint.y)
                                ? 1f
                                : 0f;
                out[offset + 29] = game.currentMap.supports(position) ? 0f : 1f;
                out[offset + 30] = game.currentMap.distanceToSafety(position);
                out[offset + 31] = game.currentMap.approximateDistanceToHazard(position);
                int actionOffset = agentIndex * RL_ACTION_SIZE;
                out[offset + 32] =
                        actionOffset < effectiveActions.length ? effectiveActions[actionOffset] : 0f;
                out[offset + 33] =
                        actionOffset + 1 < effectiveActions.length
                                ? effectiveActions[actionOffset + 1]
                                : 0f;
                out[offset + 34] =
                        agentIndex < checkpointDeadlineTimers.length
                                ? checkpointDeadlineTimers[agentIndex]
                                : 0f;
                out[offset + 35] =
                        agentIndex < checkpointDeadlineDurations.length
                                ? checkpointDeadlineDurations[agentIndex]
                                : 0f;
                out[offset + 36] =
                        agentIndex < checkpointsReached.length ? checkpointsReached[agentIndex] : 0f;
                out[offset + 37] = getMaxCheckpoints();
                out[offset + 38] =
                        agentIndex < checkpointTimeoutEvents.length
                                        && checkpointTimeoutEvents[agentIndex]
                                ? 1f
                                : 0f;
                out[offset + 39] = episodeDone ? 1f : 0f;
            }
        }

        private String getCurrentMapId() {
            return game.currentMap == null ? "" : game.currentMap.getId();
        }

        private String getCurrentMapName() {
            return game.currentMap == null ? "" : game.currentMap.getName();
        }

        private void buildEffectiveActions() {
            for (int i = 0; i < effectiveActions.length; i++) {
                effectiveActions[i] = 0f;
            }
            for (int agentIndex = 0; agentIndex < getControlledAgentCount(); agentIndex++) {
                Car car = getControlledCar(agentIndex);
                if (car == null || !car.active) {
                    continue;
                }
                int actionOffset = agentIndex * RL_ACTION_SIZE;
                effectiveActions[actionOffset] = car.externalControlDecision.throttle;
                effectiveActions[actionOffset + 1] = car.externalControlDecision.turn;
            }
        }

        private void ensureOpen() {
            if (closed) {
                throw new IllegalStateException("RL training environment is already closed.");
            }
        }
    }

    private static final class RlAgentSnapshot {
        private boolean active;
        private int checkpointTargetSequence;
        private boolean checkpointTargetActive;
        private boolean checkpointTargetInside;
        private boolean wallContact;
        private boolean offRoad;
        private float offRoadDistance;
        private float edgeDistance;
        private float checkpointRouteDistance;
        private float checkpointTargetRadius;
        private float raceRouteProgress;
        private float routeForwardSpeed;
        private float speed;
        private float signedForwardSpeed;
        private float nearestCarDistance;
        private int carHitCount;
        private float effectiveThrottle;
        private float recoverySpeed;
        private float angularSpeed;

        private void clear() {
            active = false;
            checkpointTargetSequence = 0;
            checkpointTargetActive = false;
            checkpointTargetInside = false;
            wallContact = false;
            offRoad = false;
            offRoadDistance = 0f;
            edgeDistance = 0f;
            checkpointRouteDistance = 0f;
            checkpointTargetRadius = 0f;
            raceRouteProgress = 0f;
            routeForwardSpeed = 0f;
            speed = 0f;
            signedForwardSpeed = 0f;
            nearestCarDistance = Float.MAX_VALUE;
            carHitCount = 0;
            effectiveThrottle = 0f;
            recoverySpeed = 0f;
            angularSpeed = 0f;
        }
    }

    private static final class CarTemplate {
        private final int vehicleId;
        private final String name;
        private final boolean playerControlled;
        private final Color color;
        private final CarVisual visual;
        private final String statsLabel;
        private final boolean externallyControlled;
        private final boolean modelControlled;
        private CarPhysics physics;
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
        private int roundRaceLap;
        private int roundRaceStartCheckpointIndex;
        private int roundRaceNextCheckpointIndex;
        private int roundRaceCheckpointsCompleted;
        private float roundRaceFinishTime;
        private float roundRaceCurrentLapStartTime;
        private float roundRaceBestLapTime;
        private boolean roundRaceLapCounterCrossed;
        private boolean roundRaceFinished;
        private final FloatArray roundRaceLapTimes = new FloatArray();
        private Car currentCar;

        private CarTemplate(
                int vehicleId,
                String name,
                boolean playerControlled,
                Color color,
                CarVisual visual,
                String statsLabel,
                boolean externallyControlled,
                boolean modelControlled,
                CarPhysics physics) {
            this.vehicleId = vehicleId;
            this.name = name;
            this.playerControlled = playerControlled;
            this.color = color;
            this.visual = visual;
            this.statsLabel = statsLabel == null || statsLabel.length() == 0 ? name : statsLabel;
            this.externallyControlled = externallyControlled;
            this.modelControlled = modelControlled;
            this.physics = physics == null ? CarPhysics.DEFAULT : physics;
        }
    }

    private static final class DestructionEffect {
        private final Vector2 position = new Vector2();
        private final Color color = new Color();
        private float timer;
        private float rotationDeg;
        private float scale;
    }

    private enum GameMode {
        MAIN_MENU,
        OPTIONS_MENU,
        PAUSE_MENU,
        PLAYING
    }

    private enum MapDecorStyle {
        GRID,
        RUNWAY,
        ORBIT,
        CROSS,
        DIAGONAL,
        FORTRESS
    }

    private static final class ThemeChoice {
        private final String name;
        private final String displayName;

        private ThemeChoice(String name, String displayName) {
            this.name = name;
            this.displayName = displayName;
        }
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

    private static final class CarVisual {
        private final String spritePath;
        private final Color color;
        private final int spriteSourceX;
        private final int spriteSourceY;
        private final int spriteSourceWidth;
        private final int spriteSourceHeight;
        private final boolean sharedTexture;
        private final float spriteRotationOffsetDeg;

        private CarVisual(String spritePath, Color color) {
            this(
                    spritePath,
                    0,
                    0,
                    0,
                    0,
                    false,
                    CAR_SPRITE_ROTATION_OFFSET_DEG,
                    color);
        }

        private CarVisual(
                String spritePath,
                int spriteSourceX,
                int spriteSourceY,
                int spriteSourceWidth,
                int spriteSourceHeight,
                boolean sharedTexture,
                float spriteRotationOffsetDeg,
                Color color) {
            this.spritePath = spritePath;
            this.color = color;
            this.spriteSourceX = spriteSourceX;
            this.spriteSourceY = spriteSourceY;
            this.spriteSourceWidth = spriteSourceWidth;
            this.spriteSourceHeight = spriteSourceHeight;
            this.sharedTexture = sharedTexture;
            this.spriteRotationOffsetDeg = spriteRotationOffsetDeg;
        }

        private boolean hasSourceRegion() {
            return spriteSourceWidth > 0 && spriteSourceHeight > 0;
        }
    }
}
