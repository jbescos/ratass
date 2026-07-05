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
    private static final String DEFAULT_THEME_NAME = "gt3";
    private static final String HALLOWEEN_THEME_NAME = "halloween";
    private static final String CAMERA_FOLLOW_BEHIND_PROPERTY = "camera.follow.behind";
    private static final String CAMERA_FOLLOW_BEHIND_PREF_KEY = CAMERA_FOLLOW_BEHIND_PROPERTY;
    private static final String CAMERA_ZOOM_PROPERTY = "camera.zoom";
    private static final String CAMERA_ZOOM_PREF_KEY = CAMERA_ZOOM_PROPERTY;
    private static final String CAR_COUNT_PROPERTY = "cars.count";
    private static final String CAR_COUNT_PREF_KEY = CAR_COUNT_PROPERTY;
    private static final String PLAYER_CAR_PROPERTY = "cars.player.index";
    private static final String PLAYER_CAR_PREF_KEY = PLAYER_CAR_PROPERTY;
    private static final String DRIVER_POLICY_PROPERTY = "driver.policy.id";
    private static final String DRIVER_POLICY_PREF_KEY = DRIVER_POLICY_PROPERTY;
    private static final String RACE_LAPS_PROPERTY = "race.laps";
    private static final String RACE_LAPS_PREF_KEY = RACE_LAPS_PROPERTY;
    private static final String SANDBOX_MAP_PREF_KEY = "sandbox.map.index";
    private static final String THEME_DIRECTORY = "theme";
    private static final String THEME_MANIFEST_PATH = "themes.txt";
    private static final String THEME_CAR_SHEET_PATH = "cars/cars.png";
    private static final String THEME_FLAT_CAR_SHEET_PATH = "cars.png";
    private static final String THEME_CAR_DIRECTORY_PATH = "cars";
    private static final String THEME_ENEMY_NAMES_PATH = "enemy-names.txt";
    private static final String RL_ENEMY_POLICY_PATH = "ai/rl_enemy_policy.json";
    private static final String RL_POLICY_DIRECTORY = "ai/policies";
    private static final String RL_POLICY_FILE_NAME = "rl_enemy_policy.json";
    private static final String RL_LEGACY_POLICY_ID = "legacy";
    private static final String MANUAL_DRIVER_POLICY_ID = "manual";
    private static final DriverPolicyChoice[] DRIVER_POLICY_CHOICES = new DriverPolicyChoice[] {
            new DriverPolicyChoice(MANUAL_DRIVER_POLICY_ID, "Manual"),
            new DriverPolicyChoice("rookie", "Rookie"),
            new DriverPolicyChoice("expert", "Expert"),
            new DriverPolicyChoice("aggressive", "Aggressive"),
            new DriverPolicyChoice("clean", "Clean"),
            new DriverPolicyChoice("qualifier", "Qualifier"),
            new DriverPolicyChoice("veteran", "Veteran"),
            new DriverPolicyChoice("reckless", "Reckless")
    };
    private static final ThemeChoice[] FALLBACK_THEME_CHOICES = new ThemeChoice[] {
            new ThemeChoice("gt3", "GT3")
    };

    private static Rectangle[] createRectangleArray(int count) {
        Rectangle[] rectangles = new Rectangle[count];
        for (int i = 0; i < rectangles.length; i++) {
            rectangles[i] = new Rectangle();
        }
        return rectangles;
    }

    private static final int THEME_CAR_COLUMNS = 10;
    private static final int THEME_CAR_SHEET_ROWS = 5;
    private static final int MAX_CAR_VISUAL_COUNT = 10;
    private static final float HALLOWEEN_CIRCUIT_TINT_R = 0.56f;
    private static final float HALLOWEEN_CIRCUIT_TINT_G = 0.52f;
    private static final float HALLOWEEN_CIRCUIT_TINT_B = 0.64f;
    private static final float HALLOWEEN_WORLD_RAIN_ALPHA = 0.24f;
    private static final int STANDALONE_CAR_PREVIEW_COLUMNS = 5;
    private static final int STANDALONE_CAR_PREVIEW_CELL_WIDTH = 150;
    private static final int STANDALONE_CAR_PREVIEW_CELL_HEIGHT = 200;
    private static final int DEFAULT_CAR_COUNT = 10;
    private static final int DEFAULT_PLAYER_CAR_INDEX = 0;
    private static final int DEFAULT_RACE_LAPS = 3;
    private static final int MIN_RACE_LAPS = 1;
    private static final int MAX_RACE_LAPS = 10;
    private static final int MIN_CAR_COUNT = 1;
    private static final int MAX_CAR_COUNT = 10;
    private static final int DEFAULT_DRIVER_POLICY_INDEX = 1;
    private static final int OPTIONS_THEME_SELECTION = 0;
    private static final int OPTIONS_CARS_SELECTION = 1;
    private static final int OPTIONS_PLAYER_CAR_SELECTION = 2;
    private static final int OPTIONS_DRIVER_POLICY_SELECTION = 3;
    private static final int OPTIONS_LAPS_SELECTION = 4;
    private static final int OPTIONS_CAMERA_SELECTION = 5;
    private static final int OPTIONS_ZOOM_SELECTION = 6;
    private static final int OPTIONS_BACK_SELECTION = 7;
    private static final int MAIN_MENU_NEW_GAME_SELECTION = 0;
    private static final int MAIN_MENU_SANDBOX_SELECTION = 1;
    private static final int MAIN_MENU_MAPS_SELECTION = 2;
    private static final int MAIN_MENU_OPTIONS_SELECTION = 3;
    private static final int MAIN_MENU_EXIT_SELECTION = 4;
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
    private static final float RACE_ROUTE_MIN_DELTA = 0.001f;
    private static final float SLIPSTREAM_RANGE = Car.HEIGHT * 7.0f;
    private static final float SLIPSTREAM_MIN_DISTANCE = Car.HEIGHT * 1.15f;
    private static final float SLIPSTREAM_LATERAL_WIDTH = Car.WIDTH * 2.35f;
    private static final float SLIPSTREAM_SPEED_CAP_RATIO = 0.20f;
    private static final float SLIPSTREAM_MIN_ALIGNMENT = 0.82f;
    private static final float SLIPSTREAM_MAX_SPEED_BONUS = 0.10f;
    private static final float SLIPSTREAM_ENGINE_FORCE_BONUS = 0.20f;
    private static final float SLIPSTREAM_ATTACK_LERP = 9.0f;
    private static final float SLIPSTREAM_RELEASE_LERP = 6.0f;
    private static final float PASSING_ASSIST_RANGE = Car.HEIGHT * 7.0f;
    private static final float PASSING_ASSIST_MIN_FORWARD_DISTANCE = Car.HEIGHT * 0.45f;
    private static final float PASSING_ASSIST_RELEASE_BEHIND_DISTANCE = Car.HEIGHT * 1.15f;
    private static final float PASSING_ASSIST_CLOSE_FOLLOW_DISTANCE = Car.HEIGHT * 3.2f;
    private static final float PASSING_ASSIST_ACQUIRE_LATERAL_DISTANCE = Car.WIDTH * 1.5f;
    private static final float PASSING_ASSIST_HOLD_LATERAL_DISTANCE = Car.WIDTH * 2.5f;
    private static final float PASSING_ASSIST_MIN_ALIGNMENT = 0.82f;
    private static final float PASSING_ASSIST_MIN_CLOSING_SPEED = 0.12f;
    private static final float PASSING_ASSIST_MAX_RECEDING_SPEED = 0.20f;
    private static final float PASSING_ASSIST_TIME_TO_CONTACT = 2.0f;
    private static final float PASSING_ASSIST_MIN_SPEED_RATIO = 0.10f;
    private static final float PASSING_ASSIST_SIDE_GAP = Car.WIDTH * 0.18f;
    private static final float PASSING_ASSIST_EDGE_GAP = Car.WIDTH * 0.16f;
    private static final float PASSING_ASSIST_SIDE_SWITCH_HYSTERESIS = Car.WIDTH * 0.35f;
    private static final float PASSING_ASSIST_MIN_TURN = 0.30f;
    private static final float PASSING_ASSIST_MAX_TURN = 0.68f;
    private static final float PASSING_ASSIST_ATTACK_LERP = 9.0f;
    private static final float PASSING_ASSIST_RELEASE_LERP = 6.0f;
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
    private static final float CAMERA_TARGET_SWITCH_DURATION = 0.95f;
    private static final float CAMERA_TARGET_SWITCH_LERP_SPEED = 2.4f;
    private static final float CAMERA_TARGET_SWITCH_ZOOM_OUT = 0.28f;
    private static final float CAR_SPEED_BAR_WIDTH = 38f;
    private static final float CAR_SPEED_BAR_HEIGHT = 5f;
    private static final float CAR_SPEED_BAR_OFFSET_Y = 60f;
    private static final float CAR_SPEED_BAR_BORDER = 1f;
    private static final float CAR_TELEMETRY_LABEL_WIDTH = 58f;
    private static final float CAR_TELEMETRY_LABEL_GAP = 4f;
    private static final float CAR_TELEMETRY_ROW_STEP = 9f;
    private static final int CAR_TELEMETRY_ROW_COUNT = 4;
    private static final int SANDBOX_PHYSICS_HORSE_POWER = 0;
    private static final int SANDBOX_PHYSICS_BRAKE_FORCE = 1;
    private static final int SANDBOX_PHYSICS_STEERING_TORQUE = 2;
    private static final int SANDBOX_PHYSICS_WHEEL_GRIP = 3;
    private static final int SANDBOX_PHYSICS_MASS_MULTIPLIER = 4;
    private static final int SANDBOX_PHYSICS_TUNER_COUNT = 5;
    private static final String[] SANDBOX_PHYSICS_TUNER_LABELS = {
            "horsepower",
            "brake",
            "steering",
            "grip",
            "mass"
    };
    private static final float[] SANDBOX_PHYSICS_TUNER_MIN = {
            250f,
            240f,
            10f,
            0.20f,
            0.75f
    };
    private static final float[] SANDBOX_PHYSICS_TUNER_MAX = {
            650f,
            980f,
            120f,
            2.10f,
            1.75f
    };
    private static final float[] SANDBOX_PHYSICS_TUNER_STEP = {
            10f,
            20f,
            2.5f,
            0.05f,
            0.05f
    };
    private static final float SANDBOX_PHYSICS_TUNER_WIDTH = 330f;
    private static final float SANDBOX_PHYSICS_TUNER_HEADER_HEIGHT = 30f;
    private static final float SANDBOX_PHYSICS_TUNER_ROW_HEIGHT = 24f;
    private static final float SANDBOX_PHYSICS_TUNER_MARGIN = 14f;
    private static final int ROUND_SPAWN_ATTEMPTS = 3200;
    private static final float ROUND_SPAWN_SAFE_MARGIN = 1.15f;
    private static final float ROUND_SPAWN_CROWDED_SAFE_MARGIN = 1.0f;
    private static final float ROUND_SPAWN_MIN_DISTANCE = 1.95f;
    private static final float EVENT_CALLOUT_DURATION = 1.35f;
    public static final int RL_OBSERVATION_SIZE = 25;
    public static final int RL_ACTION_SIZE = 2;
    public static final int RL_REWARD_BREAKDOWN_SIZE = 10;
    private static final int RL_REWARD_ROUTE_PROGRESS = 0;
    private static final int RL_REWARD_STEP_COST = 1;
    private static final int RL_REWARD_OFF_ROAD = 2;
    private static final int RL_REWARD_STEERING = 3;
    private static final int RL_REWARD_REVERSE_SPEED = 4;
    private static final int RL_REWARD_CAR_PUSH = 5;
    private static final int RL_REWARD_ROUTE_ALIGNMENT = 6;
    private static final int RL_REWARD_NO_PROGRESS = 7;
    private static final int RL_REWARD_OFF_ROAD_RECOVERY = 8;
    private static final int RL_REWARD_OFF_ROAD_FAILURE = 9;
    private static final String[] RL_REWARD_BREAKDOWN_NAMES = {
            "route_progress",
            "step_cost",
            "off_road",
            "steering",
            "reverse_speed",
            "car_push",
            "route_alignment",
            "no_progress",
            "off_road_recovery",
            "off_road_failure"
    };
    private static final String[] RL_OBSERVATION_NAMES = {
            "route_fwd",
            "route_side",
            "target_fwd",
            "target_side",
            "route_curvature",
            "target_curvature",
            "route_left_margin",
            "route_right_margin",
            "target_route_left_clear",
            "target_route_right_clear",
            "forward_speed",
            "route_forward_speed",
            "route_lateral_speed",
            "angular_speed",
            "slip_angle",
            "off_road_dist",
            "trajectory_clear",
            "brake_demand",
            "recovery_fwd",
            "recovery_side",
            "road_left",
            "road_right",
            "road_front",
            "road_front_left",
            "road_front_right"
    };
    private static final int RL_DEBUG_TRACE_SIZE = 23;
    private static final String[] RL_DEBUG_TRACE_NAMES = {
            "active",
            "car_x",
            "car_y",
            "car_angle",
            "speed",
            "forward_speed",
            "lateral_speed",
            "route_progress",
            "route_progress_normalized",
            "route_tangent_x",
            "route_tangent_y",
            "route_forward_speed",
            "route_delta",
            "route_distance_since_target",
            "route_target_distance",
            "route_targets_reached",
            "route_targets_required",
            "off_road",
            "off_road_distance",
            "edge_distance",
            "effective_throttle",
            "effective_turn",
            "episode_done"
    };
    private static final int RL_DEFAULT_CONTROLLED_AGENTS = 1;
    private static final int RL_DEFAULT_FIELD_SIZE = 1;
    private static final int RL_DEFAULT_ACTION_REPEAT = 4;
    private static final int RL_DEFAULT_MAX_ACTION_STEPS = 6400;
    private static final int RL_DEFAULT_ROUTE_TARGETS = 6;
    private static final float RL_LIVE_DECISION_INTERVAL = RL_DEFAULT_ACTION_REPEAT * PHYSICS_STEP;
    private static final float RL_INITIAL_DECISION_STAGGER = 0.24f;
    private static final float RL_ANGULAR_VELOCITY_NORMALIZER = 8f;
    private static final float RL_OFF_ROAD_DISTANCE_NORMALIZER = Car.HEIGHT * 4f;
    private static final float RL_ROUTE_MARGIN_NORMALIZER = Car.HEIGHT * 4f;
    private static final float RL_ROUTE_MARGIN = 0.50f;
    private static final float RL_RAYCAST_DISTANCE = Car.HEIGHT * 30f;
    private static final float RL_LATERAL_ROAD_CLEARANCE_DISTANCE = Car.HEIGHT * 4f;
    private static final float RL_FRONT_ROAD_CLEARANCE_DISTANCE = Car.HEIGHT * 12f;
    private static final float RL_FRONT_DIAGONAL_ROAD_CLEARANCE_DISTANCE = Car.HEIGHT * 7f;
    private static final float RL_ROUTE_LOOKAHEAD_DISTANCE = Car.HEIGHT * 7f;
    private static final float RL_STEERING_MIN_LOOKAHEAD_DISTANCE = Car.HEIGHT * 3f;
    private static final float RL_STEERING_MAX_LOOKAHEAD_DISTANCE = Car.HEIGHT * 8f;
    private static final float RL_STEERING_LOOKAHEAD_SECONDS = 0.35f;
    private static final float RL_SENSOR_MIN_LOOKAHEAD_DISTANCE = Car.HEIGHT * 4f;
    private static final float RL_SENSOR_MAX_LOOKAHEAD_DISTANCE = Car.HEIGHT * 85f;
    private static final float RL_SENSOR_LOOKAHEAD_SECONDS = 0.45f;
    private static final float RL_BRAKE_DEMAND_SAMPLE_STEP = Car.HEIGHT;
    private static final float RL_BRAKE_DEMAND_CURVATURE_EPSILON = 0.001f;
    private static final float RL_SHORT_RAYCAST_DISTANCE = 7.5f;
    private static final float RL_RAYCAST_STEP = 0.40f;
    private static final float SANDBOX_ROUTE_LINE_SAMPLE_STEP = 0.70f;
    private static final float SANDBOX_ROUTE_LINE_WIDTH = 0.08f;
    private static final float SANDBOX_SENSOR_RAY_WIDTH = 0.04f;
    private static final float SANDBOX_SENSOR_HIT_RADIUS = 0.13f;
    private static final float SANDBOX_ROUTE_ARROW_LENGTH = 1.15f;
    private static final float SANDBOX_ROUTE_ARROW_WIDTH = 0.11f;
    private static final float SANDBOX_SENSOR_LABEL_DISTANCE = 0.95f;
    private static final float SANDBOX_SENSOR_LABEL_STEP = 10f;
    private static final float SANDBOX_SENSOR_WORLD_LABEL_OFFSET = 0.24f;
    private static final float RL_CONTROL_DEADZONE = 0.06f;
    private static final float RL_ACTION_FLIP_DEADZONE = 0.18f;
    private static final float RL_STEP_PENALTY = 0.006f;
    private static final float RL_PROGRESS_REWARD = 0.25f;
    private static final float RL_PROGRESS_FAST_DELTA = Car.HEIGHT;
    private static final float RL_PROGRESS_FAST_BONUS = 3.00f;
    private static final float RL_PROGRESS_MOVEMENT_TOLERANCE = 1.50f;
    private static final float RL_PROGRESS_MOVEMENT_EPSILON = 0.05f;
    private static final float RL_NO_PROGRESS_RESET_DISTANCE = Car.HEIGHT * 0.50f;
    private static final int RL_DEFAULT_NO_PROGRESS_MAX_ACTION_STEPS = 600;
    private static final float RL_NO_PROGRESS_PENALTY = 50f;
    private static final float RL_OFF_ROAD_RECOVERY_REWARD = 4f;
    private static final int RL_DEFAULT_OFF_ROAD_FAILURE_MAX_ACTION_STEPS = 45;
    private static final float RL_OFF_ROAD_FAILURE_PENALTY = 50f;
    private static final float RL_ROUTE_ALIGNMENT_REWARD = 0f;
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
    private static final float RL_RANDOM_SPAWN_MIN_SEGMENT_PROGRESS = 0.04f;
    private static final float RL_RANDOM_SPAWN_MAX_SEGMENT_PROGRESS = 0.96f;
    private static final float RL_RANDOM_SPAWN_SEGMENT_ERROR_MIN = 2.0f;
    private static final float RL_RANDOM_SPAWN_SEGMENT_ERROR_RATIO = 0.18f;
    private static final float RL_RANDOM_SPAWN_SEGMENT_ERROR_PENALTY = 4.5f;
    private static final float RL_RANDOM_SPAWN_ROUTE_TARGET_EPSILON = 0.35f;
    private static final int RL_ROUTE_TRAINING_CHUNKS_PER_LAP = 4;
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
    private static final float SIDEBAR_LEADERBOARD_COMPACT_ROW_STEP = 15.5f;
    private static final float SIDEBAR_CAMERA_TARGET_ICON_WIDTH = 18f;
    private static final float HUD_FONT_SCALE = 1.18f;
    private static final float TITLE_FONT_SCALE = 2.25f;
    private static final float LEADERBOARD_FONT_SCALE = 0.96f;
    private static final float LABEL_FONT_SCALE = 1.05f;
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
    private static final float MENU_MAP_DEBUG_SAMPLE_STEP = 0.55f;
    private static final float MENU_MAP_DEBUG_ARROW_STEP = 18f;
    private static final float MENU_MAP_DEBUG_MIN_LINE_WIDTH = 3f;
    private static final float MENU_MAP_DEBUG_MAX_LINE_WIDTH = 6f;
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
    private final Array<String> sessionEnemyPolicyIds = new Array<String>();
    private final Array<String> sessionEnemyPolicyPool = new Array<String>();
    private final Array<Rectangle> menuCarSheetSourceBounds = new Array<Rectangle>();
    private final Array<ThemeChoice> themeChoices = new Array<ThemeChoice>();
    private final Array<String> themeEnemyNames = new Array<String>();
    private final Array<ArenaMap> sandboxMenuMaps = new Array<ArenaMap>();
    private final LinkedHashMap<String, Texture> arenaSurfaceTextureCache =
            new LinkedHashMap<String, Texture>();
    private final LinkedHashMap<String, Texture> mapDebugMaskTextureCache =
            new LinkedHashMap<String, Texture>();
    private final Random sessionEnemyVisualRandom = new Random();
    private final Random sessionEnemyPolicyRandom = new Random();
    private final GlyphLayout glyphLayout = new GlyphLayout();
    private int sessionEnemyVisualPaletteSize = -1;
    private int sessionEnemyPolicyAvailableCount = -1;
    private final Color tint = new Color();
    private final Rectangle mapBounds = new Rectangle();
    private final Vector2 focusPoint = new Vector2();
    private final Vector2 growthPickupPosition = new Vector2();
    private final Vector2 pointPickupPosition = new Vector2();
    private final Vector2 checkpointTargetPosition = new Vector2();
    private final Vector2 raceTargetPosition = new Vector2();
    private final Vector2 raceSecondTargetPosition = new Vector2();
    private final Vector2 racePreviousTargetPosition = new Vector2();
    private final Vector2 raceRouteDirection = new Vector2();
    private final Vector2 sandboxSensorForward = new Vector2();
    private final Vector2 sandboxSensorSide = new Vector2();
    private final Vector2 sandboxSensorDirection = new Vector2();
    private final Vector2 sandboxSensorPoint = new Vector2();
    private final Vector2 sandboxSensorRoutePoint = new Vector2();
    private final Vector3 sandboxSensorProjection = new Vector3();
    private final Vector2 sandboxRlObservationForward = new Vector2();
    private final Vector2 sandboxRlObservationRouteTarget = new Vector2();
    private final Vector2 sandboxRlObservationSide = new Vector2();
    private final float[] sandboxRlObservation = new float[RL_OBSERVATION_SIZE];
    private final Vector2 pickupCandidate = new Vector2();
    private final Vector2 lastGrowthPickupPosition = new Vector2();
    private final Vector2 lastPointPickupPosition = new Vector2();
    private final Vector2 spawnCandidate = new Vector2();
    private final Array<SpawnPoint> roundSpawns = new Array<SpawnPoint>();
    private final Array<CarTemplate> roundGridOrder = new Array<CarTemplate>();
    private final Rectangle menuNewGameBounds = new Rectangle();
    private final Rectangle menuSandboxBounds = new Rectangle();
    private final Rectangle menuSandboxPrevBounds = new Rectangle();
    private final Rectangle menuSandboxNextBounds = new Rectangle();
    private final Rectangle menuMapsBounds = new Rectangle();
    private final Rectangle menuOptionsBounds = new Rectangle();
    private final Rectangle menuExitBounds = new Rectangle();
    private final Rectangle mapDebugPreviewBounds = new Rectangle();
    private final Rectangle mapDebugImageBounds = new Rectangle();
    private final Rectangle mapDebugPrevBounds = new Rectangle();
    private final Rectangle mapDebugNextBounds = new Rectangle();
    private final Rectangle mapDebugBackBounds = new Rectangle();
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
    private final Rectangle optionsDriverPolicyBounds = new Rectangle();
    private final Rectangle optionsDriverPolicyPrevBounds = new Rectangle();
    private final Rectangle optionsDriverPolicyNextBounds = new Rectangle();
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
    private final Rectangle sandboxPhysicsTunerBounds = new Rectangle();
    private final Rectangle sandboxPhysicsResetBounds = new Rectangle();
    private final Rectangle[] sandboxPhysicsDecreaseBounds =
            createRectangleArray(SANDBOX_PHYSICS_TUNER_COUNT);
    private final Rectangle[] sandboxPhysicsIncreaseBounds =
            createRectangleArray(SANDBOX_PHYSICS_TUNER_COUNT);
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
    private final Vector2 mapDebugPoint = new Vector2();
    private final Vector2 mapDebugTangent = new Vector2();
    private final Rectangle mapDebugMapBounds = new Rectangle();
    private final ImpactContactListener impactContactListener = new ImpactContactListener();
    private final Comparator<CarTemplate> leaderboardComparator = new Comparator<CarTemplate>() {
        @Override
        public int compare(CarTemplate left, CarTemplate right) {
            if (isLiveLapRaceMode()) {
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
    private CarPhysics sandboxPhysicsOverride = CarPhysics.DEFAULT;

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
    private float cameraTargetTransitionTimer;
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
    private int selectedDriverPolicyIndex = DEFAULT_DRIVER_POLICY_INDEX;
    private int selectedRaceLapCount = DEFAULT_RACE_LAPS;
    private int selectedSandboxMapIndex;
    private int mainMenuSelection;
    private int pauseMenuSelection;
    private int optionsMenuSelection;
    private int cameraTargetRosterIndex = -1;
    private Car boostedCar;
    private Car playerCar;
    private Car winner;
    private RlPolicy rlEnemyPolicy;
    private final Map<String, RlPolicy> rlPolicies = new LinkedHashMap<String, RlPolicy>();
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
        if (sandboxMode) {
            resetSandboxPhysicsTuning();
        }
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
        if (!sandbox) {
            return ArenaMaps.createDefaultSet(mapScale);
        }

        Array<ArenaMap> maps = createSandboxMapSet();
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
        sandboxMenuMaps.addAll(createSandboxMapSet());
        selectedSandboxMapIndex = clampSandboxMapIndex(selectedSandboxMapIndex);
    }

    private Array<ArenaMap> createSandboxMapSet() {
        Array<ArenaMap> maps = new Array<ArenaMap>();
        try {
            addUniqueSandboxMaps(maps, ArenaMaps.createDefaultSet(mapScale));
        } catch (RuntimeException exception) {
            if (Gdx.app != null) {
                Gdx.app.error("RatassGame", "Could not load sandbox game map list.", exception);
            }
        }
        try {
            addUniqueSandboxMaps(maps, ArenaMaps.createHeadlessTrainingSet(mapScale));
        } catch (RuntimeException exception) {
            if (Gdx.app != null) {
                Gdx.app.error("RatassGame", "Could not load sandbox training map list.", exception);
            }
        }
        return maps;
    }

    private void addUniqueSandboxMaps(Array<ArenaMap> target, Array<ArenaMap> source) {
        if (source == null) {
            return;
        }
        for (int i = 0; i < source.size; i++) {
            ArenaMap candidate = source.get(i);
            if (!hasSandboxMap(target, candidate)) {
                target.add(candidate);
            }
        }
    }

    private boolean hasSandboxMap(Array<ArenaMap> maps, ArenaMap candidate) {
        if (candidate == null) {
            return true;
        }
        String candidateId = candidate.getId();
        for (int i = 0; i < maps.size; i++) {
            ArenaMap existing = maps.get(i);
            if (existing == candidate) {
                return true;
            }
            if (existing != null
                    && candidateId != null
                    && candidateId.equals(existing.getId())) {
                return true;
            }
        }
        return false;
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
        rlPolicies.clear();
        RlPolicy legacyPolicy = loadRlPolicy(RL_ENEMY_POLICY_PATH, RL_LEGACY_POLICY_ID);
        RlPolicy firstProfilePolicy = null;

        for (int i = 0; i < DRIVER_POLICY_CHOICES.length; i++) {
            String policyId = DRIVER_POLICY_CHOICES[i].id;
            if (isManualDriverPolicyId(policyId)) {
                continue;
            }
            RlPolicy policy = loadRlPolicy(buildRlPolicyPath(policyId), policyId);
            if (policy != null) {
                rlPolicies.put(policyId, policy);
                if (firstProfilePolicy == null) {
                    firstProfilePolicy = policy;
                }
            }
        }
        return legacyPolicy == null ? firstProfilePolicy : legacyPolicy;
    }

    private String buildRlPolicyPath(String policyId) {
        return RL_POLICY_DIRECTORY + "/" + policyId + "/" + RL_POLICY_FILE_NAME;
    }

    private RlPolicy getRlPolicyById(String policyId) {
        if (isManualDriverPolicyId(policyId)) {
            return null;
        }
        RlPolicy policy = rlPolicies.get(policyId);
        return policy == null ? rlEnemyPolicy : policy;
    }

    private static boolean isManualDriverPolicyId(String policyId) {
        return policyId != null
                && MANUAL_DRIVER_POLICY_ID.equals(policyId.trim().toLowerCase(Locale.US));
    }

    private String getSelectedDriverPolicyId() {
        return getSelectedDriverPolicyChoice().id;
    }

    private DriverPolicyChoice getSelectedDriverPolicyChoice() {
        return DRIVER_POLICY_CHOICES[clampDriverPolicyIndex(selectedDriverPolicyIndex)];
    }

    private static int findDriverPolicyIndex(String policyId) {
        if (policyId == null) {
            return DEFAULT_DRIVER_POLICY_INDEX;
        }
        String lookup = policyId.trim().toLowerCase(Locale.US);
        for (int i = 0; i < DRIVER_POLICY_CHOICES.length; i++) {
            DriverPolicyChoice choice = DRIVER_POLICY_CHOICES[i];
            if (choice.id.equals(lookup) || choice.displayName.toLowerCase(Locale.US).equals(lookup)) {
                return i;
            }
        }
        return DEFAULT_DRIVER_POLICY_INDEX;
    }

    private static int clampDriverPolicyIndex(int policyIndex) {
        if (DRIVER_POLICY_CHOICES.length == 0) {
            return DEFAULT_DRIVER_POLICY_INDEX;
        }
        return MathUtils.clamp(policyIndex, 0, DRIVER_POLICY_CHOICES.length - 1);
    }

    private RlPolicy loadRlPolicy(String path, String policyId) {
        if (Gdx.files == null) {
            return null;
        }
        try {
            FileHandle policyFile = Gdx.files.internal(path);
            if (!policyFile.exists()) {
                return null;
            }
            RlPolicy policy = RlPolicy.fromJson(policyFile.readString("UTF-8"));
            if (policy.getObservationSize() != RL_OBSERVATION_SIZE
                    || policy.getActionSize() != RL_ACTION_SIZE) {
                Gdx.app.log(
                        "RatassGame",
                        "Ignoring RL policy "
                                + policyId
                                + " with observation/action size "
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
            Gdx.app.error("RatassGame", "Could not load RL policy " + policyId + " from " + path + ".", exception);
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
        if (choice == null || !themeHasCars(choice.name)) {
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

    private boolean themeHasCars(String themeName) {
        return themeAssetExists(themeName, THEME_CAR_SHEET_PATH)
                || themeAssetExists(themeName, THEME_FLAT_CAR_SHEET_PATH)
                || themeHasStandaloneCars(themeName);
    }

    private boolean themeHasStandaloneCars(String themeName) {
        for (int i = 0; i < MAX_CAR_VISUAL_COUNT; i++) {
            if (themeAssetExists(themeName, buildThemeCarFilePath(i))) {
                return true;
            }
        }
        return false;
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
        selectedDriverPolicyIndex =
                findDriverPolicyIndex(loadConfiguredProperty(DRIVER_POLICY_PROPERTY));
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
            selectedDriverPolicyIndex =
                    findDriverPolicyIndex(
                            preferences.getString(
                                    DRIVER_POLICY_PREF_KEY,
                                    getSelectedDriverPolicyId()));
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
        selectedDriverPolicyIndex = clampDriverPolicyIndex(selectedDriverPolicyIndex);
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
        preferences.putString(DRIVER_POLICY_PREF_KEY, getSelectedDriverPolicyId());
        preferences.putInteger(RACE_LAPS_PREF_KEY, selectedRaceLapCount);
        preferences.putInteger(SANDBOX_MAP_PREF_KEY, selectedSandboxMapIndex);
        preferences.flush();
    }

    private void createRoster() {
        roster.clear();
        CarVisual playerVisual = getPlayerCarVisual();
        RlPolicy playerPolicy = getRlPolicyById(getSelectedDriverPolicyId());
        boolean playerAutopilot = playerPolicy != null;
        addRosterTemplate(
                "You",
                true,
                new Color(playerVisual.color),
                playerVisual,
                "player",
                false,
                playerAutopilot,
                playerPolicy);

        int enemyCount = getConfiguredEnemyCount();
        for (int enemyIndex = 0; enemyIndex < enemyCount; enemyIndex++) {
            CarVisual visual = getEnemyCarVisual(enemyIndex);
            RlPolicy enemyPolicy = getEnemyDriverPolicy(enemyIndex);
            boolean modelControlledEnemy = enemyPolicy != null;
            addRosterTemplate(
                    getEnemyName(enemyIndex),
                    false,
                    new Color(visual.color),
                    visual,
                    "rl-" + enemyIndex,
                    !modelControlledEnemy,
                    modelControlledEnemy,
                    enemyPolicy);
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

    private RlPolicy getEnemyDriverPolicy(int enemyIndex) {
        ensureSessionEnemyPolicyIds(enemyIndex + 1);
        if (enemyIndex >= 0 && enemyIndex < sessionEnemyPolicyIds.size) {
            return getRlPolicyById(sessionEnemyPolicyIds.get(enemyIndex));
        }
        return rlEnemyPolicy;
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

    private void ensureSessionEnemyPolicyIds(int requiredCount) {
        int availableCount = getAvailableDriverPolicyCount();
        if (sessionEnemyPolicyAvailableCount != availableCount) {
            sessionEnemyPolicyAvailableCount = availableCount;
            sessionEnemyPolicyIds.clear();
            sessionEnemyPolicyPool.clear();
        }

        while (sessionEnemyPolicyIds.size < requiredCount) {
            if (sessionEnemyPolicyPool.size == 0) {
                refillSessionEnemyPolicyPool();
            }
            if (sessionEnemyPolicyPool.size == 0) {
                return;
            }
            sessionEnemyPolicyIds.add(sessionEnemyPolicyPool.pop());
        }
    }

    private int getAvailableDriverPolicyCount() {
        int count = 0;
        for (int i = 0; i < DRIVER_POLICY_CHOICES.length; i++) {
            String policyId = DRIVER_POLICY_CHOICES[i].id;
            if (rlPolicies.containsKey(policyId)) {
                count++;
            }
        }
        return count;
    }

    private void refillSessionEnemyPolicyPool() {
        sessionEnemyPolicyPool.clear();
        for (int i = 0; i < DRIVER_POLICY_CHOICES.length; i++) {
            String policyId = DRIVER_POLICY_CHOICES[i].id;
            if (rlPolicies.containsKey(policyId)) {
                sessionEnemyPolicyPool.add(policyId);
            }
        }
        for (int i = sessionEnemyPolicyPool.size - 1; i > 0; i--) {
            sessionEnemyPolicyPool.swap(i, sessionEnemyPolicyRandom.nextInt(i + 1));
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
        addRosterTemplate(
                name,
                playerControlled,
                color,
                visual,
                statsLabel,
                externallyControlled,
                modelControlled,
                null);
    }

    private void addRosterTemplate(
            String name,
            boolean playerControlled,
            Color color,
            CarVisual visual,
            String statsLabel,
            boolean externallyControlled,
            boolean modelControlled,
            RlPolicy rlPolicy) {
        roster.add(new CarTemplate(
                roster.size,
                name,
                playerControlled,
                color,
                visual,
                statsLabel,
                externallyControlled,
                modelControlled,
                rlPolicy,
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

        if (loadStandaloneThemeCarVisuals()) {
            return;
        }

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

    private boolean loadStandaloneThemeCarVisuals() {
        Array<FileHandle> carHandles = resolveStandaloneThemeCarHandles();
        if (carHandles.size == 0) {
            return false;
        }

        for (int i = 0; i < carHandles.size; i++) {
            FileHandle handle = carHandles.get(i);
            Pixmap source = null;
            try {
                source = new Pixmap(handle);
                themeCarVisuals.add(
                        new CarVisual(
                                THEME_CAR_DIRECTORY_PATH + "/" + handle.name(),
                                sampleSpriteColor(
                                        source,
                                        new Rectangle(
                                                0f,
                                                0f,
                                                source.getWidth(),
                                                source.getHeight()))));
            } catch (RuntimeException exception) {
                Gdx.app.error("RatassGame", "Could not load themed car " + handle.path(), exception);
            } finally {
                if (source != null) {
                    source.dispose();
                }
            }
        }
        return themeCarVisuals.size > 0;
    }

    private void ensureMenuCarPreviewLoaded() {
        if (configuredThemeName.equals(menuCarSheetThemeName)) {
            return;
        }

        disposeMenuCarPreview();
        menuCarSheetThemeName = configuredThemeName;

        if (loadStandaloneMenuCarPreview()) {
            return;
        }

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

    private boolean loadStandaloneMenuCarPreview() {
        Array<FileHandle> carHandles = resolveStandaloneThemeCarHandles();
        if (carHandles.size == 0) {
            return false;
        }

        int columnCount = Math.min(STANDALONE_CAR_PREVIEW_COLUMNS, carHandles.size);
        int rowCount = MathUtils.ceil(carHandles.size / (float) columnCount);
        Pixmap sheet = new Pixmap(
                columnCount * STANDALONE_CAR_PREVIEW_CELL_WIDTH,
                rowCount * STANDALONE_CAR_PREVIEW_CELL_HEIGHT,
                Pixmap.Format.RGBA8888);
        sheet.setColor(0f, 0f, 0f, 0f);
        sheet.fill();

        try {
            for (int i = 0; i < carHandles.size; i++) {
                FileHandle handle = carHandles.get(i);
                Pixmap car = null;
                try {
                    car = new Pixmap(handle);
                    int column = i % columnCount;
                    int row = i / columnCount;
                    sheet.drawPixmap(
                            car,
                            0,
                            0,
                            car.getWidth(),
                            car.getHeight(),
                            column * STANDALONE_CAR_PREVIEW_CELL_WIDTH,
                            row * STANDALONE_CAR_PREVIEW_CELL_HEIGHT,
                            STANDALONE_CAR_PREVIEW_CELL_WIDTH,
                            STANDALONE_CAR_PREVIEW_CELL_HEIGHT);
                    menuCarSheetSourceBounds.add(new Rectangle(
                            column * STANDALONE_CAR_PREVIEW_CELL_WIDTH,
                            row * STANDALONE_CAR_PREVIEW_CELL_HEIGHT,
                            STANDALONE_CAR_PREVIEW_CELL_WIDTH,
                            STANDALONE_CAR_PREVIEW_CELL_HEIGHT));
                } catch (RuntimeException exception) {
                    Gdx.app.error("RatassGame", "Could not load menu car " + handle.path(), exception);
                } finally {
                    if (car != null) {
                        car.dispose();
                    }
                }
            }

            if (menuCarSheetSourceBounds.size == 0) {
                return false;
            }
            menuCarSheetTexture = new Texture(sheet);
            menuCarSheetTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            clampSelectedPlayerCarToMenuPreview();
            return true;
        } finally {
            sheet.dispose();
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

    private String buildThemeCarFilePath(int index) {
        return THEME_CAR_DIRECTORY_PATH + "/" + String.format(Locale.ROOT, "%02d.png", index);
    }

    private Array<FileHandle> resolveStandaloneThemeCarHandles() {
        Array<FileHandle> handles = new Array<FileHandle>();
        for (int i = 0; i < MAX_CAR_VISUAL_COUNT; i++) {
            FileHandle handle = resolveThemedAssetHandle(buildThemeCarFilePath(i));
            if (handle != null && handle.exists()) {
                handles.add(handle);
            }
        }
        return handles;
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
        handleSandboxPhysicsTunerPointerInput();
        handleCameraTargetInput();
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
        boolean hasScrollbar = sidebarTablesScrollbarBounds.width > 0f;

        if (Gdx.input.justTouched()) {
            hudTouchPoint.set(Gdx.input.getX(), Gdx.input.getY(), 0f);
            hudViewport.unproject(hudTouchPoint);
            if (hasScrollbar && sidebarTablesScrollbarBounds.contains(hudTouchPoint.x, hudTouchPoint.y)) {
                sidebarTablesScrollbarDragging = true;
                sidebarTablesScrollbarGrabOffsetY = hudTouchPoint.y - sidebarTablesScrollbarBounds.y;
                return;
            }
            if (selectCameraTargetFromSidebarTables(
                    hudTouchPoint.x,
                    hudTouchPoint.y,
                    sidebarX,
                    sidebarWidth,
                    hudHeight)) {
                return;
            }
        }

        if (!Gdx.input.isTouched()) {
            sidebarTablesScrollbarDragging = false;
            return;
        }

        if (!hasScrollbar) {
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

    private boolean selectCameraTargetFromSidebarTables(
            float touchX,
            float touchY,
            float sidebarX,
            float sidebarWidth,
            float hudHeight) {
        refreshLeaderboardEntries();
        if (leaderboardEntries.size == 0
                || !getSidebarTablesViewportBounds(sidebarX, sidebarWidth, hudHeight, sidebarTablesViewportBounds)
                || !sidebarTablesViewportBounds.contains(touchX, touchY)) {
            return false;
        }

        clampSidebarTablesScrollOffset(sidebarX, sidebarWidth, hudHeight);

        float x = sidebarX + SIDEBAR_CONTENT_MARGIN;
        float viewportTop = sidebarTablesViewportBounds.y + sidebarTablesViewportBounds.height - 2f;
        float rowStep = getSidebarCompactLeaderboardRowStep();
        float headerStep = Math.max(16f, rowStep + 1f);
        boolean overflow = getSidebarTablesMaxScroll(sidebarX, sidebarWidth, hudHeight) > 0.5f;
        float contentWidth =
                Math.max(1f, sidebarWidth - SIDEBAR_CONTENT_MARGIN * 2f - (overflow ? 12f : 0f));
        float pointsWidth = 34f;
        float currentLapWidth = 62f;
        float bestLapWidth = 54f;
        float gap = 7f;
        float pointsX = x + contentWidth;
        float currentLapX = pointsX - pointsWidth - gap;
        float bestLapX = currentLapX - currentLapWidth - gap;
        float nameColumnRight = bestLapX - bestLapWidth - gap;
        if (touchX < x || touchX > nameColumnRight) {
            return false;
        }

        float contentY = headerStep;
        for (int i = 0; i < leaderboardEntries.size; i++) {
            CarTemplate template = leaderboardEntries.get(i);
            float rowY = viewportTop - contentY - sidebarTablesScrollOffset;
            if (touchY >= rowY - rowStep + 2f && touchY <= rowY + 4f) {
                return setCameraTargetTemplate(template, true);
            }
            contentY += rowStep;
        }
        return false;
    }

    private void handleSandboxPhysicsTunerPointerInput() {
        if (!sandboxMode || gameMode != GameMode.PLAYING || !Gdx.input.justTouched()) {
            return;
        }

        float hudWidth = hudViewport.getWorldWidth();
        float hudHeight = hudViewport.getWorldHeight();
        float playfieldWidth = playfieldHudWidth > 0f ? playfieldHudWidth : hudWidth;
        updateSandboxPhysicsTunerLayout(playfieldWidth, hudHeight);

        hudTouchPoint.set(Gdx.input.getX(), Gdx.input.getY(), 0f);
        hudViewport.unproject(hudTouchPoint);
        if (!sandboxPhysicsTunerBounds.contains(hudTouchPoint.x, hudTouchPoint.y)) {
            return;
        }

        if (sandboxPhysicsResetBounds.contains(hudTouchPoint.x, hudTouchPoint.y)) {
            resetSandboxPhysicsTuning();
            return;
        }

        for (int i = 0; i < SANDBOX_PHYSICS_TUNER_COUNT; i++) {
            if (sandboxPhysicsDecreaseBounds[i].contains(hudTouchPoint.x, hudTouchPoint.y)) {
                changeSandboxPhysicsTuning(i, -1);
                return;
            }
            if (sandboxPhysicsIncreaseBounds[i].contains(hudTouchPoint.x, hudTouchPoint.y)) {
                changeSandboxPhysicsTuning(i, 1);
                return;
            }
        }
    }

    private void updateSandboxPhysicsTunerLayout(float playfieldWidth, float hudHeight) {
        float width = Math.min(
                SANDBOX_PHYSICS_TUNER_WIDTH,
                Math.max(230f, playfieldWidth - SANDBOX_PHYSICS_TUNER_MARGIN * 2f));
        float height =
                SANDBOX_PHYSICS_TUNER_HEADER_HEIGHT
                        + SANDBOX_PHYSICS_TUNER_ROW_HEIGHT * SANDBOX_PHYSICS_TUNER_COUNT
                        + 12f;
        float x = SANDBOX_PHYSICS_TUNER_MARGIN;
        float y = Math.max(
                SANDBOX_PHYSICS_TUNER_MARGIN,
                hudHeight - height - 78f);
        sandboxPhysicsTunerBounds.set(x, y, width, height);

        float resetWidth = 58f;
        sandboxPhysicsResetBounds.set(
                x + width - resetWidth - 8f,
                y + height - SANDBOX_PHYSICS_TUNER_HEADER_HEIGHT + 5f,
                resetWidth,
                SANDBOX_PHYSICS_TUNER_HEADER_HEIGHT - 10f);

        float buttonSize = Math.min(24f, SANDBOX_PHYSICS_TUNER_ROW_HEIGHT - 5f);
        float rowTop = y + height - SANDBOX_PHYSICS_TUNER_HEADER_HEIGHT;
        for (int i = 0; i < SANDBOX_PHYSICS_TUNER_COUNT; i++) {
            float rowY = rowTop - (i + 1) * SANDBOX_PHYSICS_TUNER_ROW_HEIGHT;
            float buttonY = rowY + (SANDBOX_PHYSICS_TUNER_ROW_HEIGHT - buttonSize) * 0.5f;
            sandboxPhysicsDecreaseBounds[i].set(
                    x + width - buttonSize * 2f - 17f,
                    buttonY,
                    buttonSize,
                    buttonSize);
            sandboxPhysicsIncreaseBounds[i].set(
                    x + width - buttonSize - 8f,
                    buttonY,
                    buttonSize,
                    buttonSize);
        }
    }

    private void resetSandboxPhysicsTuning() {
        sandboxPhysicsOverride = CarPhysics.DEFAULT;
        applySandboxPhysicsOverride();
    }

    private void changeSandboxPhysicsTuning(int index, int direction) {
        float value =
                MathUtils.clamp(
                        getSandboxPhysicsTuningValue(index)
                                + SANDBOX_PHYSICS_TUNER_STEP[index] * direction,
                        SANDBOX_PHYSICS_TUNER_MIN[index],
                        SANDBOX_PHYSICS_TUNER_MAX[index]);
        setSandboxPhysicsTuningValue(index, value);
    }

    private float getSandboxPhysicsTuningValue(int index) {
        switch (index) {
            case SANDBOX_PHYSICS_HORSE_POWER:
                return sandboxPhysicsOverride.horsePower;
            case SANDBOX_PHYSICS_BRAKE_FORCE:
                return sandboxPhysicsOverride.brakeForce;
            case SANDBOX_PHYSICS_STEERING_TORQUE:
                return sandboxPhysicsOverride.steeringTorque;
            case SANDBOX_PHYSICS_WHEEL_GRIP:
                return sandboxPhysicsOverride.wheelGrip;
            case SANDBOX_PHYSICS_MASS_MULTIPLIER:
                return sandboxPhysicsOverride.massMultiplier;
            default:
                return 0f;
        }
    }

    private void setSandboxPhysicsTuningValue(int index, float value) {
        float horsePower = sandboxPhysicsOverride.horsePower;
        float brakeForce = sandboxPhysicsOverride.brakeForce;
        float steeringTorque = sandboxPhysicsOverride.steeringTorque;
        float wheelGrip = sandboxPhysicsOverride.wheelGrip;
        float massMultiplier = sandboxPhysicsOverride.massMultiplier;

        switch (index) {
            case SANDBOX_PHYSICS_HORSE_POWER:
                horsePower = value;
                break;
            case SANDBOX_PHYSICS_BRAKE_FORCE:
                brakeForce = value;
                break;
            case SANDBOX_PHYSICS_STEERING_TORQUE:
                steeringTorque = value;
                break;
            case SANDBOX_PHYSICS_WHEEL_GRIP:
                wheelGrip = value;
                break;
            case SANDBOX_PHYSICS_MASS_MULTIPLIER:
                massMultiplier = value;
                break;
            default:
                return;
        }

        sandboxPhysicsOverride =
                sandboxPhysicsOverride.withSandboxTuning(
                        massMultiplier,
                        horsePower,
                        brakeForce,
                        steeringTorque,
                        wheelGrip);
        applySandboxPhysicsOverride();
    }

    private void applySandboxPhysicsOverride() {
        if (!sandboxMode) {
            return;
        }
        for (int i = 0; i < roster.size; i++) {
            roster.get(i).physics = sandboxPhysicsOverride;
        }
        for (int i = 0; i < cars.size; i++) {
            Car car = cars.get(i);
            if (car.active && car.body != null) {
                car.rebuildCollisionFixture();
            }
        }
    }

    private void handleMenuInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
                || Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
            if (gameMode == GameMode.OPTIONS_MENU) {
                closeOptionsMenu();
                return;
            }
            if (gameMode == GameMode.MAPS_MENU) {
                gameMode = GameMode.MAIN_MENU;
                mainMenuSelection = MAIN_MENU_MAPS_SELECTION;
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
        } else if (gameMode == GameMode.MAPS_MENU) {
            handleMapsMenuInput();
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
            } else if (mainMenuSelection == MAIN_MENU_MAPS_SELECTION) {
                openMapsMenu();
            } else if (mainMenuSelection == MAIN_MENU_OPTIONS_SELECTION) {
                openOptionsMenu(false);
            } else {
                Gdx.app.exit();
            }
        }
    }

    private void handleMapsMenuInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)
                || Gdx.input.isKeyJustPressed(Input.Keys.A)) {
            changeSandboxMapSelection(-1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)
                || Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            changeSandboxMapSelection(1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            gameMode = GameMode.MAIN_MENU;
            mainMenuSelection = MAIN_MENU_MAPS_SELECTION;
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
        } else if (optionsMenuSelection == OPTIONS_DRIVER_POLICY_SELECTION && canChangeCarSetupOptions()) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)
                    || Gdx.input.isKeyJustPressed(Input.Keys.A)) {
                changeDriverPolicy(-1);
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)
                    || Gdx.input.isKeyJustPressed(Input.Keys.D)) {
                changeDriverPolicy(1);
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
            } else if (optionsMenuSelection == OPTIONS_DRIVER_POLICY_SELECTION && canChangeCarSetupOptions()) {
                changeDriverPolicy(1);
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

    private void openMapsMenu() {
        optionsOpenedFromPause = false;
        mainMenuSelection = MAIN_MENU_MAPS_SELECTION;
        gameMode = GameMode.MAPS_MENU;
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
            } else if (menuMapsBounds.contains(x, y)) {
                mainMenuSelection = MAIN_MENU_MAPS_SELECTION;
                openMapsMenu();
            } else if (menuOptionsBounds.contains(x, y)) {
                mainMenuSelection = MAIN_MENU_OPTIONS_SELECTION;
                openOptionsMenu(false);
            } else if (menuExitBounds.contains(x, y)) {
                mainMenuSelection = MAIN_MENU_EXIT_SELECTION;
                Gdx.app.exit();
            }
            return;
        }

        if (gameMode == GameMode.MAPS_MENU) {
            if (mapDebugPrevBounds.contains(x, y)) {
                changeSandboxMapSelection(-1);
            } else if (mapDebugNextBounds.contains(x, y)) {
                changeSandboxMapSelection(1);
            } else if (mapDebugBackBounds.contains(x, y)) {
                gameMode = GameMode.MAIN_MENU;
                mainMenuSelection = MAIN_MENU_MAPS_SELECTION;
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
        } else if (carSetupEnabled && optionsDriverPolicyPrevBounds.contains(x, y)) {
            optionsMenuSelection = OPTIONS_DRIVER_POLICY_SELECTION;
            changeDriverPolicy(-1);
        } else if (carSetupEnabled
                && (optionsDriverPolicyNextBounds.contains(x, y)
                        || optionsDriverPolicyBounds.contains(x, y))) {
            optionsMenuSelection = OPTIONS_DRIVER_POLICY_SELECTION;
            changeDriverPolicy(1);
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
                        MENU_MIN_SIDE_MARGIN + (menuButtonHeight + menuButtonGap) * 4f);

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
        menuMapsBounds.set(
                buttonX,
                firstButtonY - (menuButtonHeight + menuButtonGap) * 2f,
                buttonWidth,
                menuButtonHeight);
        menuOptionsBounds.set(
                buttonX,
                firstButtonY - (menuButtonHeight + menuButtonGap) * 3f,
                buttonWidth,
                menuButtonHeight);
        menuExitBounds.set(
                buttonX,
                firstButtonY - (menuButtonHeight + menuButtonGap) * 4f,
                buttonWidth,
                menuButtonHeight);

        float mapPanelMargin = Math.max(16f, Math.min(36f, width * 0.035f));
        float mapControlsHeight = Math.min(MENU_BUTTON_HEIGHT, Math.max(42f, height * 0.08f));
        float mapControlsGap = Math.min(MENU_BUTTON_GAP, Math.max(8f, height * 0.018f));
        float mapTitleSpace = Math.max(76f, height * 0.12f);
        float mapControlsSpace = mapControlsHeight + mapControlsGap + mapPanelMargin;
        mapDebugPreviewBounds.set(
                mapPanelMargin,
                mapControlsSpace,
                Math.max(1f, width - mapPanelMargin * 2f),
                Math.max(1f, height - mapControlsSpace - mapTitleSpace));
        float mapStepWidth = Math.min(86f, Math.max(48f, mapDebugPreviewBounds.width * 0.12f));
        float backWidth = Math.min(180f, Math.max(110f, mapDebugPreviewBounds.width * 0.22f));
        float controlsY = mapPanelMargin * 0.55f;
        mapDebugPrevBounds.set(mapPanelMargin, controlsY, mapStepWidth, mapControlsHeight);
        mapDebugNextBounds.set(
                mapPanelMargin + mapStepWidth + mapControlsGap,
                controlsY,
                mapStepWidth,
                mapControlsHeight);
        mapDebugBackBounds.set(
                width - mapPanelMargin - backWidth,
                controlsY,
                backWidth,
                mapControlsHeight);

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
        optionsDriverPolicyBounds.set(
                rowX,
                rowY - (rowHeight + rowGap) * 3f,
                rowWidth,
                rowHeight);
        optionsDriverPolicyPrevBounds.set(
                rowX,
                rowY - (rowHeight + rowGap) * 3f,
                stepButtonWidth,
                rowHeight);
        optionsDriverPolicyNextBounds.set(
                rowX + rowWidth - stepButtonWidth,
                rowY - (rowHeight + rowGap) * 3f,
                stepButtonWidth,
                rowHeight);
        optionsLapsBounds.set(
                rowX,
                rowY - (rowHeight + rowGap) * 4f,
                rowWidth,
                rowHeight);
        optionsLapsPrevBounds.set(
                rowX,
                rowY - (rowHeight + rowGap) * 4f,
                stepButtonWidth,
                rowHeight);
        optionsLapsNextBounds.set(
                rowX + rowWidth - stepButtonWidth,
                rowY - (rowHeight + rowGap) * 4f,
                stepButtonWidth,
                rowHeight);
        optionsCameraBounds.set(
                rowX,
                rowY - (rowHeight + rowGap) * 5f,
                rowWidth,
                rowHeight);
        optionsZoomBounds.set(
                rowX,
                rowY - (rowHeight + rowGap) * 6f,
                rowWidth,
                rowHeight);
        optionsZoomOutBounds.set(
                rowX,
                rowY - (rowHeight + rowGap) * 6f,
                stepButtonWidth,
                rowHeight);
        optionsZoomInBounds.set(
                rowX + rowWidth - stepButtonWidth,
                rowY - (rowHeight + rowGap) * 6f,
                stepButtonWidth,
                rowHeight);
        optionsBackBounds.set(
                rowX,
                rowY - (rowHeight + rowGap) * 7f,
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

    private void handleCameraTargetInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.HOME)) {
            resetCameraTargetToPlayer(true);
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.PAGE_DOWN)) {
            changeCameraTarget(1);
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.PAGE_UP)) {
            changeCameraTarget(-1);
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
            boolean reverse =
                    Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
                            || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
            changeCameraTarget(reverse ? -1 : 1);
        }
    }

    private void changeCameraTarget(int direction) {
        if (direction == 0 || roster.size == 0) {
            return;
        }

        refreshLeaderboardEntries();
        if (leaderboardEntries.size == 0) {
            return;
        }

        int startIndex = findCameraTargetLeaderboardIndex();
        if (startIndex < 0) {
            startIndex = findPlayerLeaderboardIndex();
        }
        if (startIndex < 0) {
            startIndex = 0;
        }

        for (int step = 1; step <= leaderboardEntries.size; step++) {
            int nextIndex = wrapLeaderboardIndex(startIndex + direction * step);
            CarTemplate template = leaderboardEntries.get(nextIndex);
            if (template != null && isCameraFollowable(template.currentCar)) {
                int rosterIndex = findRosterIndex(template);
                if (rosterIndex >= 0) {
                    setCameraTargetRosterIndex(rosterIndex, true);
                }
                return;
            }
        }
    }

    private int wrapLeaderboardIndex(int index) {
        int wrapped = index % leaderboardEntries.size;
        return wrapped < 0 ? wrapped + leaderboardEntries.size : wrapped;
    }

    private void resetCameraTargetToPlayer(boolean animate) {
        int playerIndex = findPlayerRosterIndex();
        setCameraTargetRosterIndex(playerIndex >= 0 ? playerIndex : -1, animate);
    }

    private boolean setCameraTargetTemplate(CarTemplate template, boolean animate) {
        if (template == null || !isCameraFollowable(template.currentCar)) {
            return false;
        }
        int rosterIndex = findRosterIndex(template);
        if (rosterIndex < 0) {
            return false;
        }
        setCameraTargetRosterIndex(rosterIndex, animate);
        return true;
    }

    private void setCameraTargetRosterIndex(int rosterIndex, boolean animate) {
        if (cameraTargetRosterIndex == rosterIndex) {
            return;
        }
        cameraTargetRosterIndex = rosterIndex;
        if (animate && cameraInitialized) {
            cameraTargetTransitionTimer = CAMERA_TARGET_SWITCH_DURATION;
        } else {
            cameraTargetTransitionTimer = 0f;
        }
    }

    private int findPlayerRosterIndex() {
        for (int i = 0; i < roster.size; i++) {
            CarTemplate template = roster.get(i);
            if (template.playerControlled) {
                return i;
            }
        }
        return -1;
    }

    private int findRosterIndex(CarTemplate target) {
        if (target == null) {
            return -1;
        }
        for (int i = 0; i < roster.size; i++) {
            if (roster.get(i) == target) {
                return i;
            }
        }
        return -1;
    }

    private int findCameraTargetLeaderboardIndex() {
        Car cameraTarget = getCameraTargetCar();
        if (cameraTarget == null || cameraTarget.template == null) {
            return -1;
        }
        return findLeaderboardIndex(cameraTarget.template);
    }

    private int findPlayerLeaderboardIndex() {
        for (int i = 0; i < leaderboardEntries.size; i++) {
            CarTemplate template = leaderboardEntries.get(i);
            if (template.playerControlled) {
                return i;
            }
        }
        return -1;
    }

    private int findLeaderboardIndex(CarTemplate target) {
        if (target == null) {
            return -1;
        }
        for (int i = 0; i < leaderboardEntries.size; i++) {
            if (leaderboardEntries.get(i) == target) {
                return i;
            }
        }
        return -1;
    }

    private Car getCameraTargetCar() {
        if (cameraTargetRosterIndex >= 0 && cameraTargetRosterIndex < roster.size) {
            Car car = roster.get(cameraTargetRosterIndex).currentCar;
            if (isCameraFollowable(car)) {
                return car;
            }
        }
        if (isCameraFollowable(playerCar)) {
            return playerCar;
        }
        for (int i = 0; i < roster.size; i++) {
            Car car = roster.get(i).currentCar;
            if (isCameraFollowable(car)) {
                return car;
            }
        }
        return null;
    }

    private boolean isCameraFollowable(Car car) {
        return car != null && car.active && car.body != null;
    }

    private boolean isCameraTargetTemplate(CarTemplate template) {
        Car target = getCameraTargetCar();
        return target != null && target.template == template;
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

    private void changeDriverPolicy(int delta) {
        if (DRIVER_POLICY_CHOICES.length == 0) {
            selectedDriverPolicyIndex = DEFAULT_DRIVER_POLICY_INDEX;
            return;
        }

        selectedDriverPolicyIndex =
                (selectedDriverPolicyIndex + delta + DRIVER_POLICY_CHOICES.length)
                        % DRIVER_POLICY_CHOICES.length;
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
        } else if (gameMode == GameMode.MAPS_MENU) {
            drawMapsMenu(hudWidth, hudHeight);
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
        drawMenuButton(menuMapsBounds, "Maps", mainMenuSelection == MAIN_MENU_MAPS_SELECTION);
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

    private void drawMapsMenu(float hudWidth, float hudHeight) {
        ArenaMap map = getSelectedSandboxMenuMap();
        drawMapDebugPreview(map);
        drawMenuStepButton(mapDebugPrevBounds, "<", true);
        drawMenuStepButton(mapDebugNextBounds, ">", true);
        drawMenuButton(mapDebugBackBounds, "Back", false);

        spriteBatch.begin();
        titleFont.setColor(0.98f, 0.92f, 0.76f, 1f);
        drawTextCentered(titleFont, "Maps", hudWidth * 0.5f, hudHeight - Math.max(30f, hudHeight * 0.045f));
        hudFont.setColor(0.78f, 0.86f, 0.90f, 1f);
        String mapText = map == null
                ? "No maps"
                : (selectedSandboxMapIndex + 1) + "/" + Math.max(1, sandboxMenuMaps.size)
                        + "  " + map.getId() + "  " + map.getName();
        drawTextCentered(
                hudFont,
                mapText,
                hudWidth * 0.5f,
                mapDebugPreviewBounds.y + mapDebugPreviewBounds.height + 20f);
        labelFont.setColor(0.80f, 0.88f, 0.92f, 1f);
        labelFont.draw(
                spriteBatch,
                "magenta route  red off-road segment  cyan direction  green spawns/goals  orange route-marker numbers",
                mapDebugPreviewBounds.x,
                Math.max(mapDebugPreviewBounds.y - 8f, mapDebugBackBounds.y + mapDebugBackBounds.height + 12f));
        spriteBatch.end();
    }

    private void drawMapDebugPreview(ArenaMap map) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.025f, 0.032f, 0.040f, 0.96f);
        shapeRenderer.rect(
                mapDebugPreviewBounds.x,
                mapDebugPreviewBounds.y,
                mapDebugPreviewBounds.width,
                mapDebugPreviewBounds.height);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.62f, 0.70f, 0.75f, 0.45f);
        shapeRenderer.rect(
                mapDebugPreviewBounds.x,
                mapDebugPreviewBounds.y,
                mapDebugPreviewBounds.width,
                mapDebugPreviewBounds.height);
        shapeRenderer.end();

        if (map == null) {
            Gdx.gl.glDisable(GL20.GL_BLEND);
            return;
        }

        Texture maskTexture = getOrCreateMapDebugMaskTexture(map);
        calculateMapDebugImageBounds(maskTexture);
        if (maskTexture != null) {
            spriteBatch.begin();
            spriteBatch.setColor(1f, 1f, 1f, 1f);
            spriteBatch.draw(
                    maskTexture,
                    mapDebugImageBounds.x,
                    mapDebugImageBounds.y,
                    mapDebugImageBounds.width,
                    mapDebugImageBounds.height);
            spriteBatch.end();
        }

        map.getBounds(mapDebugMapBounds);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        drawMapDebugRoute(map);
        drawMapDebugCheckpoints(map);
        drawMapDebugSpawns(map);
        shapeRenderer.end();

        spriteBatch.begin();
        drawMapDebugLabels(map);
        spriteBatch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void calculateMapDebugImageBounds(Texture texture) {
        float padding = Math.max(8f, Math.min(mapDebugPreviewBounds.width, mapDebugPreviewBounds.height) * 0.025f);
        float availableWidth = Math.max(1f, mapDebugPreviewBounds.width - padding * 2f);
        float availableHeight = Math.max(1f, mapDebugPreviewBounds.height - padding * 2f);
        float aspect = texture == null ? 1f : texture.getWidth() / (float) texture.getHeight();
        float width = availableWidth;
        float height = width / aspect;
        if (height > availableHeight) {
            height = availableHeight;
            width = height * aspect;
        }
        mapDebugImageBounds.set(
                mapDebugPreviewBounds.x + (mapDebugPreviewBounds.width - width) * 0.5f,
                mapDebugPreviewBounds.y + (mapDebugPreviewBounds.height - height) * 0.5f,
                width,
                height);
    }

    private Texture getOrCreateMapDebugMaskTexture(ArenaMap map) {
        if (map == null || Gdx.gl == null) {
            return null;
        }
        String path = getMapDebugMaskPath(map);
        if (path == null || path.length() == 0) {
            return null;
        }
        Texture texture = mapDebugMaskTextureCache.get(path);
        if (texture != null) {
            return texture;
        }
        FileHandle handle = resolveMapDebugMaskHandle(path, map.getId());
        if (handle == null || !handle.exists()) {
            return null;
        }
        try {
            texture = new Texture(handle);
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            mapDebugMaskTextureCache.put(path, texture);
            return texture;
        } catch (RuntimeException exception) {
            Gdx.app.error("RatassGame", "Could not load map debug mask " + handle.path(), exception);
            return null;
        }
    }

    private String getMapDebugMaskPath(ArenaMap map) {
        if (map == null) {
            return "";
        }
        String surfacePath = map.getSurfaceImagePath();
        if (surfacePath != null && surfacePath.length() > 0) {
            return surfacePath;
        }
        String mapId = map.getId();
        return mapId == null || mapId.length() == 0 ? "" : "assets/maps/" + mapId + "_mask.png";
    }

    private FileHandle resolveMapDebugMaskHandle(String path, String mapId) {
        if (path != null && path.length() > 0) {
            FileHandle handle = Gdx.files.internal(path);
            if (handle != null && handle.exists()) {
                return handle;
            }
            handle = Gdx.files.local(path);
            if (handle != null && handle.exists()) {
                return handle;
            }
        }
        if (mapId != null && mapId.length() > 0) {
            FileHandle handle = Gdx.files.local("assets/maps/" + mapId + "_mask.png");
            if (handle.exists()) {
                return handle;
            }
            handle = Gdx.files.local("tools/rl/trainingMaps/" + mapId + "_mask.png");
            if (handle.exists()) {
                return handle;
            }
            handle = Gdx.files.internal("maps/" + mapId + "_mask.png");
            if (handle.exists()) {
                return handle;
            }
        }
        return null;
    }

    private void drawMapDebugRoute(ArenaMap map) {
        if (map == null || !map.hasRoute()) {
            return;
        }
        float routeLength = map.getRouteLength();
        if (routeLength <= 0.001f) {
            return;
        }
        float sampleStep = Math.max(0.10f, MENU_MAP_DEBUG_SAMPLE_STEP);
        int sampleCount = MathUtils.clamp(MathUtils.ceil(routeLength / sampleStep), 12, 2200);
        float lineWidth =
                MathUtils.clamp(
                        Math.min(mapDebugImageBounds.width, mapDebugImageBounds.height) * 0.0048f,
                        MENU_MAP_DEBUG_MIN_LINE_WIDTH,
                        MENU_MAP_DEBUG_MAX_LINE_WIDTH);

        float previousX = 0f;
        float previousY = 0f;
        for (int i = 0; i <= sampleCount; i++) {
            float progress = routeLength * i / sampleCount;
            map.findRoutePoint(progress, mapDebugPoint);
            float x = mapDebugWorldToHudX(mapDebugPoint.x);
            float y = mapDebugWorldToHudY(mapDebugPoint.y);
            if (i > 0) {
                drawMapDebugSegment(previousX, previousY, x, y, lineWidth + 3.0f, 0.02f, 0.02f, 0.025f, 0.92f);
                drawMapDebugSegment(previousX, previousY, x, y, lineWidth, 1.00f, 0.00f, 0.86f, 0.96f);
            }
            previousX = x;
            previousY = y;
        }
        map.findRoutePoint(0f, mapDebugPoint);
        float firstX = mapDebugWorldToHudX(mapDebugPoint.x);
        float firstY = mapDebugWorldToHudY(mapDebugPoint.y);
        drawMapDebugSegment(previousX, previousY, firstX, firstY, lineWidth + 3.0f, 0.02f, 0.02f, 0.025f, 0.92f);
        drawMapDebugSegment(previousX, previousY, firstX, firstY, lineWidth, 1.00f, 0.00f, 0.86f, 0.96f);

        float arrowStep = Math.max(3f, MENU_MAP_DEBUG_ARROW_STEP);
        for (float progress = 0f; progress < routeLength; progress += arrowStep) {
            map.findRoutePoint(progress, mapDebugPoint);
            map.findRouteTangent(progress, mapDebugTangent);
            drawMapDebugArrow(
                    mapDebugWorldToHudX(mapDebugPoint.x),
                    mapDebugWorldToHudY(mapDebugPoint.y),
                    mapDebugTangent.x,
                    mapDebugTangent.y,
                    lineWidth * 2.8f);
        }

        drawMapDebugUnsafeRouteMarkerSegments(map, routeLength, sampleStep, lineWidth);
    }

    private void drawMapDebugUnsafeRouteMarkerSegments(
            ArenaMap map,
            float routeLength,
            float sampleStep,
            float lineWidth) {
        int markerCount = map.getRouteMarkerPointCount();
        if (markerCount < 2 || routeLength <= 0.001f) {
            return;
        }
        for (int i = 0; i < markerCount; i++) {
            float startProgress = map.getRouteMarkerProgress(i);
            float endProgress = map.getRouteMarkerProgress((i + 1) % markerCount);
            float delta = endProgress - startProgress;
            if (i == markerCount - 1 || delta <= 0.001f) {
                delta += routeLength;
            }
            if (delta <= 0.001f || delta > routeLength + 0.001f) {
                continue;
            }
            if (!routeMarkerIntervalCutsOffRoad(map, startProgress, delta, sampleStep)) {
                continue;
            }
            drawMapDebugRouteProgressSegment(
                    map,
                    startProgress,
                    delta,
                    Math.max(2f, sampleStep * 0.55f),
                    lineWidth + 5.0f,
                    0.02f,
                    0.02f,
                    0.025f,
                    0.96f);
            drawMapDebugRouteProgressSegment(
                    map,
                    startProgress,
                    delta,
                    Math.max(2f, sampleStep * 0.55f),
                    lineWidth + 2.0f,
                    1.00f,
                    0.06f,
                    0.03f,
                    0.98f);
        }
    }

    private boolean routeMarkerIntervalCutsOffRoad(
            ArenaMap map,
            float startProgress,
            float delta,
            float sampleStep) {
        float detectionStep = Math.max(0.08f, Math.min(0.25f, sampleStep * 0.5f));
        int sampleCount = MathUtils.clamp(MathUtils.ceil(delta / detectionStep), 2, 2400);
        for (int sample = 0; sample <= sampleCount; sample++) {
            float progress = startProgress + delta * sample / sampleCount;
            map.findRoutePoint(progress, mapDebugPoint);
            if (!map.approximateSupports(mapDebugPoint.x, mapDebugPoint.y)) {
                return true;
            }
        }
        return false;
    }

    private void drawMapDebugRouteProgressSegment(
            ArenaMap map,
            float startProgress,
            float delta,
            float sampleStep,
            float lineWidth,
            float red,
            float green,
            float blue,
            float alpha) {
        int sampleCount = MathUtils.clamp(MathUtils.ceil(delta / Math.max(0.08f, sampleStep)), 2, 1800);
        float previousX = 0f;
        float previousY = 0f;
        for (int sample = 0; sample <= sampleCount; sample++) {
            float progress = startProgress + delta * sample / sampleCount;
            map.findRoutePoint(progress, mapDebugPoint);
            float x = mapDebugWorldToHudX(mapDebugPoint.x);
            float y = mapDebugWorldToHudY(mapDebugPoint.y);
            if (sample > 0) {
                drawMapDebugSegment(previousX, previousY, x, y, lineWidth, red, green, blue, alpha);
            }
            previousX = x;
            previousY = y;
        }
    }

    private void drawMapDebugSegment(
            float x1,
            float y1,
            float x2,
            float y2,
            float width,
            float red,
            float green,
            float blue,
            float alpha) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length <= 0.001f) {
            return;
        }
        drawRotatedRect(
                (x1 + x2) * 0.5f,
                (y1 + y2) * 0.5f,
                length,
                width,
                MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees,
                red,
                green,
                blue,
                alpha);
    }

    private void drawMapDebugArrow(float x, float y, float tangentX, float tangentY, float size) {
        float length = (float) Math.sqrt(tangentX * tangentX + tangentY * tangentY);
        if (length <= 0.0001f) {
            return;
        }
        float tx = tangentX / length;
        float ty = tangentY / length;
        float nx = -ty;
        float ny = tx;
        shapeRenderer.setColor(0.00f, 0.86f, 1.00f, 0.92f);
        shapeRenderer.triangle(
                x + tx * size,
                y + ty * size,
                x - tx * size * 0.55f + nx * size * 0.42f,
                y - ty * size * 0.55f + ny * size * 0.42f,
                x - tx * size * 0.55f - nx * size * 0.42f,
                y - ty * size * 0.55f - ny * size * 0.42f);
    }

    private void drawMapDebugSpawns(ArenaMap map) {
        if (map == null) {
            return;
        }
        for (int i = 0; i < map.getSpawnCount(); i++) {
            SpawnPoint spawn = map.getSpawn(i);
            float x = mapDebugWorldToHudX(spawn.x);
            float y = mapDebugWorldToHudY(spawn.y);
            float forwardX = -MathUtils.sin(spawn.angleRad);
            float forwardY = MathUtils.cos(spawn.angleRad);
            shapeRenderer.setColor(0.08f, 0.88f, 0.24f, 0.98f);
            shapeRenderer.circle(x, y, 3.8f, 14);
            drawMapDebugArrow(x, y, forwardX, forwardY, 8.5f);
        }
    }

    private void drawMapDebugCheckpoints(ArenaMap map) {
        if (map == null) {
            return;
        }
        for (int i = 0; i < map.getCheckpointCount(); i++) {
            SpawnPoint checkpoint = map.getCheckpoint(i);
            float x = mapDebugWorldToHudX(checkpoint.x);
            float y = mapDebugWorldToHudY(checkpoint.y);
            shapeRenderer.setColor(0.12f, 0.82f, 0.34f, 0.96f);
            if (checkpoint.hasGate) {
                drawMapDebugSegment(
                        mapDebugWorldToHudX(checkpoint.gateStartX),
                        mapDebugWorldToHudY(checkpoint.gateStartY),
                        mapDebugWorldToHudX(checkpoint.gateEndX),
                        mapDebugWorldToHudY(checkpoint.gateEndY),
                        3.2f,
                        0.12f,
                        0.82f,
                        0.34f,
                        0.96f);
            }
            shapeRenderer.circle(x, y, 4.4f, 18);
        }
    }

    private void drawMapDebugLabels(ArenaMap map) {
        if (map == null) {
            return;
        }
        labelFont.setColor(1f, 1f, 1f, 1f);
        for (int i = 0; i < map.getCheckpointCount(); i++) {
            SpawnPoint checkpoint = map.getCheckpoint(i);
            drawTextWithShadow(
                    labelFont,
                    String.valueOf(i + 1),
                    mapDebugWorldToHudX(checkpoint.x) + 6f,
                    mapDebugWorldToHudY(checkpoint.y) + 8f);
        }
        labelFont.setColor(1.00f, 0.66f, 0.08f, 1f);
        for (int i = 0; i < map.getRouteMarkerPointCount(); i++) {
            map.getRouteMarkerPoint(i, mapDebugPoint);
            drawTextWithShadow(
                    labelFont,
                    String.valueOf(i + 1),
                    mapDebugWorldToHudX(mapDebugPoint.x) + 5f,
                    mapDebugWorldToHudY(mapDebugPoint.y) + 5f);
        }
        if (map.hasRoute()) {
            labelFont.setColor(1f, 1f, 1f, 1f);
            float routeLength = map.getRouteLength();
            for (int i = 0; i < 10; i++) {
                float progress = routeLength * i / 10f;
                map.findRoutePoint(progress, mapDebugPoint);
                drawTextWithShadow(
                        labelFont,
                        i == 0 ? "0%" : (i * 10) + "%",
                        mapDebugWorldToHudX(mapDebugPoint.x) + 7f,
                        mapDebugWorldToHudY(mapDebugPoint.y) - 7f);
            }
        }
        hudFont.setColor(0.96f, 0.90f, 0.72f, 1f);
        String info =
                "spawns=" + map.getSpawnCount()
                        + "  goals=" + map.getCheckpointCount()
                        + "  markers=" + map.getRouteMarkerPointCount()
                        + "  route=" + String.format(Locale.ROOT, "%.1f", map.getRouteLength());
        drawTextWithShadow(
                hudFont,
                info,
                mapDebugPreviewBounds.x + 12f,
                mapDebugPreviewBounds.y + mapDebugPreviewBounds.height - 14f);
    }

    private float mapDebugWorldToHudX(float worldX) {
        if (mapDebugMapBounds.width <= 0.0001f) {
            return mapDebugImageBounds.x;
        }
        return mapDebugImageBounds.x
                + (worldX - mapDebugMapBounds.x) * mapDebugImageBounds.width / mapDebugMapBounds.width;
    }

    private float mapDebugWorldToHudY(float worldY) {
        if (mapDebugMapBounds.height <= 0.0001f) {
            return mapDebugImageBounds.y;
        }
        return mapDebugImageBounds.y
                + (worldY - mapDebugMapBounds.y) * mapDebugImageBounds.height / mapDebugMapBounds.height;
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
                optionsDriverPolicyBounds,
                optionsMenuSelection == OPTIONS_DRIVER_POLICY_SELECTION,
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
            drawMenuStepButton(
                    optionsDriverPolicyPrevBounds,
                    "<",
                    optionsMenuSelection == OPTIONS_DRIVER_POLICY_SELECTION);
            drawMenuStepButton(
                    optionsDriverPolicyNextBounds,
                    ">",
                    optionsMenuSelection == OPTIONS_DRIVER_POLICY_SELECTION);
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
                "Driver",
                optionsDriverPolicyBounds.x + optionsDriverPolicyPrevBounds.width + 18f,
                optionsDriverPolicyBounds.y + optionsDriverPolicyBounds.height * 0.62f);
        setOptionValueColor(carSetupEnabled);
        drawTextRight(
                hudFont,
                buildDriverPolicyMenuValue(),
                optionsDriverPolicyBounds.x
                        + optionsDriverPolicyBounds.width
                        - optionsDriverPolicyNextBounds.width
                        - 18f,
                optionsDriverPolicyBounds.y + optionsDriverPolicyBounds.height * 0.62f);

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

    private String buildDriverPolicyMenuValue() {
        return getSelectedDriverPolicyChoice().displayName;
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
        prepareSlipstreamSnapshots();
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
                    car.template.rlPolicy,
                    rlTrainingMode);
        }
        if (!allowControl && !roundOver) {
            freezeCarsForCountdown();
        }

        for (int i = 0; i < cars.size; i++) {
            cars.get(i).capturePreviousTransform();
        }
        world.step(delta, VELOCITY_ITERATIONS, POSITION_ITERATIONS);
        applyTrackLimitSlowdowns();

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

    private void prepareSlipstreamSnapshots() {
        if (cars.size <= 1) {
            return;
        }
        for (int i = 0; i < cars.size; i++) {
            cars.get(i).prepareSlipstreamSnapshot(currentMap);
        }
    }

    private void applyTrackLimitSlowdowns() {
        for (int i = 0; i < cars.size; i++) {
            Car car = cars.get(i);
            if (!car.active || car.body == null) {
                continue;
            }
            car.applyTrackLimitSlowdown(currentMap);
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
        cameraTargetTransitionTimer = 0f;
        checkpointTargetSequence = 0;
        playerCar = null;
        finishPositionCounter = 0;
        roundNumber++;
        buildRoundSpawns(roster.size, roundSpawns);
        buildRoundGridOrder(roundGridOrder);
        int fixedRaceStartCheckpointIndex =
                shouldUseFixedRaceStartCheckpoint()
                        ? findFixedRaceStartCheckpointIndex(roundSpawns)
                        : -1;

        for (int i = 0; i < roster.size; i++) {
            CarTemplate template = roundGridOrder.get(i);
            template.roundGridPosition = i + 1;
            template.roundFinishPosition = 0;
            template.lastRoundAwardedPoints = 0;
            template.roundPickupPoints = 0;
            template.roundRaceLap = 0;
            template.roundRaceCheckpointsCompleted = 0;
            template.roundRaceLastRouteProgress = 0f;
            template.roundRaceStartRouteProgress = 0f;
            template.roundRaceStartRouteIndex = 0;
            template.roundRaceRouteIndex = 0;
            template.roundRaceDistanceThisLap = 0f;
            template.roundRaceTotalRouteProgress = 0f;
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
            initializeRoundRaceRouteState(template, spawnPoint);
            if (template.playerControlled) {
                playerCar = car;
            }
        }

        cameraInitialized = false;
        resetCameraTargetToPlayer(false);
        updateWorldCamera();
        warmArenaSurfaceTextures();
        invalidateLeaderboard();
    }

    private void buildRoundGridOrder(Array<CarTemplate> out) {
        out.clear();
        out.addAll(roster);
        if (!isLiveLapRaceMode()) {
            return;
        }
        if (!hasPreviousRaceGridOrder()) {
            out.shuffle();
            return;
        }
        out.sort(new Comparator<CarTemplate>() {
            @Override
            public int compare(CarTemplate left, CarTemplate right) {
                int positionCompare =
                        compareFinishPosition(left.nextGridPosition, right.nextGridPosition);
                if (positionCompare != 0) {
                    return positionCompare;
                }
                if (left.playerControlled != right.playerControlled) {
                    return left.playerControlled ? -1 : 1;
                }
                return left.name.compareTo(right.name);
            }
        });
    }

    private boolean hasPreviousRaceGridOrder() {
        for (int i = 0; i < roster.size; i++) {
            if (roster.get(i).nextGridPosition > 0) {
                return true;
            }
        }
        return false;
    }

    private void initializeRoundRaceRouteState(CarTemplate template, SpawnPoint spawnPoint) {
        if (template == null || currentMap == null || !currentMap.hasRoute()) {
            return;
        }
        float progress =
                spawnPoint == null
                        ? 0f
                        : currentMap.findRouteProgress(spawnPoint.x, spawnPoint.y);
        float startProgress = 0f;
        if (currentMap.getCheckpointCount() > 0) {
            int startCheckpointIndex =
                    MathUtils.clamp(
                            template.roundRaceStartCheckpointIndex,
                            0,
                            currentMap.getCheckpointCount() - 1);
            SpawnPoint startCheckpoint = currentMap.getCheckpoint(startCheckpointIndex);
            startProgress = currentMap.findRouteProgress(startCheckpoint.x, startCheckpoint.y);
        }
        template.roundRaceLastRouteProgress = progress;
        template.roundRaceStartRouteProgress = startProgress;
        template.roundRaceStartRouteIndex = currentMap.getRouteProgressIndex(startProgress);
        template.roundRaceRouteIndex = 0;
        template.roundRaceDistanceThisLap = 0f;
        template.roundRaceTotalRouteProgress = 0f;
        template.roundRaceCurrentLapStartTime = roundTimer;
    }

    private boolean shouldUseFixedRaceStartCheckpoint() {
        return currentMap != null
                && currentMap.getCheckpointCount() > 1
                && !rlTrainingRandomSpawnLocations
                && (rlTrainingRaceMode || isLiveLapRaceMode());
    }

    private int findFixedRaceStartCheckpointIndex(Array<SpawnPoint> spawnPoints) {
        return currentMap == null || currentMap.getCheckpointCount() <= 1 || spawnPoints.size == 0
                ? -1
                : 0;
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
        return !rlTrainingMode && currentMap != null && currentMap.hasRoute();
    }

    private boolean setRaceTargetsForCar(Car car, Vector2 targetOut, Vector2 secondTargetOut) {
        if (car == null || car.template == null || currentMap == null || targetOut == null) {
            return false;
        }
        if (currentMap.hasRoute() && car.body != null) {
            raceRouteDirection.set(car.body.getWorldVector(raceRouteDirection.set(0f, 1f)));
            float routeProgress =
                    currentMap.findRouteProgressNear(
                            car.body.getPosition(),
                            raceRouteDirection,
                            car.template.roundRaceLastRouteProgress);
            currentMap.findRoutePoint(routeProgress + RL_ROUTE_LOOKAHEAD_DISTANCE, targetOut);
            if (secondTargetOut != null) {
                currentMap.findRoutePoint(routeProgress + RL_ROUTE_LOOKAHEAD_DISTANCE * 2f, secondTargetOut);
            }
            return true;
        }
        if (currentMap.getCheckpointCount() <= 1) {
            return false;
        }
        int checkpointCount = currentMap.getCheckpointCount();
        int checkpointIndex =
                MathUtils.clamp(car.template.roundRaceNextCheckpointIndex, 0, checkpointCount - 1);
        SpawnPoint checkpoint = currentMap.getCheckpoint(checkpointIndex);
        targetOut.set(checkpoint.x, checkpoint.y);

        int secondCheckpointIndex = (checkpointIndex + 1) % checkpointCount;
        if (secondTargetOut != null) {
            SpawnPoint secondCheckpoint = currentMap.getCheckpoint(secondCheckpointIndex);
            secondTargetOut.set(secondCheckpoint.x, secondCheckpoint.y);
        }
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
                || !currentMap.hasRoute()) {
            return;
        }

        raceRouteDirection.set(car.body.getWorldVector(raceRouteDirection.set(0f, 1f)));
        float previousRouteProgress = car.template.roundRaceLastRouteProgress;
        float routeLength = currentMap.getRouteLength();
        int routeIndexCount = currentMap.getRouteProgressIndexCount();
        float routeProgress =
                currentMap.findRouteProgressNear(
                        car.body.getPosition(),
                        raceRouteDirection,
                        previousRouteProgress);
        float delta = currentMap.routeProgressDelta(previousRouteProgress, routeProgress);
        if (Float.isNaN(delta) || Float.isInfinite(delta)) {
            return;
        }
        if (Math.abs(delta) <= RACE_ROUTE_MIN_DELTA) {
            car.template.roundRaceLastRouteProgress = routeProgress;
            return;
        }

        int routeIndex = findRaceRouteIndex(car.template, routeProgress, routeIndexCount, delta);
        if (!car.template.roundRaceLapCounterCrossed) {
            car.template.roundRaceLastRouteProgress = routeProgress;
            if (!hasCrossedRaceRouteStart(
                    previousRouteProgress,
                    routeProgress,
                    car.template.roundRaceStartRouteProgress,
                    delta)) {
                return;
            }

            car.template.roundRaceLapCounterCrossed = true;
            car.template.roundRaceRouteIndex = routeIndex;
            car.template.roundRaceCurrentLapStartTime = roundTimer;
            updateRaceTotalRouteProgress(car.template, routeLength, routeIndexCount);
            invalidateLeaderboard();
            return;
        }

        car.template.roundRaceLastRouteProgress = routeProgress;
        if (hasCrossedRaceRouteStart(
                previousRouteProgress,
                routeProgress,
                car.template.roundRaceStartRouteProgress,
                delta)) {
            recordCompletedRaceLap(car.template);
            car.template.roundRaceLap++;
            if (car.template.roundRaceLap >= getRaceLapsToWin()) {
                markRaceFinished(car);
                return;
            }
        }
        car.template.roundRaceRouteIndex = routeIndex;
        updateRaceTotalRouteProgress(car.template, routeLength, routeIndexCount);
        invalidateLeaderboard();
    }

    private int findRaceRouteIndex(
            CarTemplate template,
            float routeProgress,
            int routeIndexCount,
            float routeDelta) {
        if (template == null || currentMap == null || routeIndexCount <= 0) {
            return 0;
        }
        int routeIndex = currentMap.getRouteProgressIndex(routeProgress);
        int relativeIndex = routeIndex - template.roundRaceStartRouteIndex;
        if (relativeIndex < 0) {
            relativeIndex += routeIndexCount;
        }
        if (template.roundRaceLapCounterCrossed
                && routeDelta < -RACE_ROUTE_MIN_DELTA
                && template.roundRaceRouteIndex < routeIndexCount / 4
                && relativeIndex > routeIndexCount * 3 / 4) {
            return 0;
        }
        return MathUtils.clamp(relativeIndex, 0, Math.max(0, routeIndexCount - 1));
    }

    private boolean hasCrossedRaceRouteStart(
            float previousRouteProgress,
            float routeProgress,
            float startRouteProgress,
            float delta) {
        if (currentMap == null || !currentMap.hasRoute() || delta <= RACE_ROUTE_MIN_DELTA) {
            return false;
        }
        float distanceToStart =
                currentMap.routeProgressDelta(previousRouteProgress, startRouteProgress);
        float distanceToCurrent = currentMap.routeProgressDelta(previousRouteProgress, routeProgress);
        return distanceToStart > RACE_ROUTE_MIN_DELTA
                && distanceToCurrent > 0f
                && distanceToStart <= distanceToCurrent + RACE_ROUTE_MIN_DELTA;
    }

    private void updateRaceTotalRouteProgress(
            CarTemplate template,
            float routeLength,
            int routeIndexCount) {
        if (template == null) {
            return;
        }
        int safeIndexCount = Math.max(1, routeIndexCount);
        template.roundRaceRouteIndex =
                MathUtils.clamp(template.roundRaceRouteIndex, 0, safeIndexCount - 1);
        float routeFraction = template.roundRaceRouteIndex / (float) safeIndexCount;
        float safeRouteLength = Math.max(0.001f, routeLength);
        template.roundRaceDistanceThisLap = routeFraction * safeRouteLength;
        template.roundRaceTotalRouteProgress =
                template.roundRaceLap * safeIndexCount + template.roundRaceRouteIndex;
        template.roundRaceCheckpointsCompleted = MathUtils.floor(template.roundRaceTotalRouteProgress);
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
        if (!checkpoint.hasGate) {
            return car.body.getPosition().dst2(checkpoint.x, checkpoint.y)
                    <= RACE_CHECKPOINT_RADIUS * RACE_CHECKPOINT_RADIUS;
        }
        return hasCrossedRaceCheckpointGate(car, checkpoint);
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
        if (left.roundRaceLapCounterCrossed != right.roundRaceLapCounterCrossed) {
            return left.roundRaceLapCounterCrossed ? -1 : 1;
        }
        int lapCompare = right.roundRaceLap - left.roundRaceLap;
        if (lapCompare != 0) {
            return lapCompare;
        }
        int routeIndexCompare = right.roundRaceRouteIndex - left.roundRaceRouteIndex;
        if (routeIndexCompare != 0) {
            return routeIndexCompare;
        }
        int gridPositionCompare =
                compareFinishPosition(left.roundGridPosition, right.roundGridPosition);
        if (gridPositionCompare != 0) {
            return gridPositionCompare;
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
            if (isLiveLapRaceMode()) {
                template.nextGridPosition = template.roundFinishPosition;
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
            cameraTargetTransitionTimer = 0f;
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
        boolean cameraTargetTransitionActive = cameraTargetTransitionTimer > 0f && cameraInitialized;

        Car cameraTargetCar = getCameraTargetCar();
        boolean carCameraActive = isCameraFollowable(cameraTargetCar);

        if (carCameraActive) {
            Vector2 carPosition = cameraTargetCar.getRenderPosition();
            Vector2 carVelocity = cameraTargetCar.body.getLinearVelocity();
            float carSpeed = carVelocity.len();
            float speedFactor = MathUtils.clamp(carSpeed / cameraTargetCar.getForwardMaxSpeed(), 0f, 1f);
            targetZoom = MathUtils.clamp(
                    PLAYER_CAMERA_ZOOM
                            + speedFactor * PLAYER_CAMERA_SPEED_ZOOM_OUT
                            + MathUtils.clamp(cameraTargetCar.getSizeScale() - 1f, 0f, 1f)
                                    * PLAYER_CAMERA_GROWTH_ZOOM_OUT,
                    PLAYER_CAMERA_ZOOM,
                    PLAYER_CAMERA_MAX_ZOOM);
            if (followCameraBehind) {
                cameraTargetCar.getRenderForwardDirection(cameraForwardDirection);
                if (!cameraForwardDirection.isZero(0.0001f)) {
                    cameraForwardDirection.nor();
                } else {
                    cameraForwardDirection.set(cameraSmoothedForwardDirection);
                }

                if (!cameraInitialized || delta <= 0f) {
                    cameraSmoothedForwardDirection.set(cameraForwardDirection);
                } else if (carSpeed >= PLAYER_CAMERA_DIRECTION_MIN_SPEED) {
                    float spinDamping =
                            1f
                                    / (1f
                                            + Math.abs(cameraTargetCar.body.getAngularVelocity())
                                                    * PLAYER_CAMERA_DIRECTION_SPIN_DAMPING);
                    float impactDamping =
                            cameraTargetCar.getRecentImpactTime() > 0f
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
                cameraTargetPosition.set(carPosition).add(cameraChaseOffset);
            } else {
                cameraLookAhead.set(carVelocity).scl(PLAYER_CAMERA_LOOK_AHEAD_TIME);
                if (cameraLookAhead.len2() > PLAYER_CAMERA_MAX_LOOK_AHEAD * PLAYER_CAMERA_MAX_LOOK_AHEAD) {
                    cameraLookAhead.setLength(PLAYER_CAMERA_MAX_LOOK_AHEAD);
                }
                cameraSmoothedForwardDirection.set(0f, 1f);
                cameraTargetPosition.set(carPosition).add(cameraLookAhead);
            }
        } else {
            cameraSmoothedForwardDirection.set(0f, 1f);
        }

        if (carCameraActive) {
            targetZoom = applyConfiguredCameraZoom(targetZoom);
        }
        if (cameraTargetTransitionActive) {
            targetZoom =
                    Math.min(
                            PLAYER_CAMERA_MAX_ZOOM / MIN_CAMERA_ZOOM,
                            targetZoom + CAMERA_TARGET_SWITCH_ZOOM_OUT);
        }
        if (!carCameraActive || !followCameraBehind) {
            clampCameraToArena(cameraTargetPosition, targetZoom);
        }

        if (!cameraInitialized || delta <= 0f) {
            cameraSmoothedPosition.set(cameraTargetPosition);
            smoothedCameraZoom = targetZoom;
            cameraInitialized = true;
        } else {
            float followSpeed =
                    cameraTargetTransitionActive
                            ? CAMERA_TARGET_SWITCH_LERP_SPEED
                            : PLAYER_CAMERA_FOLLOW_LERP_SPEED;
            float alpha = 1f - (float) Math.exp(-followSpeed * delta);
            cameraSmoothedPosition.lerp(cameraTargetPosition, alpha);
            smoothedCameraZoom += (targetZoom - smoothedCameraZoom) * alpha;
        }

        if (!carCameraActive || !followCameraBehind) {
            clampCameraToArena(cameraSmoothedPosition, smoothedCameraZoom);
        }
        applyWorldCamera(smoothedCameraZoom);
        if (cameraTargetTransitionTimer > 0f && delta > 0f) {
            cameraTargetTransitionTimer = Math.max(0f, cameraTargetTransitionTimer - delta);
        }
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

        Array<SpawnPoint> orderedSpawns = new Array<SpawnPoint>(currentMap.getSpawnCount());
        for (int i = 0; i < currentMap.getSpawnCount(); i++) {
            SpawnPoint seed = currentMap.getSpawn(i);
            SpawnPoint routeAlignedSpawn = raceRouteAlignedSpawnPoint(seed.x, seed.y, seed);
            orderedSpawns.add(routeAlignedSpawn == null ? seed : routeAlignedSpawn);
        }
        sortRaceGridSpawnsFrontFirst(orderedSpawns);

        for (int i = 0; i < count; i++) {
            out.add(orderedSpawns.get(i));
        }
        return out.size == count;
    }

    private void sortRaceGridSpawnsFrontFirst(Array<SpawnPoint> spawns) {
        if (spawns.size <= 1) {
            return;
        }

        float forwardX = 0f;
        float forwardY = 0f;
        for (int i = 0; i < spawns.size; i++) {
            SpawnPoint spawn = spawns.get(i);
            forwardX += -MathUtils.sin(spawn.angleRad);
            forwardY += MathUtils.cos(spawn.angleRad);
        }
        float length = (float) Math.sqrt(forwardX * forwardX + forwardY * forwardY);
        if (length <= 0.0001f) {
            return;
        }

        final float raceForwardX = forwardX / length;
        final float raceForwardY = forwardY / length;
        final float raceSideX = -raceForwardY;
        final float raceSideY = raceForwardX;
        spawns.sort(new Comparator<SpawnPoint>() {
            @Override
            public int compare(SpawnPoint left, SpawnPoint right) {
                float leftForward = left.x * raceForwardX + left.y * raceForwardY;
                float rightForward = right.x * raceForwardX + right.y * raceForwardY;
                int forwardCompare = Float.compare(rightForward, leftForward);
                if (forwardCompare != 0) {
                    return forwardCompare;
                }

                float leftSide = left.x * raceSideX + left.y * raceSideY;
                float rightSide = right.x * raceSideX + right.y * raceSideY;
                return Float.compare(leftSide, rightSide);
            }
        });
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

            SpawnPoint spawnPoint =
                    rlTrainingRaceMode || isLiveLapRaceMode()
                            ? randomRaceSpawnPoint(spawnCandidate.x, spawnCandidate.y)
                            : randomSpawnPoint(spawnCandidate.x, spawnCandidate.y);
            if (spawnPoint == null) {
                continue;
            }
            out.add(spawnPoint);
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
        return raceRouteAlignedSpawnPoint(x, y, null);
    }

    private SpawnPoint raceRouteAlignedSpawnPoint(float x, float y, SpawnPoint fallback) {
        if (currentMap != null && currentMap.hasRoute()) {
            float progress;
            if (fallback == null) {
                progress = currentMap.findRouteProgress(x, y);
            } else {
                raceTargetPosition.set(
                        -MathUtils.sin(fallback.angleRad),
                        MathUtils.cos(fallback.angleRad));
                progress =
                        currentMap.findRouteProgress(
                                x,
                                y,
                                raceTargetPosition.x,
                                raceTargetPosition.y);
            }
            currentMap.findRouteTangent(progress, raceSecondTargetPosition);
            if (!raceSecondTargetPosition.isZero(0.0001f)) {
                return SpawnPoint.facingPoint(
                        x,
                        y,
                        x + raceSecondTargetPosition.x,
                        y + raceSecondTargetPosition.y);
            }
        }

        if (currentMap == null || currentMap.getCheckpointCount() <= 1) {
            return fallback == null ? randomSpawnPoint(x, y) : fallback;
        }

        int checkpointIndex = findRandomRaceSpawnCheckpointIndex(x, y);
        if (checkpointIndex < 0) {
            return null;
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
        if (checkpointCount <= 1) {
            return -1;
        }

        spawnCandidate.set(x, y);
        int bestIndex = -1;
        float bestScore = Float.MAX_VALUE;
        for (int i = 0; i < checkpointCount; i++) {
            SpawnPoint checkpoint = currentMap.getCheckpoint(i);
            SpawnPoint previous =
                    currentMap.getCheckpoint((i + checkpointCount - 1) % checkpointCount);
            racePreviousTargetPosition.set(previous.x, previous.y);
            float dx = checkpoint.x - x;
            float dy = checkpoint.y - y;
            float directDistance = (float) Math.sqrt(dx * dx + dy * dy);
            if (directDistance <= 0.0001f) {
                continue;
            }

            raceTargetPosition.set(checkpoint.x, checkpoint.y);
            // Random route-target stages start inside the route segment that ends at this target.
            float segmentDistance =
                    currentMap.estimateDriveDistance(
                            racePreviousTargetPosition,
                            raceTargetPosition,
                            RL_ROUTE_MARGIN);
            float routeDistance =
                    currentMap.estimateDriveDistance(spawnCandidate, raceTargetPosition, RL_ROUTE_MARGIN);
            float routeDistanceFromPrevious =
                    currentMap.estimateDriveDistance(
                            racePreviousTargetPosition,
                            spawnCandidate,
                            RL_ROUTE_MARGIN);
            if (segmentDistance <= 0.0001f) {
                continue;
            }
            float segmentProgress = routeDistanceFromPrevious / segmentDistance;
            if (segmentProgress < RL_RANDOM_SPAWN_MIN_SEGMENT_PROGRESS
                    || segmentProgress > RL_RANDOM_SPAWN_MAX_SEGMENT_PROGRESS) {
                continue;
            }

            float segmentError =
                    Math.abs(routeDistanceFromPrevious + routeDistance - segmentDistance);
            float segmentErrorLimit =
                    Math.max(
                            RL_RANDOM_SPAWN_SEGMENT_ERROR_MIN,
                            segmentDistance * RL_RANDOM_SPAWN_SEGMENT_ERROR_RATIO);
            if (segmentError > segmentErrorLimit) {
                continue;
            }

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
            float score =
                    routeDistance
                            + segmentError * RL_RANDOM_SPAWN_SEGMENT_ERROR_PENALTY
                            + backwardPenalty
                            + nearCheckpointPenalty;
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
        if (sandboxMode) {
            drawSandboxRouteLine();
        }
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
        drawHalloweenStormOverlay();
        shapeRenderer.end();

        if (sandboxMode) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            drawSandboxRlSensorRays();
            shapeRenderer.end();
        }

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawSandboxRouteLine() {
        if (currentMap == null || !currentMap.hasRoute()) {
            return;
        }

        float routeLength = currentMap.getRouteLength();
        if (routeLength <= 0.001f) {
            return;
        }

        int sampleCount =
                MathUtils.clamp(
                        MathUtils.ceil(routeLength / SANDBOX_ROUTE_LINE_SAMPLE_STEP),
                        12,
                        2200);
        currentMap.findRoutePoint(0f, sandboxSensorRoutePoint);
        float previousX = sandboxSensorRoutePoint.x;
        float previousY = sandboxSensorRoutePoint.y;
        for (int i = 1; i <= sampleCount; i++) {
            float progress = routeLength * i / sampleCount;
            currentMap.findRoutePoint(progress, sandboxSensorPoint);
            drawSandboxRouteSegment(
                    previousX,
                    previousY,
                    sandboxSensorPoint.x,
                    sandboxSensorPoint.y,
                    SANDBOX_ROUTE_LINE_WIDTH + 0.10f,
                    0.02f,
                    0.02f,
                    0.03f,
                    0.78f);
            drawSandboxRouteSegment(
                    previousX,
                    previousY,
                    sandboxSensorPoint.x,
                    sandboxSensorPoint.y,
                    SANDBOX_ROUTE_LINE_WIDTH,
                    1.00f,
                    0.00f,
                    0.86f,
                    0.92f);
            previousX = sandboxSensorPoint.x;
            previousY = sandboxSensorPoint.y;
        }

        shapeRenderer.setColor(0.20f, 1f, 0.30f, 0.90f);
        shapeRenderer.circle(sandboxSensorRoutePoint.x, sandboxSensorRoutePoint.y, 0.18f, 16);
    }

    private void drawSandboxRouteSegment(
            float x1,
            float y1,
            float x2,
            float y2,
            float width,
            float red,
            float green,
            float blue,
            float alpha) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length <= 0.001f) {
            return;
        }
        drawRotatedRect(
                (x1 + x2) * 0.5f,
                (y1 + y2) * 0.5f,
                length,
                width,
                MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees,
                red,
                green,
                blue,
                alpha);
    }

    private void drawHalloweenStormOverlay() {
        if (!isHalloweenTheme() || worldCamera == null) {
            return;
        }

        float visibleWidth = worldCamera.viewportWidth * worldCamera.zoom;
        float visibleHeight = worldCamera.viewportHeight * worldCamera.zoom;
        float coverSize =
                (float) Math.sqrt(visibleWidth * visibleWidth + visibleHeight * visibleHeight) * 1.18f;
        float left = worldCamera.position.x - coverSize * 0.5f;
        float bottom = worldCamera.position.y - coverSize * 0.5f;
        float rainLength = Math.max(0.75f, coverSize * 0.030f);
        float rainWidth = Math.max(0.018f, coverSize * 0.0010f);

        for (int i = 0; i < 44; i++) {
            float xPhase = wrap01(hash01(i * 17, i * 37, 113) + effectClock * 0.055f);
            float yPhase = wrap01(hash01(i * 29, i * 11, 197) - effectClock * 0.34f);
            float x = left + xPhase * coverSize;
            float y = bottom + yPhase * coverSize;
            float alpha = HALLOWEEN_WORLD_RAIN_ALPHA * (0.45f + hash01(i * 5, i * 19, 71) * 0.55f);
            drawRotatedRect(
                    x,
                    y,
                    rainLength,
                    rainWidth,
                    -66f,
                    0.62f,
                    0.58f,
                    0.72f,
                    alpha);
        }

    }

    private float wrap01(float value) {
        return value - MathUtils.floor(value);
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

        if (isHalloweenTheme()) {
            spriteBatch.setColor(
                    HALLOWEEN_CIRCUIT_TINT_R,
                    HALLOWEEN_CIRCUIT_TINT_G,
                    HALLOWEEN_CIRCUIT_TINT_B,
                    1f);
        } else {
            spriteBatch.setColor(1f, 1f, 1f, 1f);
        }
        spriteBatch.draw(
                arenaSurfaceTexture,
                drawX,
                drawY,
                drawWidth,
                drawHeight);
        spriteBatch.setColor(1f, 1f, 1f, 1f);
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

            drawSlipstreamEffect(car, centerX, centerY, carWidth, carHeight, angleDeg, i);

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

    private void drawSlipstreamEffect(
            Car car,
            float centerX,
            float centerY,
            float carWidth,
            float carHeight,
            float angleDeg,
            int carIndex) {
        float boost = car.getSlipstreamBoost();
        if (boost <= 0.01f) {
            return;
        }

        float pulse = 0.5f + 0.5f * MathUtils.sin(effectClock * 12.0f + carIndex * 0.73f);
        float alpha = MathUtils.clamp(0.10f + boost * 0.28f + pulse * 0.05f, 0f, 0.42f);
        float streamLength = carHeight * (1.15f + boost * 0.85f);
        float streamWidth = carWidth * (0.055f + boost * 0.025f);
        float driftPhase = wrap01(effectClock * 2.4f + carIndex * 0.17f);

        for (int lane = -1; lane <= 1; lane++) {
            float sideOffset = lane * carWidth * 0.36f;
            float lanePhase = wrap01(driftPhase + lane * 0.21f + 0.33f);
            float backOffset = -carHeight * (0.92f + lanePhase * 0.92f);
            float laneAlpha = alpha * (lane == 0 ? 0.95f : 0.58f);
            drawOffsetRotatedRect(
                    centerX,
                    centerY,
                    sideOffset,
                    backOffset,
                    streamWidth,
                    streamLength * (0.72f + lanePhase * 0.28f),
                    angleDeg,
                    1f,
                    1f,
                    1f,
                    laneAlpha);
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

        drawCarTelemetryBars(playfieldWidth, hudHeight);

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
        String cameraTargetText = buildCameraTargetText();
        if (cameraTargetText.length() > 0) {
            hudFont.setColor(0.68f, 0.88f, 1f, 1f);
            hudFont.draw(spriteBatch, cameraTargetText, 22f, topHudLineY - topHudLineStep * 2f);
        }

        drawSidebarSummary(sidebarX, sidebarWidth, hudHeight);
        drawSidebarTables(sidebarX, sidebarWidth, hudHeight);
        drawSidebarMinimapOverlay(sidebarX, sidebarWidth, hudHeight);
        drawSidebarFooter(sidebarX, sidebarWidth);
        drawCarLabels();
        if (sandboxMode) {
            drawSandboxRlSensorHudLabels(playfieldWidth, hudHeight);
        }

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
        } else if (roundOver) {
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

        if (sandboxMode) {
            drawSandboxPhysicsTuner(playfieldWidth, hudHeight);
        }

        if (touchControlsEnabled) {
            drawTouchControls();
        }
    }

    private void drawSandboxPhysicsTuner(float playfieldWidth, float hudHeight) {
        updateSandboxPhysicsTunerLayout(playfieldWidth, hudHeight);
        if (sandboxPhysicsTunerBounds.width <= 0f || sandboxPhysicsTunerBounds.height <= 0f) {
            return;
        }

        shapeRenderer.setProjectionMatrix(hudCamera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.035f, 0.045f, 0.060f, 0.88f);
        shapeRenderer.rect(
                sandboxPhysicsTunerBounds.x,
                sandboxPhysicsTunerBounds.y,
                sandboxPhysicsTunerBounds.width,
                sandboxPhysicsTunerBounds.height);
        shapeRenderer.setColor(0.98f, 0.84f, 0.28f, 0.16f);
        shapeRenderer.rect(
                sandboxPhysicsTunerBounds.x,
                sandboxPhysicsTunerBounds.y + sandboxPhysicsTunerBounds.height - 3f,
                sandboxPhysicsTunerBounds.width,
                3f);

        for (int i = 0; i < SANDBOX_PHYSICS_TUNER_COUNT; i++) {
            float rowY =
                    sandboxPhysicsTunerBounds.y
                            + sandboxPhysicsTunerBounds.height
                            - SANDBOX_PHYSICS_TUNER_HEADER_HEIGHT
                            - (i + 1) * SANDBOX_PHYSICS_TUNER_ROW_HEIGHT;
            shapeRenderer.setColor(i % 2 == 0 ? 0.10f : 0.075f, 0.105f, 0.125f, 0.62f);
            shapeRenderer.rect(
                    sandboxPhysicsTunerBounds.x + 7f,
                    rowY + 2f,
                    sandboxPhysicsTunerBounds.width - 14f,
                    SANDBOX_PHYSICS_TUNER_ROW_HEIGHT - 4f);
            drawSandboxPhysicsTunerButtonShape(sandboxPhysicsDecreaseBounds[i], 0.12f, 0.16f, 0.20f);
            drawSandboxPhysicsTunerButtonShape(sandboxPhysicsIncreaseBounds[i], 0.15f, 0.20f, 0.16f);
        }
        drawSandboxPhysicsTunerButtonShape(sandboxPhysicsResetBounds, 0.18f, 0.13f, 0.11f);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.80f, 0.88f, 0.94f, 0.34f);
        shapeRenderer.rect(
                sandboxPhysicsTunerBounds.x,
                sandboxPhysicsTunerBounds.y,
                sandboxPhysicsTunerBounds.width,
                sandboxPhysicsTunerBounds.height);
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        spriteBatch.begin();
        hudFont.setColor(0.96f, 0.92f, 0.78f, 1f);
        hudFont.draw(
                spriteBatch,
                "sandbox physics",
                sandboxPhysicsTunerBounds.x + 10f,
                sandboxPhysicsTunerBounds.y + sandboxPhysicsTunerBounds.height - 10f);
        hudFont.setColor(1f, 0.86f, 0.62f, 1f);
        drawTextCentered(
                hudFont,
                "reset",
                sandboxPhysicsResetBounds.x + sandboxPhysicsResetBounds.width * 0.5f,
                sandboxPhysicsResetBounds.y + sandboxPhysicsResetBounds.height * 0.68f);

        for (int i = 0; i < SANDBOX_PHYSICS_TUNER_COUNT; i++) {
            float rowY =
                    sandboxPhysicsTunerBounds.y
                            + sandboxPhysicsTunerBounds.height
                            - SANDBOX_PHYSICS_TUNER_HEADER_HEIGHT
                            - (i + 1) * SANDBOX_PHYSICS_TUNER_ROW_HEIGHT;
            float textY = rowY + SANDBOX_PHYSICS_TUNER_ROW_HEIGHT * 0.68f;
            hudFont.setColor(0.82f, 0.88f, 0.91f, 1f);
            hudFont.draw(
                    spriteBatch,
                    SANDBOX_PHYSICS_TUNER_LABELS[i],
                    sandboxPhysicsTunerBounds.x + 12f,
                    textY);
            hudFont.setColor(0.98f, 0.92f, 0.76f, 1f);
            drawTextCentered(
                    hudFont,
                    formatSandboxPhysicsTuningValue(i),
                    sandboxPhysicsTunerBounds.x + sandboxPhysicsTunerBounds.width - 72f,
                    textY);
            hudFont.setColor(0.94f, 0.96f, 0.98f, 1f);
            drawTextCentered(
                    hudFont,
                    "-",
                    sandboxPhysicsDecreaseBounds[i].x + sandboxPhysicsDecreaseBounds[i].width * 0.5f,
                    sandboxPhysicsDecreaseBounds[i].y + sandboxPhysicsDecreaseBounds[i].height * 0.70f);
            drawTextCentered(
                    hudFont,
                    "+",
                    sandboxPhysicsIncreaseBounds[i].x + sandboxPhysicsIncreaseBounds[i].width * 0.5f,
                    sandboxPhysicsIncreaseBounds[i].y + sandboxPhysicsIncreaseBounds[i].height * 0.70f);
        }
        spriteBatch.end();
    }

    private void drawSandboxPhysicsTunerButtonShape(
            Rectangle bounds,
            float red,
            float green,
            float blue) {
        shapeRenderer.setColor(red, green, blue, 0.88f);
        shapeRenderer.rect(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    private String formatSandboxPhysicsTuningValue(int index) {
        float value = getSandboxPhysicsTuningValue(index);
        if (index == SANDBOX_PHYSICS_HORSE_POWER || index == SANDBOX_PHYSICS_BRAKE_FORCE) {
            return String.valueOf(Math.round(value));
        }
        if (index == SANDBOX_PHYSICS_STEERING_TORQUE) {
            return String.format(Locale.ROOT, "%.1f", value);
        }
        return String.format(Locale.ROOT, "%.2f", value);
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
                "Cars " + cars.size + "  |  You " + roster.first().totalPoints + " pts",
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
        float nameX = x + SIDEBAR_CAMERA_TARGET_ICON_WIDTH;
        float nameWidth = Math.max(36f, bestLapX - nameX - bestLapWidth - gap);
        float contentY = 0f;

        drawSidebarCameraTargetIcons(x, viewportTop, viewportBottom, headerStep, rowStep);

        float y = viewportTop - contentY - sidebarTablesScrollOffset;
        font.setColor(0.98f, 0.95f, 0.84f, 1f);
        drawSidebarTableText(font, "Player", nameX, y, viewportBottom, viewportTop);
        drawSidebarTableRightAlignedText(font, "Best", bestLapX, y, viewportBottom, viewportTop);
        drawSidebarTableRightAlignedText(font, "Current", currentLapX, y, viewportBottom, viewportTop);
        drawSidebarTableRightAlignedText(font, "Pts", pointsX, y, viewportBottom, viewportTop);
        contentY += headerStep;

        for (int i = 0; i < leaderboardEntries.size; i++) {
            CarTemplate template = leaderboardEntries.get(i);
            boolean active = template.currentCar != null && template.currentCar.active;
            boolean cameraTarget = isCameraTargetTemplate(template);
            y = viewportTop - contentY - sidebarTablesScrollOffset;

            if (cameraTarget) {
                font.setColor(1f, 0.94f, 0.54f, 1f);
            } else if (!active && !roundOver) {
                font.setColor(0.66f, 0.72f, 0.78f, 1f);
            } else {
                font.setColor(0.84f, 0.90f, 0.94f, 1f);
            }

            String player = truncateTextToWidth(font, buildLeaderboardPlayerLabel(template), nameWidth);
            drawSidebarTableText(font, player, nameX, y, viewportBottom, viewportTop);
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

    private void drawSidebarCameraTargetIcons(
            float x,
            float viewportTop,
            float viewportBottom,
            float headerStep,
            float rowStep) {
        if (leaderboardEntries.size == 0) {
            return;
        }

        spriteBatch.end();
        shapeRenderer.setProjectionMatrix(hudCamera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        float contentY = headerStep;
        for (int i = 0; i < leaderboardEntries.size; i++) {
            CarTemplate template = leaderboardEntries.get(i);
            float rowY = viewportTop - contentY - sidebarTablesScrollOffset;
            if (rowY + 4f >= viewportBottom && rowY - rowStep <= viewportTop) {
                drawSidebarCameraIconFill(
                        x + 1f,
                        rowY - rowStep * 0.40f,
                        isCameraTargetTemplate(template),
                        template.currentCar != null && template.currentCar.active);
            }
            contentY += rowStep;
        }
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        contentY = headerStep;
        for (int i = 0; i < leaderboardEntries.size; i++) {
            CarTemplate template = leaderboardEntries.get(i);
            float rowY = viewportTop - contentY - sidebarTablesScrollOffset;
            if (rowY + 4f >= viewportBottom && rowY - rowStep <= viewportTop) {
                drawSidebarCameraIconLine(
                        x + 1f,
                        rowY - rowStep * 0.40f,
                        isCameraTargetTemplate(template),
                        template.currentCar != null && template.currentCar.active);
            }
            contentY += rowStep;
        }
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
        spriteBatch.begin();
    }

    private void drawSidebarCameraIconFill(float x, float centerY, boolean selected, boolean active) {
        float alpha = active ? 0.76f : 0.30f;
        if (selected) {
            shapeRenderer.setColor(1f, 0.82f, 0.28f, 0.90f);
        } else {
            shapeRenderer.setColor(0.40f, 0.52f, 0.60f, alpha);
        }
        shapeRenderer.rect(x + 1.5f, centerY - 3.3f, 10.5f, 6.6f);
        shapeRenderer.rect(x + 4.0f, centerY + 2.6f, 4.2f, 2.2f);

        if (selected) {
            shapeRenderer.setColor(0.10f, 0.10f, 0.09f, 0.70f);
        } else {
            shapeRenderer.setColor(0.04f, 0.06f, 0.08f, active ? 0.72f : 0.42f);
        }
        shapeRenderer.circle(x + 6.7f, centerY, 1.8f, 14);
    }

    private void drawSidebarCameraIconLine(float x, float centerY, boolean selected, boolean active) {
        if (selected) {
            shapeRenderer.setColor(1f, 0.96f, 0.62f, 0.98f);
        } else {
            shapeRenderer.setColor(0.72f, 0.84f, 0.90f, active ? 0.66f : 0.34f);
        }
        shapeRenderer.rect(x + 1.5f, centerY - 3.3f, 10.5f, 6.6f);
        shapeRenderer.rect(x + 4.0f, centerY + 2.6f, 4.2f, 2.2f);
        shapeRenderer.circle(x + 6.7f, centerY, 1.9f, 14);
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
        if (!leaderboardDirty && !isLiveLapRaceMode()) {
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

    private void drawCarTelemetryBars(float playfieldWidth, float hudHeight) {
        if (playfieldWidth <= 0f || hudHeight <= 0f || cars.size == 0) {
            return;
        }

        float groupWidth = CAR_TELEMETRY_LABEL_WIDTH + CAR_TELEMETRY_LABEL_GAP + CAR_SPEED_BAR_WIDTH;
        float groupHeight =
                CAR_TELEMETRY_ROW_STEP * (CAR_TELEMETRY_ROW_COUNT - 1)
                        + CAR_SPEED_BAR_HEIGHT;

        shapeRenderer.setProjectionMatrix(hudCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < cars.size; i++) {
            Car car = cars.get(i);
            if (!car.active || car.body == null) {
                continue;
            }

            Vector2 renderPosition = car.getRenderPosition();
            carLabelProjection.set(renderPosition.x, renderPosition.y, 0f);
            worldViewport.project(carLabelProjection);
            carLabelProjection.y = Gdx.graphics.getHeight() - carLabelProjection.y;
            hudViewport.unproject(carLabelProjection);
            if (carLabelProjection.x < 0f
                    || carLabelProjection.x > playfieldWidth
                    || carLabelProjection.y < 0f
                    || carLabelProjection.y > hudHeight) {
                continue;
            }

            float groupLeft = carLabelProjection.x - groupWidth * 0.5f;
            float telemetryBottom =
                    carLabelProjection.y
                            - CAR_SPEED_BAR_OFFSET_Y
                            - CAR_TELEMETRY_ROW_STEP * (CAR_TELEMETRY_ROW_COUNT - 1);

            groupLeft = MathUtils.clamp(groupLeft, 2f, Math.max(2f, playfieldWidth - groupWidth - 2f));
            telemetryBottom =
                    MathUtils.clamp(
                            telemetryBottom,
                            2f,
                            Math.max(2f, hudHeight - groupHeight - 2f));
            float driftBottom = telemetryBottom;
            float steeringBottom = driftBottom + CAR_TELEMETRY_ROW_STEP;
            float brakeBottom = steeringBottom + CAR_TELEMETRY_ROW_STEP;
            float speedBottom = brakeBottom + CAR_TELEMETRY_ROW_STEP;
            float barLeft = groupLeft + CAR_TELEMETRY_LABEL_WIDTH + CAR_TELEMETRY_LABEL_GAP;
            float speedRatio =
                    MathUtils.clamp(
                            car.body.getLinearVelocity().len() / Math.max(0.001f, car.getForwardMaxSpeed()),
                            0f,
                            1f);
            float brake = car.getBrakeSignal();
            float steering = MathUtils.clamp(car.lastTurnCommand, -1f, 1f);
            float drift = car.getLateralSlipSignal();

            drawTelemetryLabelBackground(groupLeft, speedBottom);
            drawTelemetryLabelBackground(groupLeft, brakeBottom);
            drawTelemetryLabelBackground(groupLeft, steeringBottom);
            drawTelemetryLabelBackground(groupLeft, driftBottom);
            drawSpeedTelemetryBar(barLeft, speedBottom, speedRatio);
            drawBrakeTelemetryBar(barLeft, brakeBottom, brake);
            drawSteeringTelemetryBar(barLeft, steeringBottom, steering);
            drawDriftTelemetryBar(barLeft, driftBottom, drift);
        }
        shapeRenderer.end();

        spriteBatch.setProjectionMatrix(hudCamera.combined);
        spriteBatch.begin();
        labelFont.setColor(0.04f, 0.05f, 0.06f, 0.94f);
        for (int i = 0; i < cars.size; i++) {
            Car car = cars.get(i);
            if (!car.active || car.body == null) {
                continue;
            }

            Vector2 renderPosition = car.getRenderPosition();
            carLabelProjection.set(renderPosition.x, renderPosition.y, 0f);
            worldViewport.project(carLabelProjection);
            carLabelProjection.y = Gdx.graphics.getHeight() - carLabelProjection.y;
            hudViewport.unproject(carLabelProjection);
            if (carLabelProjection.x < 0f
                    || carLabelProjection.x > playfieldWidth
                    || carLabelProjection.y < 0f
                    || carLabelProjection.y > hudHeight) {
                continue;
            }

            float groupLeft = carLabelProjection.x - groupWidth * 0.5f;
            float telemetryBottom =
                    carLabelProjection.y
                            - CAR_SPEED_BAR_OFFSET_Y
                            - CAR_TELEMETRY_ROW_STEP * (CAR_TELEMETRY_ROW_COUNT - 1);

            groupLeft = MathUtils.clamp(groupLeft, 2f, Math.max(2f, playfieldWidth - groupWidth - 2f));
            telemetryBottom =
                    MathUtils.clamp(
                            telemetryBottom,
                            2f,
                            Math.max(2f, hudHeight - groupHeight - 2f));
            float driftBottom = telemetryBottom;
            float steeringBottom = driftBottom + CAR_TELEMETRY_ROW_STEP;
            float brakeBottom = steeringBottom + CAR_TELEMETRY_ROW_STEP;
            float speedBottom = brakeBottom + CAR_TELEMETRY_ROW_STEP;
            drawTelemetryLabel("speed", groupLeft, speedBottom);
            drawTelemetryLabel("brake", groupLeft, brakeBottom);
            drawTelemetryLabel("steering", groupLeft, steeringBottom);
            drawTelemetryLabel("drift", groupLeft, driftBottom);
        }
        spriteBatch.end();
    }

    private void drawTelemetryLabelBackground(float left, float bottom) {
        shapeRenderer.setColor(1f, 1f, 1f, 0.88f);
        shapeRenderer.rect(
                left,
                bottom - 1f,
                CAR_TELEMETRY_LABEL_WIDTH,
                CAR_TELEMETRY_ROW_STEP - 1f);
    }

    private void drawTelemetryLabel(String label, float left, float bottom) {
        labelFont.draw(spriteBatch, label, left + 2f, bottom + CAR_SPEED_BAR_HEIGHT + 3f);
    }

    private void drawSpeedTelemetryBar(float left, float bottom, float speedRatio) {
        float innerLeft = left + CAR_SPEED_BAR_BORDER;
        float innerBottom = bottom + CAR_SPEED_BAR_BORDER;
        float innerWidth = CAR_SPEED_BAR_WIDTH - CAR_SPEED_BAR_BORDER * 2f;
        float innerHeight = CAR_SPEED_BAR_HEIGHT - CAR_SPEED_BAR_BORDER * 2f;
        float fillWidth = Math.max(1.5f, innerWidth * speedRatio);

        shapeRenderer.setColor(0.01f, 0.012f, 0.016f, 0.92f);
        shapeRenderer.rect(left, bottom, CAR_SPEED_BAR_WIDTH, CAR_SPEED_BAR_HEIGHT);
        shapeRenderer.setColor(0.18f, 0.22f, 0.26f, 0.92f);
        shapeRenderer.rect(innerLeft, innerBottom, innerWidth, innerHeight);

        float red = 0.18f + 0.82f * speedRatio;
        float green = 0.88f - 0.10f * speedRatio;
        float blue = 0.38f - 0.30f * speedRatio;
        shapeRenderer.setColor(red, green, blue, 0.96f);
        shapeRenderer.rect(innerLeft, innerBottom, fillWidth, innerHeight);

        if (speedRatio >= 0.985f) {
            shapeRenderer.setColor(1f, 0.96f, 0.58f, 1f);
            shapeRenderer.rect(innerLeft + innerWidth - 2.0f, innerBottom, 2.0f, innerHeight);
        }
    }

    private void drawSteeringTelemetryBar(float left, float bottom, float steering) {
        float innerLeft = left + CAR_SPEED_BAR_BORDER;
        float innerBottom = bottom + CAR_SPEED_BAR_BORDER;
        float innerWidth = CAR_SPEED_BAR_WIDTH - CAR_SPEED_BAR_BORDER * 2f;
        float innerHeight = CAR_SPEED_BAR_HEIGHT - CAR_SPEED_BAR_BORDER * 2f;
        float centerX = innerLeft + innerWidth * 0.5f;
        float fillWidth = innerWidth * 0.5f * Math.abs(steering);

        shapeRenderer.setColor(0.01f, 0.012f, 0.016f, 0.92f);
        shapeRenderer.rect(left, bottom, CAR_SPEED_BAR_WIDTH, CAR_SPEED_BAR_HEIGHT);
        shapeRenderer.setColor(0.18f, 0.22f, 0.26f, 0.92f);
        shapeRenderer.rect(innerLeft, innerBottom, innerWidth, innerHeight);
        shapeRenderer.setColor(1f, 0.86f, 0.14f, 0.96f);
        if (steering > 0f) {
            shapeRenderer.rect(centerX - fillWidth, innerBottom, fillWidth, innerHeight);
        } else if (steering < 0f) {
            shapeRenderer.rect(centerX, innerBottom, fillWidth, innerHeight);
        }
        shapeRenderer.setColor(1f, 0.96f, 0.58f, 1f);
        shapeRenderer.rect(centerX - 0.5f, innerBottom, 1f, innerHeight);
    }

    private void drawBrakeTelemetryBar(float left, float bottom, float brake) {
        drawSignalTelemetryBar(left, bottom, brake, 1f, 0.22f, 0.12f);
    }

    private void drawDriftTelemetryBar(float left, float bottom, float drift) {
        drawSignalTelemetryBar(left, bottom, drift, 0.24f, 0.72f, 1f);
    }

    private void drawSignalTelemetryBar(
            float left, float bottom, float ratio, float red, float green, float blue) {
        float clampedRatio = MathUtils.clamp(ratio, 0f, 1f);
        float innerLeft = left + CAR_SPEED_BAR_BORDER;
        float innerBottom = bottom + CAR_SPEED_BAR_BORDER;
        float innerWidth = CAR_SPEED_BAR_WIDTH - CAR_SPEED_BAR_BORDER * 2f;
        float innerHeight = CAR_SPEED_BAR_HEIGHT - CAR_SPEED_BAR_BORDER * 2f;
        float fillWidth = clampedRatio <= 0.001f ? 0f : Math.max(1.5f, innerWidth * clampedRatio);

        shapeRenderer.setColor(0.01f, 0.012f, 0.016f, 0.92f);
        shapeRenderer.rect(left, bottom, CAR_SPEED_BAR_WIDTH, CAR_SPEED_BAR_HEIGHT);
        shapeRenderer.setColor(0.18f, 0.22f, 0.26f, 0.92f);
        shapeRenderer.rect(innerLeft, innerBottom, innerWidth, innerHeight);
        if (fillWidth > 0f) {
            shapeRenderer.setColor(red, green, blue, 0.96f);
            shapeRenderer.rect(innerLeft, innerBottom, fillWidth, innerHeight);
        }
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

    private boolean shouldDrawSandboxRlSensors() {
        return sandboxMode && currentMap != null && cars.size > 0;
    }

    private void fillSandboxRlObservation(Car car) {
        fillRlObservation(
                sandboxRlObservation,
                0,
                car,
                currentMap,
                car.template == null ? 0f : car.template.roundRaceLastRouteProgress,
                sandboxRlObservationForward,
                sandboxRlObservationRouteTarget,
                sandboxRlObservationSide);
    }

    private void prepareSandboxSensorAxes(Car car) {
        car.getRenderForwardDirection(sandboxSensorForward);
        sandboxSensorSide.set(-sandboxSensorForward.y, sandboxSensorForward.x);
    }

    private void drawSandboxRlSensorRays() {
        if (!shouldDrawSandboxRlSensors()) {
            return;
        }

        for (int i = 0; i < cars.size; i++) {
            Car car = cars.get(i);
            if (!car.active || car.body == null) {
                continue;
            }
            fillSandboxRlObservation(car);
            prepareSandboxSensorAxes(car);
            Vector2 origin = car.getRenderPosition();
            Vector2 velocity = car.body.getLinearVelocity();
            float trajectoryX = velocity.len2() > 0.25f ? velocity.x : sandboxSensorForward.x;
            float trajectoryY = velocity.len2() > 0.25f ? velocity.y : sandboxSensorForward.y;
            drawSandboxRoadSensorRay(
                    origin,
                    trajectoryX,
                    trajectoryY,
                    16,
                    Math.min(getRlSensorLookaheadDistance(car), RL_RAYCAST_DISTANCE));
            drawSandboxRoadSensorRay(
                    origin,
                    sandboxSensorSide.x,
                    sandboxSensorSide.y,
                    20,
                    RL_LATERAL_ROAD_CLEARANCE_DISTANCE);
            drawSandboxRoadSensorRay(
                    origin,
                    -sandboxSensorSide.x,
                    -sandboxSensorSide.y,
                    21,
                    RL_LATERAL_ROAD_CLEARANCE_DISTANCE);
            drawSandboxRoadSensorRay(
                    origin,
                    sandboxSensorForward.x,
                    sandboxSensorForward.y,
                    22,
                    RL_FRONT_ROAD_CLEARANCE_DISTANCE);
            drawSandboxRoadSensorRay(
                    origin,
                    sandboxSensorForward.x + sandboxSensorSide.x,
                    sandboxSensorForward.y + sandboxSensorSide.y,
                    23,
                    RL_FRONT_DIAGONAL_ROAD_CLEARANCE_DISTANCE);
            drawSandboxRoadSensorRay(
                    origin,
                    sandboxSensorForward.x - sandboxSensorSide.x,
                    sandboxSensorForward.y - sandboxSensorSide.y,
                    24,
                    RL_FRONT_DIAGONAL_ROAD_CLEARANCE_DISTANCE);

            drawSandboxRouteGuidance(car, origin);
        }
    }

    private void drawSandboxRoadSensorRay(
            Vector2 origin,
            float directionX,
            float directionY,
            int observationIndex,
            float maxDistance) {
        if (!isSandboxObservationIndexAvailable(observationIndex)) {
            return;
        }
        float value = MathUtils.clamp(sandboxRlObservation[observationIndex], 0f, 1f);
        float danger = 1f - value;
        shapeRenderer.setColor(0.18f + danger * 0.82f, 0.92f - danger * 0.68f, 0.18f, 0.82f);
        drawSandboxSensorRay(origin, directionX, directionY, value, maxDistance, value < 0.98f);
    }

    private void drawSandboxSensorRay(
            Vector2 origin,
            float directionX,
            float directionY,
            float normalizedDistance,
            float maxDistance,
            boolean markEndpoint) {
        float length = (float) Math.sqrt(directionX * directionX + directionY * directionY);
        if (length <= 0.0001f) {
            return;
        }
        float unitX = directionX / length;
        float unitY = directionY / length;
        float rayDistance =
                Math.max(SANDBOX_SENSOR_RAY_WIDTH, MathUtils.clamp(normalizedDistance, 0f, 1f) * maxDistance);
        float endX = origin.x + unitX * rayDistance;
        float endY = origin.y + unitY * rayDistance;
        shapeRenderer.line(origin.x, origin.y, endX, endY);
        if (markEndpoint) {
            shapeRenderer.circle(endX, endY, SANDBOX_SENSOR_HIT_RADIUS, 12);
        }
    }

    private void drawSandboxRouteGuidance(Car car, Vector2 origin) {
        if (currentMap == null || !currentMap.hasRoute() || car.body == null) {
            return;
        }

        float routeProgress =
                currentMap.findRouteProgressNear(
                        car.body.getPosition(),
                        sandboxSensorForward,
                        car.template == null ? 0f : car.template.roundRaceLastRouteProgress);

        currentMap.findRoutePoint(routeProgress, sandboxSensorRoutePoint);
        shapeRenderer.setColor(1f, 0.58f, 0.08f, 0.82f);
        shapeRenderer.line(origin.x, origin.y, sandboxSensorRoutePoint.x, sandboxSensorRoutePoint.y);
        shapeRenderer.circle(sandboxSensorRoutePoint.x, sandboxSensorRoutePoint.y, 0.11f, 12);

        float steeringLookaheadDistance = getRlSteeringLookaheadDistance(car);
        currentMap.findRoutePoint(routeProgress + steeringLookaheadDistance, sandboxSensorPoint);
        shapeRenderer.setColor(1f, 0.92f, 0.18f, 0.90f);
        shapeRenderer.line(origin.x, origin.y, sandboxSensorPoint.x, sandboxSensorPoint.y);
        shapeRenderer.circle(sandboxSensorPoint.x, sandboxSensorPoint.y, 0.12f, 12);

        currentMap.findRouteTangent(
                routeProgress + steeringLookaheadDistance,
                sandboxSensorDirection);
        drawSandboxRouteArrow(origin, sandboxSensorDirection, 0.28f, 1f, 0.74f, 0.78f);

        currentMap.findRouteTangent(routeProgress, sandboxSensorDirection);
        drawSandboxRouteArrow(origin, sandboxSensorDirection, 0.10f, 0.78f, 1f, 0.88f);
    }

    private void drawSandboxRouteArrow(
            Vector2 origin,
            Vector2 direction,
            float red,
            float green,
            float blue,
            float alpha) {
        if (direction.isZero(0.0001f)) {
            return;
        }
        sandboxSensorDirection.set(direction).nor().scl(SANDBOX_ROUTE_ARROW_LENGTH);
        shapeRenderer.setColor(red, green, blue, alpha);
        shapeRenderer.line(
                origin.x,
                origin.y,
                origin.x + sandboxSensorDirection.x,
                origin.y + sandboxSensorDirection.y);
        shapeRenderer.circle(
                origin.x + sandboxSensorDirection.x,
                origin.y + sandboxSensorDirection.y,
                SANDBOX_ROUTE_ARROW_WIDTH,
                10);
    }

    private void drawSandboxRlSensorHudLabels(float playfieldWidth, float hudHeight) {
        if (!shouldDrawSandboxRlSensors() || playfieldWidth <= 0f || hudHeight <= 0f) {
            return;
        }

        for (int i = 0; i < cars.size; i++) {
            Car car = cars.get(i);
            if (!car.active || car.body == null) {
                continue;
            }
            fillSandboxRlObservation(car);
            prepareSandboxSensorAxes(car);
            Vector2 origin = car.getRenderPosition();

            labelFont.setColor(0.70f, 1f, 0.72f, 0.96f);
            Vector2 velocity = car.body.getLinearVelocity();
            float trajectoryX = velocity.len2() > 0.25f ? velocity.x : sandboxSensorForward.x;
            float trajectoryY = velocity.len2() > 0.25f ? velocity.y : sandboxSensorForward.y;
            drawSandboxRayLabel(
                    playfieldWidth,
                    hudHeight,
                    origin,
                    trajectoryX,
                    trajectoryY,
                    16,
                    Math.min(getRlSensorLookaheadDistance(car), RL_RAYCAST_DISTANCE),
                    "path");
            drawSandboxRayLabel(
                    playfieldWidth,
                    hudHeight,
                    origin,
                    sandboxSensorSide.x,
                    sandboxSensorSide.y,
                    20,
                    RL_LATERAL_ROAD_CLEARANCE_DISTANCE,
                    "roadL");
            drawSandboxRayLabel(
                    playfieldWidth,
                    hudHeight,
                    origin,
                    -sandboxSensorSide.x,
                    -sandboxSensorSide.y,
                    21,
                    RL_LATERAL_ROAD_CLEARANCE_DISTANCE,
                    "roadR");
            drawSandboxRayLabel(
                    playfieldWidth,
                    hudHeight,
                    origin,
                    sandboxSensorForward.x,
                    sandboxSensorForward.y,
                    22,
                    RL_FRONT_ROAD_CLEARANCE_DISTANCE,
                    "roadF");
            drawSandboxRayLabel(
                    playfieldWidth,
                    hudHeight,
                    origin,
                    sandboxSensorForward.x + sandboxSensorSide.x,
                    sandboxSensorForward.y + sandboxSensorSide.y,
                    23,
                    RL_FRONT_DIAGONAL_ROAD_CLEARANCE_DISTANCE,
                    "roadFL");
            drawSandboxRayLabel(
                    playfieldWidth,
                    hudHeight,
                    origin,
                    sandboxSensorForward.x - sandboxSensorSide.x,
                    sandboxSensorForward.y - sandboxSensorSide.y,
                    24,
                    RL_FRONT_DIAGONAL_ROAD_CLEARANCE_DISTANCE,
                    "roadFR");

            drawSandboxObservationSummary(car, playfieldWidth, hudHeight);
        }
    }

    private void drawSandboxRayLabel(
            float playfieldWidth,
            float hudHeight,
            Vector2 origin,
            float directionX,
            float directionY,
            int observationIndex,
            float maxDistance,
            String shortName) {
        if (!isSandboxObservationIndexAvailable(observationIndex)) {
            return;
        }
        float length = (float) Math.sqrt(directionX * directionX + directionY * directionY);
        if (length <= 0.0001f) {
            return;
        }
        float unitX = directionX / length;
        float unitY = directionY / length;
        float value = MathUtils.clamp(sandboxRlObservation[observationIndex], 0f, 1f);
        float rayDistance = Math.max(0.18f, value * maxDistance);
        float worldX = origin.x + unitX * (rayDistance + SANDBOX_SENSOR_WORLD_LABEL_OFFSET);
        float worldY = origin.y + unitY * (rayDistance + SANDBOX_SENSOR_WORLD_LABEL_OFFSET);
        if (!projectSandboxWorldToHud(worldX, worldY, playfieldWidth, hudHeight)) {
            return;
        }
        drawTextWithShadow(
                labelFont,
                observationIndex + " " + shortName + "=" + formatSandboxValue(value),
                sandboxSensorProjection.x,
                sandboxSensorProjection.y);
    }

    private void drawSandboxObservationSummary(Car car, float playfieldWidth, float hudHeight) {
        Vector2 origin = car.getRenderPosition();
        if (!projectSandboxWorldToHud(
                origin.x,
                origin.y + car.getHeight() * SANDBOX_SENSOR_LABEL_DISTANCE,
                playfieldWidth,
                hudHeight)) {
            return;
        }

        float left = MathUtils.clamp(sandboxSensorProjection.x + 8f, 2f, Math.max(2f, playfieldWidth - 260f));
        float top = MathUtils.clamp(sandboxSensorProjection.y + 54f, 48f, Math.max(48f, hudHeight - 6f));
        String owner = car.playerControlled ? "YOU" : car.name;

        labelFont.setColor(1f, 0.95f, 0.52f, 0.98f);
        drawTextWithShadow(labelFont, owner + " RL sensors", left, top);
        labelFont.setColor(0.90f, 0.96f, 1f, 0.96f);
        int rowCount = (RL_OBSERVATION_SIZE + 3) / 4;
        for (int row = 0; row < rowCount; row++) {
            int first = row * 4;
            drawSandboxObservationRow(
                    left,
                    top - SANDBOX_SENSOR_LABEL_STEP * (row + 1),
                    first,
                    first + 1 < RL_OBSERVATION_SIZE ? first + 1 : -1,
                    first + 2 < RL_OBSERVATION_SIZE ? first + 2 : -1,
                    first + 3 < RL_OBSERVATION_SIZE ? first + 3 : -1);
        }
    }

    private void drawSandboxObservationRow(float left, float top, int first, int second, int third, int fourth) {
        String text =
                sandboxObservationText(first)
                        + "  "
                        + sandboxObservationText(second)
                        + "  "
                        + sandboxObservationText(third)
                        + "  "
                        + sandboxObservationText(fourth);
        drawTextWithShadow(labelFont, text, left, top);
    }

    private String sandboxObservationText(int index) {
        if (index < 0) {
            return "";
        }
        if (!isSandboxObservationIndexAvailable(index)) {
            return index + " n/a";
        }
        String name = index < RL_OBSERVATION_NAMES.length ? RL_OBSERVATION_NAMES[index] : "obs";
        return index + " " + name + "=" + formatSandboxValue(sandboxRlObservation[index]);
    }

    private boolean projectSandboxWorldToHud(
            float worldX,
            float worldY,
            float playfieldWidth,
            float hudHeight) {
        sandboxSensorProjection.set(worldX, worldY, 0f);
        worldViewport.project(sandboxSensorProjection);
        sandboxSensorProjection.y = Gdx.graphics.getHeight() - sandboxSensorProjection.y;
        hudViewport.unproject(sandboxSensorProjection);
        return sandboxSensorProjection.x >= -48f
                && sandboxSensorProjection.x <= playfieldWidth + 48f
                && sandboxSensorProjection.y >= -48f
                && sandboxSensorProjection.y <= hudHeight + 48f;
    }

    private boolean isSandboxObservationIndexAvailable(int observationIndex) {
        return observationIndex >= 0
                && observationIndex < sandboxRlObservation.length
                && observationIndex < RL_OBSERVATION_SIZE;
    }

    private void drawTextWithShadow(BitmapFont font, String text, float x, float y) {
        Color original = font.getColor();
        float red = original.r;
        float green = original.g;
        float blue = original.b;
        float alpha = original.a;
        font.setColor(0f, 0f, 0f, alpha * 0.70f);
        font.draw(spriteBatch, text, x + 1f, y - 1f);
        font.setColor(red, green, blue, alpha);
        font.draw(spriteBatch, text, x, y);
    }

    private String formatSandboxValue(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
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
        String baseLabel = template.playerControlled ? "YOU" : template.name;
        String policyLabel = buildLoadedDriverPolicyLabel(template.rlPolicy);
        return policyLabel.length() == 0 ? baseLabel : baseLabel + " (" + policyLabel + ")";
    }

    private String buildCameraTargetText() {
        Car target = getCameraTargetCar();
        if (target == null || target.template == null || target.playerControlled) {
            return "";
        }
        return "Camera: " + buildLeaderboardPlayerLabel(target.template);
    }

    private String buildLoadedDriverPolicyLabel(RlPolicy policy) {
        if (policy == null) {
            return "";
        }
        for (int i = 0; i < DRIVER_POLICY_CHOICES.length; i++) {
            DriverPolicyChoice choice = DRIVER_POLICY_CHOICES[i];
            if (rlPolicies.get(choice.id) == policy) {
                return choice.id;
            }
        }
        return policy == rlEnemyPolicy ? RL_LEGACY_POLICY_ID : "";
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
                .append("  |  Cars: ").append(cars.size)
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

        return "No route";
    }

    private String buildObjectiveText() {
        if (preRoundCountdownTimer > 0f) {
            return isLiveLapRaceMode()
                    ? "Prepare for the horn. Complete "
                            + getRaceLapsToWin()
                            + " laps."
                    : "This map needs a generated route before it can be raced.";
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

        return "Route missing.";
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
        int progressPercent = MathUtils.round(getRaceLapProgressFraction(template) * 100f);
        return "Lap " + lap + "/" + raceLaps + " " + progressPercent + "%";
    }

    private float getRaceLapProgressFraction(CarTemplate template) {
        if (template == null || currentMap == null || !currentMap.hasRoute()) {
            return 0f;
        }
        return MathUtils.clamp(
                template.roundRaceDistanceThisLap / Math.max(0.001f, currentMap.getRouteLength()),
                0f,
                1f);
    }

    private String buildHeadline() {
        if (!roundOver) {
            return "RACING";
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
            return "Race in progress.";
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

    private boolean isHalloweenTheme() {
        return HALLOWEEN_THEME_NAME.equals(toThemeLookupKey(configuredThemeName));
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

    private void disposeMapDebugMaskTextures() {
        for (Texture texture : mapDebugMaskTextureCache.values()) {
            disposeTexture(texture);
        }
        mapDebugMaskTextureCache.clear();
    }

    @Override
    public void dispose() {
        if (world != null) {
            world.dispose();
        }
        disposeRosterSpriteTextures();
        disposeArenaSurfaceTextures();
        disposeMapDebugMaskTextures();
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

    private final class ImpactContactListener implements ContactListener {
        @Override
        public void beginContact(Contact contact) {
            Car carA = contactCar(contact.getFixtureA());
            Car carB = contactCar(contact.getFixtureB());
            if (carA == null || carB == null) {
                return;
            }
            carA.beginCarContact();
            carB.beginCarContact();
        }

        @Override
        public void endContact(Contact contact) {
            Car carA = contactCar(contact.getFixtureA());
            Car carB = contactCar(contact.getFixtureB());
            if (carA == null || carB == null) {
                return;
            }
            carA.endCarContact();
            carB.endCarContact();
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

        private Car contactCar(Fixture fixture) {
            if (fixture == null || fixture.getBody() == null) {
                return null;
            }
            Object userData = fixture.getBody().getUserData();
            return userData instanceof Car ? (Car) userData : null;
        }
    }

    private static final class CarPhysics {
        private static final CarPhysics DEFAULT =
                new CarPhysics(
                        1.25f,
                        550f,
                        0.42f,
                        620f,
                        0.50f,
                        64.0f,
                        1.10f,
                        0.76f,
                        1.18f,
                        15.0f,
                        6.8f,
                        0.145f,
                        0.028f,
                        72.0f,
                        48.0f,
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

        private CarPhysics withSandboxTuning(
                float massMultiplier,
                float horsePower,
                float brakeForce,
                float steeringTorque,
                float wheelGrip) {
            return new CarPhysics(
                    massMultiplier,
                    horsePower,
                    reversePowerMultiplier,
                    brakeForce,
                    reverseSpeedMultiplier,
                    steeringTorque,
                    lowSpeedSteeringAuthority,
                    highSpeedSteeringAuthority,
                    wheelGrip,
                    lateralGripPerSecond,
                    yawGripPerSecond,
                    rollingResistance,
                    aeroDrag,
                    maxForwardSpeed,
                    steeringReferenceSpeed,
                    trackLimitMaxSpeedMultiplier,
                    trackLimitVelocityDamping,
                    trackLimitAngularDamping,
                    linearDamping,
                    angularDamping,
                    fixtureDensity,
                    fixtureFriction,
                    fixtureRestitution,
                    collisionImpulseFactor,
                    maxCollisionImpulse);
        }

        private float engineForce() {
            return horsePower * HORSEPOWER_TO_FORCE;
        }
    }

    private static final class Car {
        private static final float WIDTH = 1.14f;
        private static final float HEIGHT = 1.58f;
        private static final float HALF_WIDTH = WIDTH * 0.5f;
        private static final float HALF_HEIGHT = HEIGHT * 0.5f;
        private static final float GROWTH_SCALE = 1.40f;
        private static final float GROWTH_MASS_MULTIPLIER = 10f;
        private static final float REVERSE_ENGAGE_SPEED = 0.08f;
        private static final float REVERSE_ENGAGE_STOP_SPEED = 0.035f;
        private static final float GROWTH_TURN_MULTIPLIER = 0.90f;
        private static final float MAX_GROWTH_SPEED_MULTIPLIER = 1.06f;
        private static final float TIRE_SLIP_EPSILON = 0.85f;
        private static final float TIRE_PEAK_SLIP_ANGLE = 0.18f;
        private static final float TIRE_SLIDE_SLIP_ANGLE = 0.72f;
        private static final float TIRE_SLIDE_GRIP_FRACTION = 0.58f;
        private static final float TIRE_SLIDE_SCRUB_DECELERATION_FRACTION = 0.35f;
        private static final float DRIVE_TRACTION_MULTIPLIER = 1.18f;
        private static final float BRAKE_TRACTION_MULTIPLIER = 1.55f;
        private static final float BRAKE_COMBINED_SLIP_WEIGHT = 0.62f;
        private static final float ACCEL_COMBINED_SLIP_WEIGHT = 0.38f;
        private static final float MAX_LOW_SPEED_STEER_ANGLE = 0.58f;
        private static final float MAX_HIGH_SPEED_STEER_ANGLE = 0.13f;
        private static final float MIN_WHEELBASE = 0.85f;
        private static final float SOFT_SPEED_LIMIT_ACCEL = 7.5f;
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
        private static final float TRACK_LIMIT_FORWARD_ACCELERATION_MULTIPLIER = 0.12f;
        private static final float ARENA_WALL_CONTACT_DURATION = 0.28f;
        private static final float AUTO_RECOVERY_TRIGGER_SECONDS = 3f;
        private static final float CONTACT_AUTO_RECOVERY_RANGE_FACTOR = 0.58f;
        private static final float CONTACT_AUTO_RECOVERY_DISTANCE = HEIGHT;
        private static final float CONTACT_AUTO_RECOVERY_MAX_SECONDS = 2.4f;
        private static final float CONTACT_AUTO_RECOVERY_THROTTLE = 0.92f;
        private static final float CONTACT_AUTO_RECOVERY_FORWARD_SPEED_EPSILON = 0.35f;
        private static final float CONTACT_AUTO_RECOVERY_FORWARD_ALIGNMENT_MIN = 0.60f;
        private static final float OFF_ROAD_AUTO_RECOVERY_MAX_SECONDS = 5.0f;
        private static final float OFF_ROAD_AUTO_RECOVERY_FORWARD_THROTTLE = 0.86f;
        private static final float OFF_ROAD_AUTO_RECOVERY_REVERSE_THROTTLE = 0.62f;
        private static final float NO_PROGRESS_AUTO_RECOVERY_DISTANCE = HEIGHT * 0.50f;
        private static final float NO_PROGRESS_AUTO_RECOVERY_LOOKAHEAD = HEIGHT * 2.5f;
        private static final float NO_PROGRESS_AUTO_RECOVERY_MAX_SECONDS = 5.0f;

        private final CarTemplate template;
        private final String name;
        private final boolean playerControlled;
        private final boolean externallyControlled;
        private final boolean modelControlled;
        private final Color color;
        private final AiControlDecision externalControlDecision = new AiControlDecision();
        private final AiControlDecision rawExternalControlDecision = new AiControlDecision();
        private final AiControlDecision automaticRecoveryControlDecision = new AiControlDecision();
        private final float[] rlObservation = new float[RL_OBSERVATION_SIZE];
        private final Vector2 forwardAxis = new Vector2();
        private final Vector2 sidewaysAxis = new Vector2();
        private final Vector2 working = new Vector2();
        private final Vector2 pendingImpactImpulse = new Vector2();
        private final Vector2 impactRecoveryPoint = new Vector2();
        private final Vector2 impactOutward = new Vector2();
        private final Vector2 autoRecoveryStartPosition = new Vector2();
        private final Vector2 autoRecoveryDirection = new Vector2();
        private final Vector2 autoRecoveryTarget = new Vector2();
        private final Vector2 autoRecoveryToTarget = new Vector2();
        private final Vector2 arenaWallPosition = new Vector2();
        private final Vector2 arenaWallCorrection = new Vector2();
        private final Vector2 arenaWallVelocity = new Vector2();
        private final Vector2 previousRenderPosition = new Vector2();
        private final Vector2 renderPosition = new Vector2();
        private final Vector2 rlObservationForward = new Vector2();
        private final Vector2 rlObservationRouteTarget = new Vector2();
        private final Vector2 rlObservationSide = new Vector2();
        private final Vector2 passingRouteForward = new Vector2();
        private final Vector2 passingRoutePoint = new Vector2();
        private final Vector2 passingRouteTangent = new Vector2();
        private final float[] passingSelfRouteMargins = new float[2];
        private final float[] passingTargetRouteMargins = new float[2];

        private Body body;
        private boolean active = true;
        private boolean growthBoosted;
        private int lastAttackerId = -1;
        private int carHitCount;
        private int carContactCount;
        private float sizeScale = 1f;
        private float impactSlideTimer;
        private float impactSlideStrength;
        private float controlLockTimer;
        private float recentImpactTimer;
        private float arenaWallContactTimer;
        private float ramChargeTimer;
        private float slipstreamBoost;
        private float slipstreamSnapshotX;
        private float slipstreamSnapshotY;
        private float slipstreamSnapshotForwardX;
        private float slipstreamSnapshotForwardY;
        private float slipstreamSnapshotSideX;
        private float slipstreamSnapshotSideY;
        private float slipstreamSnapshotForwardSpeed;
        private float slipstreamSnapshotBaseSpeed;
        private float slipstreamSnapshotSpeedCap;
        private boolean slipstreamSnapshotReady;
        private boolean slipstreamSnapshotOnRoad;
        private Car passingAssistTarget;
        private int passingAssistDirection;
        private float passingAssistTurnBias;
        private float rlDecisionTimer;
        private float sustainedCarContactTimer;
        private float sustainedOffRoadTimer;
        private float sustainedNoProgressTimer;
        private float contactAutoRecoveryTimer;
        private float offRoadAutoRecoveryTimer;
        private float noProgressAutoRecoveryTimer;
        private float noProgressReferenceRouteProgress;
        private boolean contactAutoRecoveryActive;
        private boolean offRoadAutoRecoveryActive;
        private boolean noProgressAutoRecoveryActive;
        private boolean noProgressRouteTrackingInitialized;
        private float contactAutoRecoveryThrottleSign = -1f;
        private boolean reverseDriveActive;
        private float lastThrottleCommand;
        private float lastTurnCommand;
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
                RlPolicy rlPolicy,
                boolean trainingMode) {
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
            boolean automaticOffRoadRecoveryAllowed =
                    !trainingMode && allowControl && controlLockTimer <= 0f;
            boolean automaticContactRecoveryAllowed =
                    !trainingMode && allowControl && controlLockTimer <= 0f;
            updateAutomaticRecoveryState(
                    delta,
                    arenaMap,
                    cars,
                    automaticOffRoadRecoveryAllowed,
                    automaticContactRecoveryAllowed);
            if ((automaticOffRoadRecoveryAllowed || automaticContactRecoveryAllowed)
                    && applyAutomaticRecoveryControl(
                            arenaMap,
                            cars,
                            delta,
                            throttle,
                            turn,
                            automaticOffRoadRecoveryAllowed,
                            automaticContactRecoveryAllowed)) {
                throttle = automaticRecoveryControlDecision.throttle;
                turn = automaticRecoveryControlDecision.turn;
                resetPassingAssist();
            } else {
                turn = applyPassingAssist(delta, arenaMap, cars, turn);
            }
            lastThrottleCommand = allowControl && controlLockTimer <= 0f ? throttle : 0f;
            lastTurnCommand = allowControl && controlLockTimer <= 0f ? turn : 0f;

            applyGrip(delta, impactSlideFactor);

            if (!allowControl || controlLockTimer > 0f) {
                updateSlipstream(delta, cars, 0f);
                return;
            }

            updateSlipstream(delta, cars, throttle);
            drive(throttle, turn, arenaMap);
        }

        private void prepareSlipstreamSnapshot(ArenaMap arenaMap) {
            if (!active || body == null) {
                slipstreamSnapshotReady = false;
                slipstreamSnapshotOnRoad = false;
                return;
            }

            updateAxes();
            Vector2 position = body.getPosition();
            Vector2 velocity = body.getLinearVelocity();
            slipstreamSnapshotX = position.x;
            slipstreamSnapshotY = position.y;
            slipstreamSnapshotForwardX = forwardAxis.x;
            slipstreamSnapshotForwardY = forwardAxis.y;
            slipstreamSnapshotSideX = sidewaysAxis.x;
            slipstreamSnapshotSideY = sidewaysAxis.y;
            slipstreamSnapshotForwardSpeed =
                    slipstreamSnapshotForwardX * velocity.x
                            + slipstreamSnapshotForwardY * velocity.y;
            slipstreamSnapshotBaseSpeed = getBaseForwardMaxSpeed();
            slipstreamSnapshotSpeedCap =
                    slipstreamSnapshotBaseSpeed * SLIPSTREAM_SPEED_CAP_RATIO;
            slipstreamSnapshotOnRoad = arenaMap == null || arenaMap.supports(position);
            slipstreamSnapshotReady = true;
        }

        private float applyPassingAssist(
                float delta,
                ArenaMap arenaMap,
                Array<Car> cars,
                float requestedTurn) {
            if ((!modelControlled && !externallyControlled)
                    || arenaMap == null
                    || !arenaMap.hasRoute()
                    || cars == null
                    || cars.size <= 1
                    || !slipstreamSnapshotReady
                    || !slipstreamSnapshotOnRoad) {
                resetPassingAssist();
                return requestedTurn;
            }

            Car target = findPassingAssistTarget(cars);
            float targetBias = 0f;
            if (target != null) {
                if (target != passingAssistTarget) {
                    passingAssistTarget = target;
                    passingAssistDirection = 0;
                }
                int selectedDirection = selectPassingAssistDirection(arenaMap, cars, target);
                if (selectedDirection != 0) {
                    passingAssistDirection = selectedDirection;
                    targetBias = calculatePassingAssistTurnBias(target, selectedDirection);
                } else {
                    passingAssistTarget = null;
                    passingAssistDirection = 0;
                }
            } else {
                passingAssistTarget = null;
                passingAssistDirection = 0;
            }

            float lerpSpeed =
                    Math.abs(targetBias) > Math.abs(passingAssistTurnBias)
                            ? PASSING_ASSIST_ATTACK_LERP
                            : PASSING_ASSIST_RELEASE_LERP;
            float alpha = 1f - (float) Math.exp(-lerpSpeed * Math.max(0f, delta));
            passingAssistTurnBias +=
                    (targetBias - passingAssistTurnBias) * MathUtils.clamp(alpha, 0f, 1f);
            if (Math.abs(passingAssistTurnBias) < 0.001f && targetBias == 0f) {
                passingAssistTurnBias = 0f;
            }

            if (passingAssistTarget == null || Math.abs(passingAssistTurnBias) < 0.001f) {
                return MathUtils.clamp(
                        requestedTurn + passingAssistTurnBias * 0.25f,
                        -1f,
                        1f);
            }
            if (requestedTurn * passingAssistTurnBias <= 0f) {
                return passingAssistTurnBias;
            }
            return Math.copySign(
                    Math.max(Math.abs(requestedTurn), Math.abs(passingAssistTurnBias)),
                    passingAssistTurnBias);
        }

        private Car findPassingAssistTarget(Array<Car> cars) {
            if (isPassingAssistTargetCandidate(passingAssistTarget, true)) {
                return passingAssistTarget;
            }

            Car nearest = null;
            float nearestForwardDistance = Float.MAX_VALUE;
            for (int i = 0; i < cars.size; i++) {
                Car candidate = cars.get(i);
                if (!isPassingAssistTargetCandidate(candidate, false)) {
                    continue;
                }
                float dx = candidate.slipstreamSnapshotX - slipstreamSnapshotX;
                float dy = candidate.slipstreamSnapshotY - slipstreamSnapshotY;
                float forwardDistance =
                        dx * slipstreamSnapshotForwardX + dy * slipstreamSnapshotForwardY;
                if (forwardDistance < nearestForwardDistance) {
                    nearest = candidate;
                    nearestForwardDistance = forwardDistance;
                }
            }
            return nearest;
        }

        private boolean isPassingAssistTargetCandidate(Car candidate, boolean heldTarget) {
            if (candidate == null
                    || candidate == this
                    || !candidate.active
                    || candidate.body == null
                    || !candidate.slipstreamSnapshotReady
                    || !candidate.slipstreamSnapshotOnRoad) {
                return false;
            }

            float minimumSpeed = getBaseForwardMaxSpeed() * PASSING_ASSIST_MIN_SPEED_RATIO;
            if (slipstreamSnapshotForwardSpeed < minimumSpeed) {
                return false;
            }

            float dx = candidate.slipstreamSnapshotX - slipstreamSnapshotX;
            float dy = candidate.slipstreamSnapshotY - slipstreamSnapshotY;
            float forwardDistance =
                    dx * slipstreamSnapshotForwardX + dy * slipstreamSnapshotForwardY;
            float minimumForwardDistance =
                    heldTarget
                            ? -PASSING_ASSIST_RELEASE_BEHIND_DISTANCE
                            : PASSING_ASSIST_MIN_FORWARD_DISTANCE;
            float maximumForwardDistance =
                    heldTarget ? PASSING_ASSIST_RANGE * 1.2f : PASSING_ASSIST_RANGE;
            if (forwardDistance < minimumForwardDistance
                    || forwardDistance > maximumForwardDistance) {
                return false;
            }

            float lateralDistance =
                    Math.abs(dx * slipstreamSnapshotSideX + dy * slipstreamSnapshotSideY);
            float maximumLateralDistance =
                    heldTarget
                            ? PASSING_ASSIST_HOLD_LATERAL_DISTANCE
                            : PASSING_ASSIST_ACQUIRE_LATERAL_DISTANCE;
            if (lateralDistance > maximumLateralDistance) {
                return false;
            }

            float alignment =
                    slipstreamSnapshotForwardX * candidate.slipstreamSnapshotForwardX
                            + slipstreamSnapshotForwardY
                                    * candidate.slipstreamSnapshotForwardY;
            if (alignment < PASSING_ASSIST_MIN_ALIGNMENT) {
                return false;
            }

            Vector2 candidateVelocity = candidate.body.getLinearVelocity();
            float candidateSpeedAlongFollower =
                    candidateVelocity.x * slipstreamSnapshotForwardX
                            + candidateVelocity.y * slipstreamSnapshotForwardY;
            float closingSpeed = slipstreamSnapshotForwardSpeed - candidateSpeedAlongFollower;
            if (heldTarget) {
                return forwardDistance <= PASSING_ASSIST_MIN_FORWARD_DISTANCE
                        || closingSpeed >= -PASSING_ASSIST_MAX_RECEDING_SPEED;
            }
            float contactDistance = HEIGHT * (sizeScale + candidate.sizeScale) * 0.5f;
            float clearDistance = Math.max(0f, forwardDistance - contactDistance);
            float timeToContact =
                    closingSpeed > 0.001f
                            ? clearDistance / closingSpeed
                            : Float.POSITIVE_INFINITY;
            boolean closeFollowing =
                    forwardDistance <= PASSING_ASSIST_CLOSE_FOLLOW_DISTANCE
                            && closingSpeed >= -PASSING_ASSIST_MAX_RECEDING_SPEED;
            return closeFollowing
                    || closingSpeed >= PASSING_ASSIST_MIN_CLOSING_SPEED
                    || timeToContact <= PASSING_ASSIST_TIME_TO_CONTACT;
        }

        private int selectPassingAssistDirection(
                ArenaMap arenaMap,
                Array<Car> cars,
                Car target) {
            if (!calculatePassingRouteMargins(arenaMap, this, passingSelfRouteMargins)
                    || !calculatePassingRouteMargins(
                            arenaMap,
                            target,
                            passingTargetRouteMargins)) {
                return 0;
            }
            float followerHalfWidth = HALF_WIDTH * sizeScale;
            float targetHalfWidth = HALF_WIDTH * target.sizeScale;
            float desiredSeparation =
                    followerHalfWidth + targetHalfWidth + PASSING_ASSIST_SIDE_GAP;
            float requiredTargetMargin =
                    desiredSeparation + followerHalfWidth + PASSING_ASSIST_EDGE_GAP;
            float requiredFollowerMargin = followerHalfWidth + PASSING_ASSIST_EDGE_GAP;
            float leftSpace =
                    Math.min(
                            passingSelfRouteMargins[0] - requiredFollowerMargin,
                            passingTargetRouteMargins[0] - requiredTargetMargin);
            float rightSpace =
                    Math.min(
                            passingSelfRouteMargins[1] - requiredFollowerMargin,
                            passingTargetRouteMargins[1] - requiredTargetMargin);
            boolean leftAvailable =
                    leftSpace >= 0f
                            && isPassingSideClear(cars, target, 1, desiredSeparation);
            boolean rightAvailable =
                    rightSpace >= 0f
                            && isPassingSideClear(cars, target, -1, desiredSeparation);
            if (!leftAvailable && !rightAvailable) {
                return 0;
            }
            if (passingAssistDirection > 0 && !leftAvailable) {
                return 0;
            }
            if (passingAssistDirection < 0 && !rightAvailable) {
                return 0;
            }
            if (passingAssistDirection > 0
                    && leftAvailable
                    && (!rightAvailable
                            || rightSpace <= leftSpace + PASSING_ASSIST_SIDE_SWITCH_HYSTERESIS)) {
                return 1;
            }
            if (passingAssistDirection < 0
                    && rightAvailable
                    && (!leftAvailable
                            || leftSpace <= rightSpace + PASSING_ASSIST_SIDE_SWITCH_HYSTERESIS)) {
                return -1;
            }
            return leftAvailable && (!rightAvailable || leftSpace >= rightSpace) ? 1 : -1;
        }

        private boolean isPassingSideClear(
                Array<Car> cars,
                Car target,
                int direction,
                float desiredSeparation) {
            if (cars == null || target == null) {
                return false;
            }

            float targetDx = target.slipstreamSnapshotX - slipstreamSnapshotX;
            float targetDy = target.slipstreamSnapshotY - slipstreamSnapshotY;
            float targetForwardDistance =
                    targetDx * slipstreamSnapshotForwardX
                            + targetDy * slipstreamSnapshotForwardY;
            float laneCenter = direction * desiredSeparation;
            float longitudinalRearLimit = -HEIGHT * 1.35f;
            float longitudinalFrontLimit =
                    Math.max(HEIGHT * 2f, targetForwardDistance + HEIGHT * 1.35f);
            float lateralClearance = WIDTH * 0.95f;
            for (int i = 0; i < cars.size; i++) {
                Car other = cars.get(i);
                if (other == null
                        || other == this
                        || other == target
                        || !other.active
                        || !other.slipstreamSnapshotReady) {
                    continue;
                }
                float dx = other.slipstreamSnapshotX - slipstreamSnapshotX;
                float dy = other.slipstreamSnapshotY - slipstreamSnapshotY;
                float forwardDistance =
                        dx * slipstreamSnapshotForwardX + dy * slipstreamSnapshotForwardY;
                if (forwardDistance < longitudinalRearLimit
                        || forwardDistance > longitudinalFrontLimit) {
                    continue;
                }
                float lateralDistance =
                        -(dx * slipstreamSnapshotSideX + dy * slipstreamSnapshotSideY);
                if (Math.abs(lateralDistance - laneCenter) < lateralClearance) {
                    return false;
                }
            }
            return true;
        }

        private boolean calculatePassingRouteMargins(
                ArenaMap arenaMap,
                Car car,
                float[] out) {
            if (arenaMap == null
                    || !arenaMap.hasRoute()
                    || car == null
                    || car.body == null
                    || out == null
                    || out.length < 2) {
                return false;
            }

            passingRouteForward.set(car.body.getWorldVector(passingRouteForward.set(0f, 1f)));
            float referenceProgress =
                    car.template == null ? 0f : car.template.roundRaceLastRouteProgress;
            float routeProgress =
                    arenaMap.findRouteProgressNear(
                            car.body.getPosition(),
                            passingRouteForward,
                            referenceProgress);
            arenaMap.findRoutePoint(routeProgress, passingRoutePoint);
            arenaMap.findRouteTangent(routeProgress, passingRouteTangent);
            Vector2 position = car.body.getPosition();
            float routeNormalX = -passingRouteTangent.y;
            float routeNormalY = passingRouteTangent.x;
            float lateralOffset =
                    (position.x - passingRoutePoint.x) * routeNormalX
                            + (position.y - passingRoutePoint.y) * routeNormalY;
            out[0] = arenaMap.getRouteLeftClearance(routeProgress) - lateralOffset;
            out[1] = arenaMap.getRouteRightClearance(routeProgress) + lateralOffset;
            return out[0] > 0f || out[1] > 0f;
        }

        private float calculatePassingAssistTurnBias(Car target, int direction) {
            float dx = target.slipstreamSnapshotX - slipstreamSnapshotX;
            float dy = target.slipstreamSnapshotY - slipstreamSnapshotY;
            float forwardDistance =
                    dx * slipstreamSnapshotForwardX + dy * slipstreamSnapshotForwardY;
            // The cached body-side axis points right; passing directions are left-positive.
            float targetLateral =
                    -(dx * slipstreamSnapshotSideX + dy * slipstreamSnapshotSideY);
            float desiredSeparation =
                    HALF_WIDTH * sizeScale
                            + HALF_WIDTH * target.sizeScale
                            + PASSING_ASSIST_SIDE_GAP;
            float lateralError =
                    direction * (targetLateral + direction * desiredSeparation);
            float lateralNeed = MathUtils.clamp(lateralError / desiredSeparation, 0f, 1f);
            float distanceUrgency =
                    1f
                            - MathUtils.clamp(
                                    (forwardDistance - PASSING_ASSIST_MIN_FORWARD_DISTANCE)
                                            / Math.max(
                                                    0.001f,
                                                    PASSING_ASSIST_RANGE
                                                            - PASSING_ASSIST_MIN_FORWARD_DISTANCE),
                                    0f,
                                    1f);
            Vector2 targetVelocity = target.body.getLinearVelocity();
            float targetSpeedAlongFollower =
                    targetVelocity.x * slipstreamSnapshotForwardX
                            + targetVelocity.y * slipstreamSnapshotForwardY;
            float closingSpeed = slipstreamSnapshotForwardSpeed - targetSpeedAlongFollower;
            float closingUrgency =
                    MathUtils.clamp(
                            (closingSpeed - PASSING_ASSIST_MIN_CLOSING_SPEED)
                                    / Math.max(1f, getBaseForwardMaxSpeed() * 0.16f),
                            0f,
                            1f);
            float urgency = Math.max(distanceUrgency, closingUrgency);
            float turnMagnitude =
                    MathUtils.lerp(
                                    PASSING_ASSIST_MIN_TURN,
                                    PASSING_ASSIST_MAX_TURN,
                                    urgency)
                            * lateralNeed;
            return direction * turnMagnitude;
        }

        private void resetPassingAssist() {
            passingAssistTarget = null;
            passingAssistDirection = 0;
            passingAssistTurnBias = 0f;
        }

        private void updateAutomaticRecoveryState(
                float delta,
                ArenaMap arenaMap,
                Array<Car> cars,
                boolean allowOffRoadRecovery,
                boolean allowContactRecovery) {
            if (!allowOffRoadRecovery && !allowContactRecovery) {
                sustainedCarContactTimer = 0f;
                sustainedOffRoadTimer = 0f;
                contactAutoRecoveryActive = false;
                offRoadAutoRecoveryActive = false;
                resetNoProgressAutoRecoveryState();
                return;
            }

            boolean offRoad = arenaMap != null && !arenaMap.supports(body.getPosition());
            if (allowOffRoadRecovery) {
                if (offRoad) {
                    sustainedOffRoadTimer += delta;
                    if (sustainedOffRoadTimer >= AUTO_RECOVERY_TRIGGER_SECONDS) {
                        offRoadAutoRecoveryActive = true;
                    }
                } else {
                    sustainedOffRoadTimer = 0f;
                    offRoadAutoRecoveryActive = false;
                    offRoadAutoRecoveryTimer = 0f;
                }
            } else {
                sustainedOffRoadTimer = 0f;
                offRoadAutoRecoveryActive = false;
                offRoadAutoRecoveryTimer = 0f;
            }

            updateNoProgressAutoRecoveryState(
                    delta,
                    arenaMap,
                    allowOffRoadRecovery && !offRoad);

            if (!allowContactRecovery) {
                sustainedCarContactTimer = 0f;
                contactAutoRecoveryActive = false;
                contactAutoRecoveryTimer = 0f;
                return;
            }

            if (contactAutoRecoveryActive) {
                return;
            }

            if (carContactCount > 0) {
                if (hasForwardMovingContactCar(cars)) {
                    sustainedCarContactTimer = 0f;
                } else {
                    sustainedCarContactTimer += delta;
                    if (sustainedCarContactTimer >= AUTO_RECOVERY_TRIGGER_SECONDS) {
                        startContactAutoRecovery(cars);
                    }
                }
            } else {
                sustainedCarContactTimer = 0f;
            }
        }

        private boolean applyAutomaticRecoveryControl(
                ArenaMap arenaMap,
                Array<Car> cars,
                float delta,
                float modelThrottle,
                float modelTurn,
                boolean allowOffRoadRecovery,
                boolean allowContactRecovery) {
            if (allowOffRoadRecovery && offRoadAutoRecoveryActive) {
                return applyOffRoadAutoRecoveryControl(arenaMap, delta);
            }
            if (allowContactRecovery && contactAutoRecoveryActive) {
                return applyContactAutoRecoveryControl(delta);
            }
            if (allowOffRoadRecovery && noProgressAutoRecoveryActive) {
                return applyNoProgressAutoRecoveryControl(arenaMap, delta);
            }
            automaticRecoveryControlDecision.set(modelThrottle, modelTurn);
            return false;
        }

        private void updateNoProgressAutoRecoveryState(
                float delta,
                ArenaMap arenaMap,
                boolean allowed) {
            if (!allowed
                    || arenaMap == null
                    || !arenaMap.hasRoute()
                    || template == null
                    || template.roundRaceFinished) {
                resetNoProgressAutoRecoveryState();
                return;
            }

            float routeProgress = template.roundRaceLastRouteProgress;
            if (Float.isNaN(routeProgress) || Float.isInfinite(routeProgress)) {
                resetNoProgressAutoRecoveryState();
                return;
            }
            if (!noProgressRouteTrackingInitialized) {
                noProgressReferenceRouteProgress = routeProgress;
                noProgressRouteTrackingInitialized = true;
                return;
            }

            float routeDelta =
                    arenaMap.routeProgressDelta(noProgressReferenceRouteProgress, routeProgress);
            if (!Float.isNaN(routeDelta)
                    && !Float.isInfinite(routeDelta)
                    && routeDelta >= NO_PROGRESS_AUTO_RECOVERY_DISTANCE) {
                sustainedNoProgressTimer = 0f;
                noProgressAutoRecoveryTimer = 0f;
                noProgressReferenceRouteProgress = routeProgress;
                noProgressAutoRecoveryActive = false;
                return;
            }

            if (!noProgressAutoRecoveryActive) {
                sustainedNoProgressTimer += delta;
                if (sustainedNoProgressTimer >= AUTO_RECOVERY_TRIGGER_SECONDS) {
                    noProgressAutoRecoveryActive = true;
                    noProgressAutoRecoveryTimer = 0f;
                    noProgressReferenceRouteProgress = routeProgress;
                }
            }
        }

        private void resetNoProgressAutoRecoveryState() {
            sustainedNoProgressTimer = 0f;
            noProgressAutoRecoveryTimer = 0f;
            noProgressReferenceRouteProgress = 0f;
            noProgressAutoRecoveryActive = false;
            noProgressRouteTrackingInitialized = false;
        }

        private boolean applyContactAutoRecoveryControl(float delta) {
            contactAutoRecoveryTimer += delta;
            float moved =
                    (body.getPosition().x - autoRecoveryStartPosition.x) * autoRecoveryDirection.x
                            + (body.getPosition().y - autoRecoveryStartPosition.y)
                                    * autoRecoveryDirection.y;
            if (moved >= CONTACT_AUTO_RECOVERY_DISTANCE
                    || contactAutoRecoveryTimer >= CONTACT_AUTO_RECOVERY_MAX_SECONDS) {
                contactAutoRecoveryActive = false;
                contactAutoRecoveryTimer = 0f;
                sustainedCarContactTimer = 0f;
                automaticRecoveryControlDecision.set(0f, 0f);
                return false;
            }

            automaticRecoveryControlDecision.set(
                    contactAutoRecoveryThrottleSign * CONTACT_AUTO_RECOVERY_THROTTLE,
                    0f);
            return true;
        }

        private boolean applyOffRoadAutoRecoveryControl(ArenaMap arenaMap, float delta) {
            offRoadAutoRecoveryTimer += delta;
            if (arenaMap == null
                    || arenaMap.supports(body.getPosition())
                    || offRoadAutoRecoveryTimer >= OFF_ROAD_AUTO_RECOVERY_MAX_SECONDS) {
                offRoadAutoRecoveryActive = false;
                offRoadAutoRecoveryTimer = 0f;
                sustainedOffRoadTimer = 0f;
                automaticRecoveryControlDecision.set(0f, 0f);
                return false;
            }

            findOffRoadAutoRecoveryTarget(arenaMap, autoRecoveryTarget);
            return applyAutoRecoveryTowardTarget(autoRecoveryTarget);
        }

        private boolean applyNoProgressAutoRecoveryControl(ArenaMap arenaMap, float delta) {
            noProgressAutoRecoveryTimer += delta;
            if (arenaMap == null
                    || !arenaMap.hasRoute()
                    || template == null
                    || template.roundRaceFinished
                    || noProgressAutoRecoveryTimer >= NO_PROGRESS_AUTO_RECOVERY_MAX_SECONDS) {
                resetNoProgressAutoRecoveryState();
                automaticRecoveryControlDecision.set(0f, 0f);
                return false;
            }

            findNoProgressAutoRecoveryTarget(arenaMap, autoRecoveryTarget);
            return applyAutoRecoveryTowardTarget(autoRecoveryTarget);
        }

        private boolean applyAutoRecoveryTowardTarget(Vector2 target) {
            autoRecoveryToTarget.set(target).sub(body.getPosition());
            if (autoRecoveryToTarget.isZero(0.0001f)) {
                automaticRecoveryControlDecision.set(0f, 0f);
                return true;
            }
            autoRecoveryToTarget.nor();
            updateAxes();
            float forwardAlignment = autoRecoveryToTarget.dot(forwardAxis);
            float sideAlignment = autoRecoveryToTarget.dot(sidewaysAxis);
            boolean reverse = forwardAlignment < -0.25f;
            float throttle =
                    reverse
                            ? -OFF_ROAD_AUTO_RECOVERY_REVERSE_THROTTLE
                            : OFF_ROAD_AUTO_RECOVERY_FORWARD_THROTTLE;
            float turn =
                    MathUtils.clamp(
                            (reverse ? sideAlignment : -sideAlignment) * 1.35f,
                            -1f,
                            1f);
            automaticRecoveryControlDecision.set(throttle, turn);
            return true;
        }

        private void findNoProgressAutoRecoveryTarget(ArenaMap arenaMap, Vector2 out) {
            float referenceProgress = template == null ? 0f : template.roundRaceLastRouteProgress;
            arenaMap.findRoutePoint(referenceProgress + NO_PROGRESS_AUTO_RECOVERY_LOOKAHEAD, out);
        }

        private void findOffRoadAutoRecoveryTarget(ArenaMap arenaMap, Vector2 out) {
            if (arenaMap.hasRoute()) {
                updateAxes();
                float referenceProgress = template == null ? 0f : template.roundRaceLastRouteProgress;
                float routeProgress =
                        arenaMap.findRouteProgressNear(
                                body.getPosition(),
                                forwardAxis,
                                referenceProgress);
                arenaMap.findRoutePoint(routeProgress, out);
                return;
            }
            arenaMap.findRecoveryPoint(body.getPosition(), out);
        }

        private void startContactAutoRecovery(Array<Car> cars) {
            updateAxes();
            Car nearest = findNearestContactCar(cars);
            float throttleSign = -1f;
            if (nearest != null && nearest.body != null) {
                Vector2 otherPosition = nearest.body.getPosition();
                float dx = otherPosition.x - body.getPosition().x;
                float dy = otherPosition.y - body.getPosition().y;
                float longitudinal = dx * forwardAxis.x + dy * forwardAxis.y;
                throttleSign = longitudinal >= 0f ? -1f : 1f;
            }
            contactAutoRecoveryThrottleSign = throttleSign;
            autoRecoveryDirection.set(forwardAxis).scl(throttleSign);
            if (autoRecoveryDirection.isZero(0.0001f)) {
                autoRecoveryDirection.set(0f, -1f);
            } else {
                autoRecoveryDirection.nor();
            }
            autoRecoveryStartPosition.set(body.getPosition());
            contactAutoRecoveryTimer = 0f;
            contactAutoRecoveryActive = true;
        }

        private boolean hasForwardMovingContactCar(Array<Car> cars) {
            if (body == null || carContactCount <= 0) {
                return false;
            }
            Car nearest = findNearestContactCar(cars);
            if (nearest == null || nearest.body == null) {
                return false;
            }
            float forwardSpeed = getSignedForwardSpeed();
            float nearestForwardSpeed = nearest.getSignedForwardSpeed();
            if (forwardSpeed <= CONTACT_AUTO_RECOVERY_FORWARD_SPEED_EPSILON
                    || nearestForwardSpeed <= CONTACT_AUTO_RECOVERY_FORWARD_SPEED_EPSILON) {
                return false;
            }
            return forwardAxis.dot(nearest.forwardAxis)
                    >= CONTACT_AUTO_RECOVERY_FORWARD_ALIGNMENT_MIN;
        }

        private Car findNearestContactCar(Array<Car> cars) {
            if (cars == null) {
                return null;
            }
            Vector2 position = body.getPosition();
            Car nearest = null;
            float nearestDistanceSquared = Float.MAX_VALUE;
            for (int i = 0; i < cars.size; i++) {
                Car other = cars.get(i);
                if (other == null || other == this || !other.active || other.body == null) {
                    continue;
                }

                Vector2 otherPosition = other.body.getPosition();
                float dx = otherPosition.x - position.x;
                float dy = otherPosition.y - position.y;
                float distanceSquared = dx * dx + dy * dy;
                float contactRange =
                        (Math.max(getWidth(), getHeight())
                                        + Math.max(other.getWidth(), other.getHeight()))
                                * CONTACT_AUTO_RECOVERY_RANGE_FACTOR;
                if (distanceSquared <= contactRange * contactRange
                        && distanceSquared < nearestDistanceSquared) {
                    nearest = other;
                    nearestDistanceSquared = distanceSquared;
                }
            }
            return nearest;
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
                fillRlObservation(
                        rlObservation,
                        0,
                        this,
                        arenaMap,
                        template == null ? 0f : template.roundRaceLastRouteProgress,
                        rlObservationForward,
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
                rlDecisionTimer = RL_LIVE_DECISION_INTERVAL;
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

        private float getBrakeSignal() {
            if (body == null) {
                return 0f;
            }
            float throttle = lastThrottleCommand;
            if (Math.abs(throttle) <= 0.001f) {
                return 0f;
            }
            float signedForwardSpeed = getSignedForwardSpeed();
            boolean braking =
                    (throttle > 0f && signedForwardSpeed < -REVERSE_ENGAGE_SPEED)
                            || (throttle < 0f && !reverseDriveActive);
            return braking ? MathUtils.clamp(Math.abs(throttle), 0f, 1f) : 0f;
        }

        private void updateSlipstream(float delta, Array<Car> cars, float throttle) {
            if (cars == null || cars.size <= 1 || !slipstreamSnapshotReady) {
                slipstreamBoost = 0f;
                return;
            }

            float targetBoost = calculateSlipstreamBoost(cars, throttle);
            float lerpSpeed =
                    targetBoost > slipstreamBoost
                            ? SLIPSTREAM_ATTACK_LERP
                            : SLIPSTREAM_RELEASE_LERP;
            float alpha = 1f - (float) Math.exp(-lerpSpeed * Math.max(0f, delta));
            slipstreamBoost +=
                    (targetBoost - slipstreamBoost) * MathUtils.clamp(alpha, 0f, 1f);
            if (slipstreamBoost < 0.01f && targetBoost <= 0f) {
                slipstreamBoost = 0f;
            }
        }

        private float calculateSlipstreamBoost(Array<Car> cars, float throttle) {
            if (throttle <= 0.05f
                    || body == null
                    || !slipstreamSnapshotOnRoad) {
                return 0f;
            }

            float forwardSpeed = slipstreamSnapshotForwardSpeed;
            float speedCap = slipstreamSnapshotSpeedCap;
            if (forwardSpeed < speedCap) {
                return 0f;
            }

            float rangeSquared = SLIPSTREAM_RANGE * SLIPSTREAM_RANGE;
            float bestBoost = 0f;
            for (int i = 0; i < cars.size; i++) {
                Car other = cars.get(i);
                if (other == null
                        || other == this
                        || !other.slipstreamSnapshotReady
                        || !other.slipstreamSnapshotOnRoad) {
                    continue;
                }

                float dx = other.slipstreamSnapshotX - slipstreamSnapshotX;
                float dy = other.slipstreamSnapshotY - slipstreamSnapshotY;
                float distanceSquared = dx * dx + dy * dy;
                if (distanceSquared > rangeSquared) {
                    continue;
                }

                float longitudinal =
                        dx * slipstreamSnapshotForwardX + dy * slipstreamSnapshotForwardY;
                if (longitudinal <= SLIPSTREAM_MIN_DISTANCE
                        || longitudinal >= SLIPSTREAM_RANGE) {
                    continue;
                }

                float lateral =
                        Math.abs(dx * slipstreamSnapshotSideX + dy * slipstreamSnapshotSideY);
                float distanceFraction =
                        MathUtils.clamp(
                                (longitudinal - SLIPSTREAM_MIN_DISTANCE)
                                        / Math.max(
                                                0.001f,
                                                SLIPSTREAM_RANGE - SLIPSTREAM_MIN_DISTANCE),
                                0f,
                                1f);
                float allowedLateral =
                        SLIPSTREAM_LATERAL_WIDTH
                                * MathUtils.lerp(0.62f, 1.0f, distanceFraction);
                if (lateral >= allowedLateral) {
                    continue;
                }

                float alignment =
                        slipstreamSnapshotForwardX * other.slipstreamSnapshotForwardX
                                + slipstreamSnapshotForwardY
                                        * other.slipstreamSnapshotForwardY;
                if (alignment < SLIPSTREAM_MIN_ALIGNMENT) {
                    continue;
                }

                float otherForwardSpeed = other.slipstreamSnapshotForwardSpeed;
                if (otherForwardSpeed < other.slipstreamSnapshotSpeedCap) {
                    continue;
                }

                float distanceScore = 1f - distanceFraction;
                float lateralScore =
                        1f - MathUtils.clamp(lateral / allowedLateral, 0f, 1f);
                float alignmentScore =
                        MathUtils.clamp(
                                (alignment - SLIPSTREAM_MIN_ALIGNMENT)
                                        / Math.max(0.001f, 1f - SLIPSTREAM_MIN_ALIGNMENT),
                                0f,
                                1f);
                float speedScore =
                        MathUtils.clamp(
                                (Math.min(forwardSpeed, otherForwardSpeed) - speedCap)
                                        / Math.max(
                                                1f,
                                                slipstreamSnapshotBaseSpeed * 0.24f),
                                0f,
                                1f);
                bestBoost =
                        Math.max(
                                bestBoost,
                                distanceScore
                                        * lateralScore
                                        * (0.35f + 0.65f * alignmentScore)
                                        * (0.35f + 0.65f * speedScore));
            }
            return MathUtils.clamp(bestBoost, 0f, 1f);
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

            Vector2 velocity = body.getLinearVelocity();
            float speed = velocity.len();
            float forwardSpeed = forwardAxis.dot(velocity);
            float lateralSpeed = sidewaysAxis.dot(velocity);
            float slipAngle =
                    MathUtils.atan2(
                            Math.abs(lateralSpeed),
                            Math.abs(forwardSpeed) + TIRE_SLIP_EPSILON);
            float slipGrip = getSlipGripMultiplier(slipAngle);
            float combinedGrip = getCombinedGripMultiplier();
            float lateralImpulse =
                    -lateralSpeed
                            * body.getMass()
                            * physics.wheelGrip
                            * gripMultiplier
                            * MathUtils.lerp(TIRE_SLIDE_GRIP_FRACTION, 1f, slipGrip);
            float maxLateralImpulse =
                    body.getMass()
                            * physics.lateralGripPerSecond
                            * physics.wheelGrip
                            * gripMultiplier
                            * slipGrip
                            * combinedGrip
                            * clampedDelta;
            lateralImpulse =
                    MathUtils.clamp(lateralImpulse, -maxLateralImpulse, maxLateralImpulse);
            working.set(sidewaysAxis).scl(lateralImpulse);
            body.applyLinearImpulse(working, body.getWorldCenter(), true);

            float slideProgress =
                    MathUtils.clamp(
                            (1f - slipGrip) / (1f - TIRE_SLIDE_GRIP_FRACTION),
                            0f,
                            1f);
            float tireScrub = slideProgress * slideProgress * (3f - 2f * slideProgress);
            if (speed > 0.001f && tireScrub > 0f) {
                float scrubDeceleration =
                        physics.lateralGripPerSecond
                                * physics.wheelGrip
                                * gripMultiplier
                                * TIRE_SLIDE_SCRUB_DECELERATION_FRACTION
                                * tireScrub;
                float scrubForce =
                        Math.min(
                                body.getMass() * scrubDeceleration,
                                body.getMass() * speed / clampedDelta);
                working.set(velocity).nor().scl(-scrubForce);
                body.applyForceToCenter(working, true);
            }

            float yawImpulse =
                    -body.getAngularVelocity()
                            * body.getInertia()
                            * physics.yawGripPerSecond
                            * physics.wheelGrip
                            * gripMultiplier
                            * MathUtils.lerp(0.48f, 1f, slipGrip)
                            * clampedDelta;
            body.applyAngularImpulse(yawImpulse, true);

            if (speed > 0.001f) {
                working.set(velocity).scl(-physics.aeroDrag * speed * dragMultiplier);
                body.applyForceToCenter(working, true);
            }
            if (Math.abs(forwardSpeed) > 0.04f) {
                working.set(forwardAxis)
                        .scl(-Math.signum(forwardSpeed)
                                * physics.rollingResistance
                                * body.getMass()
                                * dragMultiplier);
                body.applyForceToCenter(working, true);
            }
        }

        private void drive(float throttle, float turn, ArenaMap arenaMap) {
            updateAxes();
            float signedForwardSpeed = forwardAxis.dot(body.getLinearVelocity());
            CarPhysics physics = physics();
            boolean reverseDriveEngaged = updateReverseDriveEngagement(throttle, signedForwardSpeed);

            float engineForce = 0f;
            float trackLimitForwardAccelerationMultiplier = 1f;
            if (throttle != 0f) {
                boolean braking =
                        (throttle > 0f && signedForwardSpeed < -REVERSE_ENGAGE_SPEED)
                                || (throttle < 0f && signedForwardSpeed > REVERSE_ENGAGE_SPEED);
                if (braking) {
                    applyServiceBrake(Math.abs(throttle), physics);
                } else if (throttle > 0f) {
                    trackLimitForwardAccelerationMultiplier =
                            getTrackLimitForwardAccelerationMultiplier(arenaMap);
                    float speedRatio =
                            MathUtils.clamp(
                                    Math.abs(signedForwardSpeed) / getForwardMaxSpeed(),
                                    0f,
                                    1f);
                    engineForce =
                            limitLongitudinalForce(
                                    throttle
                                            * physics.engineForce()
                                            * enginePowerCurve(speedRatio)
                                            * trackLimitForwardAccelerationMultiplier,
                                    physics,
                                    false);
                } else if (reverseDriveEngaged) {
                    float speedRatio =
                            MathUtils.clamp(
                                    Math.abs(signedForwardSpeed) / getReverseMaxSpeed(),
                                    0f,
                                    1f);
                    engineForce =
                            limitLongitudinalForce(
                                    throttle
                                            * physics.engineForce()
                                            * physics.reversePowerMultiplier
                                            * enginePowerCurve(speedRatio),
                                    physics,
                                    false);
                } else {
                    applyServiceBrake(Math.abs(throttle), physics);
                }
            }

            if (engineForce != 0f) {
                working.set(forwardAxis).scl(engineForce);
                body.applyForceToCenter(working, true);
            }
            if (slipstreamBoost > 0.01f
                    && throttle > 0f
                    && signedForwardSpeed
                            > getBaseForwardMaxSpeed() * SLIPSTREAM_SPEED_CAP_RATIO) {
                float draftForce =
                        limitLongitudinalForce(
                                physics.engineForce()
                                        * SLIPSTREAM_ENGINE_FORCE_BONUS
                                        * slipstreamBoost
                                        * MathUtils.clamp(throttle, 0f, 1f)
                                        * trackLimitForwardAccelerationMultiplier,
                                physics,
                                false);
                working.set(forwardAxis).scl(draftForce);
                body.applyForceToCenter(working, true);
            }

            float steeringStrength = getSteeringAuthority();
            float turnTorque = physics.steeringTorque;
            if (growthBoosted) {
                steeringStrength = MathUtils.clamp(steeringStrength * 0.96f, 0.30f, 1.3f);
                turnTorque *= GROWTH_TURN_MULTIPLIER;
            }
            turnTorque *= getSteeringInertiaCompensation();

            applySteering(
                    turn,
                    turnTorque,
                    steeringStrength,
                    signedForwardSpeed);

            applySoftSpeedLimit(reverseDriveEngaged, physics);
        }

        private float getTrackLimitForwardAccelerationMultiplier(ArenaMap arenaMap) {
            float severity = getTrackLimitSlowSeverity(arenaMap, body.getPosition());
            return MathUtils.lerp(
                    1f,
                    TRACK_LIMIT_FORWARD_ACCELERATION_MULTIPLIER,
                    severity);
        }

        private boolean updateReverseDriveEngagement(float throttle, float signedForwardSpeed) {
            if (throttle >= -0.1f) {
                reverseDriveActive = false;
                return false;
            }
            if (signedForwardSpeed > REVERSE_ENGAGE_SPEED) {
                reverseDriveActive = false;
                return false;
            }
            if (reverseDriveActive) {
                return true;
            }
            if (body.getLinearVelocity().len() > REVERSE_ENGAGE_STOP_SPEED) {
                return false;
            }

            reverseDriveActive = true;
            return true;
        }

        private float getSteeringMotionFactor() {
            float movingSpeed = body.getLinearVelocity().len();
            float speedFactor = MathUtils.clamp(movingSpeed / 1.4f, 0f, 1f);
            return speedFactor * speedFactor * (3f - 2f * speedFactor);
        }

        private void applySteering(
                float turn,
                float turnTorque,
                float steeringStrength,
                float signedForwardSpeed) {
            float motionFactor = getSteeringMotionFactor();
            if (Math.abs(turn) <= 0.001f || motionFactor <= 0.001f) {
                return;
            }

            float speedRatio =
                    MathUtils.clamp(
                            Math.abs(signedForwardSpeed) / Math.max(1f, getForwardMaxSpeed()),
                            0f,
                            1f);
            float maxSteerAngle =
                    MathUtils.lerp(
                            MAX_LOW_SPEED_STEER_ANGLE,
                            MAX_HIGH_SPEED_STEER_ANGLE,
                            (float) Math.pow(speedRatio, 1.25f));
            float wheelbase = Math.max(MIN_WHEELBASE, HEIGHT * sizeScale * 0.78f);
            float desiredAngularVelocity =
                    (signedForwardSpeed / wheelbase)
                            * (float) Math.tan(turn * maxSteerAngle * steeringStrength)
                            * motionFactor;
            float maxYawRate = MathUtils.lerp(4.8f, 2.4f, speedRatio);
            desiredAngularVelocity =
                    MathUtils.clamp(desiredAngularVelocity, -maxYawRate, maxYawRate);
            float maxAngularVelocityChange =
                    turnTorque * MathUtils.lerp(0.52f, 0.38f, speedRatio) * PHYSICS_STEP;
            float angularVelocityDelta =
                    MathUtils.clamp(
                            desiredAngularVelocity - body.getAngularVelocity(),
                            -maxAngularVelocityChange,
                            maxAngularVelocityChange);
            body.applyAngularImpulse(angularVelocityDelta * body.getInertia(), true);
        }

        private void applyServiceBrake(float throttleMagnitude, CarPhysics physics) {
            Vector2 velocity = body.getLinearVelocity();
            float speedToRemove = velocity.len();
            if (speedToRemove <= 0.001f || throttleMagnitude <= 0f) {
                return;
            }

            float maxBrakeForce =
                    speedToRemove * body.getMass() / Math.max(0.001f, PHYSICS_STEP);
            float brakeForce =
                    Math.min(
                            limitLongitudinalForce(
                                    physics.brakeForce * throttleMagnitude,
                                    physics,
                                    true),
                            maxBrakeForce);
            working.set(velocity).nor().scl(-brakeForce);
            body.applyForceToCenter(working, true);
        }

        private float getSlipGripMultiplier(float slipAngle) {
            if (slipAngle <= TIRE_PEAK_SLIP_ANGLE) {
                return 1f;
            }
            float slide =
                    MathUtils.clamp(
                            (slipAngle - TIRE_PEAK_SLIP_ANGLE)
                                    / (TIRE_SLIDE_SLIP_ANGLE - TIRE_PEAK_SLIP_ANGLE),
                            0f,
                            1f);
            return MathUtils.lerp(1f, TIRE_SLIDE_GRIP_FRACTION, slide);
        }

        private float getCombinedGripMultiplier() {
            float brakeDemand = getBrakeSignal() * BRAKE_COMBINED_SLIP_WEIGHT;
            float accelDemand =
                    Math.max(0f, Math.abs(lastThrottleCommand) - brakeDemand)
                            * ACCEL_COMBINED_SLIP_WEIGHT;
            float longitudinalDemand = MathUtils.clamp(Math.max(brakeDemand, accelDemand), 0f, 0.82f);
            return MathUtils.clamp(
                    (float) Math.sqrt(1f - longitudinalDemand * longitudinalDemand),
                    0.56f,
                    1f);
        }

        private float limitLongitudinalForce(
                float requestedForce,
                CarPhysics physics,
                boolean braking) {
            float maxForce =
                    body.getMass()
                            * physics.lateralGripPerSecond
                            * physics.wheelGrip
                            * (braking ? BRAKE_TRACTION_MULTIPLIER : DRIVE_TRACTION_MULTIPLIER);
            return MathUtils.clamp(requestedForce, -maxForce, maxForce);
        }

        private void applySoftSpeedLimit(boolean reverseDriveEngaged, CarPhysics physics) {
            float signedForwardSpeed = forwardAxis.dot(body.getLinearVelocity());
            float maxSpeed = reverseDriveEngaged ? getReverseMaxSpeed() : getForwardMaxSpeed();
            float overspeed = Math.abs(signedForwardSpeed) - maxSpeed;
            if (overspeed <= 0f) {
                return;
            }
            float force =
                    body.getMass()
                            * Math.min(
                                    physics.brakeForce / Math.max(0.001f, body.getMass()),
                                    SOFT_SPEED_LIMIT_ACCEL + overspeed * 1.15f);
            working.set(forwardAxis).scl(-Math.signum(signedForwardSpeed) * force);
            body.applyForceToCenter(working, true);
        }

        private CarPhysics physics() {
            return template.physics;
        }

        private float getForwardMaxSpeed() {
            return getBaseForwardMaxSpeed()
                    * (1f + SLIPSTREAM_MAX_SPEED_BONUS * slipstreamBoost);
        }

        private float getBaseForwardMaxSpeed() {
            return physics().maxForwardSpeed * (growthBoosted ? MAX_GROWTH_SPEED_MULTIPLIER : 1f);
        }

        private float getSlipstreamBoost() {
            return slipstreamBoost;
        }

        private float getReverseMaxSpeed() {
            return getForwardMaxSpeed() * physics().reverseSpeedMultiplier;
        }

        private float enginePowerCurve(float speedRatio) {
            float clamped = MathUtils.clamp(speedRatio, 0f, 1f);
            return Math.max(0f, 1f - (float) Math.pow(clamped, 2.35f));
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
            authority *= 1f - MathUtils.clamp(slipRatio * 0.22f, 0f, 0.20f);
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
            float deceleration = Math.max(0.001f, getMaximumServiceBrakeDeceleration());
            return speed * speed / (2f * deceleration);
        }

        private float getMaximumServiceBrakeDeceleration() {
            if (body == null) {
                return 0f;
            }
            CarPhysics physics = physics();
            float requestedDeceleration =
                    physics.brakeForce / Math.max(0.001f, body.getMass());
            float gripLimitedDeceleration =
                    physics.lateralGripPerSecond
                            * physics.wheelGrip
                            * BRAKE_TRACTION_MULTIPLIER;
            return Math.max(0f, Math.min(requestedDeceleration, gripLimitedDeceleration));
        }

        private float getMaximumLateralAcceleration() {
            CarPhysics physics = physics();
            return Math.max(0f, physics.lateralGripPerSecond * physics.wheelGrip);
        }

        private void applyTrackLimitSlowdown(ArenaMap arenaMap) {
            if (!active || body == null || arenaMap == null) {
                return;
            }

            Vector2 position = body.getPosition();
            float slowSeverity = getTrackLimitSlowSeverity(arenaMap, position);
            if (slowSeverity <= 0f) {
                return;
            }

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

        private static boolean isTrackLimitSlowdownActive(ArenaMap arenaMap, Vector2 position) {
            return getTrackLimitSlowSeverity(arenaMap, position) > 0f;
        }

        private static float getTrackLimitSlowSeverity(ArenaMap arenaMap, Vector2 position) {
            if (arenaMap == null || position == null) {
                return 0f;
            }
            if (!arenaMap.supports(position)) {
                return 1f;
            }
            float edgeDistance = arenaMap.distanceToHazard(position);
            if (edgeDistance >= TRACK_LIMIT_SLOW_MARGIN) {
                return 0f;
            }
            return 1f - MathUtils.clamp(edgeDistance / TRACK_LIMIT_SLOW_MARGIN, 0f, 1f);
        }

        private static float getTrackLimitPenaltyDistance(ArenaMap arenaMap, Vector2 position) {
            if (arenaMap == null || position == null) {
                return 0f;
            }
            if (!arenaMap.supports(position)) {
                return TRACK_LIMIT_SLOW_MARGIN + arenaMap.distanceToSafety(position);
            }
            return Math.max(0f, TRACK_LIMIT_SLOW_MARGIN - arenaMap.distanceToHazard(position));
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

        private void beginCarContact() {
            carContactCount++;
        }

        private void endCarContact() {
            carContactCount = Math.max(0, carContactCount - 1);
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
            sustainedCarContactTimer = 0f;
            sustainedOffRoadTimer = 0f;
            contactAutoRecoveryTimer = 0f;
            offRoadAutoRecoveryTimer = 0f;
            resetNoProgressAutoRecoveryState();
            slipstreamBoost = 0f;
            slipstreamSnapshotReady = false;
            slipstreamSnapshotOnRoad = false;
            resetPassingAssist();
            contactAutoRecoveryActive = false;
            offRoadAutoRecoveryActive = false;
            contactAutoRecoveryThrottleSign = -1f;
            reverseDriveActive = false;
            carContactCount = 0;
            lastThrottleCommand = 0f;
            lastTurnCommand = 0f;
            externalControlDecision.set(0f, 0f);
            rawExternalControlDecision.set(0f, 0f);
            automaticRecoveryControlDecision.set(0f, 0f);
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

    }

    private static float getRlSensorLookaheadDistance(Car car) {
        if (car == null || car.body == null) {
            return RL_SENSOR_MIN_LOOKAHEAD_DISTANCE;
        }
        float speed = car.body.getLinearVelocity().len();
        return MathUtils.clamp(
                speed * RL_SENSOR_LOOKAHEAD_SECONDS + car.getEstimatedBrakingDistance(),
                RL_SENSOR_MIN_LOOKAHEAD_DISTANCE,
                RL_SENSOR_MAX_LOOKAHEAD_DISTANCE);
    }

    private static float getRlSteeringLookaheadDistance(Car car) {
        if (car == null || car.body == null) {
            return RL_STEERING_MIN_LOOKAHEAD_DISTANCE;
        }
        return MathUtils.clamp(
                car.body.getLinearVelocity().len() * RL_STEERING_LOOKAHEAD_SECONDS,
                RL_STEERING_MIN_LOOKAHEAD_DISTANCE,
                RL_STEERING_MAX_LOOKAHEAD_DISTANCE);
    }

    private static float getRlBrakeDemand(
            Car car,
            ArenaMap arenaMap,
            float routeProgress,
            float routeForwardSpeed,
            float lookaheadDistance) {
        if (car == null
                || car.body == null
                || arenaMap == null
                || !arenaMap.hasRoute()
                || routeForwardSpeed <= 0.05f) {
            return 0f;
        }

        float brakingDeceleration = car.getMaximumServiceBrakeDeceleration();
        float lateralAcceleration = car.getMaximumLateralAcceleration();
        if (brakingDeceleration <= 0.001f || lateralAcceleration <= 0.001f) {
            return 0f;
        }

        float scanDistance = Math.min(lookaheadDistance, arenaMap.getRouteLength());
        int sampleCount = Math.max(1, MathUtils.ceil(scanDistance / RL_BRAKE_DEMAND_SAMPLE_STEP));
        float speedSquared = routeForwardSpeed * routeForwardSpeed;
        float brakeDemand = 0f;
        for (int sample = 0; sample <= sampleCount; sample++) {
            float distance = Math.min(scanDistance, sample * RL_BRAKE_DEMAND_SAMPLE_STEP);
            float curvature =
                    Math.abs(arenaMap.getRouteCurvaturePerWorldUnit(routeProgress + distance));
            if (curvature <= RL_BRAKE_DEMAND_CURVATURE_EPSILON) {
                continue;
            }

            float safeSpeedSquared = lateralAcceleration / curvature;
            if (speedSquared <= safeSpeedSquared) {
                continue;
            }
            float availableDistance = Math.max(Car.HEIGHT * 0.5f, distance);
            float requiredDeceleration =
                    (speedSquared - safeSpeedSquared) / (2f * availableDistance);
            brakeDemand =
                    Math.max(
                            brakeDemand,
                            MathUtils.clamp(
                                    requiredDeceleration / brakingDeceleration,
                                    0f,
                                    1f));
            if (brakeDemand >= 1f) {
                return 1f;
            }
        }
        return brakeDemand;
    }

    private static void fillRlObservation(
            float[] observations,
            int offset,
            Car car,
            ArenaMap arenaMap,
            float referenceRouteProgress,
            Vector2 observationForward,
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
        observationForward.set(car.body.getWorldVector(observationForward.set(0f, 1f)));
        observationSide.set(-observationForward.y, observationForward.x);

        float routeProgress = arenaMap.findRouteProgressNear(
                position,
                observationForward,
                referenceRouteProgress);
        observationRouteTarget.set(0f, 1f);
        arenaMap.findRouteTangent(routeProgress, observationRouteTarget);
        float routeTangentX = observationRouteTarget.x;
        float routeTangentY = observationRouteTarget.y;
        float routeNormalX = -routeTangentY;
        float routeNormalY = routeTangentX;
        float routeTangentForward =
                MathUtils.clamp(observationRouteTarget.dot(observationForward), -1f, 1f);
        float routeTangentSide =
                MathUtils.clamp(observationRouteTarget.dot(observationSide), -1f, 1f);

        arenaMap.findRoutePoint(routeProgress, observationRouteTarget);
        float centerlineDx = observationRouteTarget.x - position.x;
        float centerlineDy = observationRouteTarget.y - position.y;
        float lateralOffset = -(centerlineDx * routeNormalX + centerlineDy * routeNormalY);
        float routeLeftMargin =
                arenaMap.getRouteLeftClearance(routeProgress) - lateralOffset;
        float routeRightMargin =
                arenaMap.getRouteRightClearance(routeProgress) + lateralOffset;

        float sensorLookaheadDistance = getRlSensorLookaheadDistance(car);
        float steeringLookaheadDistance = getRlSteeringLookaheadDistance(car);
        float steeringRouteProgress = routeProgress + steeringLookaheadDistance;
        float targetForward = routeTangentForward;
        float targetSide = routeTangentSide;
        float routeCurvature = 0f;
        float targetCurvature = 0f;
        float targetRouteLeftClearance = 0f;
        float targetRouteRightClearance = 0f;
        if (arenaMap.hasRoute()) {
            arenaMap.findRoutePoint(steeringRouteProgress, observationRouteTarget);
            float targetDx = observationRouteTarget.x - position.x;
            float targetDy = observationRouteTarget.y - position.y;
            float targetDistance = (float) Math.sqrt(targetDx * targetDx + targetDy * targetDy);
            if (targetDistance > 0.0001f) {
                float targetUnitX = targetDx / targetDistance;
                float targetUnitY = targetDy / targetDistance;
                targetForward =
                        MathUtils.clamp(
                                targetUnitX * observationForward.x
                                        + targetUnitY * observationForward.y,
                                -1f,
                                1f);
                targetSide =
                        MathUtils.clamp(
                                targetUnitX * observationSide.x
                                        + targetUnitY * observationSide.y,
                                -1f,
                                1f);
            }
            routeCurvature = arenaMap.getRouteCurvature(routeProgress);
            targetCurvature = arenaMap.getRouteCurvature(steeringRouteProgress);
            targetRouteLeftClearance = arenaMap.getRouteLeftClearance(steeringRouteProgress);
            targetRouteRightClearance = arenaMap.getRouteRightClearance(steeringRouteProgress);
        }

        float forwardSpeed = observationForward.dot(velocity);
        float lateralSpeed = observationSide.dot(velocity);
        float routeForwardSpeed = velocity.x * routeTangentX + velocity.y * routeTangentY;
        float routeLateralSpeed = velocity.x * routeNormalX + velocity.y * routeNormalY;
        float brakeDemand =
                getRlBrakeDemand(
                        car,
                        arenaMap,
                        routeProgress,
                        routeForwardSpeed,
                        sensorLookaheadDistance);
        float slipAngle =
                MathUtils.clamp(
                        (float) Math.atan2(lateralSpeed, Math.max(Math.abs(forwardSpeed), 0.001f))
                                / (MathUtils.PI * 0.5f),
                        -1f,
                        1f);
        float maxForwardSpeed = Math.max(0.001f, car.getForwardMaxSpeed());
        boolean offRoad = Car.isTrackLimitSlowdownActive(arenaMap, position);
        float offRoadDistance = offRoad ? Car.getTrackLimitPenaltyDistance(arenaMap, position) : 0f;
        float trajectoryDirectionX = velocity.len2() > 0.25f ? velocity.x : observationForward.x;
        float trajectoryDirectionY = velocity.len2() > 0.25f ? velocity.y : observationForward.y;
        float trajectoryClearance =
                offRoad
                        ? 0f
                        : sampleRlRayClearance(
                                arenaMap,
                                position,
                                trajectoryDirectionX,
                                trajectoryDirectionY,
                                sensorLookaheadDistance);
        float leftRoadClearance =
                sampleRlRayClearance(
                        arenaMap,
                        position,
                        observationSide.x,
                        observationSide.y,
                        RL_LATERAL_ROAD_CLEARANCE_DISTANCE);
        float rightRoadClearance =
                sampleRlRayClearance(
                        arenaMap,
                        position,
                        -observationSide.x,
                        -observationSide.y,
                        RL_LATERAL_ROAD_CLEARANCE_DISTANCE);
        float frontRoadClearance =
                sampleRlRayClearance(
                        arenaMap,
                        position,
                        observationForward.x,
                        observationForward.y,
                        RL_FRONT_ROAD_CLEARANCE_DISTANCE);
        float frontLeftRoadClearance =
                sampleRlRayClearance(
                        arenaMap,
                        position,
                        observationForward.x + observationSide.x,
                        observationForward.y + observationSide.y,
                        RL_FRONT_DIAGONAL_ROAD_CLEARANCE_DISTANCE);
        float frontRightRoadClearance =
                sampleRlRayClearance(
                        arenaMap,
                        position,
                        observationForward.x - observationSide.x,
                        observationForward.y - observationSide.y,
                        RL_FRONT_DIAGONAL_ROAD_CLEARANCE_DISTANCE);
        float recoveryForward = 0f;
        float recoverySide = 0f;
        float recoveryDistance =
                (float) Math.sqrt(centerlineDx * centerlineDx + centerlineDy * centerlineDy);
        if (offRoad && recoveryDistance > 0.0001f) {
            float recoveryUnitX = centerlineDx / recoveryDistance;
            float recoveryUnitY = centerlineDy / recoveryDistance;
            recoveryForward =
                    MathUtils.clamp(
                            recoveryUnitX * observationForward.x
                                    + recoveryUnitY * observationForward.y,
                            -1f,
                            1f);
            recoverySide =
                    MathUtils.clamp(
                            recoveryUnitX * observationSide.x
                                    + recoveryUnitY * observationSide.y,
                            -1f,
                            1f);
        }

        observations[offset] = routeTangentForward;
        observations[offset + 1] = routeTangentSide;
        observations[offset + 2] = targetForward;
        observations[offset + 3] = targetSide;
        observations[offset + 4] = routeCurvature;
        observations[offset + 5] = targetCurvature;
        observations[offset + 6] = normalizedRlValue(routeLeftMargin, RL_ROUTE_MARGIN_NORMALIZER);
        observations[offset + 7] = normalizedRlValue(routeRightMargin, RL_ROUTE_MARGIN_NORMALIZER);
        observations[offset + 8] =
                MathUtils.clamp(targetRouteLeftClearance / RL_ROUTE_MARGIN_NORMALIZER, 0f, 1f);
        observations[offset + 9] =
                MathUtils.clamp(targetRouteRightClearance / RL_ROUTE_MARGIN_NORMALIZER, 0f, 1f);
        observations[offset + 10] = normalizedRlValue(forwardSpeed, maxForwardSpeed);
        observations[offset + 11] = normalizedRlValue(routeForwardSpeed, maxForwardSpeed);
        observations[offset + 12] = normalizedRlValue(routeLateralSpeed, maxForwardSpeed);
        observations[offset + 13] =
                normalizedRlValue(car.body.getAngularVelocity(), RL_ANGULAR_VELOCITY_NORMALIZER);
        observations[offset + 14] = slipAngle;
        observations[offset + 15] =
                MathUtils.clamp(offRoadDistance / RL_OFF_ROAD_DISTANCE_NORMALIZER, 0f, 1f);
        observations[offset + 16] = trajectoryClearance;
        observations[offset + 17] = brakeDemand;
        observations[offset + 18] = recoveryForward;
        observations[offset + 19] = recoverySide;
        observations[offset + 20] = leftRoadClearance;
        observations[offset + 21] = rightRoadClearance;
        observations[offset + 22] = frontRoadClearance;
        observations[offset + 23] = frontLeftRoadClearance;
        observations[offset + 24] = frontRightRoadClearance;
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

    private static float normalizedRlValue(float value, float normalizer) {
        return MathUtils.clamp(value / Math.max(0.0001f, normalizer), -1f, 1f);
    }

    public static final class RlTrainingConfig {
        public final Array<ArenaMap> maps = new Array<ArenaMap>();
        public int controlledAgentCount = RL_DEFAULT_CONTROLLED_AGENTS;
        public int fieldSize = RL_DEFAULT_FIELD_SIZE;
        public int actionRepeat = RL_DEFAULT_ACTION_REPEAT;
        public int maxActionSteps = RL_DEFAULT_MAX_ACTION_STEPS;
        public int noProgressMaxActionSteps = RL_DEFAULT_NO_PROGRESS_MAX_ACTION_STEPS;
        public int offRoadFailureMaxActionSteps =
                RL_DEFAULT_OFF_ROAD_FAILURE_MAX_ACTION_STEPS;
        public int routeTargets = RL_DEFAULT_ROUTE_TARGETS;
        public float routeTargetFraction;
        public long seed = 1L;
        public boolean skipCountdown = true;
        public boolean raceMode = true;
        public boolean randomRaceSpawns;
        public boolean debugTraceEnabled;
        public boolean rewardBreakdownEnabled = true;
        public boolean stepDetailsEnabled = true;
        public float stepPenalty = RL_STEP_PENALTY;
        public float progressReward = RL_PROGRESS_REWARD;
        public float routeAlignmentReward = RL_ROUTE_ALIGNMENT_REWARD;
        public float steeringPenalty = RL_STEERING_PENALTY;
        public float reverseSpeedFreeEpsilon = RL_REVERSE_SPEED_FREE_EPSILON;
        public float reverseSpeedPenaltyPerUnit = RL_REVERSE_SPEED_PENALTY_PER_UNIT;
        public float reverseSpeedMaxPenalty = RL_REVERSE_SPEED_MAX_PENALTY;
        public float carPushPenalty = RL_CAR_PUSH_PENALTY;
        public float carPushMaxStepPenalty = RL_CAR_PUSH_MAX_STEP_PENALTY;
        public float offRoadPenalty = RL_OFF_ROAD_PENALTY;
        public float offRoadDistancePenalty = RL_OFF_ROAD_DISTANCE_PENALTY;
        public float offRoadMaxPenalty = RL_OFF_ROAD_MAX_PENALTY;
        public float noProgressPenalty = RL_NO_PROGRESS_PENALTY;
        public float offRoadRecoveryReward = RL_OFF_ROAD_RECOVERY_REWARD;
        public float offRoadFailurePenalty = RL_OFF_ROAD_FAILURE_PENALTY;

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

        public RlTrainingConfig withNoProgressMaxActionSteps(int noProgressMaxActionSteps) {
            this.noProgressMaxActionSteps = noProgressMaxActionSteps;
            return this;
        }

        public RlTrainingConfig withOffRoadFailureMaxActionSteps(
                int offRoadFailureMaxActionSteps) {
            this.offRoadFailureMaxActionSteps = offRoadFailureMaxActionSteps;
            return this;
        }

        public RlTrainingConfig withRouteTargets(int routeTargets) {
            this.routeTargets = routeTargets;
            return this;
        }

        public RlTrainingConfig withRouteTargetFraction(float routeTargetFraction) {
            this.routeTargetFraction = routeTargetFraction;
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

        public RlTrainingConfig withRewardBreakdownEnabled(boolean rewardBreakdownEnabled) {
            this.rewardBreakdownEnabled = rewardBreakdownEnabled;
            return this;
        }

        public RlTrainingConfig withStepDetailsEnabled(boolean stepDetailsEnabled) {
            this.stepDetailsEnabled = stepDetailsEnabled;
            return this;
        }

        public RlTrainingConfig withStepPenalty(float stepPenalty) {
            this.stepPenalty = stepPenalty;
            return this;
        }

        public RlTrainingConfig withProgressReward(float progressReward) {
            this.progressReward = progressReward;
            return this;
        }

        public RlTrainingConfig withRouteAlignmentReward(float routeAlignmentReward) {
            this.routeAlignmentReward = routeAlignmentReward;
            return this;
        }

        public RlTrainingConfig withSteeringPenalty(float steeringPenalty) {
            this.steeringPenalty = steeringPenalty;
            return this;
        }

        public RlTrainingConfig withReverseSpeedPenalty(
                float freeEpsilon,
                float penaltyPerUnit,
                float maxPenalty) {
            this.reverseSpeedFreeEpsilon = freeEpsilon;
            this.reverseSpeedPenaltyPerUnit = penaltyPerUnit;
            this.reverseSpeedMaxPenalty = maxPenalty;
            return this;
        }

        public RlTrainingConfig withCarPushPenalty(float penalty, float maxStepPenalty) {
            this.carPushPenalty = penalty;
            this.carPushMaxStepPenalty = maxStepPenalty;
            return this;
        }

        public RlTrainingConfig withOffRoadPenalty(
                float penalty,
                float distancePenalty,
                float maxPenalty) {
            this.offRoadPenalty = penalty;
            this.offRoadDistancePenalty = distancePenalty;
            this.offRoadMaxPenalty = maxPenalty;
            return this;
        }

        public RlTrainingConfig withNoProgressPenalty(float noProgressPenalty) {
            this.noProgressPenalty = noProgressPenalty;
            return this;
        }

        public RlTrainingConfig withOffRoadRecoveryReward(float offRoadRecoveryReward) {
            this.offRoadRecoveryReward = offRoadRecoveryReward;
            return this;
        }

        public RlTrainingConfig withOffRoadFailurePenalty(float offRoadFailurePenalty) {
            this.offRoadFailurePenalty = offRoadFailurePenalty;
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
        public final boolean episodeTerminated;
        public final boolean episodeTruncated;
        public final int actionStep;
        public final int winnerAgentIndex;
        public final String winnerLabel;
        public final String currentMapId;
        public final String currentMapName;
        public final int[] routeTargetsReached;
        public final float[] routeProgressDeltas;
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
                boolean episodeTerminated,
                boolean episodeTruncated,
                int actionStep,
                int winnerAgentIndex,
                String winnerLabel,
                String currentMapId,
                String currentMapName,
                int[] routeTargetsReached,
                float[] routeProgressDeltas,
                float[] debugTrace,
                String[] debugTraceNames) {
            this.observations = observations;
            this.rewards = rewards;
            this.rewardBreakdown = rewardBreakdown;
            this.rewardBreakdownNames = rewardBreakdownNames;
            this.effectiveActions = effectiveActions;
            this.dones = dones;
            this.episodeDone = episodeDone;
            this.episodeTerminated = episodeTerminated;
            this.episodeTruncated = episodeTruncated;
            this.actionStep = actionStep;
            this.winnerAgentIndex = winnerAgentIndex;
            this.winnerLabel = winnerLabel;
            this.currentMapId = currentMapId;
            this.currentMapName = currentMapName;
            this.routeTargetsReached = routeTargetsReached;
            this.routeProgressDeltas = routeProgressDeltas;
            this.debugTrace = debugTrace;
            this.debugTraceNames = debugTraceNames;
        }
    }

    public static final class RlTrainingEnvironment implements AutoCloseable {
        private final RlTrainingConfig config;
        private final RatassGame game = new RatassGame();
        private final Array<ArenaMap> trainingMaps;
        private final MapProgression trainingMapProgression;
        private final Array<Integer> controlledVehicleIds = new Array<Integer>();
        private final Vector2 observationForward = new Vector2();
        private final Vector2 observationRecovery = new Vector2();
        private final Vector2 observationRouteTarget = new Vector2();
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
        private final float[] routeProgressDeltas;
        private final float[] progressSinceDeadlineReset;
        private final int[] actionsSinceProgress;
        private final int[] consecutiveOffRoadActions;
        private final float[] bestRaceRouteProgress;
        private final float[] routeDistanceSinceLastTarget;
        private final int[] raceNextCheckpointIndices;
        private final int[] raceTargetSequences;
        private final int[] routeTargetsReached;
        private final int[] checkpointCrossRewardSequences;
        private final boolean[] checkpointCrossRewardEvents;
        private final boolean[] checkpointTimeoutEvents;
        private final boolean[] raceCheckpointCrossEvents;
        private final boolean[] dones;
        private final int controlledAgentCount;
        private int episodeIndex;
        private int actionStep;
        private boolean episodeStarted;
        private boolean episodeDone;
        private boolean episodeTerminated;
        private boolean episodeTruncated;
        private boolean episodeNoProgressFailure;
        private boolean episodeOffRoadFailure;
        private boolean closed;

        public RlTrainingEnvironment(RlTrainingConfig config) {
            this.config = config == null ? new RlTrainingConfig() : config;
            this.config.raceMode = true;
            trainingMaps =
                    this.config.maps.size == 0
                            ? ArenaMaps.createHeadlessTrainingSet()
                            : this.config.maps;
            trainingMapProgression =
                    new MapProgression(
                            trainingMaps,
                            new Random(this.config.seed ^ 0x9E3779B97F4A7C15L));
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
            routeProgressDeltas = new float[controlledAgentCount];
            progressSinceDeadlineReset = new float[controlledAgentCount];
            actionsSinceProgress = new int[controlledAgentCount];
            consecutiveOffRoadActions = new int[controlledAgentCount];
            bestRaceRouteProgress = new float[controlledAgentCount];
            routeDistanceSinceLastTarget = new float[controlledAgentCount];
            raceNextCheckpointIndices = new int[controlledAgentCount];
            raceTargetSequences = new int[controlledAgentCount];
            routeTargetsReached = new int[controlledAgentCount];
            checkpointCrossRewardSequences = new int[controlledAgentCount];
            checkpointCrossRewardEvents = new boolean[controlledAgentCount];
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

        public String[] getObservationNames() {
            return RL_OBSERVATION_NAMES.clone();
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
            if (episodeIndex > 1) {
                trainingMapProgression.advance();
            }
            game.mapProgression = trainingMapProgression;
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
            episodeTerminated = false;
            episodeTruncated = false;
            episodeNoProgressFailure = false;
            episodeOffRoadFailure = false;
            clearEpisodeMetrics();
            clearRewards();
            game.checkpointTargetActive = false;
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
            captureBeforeSnapshots();
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
            updateRouteProgress();
            updateNoProgressState();
            updateOffRoadFailureState();
            boolean trainingRaceCompleted = hasCompletedTrainingRace();
            episodeNoProgressFailure =
                    !game.roundOver && !trainingRaceCompleted && hasNoProgressTimeout();
            episodeOffRoadFailure =
                    !game.roundOver && !trainingRaceCompleted && hasOffRoadFailureTimeout();
            episodeTerminated =
                    game.roundOver
                            || trainingRaceCompleted
                            || episodeNoProgressFailure
                            || episodeOffRoadFailure;
            episodeTruncated = !episodeTerminated && maxStepsReached;
            episodeDone = episodeTerminated || episodeTruncated;
            buildObservations();
            computeRewards();
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

        private int getRequiredRouteTargets() {
            if (config.raceMode && config.routeTargets < 0) {
                return 1;
            }
            if (config.raceMode && config.routeTargets == 0) {
                return Math.max(1, game.getRaceLapsToWin());
            }
            return config.routeTargets <= 0 ? RL_DEFAULT_ROUTE_TARGETS : config.routeTargets;
        }

        private float getRouteTargetDistance() {
            if (game.currentMap == null || !game.currentMap.hasRoute()) {
                return 0f;
            }
            float routeLength = game.currentMap.getRouteLength();
            if (config.routeTargetFraction > 0f && config.randomRaceSpawns) {
                return routeLength * MathUtils.clamp(config.routeTargetFraction, 0.001f, 1f);
            }
            if (config.routeTargets > 0 && config.randomRaceSpawns) {
                return routeLength / RL_ROUTE_TRAINING_CHUNKS_PER_LAP;
            }
            return routeLength;
        }

        private float getBaseCheckpointDeadlineSeconds() {
            float episodeSeconds =
                    getMaxActionSteps()
                            * Math.max(1, config.actionRepeat)
                            * PHYSICS_STEP;
            float perCheckpointSeconds = episodeSeconds / Math.max(1, getRequiredRouteTargets());
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
            if (car != null && car.active && car.body != null && game.currentMap != null) {
                observationForward.set(car.body.getWorldVector(observationForward.set(0f, 1f)));
                bestRaceRouteProgress[agentIndex] =
                        game.currentMap.findRouteProgressNear(
                                car.body.getPosition(),
                                observationForward,
                                bestRaceRouteProgress[agentIndex]);
            } else {
                bestRaceRouteProgress[agentIndex] = 0f;
            }
        }

        private float getRaceTargetRadius() {
            return Math.max(RACE_CHECKPOINT_MIN_RADIUS, RACE_CHECKPOINT_RADIUS);
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

        private void captureBeforeSnapshots() {
            for (int agentIndex = 0; agentIndex < getControlledAgentCount(); agentIndex++) {
                captureBeforeSnapshot(agentIndex, beforeSnapshots[agentIndex]);
            }
        }

        private void captureBeforeSnapshot(int agentIndex, RlAgentSnapshot snapshot) {
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
            snapshot.positionX = position.x;
            snapshot.positionY = position.y;
            snapshot.outsideRoad = !game.currentMap.supports(position);
            snapshot.offRoad =
                    snapshot.outsideRoad
                            || game.currentMap.distanceToHazard(position)
                                    < Car.TRACK_LIMIT_SLOW_MARGIN;
            snapshot.offRoadDistance =
                    snapshot.offRoad
                            ? Car.getTrackLimitPenaltyDistance(game.currentMap, position)
                            : 0f;
            snapshot.carHitCount = car.getCarHitCount();
            if (!snapshot.offRoad) {
                observationForward.set(car.body.getWorldVector(observationForward.set(0f, 1f)));
                snapshot.raceRouteProgress =
                        game.currentMap.findRouteProgressNear(
                                position,
                                observationForward,
                                bestRaceRouteProgress[agentIndex]);
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
            snapshot.positionX = position.x;
            snapshot.positionY = position.y;
            snapshot.edgeDistance = game.currentMap.distanceToHazard(position);
            snapshot.outsideRoad = !game.currentMap.supports(position);
            snapshot.offRoad =
                    snapshot.outsideRoad
                            || snapshot.edgeDistance < Car.TRACK_LIMIT_SLOW_MARGIN;
            snapshot.offRoadDistance =
                    snapshot.offRoad ? Car.getTrackLimitPenaltyDistance(game.currentMap, position) : 0f;
            Vector2 velocity = car.body.getLinearVelocity();
            snapshot.speed = velocity.len();
            snapshot.signedForwardSpeed = car.getSignedForwardSpeed();
            snapshot.wallContact = car.hasRecentArenaWallContact();
            snapshot.carHitCount = car.getCarHitCount();
            snapshot.angularSpeed = Math.abs(car.body.getAngularVelocity());
            observationForward.set(car.body.getWorldVector(observationForward.set(0f, 1f)));
            snapshot.effectiveThrottle = car.lastThrottleCommand;
            snapshot.raceRouteProgress =
                    game.currentMap.findRouteProgressNear(
                            position,
                            observationForward,
                            bestRaceRouteProgress[agentIndex]);
            game.currentMap.findRouteTangentAt(position, observationForward, observationRouteTarget);
            snapshot.routeForwardAlignment =
                    MathUtils.clamp(observationRouteTarget.dot(observationForward), -1f, 1f);
            snapshot.routeForwardSpeed = observationRouteTarget.dot(velocity);
            observationSide.set(-observationForward.y, observationForward.x);
            game.currentMap.findRecoveryPoint(position, observationRecovery);
            observationRecovery.sub(position);
            if (!observationRecovery.isZero(0.0001f)) {
                observationRecovery.nor();
                snapshot.recoverySpeed = observationRecovery.dot(velocity);
            }
        }

        private float getRouteForwardSpeed(
                Vector2 position,
                Vector2 velocity,
                Vector2 preferredDirection) {
            if (game.currentMap == null
                    || position == null
                    || velocity == null
                    || !game.currentMap.hasRoute()) {
                return 0f;
            }
            game.currentMap.findRouteTangentAt(position, preferredDirection, observationRouteTarget);
            return observationRouteTarget.dot(velocity);
        }

        private void clearStepEvents() {
            for (int agentIndex = 0; agentIndex < getControlledAgentCount(); agentIndex++) {
                routeProgressDeltas[agentIndex] = 0f;
                checkpointCrossRewardEvents[agentIndex] = false;
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

        private void updateRouteProgress() {
            if (game.currentMap == null || !game.currentMap.hasRoute()) {
                return;
            }
            float routeTargetDistance = getRouteTargetDistance();
            if (routeTargetDistance <= 0.001f) {
                return;
            }
            for (int agentIndex = 0; agentIndex < getControlledAgentCount(); agentIndex++) {
                RlAgentSnapshot before = beforeSnapshots[agentIndex];
                RlAgentSnapshot after = afterSnapshots[agentIndex];
                if (!before.active || !after.active) {
                    continue;
                }

                if (before.offRoad || after.offRoad) {
                    routeProgressDeltas[agentIndex] = 0f;
                    if (!after.offRoad) {
                        bestRaceRouteProgress[agentIndex] = after.raceRouteProgress;
                    }
                    continue;
                }

                float delta =
                        game.currentMap.routeProgressDelta(
                                before.raceRouteProgress,
                                after.raceRouteProgress);
                float movedX = after.positionX - before.positionX;
                float movedY = after.positionY - before.positionY;
                float maxPlausibleProgress =
                        (float) Math.sqrt(movedX * movedX + movedY * movedY)
                                        * RL_PROGRESS_MOVEMENT_TOLERANCE
                                + RL_PROGRESS_MOVEMENT_EPSILON;
                delta = MathUtils.clamp(delta, -maxPlausibleProgress, maxPlausibleProgress);
                routeProgressDeltas[agentIndex] = delta;
                routeDistanceSinceLastTarget[agentIndex] =
                        Math.max(0f, routeDistanceSinceLastTarget[agentIndex] + delta);
                bestRaceRouteProgress[agentIndex] = after.raceRouteProgress;
                while (routeDistanceSinceLastTarget[agentIndex] >= routeTargetDistance) {
                    routeDistanceSinceLastTarget[agentIndex] -= routeTargetDistance;
                    routeTargetsReached[agentIndex]++;
                    raceTargetSequences[agentIndex]++;
                }
            }
        }

        private void updateNoProgressState() {
            for (int agentIndex = 0; agentIndex < getControlledAgentCount(); agentIndex++) {
                RlAgentSnapshot after = afterSnapshots[agentIndex];
                if (!after.active) {
                    actionsSinceProgress[agentIndex] = 0;
                    progressSinceDeadlineReset[agentIndex] = 0f;
                    continue;
                }

                actionsSinceProgress[agentIndex]++;
                progressSinceDeadlineReset[agentIndex] =
                        Math.max(
                                0f,
                                progressSinceDeadlineReset[agentIndex]
                                        + routeProgressDeltas[agentIndex]);
                if (progressSinceDeadlineReset[agentIndex] >= RL_NO_PROGRESS_RESET_DISTANCE) {
                    actionsSinceProgress[agentIndex] = 0;
                    progressSinceDeadlineReset[agentIndex] = 0f;
                }
            }
        }

        private boolean hasNoProgressTimeout() {
            if (config.noProgressMaxActionSteps <= 0) {
                return false;
            }
            boolean hasActiveAgent = false;
            for (int agentIndex = 0; agentIndex < getControlledAgentCount(); agentIndex++) {
                if (!afterSnapshots[agentIndex].active) {
                    continue;
                }
                hasActiveAgent = true;
                if (actionsSinceProgress[agentIndex] < config.noProgressMaxActionSteps) {
                    return false;
                }
            }
            return hasActiveAgent;
        }

        private void updateOffRoadFailureState() {
            for (int agentIndex = 0; agentIndex < getControlledAgentCount(); agentIndex++) {
                RlAgentSnapshot after = afterSnapshots[agentIndex];
                consecutiveOffRoadActions[agentIndex] =
                        after.active && after.outsideRoad
                                ? consecutiveOffRoadActions[agentIndex] + 1
                                : 0;
            }
        }

        private boolean hasOffRoadFailureTimeout() {
            if (config.offRoadFailureMaxActionSteps <= 0) {
                return false;
            }
            boolean hasActiveAgent = false;
            for (int agentIndex = 0; agentIndex < getControlledAgentCount(); agentIndex++) {
                if (!afterSnapshots[agentIndex].active) {
                    continue;
                }
                hasActiveAgent = true;
                if (consecutiveOffRoadActions[agentIndex]
                        < config.offRoadFailureMaxActionSteps) {
                    return false;
                }
            }
            return hasActiveAgent;
        }

        private void computeRewards() {
            for (int agentIndex = 0; agentIndex < getControlledAgentCount(); agentIndex++) {
                clearRewardBreakdown(agentIndex);
                RlAgentSnapshot before = beforeSnapshots[agentIndex];
                RlAgentSnapshot after = afterSnapshots[agentIndex];
                float reward = 0f;

                if (before.active && after.active) {
                    reward += recordReward(agentIndex, RL_REWARD_STEP_COST, -config.stepPenalty);
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
                            RL_REWARD_OFF_ROAD_RECOVERY,
                            getOffRoadRecoveryReward(before, after));
                    reward += recordReward(
                            agentIndex,
                            RL_REWARD_ROUTE_PROGRESS,
                            getProgressReward(agentIndex, after));
                    reward += recordReward(
                            agentIndex,
                            RL_REWARD_ROUTE_ALIGNMENT,
                            getRouteAlignmentReward(after));
                    reward += recordReward(
                            agentIndex,
                            RL_REWARD_REVERSE_SPEED,
                            -getReverseSpeedPenalty(after));
                    reward += recordReward(
                            agentIndex,
                            RL_REWARD_CAR_PUSH,
                            -getCarPushPenalty(before, after));
                    if (episodeNoProgressFailure) {
                        reward += recordReward(
                                agentIndex,
                                RL_REWARD_NO_PROGRESS,
                                -config.noProgressPenalty);
                    }
                    if (episodeOffRoadFailure) {
                        reward += recordReward(
                                agentIndex,
                                RL_REWARD_OFF_ROAD_FAILURE,
                                -config.offRoadFailurePenalty);
                    }
                }

                rewards[agentIndex] = reward;
                dones[agentIndex] = episodeDone;
            }
        }

        private void clearRewardBreakdown(int agentIndex) {
            if (!config.rewardBreakdownEnabled) {
                return;
            }
            int offset = agentIndex * RL_REWARD_BREAKDOWN_SIZE;
            for (int i = 0; i < RL_REWARD_BREAKDOWN_SIZE; i++) {
                rewardBreakdown[offset + i] = 0f;
            }
        }

        private float recordReward(int agentIndex, int bucket, float reward) {
            if (config.rewardBreakdownEnabled) {
                rewardBreakdown[agentIndex * RL_REWARD_BREAKDOWN_SIZE + bucket] += reward;
            }
            return reward;
        }

        private float getOffRoadPenalty(RlAgentSnapshot snapshot) {
            return Math.min(
                    config.offRoadMaxPenalty,
                    config.offRoadPenalty
                            + snapshot.offRoadDistance * config.offRoadDistancePenalty);
        }

        private float getOffRoadRecoveryReward(
                RlAgentSnapshot before,
                RlAgentSnapshot after) {
            if (!before.offRoad && !after.offRoad) {
                return 0f;
            }
            return (before.offRoadDistance - after.offRoadDistance)
                    * config.offRoadRecoveryReward;
        }

        private float getProgressReward(int agentIndex, RlAgentSnapshot after) {
            if (after.offRoad) {
                return 0f;
            }
            if (Math.abs(after.routeForwardSpeed) < RACE_CHECKPOINT_MIN_FORWARD_CROSS_SPEED) {
                return 0f;
            }
            if (after.routeForwardSpeed > 0f
                    && after.signedForwardSpeed < RACE_CHECKPOINT_MIN_FORWARD_CROSS_SPEED) {
                return 0f;
            }
            float progress = routeProgressDeltas[agentIndex];
            if (progress <= 0f) {
                return progress * config.progressReward;
            }
            float fastFraction = Math.max(0f, progress / RL_PROGRESS_FAST_DELTA);
            float multiplier = 1f + RL_PROGRESS_FAST_BONUS * fastFraction;
            return progress * multiplier * config.progressReward;
        }

        private float getRouteAlignmentReward(RlAgentSnapshot snapshot) {
            if (snapshot.offRoad || config.routeAlignmentReward <= 0f) {
                return 0f;
            }
            if (snapshot.signedForwardSpeed < RACE_CHECKPOINT_MIN_FORWARD_CROSS_SPEED) {
                return 0f;
            }
            return Math.max(0f, snapshot.routeForwardAlignment) * config.routeAlignmentReward;
        }

        private float getReverseSpeedPenalty(RlAgentSnapshot snapshot) {
            float reverseSpeed =
                    Math.max(0f, -snapshot.signedForwardSpeed - config.reverseSpeedFreeEpsilon);
            return Math.min(
                    config.reverseSpeedMaxPenalty,
                    reverseSpeed * config.reverseSpeedPenaltyPerUnit);
        }

        private float getSteeringPenalty(int agentIndex) {
            float turn = MathUtils.clamp(currentActionTurn[agentIndex], -1f, 1f);
            return turn * turn * config.steeringPenalty;
        }

        private float getCarPushPenalty(RlAgentSnapshot before, RlAgentSnapshot after) {
            int hits = Math.max(0, after.carHitCount - before.carHitCount);
            return Math.min(config.carPushMaxStepPenalty, hits * config.carPushPenalty);
        }

        private void buildObservations() {
            for (int i = 0; i < observations.length; i++) {
                observations[i] = 0f;
            }
            if (game.currentMap == null) {
                return;
            }

            for (int agentIndex = 0; agentIndex < getControlledAgentCount(); agentIndex++) {
                fillRlObservation(
                        observations,
                        agentIndex * RL_OBSERVATION_SIZE,
                        getControlledCar(agentIndex),
                        game.currentMap,
                        bestRaceRouteProgress[agentIndex],
                        observationForward,
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
            int bestAgentIndex = -1;
            int bestRouteTargetsReached = 0;
            for (int i = 0; i < getControlledAgentCount(); i++) {
                if (routeTargetsReached[i] > bestRouteTargetsReached) {
                    bestRouteTargetsReached = routeTargetsReached[i];
                    bestAgentIndex = i;
                }
            }
            return bestRouteTargetsReached >= getRequiredRouteTargets() ? bestAgentIndex : -1;
        }

        private boolean hasCompletedTrainingRace() {
            for (int i = 0; i < getControlledAgentCount(); i++) {
                if (routeTargetsReached[i] >= getRequiredRouteTargets()) {
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
            if (config.rewardBreakdownEnabled) {
                for (int i = 0; i < rewardBreakdown.length; i++) {
                    rewardBreakdown[i] = 0f;
                }
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
                routeProgressDeltas[i] = 0f;
                progressSinceDeadlineReset[i] = 0f;
                actionsSinceProgress[i] = 0;
                consecutiveOffRoadActions[i] = 0;
                routeDistanceSinceLastTarget[i] = 0f;
                raceTargetSequences[i] = 0;
                resetRaceRouteProgressBaseline(i);
                routeTargetsReached[i] = 0;
                checkpointCrossRewardSequences[i] = -1;
                checkpointCrossRewardEvents[i] = false;
                checkpointTimeoutEvents[i] = false;
                raceCheckpointCrossEvents[i] = false;
            }
        }

        private RlStepResult createResult() {
            float[] observationCopy = new float[observations.length];
            float[] rewardCopy = new float[rewards.length];
            float[] rewardBreakdownCopy =
                    config.rewardBreakdownEnabled
                            ? new float[rewardBreakdown.length]
                            : new float[0];
            float[] effectiveActionCopy =
                    config.stepDetailsEnabled ? new float[effectiveActions.length] : new float[0];
            boolean[] doneCopy = config.stepDetailsEnabled ? new boolean[dones.length] : new boolean[0];
            int[] routeTargetsReachedCopy =
                    config.stepDetailsEnabled ? new int[routeTargetsReached.length] : new int[0];
            float[] routeProgressDeltasCopy =
                    config.stepDetailsEnabled
                            ? new float[routeProgressDeltas.length]
                            : new float[0];
            float[] debugTraceCopy =
                    config.debugTraceEnabled
                            ? new float[getControlledAgentCount() * RL_DEBUG_TRACE_SIZE]
                            : new float[0];
            if (config.stepDetailsEnabled || config.debugTraceEnabled) {
                buildEffectiveActions();
            }
            System.arraycopy(observations, 0, observationCopy, 0, observations.length);
            System.arraycopy(rewards, 0, rewardCopy, 0, rewards.length);
            if (config.rewardBreakdownEnabled) {
                System.arraycopy(
                        rewardBreakdown,
                        0,
                        rewardBreakdownCopy,
                        0,
                        rewardBreakdown.length);
            }
            if (config.stepDetailsEnabled) {
                System.arraycopy(effectiveActions, 0, effectiveActionCopy, 0, effectiveActions.length);
                System.arraycopy(dones, 0, doneCopy, 0, dones.length);
                System.arraycopy(
                        routeTargetsReached,
                        0,
                        routeTargetsReachedCopy,
                        0,
                        routeTargetsReached.length);
                System.arraycopy(
                        routeProgressDeltas,
                        0,
                        routeProgressDeltasCopy,
                        0,
                        routeProgressDeltas.length);
            }
            if (config.debugTraceEnabled) {
                buildDebugTrace(debugTraceCopy);
            }
            return new RlStepResult(
                    observationCopy,
                    rewardCopy,
                    rewardBreakdownCopy,
                    config.rewardBreakdownEnabled ? RL_REWARD_BREAKDOWN_NAMES : new String[0],
                    effectiveActionCopy,
                    doneCopy,
                    episodeDone,
                    episodeTerminated,
                    episodeTruncated,
                    actionStep,
                    config.stepDetailsEnabled ? getWinnerAgentIndex() : -1,
                    config.stepDetailsEnabled ? getWinnerLabel() : "",
                    config.stepDetailsEnabled ? getCurrentMapId() : "",
                    config.stepDetailsEnabled ? getCurrentMapName() : "",
                    routeTargetsReachedCopy,
                    routeProgressDeltasCopy,
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
            Vector2 routeTangent = new Vector2();
            for (int agentIndex = 0; agentIndex < getControlledAgentCount(); agentIndex++) {
                int offset = agentIndex * RL_DEBUG_TRACE_SIZE;
                Car car = getControlledCar(agentIndex);
                if (car == null || !car.active || car.body == null) {
                    continue;
                }

                Vector2 position = car.body.getPosition();
                Vector2 velocity = car.body.getLinearVelocity();
                forward.set(car.body.getWorldVector(forward.set(0f, 1f)));
                side.set(-forward.y, forward.x);
                float routeProgress =
                        game.currentMap.findRouteProgressNear(
                                position,
                                forward,
                                bestRaceRouteProgress[agentIndex]);
                game.currentMap.findRouteTangent(routeProgress, routeTangent);
                out[offset] = 1f;
                out[offset + 1] = position.x;
                out[offset + 2] = position.y;
                out[offset + 3] = car.body.getAngle();
                out[offset + 4] = velocity.len();
                out[offset + 5] = forward.dot(velocity);
                out[offset + 6] = side.dot(velocity);
                out[offset + 7] = routeProgress;
                out[offset + 8] =
                        game.currentMap.hasRoute()
                                ? routeProgress / Math.max(0.001f, game.currentMap.getRouteLength())
                                : 0f;
                out[offset + 9] = routeTangent.x;
                out[offset + 10] = routeTangent.y;
                out[offset + 11] = routeTangent.dot(velocity);
                out[offset + 12] = routeProgressDeltas[agentIndex];
                out[offset + 13] = routeDistanceSinceLastTarget[agentIndex];
                out[offset + 14] = getRouteTargetDistance();
                out[offset + 15] = routeTargetsReached[agentIndex];
                out[offset + 16] = getRequiredRouteTargets();
                out[offset + 17] =
                        Car.isTrackLimitSlowdownActive(game.currentMap, position) ? 1f : 0f;
                out[offset + 18] = Car.getTrackLimitPenaltyDistance(game.currentMap, position);
                out[offset + 19] = game.currentMap.approximateDistanceToHazard(position);
                int actionOffset = agentIndex * RL_ACTION_SIZE;
                out[offset + 20] =
                        actionOffset < effectiveActions.length ? effectiveActions[actionOffset] : 0f;
                out[offset + 21] =
                        actionOffset + 1 < effectiveActions.length
                                ? effectiveActions[actionOffset + 1]
                                : 0f;
                out[offset + 22] = episodeDone ? 1f : 0f;
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
                effectiveActions[actionOffset] = car.lastThrottleCommand;
                effectiveActions[actionOffset + 1] = car.lastTurnCommand;
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
        private boolean outsideRoad;
        private float offRoadDistance;
        private float positionX;
        private float positionY;
        private float edgeDistance;
        private float checkpointRouteDistance;
        private float checkpointTargetRadius;
        private float raceRouteProgress;
        private float routeForwardAlignment;
        private float routeForwardSpeed;
        private float speed;
        private float signedForwardSpeed;
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
            outsideRoad = false;
            offRoadDistance = 0f;
            positionX = 0f;
            positionY = 0f;
            edgeDistance = 0f;
            checkpointRouteDistance = 0f;
            checkpointTargetRadius = 0f;
            raceRouteProgress = 0f;
            routeForwardAlignment = 0f;
            routeForwardSpeed = 0f;
            speed = 0f;
            signedForwardSpeed = 0f;
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
        private final RlPolicy rlPolicy;
        private CarPhysics physics;
        private Texture spriteTexture;
        private boolean ownsSpriteTexture;
        private int spriteSourceX;
        private int spriteSourceY;
        private int spriteSourceWidth;
        private int spriteSourceHeight;
        private int totalPoints;
        private int roundGridPosition;
        private int roundFinishPosition;
        private int nextGridPosition;
        private int lastRoundAwardedPoints;
        private int roundPickupPoints;
        private int roundRaceLap;
        private int roundRaceStartCheckpointIndex;
        private int roundRaceNextCheckpointIndex;
        private int roundRaceCheckpointsCompleted;
        private float roundRaceLastRouteProgress;
        private float roundRaceStartRouteProgress;
        private int roundRaceStartRouteIndex;
        private int roundRaceRouteIndex;
        private float roundRaceDistanceThisLap;
        private float roundRaceTotalRouteProgress;
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
                RlPolicy rlPolicy,
                CarPhysics physics) {
            this.vehicleId = vehicleId;
            this.name = name;
            this.playerControlled = playerControlled;
            this.color = color;
            this.visual = visual;
            this.statsLabel = statsLabel == null || statsLabel.length() == 0 ? name : statsLabel;
            this.externallyControlled = externallyControlled;
            this.modelControlled = modelControlled;
            this.rlPolicy = rlPolicy;
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
        MAPS_MENU,
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

    private static final class DriverPolicyChoice {
        private final String id;
        private final String displayName;

        private DriverPolicyChoice(String id, String displayName) {
            this.id = id;
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
