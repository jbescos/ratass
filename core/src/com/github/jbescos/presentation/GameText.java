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
        String tieredCardTitle = translateTieredCardTitle(language, english);
        if (tieredCardTitle != null) {
            return tieredCardTitle;
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
            return "PASAR " + english.substring("SKIP ".length());
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
        int targetSeparator = english.lastIndexOf(" on ");
        if (targetSeparator > 0) {
            return translate(language, english.substring(0, targetSeparator))
                    + " contra "
                    + english.substring(targetSeparator + 4);
        }
        return english;
    }

    private static String translateTieredCardTitle(
            GameLanguage language,
            String english) {
        if (english.length() < 5 || english.charAt(0) != 'T') {
            return null;
        }
        int separator = english.indexOf("  ");
        if (separator < 2 || separator + 2 >= english.length()) {
            return null;
        }
        for (int i = 1; i < separator; i++) {
            if (!Character.isDigit(english.charAt(i))) {
                return null;
            }
        }
        return english.substring(0, separator + 2)
                + translate(language, english.substring(separator + 2));
    }

    public static String countdownContext(
            GameLanguage language,
            int currentCircuit,
            int circuitCount) {
        String ready = translate(language, "GET READY");
        if (currentCircuit <= 0 || circuitCount <= 0) {
            return ready;
        }
        return ready
                + "  |  "
                + translate(language, "CIRCUIT")
                + " "
                + currentCircuit
                + " / "
                + circuitCount;
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
                || text.indexOf('\n') >= 0
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
        result = result.replace("Always active", "Siempre activo");
        result = result.replace("Driver: best avg-lap", "Piloto: mejor vuelta media");
        result = result.replace("Automatic call", "Llamada automática");
        result = result.replace("best avg-lap driver", "piloto con mejor vuelta media");
        result = result.replace("best-driver advice", "consejos del mejor piloto");
        result = result.replace("3s charge", "carga de 3s");
        result = result.replace("30s hunt", "caza de 30s");
        result = result.replace("After 3s", "Tras 3s");
        result = result.replace("Offender for", "Agresor durante");
        result = result.replace("Offender", "Agresor");
        result = result.replace("Random Tier 1 Revenge", "Venganza T1 aleatoria");
        result = result.replace("Random Tier 2 Revenge", "Venganza T2 aleatoria");
        result = result.replace("Random Tier 3 Revenge", "Venganza T3 aleatoria");
        result = result.replace("random Tier 1 Powerup", "Potenciar T1 aleatorio");
        result = result.replace("random Tier 2 Powerup", "Potenciar T2 aleatorio");
        result = result.replace("random Tier 3 Powerup", "Potenciar T3 aleatorio");
        result = result.replace("Random Tier", "Nivel aleatorio");
        result = result.replace("Automatic", "Automático");
        result = result.replace("Activation", "Activación");
        result = result.replace("Revenge activation", "Activación de venganza");
        result = result.replace("Shared cards and Revenge", "Cartas y Venganza compartidas");
        result = result.replace("Cancels targeting Revenge", "Cancela venganzas dirigidas");
        result = result.replace("Debuffs remain", "Los perjuicios continúan");
        result = result.replace("Intangible", "Intangible");
        result = result.replace("effect", "efecto");
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
        result = result.replace("Hit received", "Golpe recibido");
        result = result.replace("Long straight", "Recta larga");
        result = result.replace("Slipstream", "Rebufo");
        result = result.replace("Corner", "Curva");
        result = result.replace("Drifting", "Derrape");
        result = result.replace("Off-road", "Fuera de pista");
        result = result.replace("Each activation", "Cada activación");
        result = result.replace("In draft", "En rebufo");
        result = result.replace("Stay on-road", "Seguir en pista");
        result = result.replace("Safe re-entry", "Regreso seguro");
        result = result.replace("Drift exit", "Salida de derrape");
        result = result.replace("Leave draft", "Salir del rebufo");
        result = result.replace("Race events", "Eventos de carrera");
        result = result.replace("Race event", "Evento de carrera");
        result = result.replace("Lower position", "Posición retrasada");
        result = result.replace("Clean lap", "Vuelta limpia");
        result = result.replace("Clean run", "Conducción limpia");
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
        result = result.replace("Aero", "Aero");
        result = result.replace("bonuses only", "solo bonificaciones");
        result = result.replace("bonuses and penalties", "bonificaciones y penalizaciones");
        result = result.replace("while cornering", "solo en curva");
        result = result.replace("Stronger hits", "Golpes más fuertes");
        result = result.replace("Freer rear while drifting", "Trasera libre al derrapar");
        result = result.replace("reach and boost", "alcance e impulso");
        result = result.replace("up to", "hasta");
        result = result.replace("bursts", "en ráfagas");
        result = result.replace("Cooldown after effect", "Recarga tras efecto");
        result = result.replace("Cooldown", "Recarga");
        result = result.replace("random Tier", "Nivel aleatorio");
        result = result.replace("full throttle", "acelerador máximo");
        result = result.replace("full brake", "frenado máximo");
        result = result.replace("3 impact shots", "3 disparos de impacto");
        result = result.replace("impact shots", "disparos de impacto");
        result = result.replace("shots/s", "disparos/s");
        result = result.replace("1s apart", "cada 1s");
        result = result.replace("after 3s", "tras 3s");
        result = result.replace("swap with", "intercambio con");
        result = result.replace("pull offender to you", "atraer al agresor hacia ti");
        result = result.replace("over 1s", "durante 1s");
        result = result.replace("reflect impact", "reflejar impacto");
        result = result.replace("medium outward field", "campo repulsor medio");
        result = result.replace("wide outward field", "campo repulsor amplio");
        result = result.replace("outward field", "campo repulsor");
        result = result.replace("until collision", "hasta colisión");
        result = result.replace("blind", "ciego");
        result = result.replace("random", "aleatoria");
        result = result.replace("immediately", "inmediatamente");
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
        result = result.replace("you lead", "quedas delante");
        result = result.replace("leading rival falls last", "el rival líder queda último");
        result = result.replace(" for ", " durante ");
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
        put(text, "Fit", "Ajustar");
        put(text, "Display", "Pantalla");
        put(text, "Windowed", "Ventana");
        put(text, "Fullscreen", "Pantalla completa");
        put(text, "Music", "Música");
        put(text, "Sound FX", "Efectos");
        put(text, "Left", "Izquierda");
        put(text, "Right", "Derecha");
        put(text, "Press a key", "Pulsa una tecla");
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
        put(text, "CARD AVAILABILITY", "DISPONIBILIDAD DE CARTAS");
        put(text, "WEATHER", "CLIMA");
        put(text, "CARD TIERS", "NIVELES DE CARTA");
        put(text, "Tier", "Nivel");
        put(text, "Unlock lvl", "Nivel desbloqueo");
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
        put(text, "STRATEGY", "ESTRATEGIA");
        put(text, "ALGORITHMIC", "ALGORÍTMICA");
        put(text, "ACTIVE CARD EFFECTS", "EFECTOS ACTIVOS");
        put(text, "RACE STATUS", "ESTADO DE CARRERA");
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
        put(text, "EDITING", "EDITANDO");
        put(text, "COPY TO ALL", "COPIAR A TODOS");
        put(text, "ACCEPT", "ACEPTAR");
        put(text, "CANCEL", "CANCELAR");
        put(text, "SKIP", "PASAR");
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
        put(text, "CIRCUIT", "CIRCUITO");
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
        put(text, "sandbox settings", "ajustes de pruebas");
        put(text, "sensors & route", "sensores y trazada");
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
        put(text, "Ballast Sprint", "Sprint lastrado");
        put(text, "Reinforced Streamliner", "Perfil reforzado");
        put(text, "Featherweight Drive", "Transmisión ligera");
        put(text, "Track Wing", "Alerón de pista");
        put(text, "Grounded Aero", "Aero al suelo");
        put(text, "Light Compound", "Compuesto ligero");
        put(text, "Agile Chassis", "Chasis ágil");
        put(text, "Streamlined Chassis", "Chasis aerodinámico");
        put(text, "Aero Featherweight", "Peso pluma aero");
        put(text, "Reinforced Longtail", "Cola larga reforzada");
        put(text, "Titanium Drive", "Transmisión de titanio");
        put(text, "Downforce Package", "Paquete de carga");
        put(text, "Grounded Downforce", "Carga al suelo");
        put(text, "Magnesium Suspension", "Suspensión de magnesio");
        put(text, "Aero-Agile Chassis", "Chasis aero ágil");
        put(text, "Carbon Longtail", "Cola larga de carbono");
        put(text, "Venturi Monocoque", "Monocasco Venturi");
        put(text, "Power Monocoque", "Monocasco de potencia");
        put(text, "Titanium Skeleton", "Esqueleto de titanio");
        put(text, "Hypercar Core", "Núcleo hipercoche");
        put(text, "Active Aero Shell", "Carcasa aero activa");
        put(text, "Carbon Prototype", "Prototipo de carbono");
        put(text, "Track Vacuum", "Vacío de pista");
        put(text, "Wing Car", "Coche alado");
        put(text, "Feather Ground", "Suelo ligero");
        put(text, "Technique Coupler", "Acoplador técnico");
        put(text, "Technique Matrix", "Matriz técnica");
        put(text, "Technique Singularity", "Singularidad técnica");
        put(text, "Corner Focus", "Enfoque de curva");
        put(text, "Draft Focus", "Enfoque de rebufo");
        put(text, "Straight Focus", "Enfoque de recta");
        put(text, "Drift Focus", "Enfoque de derrape");
        put(text, "Rally Focus", "Enfoque de rally");
        put(text, "Apex Focus", "Enfoque de vértice");
        put(text, "Sprint Focus", "Enfoque de sprint");
        put(text, "Slide Focus", "Enfoque de deslizamiento");
        put(text, "Traction Focus", "Enfoque de tracción");
        put(text, "Agility Focus", "Enfoque de agilidad");
        put(text, "Corner Expert", "Experto en curva");
        put(text, "Draft Expert", "Experto en rebufo");
        put(text, "Straight Expert", "Experto en recta");
        put(text, "Drift Expert", "Experto en derrape");
        put(text, "Rally Expert", "Experto en rally");
        put(text, "Apex Expert", "Experto en vértice");
        put(text, "Sprint Expert", "Experto en sprint");
        put(text, "Slide Expert", "Experto en deslizamiento");
        put(text, "Traction Expert", "Experto en tracción");
        put(text, "Agility Expert", "Experto en agilidad");
        put(text, "Corner Master", "Maestro de curva");
        put(text, "Draft Master", "Maestro de rebufo");
        put(text, "Straight Master", "Maestro de recta");
        put(text, "Drift Master", "Maestro de derrape");
        put(text, "Rally Master", "Maestro de rally");
        put(text, "Apex Master", "Maestro de vértice");
        put(text, "Sprint Master", "Maestro de sprint");
        put(text, "Slide Master", "Maestro de deslizamiento");
        put(text, "Traction Master", "Maestro de tracción");
        put(text, "Agility Master", "Maestro de agilidad");
        put(text, "Underdog Instinct", "Instinto del rezagado");
        put(text, "Comeback Drive", "Remontada");
        put(text, "Last Place Fury", "Furia del último");
        put(text, "Close Quarters", "Cuerpo a cuerpo");
        put(text, "Pack Racer", "Piloto de grupo");
        put(text, "Traffic Dominance", "Dominio del tráfico");
        put(text, "Powerup Link", "Enlace potenciador");
        put(text, "Powerup Matrix", "Matriz potenciadora");
        put(text, "Powerup Nexus", "Nexo potenciador");
        put(text, "Nitro Pulse", "Pulso nitro");
        put(text, "Ace Hotline", "Línea del as");
        put(text, "Time Ripple", "Onda temporal");
        put(text, "Quantum Duo", "Dúo cuántico");
        put(text, "Grip Fan", "Ventilador de agarre");
        put(text, "Ghost Cloak", "Manto fantasma");
        put(text, "Lucky Spark", "Chispa de suerte");
        put(text, "Grudge Spark", "Chispa de rencor");
        put(text, "Rival hit: arm until next rival hit | Reflect impact",
                "Golpe rival: preparar hasta el siguiente golpe | Reflejar impacto");
        put(text, "Draft Magnet", "Imán de rebufo");
        put(text, "Position Hijack", "Robo de posición");
        put(text, "Redline Hex", "Maldición roja");
        put(text, "Phase Shield", "Escudo de fase");
        put(text, "Rocket Exhaust", "Escape cohete");
        put(text, "Priority Hotline", "Línea prioritaria");
        put(text, "Chrono Shift", "Cambio crono");
        put(text, "Quantum Trio", "Trío cuántico");
        put(text, "Phantom Cloak", "Manto espectral");
        put(text, "Chaos Relay", "Relé del caos");
        put(text, "Vengeance Core", "Núcleo de venganza");
        put(text, "Gravity Well", "Pozo de gravedad");
        put(text, "Quantum Quartet", "Cuarteto cuántico");
        put(text, "Hyperdrive", "Hiperimpulso");
        put(text, "Temporal Dominion", "Dominio temporal");
        put(text, "Void Cloak", "Manto del vacío");
        put(text, "Wildcard Core", "Núcleo comodín");
        put(text, "Nemesis Engine", "Motor némesis");
        put(text, "Crown Breaker", "Rompecoronas");
        put(text, "Vendetta Hook", "Gancho de vendetta");
        put(text, "Repulsor Wave", "Onda repulsora");
        put(text, "Hunter Barrage", "Ráfaga cazadora");
        put(text, "Hunter Storm", "Tormenta cazadora");
        put(text, "Rival hit -> offender: 2 shots/s for 3s",
                "Golpe rival -> agresor: 2 disparos/s durante 3s");
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
        put(text, "Triad Coup", "Golpe de tríada");

        put(text, "A calibrated control unit strengthens the equipped Technique effect.",
                "Una unidad de control calibrada refuerza el efecto de la Técnica equipada.");
        put(text, "A racing control matrix greatly strengthens the equipped Technique effect.",
                "Una matriz de control de competición refuerza mucho la Técnica equipada.");
        put(text, "An experimental control core doubles the equipped Technique effect.",
                "Un núcleo de control experimental duplica el efecto de la Técnica equipada.");
        put(text, "A passive timing link strengthens Powerups and recharges them faster.",
                "Un enlace pasivo refuerza los Potenciadores y acelera su recarga.");
        put(text, "A passive timing matrix strengthens Powerups and recharges them much faster.",
                "Una matriz pasiva refuerza los Potenciadores y acelera mucho su recarga.");
        put(text, "A passive timing nexus doubles Powerup effects and cooldown recovery.",
                "Un nexo pasivo duplica los efectos de Potenciar y su recuperación.");
        put(text, "Technique effects x1.25", "Efectos de Técnica x1.25");
        put(text, "Technique effects x1.5", "Efectos de Técnica x1.5");
        put(text, "Technique effects x2", "Efectos de Técnica x2");
        put(text, "Activation: Passive\nPowerup effects x1.25\nCooldown recovery x1.25",
                "Activación: Pasiva\nEfectos de Potenciar x1.25\nRecuperación x1.25");
        put(text, "Activation: Passive\nPowerup effects x1.5\nCooldown recovery x1.5",
                "Activación: Pasiva\nEfectos de Potenciar x1.5\nRecuperación x1.5");
        put(text, "Activation: Passive\nPowerup effects x2\nCooldown recovery x2",
                "Activación: Pasiva\nEfectos de Potenciar x2\nRecuperación x2");

        put(text, "Cornering amplifies active grip bonuses and every active aero bonus or penalty. Grip penalties and weather stay unchanged.",
                "En curva amplifica las bonificaciones activas de agarre y cada bonificación o penalización activa de aero. Las penalizaciones de agarre y el clima no cambian.");
        put(text, "Slipstreaming amplifies every active power and aero bonus or penalty.",
                "En rebufo amplifica cada bonificación o penalización activa de potencia y aero.");
        put(text, "A long straight amplifies every active power and aero bonus or penalty.",
                "En recta larga amplifica cada bonificación o penalización activa de potencia y aero.");
        put(text, "Drifting amplifies every active power and mass bonus or penalty.",
                "Al derrapar amplifica cada bonificación o penalización activa de potencia y masa.");
        put(text, "Leaving the road amplifies every active power, aero, and mass bonus or penalty, plus active grip bonuses. Grip penalties and weather stay unchanged.",
                "Fuera de pista amplifica cada bonificación o penalización activa de potencia, aero y masa, además de las bonificaciones activas de agarre. Las penalizaciones de agarre y el clima no cambian.");
        put(text, "Cornering amplifies every active aero and mass bonus or penalty.",
                "En curva amplifica cada bonificación o penalización activa de aero y masa.");
        put(text, "A long straight amplifies every active power and mass bonus or penalty.",
                "En recta larga amplifica cada bonificación o penalización activa de potencia y masa.");
        put(text, "Drifting amplifies every active aero and mass bonus or penalty.",
                "Al derrapar amplifica cada bonificación o penalización activa de aero y masa.");
        put(text, "Cornering amplifies every active power bonus or penalty and active grip bonuses. Grip penalties and weather stay unchanged.",
                "En curva amplifica cada bonificación o penalización activa de potencia y las bonificaciones activas de agarre. Las penalizaciones de agarre y el clima no cambian.");
        put(text, "Cornering amplifies active grip bonuses and every active mass bonus or penalty. Grip penalties and weather stay unchanged.",
                "En curva amplifica las bonificaciones activas de agarre y cada bonificación o penalización activa de masa. Las penalizaciones de agarre y el clima no cambian.");

        put(text, "Power and grip trade aerodynamic efficiency.",
                "Potencia y agarre sacrifican eficiencia aerodinámica.");
        put(text, "Power and grip require extra chassis mass.",
                "Potencia y agarre requieren más masa de chasis.");
        put(text, "Power and aero efficiency trade tire grip.",
                "Potencia y eficiencia aero sacrifican agarre.");
        put(text, "Power and aero efficiency require extra chassis mass.",
                "Potencia y eficiencia aero requieren más masa de chasis.");
        put(text, "Power and lower mass trade tire grip.",
                "Potencia y menor masa sacrifican agarre.");
        put(text, "Power and lower mass trade aerodynamic efficiency.",
                "Potencia y menor masa sacrifican eficiencia aerodinámica.");
        put(text, "Grip and aero efficiency trade engine power.",
                "Agarre y eficiencia aero sacrifican potencia.");
        put(text, "Grip and aero efficiency require extra chassis mass.",
                "Agarre y eficiencia aero requieren más masa de chasis.");
        put(text, "Grip and lower mass trade engine power.",
                "Agarre y menor masa sacrifican potencia.");
        put(text, "Grip and lower mass trade aerodynamic efficiency.",
                "Agarre y menor masa sacrifican eficiencia aerodinámica.");
        put(text, "Aero efficiency and lower mass trade engine power.",
                "Eficiencia aero y menor masa sacrifican potencia.");
        put(text, "Aero efficiency and lower mass trade tire grip.",
                "Eficiencia aero y menor masa sacrifican agarre.");
        put(text, "Power and grip improve together.",
                "Potencia y agarre mejoran juntos.");
        put(text, "Power and aero efficiency improve together.",
                "Potencia y eficiencia aero mejoran juntas.");
        put(text, "Grip and aero efficiency improve together.",
                "Agarre y eficiencia aero mejoran juntos.");
        put(text, "Power and lower mass improve together.",
                "Potencia y menor masa mejoran juntas.");
        put(text, "Grip and lower mass improve together.",
                "Agarre y menor masa mejoran juntos.");
        put(text, "Aero efficiency and lower mass improve together.",
                "Eficiencia aero y menor masa mejoran juntas.");
        put(text, "Power, grip and aero efficiency improve together.",
                "Potencia, agarre y eficiencia aero mejoran juntos.");
        put(text, "Power, grip and lower mass improve together.",
                "Potencia, agarre y menor masa mejoran juntos.");
        put(text, "Power, aero efficiency and lower mass improve together.",
                "Potencia, eficiencia aero y menor masa mejoran juntos.");
        put(text, "Grip, aero efficiency and lower mass improve together.",
                "Agarre, eficiencia aero y menor masa mejoran juntos.");
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
        put(text, "Leaving an on-road corner amplifies the car's tuned power and grip, including weaknesses.",
                "Salir de una curva amplifica la potencia y el agarre modificados, incluso sus debilidades.");
        put(text, "A rival's wake amplifies the car's aerodynamic tuning, whether it is beneficial or harmful.",
                "El rebufo rival amplifica la configuración aerodinámica, sea favorable o perjudicial.");
        put(text, "Continuous clean driving increasingly amplifies tuned power and aero until the car leaves the road.",
                "Conducir limpiamente amplifica potencia y aero hasta que el coche sale de pista.");
        put(text, "A quick legitimate return to the road amplifies the car's current power and grip tuning.",
                "Un regreso rápido y válido a pista amplifica la potencia y el agarre actuales.");
        put(text, "Sustained on-road slip stores a multiplier for the car's tuned power and aero.",
                "El derrape sostenido en pista acumula un multiplicador de potencia y aero.");
        put(text, "Charges in another car's wake, then amplifies tuned power and aero when pulling out.",
                "Carga en el rebufo y amplifica potencia y aero al salir de él.");
        put(text, "Every gained race position amplifies tuned power and grip to consolidate the pass.",
                "Cada posición ganada amplifica potencia y agarre para asegurar el adelantamiento.");
        put(text, "A fast corner loads a multiplier for tuned power, grip and aero on exit.",
                "Una curva rápida carga un multiplicador de potencia, agarre y aero para la salida.");
        put(text, "Uninterrupted clean speed progressively doubles every tuned performance deviation.",
                "La velocidad limpia duplica progresivamente cada desviación de rendimiento.");
        put(text, "Drafting, corner exits and overtakes double tuned performance deviations during their response.",
                "Rebufos, salidas y adelantamientos duplican las desviaciones mientras responden.");
        put(text, "Reads the field and gains performance as the car falls back, reaching full strength in last place.",
                "Aumenta el rendimiento cuanto más atrás estás, hasta su máximo en última posición.");
        put(text, "Raises the car's pace whenever a rival is nearby, helping attacks and defensive runs.",
                "Aumenta el ritmo con rivales cerca para atacar y defender.");
        put(text, "Kicks the car forward when open road invites a nitro burst.",
                "Impulsa el coche cuando una recta libre permite usar nitro.");
        put(text, "Calls the best benchmarked driver, who gives you driving advice for 10 seconds.",
                "Llama al mejor piloto, que te aconseja durante 10 segundos.");
        put(text, "Doubles local time for your car and quantum copies; movement and decisions run x2.",
                "Duplica el tiempo local de tu coche y sus copias cuánticas; movimiento y decisiones x2.");
        put(text, "Doubles local time for your car and quantum copies, recharging faster.",
                "Duplica el tiempo local de tu coche y sus copias cuánticas, con recarga más rápida.");
        put(text, "Doubles local time for your entire quantum family with the fastest recharge.",
                "Duplica el tiempo local de toda tu familia cuántica con la recarga más rápida.");
        put(text, "Automatic: local time x2 | 2s\nCooldown: 60s",
                "Automático: tiempo local x2 | 2s\nRecarga: 60s");
        put(text, "Automatic: local time x2 | 2s\nCooldown: 40s",
                "Automático: tiempo local x2 | 2s\nRecarga: 40s");
        put(text, "Automatic: local time x2 | 2s\nCooldown: 30s",
                "Automático: tiempo local x2 | 2s\nRecarga: 30s");
        put(text, "Creates two physical cars. Each drives independently, shares the same cards and executes Revenge with the group; a hit to any copy arms it.",
                "Crea dos coches físicos. Cada uno conduce de forma independiente, comparte cartas y ejecuta la Venganza con el grupo; golpear cualquier copia la prepara.");
        put(text, "A glowing underbody fan pins the car down as a demanding corner arrives.",
                "Un ventilador luminoso pega el coche al suelo al llegar una curva exigente.");
        put(text, "The car phases out when traffic is nearby, becoming invisible and intangible to rivals.",
                "Con tráfico cerca, el coche se vuelve invisible e intangible.");
        put(text, "Prepares a random Tier 1 Powerup and copies its real trigger, effect and cooldown.",
                "Prepara un Potenciar T1 aleatorio con su activación, efecto y recarga.");
        put(text, "A green catalyst ignites whenever Revenge activates and strengthens its real effect.",
                "Un catalizador verde se enciende con cada Venganza y refuerza su efecto real.");
        put(text, "Prepares a random Tier 2 Powerup and copies its real trigger, effect and cooldown.",
                "Prepara un Potenciar T2 aleatorio con su activación, efecto y recarga.");
        put(text, "A stronger green core surges whenever Revenge activates and magnifies its outcome.",
                "Un núcleo verde más potente surge con cada Venganza y amplía su resultado.");
        put(text, "Prepares a random Tier 3 Powerup and copies its real trigger, effect and cooldown.",
                "Prepara un Potenciar T3 aleatorio con su activación, efecto y recarga.");
        put(text, "An extreme green engine doubles the consequences whenever Revenge activates.",
                "Un motor verde extremo duplica las consecuencias de cada Venganza.");
        put(text, "A rival hit arms the counter until the next qualified hit is reflected into its attacker.",
                "Un golpe rival arma el contraataque hasta que el siguiente golpe válido se refleja contra el atacante.");
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
        put(text, "Replaces the active driver with the best benchmarked driver while this Powerup is equipped.",
                "Sustituye al piloto activo por el mejor piloto evaluado mientras este Potenciar esté equipado.");
        put(text, "Creates three physical cars. Each drives independently, shares the same cards and executes Revenge with the group; a hit to any copy arms it.",
                "Crea tres coches físicos. Cada uno conduce de forma independiente, comparte cartas y ejecuta la Venganza con el grupo; golpear cualquier copia la prepara.");
        put(text, "Creates four physical cars. Each drives independently, shares the same cards and executes Revenge with the group; a hit to any copy arms it.",
                "Crea cuatro coches físicos. Cada uno conduce de forma independiente, comparte cartas y ejecuta la Venganza con el grupo; golpear cualquier copia la prepara.");
        put(text, "An improved phase field hides the car and prevents rivals from making contact for longer.",
                "Un campo de fase mejorado oculta el coche e impide contactos durante más tiempo.");
        put(text, "A visible ground field forms in corners or close traffic for extreme stability.",
                "Un campo visible aporta estabilidad extrema en curvas o tráfico.");
        put(text, "Open road triggers an extreme launch and turns the car into a visible streak.",
                "Una recta libre activa un impulso extremo y convierte el coche en una estela.");
        put(text, "A championship phase system removes the car from sight and contact for an extended attack window.",
                "Un sistema de fase oculta el coche y evita contactos durante un ataque prolongado.");
        put(text, "A rival hit marks its offender and empowers you until an automatic close-range ram.",
                "Un golpe rival marca al agresor y te potencia hasta una embestida automática a corta distancia.");
        put(text, "A qualified hit marks its offender. After charging, the hook pulls them back only while they are ahead.",
                "Marca al agresor y después lo atrae hacia ti solo si va por delante.");
        put(text, "A qualified rival hit arms a medium-range energy wave that pushes nearby cars away.",
                "Un golpe rival prepara una onda de alcance medio que aparta los coches cercanos.");
        put(text, "Marks the rival who hit you, then hunts them anywhere on the circuit with three impact shots.",
                "Marca al rival que te golpeó y lo caza por todo el circuito con tres disparos de impacto.");
        put(text, "Marks the rival who hit you, then saturates their position with a rapid impact storm anywhere on the circuit.",
                "Marca al rival que te golpeó y satura su posición con una rápida tormenta de impactos por todo el circuito.");
        put(text, "A qualified rival hit arms a wide high-energy field that clears space for your comeback.",
                "Un golpe rival prepara un campo amplio que despeja tu remontada.");
        put(text, "Throws a sticky tether at the rival who hit you and strips all tire traction.",
                "Lanza una atadura pegajosa al agresor y elimina su agarre.");
        put(text, "Launches a disruptive snare that forces the rival responsible for hitting you to brake without reversing.",
                "Lanza una trampa que obliga al agresor a frenar sin retroceder.");
        put(text, "Hurls a heavy energy anchor that forces the rival responsible for hitting you to brake without reversing.",
                "Lanza un ancla que obliga al agresor a frenar sin retroceder.");
        put(text, "Blinds and weakens the rival who hit you for 20 seconds.",
                "Ciega y debilita durante 20 segundos al rival que te golpeó.");
        put(text, "Chains the rival who hit you to a heavier, weakened and blinded car for 30 seconds.",
                "Vuelve pesado, débil y ciego durante 30 segundos al rival que te golpeó.");
        put(text, "Crushes the rival who hit you with blindness, extreme weight, and severe performance loss for 40 seconds.",
                "Impone durante 40 segundos ceguera, peso extremo y una gran pérdida de rendimiento al rival que te golpeó.");
        put(text, "Binds the offender and the car directly behind you, then reverses their places while moving you to the front.",
                "Une al agresor y al coche justo detrás, invierte sus puestos y te coloca delante.");
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
