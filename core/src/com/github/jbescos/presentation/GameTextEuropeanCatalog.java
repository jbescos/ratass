package com.github.jbescos.presentation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** French, German and Italian text kept outside the main rendering class. */
final class GameTextEuropeanCatalog {
    private static final String[][] UI_TEXT = {
        {"New Game", "Nouvelle partie", "Neues Spiel", "Nuova partita"},
        {"Continue", "Continuer", "Fortsetzen", "Continua"},
        {"Sandbox", "Bac à sable", "Testbereich", "Sandbox"},
        {"Options", "Options", "Optionen", "Opzioni"},
        {"Exit", "Quitter", "Beenden", "Esci"},
        {"Back", "Retour", "Zurück", "Indietro"},
        {"Back to Sandbox", "Retour au bac à sable", "Zurück zum Testbereich", "Torna alla sandbox"},
        {"Cancel", "Annuler", "Abbrechen", "Annulla"},
        {"Main Menu", "Menu principal", "Hauptmenü", "Menu principale"},
        {"Paused", "Pause", "Pause", "Pausa"},
        {"Custom", "Personnalisé", "Benutzerdefiniert", "Personalizzata"},
        {"Choose Run", "Choisir une partie", "Lauf wählen", "Scegli partita"},
        {"Custom Run", "Partie personnalisée", "Eigener Lauf", "Partita personalizzata"},
        {"Choose Car", "Choisir la voiture", "Auto wählen", "Scegli auto"},
        {"Choose Your Car", "Choisissez votre voiture", "Wähle dein Auto", "Scegli la tua auto"},
        {"Start Race", "Lancer la course", "Rennen starten", "Inizia gara"},
        {"Name", "Nom", "Name", "Nome"},
        {"Normal", "Normal", "Normal", "Normale"},
        {"Maps", "Circuits", "Strecken", "Mappe"},
        {"Map", "Circuit", "Strecke", "Mappa"},
        {"No maps", "Aucun circuit", "Keine Strecken", "Nessuna mappa"},
        {"Camera", "Caméra", "Kamera", "Telecamera"},
        {"Top Down", "Vue du dessus", "Draufsicht", "Dall'alto"},
        {"Chase", "Suivi", "Verfolgung", "Inseguimento"},
        {"Whole Map", "Circuit entier", "Ganze Strecke", "Mappa intera"},
        {"Free", "Libre", "Frei", "Libera"},
        {"Zoom", "Zoom", "Zoom", "Zoom"},
        {"Fit", "Ajuster", "Einpassen", "Adatta"},
        {"Display", "Affichage", "Anzeige", "Schermo"},
        {"Windowed", "Fenêtré", "Fenster", "Finestra"},
        {"Fullscreen", "Plein écran", "Vollbild", "Schermo intero"},
        {"Music", "Musique", "Musik", "Musica"},
        {"Sound FX", "Effets", "Effekte", "Effetti"},
        {"TV CAMERA", "CAMÉRA TV", "TV-KAMERA", "REGIA TV"},
        {"Loading settings", "Chargement des réglages", "Einstellungen laden", "Caricamento impostazioni"},
        {"Loading drivers", "Chargement des pilotes", "Fahrer laden", "Caricamento piloti"},
        {"Loading circuits", "Chargement des circuits", "Strecken laden", "Caricamento circuiti"},
        {"Loading Rogue Circuit", "Chargement de Rogue Circuit", "Rogue Circuit laden", "Caricamento Rogue Circuit"},
        {"Loading Circuit", "Chargement du circuit", "Strecke laden", "Caricamento circuito"},
        {"Loading Sandbox", "Chargement du bac à sable", "Testbereich laden", "Caricamento sandbox"},
        {"Loading Race", "Chargement de la course", "Rennen laden", "Caricamento gara"},
        {"Building starting grid", "Préparation de la grille", "Startfeld aufbauen", "Preparazione griglia"},
        {"Starting engines", "Démarrage des moteurs", "Motoren starten", "Avvio motori"},
        {"Preparing menu", "Préparation du menu", "Menü vorbereiten", "Preparazione menu"},
        {"Ready", "Prêt", "Bereit", "Pronto"},
        {"CARD TYPES", "TYPES DE CARTE", "KARTENTYPEN", "TIPI DI CARTA"},
        {"WEATHER", "MÉTÉO", "WETTER", "METEO"},
        {"CARD TIERS", "NIVEAUX DE CARTE", "KARTENSTUFEN", "LIVELLI CARTE"},
        {"Laps", "Tours", "Runden", "Giri"},
        {"XP / level", "XP / niveau", "XP / Stufe", "XP / livello"},
        {"Lap XP cap", "Limite XP / tour", "Runden-XP-Limit", "Limite XP giro"},
        {"Player", "Pilote", "Fahrer", "Pilota"},
        {"Best", "Meilleur", "Beste", "Migliore"},
        {"Current", "Actuel", "Aktuell", "Attuale"},
        {"Pts", "Pts", "Pkt", "Pti"},
        {"Lap", "Tour", "Runde", "Giro"},
        {"Time", "Temps", "Zeit", "Tempo"},
        {"Circuit Map", "Plan du circuit", "Streckenkarte", "Mappa circuito"},
        {"CAR STATS", "STATS VOITURE", "FAHRZEUGWERTE", "STATISTICHE AUTO"},
        {"TELEMETRY", "TÉLÉMÉTRIE", "TELEMETRIE", "TELEMETRIA"},
        {"CARDS", "CARTES", "KARTEN", "CARTE"},
        {"LOADOUT", "ÉQUIPEMENT", "AUSRÜSTUNG", "EQUIPAGGIAMENTO"},
        {"ACTIVE CARD EFFECTS", "EFFETS ACTIFS", "AKTIVE EFFEKTE", "EFFETTI ATTIVI"},
        {"RACE STATUS", "ÉTAT DE COURSE", "RENNSTATUS", "STATO GARA"},
        {"RACE LOG", "JOURNAL", "RENNPROTOKOLL", "REGISTRO GARA"},
        {"IMPACT", "IMPACT", "AUFPRALL", "IMPATTO"},
        {"OVERTAKE", "DÉPASSEMENT", "ÜBERHOLEN", "SORPASSO"},
        {"WARNING", "ALERTE", "WARNUNG", "AVVISO"},
        {"SPEED", "VITESSE", "TEMPO", "VELOCITÀ"},
        {"OFF ROAD", "HORS PISTE", "ABSEITS", "FUORI PISTA"},
        {"REVERSE", "MARCHE AR.", "RÜCKWÄRTS", "RETROMARCIA"},
        {"THROTTLE", "ACCÉLÉRATEUR", "GAS", "ACCELERATORE"},
        {"BRAKE", "FREIN", "BREMSE", "FRENO"},
        {"STEERING", "DIRECTION", "LENKUNG", "STERZO"},
        {"DRIFT", "DÉRAPAGE", "DRIFT", "DERAPATA"},
        {"POWER", "PUISSANCE", "LEISTUNG", "POTENZA"},
        {"TOP SPEED", "VIT. MAX", "HÖCHSTTEMPO", "VEL. MAX"},
        {"GRIP", "ADHÉRENCE", "GRIP", "ADERENZA"},
        {"MASS", "MASSE", "MASSE", "MASSA"},
        {"AVG LAP", "TOUR MOYEN", "Ø RUNDE", "GIRO MEDIO"},
        {"MAX SPEED", "VIT. MAX", "MAX. TEMPO", "VEL. MAX"},
        {"LEVEL UP - CHOOSE AN UPGRADE", "NIVEAU SUPÉRIEUR - CHOISISSEZ", "STUFENAUFSTIEG - UPGRADE WÄHLEN", "NUOVO LIVELLO - SCEGLI MIGLIORIA"},
        {"YOUR 5 SLOTS", "VOS 5 EMPLACEMENTS", "DEINE 5 PLÄTZE", "I TUOI 5 SLOT"},
        {"SANDBOX - CHOOSE A CARD", "BAC À SABLE - CHOISISSEZ", "TESTBEREICH - KARTE WÄHLEN", "SANDBOX - SCEGLI UNA CARTA"},
        {"ACCEPT", "ACCEPTER", "ANNEHMEN", "ACCETTA"},
        {"CANCEL", "ANNULER", "ABBRECHEN", "ANNULLA"},
        {"SKIP", "PASSER", "ÜBERSPRINGEN", "SALTA"},
        {"EMPTY", "VIDE", "LEER", "VUOTO"},
        {"EMPTY SLOT", "EMPLACEMENT VIDE", "LEERER PLATZ", "SLOT VUOTO"},
        {"No card equipped", "Aucune carte équipée", "Keine Karte ausgerüstet", "Nessuna carta equipaggiata"},
        {"No card effect active", "Aucun effet actif", "Kein Effekt aktiv", "Nessun effetto attivo"},
        {"CARD PICK PENDING", "CARTE EN ATTENTE", "KARTENWAHL OFFEN", "SCELTA CARTA IN ATTESA"},
        {"READY", "PRÊT", "BEREIT", "PRONTO"},
        {"ARMED", "ARMÉ", "SCHARF", "ARMATO"},
        {"ACTIVE", "ACTIF", "AKTIV", "ATTIVO"},
        {"WAIT", "ATTENTE", "WARTEN", "ATTESA"},
        {"NOW", "MAINTENANT", "JETZT", "ORA"},
        {"MAX", "MAX", "MAX", "MAX"},
        {"GET READY", "PRÉPAREZ-VOUS", "BEREIT MACHEN", "PREPARATI"},
        {"FINISHING RACE", "FIN DE COURSE", "RENNEN ENDET", "FINE GARA"},
        {"RACE COMPLETE", "COURSE TERMINÉE", "RENNEN BEENDET", "GARA COMPLETATA"},
        {"RACE WINNER", "VAINQUEUR", "RENNSIEGER", "VINCITORE"},
        {"CHAMPIONSHIP STANDINGS", "CLASSEMENT", "MEISTERSCHAFT", "CLASSIFICA"},
        {"FINAL CHAMPIONSHIP COMPLETE", "CHAMPIONNAT TERMINÉ", "MEISTERSCHAFT BEENDET", "CAMPIONATO COMPLETATO"},
        {"CONTINUE", "CONTINUER", "FORTSETZEN", "CONTINUA"},
        {"CHAMPION", "CHAMPION", "MEISTER", "CAMPIONE"},
        {"CHAMPIONSHIP LOST", "CHAMPIONNAT PERDU", "MEISTERSCHAFT VERLOREN", "CAMPIONATO PERSO"},
        {"NEW RUN", "NOUVELLE PARTIE", "NEUER LAUF", "NUOVA PARTITA"},
        {"RESET", "RÉINITIALISER", "ZURÜCKSETZEN", "RIPRISTINA"},
        {"RACE OVER", "COURSE TERMINÉE", "RENNEN VORBEI", "GARA FINITA"},
        {"NO FINISHER", "AUCUN ARRIVANT", "NIEMAND IM ZIEL", "NESSUN ARRIVO"},
        {"FIN", "ARRIVÉE", "ZIEL", "ARRIVO"},
        {"OUT", "ÉLIMINÉ", "RAUS", "FUORI"},
        {"Free practice", "Essais libres", "Freies Training", "Prove libere"},
        {"Standings updated. Next circuit in a moment.", "Classement mis à jour. Prochain circuit sous peu.", "Tabelle aktualisiert. Nächste Strecke folgt.", "Classifica aggiornata. Prossimo circuito a breve."},
        {"Route missing.", "Trajectoire absente.", "Route fehlt.", "Percorso assente."},
        {"Race checkpoints unavailable", "Points de contrôle indisponibles", "Kontrollpunkte fehlen", "Checkpoint non disponibili"},
        {"PREVIEW", "APERÇU", "VORSCHAU", "ANTEPRIMA"},
        {"ON", "ACTIF", "AN", "ATTIVO"},
        {"MAIN MENU", "MENU PRINCIPAL", "HAUPTMENÜ", "MENU PRINCIPALE"},
        {"APPLY & RESTART", "APPLIQUER ET RELANCER", "ANWENDEN & NEUSTART", "APPLICA E RIAVVIA"},
        {"sandbox tuning", "réglages du bac à sable", "Testbereich-Tuning", "regolazione sandbox"},
        {"control", "contrôle", "Steuerung", "controllo"},
        {"weather", "météo", "Wetter", "meteo"},
        {"map", "circuit", "Strecke", "mappa"},
        {"cars", "voitures", "Autos", "auto"},
        {"horsepower", "puissance", "Leistung", "potenza"},
        {"brake", "frein", "Bremse", "freno"},
        {"steering", "direction", "Lenkung", "sterzo"},
        {"grip", "adhérence", "Grip", "aderenza"},
        {"mass", "masse", "Masse", "massa"},
        {"Driver", "Pilote", "Fahrer", "Pilota"},
        {"Tuning", "Améliorations", "Tuning", "Migliorie"},
        {"Technique", "Technique", "Technik", "Tecnica"},
        {"Powerup", "Bonus", "Power-up", "Potenziamento"},
        {"Revenge", "Vengeance", "Rache", "Vendetta"},
        {"Sunny", "Soleil", "Sonnig", "Sole"},
        {"Rain", "Pluie", "Regen", "Pioggia"},
        {"Snow", "Neige", "Schnee", "Neve"},
        {"sunny", "soleil", "sonnig", "sole"},
        {"raining", "pluie", "Regen", "pioggia"},
        {"snowing", "neige", "Schnee", "neve"},
        {"Automatic", "Automatique", "Automatisch", "Automatico"},
        {"Manual", "Manuel", "Manuell", "Manuale"},
        {"Overtake", "Dépassement", "Überholen", "Sorpasso"},
        {"Fastest lap", "Meilleur tour", "Schnellste Runde", "Giro veloce"},
        {"Push off-road", "Rival hors piste", "Rivale abgedrängt", "Rivale fuori pista"},
        {"Drift", "Dérive", "Drift", "Derapata"},
        {"Finish", "Arrivée", "Ziel", "Arrivo"}
    };

    private static final String[][] CARD_TITLES = {
        {"Club Tune", "Réglage Club", "Club-Tuning", "Assetto Club"},
        {"Lightweight Tune", "Réglage Léger", "Leichtbau-Tuning", "Assetto Leggero"},
        {"Streamline Kit", "Kit Aérodynamique", "Aero-Kit", "Kit Aerodinamico"},
        {"Short-Ratio Gearbox", "Boîte Courte", "Kurzes Getriebe", "Cambio Corto"},
        {"Carbon Panels", "Panneaux Carbone", "Carbon-Paneele", "Pannelli in Carbonio"},
        {"Race Tune", "Réglage Course", "Renn-Tuning", "Assetto Gara"},
        {"Ballast Powertrain", "Moteur Lesté", "Ballast-Antrieb", "Motore Zavorrato"},
        {"Le Mans Body", "Carrosserie Le Mans", "Le-Mans-Karosserie", "Carrozzeria Le Mans"},
        {"Drift Differential", "Différentiel Drift", "Drift-Differenzial", "Differenziale Drift"},
        {"Carbon Monocoque", "Monocoque Carbone", "Carbon-Monocoque", "Monoscocca in Carbonio"},
        {"Aero Prototype", "Prototype Aéro", "Aero-Prototyp", "Prototipo Aero"},
        {"Ground Effect", "Effet de Sol", "Bodeneffekt", "Effetto Suolo"},
        {"Velocity Shell", "Carrosserie Véloce", "Tempo-Hülle", "Guscio Veloce"},
        {"Torque Vectoring", "Vectorisation du Couple", "Drehmomentlenkung", "Controllo di Coppia"},
        {"Graphene Chassis", "Châssis Graphène", "Graphen-Chassis", "Telaio in Grafene"},
        {"Corner Exit", "Sortie de Virage", "Kurvenausgang", "Uscita di Curva"},
        {"Draft Hunter", "Chasseur d'Aspiration", "Windschattenjäger", "Cacciatore di Scia"},
        {"Clean Momentum", "Élan Propre", "Sauberer Schwung", "Slancio Pulito"},
        {"Recovery Launch", "Relance", "Rückkehr-Schub", "Rilancio"},
        {"Drift Slingshot", "Fronde de Drift", "Drift-Katapult", "Fionda Drift"},
        {"Slipstream Slingshot", "Fronde d'Aspiration", "Windschatten-Katapult", "Fionda di Scia"},
        {"Overtake Surge", "Poussée de Dépassement", "Überholschub", "Spinta Sorpasso"},
        {"Apex Slingshot", "Fronde de Corde", "Scheitel-Katapult", "Fionda al Punto di Corda"},
        {"Perfect Lap", "Tour Parfait", "Perfekte Runde", "Giro Perfetto"},
        {"Racecraft Mastery", "Maîtrise de Course", "Rennkunst", "Maestria di Gara"},
        {"Underdog Instinct", "Instinct d'Outsider", "Außenseiterinstinkt", "Istinto Sfavorito"},
        {"Comeback Drive", "Remontée", "Comeback-Antrieb", "Rimonta"},
        {"Last Place Fury", "Fureur du Dernier", "Schlusslicht-Wut", "Furia dell'Ultimo"},
        {"Close Quarters", "Corps à Corps", "Nahkampf", "Corpo a Corpo"},
        {"Pack Racer", "Pilote de Peloton", "Pulkfahrer", "Pilota di Gruppo"},
        {"Traffic Dominance", "Domination du Trafic", "Verkehrsdominanz", "Dominio del Traffico"},
        {"Nitro Pulse", "Impulsion Nitro", "Nitro-Impuls", "Impulso Nitro"},
        {"Mirror Duo", "Duo Miroir", "Spiegel-Duo", "Duo Specchio"},
        {"Grip Fan", "Ventilateur d'Appui", "Grip-Lüfter", "Ventola di Aderenza"},
        {"Ghost Cloak", "Voile Fantôme", "Geistermantel", "Manto Fantasma"},
        {"Lucky Spark", "Étincelle Chanceuse", "Glücksfunke", "Scintilla Fortunata"},
        {"Impact Reversal", "Renvoi d'Impact", "Aufprall-Umkehr", "Inversione d'Impatto"},
        {"Draft Magnet", "Aimant d'Aspiration", "Windschattenmagnet", "Magnete di Scia"},
        {"Position Hijack", "Vol de Position", "Positionsraub", "Furto di Posizione"},
        {"Redline Hex", "Malédiction Rouge", "Drehzahlfluch", "Maledizione Rossa"},
        {"Phase Shield", "Bouclier de Phase", "Phasenschild", "Scudo di Fase"},
        {"Rocket Exhaust", "Échappement Fusée", "Raketen-Auspuff", "Scarico a Razzo"},
        {"Mirror Trio", "Trio Miroir", "Spiegel-Trio", "Trio Specchio"},
        {"Phantom Cloak", "Voile Spectral", "Phantommantel", "Manto Spettrale"},
        {"Chaos Relay", "Relais du Chaos", "Chaos-Relais", "Relè del Caos"},
        {"Gravity Well", "Puits de Gravité", "Gravitationsfeld", "Pozzo Gravitazionale"},
        {"Mirror Quartet", "Quatuor Miroir", "Spiegel-Quartett", "Quartetto Specchio"},
        {"Hyperdrive", "Hyperpropulsion", "Hyperantrieb", "Iperguida"},
        {"Void Cloak", "Voile du Néant", "Leerenmantel", "Manto del Vuoto"},
        {"Wildcard Core", "Cœur Joker", "Joker-Kern", "Nucleo Jolly"},
        {"Crown Breaker", "Briseur de Couronne", "Kronenbrecher", "Spezzacorona"},
        {"Vendetta Hook", "Crochet de Vendetta", "Rachehaken", "Gancio Vendetta"},
        {"Repulsor Surge", "Onde Répulsive", "Repulsorwelle", "Ondata Repulsiva"},
        {"Tar Tether", "Longe de Goudron", "Teerfessel", "Legame di Catrame"},
        {"EMP Snare", "Piège IEM", "EMP-Falle", "Trappola EMP"},
        {"Void Anchor", "Ancre du Néant", "Leerenanker", "Ancora del Vuoto"},
        {"Blind Hex", "Malédiction Aveugle", "Blindfluch", "Maledizione Cieca"},
        {"Burden Hex", "Malédiction du Fardeau", "Lastfluch", "Maledizione del Peso"},
        {"Doom Hex", "Malédiction Fatale", "Verdammnisfluch", "Maledizione Fatale"},
        {"Loaded Grudge", "Rancune Chargée", "Geladener Groll", "Rancore Carico"},
        {"Chaos Retort", "Riposte du Chaos", "Chaos-Konterschlag", "Replica del Caos"},
        {"Fate's Revenge", "Vengeance du Destin", "Rache des Schicksals", "Vendetta del Destino"}
    };

    private static final String[][] CARD_DESCRIPTIONS = {
        {"A dependable first race setup with more power, speed and tire grip.", "Un réglage fiable offrant plus de puissance, vitesse et adhérence.", "Ein verlässliches Setup mit mehr Leistung, Tempo und Grip.", "Un assetto affidabile con più potenza, velocità e aderenza."},
        {"A stripped chassis accelerates and changes direction quickly, but gives up tire stability.", "Un châssis allégé accélère et tourne vite, au prix de la stabilité.", "Ein leichtes Chassis beschleunigt und lenkt schnell, verliert aber Stabilität.", "Un telaio leggero accelera e gira rapidamente, sacrificando stabilità."},
        {"An aerodynamically efficient body carries speed on open road at the cost of cornering confidence.", "Une carrosserie efficace gagne en ligne droite mais rassure moins en virage.", "Effiziente Aerodynamik bringt Tempo auf Geraden, aber weniger Kurvensicherheit.", "Una carrozzeria efficiente guadagna velocità, ma riduce la sicurezza in curva."},
        {"Close gearing launches hard between corners but reaches its limit earlier on long straights.", "Les rapports courts relancent fort mais limitent les longues lignes droites.", "Kurze Gänge ziehen stark, erreichen auf langen Geraden aber früher ihr Limit.", "Rapporti corti spingono forte, ma limitano i lunghi rettilinei."},
        {"Light body panels improve acceleration, response and aero efficiency.", "Des panneaux légers améliorent accélération, réponse et aérodynamique.", "Leichte Paneele verbessern Beschleunigung, Reaktion und Aero.", "Pannelli leggeri migliorano accelerazione, risposta e aerodinamica."},
        {"Sharper race hardware combines sustained power with high-speed stability.", "Du matériel de course associe puissance durable et stabilité rapide.", "Renntechnik verbindet dauerhafte Leistung mit Hochgeschwindigkeitsstabilität.", "Componenti da gara uniscono potenza costante e stabilità ad alta velocità."},
        {"A reinforced, heavier car carries extra power and grip so contact no longer ruins its pace.", "Une voiture renforcée gagne puissance et adhérence pour résister aux contacts.", "Ein schweres, verstärktes Auto hält mit mehr Leistung und Grip sein Tempo.", "Un'auto rinforzata usa più potenza e aderenza per resistere ai contatti."},
        {"Exceptional aero efficiency rewards committed high-speed driving without sacrificing basic stability.", "Une excellente aérodynamique récompense la vitesse sans perdre la stabilité.", "Starke Aero belohnt hohes Tempo ohne die Grundstabilität zu opfern.", "Un'aerodinamica eccellente premia la velocità senza perdere stabilità."},
        {"An aggressive differential makes sustained rotation easy and straightens with strong drive.", "Un différentiel agressif facilite les longues dérives et les sorties puissantes.", "Ein aggressives Differenzial erleichtert lange Drifts und starke Ausgänge.", "Un differenziale aggressivo facilita derapate lunghe e uscite potenti."},
        {"A rigid carbon cell cuts inertia while sharpening power delivery, aero and response.", "La cellule carbone réduit l'inertie et améliore puissance, aéro et réponse.", "Die Carbonzelle senkt Trägheit und verbessert Leistung, Aero und Reaktion.", "La cellula in carbonio riduce l'inerzia e migliora potenza, aero e risposta."},
        {"An aerodynamically efficient body and high-speed downforce turn open road into a decisive advantage.", "Une carrosserie efficace transforme les portions rapides en avantage décisif.", "Effiziente Aero macht schnelle Abschnitte zum entscheidenden Vorteil.", "Una carrozzeria efficiente rende decisivi i tratti veloci."},
        {"A sealed floor creates exceptional cornering force while adding weight and aerodynamic resistance.", "Un fond scellé offre un appui extrême mais ajoute poids et résistance.", "Ein versiegelter Unterboden erzeugt enormen Kurvengrip, aber mehr Gewicht.", "Un fondo sigillato crea enorme aderenza in curva, ma aggiunge peso."},
        {"A radical long-tail body maximizes aero efficiency for huge straight-line pace with modest cornering support.", "Une longue carrosserie maximise la vitesse en ligne droite, avec peu d'appui.", "Ein Langheck maximiert Geradentempo bei mäßiger Kurvenunterstützung.", "Una coda lunga massimizza la velocità sul dritto con poco supporto in curva."},
        {"Active torque distribution rotates the car decisively through corners while preserving competitive speed and contact strength.", "La répartition active du couple améliore la rotation sans perdre vitesse ni force.", "Aktive Drehmomentverteilung verbessert Kurvenlage, Tempo und Kontaktstärke.", "La coppia attiva migliora la rotazione senza perdere velocità o forza."},
        {"An ultralight structure delivers extreme acceleration, aero efficiency and precise handling.", "Une structure ultralégère offre accélération extrême et conduite précise.", "Ultraleichtbau liefert extreme Beschleunigung, Aero und Präzision.", "Una struttura ultraleggera offre accelerazione estrema e guida precisa."},
        {"Leaving an on-road corner under power creates a short launch onto the next section.", "Sortir d'un virage en accélérant déclenche une brève poussée.", "Ein Kurvenausgang unter Last erzeugt einen kurzen Schub.", "Uscire da una curva accelerando genera una breve spinta."},
        {"Finds a rival's wake sooner and turns close following into useful speed.", "Trouve plus tôt l'aspiration et transforme le suivi en vitesse.", "Findet Windschatten früher und macht dichtes Folgen schneller.", "Trova prima la scia e trasforma la vicinanza in velocità."},
        {"Continuous on-road driving builds a speed advantage that is lost by leaving the circuit.", "Rester en piste accumule un avantage perdu en sortant.", "Fahren auf der Strecke baut Tempo auf, das abseits verloren geht.", "Restare in pista accumula velocità, persa uscendo dal tracciato."},
        {"A quick legitimate return to the road restores traction and accelerates back into the race.", "Un retour propre en piste restaure adhérence et accélération.", "Eine schnelle Rückkehr stellt Grip und Beschleunigung wieder her.", "Un rapido rientro ripristina aderenza e accelerazione."},
        {"Sustained on-road slip stores energy and releases it when the car straightens.", "Une dérive prolongée stocke de l'énergie libérée en ligne droite.", "Langer Drift speichert Energie und setzt sie beim Geradestellen frei.", "Una derapata prolungata accumula energia rilasciata sul dritto."},
        {"Charges in another car's wake and launches when you pull out to pass.", "Se charge dans l'aspiration et propulse lors du dépassement.", "Lädt im Windschatten und beschleunigt beim Ausscheren.", "Si carica in scia e spinge quando esci per sorpassare."},
        {"Every gained race position immediately provides power to complete the move.", "Chaque position gagnée donne de la puissance pour finir la manœuvre.", "Jede gewonnene Position liefert sofort Leistung für das Manöver.", "Ogni posizione guadagnata fornisce potenza per completare il sorpasso."},
        {"Loads energy through a fast on-road corner and releases it as the road straightens.", "Charge dans un virage rapide et libère l'énergie à la sortie.", "Lädt in schnellen Kurven Energie und setzt sie auf der Geraden frei.", "Carica energia in una curva veloce e la rilascia in uscita."},
        {"Clean speed, accurate corner exits and uninterrupted momentum compound throughout the lap.", "Vitesse propre et bonnes sorties se cumulent pendant le tour.", "Sauberes Tempo und gute Ausgänge verstärken sich über die Runde.", "Velocità pulita e buone uscite si accumulano durante il giro."},
        {"Drafting, corner exits and overtakes each trigger a powerful race-winning response.", "Aspiration, sorties et dépassements déclenchent de fortes poussées.", "Windschatten, Ausgänge und Überholen lösen starke Schübe aus.", "Scia, uscite e sorpassi attivano potenti spinte."},
        {"Reads the field and gains performance as the car falls back, reaching full strength in last place.", "Les performances augmentent en reculant, au maximum en dernière place.", "Die Leistung steigt weiter hinten und erreicht am Ende ihr Maximum.", "Le prestazioni aumentano arretrando, fino al massimo in ultima posizione."},
        {"Raises the car's pace whenever a rival is nearby, helping attacks and defensive runs.", "Augmente le rythme près des rivaux pour attaquer et défendre.", "Erhöht das Tempo bei nahen Rivalen für Angriff und Verteidigung.", "Aumenta il ritmo con rivali vicini per attaccare e difendere."},
        {"Kicks the car forward when open road invites a nitro burst.", "Propulse la voiture sur une ligne droite dégagée.", "Schiebt das Auto auf freier Geraden kräftig nach vorn.", "Spinge l'auto in avanti su un rettilineo libero."},
        {"Open road creates a second physical copy that races beside the original.", "Une route libre crée une copie physique à côté de l'original.", "Freie Strecke erzeugt eine physische Kopie neben dem Original.", "La strada libera crea una copia fisica accanto all'originale."},
        {"A glowing underbody fan pins the car down as a demanding corner arrives.", "Un ventilateur lumineux plaque la voiture avant un virage difficile.", "Ein leuchtender Unterbodenlüfter erhöht den Grip vor harten Kurven.", "Una ventola luminosa incolla l'auto prima di una curva difficile."},
        {"The car phases out when traffic is nearby, becoming invisible and intangible to rivals.", "La voiture devient invisible et intangible près du trafic.", "Bei Verkehr wird das Auto unsichtbar und für Rivalen unberührbar.", "Con traffico vicino, l'auto diventa invisibile e intangibile."},
        {"Prepares a random Tier 1 Powerup and copies its real trigger, effect and cooldown.", "Prépare un Bonus T1 aléatoire avec son déclencheur et son effet.", "Bereitet ein zufälliges T1-Power-up samt Auslöser und Effekt vor.", "Prepara un Potenziamento T1 casuale con attivazione ed effetto."},
        {"Prepares a random Tier 2 Powerup and copies its real trigger, effect and cooldown.", "Prépare un Bonus T2 aléatoire avec son déclencheur et son effet.", "Bereitet ein zufälliges T2-Power-up samt Auslöser und Effekt vor.", "Prepara un Potenziamento T2 casuale con attivazione ed effetto."},
        {"Prepares a random Tier 3 Powerup and copies its real trigger, effect and cooldown.", "Prépare un Bonus T3 aléatoire avec son déclencheur et son effet.", "Bereitet ein zufälliges T3-Power-up samt Auslöser und Effekt vor.", "Prepara un Potenziamento T3 casuale con attivazione ed effetto."},
        {"A qualified rival hit arms a counter that throws away the next car to strike you.", "Un choc rival arme un renvoi contre la prochaine voiture.", "Ein Rivaltreffer lädt einen Konter gegen das nächste Auto.", "Un colpo rivale arma un contrattacco contro la prossima auto."},
        {"A qualified rival hit arms a short pulsing field that forces nearby cars toward the outside.", "Un choc arme un champ qui repousse brièvement les voitures proches.", "Ein Treffer lädt ein Feld, das nahe Autos kurz nach außen drückt.", "Un colpo arma un campo che spinge fuori le auto vicine."},
        {"A qualified hit marks its offender. After charging, it exchanges positions only while they are ahead.", "Marque l'agresseur et échange les positions seulement s'il est devant.", "Markiert den Angreifer und tauscht nur, wenn er voraus ist.", "Segna l'aggressore e scambia posizione solo se è davanti."},
        {"A qualified hit curses its offender's throttle, forcing them to commit through whatever comes next.", "Maudit l'accélérateur de l'agresseur et le force à fond.", "Verflucht das Gas des Angreifers und zwingt ihn auf Vollgas.", "Maledice l'acceleratore dell'aggressore e lo forza al massimo."},
        {"An energy shell forms when traffic closes in, absorbing frontal recoil.", "Une coque d'énergie absorbe les chocs frontaux dans le trafic.", "Eine Energiehülle fängt bei Verkehr frontale Rückstöße ab.", "Un guscio energetico assorbe gli urti frontali nel traffico."},
        {"Twin exhaust rockets ignite on a clear straight for a forceful launch.", "Deux fusées d'échappement donnent une forte poussée en ligne droite.", "Zwei Auspuffraketen liefern auf freier Geraden starken Schub.", "Due razzi di scarico danno una forte spinta sul rettilineo."},
        {"Open road creates two physical copies spread across the track.", "Une route libre crée deux copies physiques sur la piste.", "Freie Strecke erzeugt zwei physische Kopien auf der Fahrbahn.", "La strada libera crea due copie fisiche sulla pista."},
        {"Open road creates three physical copies spread across the track.", "Une route libre crée trois copies physiques sur la piste.", "Freie Strecke erzeugt drei physische Kopien auf der Fahrbahn.", "La strada libera crea tre copie fisiche sulla pista."},
        {"An improved phase field hides the car and prevents rivals from making contact for longer.", "Un champ amélioré cache la voiture et bloque les contacts plus longtemps.", "Ein besseres Phasenfeld versteckt das Auto länger vor Kontakten.", "Un campo migliorato nasconde l'auto e impedisce contatti più a lungo."},
        {"A visible ground field forms in corners or close traffic for extreme stability.", "Un champ visible apporte une stabilité extrême en virage ou trafic.", "Ein sichtbares Feld sorgt in Kurven oder Verkehr für extreme Stabilität.", "Un campo visibile dà stabilità estrema in curva o nel traffico."},
        {"Open road triggers an extreme launch and turns the car into a visible streak.", "Une route libre déclenche une poussée extrême et une traînée visible.", "Freie Strecke löst extremen Schub und eine sichtbare Spur aus.", "La strada libera attiva una spinta estrema e una scia visibile."},
        {"A championship phase system removes the car from sight and contact for an extended attack window.", "Un système de phase cache la voiture et bloque les contacts longtemps.", "Ein Phasensystem entfernt das Auto länger aus Sicht und Kontakt.", "Un sistema di fase nasconde l'auto e impedisce contatti a lungo."},
        {"A qualified rival hit arms a brutal hunt for the offender responsible.", "Un choc rival arme une chasse brutale contre l'agresseur.", "Ein Rivaltreffer startet eine brutale Jagd auf den Angreifer.", "Un colpo rivale arma una caccia brutale all'aggressore."},
        {"A qualified hit marks its offender. After charging, the hook pulls you directly back toward them.", "Marque l'agresseur puis vous attire directement vers lui.", "Markiert den Angreifer und zieht dich direkt zu ihm zurück.", "Segna l'aggressore e ti trascina direttamente verso di lui."},
        {"A qualified rival hit arms a wide high-energy field that clears space for your comeback.", "Un choc arme un large champ qui libère la voie.", "Ein Rivaltreffer lädt ein breites Feld, das Platz schafft.", "Un colpo arma un ampio campo che libera spazio."},
        {"Throws a sticky tether at the rival who hit you and strips all tire traction.", "Lance une longe collante qui supprime toute adhérence de l'agresseur.", "Wirft eine klebrige Fessel und nimmt dem Angreifer jeden Grip.", "Lancia un legame appiccicoso che azzera l'aderenza dell'aggressore."},
        {"Launches a disruptive snare that forces the rival responsible for hitting you to brake without reversing.", "Force l'agresseur à freiner sans passer en marche arrière.", "Zwingt den Angreifer zum Bremsen, ohne rückwärts zu fahren.", "Costringe l'aggressore a frenare senza andare in retromarcia."},
        {"Hurls a heavy energy anchor that forces the rival responsible for hitting you to brake without reversing.", "Une ancre d'énergie force l'agresseur à freiner sans reculer.", "Ein Energieanker zwingt den Angreifer zum Bremsen ohne Rückwärtsfahrt.", "Un'ancora energetica costringe l'aggressore a frenare senza retrocedere."},
        {"Curses the rival who hit you until that offender collides with another car.", "Aveugle l'agresseur jusqu'à sa prochaine collision.", "Blendet den Angreifer bis zu seiner nächsten Kollision.", "Acceca l'aggressore fino alla prossima collisione."},
        {"Chains the rival who hit you to a heavier, blinded car until its next collision.", "Rend l'agresseur lourd et aveugle jusqu'à sa prochaine collision.", "Macht den Angreifer bis zur nächsten Kollision schwer und blind.", "Rende l'aggressore pesante e cieco fino alla prossima collisione."},
        {"Crushes the rival who hit you with blindness, extreme weight, and reduced grip until its next collision.", "Impose aveuglement, poids extrême et faible adhérence jusqu'au choc.", "Belegt den Angreifer bis zum nächsten Treffer mit Blindheit, Masse und wenig Grip.", "Impone cecità, peso estremo e poca aderenza fino al prossimo urto."},
        {"A rival hit executes a random Tier 1 Revenge card, then prepares a different retaliation.", "Un choc exécute une Vengeance T1 aléatoire puis en prépare une autre.", "Ein Treffer führt eine zufällige T1-Rache aus und bereitet die nächste vor.", "Un colpo esegue una Vendetta T1 casuale e ne prepara un'altra."},
        {"A rival hit executes a random Tier 2 Revenge card, then prepares a different retaliation.", "Un choc exécute une Vengeance T2 aléatoire puis en prépare une autre.", "Ein Treffer führt eine zufällige T2-Rache aus und bereitet die nächste vor.", "Un colpo esegue una Vendetta T2 casuale e ne prepara un'altra."},
        {"A rival hit executes a random Tier 3 Revenge card, then prepares a different retaliation.", "Un choc exécute une Vengeance T3 aléatoire puis en prépare une autre.", "Ein Treffer führt eine zufällige T3-Rache aus und bereitet die nächste vor.", "Un colpo esegue una Vendetta T3 casuale e ne prepara un'altra."}
    };

    private GameTextEuropeanCatalog() {
    }

    static Map<String, String> create(GameLanguage language) {
        int column = languageColumn(language);
        Map<String, String> text = new HashMap<String, String>();
        addRows(text, UI_TEXT, column);
        addRows(text, CARD_TITLES, column);
        addRows(text, CARD_DESCRIPTIONS, column);
        return Collections.unmodifiableMap(text);
    }

    static String translateDynamic(GameLanguage language, String english) {
        String translated = translatePrefix(language, english, "Theme: ", "Thème: ", "Thema: ", "Tema: ");
        if (translated != null) return translated;
        translated = translatePrefix(language, english, "Tier ", "Niveau ", "Stufe ", "Livello ");
        if (translated != null) return translated;
        translated = translatePrefix(language, english, "Level ", "Niveau ", "Stufe ", "Livello ");
        if (translated != null) return translated;
        translated = translatePrefix(language, english, "Circuit ", "Circuit ", "Strecke ", "Circuito ");
        if (translated != null) return translated;
        translated = translatePrefix(language, english, "Map ", "Circuit ", "Strecke ", "Mappa ");
        if (translated != null) return translated;
        translated = translatePrefix(language, english, "CHAMPIONSHIP ", "CHAMPIONNAT ", "MEISTERSCHAFT ", "CAMPIONATO ");
        if (translated != null) return translated;
        translated = translatePrefix(language, english, "Lap ", "Tour ", "Runde ", "Giro ");
        if (translated != null) return translated;
        translated = translatePrefix(language, english, "Cars ", "Voitures ", "Autos ", "Auto ");
        if (translated != null) return translated;
        translated = translatePrefix(language, english, "Car ", "Voiture ", "Auto ", "Auto ");
        if (translated != null) return translated;
        translated = translatePrefix(language, english, "Loading ", "Chargement ", "Laden: ", "Caricamento ");
        if (translated != null) return translated;
        translated = translatePrefix(language, english, "Preparing ", "Préparation ", "Vorbereiten: ", "Preparazione ");
        if (translated != null) return translated;
        translated = translatePrefix(language, english, "Finished #", "Arrivé #", "Ziel #", "Arrivato #");
        if (translated != null) return translated;
        translated = translatePrefix(language, english, "TV CAMERA: ", "CAMÉRA TV : ", "TV-KAMERA: ", "REGIA TV: ");
        if (translated != null) return translated;
        translated = translatePrefix(language, english, "CONTINUE ", "CONTINUER ", "FORTSETZEN ", "CONTINUA ");
        if (translated != null) return translated;
        translated = translatePrefix(language, english, "SKIP ", "PASSER ", "ÜBERSPRINGEN ", "SALTA ");
        if (translated != null) return translated;
        translated = translatePrefix(language, english, "Starts in ", "Départ dans ", "Start in ", "Inizio tra ");
        if (translated != null) return translated;
        translated = translatePrefix(language, english, "Finish closes in ", "Arrivée fermée dans ", "Ziel schließt in ", "Arrivo chiude tra ");
        if (translated != null) return translated;

        if (english.indexOf(" | Tier ") >= 0 || english.indexOf(" SLOTS") >= 0 || english.indexOf(" PAGE ") >= 0) {
            return replaceByLanguage(
                    language,
                    english,
                    new String[] {" | Tier ", " SLOTS", " PAGE "},
                    new String[][] {
                        {" | Niveau ", " EMPLACEMENTS", " PAGE "},
                        {" | Stufe ", " PLÄTZE", " SEITE "},
                        {" | Livello ", " SLOT", " PAGINA "}
                    });
        }
        int detailSeparator = english.indexOf(" - ");
        if (detailSeparator > 0) {
            return GameText.translate(language, english.substring(0, detailSeparator))
                    + " - "
                    + GameText.translate(language, english.substring(detailSeparator + 3));
        }
        int targetSeparator = english.lastIndexOf(" on ");
        if (targetSeparator > 0) {
            String connector = select(language, " sur ", " gegen ", " su ");
            return GameText.translate(language, english.substring(0, targetSeparator))
                    + connector
                    + english.substring(targetSeparator + 4);
        }
        if (english.indexOf(" SLAMMED ") >= 0
                || english.indexOf(" SHOVED ") >= 0
                || english.indexOf(" CLIPPED ") >= 0
                || english.indexOf(" USED ") >= 0
                || english.indexOf(" PASSED ") >= 0) {
            return translateIncident(language, english);
        }
        if (looksLikeCardEffect(english)) {
            return translateCardEffect(language, english);
        }
        return english;
    }

    private static String translateIncident(GameLanguage language, String text) {
        if (language == GameLanguage.FRENCH) {
            return text.replace("HIT: ", "CHOC : ").replace("REVENGE: ", "VENGEANCE : ")
                    .replace("PASS: ", "DÉPASSEMENT : ").replace(" SLAMMED ", " A PERCUTÉ ")
                    .replace(" SHOVED ", " A POUSSÉ ").replace(" CLIPPED ", " A TOUCHÉ ")
                    .replace(" HIT ", " A HEURTÉ ").replace(" USED ", " A UTILISÉ ")
                    .replace(" ON ", " SUR ").replace(" PASSED ", " A DÉPASSÉ ")
                    .replace(" POSITIONS GAINED", " POSITIONS GAGNÉES")
                    .replace(" POSITION GAINED", " POSITION GAGNÉE");
        }
        if (language == GameLanguage.GERMAN) {
            return text.replace("HIT: ", "TREFFER: ").replace("REVENGE: ", "RACHE: ")
                    .replace("PASS: ", "ÜBERHOLEN: ").replace(" SLAMMED ", " RAMMTE ")
                    .replace(" SHOVED ", " SCHOB ").replace(" CLIPPED ", " STREIFTE ")
                    .replace(" HIT ", " TRAF ").replace(" USED ", " NUTZTE ")
                    .replace(" ON ", " GEGEN ").replace(" PASSED ", " ÜBERHOLTE ")
                    .replace(" POSITIONS GAINED", " PLÄTZE GEWONNEN")
                    .replace(" POSITION GAINED", " PLATZ GEWONNEN");
        }
        return text.replace("HIT: ", "URTO: ").replace("REVENGE: ", "VENDETTA: ")
                .replace("PASS: ", "SORPASSO: ").replace(" SLAMMED ", " HA SPERONATO ")
                .replace(" SHOVED ", " HA SPINTO ").replace(" CLIPPED ", " HA SFIORATO ")
                .replace(" HIT ", " HA COLPITO ").replace(" USED ", " HA USATO ")
                .replace(" ON ", " SU ").replace(" PASSED ", " HA SORPASSATO ")
                .replace(" POSITIONS GAINED", " POSIZIONI GUADAGNATE")
                .replace(" POSITION GAINED", " POSIZIONE GUADAGNATA");
    }

    private static String translateCardEffect(GameLanguage language, String text) {
        String[] source = {
            "Clear straight", "Fast corner exit", "Corner exit", "Corner ahead", "Corner or traffic",
            "Traffic or corner", "Nearby rival on straight", "Nearby rival", "Rival hit", "Hit taken",
            "Each activation", "Stay on-road", "Safe re-entry", "Drift exit", "Leave draft",
            "Race events", "Lower position", "Clean lap", "Overtake", "Powerup", "Revenge", "Draft",
            "draft", "Power", "power", "Speed", "speed", "Grip", "grip", "Steering", "steering",
            "Mass", "mass", "Stronger hits", "Freer rear while drifting", "reach and boost", "up to",
            "bursts", "Cooldown after effect", "Cooldown", "random Tier", "full throttle", "full brake",
            "after 3s", "swap with", "pull to", "over 5s", "reflect impact", "wide outward field",
            "outward field", "until collision", "blind", "random", "next hit", "Explosive ram", "Recoil",
            "Push", "offender", "if ahead", "cars for", "car for", "Rivals pass through you", "invisible",
            "launch", "shield"
        };
        String[][] target = {
            {"Ligne droite libre", "Sortie rapide", "Sortie de virage", "Virage proche", "Virage ou trafic", "Trafic ou virage", "Rival proche en ligne droite", "Rival proche", "Choc rival", "Choc reçu", "Chaque activation", "Rester en piste", "Retour sûr", "Sortie de dérive", "Quitter l'aspiration", "Événements de course", "Position basse", "Tour propre", "Dépassement", "Bonus", "Vengeance", "Aspiration", "aspiration", "Puissance", "puissance", "Vitesse", "vitesse", "Adhérence", "adhérence", "Direction", "direction", "Masse", "masse", "Chocs renforcés", "Arrière plus libre en dérive", "portée et poussée", "jusqu'à", "par rafales", "Recharge après effet", "Recharge", "Niveau aléatoire", "plein gaz", "frein maximal", "après 3 s", "échanger avec", "attirer vers", "sur 5 s", "renvoyer l'impact", "large champ répulsif", "champ répulsif", "jusqu'au choc", "aveugle", "aléatoire", "prochain choc", "Bélier explosif", "Recul", "Poussée", "agresseur", "s'il est devant", "voitures pendant", "voiture pendant", "Les rivaux vous traversent", "invisible", "poussée", "bouclier"},
            {"Freie Gerade", "Schneller Kurvenausgang", "Kurvenausgang", "Kurve voraus", "Kurve oder Verkehr", "Verkehr oder Kurve", "Rivale auf Gerader", "Rivale in Nähe", "Rivaltreffer", "Treffer erhalten", "Jede Aktivierung", "Auf Strecke bleiben", "Sichere Rückkehr", "Driftausgang", "Windschatten verlassen", "Rennereignisse", "Hintere Position", "Saubere Runde", "Überholen", "Power-up", "Rache", "Windschatten", "Windschatten", "Leistung", "Leistung", "Tempo", "Tempo", "Grip", "Grip", "Lenkung", "Lenkung", "Masse", "Masse", "Stärkere Treffer", "Freieres Heck beim Driften", "Reichweite und Schub", "bis zu", "in Schüben", "Abklingzeit nach Effekt", "Abklingzeit", "zufällige Stufe", "Vollgas", "Vollbremsung", "nach 3 s", "tauschen mit", "ziehen zu", "über 5 s", "Aufprall zurückwerfen", "breites Abstoßfeld", "Abstoßfeld", "bis Kollision", "blind", "zufällig", "nächster Treffer", "Explosiver Rammstoß", "Rückstoß", "Schub", "Angreifer", "falls voraus", "Autos für", "Auto für", "Rivalen fahren durch dich", "unsichtbar", "Schub", "Schild"},
            {"Rettilineo libero", "Uscita veloce", "Uscita di curva", "Curva vicina", "Curva o traffico", "Traffico o curva", "Rivale vicino sul rettilineo", "Rivale vicino", "Colpo rivale", "Colpo subito", "Ogni attivazione", "Resta in pista", "Rientro sicuro", "Uscita derapata", "Esci dalla scia", "Eventi di gara", "Posizione arretrata", "Giro pulito", "Sorpasso", "Potenziamento", "Vendetta", "Scia", "scia", "Potenza", "potenza", "Velocità", "velocità", "Aderenza", "aderenza", "Sterzo", "sterzo", "Massa", "massa", "Urti più forti", "Retrotreno libero in derapata", "portata e spinta", "fino a", "a raffiche", "Ricarica dopo l'effetto", "Ricarica", "Livello casuale", "tutto gas", "frenata massima", "dopo 3 s", "scambia con", "trascina verso", "in 5 s", "riflette l'impatto", "ampio campo repulsivo", "campo repulsivo", "fino alla collisione", "cieco", "casuale", "prossimo colpo", "Ariete esplosivo", "Rinculo", "Spinta", "aggressore", "se davanti", "auto per", "auto per", "I rivali ti attraversano", "invisibile", "spinta", "scudo"}
        };
        String result = text;
        String[] replacements = target[languageColumn(language) - 1];
        for (int i = 0; i < source.length; i++) {
            result = result.replace(source[i], replacements[i]);
        }
        return result;
    }

    private static boolean looksLikeCardEffect(String text) {
        return text.indexOf('|') >= 0 || text.indexOf("->") >= 0 || text.indexOf(':') >= 0;
    }

    private static String translatePrefix(
            GameLanguage language,
            String text,
            String englishPrefix,
            String frenchPrefix,
            String germanPrefix,
            String italianPrefix) {
        if (!text.startsWith(englishPrefix)) {
            return null;
        }
        return select(language, frenchPrefix, germanPrefix, italianPrefix)
                + text.substring(englishPrefix.length());
    }

    private static String replaceByLanguage(
            GameLanguage language,
            String text,
            String[] source,
            String[][] replacements) {
        String result = text;
        String[] target = replacements[languageColumn(language) - 1];
        for (int i = 0; i < source.length; i++) {
            result = result.replace(source[i], target[i]);
        }
        return result;
    }

    private static String select(
            GameLanguage language,
            String french,
            String german,
            String italian) {
        switch (language) {
            case FRENCH:
                return french;
            case GERMAN:
                return german;
            case ITALIAN:
                return italian;
            default:
                return "";
        }
    }

    private static void addRows(Map<String, String> target, String[][] rows, int column) {
        for (String[] row : rows) {
            put(target, row[0], row[column]);
        }
    }

    private static int languageColumn(GameLanguage language) {
        switch (language) {
            case FRENCH:
                return 1;
            case GERMAN:
                return 2;
            case ITALIAN:
                return 3;
            default:
                throw new IllegalArgumentException("Unsupported European language: " + language);
        }
    }

    private static void put(Map<String, String> target, String english, String translated) {
        target.put(english, translated);
        String uppercaseEnglish = english.toUpperCase(Locale.ROOT);
        if (!uppercaseEnglish.equals(english)) {
            target.put(uppercaseEnglish, translated.toUpperCase(Locale.ROOT));
        }
    }
}
