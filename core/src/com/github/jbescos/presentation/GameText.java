package com.github.jbescos.presentation;

import com.github.jbescos.gameplay.roguelite.CustomGameRules.WeatherType;
import com.github.jbescos.gameplay.roguelite.RogueliteExperienceAwards.Reason;
import com.github.jbescos.gameplay.roguelite.RogueliteSlotType;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class GameText {
    private static final Map<String, String> SPANISH = createSpanishText();
    private static final Map<String, String> FRENCH =
            GameTextEuropeanCatalog.create(GameLanguage.FRENCH);
    private static final Map<String, String> GERMAN =
            GameTextEuropeanCatalog.create(GameLanguage.GERMAN);
    private static final Map<String, String> ITALIAN =
            GameTextEuropeanCatalog.create(GameLanguage.ITALIAN);

    private GameText() {
    }

    public static String translate(GameLanguage language, String english) {
        if (english == null || language == null || language == GameLanguage.ENGLISH) {
            return english;
        }
        Map<String, String> translations = translations(language);
        String exact = translations.get(english);
        if (exact != null) {
            return exact;
        }
        if (language != GameLanguage.SPANISH) {
            return GameTextEuropeanCatalog.translateDynamic(language, english);
        }
        if (english.startsWith("Theme: ")) {
            return "Tema: " + english.substring("Theme: ".length());
        }
        if (english.startsWith("Tier ")) {
            return "Nivel " + english.substring("Tier ".length());
        }
        if (english.startsWith("Level ")) {
            return "Nivel " + english.substring("Level ".length());
        }
        if (english.startsWith("Circuit ")) {
            return "Circuito " + english.substring("Circuit ".length());
        }
        if (english.startsWith("Map ")) {
            return "Mapa " + english.substring("Map ".length());
        }
        if (english.startsWith("CHAMPIONSHIP ")) {
            return "CAMPEONATO " + english.substring("CHAMPIONSHIP ".length());
        }
        if (english.startsWith("Lap ")) {
            return "Vuelta " + english.substring("Lap ".length());
        }
        if (english.startsWith("Cars ")) {
            return "Coches " + english.substring("Cars ".length());
        }
        if (english.startsWith("Car ")) {
            return "Coche " + english.substring("Car ".length());
        }
        if (english.startsWith("Loading ")) {
            return "Cargando " + english.substring("Loading ".length());
        }
        if (english.startsWith("Preparing ")) {
            return "Preparando " + english.substring("Preparing ".length());
        }
        if (english.startsWith("Finished #")) {
            return "Terminó #" + english.substring("Finished #".length());
        }
        if (english.startsWith("TV CAMERA: ")) {
            return "CÁMARA TV: " + english.substring("TV CAMERA: ".length());
        }
        if (english.startsWith("CONTINUE ")) {
            return "CONTINUAR " + english.substring("CONTINUE ".length());
        }
        if (english.startsWith("SKIP ")) {
            return "DESCARTAR " + english.substring("SKIP ".length());
        }
        if (english.startsWith("Starts in ")) {
            return "Empieza en " + english.substring("Starts in ".length());
        }
        if (english.startsWith("Finish closes in ")) {
            return "La meta cierra en " + english.substring("Finish closes in ".length());
        }
        if (english.indexOf(" | Tier ") >= 0
                || english.indexOf(" SLOTS") >= 0
                || english.indexOf(" PAGE ") >= 0) {
            return english
                    .replace(" | Tier ", " | Nivel ")
                    .replace(" SLOTS", " HUECOS")
                    .replace(" PAGE ", " PÁGINA ");
        }
        int detailSeparator = english.indexOf(" - ");
        if (detailSeparator > 0) {
            return translate(language, english.substring(0, detailSeparator))
                    + " - "
                    + translate(language, english.substring(detailSeparator + 3));
        }
        int targetSeparator = english.lastIndexOf(" on ");
        if (targetSeparator > 0) {
            return translate(language, english.substring(0, targetSeparator))
                    + " contra "
                    + english.substring(targetSeparator + 4);
        }
        if (english.indexOf(" SLAMMED ") >= 0
                || english.indexOf(" SHOVED ") >= 0
                || english.indexOf(" CLIPPED ") >= 0
                || english.indexOf(" USED ") >= 0
                || english.indexOf(" PASSED ") >= 0) {
            return english
                    .replace("HIT: ", "GOLPE: ")
                    .replace("REVENGE: ", "VENGANZA: ")
                    .replace("PASS: ", "ADELANTAMIENTO: ")
                    .replace(" SLAMMED ", " EMBISTIÓ A ")
                    .replace(" SHOVED ", " EMPUJÓ A ")
                    .replace(" CLIPPED ", " ROZÓ A ")
                    .replace(" HIT ", " GOLPEÓ A ")
                    .replace(" USED ", " USÓ ")
                    .replace(" ON ", " CONTRA ")
                    .replace(" PASSED ", " ADELANTÓ A ")
                    .replace(" POSITIONS GAINED", " POSICIONES GANADAS")
                    .replace(" POSITION GAINED", " POSICIÓN GANADA");
        }
        if (looksLikeCardEffect(english)) {
            return translateCardEffect(english);
        }
        return english;
    }

    public static String slotType(GameLanguage language, RogueliteSlotType slotType) {
        if (slotType == null) {
            return "";
        }
        if (language == null || language == GameLanguage.ENGLISH) {
            return slotType.getDisplayName();
        }
        if (language != GameLanguage.SPANISH) {
            return translate(language, slotType.getDisplayName());
        }
        switch (slotType) {
            case DRIVER:
                return "Piloto";
            case TUNING:
                return "Mejoras";
            case TECHNIQUE:
                return "Técnica";
            case POWERUP:
                return "Potenciar";
            case REVENGE:
                return "Venganza";
            default:
                return slotType.getDisplayName();
        }
    }

    public static String weather(GameLanguage language, WeatherType weather) {
        if (weather == null || language == null || language == GameLanguage.ENGLISH) {
            return weather == null ? "" : weather.getDisplayName();
        }
        if (language != GameLanguage.SPANISH) {
            return translate(language, weather.getDisplayName());
        }
        switch (weather) {
            case SUNNY:
                return "Soleado";
            case RAIN:
                return "Lluvia";
            case SNOW:
                return "Nieve";
            default:
                return weather.getDisplayName();
        }
    }

    public static String experienceReason(GameLanguage language, Reason reason) {
        if (reason == null || language == null || language == GameLanguage.ENGLISH) {
            return reason == null ? "" : reason.getDisplayName();
        }
        if (language != GameLanguage.SPANISH) {
            return translate(language, reason.getDisplayName());
        }
        switch (reason) {
            case OVERTAKE:
                return "Adelantamiento";
            case FASTEST_LAP:
                return "Vuelta rápida";
            case REVENGE:
                return "Venganza";
            case PUSH_OFF_ROAD:
                return "Rival fuera";
            case DRIFT:
                return "Derrape";
            case FINISH:
                return "Final";
            default:
                return reason.getDisplayName();
        }
    }

    private static boolean looksLikeCardEffect(String text) {
        return text.indexOf('|') >= 0
                || text.indexOf("->") >= 0
                || text.indexOf(':') >= 0
                || text.startsWith("Cooldown:")
                || text.startsWith("Each activation:");
    }

    private static Map<String, String> translations(GameLanguage language) {
        switch (language) {
            case SPANISH:
                return SPANISH;
            case FRENCH:
                return FRENCH;
            case GERMAN:
                return GERMAN;
            case ITALIAN:
                return ITALIAN;
            default:
                return Collections.emptyMap();
        }
    }

    private static String translateCardEffect(String text) {
        String result = text;
        result = result.replace("Clear straight", "Recta libre");
        result = result.replace("Fast corner exit", "Salida rápida de curva");
        result = result.replace("Corner exit", "Salida de curva");
        result = result.replace("Corner ahead", "Curva cercana");
        result = result.replace("Corner or traffic", "Curva o tráfico");
        result = result.replace("Traffic or corner", "Tráfico o curva");
        result = result.replace("Nearby rival on straight", "Rival cercano en recta");
        result = result.replace("Nearby rival", "Rival cercano");
        result = result.replace("Rival hit", "Golpe rival");
        result = result.replace("Hit taken", "Golpe recibido");
        result = result.replace("Each activation", "Cada activación");
        result = result.replace("Stay on-road", "Seguir en pista");
        result = result.replace("Safe re-entry", "Regreso seguro");
        result = result.replace("Drift exit", "Salida de derrape");
        result = result.replace("Leave draft", "Salir del rebufo");
        result = result.replace("Race events", "Eventos de carrera");
        result = result.replace("Lower position", "Posición retrasada");
        result = result.replace("Clean lap", "Vuelta limpia");
        result = result.replace("Overtake", "Adelantamiento");
        result = result.replace("Powerup", "Potenciar");
        result = result.replace("Revenge", "Venganza");
        result = result.replace("Draft", "Rebufo");
        result = result.replace("draft", "rebufo");
        result = result.replace("Power", "Potencia");
        result = result.replace("power", "potencia");
        result = result.replace("Speed", "Velocidad");
        result = result.replace("speed", "velocidad");
        result = result.replace("Grip", "Agarre");
        result = result.replace("grip", "agarre");
        result = result.replace("Steering", "Dirección");
        result = result.replace("steering", "dirección");
        result = result.replace("Mass", "Masa");
        result = result.replace("mass", "masa");
        result = result.replace("Stronger hits", "Golpes más fuertes");
        result = result.replace("Freer rear while drifting", "Trasera libre al derrapar");
        result = result.replace("reach and boost", "alcance e impulso");
        result = result.replace("up to", "hasta");
        result = result.replace("bursts", "en ráfagas");
        result = result.replace("Cooldown after effect", "Recarga tras efecto");
        result = result.replace("Cooldown", "Recarga");
        result = result.replace("random Tier", "azar Nivel");
        result = result.replace("full throttle", "acelerador máximo");
        result = result.replace("full brake", "frenado máximo");
        result = result.replace("after 3s", "tras 3s");
        result = result.replace("swap with", "intercambio con");
        result = result.replace("pull to", "atraer al");
        result = result.replace("over 5s", "durante 5s");
        result = result.replace("reflect impact", "reflejar impacto");
        result = result.replace("wide outward field", "campo repulsor amplio");
        result = result.replace("outward field", "campo repulsor");
        result = result.replace("until collision", "hasta colisión");
        result = result.replace("blind", "ciego");
        result = result.replace("random", "aleatoria");
        result = result.replace("next hit", "siguiente golpe");
        result = result.replace("Explosive ram", "Embestida explosiva");
        result = result.replace("Recoil", "Retroceso");
        result = result.replace("Push", "Empuje");
        result = result.replace("offender", "agresor");
        result = result.replace("if ahead", "si va delante");
        result = result.replace("cars for", "coches durante");
        result = result.replace("car for", "coche durante");
        result = result.replace("Rivals pass through you", "Los rivales te atraviesan");
        result = result.replace("invisible", "invisible");
        result = result.replace("launch", "impulso");
        result = result.replace("shield", "escudo");
        return result;
    }

    private static Map<String, String> createSpanishText() {
        Map<String, String> text = new HashMap<String, String>();
        put(text, "New Game", "Nueva partida");
        put(text, "Continue", "Continuar");
        put(text, "Sandbox", "Zona de pruebas");
        put(text, "Options", "Opciones");
        put(text, "Exit", "Salir");
        put(text, "Back", "Volver");
        put(text, "Back to Sandbox", "Volver a pruebas");
        put(text, "Cancel", "Cancelar");
        put(text, "Main Menu", "Menú principal");
        put(text, "Paused", "Pausa");
        put(text, "Custom", "Personalizada");
        put(text, "Choose Run", "Elige partida");
        put(text, "Custom Run", "Partida personalizada");
        put(text, "Choose Car", "Elegir coche");
        put(text, "Choose Your Car", "Elige tu coche");
        put(text, "Start Race", "Empezar carrera");
        put(text, "Name", "Nombre");
        put(text, "Normal", "Normal");
        put(text, "Maps", "Mapas");
        put(text, "MAPS", "MAPAS");
        put(text, "Map", "Mapa");
        put(text, "No maps", "Sin mapas");
        put(text, "Camera", "Cámara");
        put(text, "Top Down", "Cenital");
        put(text, "Chase", "Persecución");
        put(text, "Whole Map", "Mapa completo");
        put(text, "Free", "Libre");
        put(text, "Zoom", "Zoom");
        put(text, "Fit", "Ajustar");
        put(text, "Display", "Pantalla");
        put(text, "Windowed", "Ventana");
        put(text, "Fullscreen", "Pantalla completa");
        put(text, "Music", "Música");
        put(text, "Sound FX", "Efectos");
        put(text, "Language", "Idioma");
        put(text, "TV CAMERA", "CÁMARA TV");
        put(text, "Loading settings", "Cargando ajustes");
        put(text, "Loading drivers", "Cargando pilotos");
        put(text, "Loading circuits", "Cargando circuitos");
        put(text, "Loading Rogue Circuit", "Cargando Rogue Circuit");
        put(text, "Loading Circuit", "Cargando circuito");
        put(text, "Loading Sandbox", "Cargando pruebas");
        put(text, "Loading Race", "Cargando carrera");
        put(text, "Building starting grid", "Preparando parrilla");
        put(text, "Starting engines", "Arrancando motores");
        put(text, "Preparing menu", "Preparando menú");
        put(text, "Ready", "Listo");
        put(text, "CARD TYPES", "TIPOS DE CARTA");
        put(text, "WEATHER", "CLIMA");
        put(text, "CARD TIERS", "NIVELES DE CARTA");
        put(text, "Laps", "Vueltas");
        put(text, "XP / level", "XP / nivel");
        put(text, "Lap XP cap", "Límite XP vuelta");
        put(text, "Player", "Piloto");
        put(text, "Best", "Mejor");
        put(text, "Current", "Actual");
        put(text, "Pts", "Ptos");
        put(text, "Lap", "Vuelta");
        put(text, "Time", "Tiempo");
        put(text, "LAPS", "VUELTAS");
        put(text, "Circuit Map", "Mapa del circuito");
        put(text, "CAR STATS", "DATOS DEL COCHE");
        put(text, "TELEMETRY", "TELEMETRÍA");
        put(text, "CARDS", "CARTAS");
        put(text, "LOADOUT", "EQUIPAMIENTO");
        put(text, "ACTIVE CARD EFFECTS", "EFECTOS ACTIVOS");
        put(text, "RACE STATUS", "ESTADO DE CARRERA");
        put(text, "RACE LOG", "REGISTRO");
        put(text, "IMPACT", "IMPACTO");
        put(text, "OVERTAKE", "ADELANTAMIENTO");
        put(text, "WARNING", "AVISO");
        put(text, "SPEED", "VELOCIDAD");
        put(text, "OFF ROAD", "FUERA DE PISTA");
        put(text, "REVERSE", "MARCHA ATRÁS");
        put(text, "THROTTLE", "ACELERADOR");
        put(text, "BRAKE", "FRENO");
        put(text, "STEERING", "DIRECCIÓN");
        put(text, "DRIFT", "DERRAPE");
        put(text, "POWER", "POTENCIA");
        put(text, "TOP SPEED", "VEL. MÁXIMA");
        put(text, "GRIP", "AGARRE");
        put(text, "MASS", "MASA");
        put(text, "AVG LAP", "VUELTA MEDIA");
        put(text, "MAX SPEED", "VEL. MÁXIMA");
        put(text, "OFF ROAD", "FUERA DE PISTA");
        put(text, "LEVEL UP - CHOOSE AN UPGRADE", "SUBES DE NIVEL - ELIGE UNA MEJORA");
        put(text, "YOUR 5 SLOTS", "TUS 5 HUECOS");
        put(text, "SANDBOX - CHOOSE A CARD", "PRUEBAS - ELIGE UNA CARTA");
        put(text, "ACCEPT", "ACEPTAR");
        put(text, "CANCEL", "CANCELAR");
        put(text, "SKIP", "DESCARTAR");
        put(text, "EMPTY", "VACÍO");
        put(text, "EMPTY SLOT", "HUECO VACÍO");
        put(text, "No card equipped", "Ninguna carta equipada");
        put(text, "No card effect active", "Ningún efecto activo");
        put(text, "CARD PICK PENDING", "CARTA PENDIENTE");
        put(text, "READY", "LISTO");
        put(text, "ARMED", "ARMADO");
        put(text, "ACTIVE", "ACTIVA");
        put(text, "WAIT", "ESPERA");
        put(text, "NOW", "AHORA");
        put(text, "MAX", "MÁX");
        put(text, "GET READY", "PREPÁRATE");
        put(text, "FINISHING RACE", "FINALIZANDO CARRERA");
        put(text, "RACE COMPLETE", "CARRERA TERMINADA");
        put(text, "RACE WINNER", "GANADOR");
        put(text, "CHAMPIONSHIP STANDINGS", "CLASIFICACIÓN");
        put(text, "FINAL CHAMPIONSHIP COMPLETE", "CAMPEONATO COMPLETADO");
        put(text, "CONTINUE", "CONTINUAR");
        put(text, "CHAMPION", "CAMPEÓN");
        put(text, "CHAMPIONSHIP LOST", "CAMPEONATO PERDIDO");
        put(text, "NEW RUN", "NUEVA PARTIDA");
        put(text, "RESET", "REINICIAR");
        put(text, "RACE OVER", "CARRERA TERMINADA");
        put(text, "NO FINISHER", "SIN FINALISTAS");
        put(text, "FIN", "META");
        put(text, "OUT", "FUERA");
        put(text, "Free practice", "Práctica libre");
        put(text, "Standings updated. Next circuit in a moment.", "Clasificación actualizada. Próximo circuito en breve.");
        put(text, "Route missing.", "Ruta no disponible.");
        put(text, "Race checkpoints unavailable", "Puntos de control no disponibles");
        put(text, "PREVIEW", "VISTA PREVIA");
        put(text, "ON", "ACTIVA");
        put(text, "MAIN MENU", "MENÚ PRINCIPAL");
        put(text, "BRK", "FRE");
        put(text, "THR", "ACE");
        put(text, "STEER", "DIR");
        put(text, "REV", "ATR");
        put(text, "magenta route  red off-road segment  cyan direction  green spawns/goals  orange route-marker numbers",
                "ruta magenta  tramo fuera rojo  dirección cian  salidas/metas verdes  marcadores naranjas");
        put(text, "APPLY & RESTART", "APLICAR Y REINICIAR");
        put(text, "sandbox tuning", "ajustes de pruebas");
        put(text, "control", "control");
        put(text, "weather", "clima");
        put(text, "map", "mapa");
        put(text, "cars", "coches");
        put(text, "horsepower", "potencia");
        put(text, "brake", "freno");
        put(text, "steering", "dirección");
        put(text, "grip", "agarre");
        put(text, "mass", "masa");
        put(text, "Driver", "Piloto");
        put(text, "Tuning", "Mejoras");
        put(text, "Technique", "Técnica");
        put(text, "Powerup", "Potenciar");
        put(text, "Revenge", "Venganza");
        put(text, "DRIVER", "PILOTO");
        put(text, "TUNING", "MEJORAS");
        put(text, "TECHNIQUE", "TÉCNICA");
        put(text, "POWERUP", "POTENCIAR");
        put(text, "REVENGE", "VENGANZA");
        put(text, "Sunny", "Soleado");
        put(text, "Rain", "Lluvia");
        put(text, "Snow", "Nieve");
        put(text, "sunny", "soleado");
        put(text, "raining", "lluvia");
        put(text, "snowing", "nieve");
        put(text, "Automatic", "Automático");
        put(text, "Manual", "Manual");
        putSpanishCards(text);
        return Collections.unmodifiableMap(text);
    }

    private static void putSpanishCards(Map<String, String> text) {
        put(text, "Club Tune", "Ajuste club");
        put(text, "Lightweight Tune", "Ajuste ligero");
        put(text, "Streamline Kit", "Kit aerodinámico");
        put(text, "Short-Ratio Gearbox", "Caja de cambios corta");
        put(text, "Carbon Panels", "Paneles de carbono");
        put(text, "Race Tune", "Ajuste de carrera");
        put(text, "Ballast Powertrain", "Motor lastrado");
        put(text, "Le Mans Body", "Carrocería Le Mans");
        put(text, "Drift Differential", "Diferencial de derrape");
        put(text, "Carbon Monocoque", "Monocasco de carbono");
        put(text, "Aero Prototype", "Prototipo aerodinámico");
        put(text, "Ground Effect", "Efecto suelo");
        put(text, "Velocity Shell", "Carrocería veloz");
        put(text, "Torque Vectoring", "Vectorización de par");
        put(text, "Graphene Chassis", "Chasis de grafeno");
        put(text, "Corner Exit", "Salida de curva");
        put(text, "Draft Hunter", "Cazador de rebufo");
        put(text, "Clean Momentum", "Inercia limpia");
        put(text, "Recovery Launch", "Impulso de recuperación");
        put(text, "Drift Slingshot", "Tirachinas de derrape");
        put(text, "Slipstream Slingshot", "Tirachinas de rebufo");
        put(text, "Overtake Surge", "Impulso de adelantamiento");
        put(text, "Apex Slingshot", "Tirachinas de vértice");
        put(text, "Perfect Lap", "Vuelta perfecta");
        put(text, "Racecraft Mastery", "Maestría de carrera");
        put(text, "Underdog Instinct", "Instinto del rezagado");
        put(text, "Comeback Drive", "Remontada");
        put(text, "Last Place Fury", "Furia del último");
        put(text, "Close Quarters", "Cuerpo a cuerpo");
        put(text, "Pack Racer", "Piloto de grupo");
        put(text, "Traffic Dominance", "Dominio del tráfico");
        put(text, "Nitro Pulse", "Pulso nitro");
        put(text, "Mirror Duo", "Dúo espejo");
        put(text, "Grip Fan", "Ventilador de agarre");
        put(text, "Ghost Cloak", "Manto fantasma");
        put(text, "Lucky Spark", "Chispa de suerte");
        put(text, "Impact Reversal", "Inversión de impacto");
        put(text, "Draft Magnet", "Imán de rebufo");
        put(text, "Position Hijack", "Robo de posición");
        put(text, "Redline Hex", "Maldición roja");
        put(text, "Phase Shield", "Escudo de fase");
        put(text, "Rocket Exhaust", "Escape cohete");
        put(text, "Mirror Trio", "Trío espejo");
        put(text, "Phantom Cloak", "Manto espectral");
        put(text, "Chaos Relay", "Relé del caos");
        put(text, "Gravity Well", "Pozo de gravedad");
        put(text, "Mirror Quartet", "Cuarteto espejo");
        put(text, "Hyperdrive", "Hiperimpulso");
        put(text, "Void Cloak", "Manto del vacío");
        put(text, "Wildcard Core", "Núcleo comodín");
        put(text, "Crown Breaker", "Rompecoronas");
        put(text, "Vendetta Hook", "Gancho de vendetta");
        put(text, "Repulsor Surge", "Oleada repulsora");
        put(text, "Tar Tether", "Atadura de alquitrán");
        put(text, "EMP Snare", "Trampa PEM");
        put(text, "Void Anchor", "Ancla del vacío");
        put(text, "Blind Hex", "Maldición cegadora");
        put(text, "Burden Hex", "Maldición de carga");
        put(text, "Doom Hex", "Maldición fatal");
        put(text, "Loaded Grudge", "Rencor cargado");
        put(text, "Chaos Retort", "Réplica del caos");
        put(text, "Fate's Revenge", "Venganza del destino");

        put(text, "A dependable first race setup with more power, speed and tire grip.",
                "Una preparación inicial fiable con más potencia, velocidad y agarre.");
        put(text, "A stripped chassis accelerates and changes direction quickly, but gives up tire stability.",
                "Un chasis aligerado acelera y gira rápido, sacrificando estabilidad.");
        put(text, "An aerodynamically efficient body carries speed on open road at the cost of cornering confidence.",
                "Una carrocería eficiente mantiene la velocidad en recta a costa de confianza en curva.");
        put(text, "Close gearing launches hard between corners but reaches its limit earlier on long straights.",
                "Las marchas cortas aceleran fuerte entre curvas, pero limitan las rectas largas.");
        put(text, "Light body panels improve acceleration, response and aero efficiency.",
                "Paneles ligeros mejoran aceleración, respuesta y aerodinámica.");
        put(text, "Sharper race hardware combines sustained power with high-speed stability.",
                "Componentes de carrera combinan potencia constante y estabilidad a alta velocidad.");
        put(text, "A reinforced, heavier car carries extra power and grip so contact no longer ruins its pace.",
                "Un coche reforzado y pesado gana potencia y agarre para resistir los golpes.");
        put(text, "Exceptional aero efficiency rewards committed high-speed driving without sacrificing basic stability.",
                "La gran eficiencia aerodinámica premia la velocidad sin perder estabilidad básica.");
        put(text, "An aggressive differential makes sustained rotation easy and straightens with strong drive.",
                "Un diferencial agresivo facilita derrapes largos y sale con fuerza.");
        put(text, "A rigid carbon cell cuts inertia while sharpening power delivery, aero and response.",
                "La célula rígida de carbono reduce inercia y mejora potencia, aero y respuesta.");
        put(text, "An aerodynamically efficient body and high-speed downforce turn open road into a decisive advantage.",
                "La carrocería eficiente y la carga aerodinámica dominan las zonas rápidas.");
        put(text, "A sealed floor creates exceptional cornering force while adding weight and aerodynamic resistance.",
                "El fondo sellado crea gran agarre en curva, pero añade peso y resistencia.");
        put(text, "A radical long-tail body maximizes aero efficiency for huge straight-line pace with modest cornering support.",
                "La carrocería de cola larga maximiza la velocidad en recta con apoyo moderado en curva.");
        put(text, "Active torque distribution rotates the car decisively through corners while preserving competitive speed and contact strength.",
                "El reparto activo de par mejora el giro sin perder velocidad ni fuerza de contacto.");
        put(text, "An ultralight structure delivers extreme acceleration, aero efficiency and precise handling.",
                "Una estructura ultraligera ofrece aceleración extrema, eficiencia aero y manejo preciso.");
        put(text, "Leaving an on-road corner under power creates a short launch onto the next section.",
                "Salir acelerando de una curva en pista produce un impulso breve.");
        put(text, "Finds a rival's wake sooner and turns close following into useful speed.",
                "Encuentra antes el rebufo y convierte la cercanía en velocidad.");
        put(text, "Continuous on-road driving builds a speed advantage that is lost by leaving the circuit.",
                "Conducir sin salir de pista acumula una ventaja de velocidad.");
        put(text, "A quick legitimate return to the road restores traction and accelerates back into the race.",
                "Volver rápido y limpiamente a pista recupera agarre y aceleración.");
        put(text, "Sustained on-road slip stores energy and releases it when the car straightens.",
                "El derrape sostenido en pista almacena energía que se libera al enderezar.");
        put(text, "Charges in another car's wake and launches when you pull out to pass.",
                "Carga energía en el rebufo y la libera al salir para adelantar.");
        put(text, "Every gained race position immediately provides power to complete the move.",
                "Cada posición ganada aporta potencia para completar la maniobra.");
        put(text, "Loads energy through a fast on-road corner and releases it as the road straightens.",
                "Carga energía en curvas rápidas y la libera al llegar la recta.");
        put(text, "Clean speed, accurate corner exits and uninterrupted momentum compound throughout the lap.",
                "Velocidad limpia y buenas salidas de curva se acumulan durante la vuelta.");
        put(text, "Drafting, corner exits and overtakes each trigger a powerful race-winning response.",
                "Rebufos, salidas de curva y adelantamientos activan fuertes impulsos.");
        put(text, "Reads the field and gains performance as the car falls back, reaching full strength in last place.",
                "Aumenta el rendimiento cuanto más atrás estás, hasta su máximo en última posición.");
        put(text, "Raises the car's pace whenever a rival is nearby, helping attacks and defensive runs.",
                "Aumenta el ritmo con rivales cerca para atacar y defender.");
        put(text, "Kicks the car forward when open road invites a nitro burst.",
                "Impulsa el coche cuando una recta libre permite usar nitro.");
        put(text, "Open road creates a second physical copy that races beside the original.",
                "En recta crea una segunda copia física junto al original.");
        put(text, "A glowing underbody fan pins the car down as a demanding corner arrives.",
                "Un ventilador luminoso pega el coche al suelo al llegar una curva exigente.");
        put(text, "The car phases out when traffic is nearby, becoming invisible and intangible to rivals.",
                "Con tráfico cerca, el coche se vuelve invisible e intangible.");
        put(text, "Prepares a random Tier 1 Powerup and copies its real trigger, effect and cooldown.",
                "Prepara un Potenciar T1 aleatorio con su activación, efecto y recarga.");
        put(text, "Prepares a random Tier 2 Powerup and copies its real trigger, effect and cooldown.",
                "Prepara un Potenciar T2 aleatorio con su activación, efecto y recarga.");
        put(text, "Prepares a random Tier 3 Powerup and copies its real trigger, effect and cooldown.",
                "Prepara un Potenciar T3 aleatorio con su activación, efecto y recarga.");
        put(text, "A qualified rival hit arms a counter that throws away the next car to strike you.",
                "Un golpe rival prepara un contraataque que repele al siguiente coche.");
        put(text, "A qualified rival hit arms a short pulsing field that forces nearby cars toward the outside.",
                "Un golpe rival prepara un campo pulsante que expulsa coches cercanos.");
        put(text, "A qualified hit marks its offender. After charging, it exchanges positions only while they are ahead.",
                "Marca al agresor e intercambia posiciones solo si va por delante.");
        put(text, "A qualified hit curses its offender's throttle, forcing them to commit through whatever comes next.",
                "Maldice el acelerador del agresor y lo obliga a acelerar al máximo.");
        put(text, "An energy shell forms when traffic closes in, absorbing frontal recoil.",
                "Un escudo de energía absorbe impactos frontales cuando se acerca el tráfico.");
        put(text, "Twin exhaust rockets ignite on a clear straight for a forceful launch.",
                "Dos cohetes de escape se encienden en recta para dar un gran impulso.");
        put(text, "Open road creates two physical copies spread across the track.",
                "En recta crea dos copias físicas repartidas por la pista.");
        put(text, "Open road creates three physical copies spread across the track.",
                "En recta crea tres copias físicas repartidas por la pista.");
        put(text, "An improved phase field hides the car and prevents rivals from making contact for longer.",
                "Un campo de fase mejorado oculta el coche e impide contactos durante más tiempo.");
        put(text, "A visible ground field forms in corners or close traffic for extreme stability.",
                "Un campo visible aporta estabilidad extrema en curvas o tráfico.");
        put(text, "Open road triggers an extreme launch and turns the car into a visible streak.",
                "Una recta libre activa un impulso extremo y convierte el coche en una estela.");
        put(text, "A championship phase system removes the car from sight and contact for an extended attack window.",
                "Un sistema de fase oculta el coche y evita contactos durante un ataque prolongado.");
        put(text, "A qualified rival hit arms a brutal hunt for the offender responsible.",
                "Un golpe rival prepara una caza brutal contra el agresor.");
        put(text, "A qualified hit marks its offender. After charging, the hook pulls you directly back toward them.",
                "Marca al agresor y después te atrae directamente hacia él.");
        put(text, "A qualified rival hit arms a wide high-energy field that clears space for your comeback.",
                "Un golpe rival prepara un campo amplio que despeja tu remontada.");
        put(text, "Throws a sticky tether at the rival who hit you and strips all tire traction.",
                "Lanza una atadura pegajosa al agresor y elimina su agarre.");
        put(text, "Launches a disruptive snare that forces the rival responsible for hitting you to brake without reversing.",
                "Lanza una trampa que obliga al agresor a frenar sin retroceder.");
        put(text, "Hurls a heavy energy anchor that forces the rival responsible for hitting you to brake without reversing.",
                "Lanza un ancla que obliga al agresor a frenar sin retroceder.");
        put(text, "Curses the rival who hit you until that offender collides with another car.",
                "Ciega al agresor hasta que choque con otro coche.");
        put(text, "Chains the rival who hit you to a heavier, blinded car until its next collision.",
                "Vuelve al agresor pesado y ciego hasta su próxima colisión.");
        put(text, "Crushes the rival who hit you with blindness, extreme weight, and reduced grip until its next collision.",
                "Impone ceguera, peso extremo y menos agarre hasta la próxima colisión.");
        put(text, "A rival hit executes a random Tier 1 Revenge card, then prepares a different retaliation.",
                "Un golpe ejecuta una Venganza T1 aleatoria y prepara otra.");
        put(text, "A rival hit executes a random Tier 2 Revenge card, then prepares a different retaliation.",
                "Un golpe ejecuta una Venganza T2 aleatoria y prepara otra.");
        put(text, "A rival hit executes a random Tier 3 Revenge card, then prepares a different retaliation.",
                "Un golpe ejecuta una Venganza T3 aleatoria y prepara otra.");
    }

    private static void put(Map<String, String> target, String english, String spanish) {
        target.put(english, spanish);
        String uppercaseEnglish = english.toUpperCase(Locale.ROOT);
        if (!uppercaseEnglish.equals(english)) {
            target.put(uppercaseEnglish, spanish.toUpperCase(Locale.ROOT));
        }
    }
}
