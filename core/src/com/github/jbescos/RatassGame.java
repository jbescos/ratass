package com.github.jbescos;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
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
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.github.jbescos.ai.AiControlDecision;
import com.github.jbescos.ai.AiDrivingPersonality;
import com.github.jbescos.ai.AiDrivingPersonalities;
import com.github.jbescos.ai.AiVehicleView;
import com.github.jbescos.ai.CarAiController;
import com.github.jbescos.ai.rl.RlPolicy;
import com.github.jbescos.gameplay.ArenaMap;
import com.github.jbescos.gameplay.MapProgression;
import com.github.jbescos.gameplay.SpawnPoint;
import com.github.jbescos.gameplay.maps.ArenaMaps;
import java.util.Comparator;
import java.util.LinkedHashMap;
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
    private static final int DEFAULT_CAR_COUNT = 20;
    private static final int DEFAULT_PLAYER_CAR_INDEX = 0;
    private static final int MIN_CAR_COUNT = 2;
    private static final int MAX_CAR_COUNT = 50;
    private static final int OPTIONS_THEME_SELECTION = 0;
    private static final int OPTIONS_CARS_SELECTION = 1;
    private static final int OPTIONS_PLAYER_CAR_SELECTION = 2;
    private static final int OPTIONS_CAMERA_SELECTION = 3;
    private static final int OPTIONS_ZOOM_SELECTION = 4;
    private static final int OPTIONS_BACK_SELECTION = 5;
    private static final int MAIN_MENU_NEW_GAME_SELECTION = 0;
    private static final int MAIN_MENU_OPTIONS_SELECTION = 1;
    private static final int MAIN_MENU_EXIT_SELECTION = 2;
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
    private static final float SAFE_ZONE_DURATION = 10f;
    private static final float SAFE_ZONE_INITIAL_RADIUS_RATIO = 0.17f;
    private static final float SAFE_ZONE_RADIUS_DECAY = 0.86f;
    private static final float SAFE_ZONE_MIN_RADIUS = 0.58f;
    private static final float SAFE_ZONE_SPAWN_MARGIN = 0.65f;
    private static final float SAFE_ZONE_MIN_HAZARD_DISTANCE = 1.15f;
    private static final float SAFE_ZONE_MIN_MOVE_DISTANCE = 4.2f;
    private static final float SAFE_ZONE_RENDER_SAMPLE_STEP = 0.18f;
    private static final float SAFE_ZONE_MINIMAP_SAMPLE_STEP = 0.26f;
    private static final int SAFE_ZONE_SPAWN_ATTEMPTS = 128;
    private static final float CAMERA_HORIZONTAL_PADDING = 4f;
    private static final float CAMERA_VERTICAL_PADDING = 3f;
    private static final float MIN_WORLD_CAMERA_ZOOM = 0.90f;
    private static final float PLAYER_CAMERA_ZOOM = 1.04f;
    private static final float PLAYER_CAMERA_SPEED_ZOOM_OUT = 0.46f;
    private static final float PLAYER_CAMERA_GROWTH_ZOOM_OUT = 0.14f;
    private static final float PLAYER_CAMERA_MAX_ZOOM = 1.58f;
    private static final float DEFAULT_CAMERA_ZOOM = 1.00f;
    private static final float MIN_CAMERA_ZOOM = 0.70f;
    private static final float MAX_CAMERA_ZOOM = 1.50f;
    private static final float CAMERA_ZOOM_STEP = 0.10f;
    private static final float DEFAULT_MAP_SCALE = 4.00f;
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
    public static final int RL_OBSERVATION_SIZE = 39;
    public static final int RL_ACTION_SIZE = 2;
    public static final int RL_REWARD_BREAKDOWN_SIZE = 8;
    private static final int RL_REWARD_SURVIVAL = 0;
    private static final int RL_REWARD_CIRCLE = 1;
    private static final int RL_REWARD_EDGE = 2;
    private static final int RL_REWARD_ATTACK = 3;
    private static final int RL_REWARD_DRIVING = 4;
    private static final int RL_REWARD_PICKUP = 5;
    private static final int RL_REWARD_CONTROL = 6;
    private static final int RL_REWARD_WIN = 7;
    private static final String[] RL_REWARD_BREAKDOWN_NAMES = {
            "survival",
            "circle",
            "edge",
            "attack",
            "driving",
            "pickup",
            "control",
            "win"
    };
    private static final int RL_DEFAULT_CONTROLLED_AGENTS = 1;
    private static final int RL_DEFAULT_FIELD_SIZE = 12;
    private static final int RL_DEFAULT_ACTION_REPEAT = 4;
    private static final int RL_DEFAULT_MAX_ACTION_STEPS = 1350;
    private static final float RL_LIVE_DECISION_INTERVAL = 0.08f;
    private static final float RL_INITIAL_DECISION_STAGGER = 0.24f;
    private static final float RL_POSITION_NORMALIZER_MIN = 1f;
    private static final float RL_VELOCITY_NORMALIZER = 18f;
    private static final float RL_ANGULAR_VELOCITY_NORMALIZER = 8f;
    private static final float RL_HAZARD_DISTANCE_NORMALIZER = 6f;
    private static final float RL_ROUTE_MARGIN = 0.52f;
    private static final float RL_ROUTE_DIRECT_EPSILON = 0.16f;
    private static final float RL_RAYCAST_DISTANCE = 7.5f;
    private static final float RL_RAYCAST_STEP = 0.34f;
    private static final float RL_ALIVE_STEP_REWARD = 0.004f;
    private static final float RL_EDGE_RECOVERY_REWARD = 0.145f;
    private static final float RL_OPPONENT_PRESSURE_REWARD = 0.110f;
    private static final float RL_OPPONENT_ELIMINATION_REWARD = 1.250f;
    private static final float RL_IMPACT_CREDIT_REWARD = 0.160f;
    private static final float RL_WIN_REWARD = 2.000f;
    private static final float RL_ELIMINATION_PENALTY = 7.200f;
    private static final float RL_AVOIDABLE_ELIMINATION_PENALTY = 3.200f;
    private static final float RL_OUTWARD_ELIMINATION_PENALTY = 2.700f;
    private static final float RL_SPIN_STALL_PENALTY = 0.006f;
    private static final float RL_CONTROL_DEADZONE = 0.06f;
    private static final float RL_ACTION_FLIP_DEADZONE = 0.18f;
    private static final float RL_FORWARD_SPEED_REWARD = 0.017f;
    private static final float RL_REVERSE_SPEED_PENALTY = 0.012f;
    private static final float RL_RAW_THROTTLE_FLIP_PENALTY = 0.026f;
    private static final float RL_EFFECTIVE_THROTTLE_FLIP_PENALTY = 0.062f;
    private static final float RL_FORWARD_REVERSE_SPEED_FLIP_PENALTY = 0.034f;
    private static final float RL_IDLE_DITHER_PENALTY = 0.040f;
    private static final float RL_OPPONENT_CLOSING_REWARD = 0.060f;
    private static final float RL_FORWARD_THROTTLE_COMMIT_REWARD = 0.028f;
    private static final float RL_SAFE_REVERSE_ACTION_PENALTY = 0.030f;
    private static final float RL_SAFE_REVERSE_BRAKE_PENALTY = 0.018f;
    private static final float RL_REVERSE_RECOVERY_EDGE_DISTANCE = 1.15f;
    private static final float RL_REVERSE_RECOVERY_SPEED = 0.12f;
    private static final float RL_EDGE_DANGER_DISTANCE = 2.05f;
    private static final float RL_EDGE_DANGER_PENALTY = 0.165f;
    private static final float RL_EDGE_APPROACH_PENALTY = 0.235f;
    private static final float RL_EDGE_WARNING_DISTANCE = 3.60f;
    private static final float RL_EDGE_WARNING_SPEED = 2.35f;
    private static final float RL_EDGE_UNSAFE_SPEED_PENALTY = 0.185f;
    private static final float RL_SAFE_SPEED_DISTANCE = 3.80f;
    private static final float RL_SAFE_SPEED_REWARD = 0.018f;
    private static final float RL_ACCELERATION_REWARD = 0.014f;
    private static final float RL_EDGE_RECOVERY_SPEED_DISTANCE = 4.20f;
    private static final float RL_EDGE_RECOVERY_SPEED_REWARD = 0.210f;
    private static final float RL_EDGE_UNSAFE_RECOVERY_SPEED_PENALTY = 0.315f;
    private static final float RL_EDGE_FAST_APPROACH_PENALTY = 0.300f;
    private static final float RL_CONTACT_STUCK_DISTANCE = 1.18f;
    private static final float RL_CONTACT_STUCK_SPEED = 0.62f;
    private static final float RL_CONTACT_STUCK_PENALTY = 0.040f;
    private static final float RL_CONTACT_DISENGAGE_REWARD = 0.048f;
    private static final float RL_CONTACT_ESCAPE_SPEED_REWARD = 0.055f;
    private static final float RL_SAFE_ATTACK_DISTANCE = 3.15f;
    private static final float RL_FAST_IMPACT_REWARD = 0.220f;
    private static final float RL_ATTACK_EDGE_PRESSURE_REWARD = 0.170f;
    private static final float RL_UNSAFE_ATTACK_PENALTY = 0.260f;
    private static final float RL_ATTACK_SUICIDE_PENALTY = 4.200f;
    private static final float RL_GROWTH_PICKUP_REWARD = 5.000f;
    private static final float RL_GROWTH_PICKUP_SAFE_ZONE_REWARD = 4.000f;
    private static final float RL_GROWTH_PICKUP_APPROACH_REWARD = 0.200f;
    private static final float RL_SAFE_ZONE_APPROACH_REWARD = 0.560f;
    private static final float RL_SAFE_ZONE_INSIDE_REWARD = 0.145f;
    private static final float RL_SAFE_ZONE_CENTER_REWARD = 0.320f;
    private static final float RL_SAFE_ZONE_DEEP_INSIDE_REWARD = 0.340f;
    private static final float RL_SAFE_ZONE_RIM_PENALTY = 0.360f;
    private static final float RL_SAFE_ZONE_SETTLE_REWARD = 0.130f;
    private static final float RL_SAFE_ZONE_BRAKE_REWARD = 0.110f;
    private static final float RL_SAFE_ZONE_FAST_EXIT_PENALTY = 0.260f;
    private static final float RL_SAFE_ZONE_OUTWARD_PENALTY = 0.220f;
    private static final float RL_SAFE_ZONE_EXIT_PENALTY = 0.650f;
    private static final float RL_SAFE_ZONE_BRAKE_SPEED = 0.85f;
    private static final float RL_SAFE_ZONE_DEADLINE_REWARD = 1.850f;
    private static final float RL_SAFE_ZONE_DEADLINE_CENTER_REWARD = 1.600f;
    private static final float RL_SAFE_ZONE_PUSH_OUT_REWARD = 0.850f;
    private static final float RL_SAFE_ZONE_EJECTION_REWARD = 1.350f;
    private static final float RL_SAFE_ZONE_MISS_PENALTY = 7.600f;
    private static final float RL_SAFE_ZONE_URGENCY_PENALTY = 0.140f;
    private static final float RL_NAVIGATION_CIRCLE_REWARD_SCALE = 1.65f;
    private static final float RL_NAVIGATION_EDGE_PENALTY_SCALE = 1.35f;
    private static final float RL_NAVIGATION_ALIVE_STEP_REWARD = 0.004f;
    private static final float RL_NAVIGATION_COMPLETE_REWARD = 5.000f;
    private static final float RL_NAVIGATION_ROUTE_SPEED_REWARD = 0.280f;
    private static final float RL_NAVIGATION_ROUTE_BACKTRACK_PENALTY = 0.180f;
    private static final float RL_NAVIGATION_ROUTE_STALL_PENALTY = 0.160f;
    private static final float RL_NAVIGATION_EDGE_SPEED_PENALTY = 0.420f;
    private static final float RL_NAVIGATION_EDGE_THROTTLE_PENALTY = 0.180f;
    private static final float RL_NAVIGATION_EDGE_TURN_PENALTY = 0.080f;
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
    private final Array<DestructionEffect> destructionEffects = new Array<DestructionEffect>();
    private final Array<CarVisual> themeCarVisuals = new Array<CarVisual>();
    private final Array<Rectangle> menuCarSheetSourceBounds = new Array<Rectangle>();
    private final Array<ThemeChoice> themeChoices = new Array<ThemeChoice>();
    private final Array<String> themeEnemyNames = new Array<String>();
    private final LinkedHashMap<String, Texture> arenaSurfaceTextureCache =
            new LinkedHashMap<String, Texture>();
    private final GlyphLayout glyphLayout = new GlyphLayout();
    private final Color tint = new Color();
    private final Rectangle mapBounds = new Rectangle();
    private final Vector2 focusPoint = new Vector2();
    private final Vector2 growthPickupPosition = new Vector2();
    private final Vector2 pointPickupPosition = new Vector2();
    private final Vector2 safeZonePosition = new Vector2();
    private final Vector2 pickupCandidate = new Vector2();
    private final Vector2 lastGrowthPickupPosition = new Vector2();
    private final Vector2 lastPointPickupPosition = new Vector2();
    private final Vector2 lastSafeZonePosition = new Vector2();
    private final Vector2 spawnCandidate = new Vector2();
    private final Array<SpawnPoint> roundSpawns = new Array<SpawnPoint>();
    private final Rectangle menuNewGameBounds = new Rectangle();
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
    private float safeZoneTimer;
    private float safeZoneRadius;
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
    private boolean safeZoneActive;
    private boolean hasLastGrowthPickupPosition;
    private boolean hasLastPointPickupPosition;
    private boolean hasLastSafeZonePosition;
    private boolean leaderboardDirty = true;
    private boolean roundOver;
    private boolean roundStartSoundPlayed;
    private boolean rlTrainingAllowSoloRound;
    private boolean rlTrainingDisablePickups;
    private boolean rlTrainingRandomSpawnLocations;
    private int countdownCueSecond;
    private int roundNumber;
    private int playerWins;
    private int finishPositionCounter;
    private int safeZoneWave;
    private int safeZoneSequence;
    private int selectedThemeIndex;
    private int selectedCarCount = DEFAULT_CAR_COUNT;
    private int selectedPlayerCarIndex = DEFAULT_PLAYER_CAR_INDEX;
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
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private void startNewGame() {
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
        mapProgression = new MapProgression(ArenaMaps.createDefaultSet(mapScale));

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
            if (policy.getObservationSize() != RL_OBSERVATION_SIZE
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
        }

        selectedThemeIndex = findThemeIndex(configuredThemeName);
        configuredThemeName = getCurrentTheme().name;
        cameraZoom = clampCameraZoom(cameraZoom);
        selectedCarCount = clampCarCount(selectedCarCount);
        selectedPlayerCarIndex = Math.max(0, selectedPlayerCarIndex);
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
        preferences.flush();
    }

    private void createRoster() {
        roster.clear();
        CarVisual playerVisual = getPlayerCarVisual();
        addRosterTemplate(
                "You",
                true,
                new Color(playerVisual.color),
                null,
                playerVisual,
                "player");

        int enemyCount = getConfiguredEnemyCount();
        boolean modelControlledEnemies = rlEnemyPolicy != null;
        for (int enemyIndex = 0; enemyIndex < enemyCount; enemyIndex++) {
            CarVisual visual = getEnemyCarVisual(enemyIndex);
            AiDrivingPersonality personality = ENEMY_PERSONALITIES[enemyIndex % ENEMY_PERSONALITIES.length];
            addRosterTemplate(
                    getEnemyName(enemyIndex),
                    false,
                    new Color(visual.color),
                    personality,
                    visual,
                    personality.id,
                    false,
                    modelControlledEnemies);
        }
        invalidateLeaderboard();
    }

    private int getConfiguredEnemyCount() {
        return Math.max(1, selectedCarCount - 1);
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
        if (carVisualCount > 1) {
            int playerCarIndex = clampPlayerCarIndex(selectedPlayerCarIndex, carVisualCount);
            int visualIndex = enemyIndex % (carVisualCount - 1);
            if (visualIndex >= playerCarIndex) {
                visualIndex++;
            }
            return getCarVisual(visualIndex);
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
        invalidateLeaderboard();
    }

    private void addRosterTemplate(
            String name,
            boolean playerControlled,
            Color color,
            AiDrivingPersonality personality,
            CarVisual visual,
            String statsLabel) {
        addRosterTemplate(
                name,
                playerControlled,
                color,
                personality,
                visual,
                statsLabel,
                false,
                false);
    }

    private void addRosterTemplate(
            String name,
            boolean playerControlled,
            Color color,
            AiDrivingPersonality personality,
            CarVisual visual,
            String statsLabel,
            boolean externallyControlled) {
        addRosterTemplate(
                name,
                playerControlled,
                color,
                personality,
                visual,
                statsLabel,
                externallyControlled,
                false);
    }

    private void addRosterTemplate(
            String name,
            boolean playerControlled,
            Color color,
            AiDrivingPersonality personality,
            CarVisual visual,
            String statsLabel,
            boolean externallyControlled,
            boolean modelControlled) {
        roster.add(new CarTemplate(
                roster.size,
                name,
                playerControlled,
                color,
                personality,
                visual,
                statsLabel,
                externallyControlled,
                modelControlled));
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

        int maxStepsPerRound =
                Math.max(1, MathUtils.ceil(SAFE_ZONE_DURATION / PHYSICS_STEP) * 56);
        for (int round = 0; round < config.rounds; round++) {
            int steps = 0;
            while (!roundOver && steps < maxStepsPerRound) {
                stepSimulation(PHYSICS_STEP);
                steps++;
            }
            if (!roundOver) {
                finishRound(null);
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
            int carVisualCount = Math.min(themeCarVisuals.size, MAX_CAR_COUNT);
            return themeCarVisuals.get(rosterIndex % carVisualCount);
        }
        return NORMAL_CAR_VISUALS[rosterIndex % NORMAL_CAR_VISUALS.length];
    }

    private CarVisual getPlayerCarVisual() {
        return getCarVisual(selectedPlayerCarIndex);
    }

    private int getAvailableCarVisualCount() {
        return themeCarVisuals.size > 0
                ? Math.min(themeCarVisuals.size, MAX_CAR_COUNT)
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
        for (int i = 0; i < MAX_CAR_COUNT; i++) {
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
        for (int i = 0; i < MAX_CAR_COUNT; i++) {
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
        if (Gdx.input.isKeyJustPressed(Input.Keys.O)) {
            openOptionsMenu(false);
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            if (mainMenuSelection == MAIN_MENU_NEW_GAME_SELECTION) {
                startNewGame();
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
        startNewGame();
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
                        MENU_MIN_SIDE_MARGIN + (menuButtonHeight + menuButtonGap) * 2f);

        menuNewGameBounds.set(buttonX, firstButtonY, buttonWidth, menuButtonHeight);
        menuOptionsBounds.set(
                buttonX,
                firstButtonY - menuButtonHeight - menuButtonGap,
                buttonWidth,
                menuButtonHeight);
        menuExitBounds.set(
                buttonX,
                firstButtonY - (menuButtonHeight + menuButtonGap) * 2f,
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
        optionsCameraBounds.set(
                rowX,
                rowY - (rowHeight + rowGap) * 3f,
                rowWidth,
                rowHeight);
        optionsZoomBounds.set(
                rowX,
                rowY - (rowHeight + rowGap) * 4f,
                rowWidth,
                rowHeight);
        optionsZoomOutBounds.set(
                rowX,
                rowY - (rowHeight + rowGap) * 4f,
                stepButtonWidth,
                rowHeight);
        optionsZoomInBounds.set(
                rowX + rowWidth - stepButtonWidth,
                rowY - (rowHeight + rowGap) * 4f,
                stepButtonWidth,
                rowHeight);
        optionsBackBounds.set(
                rowX,
                rowY - (rowHeight + rowGap) * 5f,
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
        drawMenuButton(menuOptionsBounds, "Options", mainMenuSelection == MAIN_MENU_OPTIONS_SELECTION);
        drawMenuButton(menuExitBounds, "Exit", mainMenuSelection == MAIN_MENU_EXIT_SELECTION);

        spriteBatch.begin();
        titleFont.setColor(0.98f, 0.92f, 0.76f, 1f);
        drawTextCentered(titleFont, "RATASS", hudWidth * 0.5f, hudHeight * 0.73f);

        hudFont.setColor(0.76f, 0.84f, 0.88f, 1f);
        drawTextCentered(hudFont, getCurrentTheme().displayName, hudWidth * 0.5f, hudHeight * 0.65f);
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
                    growthPickupPosition,
                    pointPickupActive,
                    pointPickupPosition,
                    safeZoneActive,
                    safeZonePosition,
                    safeZoneRadius,
                    safeZoneTimer,
                    rlEnemyPolicy);
        }
        if (!allowControl && !roundOver) {
            freezeCarsForCountdown();
        }

        for (int i = 0; i < cars.size; i++) {
            cars.get(i).capturePreviousTransform();
        }
        world.step(delta, VELOCITY_ITERATIONS, POSITION_ITERATIONS);
        checkForEliminations();

        if (!roundOver) {
            updateRoundState();
            if (!roundOver && allowControl) {
                updateRoundTimer(delta);
                if (!roundOver) {
                    updateSafeZone(delta);
                }
                if (!roundOver && !rlTrainingDisablePickups) {
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
        safeZoneActive = false;
        hasLastGrowthPickupPosition = false;
        hasLastPointPickupPosition = false;
        hasLastSafeZonePosition = false;
        clearEventCallout();
        growthBoostTimer = 0f;
        boostedCar = null;
        preRoundCountdownTimer = ROUND_START_COUNTDOWN;
        countdownCueSecond = MathUtils.ceil(ROUND_START_COUNTDOWN) + 1;
        roundStartSoundPlayed = false;
        roundTimer = 0f;
        safeZoneTimer = 0f;
        safeZoneRadius = 0f;
        impactSoundCooldown = 0f;
        destructionSoundCooldown = 0f;
        roundOver = false;
        roundOverTimer = 0f;
        winner = null;
        safeZoneWave = 0;
        safeZoneSequence = 0;
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
        warmArenaSurfaceTextures();
        invalidateLeaderboard();
        spawnSafeZone();
        if (!rlTrainingDisablePickups) {
            spawnGrowthPickup();
        }
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

    private void updateSafeZone(float delta) {
        if (!safeZoneActive) {
            spawnSafeZone();
        }
        if (!safeZoneActive) {
            return;
        }

        safeZoneTimer = Math.max(0f, safeZoneTimer - delta);
        if (safeZoneTimer > 0f) {
            return;
        }

        resolveSafeZone();
    }

    private void resolveSafeZone() {
        Array<Car> eliminatedByZone = new Array<Car>();
        int closingSequence = safeZoneSequence;
        for (int i = 0; i < cars.size; i++) {
            Car car = cars.get(i);
            if (!car.active || car.body == null) {
                continue;
            }
            if (isInsideSafeZone(car)) {
                car.safeZoneSurvivalSequence = closingSequence;
            } else {
                eliminatedByZone.add(car);
            }
        }

        safeZoneActive = false;
        for (int i = 0; i < eliminatedByZone.size; i++) {
            Car car = eliminatedByZone.get(i);
            car.eliminatedBySafeZone = true;
            eliminateCar(car);
        }

        int aliveCars = getAliveCarCount();
        if (aliveCars == 0 || (!rlTrainingAllowSoloRound && aliveCars <= 1)) {
            finishRound(findLastAliveCar());
            return;
        }

        spawnSafeZone();
    }

    private boolean isInsideSafeZone(Car car) {
        if (!safeZoneActive || car == null || car.body == null) {
            return false;
        }
        return getSafeZoneSignedMargin(car.body.getPosition()) >= 0f;
    }

    private float getSafeZoneSignedMargin(Vector2 position) {
        if (!safeZoneActive || position == null) {
            return -Float.MAX_VALUE;
        }
        return safeZoneRadius - position.dst(safeZonePosition);
    }

    private void spawnSafeZone() {
        if (currentMap == null) {
            safeZoneActive = false;
            return;
        }

        currentMap.getBounds(mapBounds);
        currentMap.getFocusPoint(focusPoint);
        float radius = computeSafeZoneRadius(safeZoneWave);

        for (int shrink = 0; shrink < 6; shrink++) {
            safeZoneRadius = radius;
            if (trySpawnSafeZone(true) || trySpawnSafeZone(false)) {
                activateSafeZone();
                return;
            }
            radius = Math.max(SAFE_ZONE_MIN_RADIUS, radius * 0.82f);
        }

        currentMap.findRecoveryPoint(focusPoint, safeZonePosition);
        if (!currentMap.supports(safeZonePosition)) {
            currentMap.getFocusPoint(safeZonePosition);
            currentMap.clampToPlayable(safeZonePosition, SAFE_ZONE_SPAWN_MARGIN);
        }
        safeZoneRadius = Math.max(SAFE_ZONE_MIN_RADIUS, radius);
        activateSafeZone();
    }

    private void activateSafeZone() {
        safeZoneTimer = SAFE_ZONE_DURATION;
        safeZoneActive = true;
        safeZoneWave++;
        safeZoneSequence++;
        lastSafeZonePosition.set(safeZonePosition);
        hasLastSafeZonePosition = true;
    }

    private float computeSafeZoneRadius(int waveIndex) {
        float mapSpan = Math.max(1f, Math.min(mapBounds.width, mapBounds.height));
        float initialRadius = Math.max(SAFE_ZONE_MIN_RADIUS, mapSpan * SAFE_ZONE_INITIAL_RADIUS_RATIO);
        return Math.max(
                SAFE_ZONE_MIN_RADIUS,
                initialRadius * (float) Math.pow(SAFE_ZONE_RADIUS_DECAY, Math.max(0, waveIndex)));
    }

    private boolean trySpawnSafeZone(boolean requireNewSpot) {
        float minX = mapBounds.x + SAFE_ZONE_SPAWN_MARGIN;
        float maxX = mapBounds.x + mapBounds.width - SAFE_ZONE_SPAWN_MARGIN;
        float minY = mapBounds.y + SAFE_ZONE_SPAWN_MARGIN;
        float maxY = mapBounds.y + mapBounds.height - SAFE_ZONE_SPAWN_MARGIN;

        if (minX >= maxX || minY >= maxY) {
            return false;
        }

        float requiredHazardDistance =
                Math.max(
                        SAFE_ZONE_MIN_HAZARD_DISTANCE,
                        Math.min(safeZoneRadius * 0.90f, 2.25f));
        float requiredMoveDistance =
                Math.max(SAFE_ZONE_MIN_MOVE_DISTANCE, safeZoneRadius * 1.05f);

        for (int attempt = 0; attempt < SAFE_ZONE_SPAWN_ATTEMPTS; attempt++) {
            pickupCandidate.set(MathUtils.random(minX, maxX), MathUtils.random(minY, maxY));
            if (!currentMap.supports(pickupCandidate)
                    || currentMap.distanceToHazard(pickupCandidate) < requiredHazardDistance) {
                continue;
            }
            if (requireNewSpot
                    && hasLastSafeZonePosition
                    && pickupCandidate.dst2(lastSafeZonePosition)
                    < requiredMoveDistance * requiredMoveDistance) {
                continue;
            }

            safeZonePosition.set(pickupCandidate);
            return true;
        }

        return false;
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

        invalidateLeaderboard();
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

        float visibleWidth = Math.max(1f, WORLD_WIDTH - CAMERA_HORIZONTAL_PADDING);
        float visibleHeight = Math.max(1f, WORLD_HEIGHT - CAMERA_VERTICAL_PADDING);
        float zoomX = mapBounds.width / visibleWidth;
        float zoomY = mapBounds.height / visibleHeight;

        float mapFitZoom = Math.max(MIN_WORLD_CAMERA_ZOOM, Math.max(zoomX, zoomY));
        float targetZoom = mapFitZoom;
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

        if (rlTrainingRandomSpawnLocations
                && buildRandomRoundSpawns(count, out, safeMargin, minDistance, maxAttempts)) {
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
            if (!isSpawnLocationClear(spawnCandidate, out, minDistance)) {
                continue;
            }

            out.add(SpawnPoint.facingPoint(
                    spawnCandidate.x,
                    spawnCandidate.y,
                    focusPoint.x,
                    focusPoint.y));
        }

        if (out.size == count) {
            return true;
        }

        out.clear();
        return false;
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
        drawSafeZone();
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

    private void drawSafeZone() {
        if (!safeZoneActive || safeZoneRadius <= 0f || currentMap == null) {
            return;
        }

        float timeRatio = MathUtils.clamp(safeZoneTimer / SAFE_ZONE_DURATION, 0f, 1f);
        float pulse = 0.5f + 0.5f * MathUtils.sin(effectClock * 8.0f);
        float urgency = 1f - timeRatio;
        drawSafeZoneFootprint(
                0.060f + urgency * 0.035f,
                0.34f + pulse * 0.18f,
                SAFE_ZONE_RENDER_SAMPLE_STEP,
                0.16f,
                true);
    }

    private void drawSafeZoneFootprint(
            float fillAlpha,
            float edgeAlpha,
            float sampleStep,
            float centerRadius,
            boolean drawCenter) {
        currentMap.getBounds(mapBounds);
        float radiusSq = safeZoneRadius * safeZoneRadius;
        float step = Math.max(0.05f, sampleStep);
        float halfStep = step * 0.5f;
        float minX = Math.max(mapBounds.x, safeZonePosition.x - safeZoneRadius);
        float maxX = Math.min(mapBounds.x + mapBounds.width, safeZonePosition.x + safeZoneRadius);
        float minY = Math.max(mapBounds.y, safeZonePosition.y - safeZoneRadius);
        float maxY = Math.min(mapBounds.y + mapBounds.height, safeZonePosition.y + safeZoneRadius);

        shapeRenderer.setColor(0.18f, 0.88f, 1f, fillAlpha);
        for (float x = minX + halfStep; x <= maxX; x += step) {
            for (float y = minY + halfStep; y <= maxY; y += step) {
                if (isSafeZonePlayableSample(x, y, radiusSq)) {
                    shapeRenderer.rect(x - halfStep, y - halfStep, step, step);
                }
            }
        }

        shapeRenderer.setColor(0.72f, 0.96f, 1f, edgeAlpha);
        for (float x = minX + halfStep; x <= maxX; x += step) {
            for (float y = minY + halfStep; y <= maxY; y += step) {
                if (isSafeZonePlayableSample(x, y, radiusSq)
                        && isSafeZoneFootprintEdge(x, y, step, radiusSq)) {
                    shapeRenderer.rect(x - halfStep, y - halfStep, step, step);
                }
            }
        }

        if (!drawCenter) {
            return;
        }
        shapeRenderer.setColor(1f, 1f, 1f, 0.64f);
        shapeRenderer.circle(safeZonePosition.x, safeZonePosition.y, centerRadius, 20);
    }

    private boolean isSafeZoneFootprintEdge(float x, float y, float step, float radiusSq) {
        float sampleOffset = step * 1.05f;
        return !isSafeZonePlayableSample(x + sampleOffset, y, radiusSq)
                || !isSafeZonePlayableSample(x - sampleOffset, y, radiusSq)
                || !isSafeZonePlayableSample(x, y + sampleOffset, radiusSq)
                || !isSafeZonePlayableSample(x, y - sampleOffset, radiusSq);
    }

    private boolean isSafeZonePlayableSample(float x, float y, float radiusSq) {
        float dx = x - safeZonePosition.x;
        float dy = y - safeZonePosition.y;
        return dx * dx + dy * dy <= radiusSq && currentMap.supports(x, y);
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
        refreshLeaderboardEntries();

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

        if (growthPickupActive) {
            float pickupRadius = 3.1f / minimapScale;
            shapeRenderer.setColor(1f, 0.88f, 0.28f, 0.94f);
            shapeRenderer.circle(growthPickupPosition.x, growthPickupPosition.y, pickupRadius, 18);
        }

        if (safeZoneActive) {
            drawSafeZoneFootprint(
                    0.10f,
                    0.52f,
                    SAFE_ZONE_MINIMAP_SAMPLE_STEP,
                    2.2f / minimapScale,
                    true);
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
        return "WASD/Arrows: drive   Esc: menu";
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
                .append("  |  Round ").append(roundNumber)
                .append("  |  Cars left: ").append(getAliveCarCount()).append("/").append(cars.size)
                .append("  |  Arenas ").append(mapProgression.getCurrentMapNumber()).append("/")
                .append(mapProgression.getMapCount());

        if (preRoundCountdownTimer > 0f) {
            builder.append("  |  Starts in ").append(MathUtils.ceil(preRoundCountdownTimer)).append("s");
        } else if (roundOver) {
            builder.append("  |  ROUND OVER");
        } else {
            builder.append("  |  Circle ").append(getSafeZoneSecondsLeft()).append("s")
                    .append("  |  Wave ").append(Math.max(1, safeZoneWave));
        }

        return builder.toString();
    }

    private String buildSidebarStateText() {
        if (preRoundCountdownTimer > 0f) {
            return "Starts in " + MathUtils.ceil(preRoundCountdownTimer) + "s";
        }

        if (roundOver) {
            return "Round over";
        }

        if (boostedCar != null && boostedCar.active) {
            String owner = boostedCar.playerControlled ? "YOU" : boostedCar.name;
            return owner + " BIG " + MathUtils.ceil(growthBoostTimer)
                    + "s  |  circle " + getSafeZoneSecondsLeft() + "s";
        }

        if (growthPickupActive) {
            return "Mass core live  |  circle " + getSafeZoneSecondsLeft() + "s";
        }

        return "Circle closes in " + getSafeZoneSecondsLeft() + "s";
    }

    private String buildObjectiveText() {
        if (preRoundCountdownTimer > 0f) {
            return "Prepare for the horn. Drive into each blue circle before it closes. A new smaller circle appears every 10 seconds.";
        }

        if (roundOver) {
            return "Standings updated. Next arena in a moment.";
        }

        if (boostedCar != null && boostedCar.active) {
            return (boostedCar.playerControlled
                    ? "Mass core active: you are huge for "
                    : boostedCar.name + " has the mass core for ")
                    + MathUtils.ceil(growthBoostTimer)
                    + " more seconds. "
                    + "Circle closes in "
                    + getSafeZoneSecondsLeft()
                    + "s.";
        }

        if (growthPickupActive) {
            return currentMap.getName() + ": mass core live. Circle closes in "
                    + getSafeZoneSecondsLeft()
                    + "s.";
        }

        return currentMap.getName() + ": reach the blue circle in "
                + getSafeZoneSecondsLeft()
                + "s. The next circle will be smaller.";
    }

    private int getSafeZoneSecondsLeft() {
        return MathUtils.ceil(Math.max(0f, safeZoneTimer));
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
            return "No cars survived the circles. Standings updated.";
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
        if (eliminated.eliminatedBySafeZone) {
            if (eliminated.playerControlled) {
                announceEvent(
                        "MISSED CIRCLE",
                        "You were outside when it closed.",
                        new Color(0.30f, 0.82f, 1f, 1f));
                return;
            }
            if (getAliveCarCount() <= 6) {
                announceEvent(
                        "CIRCLE CLOSED",
                        eliminated.name + " missed the zone.",
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
        private static final float WIDTH = 1.36f;
        private static final float HEIGHT = 1.58f;
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
        private final boolean externallyControlled;
        private final boolean modelControlled;
        private final Color color;
        private final CarAiController aiController;
        private final AiControlDecision externalControlDecision = new AiControlDecision();
        private final AiControlDecision rawExternalControlDecision = new AiControlDecision();
        private final float[] rlObservation = new float[RL_OBSERVATION_SIZE];
        private final Vector2 forwardAxis = new Vector2();
        private final Vector2 sidewaysAxis = new Vector2();
        private final Vector2 working = new Vector2();
        private final Vector2 pendingImpactImpulse = new Vector2();
        private final Vector2 impactRecoveryPoint = new Vector2();
        private final Vector2 impactOutward = new Vector2();
        private final Vector2 previousRenderPosition = new Vector2();
        private final Vector2 renderPosition = new Vector2();
        private final Rectangle rlObservationBounds = new Rectangle();
        private final Vector2 rlObservationFocus = new Vector2();
        private final Vector2 rlObservationForward = new Vector2();
        private final Vector2 rlObservationRecovery = new Vector2();
        private final Vector2 rlObservationRouteTarget = new Vector2();
        private final Vector2 rlObservationSide = new Vector2();

        private Body body;
        private boolean active = true;
        private boolean growthBoosted;
        private boolean eliminatedBySafeZone;
        private int safeZoneSurvivalSequence;
        private int lastAttackerId = -1;
        private int eliminatedByAttackerId = -1;
        private float sizeScale = 1f;
        private float impactSlideTimer;
        private float impactSlideStrength;
        private float controlLockTimer;
        private float recentImpactTimer;
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
            aiController =
                    playerControlled
                            ? null
                            : new CarAiController(template.personality);
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
                Array<Car> cars,
                boolean allowControl,
                float playerThrottle,
                float playerTurn,
                boolean growthPickupActive,
                Vector2 growthPickupPosition,
                boolean pointPickupActive,
                Vector2 pointPickupPosition,
                boolean safeZoneActive,
                Vector2 safeZonePosition,
                float safeZoneRadius,
                float safeZoneTimeRemaining,
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
                if (playerControlled) {
                    throttle = playerThrottle;
                    turn = playerTurn;
                } else if (externallyControlled) {
                    throttle = externalControlDecision.throttle;
                    turn = externalControlDecision.turn;
                } else if (modelControlled && rlPolicy != null) {
                    AiControlDecision decision =
                            planWithRlPolicy(
                                    delta,
                                    rlPolicy,
                                    arenaMap,
                                    cars,
                                    growthPickupActive,
                                    growthPickupPosition,
                                    pointPickupActive,
                                    pointPickupPosition,
                                    safeZoneActive,
                                    safeZonePosition,
                                    safeZoneRadius,
                                    safeZoneTimeRemaining);
                    throttle = decision.throttle;
                    turn = decision.turn;
                } else if (aiController != null) {
                    AiControlDecision decision =
                            aiController.plan(
                                    delta,
                                    this,
                                    arenaMap,
                                    cars,
                                    growthPickupActive,
                                    growthPickupPosition,
                                    pointPickupActive,
                                    pointPickupPosition,
                                    safeZoneActive,
                                    safeZonePosition,
                                    safeZoneRadius,
                                    safeZoneTimeRemaining);
                    throttle = decision.throttle;
                    turn = decision.turn;
                }
            }
            lastThrottleCommand = allowControl && controlLockTimer <= 0f ? throttle : 0f;

            applyGrip(impactSlideFactor);

            if (!allowControl || controlLockTimer > 0f) {
                return;
            }

            drive(throttle, turn);
        }

        private AiControlDecision planWithRlPolicy(
                float delta,
                RlPolicy policy,
                ArenaMap arenaMap,
                Array<Car> cars,
                boolean growthPickupActive,
                Vector2 growthPickupPosition,
                boolean pointPickupActive,
                Vector2 pointPickupPosition,
                boolean safeZoneActive,
                Vector2 safeZonePosition,
                float safeZoneRadius,
                float safeZoneTimeRemaining) {
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
                        cars,
                        growthPickupActive,
                        growthPickupPosition,
                        pointPickupActive,
                        pointPickupPosition,
                        safeZoneActive,
                        safeZonePosition,
                        safeZoneRadius,
                        safeZoneTimeRemaining,
                        positionNormalizer,
                        rlObservationFocus,
                        rlObservationForward,
                        rlObservationRecovery,
                        rlObservationRouteTarget,
                        rlObservationSide);
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

        private void applyGrip(float impactSlideFactor) {
            updateAxes();

            float gripMultiplier = MathUtils.clamp(1f - 0.68f * impactSlideFactor, 0.18f, 1f);
            float dragMultiplier = MathUtils.clamp(1f - 0.58f * impactSlideFactor, 0.24f, 1f);
            if (controlLockTimer > 0f) {
                gripMultiplier *= 0.58f;
                dragMultiplier *= 0.76f;
            }
            float longitudinalForceMultiplier = getMassMultiplier();

            float lateralSpeed = sidewaysAxis.dot(body.getLinearVelocity());
            working.set(sidewaysAxis).scl(-lateralSpeed * body.getMass() * LATERAL_GRIP * gripMultiplier);
            body.applyLinearImpulse(working, body.getWorldCenter(), true);

            body.applyAngularImpulse(
                    -body.getAngularVelocity()
                            * body.getInertia()
                            * ANGULAR_GRIP
                            * gripMultiplier,
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
            if (throttle != 0f) {
                boolean braking =
                        (throttle > 0f && signedForwardSpeed < -BRAKE_SPEED_THRESHOLD)
                                || (throttle < 0f && signedForwardSpeed > BRAKE_SPEED_THRESHOLD);
                float driveForce;
                if (braking) {
                    driveForce = BRAKE_FORCE;
                } else if (throttle > 0f) {
                    driveForce = DRIVE_FORCE;
                } else {
                    driveForce = REVERSE_FORCE;
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
            if (playerControlled) {
                return "Player";
            }
            if (externallyControlled) {
                return "RL";
            }
            if (modelControlled) {
                return "RL";
            }
            return aiController.getPersonality().displayName;
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
            Array<Car> cars,
            boolean growthPickupActive,
            Vector2 growthPickupPosition,
            boolean pointPickupActive,
            Vector2 pointPickupPosition,
            boolean safeZoneActive,
            Vector2 safeZonePosition,
            float safeZoneRadius,
            float safeZoneTimeRemaining,
            float positionNormalizer,
            Vector2 observationFocus,
            Vector2 observationForward,
            Vector2 observationRecovery,
            Vector2 observationRouteTarget,
            Vector2 observationSide) {
        for (int i = 0; i < RL_OBSERVATION_SIZE; i++) {
            observations[offset + i] = 0f;
        }
        if (car == null || !car.active || car.body == null || arenaMap == null) {
            return;
        }

        Vector2 position = car.body.getPosition();
        Vector2 velocity = car.body.getLinearVelocity();
        car.body.getWorldVector(observationForward.set(0f, 1f));
        arenaMap.findRecoveryPoint(position, observationRecovery);
        observationRecovery.sub(position);
        if (!observationRecovery.isZero(0.0001f)) {
            observationRecovery.nor();
        }

        observations[offset] = 1f;
        observations[offset + 1] =
                normalizedRlValue(position.x - observationFocus.x, positionNormalizer);
        observations[offset + 2] =
                normalizedRlValue(position.y - observationFocus.y, positionNormalizer);
        observations[offset + 3] = normalizedRlValue(velocity.x, RL_VELOCITY_NORMALIZER);
        observations[offset + 4] = normalizedRlValue(velocity.y, RL_VELOCITY_NORMALIZER);
        observations[offset + 5] = observationForward.x;
        observations[offset + 6] = observationForward.y;
        observations[offset + 7] =
                normalizedRlValue(car.body.getAngularVelocity(), RL_ANGULAR_VELOCITY_NORMALIZER);
        observations[offset + 8] = MathUtils.clamp(velocity.len() / Car.MAX_SPEED, 0f, 1f);
        observations[offset + 9] =
                MathUtils.clamp(
                        arenaMap.approximateDistanceToHazard(position)
                                / RL_HAZARD_DISTANCE_NORMALIZER,
                        0f,
                        1f);
        observations[offset + 10] =
                MathUtils.clamp(
                        arenaMap.distanceToSafety(position) / RL_HAZARD_DISTANCE_NORMALIZER,
                        0f,
                        1f);
        observations[offset + 11] = car.hasGrowthBoost() ? 1f : 0f;
        observations[offset + 12] = car.hasRamCharge() ? 1f : 0f;
        observations[offset + 13] =
                MathUtils.clamp(car.recentImpactTimer / Car.RECENT_IMPACT_DURATION, 0f, 1f);

        Car nearestOpponent = findNearestRlOpponent(car, cars);
        if (nearestOpponent != null) {
            Vector2 opponentPosition = nearestOpponent.body.getPosition();
            Vector2 opponentVelocity = nearestOpponent.body.getLinearVelocity();
            observations[offset + 14] =
                    normalizedRlValue(opponentPosition.x - position.x, positionNormalizer);
            observations[offset + 15] =
                    normalizedRlValue(opponentPosition.y - position.y, positionNormalizer);
            observations[offset + 16] =
                    normalizedRlValue(opponentVelocity.x - velocity.x, RL_VELOCITY_NORMALIZER);
            observations[offset + 17] =
                    normalizedRlValue(opponentVelocity.y - velocity.y, RL_VELOCITY_NORMALIZER);
            observations[offset + 18] = 1f;
            observations[offset + 19] =
                    MathUtils.clamp(
                            arenaMap.approximateDistanceToHazard(opponentPosition)
                                    / RL_HAZARD_DISTANCE_NORMALIZER,
                            0f,
                            1f);
            observations[offset + 20] = nearestOpponent.hasGrowthBoost() ? 1f : 0f;
            observations[offset + 21] = nearestOpponent.hasRamCharge() ? 1f : 0f;
        }

        observations[offset + 22] = observationRecovery.x;
        observations[offset + 23] = observationRecovery.y;
        fillRlPickupObservation(
                observations,
                offset + 24,
                growthPickupActive,
                growthPickupPosition,
                position,
                positionNormalizer);
        fillRlSafeZoneObservation(
                observations,
                offset + 27,
                safeZoneActive,
                safeZonePosition,
                safeZoneRadius,
                safeZoneTimeRemaining,
                position,
                positionNormalizer);
        fillRlRouteObservation(
                observations,
                offset + 30,
                arenaMap,
                safeZoneActive,
                safeZonePosition,
                position,
                positionNormalizer,
                observationRouteTarget);
        fillRlRayObservations(
                observations,
                offset + 33,
                arenaMap,
                position,
                observationForward,
                observationSide);
    }

    private static void fillRlPickupObservation(
            float[] observations,
            int offset,
            boolean active,
            Vector2 pickupPosition,
            Vector2 carPosition,
            float positionNormalizer) {
        if (!active || pickupPosition == null) {
            observations[offset + 2] = 0f;
            return;
        }

        observations[offset] =
                normalizedRlValue(pickupPosition.x - carPosition.x, positionNormalizer);
        observations[offset + 1] =
                normalizedRlValue(pickupPosition.y - carPosition.y, positionNormalizer);
        observations[offset + 2] = 1f;
    }

    private static void fillRlSafeZoneObservation(
            float[] observations,
            int offset,
            boolean active,
            Vector2 safeZonePosition,
            float safeZoneRadius,
            float safeZoneTimeRemaining,
            Vector2 carPosition,
            float positionNormalizer) {
        if (!active || safeZonePosition == null || safeZoneRadius <= 0f) {
            observations[offset + 2] = -1f;
            return;
        }

        observations[offset] =
                normalizedRlValue(safeZonePosition.x - carPosition.x, positionNormalizer);
        observations[offset + 1] =
                normalizedRlValue(safeZonePosition.y - carPosition.y, positionNormalizer);
        float signedMargin = safeZoneRadius - carPosition.dst(safeZonePosition);
        float marginSignal = MathUtils.clamp(signedMargin / Math.max(0.001f, safeZoneRadius), -1f, 1f);
        float urgency = 1f - MathUtils.clamp(safeZoneTimeRemaining / SAFE_ZONE_DURATION, 0f, 1f);
        observations[offset + 2] = MathUtils.clamp(marginSignal - urgency * 0.35f, -1f, 1f);
    }

    private static void fillRlRouteObservation(
            float[] observations,
            int offset,
            ArenaMap arenaMap,
            boolean safeZoneActive,
            Vector2 safeZonePosition,
            Vector2 carPosition,
            float positionNormalizer,
            Vector2 routeTarget) {
        if (!safeZoneActive || safeZonePosition == null || arenaMap == null) {
            observations[offset + 2] = -1f;
            return;
        }

        arenaMap.findDriveTarget(carPosition, safeZonePosition, RL_ROUTE_MARGIN, routeTarget);
        observations[offset] =
                normalizedRlValue(routeTarget.x - carPosition.x, positionNormalizer);
        observations[offset + 1] =
                normalizedRlValue(routeTarget.y - carPosition.y, positionNormalizer);
        observations[offset + 2] =
                routeTarget.dst2(safeZonePosition) <= RL_ROUTE_DIRECT_EPSILON * RL_ROUTE_DIRECT_EPSILON
                        ? 1f
                        : 0f;
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
        float length = (float) Math.sqrt(directionX * directionX + directionY * directionY);
        if (arenaMap == null || position == null || length <= 0.0001f) {
            return 0f;
        }

        float unitX = directionX / length;
        float unitY = directionY / length;
        for (float distance = RL_RAYCAST_STEP;
                distance < RL_RAYCAST_DISTANCE;
                distance += RL_RAYCAST_STEP) {
            float sampleX = position.x + unitX * distance;
            float sampleY = position.y + unitY * distance;
            if (arenaMap.approximateDistanceToHazard(sampleX, sampleY) < RL_ROUTE_MARGIN) {
                return MathUtils.clamp(distance / RL_RAYCAST_DISTANCE, 0f, 1f);
            }
        }
        return 1f;
    }

    private static Car findNearestRlOpponent(Car car, Array<Car> cars) {
        Car nearest = null;
        float nearestDistanceSq = Float.MAX_VALUE;
        if (car == null || car.body == null) {
            return null;
        }

        Vector2 position = car.body.getPosition();
        for (int i = 0; i < cars.size; i++) {
            Car other = cars.get(i);
            if (other == car || !other.active || other.body == null) {
                continue;
            }

            float distanceSq = position.dst2(other.body.getPosition());
            if (distanceSq < nearestDistanceSq) {
                nearestDistanceSq = distanceSq;
                nearest = other;
            }
        }
        return nearest;
    }

    private static float normalizedRlValue(float value, float normalizer) {
        return MathUtils.clamp(value / Math.max(0.0001f, normalizer), -1f, 1f);
    }

    public static final class RlTrainingConfig {
        public final Array<ArenaMap> maps = new Array<ArenaMap>();
        public final Array<AiDrivingPersonality> opponentPersonalities =
                new Array<AiDrivingPersonality>();
        public int controlledAgentCount = RL_DEFAULT_CONTROLLED_AGENTS;
        public int fieldSize = RL_DEFAULT_FIELD_SIZE;
        public int actionRepeat = RL_DEFAULT_ACTION_REPEAT;
        public int maxActionSteps = RL_DEFAULT_MAX_ACTION_STEPS;
        public long seed = 1L;
        public boolean skipCountdown = true;
        public boolean navigationOnly;
        public int opponentCount = -1;

        public RlTrainingConfig addMap(ArenaMap map) {
            if (map != null) {
                maps.add(map);
            }
            return this;
        }

        public RlTrainingConfig addOpponentPersonality(AiDrivingPersonality personality) {
            if (personality != null) {
                opponentPersonalities.add(personality);
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

        public RlTrainingConfig withSeed(long seed) {
            this.seed = seed;
            return this;
        }

        public RlTrainingConfig withSkipCountdown(boolean skipCountdown) {
            this.skipCountdown = skipCountdown;
            return this;
        }

        public RlTrainingConfig withNavigationOnly(boolean navigationOnly) {
            this.navigationOnly = navigationOnly;
            return this;
        }

        public RlTrainingConfig withOpponentCount(int opponentCount) {
            this.opponentCount = opponentCount;
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
                String currentMapName) {
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
        private final boolean[] dones;
        private final int controlledAgentCount;
        private int episodeIndex;
        private int actionStep;
        private boolean episodeStarted;
        private boolean episodeDone;
        private boolean closed;

        public RlTrainingEnvironment(RlTrainingConfig config) {
            this.config = config == null ? new RlTrainingConfig() : config;
            trainingMaps =
                    this.config.maps.size == 0
                            ? ArenaMaps.createHeadlessTrainingSet()
                            : this.config.maps;
            controlledAgentCount =
                    MathUtils.clamp(this.config.controlledAgentCount, 1, MAX_CAR_COUNT - 1);
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
            game.rlTrainingDisablePickups = config.navigationOnly;
            game.rlTrainingRandomSpawnLocations = true;
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
            if (config.navigationOnly) {
                game.growthPickupActive = false;
                game.pointPickupActive = false;
            }

            actionStep = 0;
            episodeStarted = true;
            episodeDone = false;
            clearRewards();
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

            captureSnapshots(beforeSnapshots);
            applyActions(actions);
            int repeats = Math.max(1, config.actionRepeat);
            for (int i = 0; i < repeats && !game.roundOver; i++) {
                game.stepSimulation(PHYSICS_STEP);
            }
            actionStep++;

            boolean maxStepsReached = !game.roundOver && actionStep >= getMaxActionSteps();

            captureSnapshots(afterSnapshots);
            episodeDone = maxStepsReached || game.roundOver || !hasActiveControlledAgent();
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
                        AiDrivingPersonalities.BALANCED,
                        visual,
                        "learner-" + i,
                        true);
                controlledVehicleIds.add(
                        Integer.valueOf(game.roster.get(game.roster.size - 1).vehicleId));
            }

            int fieldSize = getFieldSize(controlledAgentCount);
            if (fieldSize > controlledAgentCount) {
                Array<AiDrivingPersonality> opponents =
                        config.opponentPersonalities.size == 0
                                ? AiDrivingPersonalities.createPresetList()
                                : config.opponentPersonalities;
                int opponentIndex = 0;
                while (game.roster.size < fieldSize) {
                    AiDrivingPersonality personality =
                            opponents.get(opponentIndex % Math.max(1, opponents.size));
                    CarVisual visual = game.getCarVisual(game.roster.size);
                    game.addRosterTemplate(
                            personality.displayName + " " + (opponentIndex + 1),
                            false,
                            new Color(visual.color),
                            personality,
                            visual,
                            personality.id,
                            false);
                    opponentIndex++;
                }
            }
            game.invalidateLeaderboard();
        }

        private int getFieldSize(int controlledAgentCount) {
            if (config.opponentCount >= 0) {
                return MathUtils.clamp(
                        controlledAgentCount + config.opponentCount,
                        controlledAgentCount,
                        MAX_CAR_COUNT);
            }
            int minimumFieldSize =
                    config.navigationOnly
                            ? controlledAgentCount
                            : Math.min(MAX_CAR_COUNT, controlledAgentCount + 1);
            return MathUtils.clamp(config.fieldSize, minimumFieldSize, MAX_CAR_COUNT);
        }

        private int getMaxActionSteps() {
            return config.maxActionSteps <= 0
                    ? RL_DEFAULT_MAX_ACTION_STEPS
                    : config.maxActionSteps;
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
            snapshot.vehicleId = car.template.vehicleId;
            snapshot.score = car.template.totalPoints;
            snapshot.pickupPoints = car.template.roundPickupPoints;
            snapshot.finishPosition = car.template.roundFinishPosition;
            snapshot.eliminatedBySafeZone = car.eliminatedBySafeZone;
            snapshot.safeZoneSurvivalSequence = car.safeZoneSurvivalSequence;
            if (!snapshot.active || game.currentMap == null) {
                return;
            }

            Vector2 position = car.body.getPosition();
            snapshot.edgeDistance = game.currentMap.approximateDistanceToHazard(position);
            snapshot.safetyDistance = game.currentMap.distanceToSafety(position);
            Vector2 velocity = car.body.getLinearVelocity();
            snapshot.speed = velocity.len();
            snapshot.angularSpeed = Math.abs(car.body.getAngularVelocity());
            snapshot.effectiveThrottle = car.externalControlDecision.throttle;
            snapshot.growthBoosted = car.hasGrowthBoost();
            if (game.growthPickupActive) {
                snapshot.growthPickupActive = true;
                snapshot.growthPickupDistance = position.dst(game.growthPickupPosition);
            }
            if (game.safeZoneActive) {
                snapshot.safeZoneActive = true;
                snapshot.safeZoneSequence = game.safeZoneSequence;
                snapshot.safeZoneDistance = position.dst(game.safeZonePosition);
                snapshot.safeZoneRouteDistance =
                        game.currentMap.estimateDriveDistance(
                                position,
                                game.safeZonePosition,
                                RL_ROUTE_MARGIN);
                snapshot.safeZoneRadius = game.safeZoneRadius;
                snapshot.safeZoneSignedMargin = game.safeZoneRadius - snapshot.safeZoneDistance;
                snapshot.safeZoneInside = snapshot.safeZoneSignedMargin >= 0f;
                snapshot.safeZoneTimeRatio =
                        MathUtils.clamp(game.safeZoneTimer / SAFE_ZONE_DURATION, 0f, 1f);
                game.currentMap.findDriveTarget(
                        position,
                        game.safeZonePosition,
                        RL_ROUTE_MARGIN,
                        observationRouteTarget);
                observationRouteTarget.sub(position);
                snapshot.routeTargetDistance = observationRouteTarget.len();
                if (snapshot.routeTargetDistance > 0.0001f) {
                    observationRouteTarget.scl(1f / snapshot.routeTargetDistance);
                    snapshot.routeTargetSpeed = observationRouteTarget.dot(velocity);
                }
            }
            car.body.getWorldVector(observationForward.set(0f, 1f));
            snapshot.forwardSpeed = observationForward.dot(velocity);
            game.currentMap.findRecoveryPoint(position, observationRecovery);
            observationRecovery.sub(position);
            if (!observationRecovery.isZero(0.0001f)) {
                observationRecovery.nor();
                snapshot.recoverySpeed = observationRecovery.dot(velocity);
            }

            float nearestDistanceSq = Float.MAX_VALUE;
            for (int i = 0; i < game.cars.size; i++) {
                Car other = game.cars.get(i);
                if (other == car) {
                    continue;
                }
                if (!other.active) {
                    if (other.eliminatedByAttackerId == snapshot.vehicleId
                            && other.template.roundFinishPosition > 0) {
                        snapshot.creditedEliminations++;
                        if (other.eliminatedBySafeZone) {
                            snapshot.safeZoneEjections++;
                        }
                    }
                    continue;
                }
                if (other.body == null) {
                    continue;
                }

                snapshot.aliveOpponents++;
                if (other.lastAttackerId == snapshot.vehicleId && other.recentImpactTimer > 0f) {
                    snapshot.attackCreditCount++;
                    if (game.safeZoneActive
                            && snapshot.safeZoneInside
                            && !game.isInsideSafeZone(other)) {
                        snapshot.safeZonePushOuts++;
                    }
                }

                float distanceSq = position.dst2(other.body.getPosition());
                if (distanceSq < nearestDistanceSq) {
                    nearestDistanceSq = distanceSq;
                    snapshot.nearestOpponentHazardDistance =
                            game.currentMap.approximateDistanceToHazard(other.body.getPosition());
                }
            }
            if (nearestDistanceSq < Float.MAX_VALUE) {
                snapshot.nearestOpponentDistance = (float) Math.sqrt(nearestDistanceSq);
            }
            snapshot.recentImpact = car.recentImpactTimer;
        }

        private void computeRewards() {
            for (int agentIndex = 0; agentIndex < getControlledAgentCount(); agentIndex++) {
                clearRewardBreakdown(agentIndex);
                RlAgentSnapshot before = beforeSnapshots[agentIndex];
                RlAgentSnapshot after = afterSnapshots[agentIndex];
                float reward = 0f;

                if (before.active) {
                    if (!after.active) {
                        reward += recordReward(
                                agentIndex,
                                RL_REWARD_SURVIVAL,
                                -getEliminationPenalty(before));
                        reward += recordReward(
                                agentIndex,
                                RL_REWARD_ATTACK,
                                -getRecklessAttackEliminationPenalty(before));
                        if (after.eliminatedBySafeZone) {
                            reward += recordReward(
                                    agentIndex,
                                    RL_REWARD_CIRCLE,
                                    -RL_SAFE_ZONE_MISS_PENALTY);
                        }
                    } else {
                        reward += recordReward(
                                agentIndex,
                                RL_REWARD_SURVIVAL,
                                config.navigationOnly
                                        ? RL_NAVIGATION_ALIVE_STEP_REWARD
                                        : RL_ALIVE_STEP_REWARD);
                        reward += recordReward(
                                agentIndex,
                                RL_REWARD_CIRCLE,
                                getSafeZoneReward(before, after)
                                        * getNavigationCircleRewardScale());
                        reward += recordReward(
                                agentIndex,
                                RL_REWARD_EDGE,
                                MathUtils.clamp(after.edgeDistance - before.edgeDistance, -1f, 1f)
                                        * RL_EDGE_RECOVERY_REWARD);
                        reward += recordReward(
                                agentIndex,
                                RL_REWARD_EDGE,
                                -getEdgeDangerPenalty(before, after) * getNavigationEdgePenaltyScale());
                        if (!config.navigationOnly) {
                            reward += recordReward(
                                    agentIndex,
                                    RL_REWARD_ATTACK,
                                    MathUtils.clamp(
                                                    before.nearestOpponentHazardDistance
                                                            - after.nearestOpponentHazardDistance,
                                                    -1f,
                                                    1f)
                                            * getAttackSafetyScale(after)
                                            * RL_OPPONENT_PRESSURE_REWARD);
                            reward += recordReward(
                                    agentIndex,
                                    RL_REWARD_ATTACK,
                                    getOpponentClosingReward(before, after));
                            reward += recordReward(
                                    agentIndex,
                                    RL_REWARD_ATTACK,
                                    Math.max(
                                                    0,
                                                    after.creditedEliminations
                                                            - before.creditedEliminations)
                                            * RL_OPPONENT_ELIMINATION_REWARD);
                            reward += recordReward(
                                    agentIndex,
                                    RL_REWARD_ATTACK,
                                    Math.max(0, after.safeZoneEjections - before.safeZoneEjections)
                                            * RL_SAFE_ZONE_EJECTION_REWARD);
                            reward += recordReward(
                                    agentIndex,
                                    RL_REWARD_ATTACK,
                                    getSafeZonePushOutReward(before, after));
                            reward += recordReward(
                                    agentIndex,
                                    RL_REWARD_ATTACK,
                                    Math.max(0, after.attackCreditCount - before.attackCreditCount)
                                            * getAttackSafetyScale(after)
                                            * RL_IMPACT_CREDIT_REWARD);
                            reward += recordReward(
                                    agentIndex,
                                    RL_REWARD_ATTACK,
                                    getFastImpactReward(before, after));
                            reward += recordReward(
                                    agentIndex,
                                    RL_REWARD_ATTACK,
                                    -getUnsafeAttackPenalty(before, after));
                            reward += recordReward(
                                    agentIndex,
                                    RL_REWARD_DRIVING,
                                    getContactDisengageReward(before, after));
                            reward += recordReward(
                                    agentIndex,
                                    RL_REWARD_DRIVING,
                                    getContactEscapeSpeedReward(before, after));
                            reward += recordReward(
                                    agentIndex,
                                    RL_REWARD_PICKUP,
                                    getGrowthPickupReward(before, after));
                        }
                        reward += recordReward(
                                agentIndex,
                                RL_REWARD_DRIVING,
                                getSpeedControlReward(before, after));
                        if (config.navigationOnly) {
                            reward += recordReward(
                                    agentIndex,
                                    RL_REWARD_DRIVING,
                                    getNavigationRouteDriveReward(after));
                            reward += recordReward(
                                    agentIndex,
                                    RL_REWARD_DRIVING,
                                    -getNavigationRouteStallPenalty(after));
                            reward += recordReward(
                                    agentIndex,
                                    RL_REWARD_EDGE,
                                    -getNavigationUnsafeSpeedPenalty(agentIndex, after));
                        } else if (!after.safeZoneActive || !after.safeZoneInside) {
                            reward += recordReward(
                                    agentIndex,
                                    RL_REWARD_DRIVING,
                                    MathUtils.clamp(after.forwardSpeed / Car.MAX_SPEED, 0f, 1f)
                                            * getSafeSpeedScale(after)
                                            * RL_FORWARD_SPEED_REWARD);
                        }
                        reward += recordReward(
                                agentIndex,
                                RL_REWARD_DRIVING,
                                -MathUtils.clamp(-after.forwardSpeed / Car.MAX_SPEED, 0f, 1f)
                                        * RL_REVERSE_SPEED_PENALTY);
                        if (!config.navigationOnly
                                && !allowsReverseRecovery(after)
                                && (!after.safeZoneActive || !after.safeZoneInside)
                                && after.effectiveThrottle > RL_ACTION_FLIP_DEADZONE) {
                            reward += recordReward(
                                    agentIndex,
                                    RL_REWARD_DRIVING,
                                    MathUtils.clamp(
                                                    (after.effectiveThrottle
                                                                    - RL_ACTION_FLIP_DEADZONE)
                                                            / (1f - RL_ACTION_FLIP_DEADZONE),
                                                    0f,
                                                    1f)
                                            * getSafeSpeedScale(after)
                                            * RL_FORWARD_THROTTLE_COMMIT_REWARD);
                        }
                        reward += recordReward(
                                agentIndex,
                                RL_REWARD_CONTROL,
                                -getActionDitherPenalty(agentIndex, before, after));
                        if (after.speed < 0.35f && after.angularSpeed > 2.2f) {
                            reward += recordReward(
                                    agentIndex,
                                    RL_REWARD_CONTROL,
                                    -RL_SPIN_STALL_PENALTY);
                        }
                    }
                }

                if (episodeDone) {
                    int winnerAgentIndex = getWinnerAgentIndex();
                    if (winnerAgentIndex == agentIndex) {
                        reward += recordReward(agentIndex, RL_REWARD_WIN, RL_WIN_REWARD);
                    }
                    if (config.navigationOnly && after.active && actionStep >= getMaxActionSteps()) {
                        reward += recordReward(
                                agentIndex,
                                RL_REWARD_WIN,
                                RL_NAVIGATION_COMPLETE_REWARD);
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

        private float getNavigationCircleRewardScale() {
            return config.navigationOnly ? RL_NAVIGATION_CIRCLE_REWARD_SCALE : 1f;
        }

        private float getNavigationEdgePenaltyScale() {
            return config.navigationOnly ? RL_NAVIGATION_EDGE_PENALTY_SCALE : 1f;
        }

        private float getSafeZoneReward(RlAgentSnapshot before, RlAgentSnapshot after) {
            float reward = 0f;
            if (after.safeZoneSurvivalSequence > before.safeZoneSurvivalSequence) {
                reward += RL_SAFE_ZONE_DEADLINE_REWARD;
                reward += getSafeZoneCenterScore(before) * RL_SAFE_ZONE_DEADLINE_CENTER_REWARD;
            }
            if (!before.safeZoneActive || !after.safeZoneActive) {
                return reward;
            }

            float approach = getSafeZoneApproachProgress(before, after);
            float urgency = 1f - MathUtils.clamp(after.safeZoneTimeRatio, 0f, 1f);
            float urgencyScale = 0.55f + urgency * 0.75f;
            reward += approach * urgencyScale * RL_SAFE_ZONE_APPROACH_REWARD;
            if (after.safeZoneInside) {
                float centerScore = getSafeZoneCenterScore(after);
                float deepInsideScore =
                        MathUtils.clamp((centerScore - 0.35f) / 0.65f, 0f, 1f);
                float rimPressure =
                        MathUtils.clamp((0.32f - centerScore) / 0.32f, 0f, 1f);
                reward += (0.45f + urgency * 0.85f) * RL_SAFE_ZONE_INSIDE_REWARD;
                reward += centerScore * (0.70f + urgency * 0.90f) * RL_SAFE_ZONE_CENTER_REWARD;
                reward += deepInsideScore * (0.55f + urgency) * RL_SAFE_ZONE_DEEP_INSIDE_REWARD;
                reward -= rimPressure * (0.35f + urgency * 0.85f) * RL_SAFE_ZONE_RIM_PENALTY;

                float targetSpeed = MathUtils.lerp(1.55f, 0.55f, urgency);
                float speedScore =
                        MathUtils.clamp(
                                (targetSpeed - after.speed) / Math.max(0.001f, targetSpeed),
                                0f,
                                1f);
                reward += speedScore * (0.55f + urgency * 0.85f) * RL_SAFE_ZONE_SETTLE_REWARD;
                if (before.safeZoneInside && before.speed > after.speed) {
                    reward +=
                            MathUtils.clamp((before.speed - after.speed) / Car.MAX_SPEED, 0f, 1f)
                                    * (0.80f + urgency * 0.70f)
                                    * RL_SAFE_ZONE_BRAKE_REWARD;
                }

                if (before.safeZoneInside && after.safeZoneDistance > before.safeZoneDistance) {
                    float edgeRisk = 1f - centerScore;
                    reward -=
                            MathUtils.clamp(after.safeZoneDistance - before.safeZoneDistance, 0f, 1.2f)
                                    * (0.55f + urgency + edgeRisk * 0.55f)
                                    * RL_SAFE_ZONE_OUTWARD_PENALTY;
                }
                if (after.speed > targetSpeed) {
                    reward -=
                            MathUtils.clamp((after.speed - targetSpeed) / Car.MAX_SPEED, 0f, 1f)
                                    * (0.80f + urgency)
                                    * RL_SAFE_ZONE_FAST_EXIT_PENALTY;
                }
            } else {
                if (before.safeZoneInside) {
                    reward -= RL_SAFE_ZONE_EXIT_PENALTY;
                }
                float outsideScale =
                        MathUtils.clamp(
                                -after.safeZoneSignedMargin
                                        / Math.max(SAFE_ZONE_MIN_RADIUS, after.safeZoneDistance),
                                0f,
                                1f);
                reward -= outsideScale * urgency * urgency * RL_SAFE_ZONE_URGENCY_PENALTY;
            }
            return reward;
        }

        private float getSafeZoneCenterScore(RlAgentSnapshot snapshot) {
            if (snapshot == null || !snapshot.safeZoneActive || !snapshot.safeZoneInside) {
                return 0f;
            }
            float radius = Math.max(
                    SAFE_ZONE_MIN_RADIUS,
                    snapshot.safeZoneRadius > 0f
                            ? snapshot.safeZoneRadius
                            : snapshot.safeZoneDistance + snapshot.safeZoneSignedMargin);
            return MathUtils.clamp(snapshot.safeZoneSignedMargin / Math.max(0.001f, radius), 0f, 1f);
        }

        private float getSafeZoneApproachProgress(
                RlAgentSnapshot before,
                RlAgentSnapshot after) {
            if (before.safeZoneRouteDistance >= 0f && after.safeZoneRouteDistance >= 0f) {
                return MathUtils.clamp(
                        before.safeZoneRouteDistance - after.safeZoneRouteDistance,
                        -1.2f,
                        1.2f);
            }
            return MathUtils.clamp(before.safeZoneDistance - after.safeZoneDistance, -1.2f, 1.2f);
        }

        private float getEliminationPenalty(RlAgentSnapshot before) {
            float penalty = RL_ELIMINATION_PENALTY;
            if (before.safetyDistance <= 0.05f
                    && before.edgeDistance > RL_EDGE_DANGER_DISTANCE) {
                float avoidableScale =
                        MathUtils.clamp(
                                (before.edgeDistance - RL_EDGE_DANGER_DISTANCE)
                                        / Math.max(
                                                0.0001f,
                                                RL_SAFE_SPEED_DISTANCE - RL_EDGE_DANGER_DISTANCE),
                                0f,
                                1f);
                penalty += avoidableScale * RL_AVOIDABLE_ELIMINATION_PENALTY;
            }
            if (before.recoverySpeed < -0.20f) {
                penalty +=
                        MathUtils.clamp(-before.recoverySpeed / Car.MAX_SPEED, 0f, 1f)
                                * RL_OUTWARD_ELIMINATION_PENALTY;
            }
            return penalty;
        }

        private float getEdgeDangerPenalty(RlAgentSnapshot before, RlAgentSnapshot after) {
            float penalty = 0f;
            if (after.edgeDistance < RL_EDGE_DANGER_DISTANCE) {
                penalty +=
                        (1f - MathUtils.clamp(
                                after.edgeDistance / RL_EDGE_DANGER_DISTANCE, 0f, 1f))
                                * RL_EDGE_DANGER_PENALTY;
            }
            if (after.edgeDistance < before.edgeDistance
                    && after.edgeDistance < RL_EDGE_WARNING_DISTANCE) {
                float danger =
                        1f - MathUtils.clamp(
                                after.edgeDistance / RL_EDGE_WARNING_DISTANCE,
                                0f,
                                1f);
                penalty +=
                        MathUtils.clamp(before.edgeDistance - after.edgeDistance, 0f, 1f)
                                * (0.45f + danger)
                                * RL_EDGE_APPROACH_PENALTY;
            }
            if (after.edgeDistance < RL_EDGE_WARNING_DISTANCE
                    && after.speed > RL_EDGE_WARNING_SPEED
                    && after.recoverySpeed < 0.35f) {
                float danger =
                        1f - MathUtils.clamp(
                                after.edgeDistance / RL_EDGE_WARNING_DISTANCE,
                                0f,
                                1f);
                float unsafeSpeed =
                        MathUtils.clamp(
                                (after.speed - RL_EDGE_WARNING_SPEED) / Car.MAX_SPEED,
                                0f,
                                1f);
                float outwardScale =
                        after.recoverySpeed < 0f
                                ? 1f + MathUtils.clamp(-after.recoverySpeed / Car.MAX_SPEED, 0f, 1f)
                                : 0.55f;
                penalty += unsafeSpeed * danger * outwardScale * RL_EDGE_UNSAFE_SPEED_PENALTY;
            }
            return penalty;
        }

        private float getOpponentClosingReward(RlAgentSnapshot before, RlAgentSnapshot after) {
            if (allowsReverseRecovery(after)
                    || before.nearestOpponentDistance <= 0f
                    || after.nearestOpponentDistance <= 0f) {
                return 0f;
            }
            float closingDistance =
                    MathUtils.clamp(
                            before.nearestOpponentDistance - after.nearestOpponentDistance,
                            0f,
                            1f);
            if (after.nearestOpponentDistance < 0.85f) {
                closingDistance *= 0.65f;
            }
            return closingDistance * getAttackSafetyScale(after) * RL_OPPONENT_CLOSING_REWARD;
        }

        private float getFastImpactReward(RlAgentSnapshot before, RlAgentSnapshot after) {
            int newAttackCredits = Math.max(0, after.attackCreditCount - before.attackCreditCount);
            if (newAttackCredits == 0) {
                return 0f;
            }
            float attackSafety = getAttackSafetyScale(after);
            float reward =
                    newAttackCredits
                            * MathUtils.clamp(before.speed / Car.MAX_SPEED, 0f, 1f)
                            * attackSafety
                            * RL_FAST_IMPACT_REWARD;
            float opponentEdgePressure =
                    1f - MathUtils.clamp(
                            after.nearestOpponentHazardDistance / RL_SAFE_ATTACK_DISTANCE,
                            0f,
                            1f);
            reward +=
                    newAttackCredits
                            * opponentEdgePressure
                            * attackSafety
                            * RL_ATTACK_EDGE_PRESSURE_REWARD;
            return reward;
        }

        private float getSafeZonePushOutReward(RlAgentSnapshot before, RlAgentSnapshot after) {
            int newPushOuts = Math.max(0, after.safeZonePushOuts - before.safeZonePushOuts);
            if (newPushOuts == 0 || !after.safeZoneActive || !after.safeZoneInside) {
                return 0f;
            }
            return newPushOuts
                    * (0.70f + getSafeZoneCenterScore(after) * 0.45f)
                    * RL_SAFE_ZONE_PUSH_OUT_REWARD;
        }

        private float getUnsafeAttackPenalty(RlAgentSnapshot before, RlAgentSnapshot after) {
            int newAttackCredits = Math.max(0, after.attackCreditCount - before.attackCreditCount);
            if (newAttackCredits == 0) {
                return 0f;
            }

            float safetyDebt = 1f - getAttackSafetyScale(after);
            if (after.safeZoneActive && !after.safeZoneInside) {
                float urgency = 1f - MathUtils.clamp(after.safeZoneTimeRatio, 0f, 1f);
                safetyDebt = Math.max(safetyDebt, 0.60f + urgency * 0.35f);
            }
            if (after.edgeDistance < RL_EDGE_WARNING_DISTANCE && after.recoverySpeed < 0f) {
                float edgeRisk =
                        1f - MathUtils.clamp(
                                after.edgeDistance / RL_EDGE_WARNING_DISTANCE,
                                0f,
                                1f);
                safetyDebt =
                        Math.max(
                                safetyDebt,
                                edgeRisk
                                        * MathUtils.clamp(
                                                -after.recoverySpeed / Car.MAX_SPEED,
                                                0f,
                                                1f));
            }
            return newAttackCredits
                    * MathUtils.clamp(safetyDebt, 0f, 1f)
                    * RL_UNSAFE_ATTACK_PENALTY;
        }

        private float getRecklessAttackEliminationPenalty(RlAgentSnapshot before) {
            if (before.safetyDistance > 0.05f) {
                return 0f;
            }
            boolean fighting =
                    before.recentImpact > Car.RECENT_IMPACT_DURATION * 0.25f
                            || before.attackCreditCount > 0
                            || (before.nearestOpponentDistance > 0f
                                    && before.nearestOpponentDistance
                                            < RL_CONTACT_STUCK_DISTANCE * 1.35f);
            if (!fighting) {
                return 0f;
            }

            float safetyDebt = 1f - getAttackSafetyScale(before);
            if (before.safeZoneActive && !before.safeZoneInside) {
                float urgency = 1f - MathUtils.clamp(before.safeZoneTimeRatio, 0f, 1f);
                safetyDebt = Math.max(safetyDebt, 0.65f + urgency * 0.30f);
            }
            return (0.45f + 0.55f * MathUtils.clamp(safetyDebt, 0f, 1f))
                    * RL_ATTACK_SUICIDE_PENALTY;
        }

        private float getContactDisengageReward(RlAgentSnapshot before, RlAgentSnapshot after) {
            if (!isContactStuck(before) || after.nearestOpponentDistance <= 0f) {
                return 0f;
            }
            return MathUtils.clamp(
                            after.nearestOpponentDistance - before.nearestOpponentDistance,
                            0f,
                            1f)
                    * RL_CONTACT_DISENGAGE_REWARD;
        }

        private float getContactEscapeSpeedReward(RlAgentSnapshot before, RlAgentSnapshot after) {
            if (!isContactStuck(before)) {
                return 0f;
            }
            return MathUtils.clamp(after.speed - before.speed, 0f, 1.2f)
                    * RL_CONTACT_ESCAPE_SPEED_REWARD;
        }

        private float getSpeedControlReward(RlAgentSnapshot before, RlAgentSnapshot after) {
            if (after.safeZoneActive && after.safeZoneInside) {
                return 0f;
            }

            float speedRatio = MathUtils.clamp(after.speed / Car.MAX_SPEED, 0f, 1f);
            float reward = 0f;

            if (!config.navigationOnly) {
                reward += speedRatio * getSafeSpeedScale(after) * RL_SAFE_SPEED_REWARD;
            }

            if (!config.navigationOnly && after.edgeDistance >= RL_EDGE_DANGER_DISTANCE) {
                reward +=
                        MathUtils.clamp((after.speed - before.speed) / Car.MAX_SPEED, 0f, 1f)
                                * RL_ACCELERATION_REWARD;
            }

            if (after.edgeDistance < RL_EDGE_RECOVERY_SPEED_DISTANCE) {
                float danger =
                        1f - MathUtils.clamp(
                                after.edgeDistance / RL_EDGE_RECOVERY_SPEED_DISTANCE,
                                0f,
                                1f);
                float recoverySpeedRatio =
                        MathUtils.clamp(after.recoverySpeed / Car.MAX_SPEED, -1f, 1f);
                if (recoverySpeedRatio > 0f) {
                    reward += recoverySpeedRatio * danger * RL_EDGE_RECOVERY_SPEED_REWARD;
                } else {
                    reward -= -recoverySpeedRatio * danger * RL_EDGE_UNSAFE_RECOVERY_SPEED_PENALTY;
                }
                if (after.edgeDistance < before.edgeDistance) {
                    reward -=
                            MathUtils.clamp(before.edgeDistance - after.edgeDistance, 0f, 1f)
                                    * speedRatio
                                    * RL_EDGE_FAST_APPROACH_PENALTY;
                }
            }

            return reward;
        }

        private float getNavigationRouteDriveReward(RlAgentSnapshot after) {
            if (!after.safeZoneActive || after.safeZoneInside || after.routeTargetDistance <= 0.05f) {
                return 0f;
            }

            float speedRatio = MathUtils.clamp(after.routeTargetSpeed / Car.MAX_SPEED, -1f, 1f);
            if (speedRatio >= 0f) {
                float safetyScale =
                        MathUtils.clamp(0.25f + getSafeSpeedScale(after) * 0.75f, 0f, 1f);
                return speedRatio * safetyScale * RL_NAVIGATION_ROUTE_SPEED_REWARD;
            }
            return speedRatio * RL_NAVIGATION_ROUTE_BACKTRACK_PENALTY;
        }

        private float getNavigationRouteStallPenalty(RlAgentSnapshot after) {
            if (!after.safeZoneActive || after.safeZoneInside || after.routeTargetDistance <= 0.18f) {
                return 0f;
            }

            float urgency = 1f - MathUtils.clamp(after.safeZoneTimeRatio, 0f, 1f);
            float safeEdgeScale =
                    MathUtils.clamp(
                            (after.edgeDistance - RL_EDGE_DANGER_DISTANCE)
                                    / Math.max(
                                            0.0001f,
                                            RL_SAFE_SPEED_DISTANCE - RL_EDGE_DANGER_DISTANCE),
                            0f,
                            1f);
            if (safeEdgeScale <= 0f) {
                return 0f;
            }

            float routeSpeedRatio = MathUtils.clamp(after.routeTargetSpeed / Car.MAX_SPEED, -1f, 1f);
            float expectedSpeedRatio = 0.11f + urgency * 0.10f;
            return MathUtils.clamp(
                            (expectedSpeedRatio - routeSpeedRatio)
                                    / Math.max(0.001f, expectedSpeedRatio),
                            0f,
                            1f)
                    * (0.45f + urgency * 0.75f)
                    * safeEdgeScale
                    * RL_NAVIGATION_ROUTE_STALL_PENALTY;
        }

        private float getNavigationUnsafeSpeedPenalty(int agentIndex, RlAgentSnapshot after) {
            if (!after.safeZoneActive || after.safeZoneInside) {
                return 0f;
            }
            if (after.edgeDistance >= RL_EDGE_WARNING_DISTANCE) {
                return 0f;
            }

            float danger =
                    1f - MathUtils.clamp(
                            after.edgeDistance / RL_EDGE_WARNING_DISTANCE,
                            0f,
                            1f);
            float speedRatio = MathUtils.clamp(after.speed / Car.MAX_SPEED, 0f, 1f);
            float unsafeSpeed =
                    MathUtils.clamp(
                            (after.speed - RL_EDGE_WARNING_SPEED * 0.45f)
                                    / Math.max(0.001f, Car.MAX_SPEED),
                            0f,
                            1f);
            float penalty = unsafeSpeed * (0.35f + danger) * RL_NAVIGATION_EDGE_SPEED_PENALTY;

            if (after.effectiveThrottle > RL_ACTION_FLIP_DEADZONE) {
                penalty +=
                        MathUtils.clamp(after.effectiveThrottle, 0f, 1f)
                                * danger
                                * RL_NAVIGATION_EDGE_THROTTLE_PENALTY;
            }

            float turn = Math.abs(currentActionTurn[agentIndex]);
            penalty += turn * speedRatio * danger * RL_NAVIGATION_EDGE_TURN_PENALTY;
            return penalty;
        }

        private float getSafeSpeedScale(RlAgentSnapshot snapshot) {
            return MathUtils.clamp(
                    (snapshot.edgeDistance - RL_EDGE_DANGER_DISTANCE)
                            / Math.max(0.0001f, RL_SAFE_SPEED_DISTANCE - RL_EDGE_DANGER_DISTANCE),
                    0f,
                    1f);
        }

        private float getAttackSafetyScale(RlAgentSnapshot snapshot) {
            if (snapshot.safetyDistance > 0.05f) {
                return 0f;
            }
            float edgeScale = MathUtils.clamp(
                    (snapshot.edgeDistance - RL_EDGE_DANGER_DISTANCE)
                            / Math.max(0.0001f, RL_SAFE_ATTACK_DISTANCE - RL_EDGE_DANGER_DISTANCE),
                    0f,
                    1f);
            if (snapshot.safeZoneActive && !snapshot.safeZoneInside) {
                float urgency = 1f - MathUtils.clamp(snapshot.safeZoneTimeRatio, 0f, 1f);
                edgeScale *= MathUtils.clamp(0.35f - urgency * 0.30f, 0f, 0.35f);
            }
            return edgeScale;
        }

        private float getGrowthPickupReward(RlAgentSnapshot before, RlAgentSnapshot after) {
            int collectedPickups = Math.max(0, after.pickupPoints - before.pickupPoints);
            float reward =
                    collectedPickups * RL_GROWTH_PICKUP_REWARD;
            if (collectedPickups > 0 && after.safeZoneActive && after.safeZoneInside) {
                reward +=
                        collectedPickups
                                * (0.75f + getSafeZoneCenterScore(after) * 0.50f)
                                * RL_GROWTH_PICKUP_SAFE_ZONE_REWARD;
            }
            if (before.growthBoosted || after.growthBoosted) {
                return reward;
            }
            if (!before.growthPickupActive
                    || !after.growthPickupActive
                    || before.growthPickupDistance <= 0f
                    || after.growthPickupDistance <= 0f
                    || after.edgeDistance < RL_EDGE_DANGER_DISTANCE) {
                return reward;
            }
            reward +=
                    MathUtils.clamp(
                                    before.growthPickupDistance - after.growthPickupDistance,
                                    -1f,
                                    1f)
                            * RL_GROWTH_PICKUP_APPROACH_REWARD;
            return reward;
        }

        private float getActionDitherPenalty(
                int agentIndex,
                RlAgentSnapshot before,
                RlAgentSnapshot after) {
            float penalty = 0f;
            boolean reverseAllowed = allowsReverseRecovery(after);
            float previousThrottle = previousActionThrottle[agentIndex];
            float currentThrottle = currentActionThrottle[agentIndex];
            if (!reverseAllowed
                    && Math.abs(previousThrottle) >= RL_ACTION_FLIP_DEADZONE
                    && Math.abs(currentThrottle) >= RL_ACTION_FLIP_DEADZONE
                    && previousThrottle * currentThrottle < 0f) {
                penalty += RL_RAW_THROTTLE_FLIP_PENALTY;
            }

            if (!reverseAllowed
                    && Math.abs(before.effectiveThrottle) >= RL_ACTION_FLIP_DEADZONE
                    && Math.abs(after.effectiveThrottle) >= RL_ACTION_FLIP_DEADZONE
                    && before.effectiveThrottle * after.effectiveThrottle < 0f) {
                penalty += RL_EFFECTIVE_THROTTLE_FLIP_PENALTY;
            }

            if (!reverseAllowed
                    && Math.abs(before.forwardSpeed) > 0.35f
                    && Math.abs(after.forwardSpeed) > 0.35f
                    && before.forwardSpeed * after.forwardSpeed < 0f) {
                penalty += RL_FORWARD_REVERSE_SPEED_FLIP_PENALTY;
            }

            if ((!after.safeZoneActive || !after.safeZoneInside)
                    && Math.abs(after.effectiveThrottle) < RL_ACTION_FLIP_DEADZONE
                    && after.speed < 1.15f
                    && after.angularSpeed < 1.80f) {
                penalty +=
                        RL_IDLE_DITHER_PENALTY
                                * (1f - Math.abs(after.effectiveThrottle) / RL_ACTION_FLIP_DEADZONE);
            }
            if (!reverseAllowed) {
                if (after.effectiveThrottle < -RL_ACTION_FLIP_DEADZONE) {
                    penalty +=
                            RL_SAFE_REVERSE_ACTION_PENALTY
                                    * MathUtils.clamp(-after.effectiveThrottle, 0f, 1f);
                }
                if (after.effectiveThrottle < -RL_ACTION_FLIP_DEADZONE
                        && after.forwardSpeed > 0.35f) {
                    penalty +=
                            MathUtils.clamp(after.forwardSpeed / Car.MAX_SPEED, 0f, 1f)
                                    * RL_SAFE_REVERSE_BRAKE_PENALTY;
                }
            }
            if (isContactStuck(after)) {
                penalty +=
                        (1f - MathUtils.clamp(
                                after.speed / RL_CONTACT_STUCK_SPEED, 0f, 1f))
                                * RL_CONTACT_STUCK_PENALTY;
            }
            return penalty;
        }

        private boolean allowsReverseRecovery(RlAgentSnapshot snapshot) {
            return snapshot.safetyDistance > 0.05f
                    || snapshot.edgeDistance < RL_REVERSE_RECOVERY_EDGE_DISTANCE
                    || (snapshot.safeZoneActive
                            && snapshot.safeZoneInside
                            && snapshot.speed > RL_SAFE_ZONE_BRAKE_SPEED)
                    || snapshot.speed < RL_REVERSE_RECOVERY_SPEED
                    || snapshot.recentImpact > Car.RECENT_IMPACT_DURATION * 0.45f
                    || isContactStuck(snapshot);
        }

        private boolean isContactStuck(RlAgentSnapshot snapshot) {
            return snapshot.nearestOpponentDistance > 0f
                    && snapshot.nearestOpponentDistance < RL_CONTACT_STUCK_DISTANCE
                    && snapshot.speed < RL_CONTACT_STUCK_SPEED;
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
                fillRlObservation(
                        observations,
                        agentIndex * RL_OBSERVATION_SIZE,
                        getControlledCar(agentIndex),
                        game.currentMap,
                        game.cars,
                        game.growthPickupActive,
                        game.growthPickupPosition,
                        game.pointPickupActive,
                        game.pointPickupPosition,
                        game.safeZoneActive,
                        game.safeZonePosition,
                        game.safeZoneRadius,
                        game.safeZoneTimer,
                        positionNormalizer,
                        observationFocus,
                        observationForward,
                        observationRecovery,
                        observationRouteTarget,
                        observationSide);
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
            if (config.navigationOnly && episodeDone) {
                int aliveAgentIndex = -1;
                for (int i = 0; i < getControlledAgentCount(); i++) {
                    Car car = getControlledCar(i);
                    if (car != null && car.active) {
                        if (aliveAgentIndex >= 0) {
                            return -1;
                        }
                        aliveAgentIndex = i;
                    }
                }
                return aliveAgentIndex;
            }
            int winnerVehicleId = getWinningVehicleId();
            if (winnerVehicleId < 0) {
                return -1;
            }
            for (int i = 0; i < controlledVehicleIds.size; i++) {
                if (controlledVehicleIds.get(i).intValue() == winnerVehicleId) {
                    return i;
                }
            }
            return -1;
        }

        private String getWinnerLabel() {
            int winnerVehicleId = getWinningVehicleId();
            if (winnerVehicleId < 0 || winnerVehicleId >= game.roster.size) {
                return "";
            }
            return game.roster.get(winnerVehicleId).statsLabel;
        }

        private int getWinningVehicleId() {
            if (game.winner != null) {
                return game.winner.template.vehicleId;
            }
            int bestVehicleId = -1;
            int bestFinishPosition = 0;
            for (int i = 0; i < game.roster.size; i++) {
                CarTemplate template = game.roster.get(i);
                if (template.roundFinishPosition > bestFinishPosition) {
                    bestFinishPosition = template.roundFinishPosition;
                    bestVehicleId = template.vehicleId;
                }
            }
            return bestVehicleId;
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
            for (int i = 0; i < rewardBreakdown.length; i++) {
                rewardBreakdown[i] = 0f;
            }
        }

        private RlStepResult createResult() {
            float[] observationCopy = new float[observations.length];
            float[] rewardCopy = new float[rewards.length];
            float[] rewardBreakdownCopy = new float[rewardBreakdown.length];
            float[] effectiveActionCopy = new float[effectiveActions.length];
            boolean[] doneCopy = new boolean[dones.length];
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
                    getCurrentMapName());
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
        private int vehicleId;
        private int score;
        private int pickupPoints;
        private int finishPosition;
        private int aliveOpponents;
        private int attackCreditCount;
        private int creditedEliminations;
        private int safeZonePushOuts;
        private int safeZoneEjections;
        private int safeZoneSequence;
        private int safeZoneSurvivalSequence;
        private boolean growthBoosted;
        private boolean growthPickupActive;
        private boolean safeZoneActive;
        private boolean safeZoneInside;
        private boolean eliminatedBySafeZone;
        private float edgeDistance;
        private float safetyDistance;
        private float nearestOpponentHazardDistance;
        private float nearestOpponentDistance;
        private float growthPickupDistance;
        private float safeZoneDistance;
        private float safeZoneRouteDistance;
        private float safeZoneRadius;
        private float safeZoneSignedMargin;
        private float safeZoneTimeRatio;
        private float routeTargetDistance;
        private float routeTargetSpeed;
        private float speed;
        private float effectiveThrottle;
        private float forwardSpeed;
        private float recoverySpeed;
        private float angularSpeed;
        private float recentImpact;

        private void clear() {
            active = false;
            vehicleId = -1;
            score = 0;
            pickupPoints = 0;
            finishPosition = 0;
            aliveOpponents = 0;
            attackCreditCount = 0;
            creditedEliminations = 0;
            safeZonePushOuts = 0;
            safeZoneEjections = 0;
            safeZoneSequence = 0;
            safeZoneSurvivalSequence = 0;
            growthBoosted = false;
            growthPickupActive = false;
            safeZoneActive = false;
            safeZoneInside = false;
            eliminatedBySafeZone = false;
            edgeDistance = 0f;
            safetyDistance = 0f;
            nearestOpponentHazardDistance = 0f;
            nearestOpponentDistance = 0f;
            growthPickupDistance = 0f;
            safeZoneDistance = 0f;
            safeZoneRouteDistance = -1f;
            safeZoneRadius = 0f;
            safeZoneSignedMargin = 0f;
            safeZoneTimeRatio = 0f;
            routeTargetDistance = 0f;
            routeTargetSpeed = 0f;
            speed = 0f;
            effectiveThrottle = 0f;
            forwardSpeed = 0f;
            recoverySpeed = 0f;
            angularSpeed = 0f;
            recentImpact = 0f;
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
        private final boolean externallyControlled;
        private final boolean modelControlled;
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
                String statsLabel,
                boolean externallyControlled,
                boolean modelControlled) {
            this.vehicleId = vehicleId;
            this.name = name;
            this.playerControlled = playerControlled;
            this.color = color;
            this.personality = personality;
            this.visual = visual;
            this.statsLabel = statsLabel == null || statsLabel.length() == 0 ? name : statsLabel;
            this.externallyControlled = externallyControlled;
            this.modelControlled = modelControlled;
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
