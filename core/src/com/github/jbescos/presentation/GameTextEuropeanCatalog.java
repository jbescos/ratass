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
        {"CARD AVAILABILITY", "DISPONIBILITÉ DES CARTES", "KARTENVERFÜGBARKEIT", "DISPONIBILITÀ CARTE"},
        {"WEATHER", "MÉTÉO", "WETTER", "METEO"},
        {"CARD TIERS", "NIVEAUX DE CARTE", "KARTENSTUFEN", "LIVELLI CARTE"},
        {"Tier", "Niveau", "Stufe", "Livello"},
        {"Unlock lvl", "Niveau requis", "Freischaltstufe", "Livello sblocco"},
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
        {"EDITING", "MODIFICATION", "BEARBEITET", "MODIFICA"},
        {"COPY TO ALL", "COPIER À TOUS", "AUF ALLE KOPIEREN", "COPIA SU TUTTI"},
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
        {"CIRCUIT", "CIRCUIT", "STRECKE", "CIRCUITO"},
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
        {"sandbox settings", "réglages du bac à sable", "Testbereich-Einstellungen", "impostazioni sandbox"},
        {"sensors & route", "capteurs et trajectoire", "Sensoren & Ideallinie", "sensori e traiettoria"},
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
        {"Time Ripple", "Onde Temporelle", "Zeitwelle", "Onda Temporale"},
        {"Chrono Shift", "Décalage Chrono", "Chronoverschiebung", "Scatto Crono"},
        {"Temporal Dominion", "Domination Temporelle", "Zeitherrschaft", "Dominio Temporale"},
        {"Doubles local time for your car and quantum copies; movement and decisions run x2.", "Double le temps local de votre voiture et de ses copies quantiques : mouvement et décisions x2.", "Verdoppelt die lokale Zeit für dein Auto und Quantenkopien: Bewegung und Entscheidungen x2.", "Raddoppia il tempo locale dell'auto e delle copie quantistiche: movimento e decisioni x2."},
        {"Doubles local time for your car and quantum copies, recharging faster.", "Double le temps local de votre voiture et de ses copies quantiques, avec une recharge plus rapide.", "Verdoppelt die lokale Zeit für dein Auto und Quantenkopien mit schnellerer Aufladung.", "Raddoppia il tempo locale dell'auto e delle copie quantistiche, con ricarica più rapida."},
        {"Doubles local time for your entire quantum family with the fastest recharge.", "Double le temps local de toute votre famille quantique avec la recharge la plus rapide.", "Verdoppelt die lokale Zeit deiner gesamten Quantenfamilie mit der schnellsten Aufladung.", "Raddoppia il tempo locale dell'intera famiglia quantistica con la ricarica più rapida."},
        {"Automatic: local time x2 | 2s\nCooldown: 60s", "Automatique : temps local x2 | 2 s\nRecharge : 60 s", "Automatisch: lokale Zeit x2 | 2 s\nAbklingzeit: 60 s", "Automatico: tempo locale x2 | 2 s\nRicarica: 60 s"},
        {"Automatic: local time x2 | 2s\nCooldown: 40s", "Automatique : temps local x2 | 2 s\nRecharge : 40 s", "Automatisch: lokale Zeit x2 | 2 s\nAbklingzeit: 40 s", "Automatico: tempo locale x2 | 2 s\nRicarica: 40 s"},
        {"Automatic: local time x2 | 2s\nCooldown: 30s", "Automatique : temps local x2 | 2 s\nRecharge : 30 s", "Automatisch: lokale Zeit x2 | 2 s\nAbklingzeit: 30 s", "Automatico: tempo locale x2 | 2 s\nRicarica: 30 s"},
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
        {"Ballast Sprint", "Sprint Lesté", "Ballast-Sprint", "Sprint Zavorrato"},
        {"Reinforced Streamliner", "Profilé Renforcé", "Verstärkter Stromlinienwagen", "Filante Rinforzata"},
        {"Featherweight Drive", "Transmission Plume", "Federleichter Antrieb", "Trasmissione Piuma"},
        {"Track Wing", "Aileron Piste", "Streckenflügel", "Ala da Pista"},
        {"Grounded Aero", "Aéro Plaquée", "Boden-Aero", "Aero Incollata"},
        {"Light Compound", "Gomme Légère", "Leichte Mischung", "Mescola Leggera"},
        {"Agile Chassis", "Châssis Agile", "Agiles Chassis", "Telaio Agile"},
        {"Streamlined Chassis", "Châssis Profilé", "Stromlinien-Chassis", "Telaio Filante"},
        {"Aero Featherweight", "Plume Aéro", "Aero-Federgewicht", "Piuma Aero"},
        {"Reinforced Longtail", "Longue Queue Renforcée", "Verstärktes Langheck", "Coda Lunga Rinforzata"},
        {"Titanium Drive", "Transmission Titane", "Titan-Antrieb", "Trasmissione in Titanio"},
        {"Downforce Package", "Kit d'Appui", "Abtriebspaket", "Pacchetto Deportanza"},
        {"Grounded Downforce", "Appui Plaqué", "Bodenabtrieb", "Deportanza Incollata"},
        {"Magnesium Suspension", "Suspension Magnésium", "Magnesium-Fahrwerk", "Sospensioni in Magnesio"},
        {"Aero-Agile Chassis", "Châssis Aéro-Agile", "Aero-Agiles Chassis", "Telaio Aero-Agile"},
        {"Carbon Longtail", "Longue Queue Carbone", "Carbon-Langheck", "Coda Lunga in Carbonio"},
        {"Venturi Monocoque", "Monocoque Venturi", "Venturi-Monocoque", "Monoscocca Venturi"},
        {"Power Monocoque", "Monocoque Puissance", "Leistungs-Monocoque", "Monoscocca Potenza"},
        {"Titanium Skeleton", "Squelette Titane", "Titan-Skelett", "Scheletro in Titanio"},
        {"Hypercar Core", "Cœur Hypercar", "Hypercar-Kern", "Nucleo Hypercar"},
        {"Active Aero Shell", "Coque Aéro Active", "Aktive Aero-Hülle", "Guscio Aero Attivo"},
        {"Carbon Prototype", "Prototype Carbone", "Carbon-Prototyp", "Prototipo in Carbonio"},
        {"Track Vacuum", "Aspiration Piste", "Streckenvakuum", "Vuoto Pista"},
        {"Wing Car", "Voiture Ailée", "Flügelwagen", "Auto Alata"},
        {"Feather Ground", "Sol Plume", "Leichtboden", "Fondo Leggero"},
        {"Corner Focus", "Focus Virage", "Kurvenfokus", "Focus Curva"},
        {"Draft Focus", "Focus Aspiration", "Windschattenfokus", "Focus Scia"},
        {"Straight Focus", "Focus Ligne Droite", "Geradenfokus", "Focus Rettilineo"},
        {"Drift Focus", "Focus Dérive", "Driftfokus", "Focus Derapata"},
        {"Rally Focus", "Focus Rallye", "Rallyefokus", "Focus Rally"},
        {"Apex Focus", "Focus Corde", "Scheitelpunktfokus", "Focus Corda"},
        {"Sprint Focus", "Focus Sprint", "Sprintfokus", "Focus Sprint"},
        {"Slide Focus", "Focus Glisse", "Rutschfokus", "Focus Scivolata"},
        {"Traction Focus", "Focus Traction", "Traktionsfokus", "Focus Trazione"},
        {"Agility Focus", "Focus Agilité", "Agilitätsfokus", "Focus Agilità"},
        {"Corner Expert", "Expert Virage", "Kurvenexperte", "Esperto Curva"},
        {"Draft Expert", "Expert Aspiration", "Windschattenexperte", "Esperto Scia"},
        {"Straight Expert", "Expert Ligne Droite", "Geradenexperte", "Esperto Rettilineo"},
        {"Drift Expert", "Expert Dérive", "Driftexperte", "Esperto Derapata"},
        {"Rally Expert", "Expert Rallye", "Rallyeexperte", "Esperto Rally"},
        {"Apex Expert", "Expert Corde", "Scheitelpunktexperte", "Esperto Corda"},
        {"Sprint Expert", "Expert Sprint", "Sprint-Experte", "Esperto Sprint"},
        {"Slide Expert", "Expert Glisse", "Rutschexperte", "Esperto Scivolata"},
        {"Traction Expert", "Expert Traction", "Traktionsexperte", "Esperto Trazione"},
        {"Agility Expert", "Expert Agilité", "Agilitätsexperte", "Esperto Agilità"},
        {"Corner Master", "Maître du Virage", "Kurvenmeister", "Maestro Curva"},
        {"Draft Master", "Maître de l'Aspiration", "Windschattenmeister", "Maestro Scia"},
        {"Straight Master", "Maître de la Ligne Droite", "Geradenmeister", "Maestro Rettilineo"},
        {"Drift Master", "Maître de la Dérive", "Driftmeister", "Maestro Derapata"},
        {"Rally Master", "Maître du Rallye", "Rallyemeister", "Maestro Rally"},
        {"Apex Master", "Maître de la Corde", "Scheitelpunktmeister", "Maestro Corda"},
        {"Sprint Master", "Maître du Sprint", "Sprintmeister", "Maestro Sprint"},
        {"Slide Master", "Maître de la Glisse", "Rutschmeister", "Maestro Scivolata"},
        {"Traction Master", "Maître de la Traction", "Traktionsmeister", "Maestro Trazione"},
        {"Agility Master", "Maître de l'Agilité", "Agilitätsmeister", "Maestro Agilità"},
        {"Underdog Instinct", "Instinct d'Outsider", "Außenseiterinstinkt", "Istinto Sfavorito"},
        {"Comeback Drive", "Remontée", "Comeback-Antrieb", "Rimonta"},
        {"Last Place Fury", "Fureur du Dernier", "Schlusslicht-Wut", "Furia dell'Ultimo"},
        {"Close Quarters", "Corps à Corps", "Nahkampf", "Corpo a Corpo"},
        {"Pack Racer", "Pilote de Peloton", "Pulkfahrer", "Pilota di Gruppo"},
        {"Traffic Dominance", "Domination du Trafic", "Verkehrsdominanz", "Dominio del Traffico"},
        {"Nitro Pulse", "Impulsion Nitro", "Nitro-Impuls", "Impulso Nitro"},
        {"Ace Hotline", "Ligne de l'As", "Ass-Hotline", "Linea dell'Asso"},
        {"Quantum Duo", "Duo Quantique", "Quanten-Duo", "Duo Quantistico"},
        {"Grip Fan", "Ventilateur d'Appui", "Grip-Lüfter", "Ventola di Aderenza"},
        {"Ghost Cloak", "Voile Fantôme", "Geistermantel", "Manto Fantasma"},
        {"Lucky Spark", "Étincelle Chanceuse", "Glücksfunke", "Scintilla Fortunata"},
        {"Grudge Spark", "Étincelle de Rancune", "Grollfunke", "Scintilla di Rancore"},
        {"Rival hit: arm until next rival hit | Reflect impact", "Choc rival : armé jusqu'au prochain choc | Renvoyer l'impact", "Rivaltreffer: bis zum nächsten Treffer laden | Aufprall zurückwerfen", "Colpo rivale: armato fino al prossimo colpo | Riflette l'impatto"},
        {"Draft Magnet", "Aimant d'Aspiration", "Windschattenmagnet", "Magnete di Scia"},
        {"Position Hijack", "Vol de Position", "Positionsraub", "Furto di Posizione"},
        {"Redline Hex", "Malédiction Rouge", "Drehzahlfluch", "Maledizione Rossa"},
        {"Phase Shield", "Bouclier de Phase", "Phasenschild", "Scudo di Fase"},
        {"Rocket Exhaust", "Échappement Fusée", "Raketen-Auspuff", "Scarico a Razzo"},
        {"Priority Hotline", "Ligne Prioritaire", "Prioritäts-Hotline", "Linea Prioritaria"},
        {"Quantum Trio", "Trio Quantique", "Quanten-Trio", "Trio Quantistico"},
        {"Phantom Cloak", "Voile Spectral", "Phantommantel", "Manto Spettrale"},
        {"Chaos Relay", "Relais du Chaos", "Chaos-Relais", "Relè del Caos"},
        {"Vengeance Core", "Cœur de Vengeance", "Rachekern", "Nucleo di Vendetta"},
        {"Gravity Well", "Puits de Gravité", "Gravitationsfeld", "Pozzo Gravitazionale"},
        {"Quantum Quartet", "Quatuor Quantique", "Quanten-Quartett", "Quartetto Quantistico"},
        {"Hyperdrive", "Hyperpropulsion", "Hyperantrieb", "Iperguida"},
        {"Void Cloak", "Voile du Néant", "Leerenmantel", "Manto del Vuoto"},
        {"Wildcard Core", "Cœur Joker", "Joker-Kern", "Nucleo Jolly"},
        {"Nemesis Engine", "Moteur Némésis", "Nemesis-Motor", "Motore Nemesi"},
        {"Crown Breaker", "Briseur de Couronne", "Kronenbrecher", "Spezzacorona"},
        {"Vendetta Hook", "Crochet de Vendetta", "Rachehaken", "Gancio Vendetta"},
        {"Repulsor Wave", "Vague Répulsive", "Repulsorstoß", "Onda Repulsiva"},
        {"Hunter Barrage", "Salve du Chasseur", "Jägersalve", "Raffica del Cacciatore"},
        {"Hunter Storm", "Tempête du Chasseur", "Jägersturm", "Tempesta del Cacciatore"},
        {"Rival hit -> offender: 2 shots/s for 3s", "Choc rival -> agresseur : 2 tirs/s pendant 3 s", "Rivaltreffer -> Angreifer: 2 Schüsse/s für 3 s", "Colpo rivale -> aggressore: 2 colpi/s per 3 s"},
        {"Repulsor Surge", "Onde Répulsive", "Repulsorwelle", "Ondata Repulsiva"},
        {"Tar Tether", "Longe de Goudron", "Teerfessel", "Legame di Catrame"},
        {"EMP Snare", "Piège IEM", "EMP-Falle", "Trappola EMP"},
        {"Void Anchor", "Ancre du Néant", "Leerenanker", "Ancora del Vuoto"},
        {"Blind Hex", "Malédiction Aveugle", "Blindfluch", "Maledizione Cieca"},
        {"Burden Hex", "Malédiction du Fardeau", "Lastfluch", "Maledizione del Peso"},
        {"Doom Hex", "Malédiction Fatale", "Verdammnisfluch", "Maledizione Fatale"},
        {"Loaded Grudge", "Rancune Chargée", "Geladener Groll", "Rancore Carico"},
        {"Chaos Retort", "Riposte du Chaos", "Chaos-Konterschlag", "Replica del Caos"},
        {"Fate's Revenge", "Vengeance du Destin", "Rache des Schicksals", "Vendetta del Destino"},
        {"Triad Coup", "Coup de Triade", "Triaden-Coup", "Colpo della Triade"}
    };

    private static final String[][] CARD_DESCRIPTIONS = {
        {"Power and grip trade aerodynamic efficiency.", "Puissance et adhérence sacrifient l'efficacité aérodynamique.", "Leistung und Grip kosten Aero-Effizienz.", "Potenza e aderenza sacrificano efficienza aerodinamica."},
        {"Power and grip require extra chassis mass.", "Puissance et adhérence exigent plus de masse.", "Leistung und Grip benötigen mehr Chassismasse.", "Potenza e aderenza richiedono più massa."},
        {"Power and aero efficiency trade tire grip.", "Puissance et efficacité aéro sacrifient l'adhérence.", "Leistung und Aero-Effizienz kosten Reifengrip.", "Potenza ed efficienza aero sacrificano aderenza."},
        {"Power and aero efficiency require extra chassis mass.", "Puissance et efficacité aéro exigent plus de masse.", "Leistung und Aero-Effizienz benötigen mehr Masse.", "Potenza ed efficienza aero richiedono più massa."},
        {"Power and lower mass trade tire grip.", "Puissance et masse réduite sacrifient l'adhérence.", "Leistung und weniger Masse kosten Reifengrip.", "Potenza e massa ridotta sacrificano aderenza."},
        {"Power and lower mass trade aerodynamic efficiency.", "Puissance et masse réduite sacrifient l'efficacité aéro.", "Leistung und weniger Masse kosten Aero-Effizienz.", "Potenza e massa ridotta sacrificano efficienza aero."},
        {"Grip and aero efficiency trade engine power.", "Adhérence et efficacité aéro sacrifient la puissance.", "Grip und Aero-Effizienz kosten Motorleistung.", "Aderenza ed efficienza aero sacrificano potenza."},
        {"Grip and aero efficiency require extra chassis mass.", "Adhérence et efficacité aéro exigent plus de masse.", "Grip und Aero-Effizienz benötigen mehr Masse.", "Aderenza ed efficienza aero richiedono più massa."},
        {"Grip and lower mass trade engine power.", "Adhérence et masse réduite sacrifient la puissance.", "Grip und weniger Masse kosten Motorleistung.", "Aderenza e massa ridotta sacrificano potenza."},
        {"Grip and lower mass trade aerodynamic efficiency.", "Adhérence et masse réduite sacrifient l'efficacité aéro.", "Grip und weniger Masse kosten Aero-Effizienz.", "Aderenza e massa ridotta sacrificano efficienza aero."},
        {"Aero efficiency and lower mass trade engine power.", "Efficacité aéro et masse réduite sacrifient la puissance.", "Aero-Effizienz und weniger Masse kosten Motorleistung.", "Efficienza aero e massa ridotta sacrificano potenza."},
        {"Aero efficiency and lower mass trade tire grip.", "Efficacité aéro et masse réduite sacrifient l'adhérence.", "Aero-Effizienz und weniger Masse kosten Reifengrip.", "Efficienza aero e massa ridotta sacrificano aderenza."},
        {"Power and grip improve together.", "Puissance et adhérence progressent ensemble.", "Leistung und Grip verbessern sich gemeinsam.", "Potenza e aderenza migliorano insieme."},
        {"Power and aero efficiency improve together.", "Puissance et efficacité aéro progressent ensemble.", "Leistung und Aero-Effizienz verbessern sich gemeinsam.", "Potenza ed efficienza aero migliorano insieme."},
        {"Grip and aero efficiency improve together.", "Adhérence et efficacité aéro progressent ensemble.", "Grip und Aero-Effizienz verbessern sich gemeinsam.", "Aderenza ed efficienza aero migliorano insieme."},
        {"Power and lower mass improve together.", "Puissance et masse réduite progressent ensemble.", "Leistung und weniger Masse verbessern sich gemeinsam.", "Potenza e massa ridotta migliorano insieme."},
        {"Grip and lower mass improve together.", "Adhérence et masse réduite progressent ensemble.", "Grip und weniger Masse verbessern sich gemeinsam.", "Aderenza e massa ridotta migliorano insieme."},
        {"Aero efficiency and lower mass improve together.", "Efficacité aéro et masse réduite progressent ensemble.", "Aero-Effizienz und weniger Masse verbessern sich gemeinsam.", "Efficienza aero e massa ridotta migliorano insieme."},
        {"Power, grip and aero efficiency improve together.", "Puissance, adhérence et efficacité aéro progressent ensemble.", "Leistung, Grip und Aero-Effizienz steigen gemeinsam.", "Potenza, aderenza ed efficienza aero migliorano insieme."},
        {"Power, grip and lower mass improve together.", "Puissance, adhérence et masse réduite progressent ensemble.", "Leistung, Grip und weniger Masse verbessern sich gemeinsam.", "Potenza, aderenza e massa ridotta migliorano insieme."},
        {"Power, aero efficiency and lower mass improve together.", "Puissance, efficacité aéro et masse réduite progressent ensemble.", "Leistung, Aero-Effizienz und weniger Masse verbessern sich gemeinsam.", "Potenza, efficienza aero e massa ridotta migliorano insieme."},
        {"Grip, aero efficiency and lower mass improve together.", "Adhérence, efficacité aéro et masse réduite progressent ensemble.", "Grip, Aero-Effizienz und weniger Masse verbessern sich gemeinsam.", "Aderenza, efficienza aero e massa ridotta migliorano insieme."},
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
        {"Cornering amplifies active grip bonuses and every active aero bonus or penalty. Grip penalties and weather stay unchanged.", "En virage, amplifie les bonus d'adhérence et tous les bonus ou malus d'aéro. Les malus d'adhérence et la météo restent inchangés.", "In Kurven werden aktive Grip-Boni und alle Aero-Boni oder -Mali verstärkt. Grip-Mali und Wetter bleiben unverändert.", "In curva amplifica i bonus di aderenza e ogni bonus o penalità aero. Le penalità di aderenza e il meteo restano invariati."},
        {"Slipstreaming amplifies every active power and aero bonus or penalty.", "Dans l'aspiration, amplifie tout bonus ou malus actif de puissance et d'aéro.", "Im Windschatten werden alle aktiven Leistungs- und Aero-Boni oder -Mali verstärkt.", "In scia amplifica ogni bonus o penalità attiva di potenza e aero."},
        {"A long straight amplifies every active power and aero bonus or penalty.", "Une longue ligne droite amplifie tout bonus ou malus actif de puissance et d'aéro.", "Eine lange Gerade verstärkt alle aktiven Leistungs- und Aero-Boni oder -Mali.", "Un lungo rettilineo amplifica ogni bonus o penalità attiva di potenza e aero."},
        {"Drifting amplifies every active power and mass bonus or penalty.", "La dérive amplifie tout bonus ou malus actif de puissance et de masse.", "Driften verstärkt alle aktiven Leistungs- und Masse-Boni oder -Mali.", "La derapata amplifica ogni bonus o penalità attiva di potenza e massa."},
        {"Leaving the road amplifies every active power, aero, and mass bonus or penalty, plus active grip bonuses. Grip penalties and weather stay unchanged.", "La sortie de piste amplifie tous les bonus ou malus de puissance, d'aéro et de masse, ainsi que les bonus d'adhérence. Les malus d'adhérence et la météo restent inchangés.", "Abseits der Strecke werden alle aktiven Leistungs-, Aero- und Masse-Boni oder -Mali sowie Grip-Boni verstärkt. Grip-Mali und Wetter bleiben unverändert.", "Fuori pista amplifica ogni bonus o penalità di potenza, aero e massa, oltre ai bonus di aderenza. Le penalità di aderenza e il meteo restano invariati."},
        {"Cornering amplifies every active aero and mass bonus or penalty.", "En virage, amplifie tout bonus ou malus actif d'aéro et de masse.", "In Kurven werden alle aktiven Aero- und Masse-Boni oder -Mali verstärkt.", "In curva amplifica ogni bonus o penalità attiva di aero e massa."},
        {"A long straight amplifies every active power and mass bonus or penalty.", "Une longue ligne droite amplifie tout bonus ou malus actif de puissance et de masse.", "Eine lange Gerade verstärkt alle aktiven Leistungs- und Masse-Boni oder -Mali.", "Un lungo rettilineo amplifica ogni bonus o penalità attiva di potenza e massa."},
        {"Drifting amplifies every active aero and mass bonus or penalty.", "La dérive amplifie tout bonus ou malus actif d'aéro et de masse.", "Driften verstärkt alle aktiven Aero- und Masse-Boni oder -Mali.", "La derapata amplifica ogni bonus o penalità attiva di aero e massa."},
        {"Cornering amplifies every active power bonus or penalty and active grip bonuses. Grip penalties and weather stay unchanged.", "En virage, amplifie tout bonus ou malus actif de puissance et les bonus d'adhérence. Les malus d'adhérence et la météo restent inchangés.", "In Kurven werden alle aktiven Leistungs-Boni oder -Mali sowie Grip-Boni verstärkt. Grip-Mali und Wetter bleiben unverändert.", "In curva amplifica ogni bonus o penalità attiva di potenza e i bonus di aderenza. Le penalità di aderenza e il meteo restano invariati."},
        {"Cornering amplifies active grip bonuses and every active mass bonus or penalty. Grip penalties and weather stay unchanged.", "En virage, amplifie les bonus d'adhérence et tout bonus ou malus actif de masse. Les malus d'adhérence et la météo restent inchangés.", "In Kurven werden Grip-Boni und alle aktiven Masse-Boni oder -Mali verstärkt. Grip-Mali und Wetter bleiben unverändert.", "In curva amplifica i bonus di aderenza e ogni bonus o penalità attiva di massa. Le penalità di aderenza e il meteo restano invariati."},
        {"Reads the field and gains performance as the car falls back, reaching full strength in last place.", "Les performances augmentent en reculant, au maximum en dernière place.", "Die Leistung steigt weiter hinten und erreicht am Ende ihr Maximum.", "Le prestazioni aumentano arretrando, fino al massimo in ultima posizione."},
        {"Raises the car's pace whenever a rival is nearby, helping attacks and defensive runs.", "Augmente le rythme près des rivaux pour attaquer et défendre.", "Erhöht das Tempo bei nahen Rivalen für Angriff und Verteidigung.", "Aumenta il ritmo con rivali vicini per attaccare e difendere."},
        {"Kicks the car forward when open road invites a nitro burst.", "Propulse la voiture sur une ligne droite dégagée.", "Schiebt das Auto auf freier Geraden kräftig nach vorn.", "Spinge l'auto in avanti su un rettilineo libero."},
        {"Calls the best benchmarked driver, who gives you driving advice for 10 seconds.", "Appelle le meilleur pilote, qui vous conseille pendant 10 secondes.", "Ruft den besten Fahrer an, der dir 10 Sekunden lang Fahrtipps gibt.", "Chiama il miglior pilota, che ti dà consigli di guida per 10 secondi."},
        {"Creates two physical cars. Each drives independently, shares the same cards and executes Revenge with the group; a hit to any copy arms it.", "Crée deux voitures physiques. Chacune conduit seule, partage les cartes et exécute la Vengeance en groupe ; toucher une copie l'arme.", "Erzeugt zwei physische Autos. Jedes fährt selbstständig, teilt die Karten und führt Rache gemeinsam aus; ein Treffer auf eine Kopie lädt sie.", "Crea due auto fisiche. Ognuna guida autonomamente, condivide le carte ed esegue Vendetta col gruppo; colpire una copia la arma."},
        {"A glowing underbody fan pins the car down as a demanding corner arrives.", "Un ventilateur lumineux plaque la voiture avant un virage difficile.", "Ein leuchtender Unterbodenlüfter erhöht den Grip vor harten Kurven.", "Una ventola luminosa incolla l'auto prima di una curva difficile."},
        {"The car phases out when traffic is nearby, becoming invisible and intangible to rivals.", "La voiture devient invisible et intangible près du trafic.", "Bei Verkehr wird das Auto unsichtbar und für Rivalen unberührbar.", "Con traffico vicino, l'auto diventa invisibile e intangibile."},
        {"Prepares a random Tier 1 Powerup and copies its real trigger, effect and cooldown.", "Prépare un Bonus T1 aléatoire avec son déclencheur et son effet.", "Bereitet ein zufälliges T1-Power-up samt Auslöser und Effekt vor.", "Prepara un Potenziamento T1 casuale con attivazione ed effetto."},
        {"A green catalyst ignites whenever Revenge activates and strengthens its real effect.", "Un catalyseur vert s'allume à chaque Vengeance et renforce son effet réel.", "Ein grüner Katalysator zündet bei jeder Rache und verstärkt ihre Wirkung.", "Un catalizzatore verde si accende a ogni Vendetta e ne rafforza l'effetto."},
        {"Prepares a random Tier 2 Powerup and copies its real trigger, effect and cooldown.", "Prépare un Bonus T2 aléatoire avec son déclencheur et son effet.", "Bereitet ein zufälliges T2-Power-up samt Auslöser und Effekt vor.", "Prepara un Potenziamento T2 casuale con attivazione ed effetto."},
        {"A stronger green core surges whenever Revenge activates and magnifies its outcome.", "Un cœur vert plus puissant surgit à chaque Vengeance et amplifie son résultat.", "Ein stärkerer grüner Kern reagiert auf jede Rache und vergrößert ihre Wirkung.", "Un nucleo verde più potente reagisce a ogni Vendetta e ne amplifica il risultato."},
        {"Prepares a random Tier 3 Powerup and copies its real trigger, effect and cooldown.", "Prépare un Bonus T3 aléatoire avec son déclencheur et son effet.", "Bereitet ein zufälliges T3-Power-up samt Auslöser und Effekt vor.", "Prepara un Potenziamento T3 casuale con attivazione ed effetto."},
        {"An extreme green engine doubles the consequences whenever Revenge activates.", "Un moteur vert extrême double les conséquences de chaque Vengeance.", "Ein extremer grüner Motor verdoppelt die Folgen jeder Rache.", "Un motore verde estremo raddoppia le conseguenze di ogni Vendetta."},
        {"A rival hit arms the counter until the next qualified hit is reflected into its attacker.", "Un choc rival arme la riposte jusqu'à ce que le prochain choc valide soit renvoyé sur l'attaquant.", "Ein Rivaltreffer lädt den Konter, bis der nächste gültige Treffer auf den Angreifer zurückgeworfen wird.", "Un colpo rivale arma il contrattacco finché il prossimo colpo valido non viene riflesso sull'aggressore."},
        {"A qualified rival hit arms a short pulsing field that forces nearby cars toward the outside.", "Un choc arme un champ qui repousse brièvement les voitures proches.", "Ein Treffer lädt ein Feld, das nahe Autos kurz nach außen drückt.", "Un colpo arma un campo che spinge fuori le auto vicine."},
        {"A qualified hit marks its offender. After charging, it exchanges positions only while they are ahead.", "Marque l'agresseur et échange les positions seulement s'il est devant.", "Markiert den Angreifer und tauscht nur, wenn er voraus ist.", "Segna l'aggressore e scambia posizione solo se è davanti."},
        {"A qualified hit curses its offender's throttle, forcing them to commit through whatever comes next.", "Maudit l'accélérateur de l'agresseur et le force à fond.", "Verflucht das Gas des Angreifers und zwingt ihn auf Vollgas.", "Maledice l'acceleratore dell'aggressore e lo forza al massimo."},
        {"An energy shell forms when traffic closes in, absorbing frontal recoil.", "Une coque d'énergie absorbe les chocs frontaux dans le trafic.", "Eine Energiehülle fängt bei Verkehr frontale Rückstöße ab.", "Un guscio energetico assorbe gli urti frontali nel traffico."},
        {"Twin exhaust rockets ignite on a clear straight for a forceful launch.", "Deux fusées d'échappement donnent une forte poussée en ligne droite.", "Zwei Auspuffraketen liefern auf freier Geraden starken Schub.", "Due razzi di scarico danno una forte spinta sul rettilineo."},
        {"Calls the best benchmarked driver more often for 10 seconds of driving advice.", "Appelle plus souvent le meilleur pilote pour 10 secondes de conseils.", "Ruft den besten Fahrer häufiger für 10 Sekunden Fahrtipps an.", "Chiama più spesso il miglior pilota per 10 secondi di consigli di guida."},
        {"Creates three physical cars. Each drives independently, shares the same cards and executes Revenge with the group; a hit to any copy arms it.", "Crée trois voitures physiques. Chacune conduit seule, partage les cartes et exécute la Vengeance en groupe ; toucher une copie l'arme.", "Erzeugt drei physische Autos. Jedes fährt selbstständig, teilt die Karten und führt Rache gemeinsam aus; ein Treffer auf eine Kopie lädt sie.", "Crea tre auto fisiche. Ognuna guida autonomamente, condivide le carte ed esegue Vendetta col gruppo; colpire una copia la arma."},
        {"Creates four physical cars. Each drives independently, shares the same cards and executes Revenge with the group; a hit to any copy arms it.", "Crée quatre voitures physiques. Chacune conduit seule, partage les cartes et exécute la Vengeance en groupe ; toucher une copie l'arme.", "Erzeugt vier physische Autos. Jedes fährt selbstständig, teilt die Karten und führt Rache gemeinsam aus; ein Treffer auf eine Kopie lädt sie.", "Crea quattro auto fisiche. Ognuna guida autonomamente, condivide le carte ed esegue Vendetta col gruppo; colpire una copia la arma."},
        {"An improved phase field hides the car and prevents rivals from making contact for longer.", "Un champ amélioré cache la voiture et bloque les contacts plus longtemps.", "Ein besseres Phasenfeld versteckt das Auto länger vor Kontakten.", "Un campo migliorato nasconde l'auto e impedisce contatti più a lungo."},
        {"A visible ground field forms in corners or close traffic for extreme stability.", "Un champ visible apporte une stabilité extrême en virage ou trafic.", "Ein sichtbares Feld sorgt in Kurven oder Verkehr für extreme Stabilität.", "Un campo visibile dà stabilità estrema in curva o nel traffico."},
        {"Open road triggers an extreme launch and turns the car into a visible streak.", "Une route libre déclenche une poussée extrême et une traînée visible.", "Freie Strecke löst extremen Schub und eine sichtbare Spur aus.", "La strada libera attiva una spinta estrema e una scia visibile."},
        {"A championship phase system removes the car from sight and contact for an extended attack window.", "Un système de phase cache la voiture et bloque les contacts longtemps.", "Ein Phasensystem entfernt das Auto länger aus Sicht und Kontakt.", "Un sistema di fase nasconde l'auto e impedisce contatti a lungo."},
        {"A rival hit marks its offender and empowers you until an automatic close-range ram.", "Un choc marque l'agresseur et vous renforce jusqu'à une charge automatique à courte portée.", "Ein Treffer markiert den Angreifer und stärkt dich bis zu einem automatischen Rammstoß aus kurzer Distanz.", "Un colpo segna l'aggressore e ti potenzia fino a una speronata automatica a corto raggio."},
        {"A qualified hit marks its offender. After charging, the hook pulls them directly back to you.", "Marque l'agresseur puis l'attire directement vers vous.", "Markiert den Angreifer und zieht ihn direkt zu dir zurück.", "Segna l'aggressore e lo trascina direttamente verso di te."},
        {"A qualified rival hit arms a medium-range energy wave that pushes nearby cars away.", "Un choc arme une vague de portée moyenne qui repousse les voitures proches.", "Ein Rivaltreffer lädt eine mittelweite Welle, die nahe Autos wegstößt.", "Un colpo arma un'onda a medio raggio che respinge le auto vicine."},
        {"Marks the rival who hit you, then hunts them anywhere on the circuit with three impact shots.", "Marque le rival qui vous a frappé puis le traque partout avec trois tirs d'impact.", "Markiert den Rivalen, der dich traf, und jagt ihn überall mit drei Aufprallschüssen.", "Segna il rivale che ti ha colpito e lo caccia ovunque con tre colpi d'impatto."},
        {"Marks the rival who hit you, then saturates their position with a rapid impact storm anywhere on the circuit.", "Marque le rival qui vous a frappé puis sature sa position d'une rapide tempête d'impacts partout sur le circuit.", "Markiert den Rivalen, der dich traf, und überzieht seine Position überall mit einem schnellen Aufprallsturm.", "Segna il rivale che ti ha colpito e satura la sua posizione con una rapida tempesta d'impatti ovunque sul circuito."},
        {"A qualified rival hit arms a wide high-energy field that clears space for your comeback.", "Un choc arme un large champ qui libère la voie.", "Ein Rivaltreffer lädt ein breites Feld, das Platz schafft.", "Un colpo arma un ampio campo che libera spazio."},
        {"Throws a sticky tether at the rival who hit you and strips all tire traction.", "Lance une longe collante qui supprime toute adhérence de l'agresseur.", "Wirft eine klebrige Fessel und nimmt dem Angreifer jeden Grip.", "Lancia un legame appiccicoso che azzera l'aderenza dell'aggressore."},
        {"Launches a disruptive snare that forces the rival responsible for hitting you to brake without reversing.", "Force l'agresseur à freiner sans passer en marche arrière.", "Zwingt den Angreifer zum Bremsen, ohne rückwärts zu fahren.", "Costringe l'aggressore a frenare senza andare in retromarcia."},
        {"Hurls a heavy energy anchor that forces the rival responsible for hitting you to brake without reversing.", "Une ancre d'énergie force l'agresseur à freiner sans reculer.", "Ein Energieanker zwingt den Angreifer zum Bremsen ohne Rückwärtsfahrt.", "Un'ancora energetica costringe l'aggressore a frenare senza retrocedere."},
        {"Blinds and weakens the rival who hit you for 20 seconds.", "Aveugle et affaiblit pendant 20 secondes le rival qui vous a frappé.", "Blendet und schwächt den Angreifer 20 Sekunden lang.", "Acceca e indebolisce per 20 secondi il rivale che ti ha colpito."},
        {"Chains the rival who hit you to a heavier, weakened and blinded car for 30 seconds.", "Rend lourd, affaibli et aveugle pendant 30 secondes le rival qui vous a frappé.", "Macht den Angreifer 30 Sekunden lang schwer, schwach und blind.", "Rende pesante, debole e cieco per 30 secondi il rivale che ti ha colpito."},
        {"Crushes the rival who hit you with blindness, extreme weight, and severe performance loss for 40 seconds.", "Impose pendant 40 secondes aveuglement, poids extrême et forte perte de performances au rival qui vous a frappé.", "Belegt den Angreifer 40 Sekunden lang mit Blindheit, extremer Masse und starkem Leistungsverlust.", "Impone per 40 secondi cecità, peso estremo e una grave perdita di prestazioni al rivale che ti ha colpito."},
        {"Binds the offender and the car directly behind you, then reverses their places while moving you to the front.", "Lie l'agresseur et la voiture juste derrière, inverse leurs places et vous place devant.", "Bindet Angreifer und direkt folgendes Auto, vertauscht ihre Plätze und setzt dich nach vorn.", "Lega aggressore e auto subito dietro, inverte i loro posti e ti porta davanti."},
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
        String revengeActivation = select(
                language,
                "Activation de vengeance",
                "Racheaktivierung",
                "Attivazione vendetta");
        String translatedEffect = select(language, "effet", "Effekt", "effetto");
        text = text.replace("Revenge activation", revengeActivation)
                .replace("effect", translatedEffect);
        text = text.replace(
                        "Automatic call",
                        select(language, "Appel automatique", "Automatischer Anruf", "Chiamata automatica"))
                .replace(
                        "best avg-lap driver",
                        select(language, "pilote au meilleur tour moyen", "Fahrer mit bester Durchschnittsrunde", "pilota col miglior giro medio"))
                .replace(
                        "Activation",
                        select(language, "Activation", "Aktivierung", "Attivazione"));
        String[] source = {
            "Clear straight", "Fast corner exit", "Corner exit", "Corner ahead", "Corner or traffic",
            "Traffic or corner", "Nearby rival on straight", "Nearby rival", "Rival hit", "Hit taken",
            "Hit received", "Long straight", "Slipstream", "Corner", "Drifting", "Off-road",
            "Each activation", "In draft", "Stay on-road", "Safe re-entry", "Drift exit", "Leave draft",
            "Race events", "Race event", "Lower position", "Clean lap", "Clean run", "Overtake", "Powerup", "Shared cards and Revenge", "Revenge", "Draft",
            "draft", "Power", "power", "Speed", "speed", "Grip", "grip", "Steering", "steering",
            "Mass", "mass", "Aero", "bonuses and penalties", "while cornering", "Stronger hits", "Freer rear while drifting", "reach and boost", "up to",
            "bursts", "Cooldown after effect", "Cooldown", "random Tier", "full throttle", "full brake", "3 impact shots", "1s apart",
            "after 3s", "swap with", "pull offender to you", "over 1s", "reflect impact", "medium outward field", "wide outward field",
            "outward field", "until collision", "blind", "random", "immediately", "Explosive ram", "Recoil",
            "Push", "offender", "if ahead", "cars for", "car for", "Rivals pass through you", "invisible", "Cancels targeting Revenge", "Debuffs remain", "Intangible",
            "launch", "shield", "you lead", "leading rival falls last"
        };
        String[][] target = {
            {"Ligne droite libre", "Sortie rapide", "Sortie de virage", "Virage proche", "Virage ou trafic", "Trafic ou virage", "Rival proche en ligne droite", "Rival proche", "Choc rival", "Choc reçu", "Choc reçu", "Longue ligne droite", "Aspiration", "Virage", "Dérive", "Hors-piste", "Chaque activation", "Dans l'aspiration", "Rester en piste", "Retour sûr", "Sortie de dérive", "Quitter l'aspiration", "Événements de course", "Événement de course", "Position basse", "Tour propre", "Conduite propre", "Dépassement", "Bonus", "Cartes et Vengeance partagées", "Vengeance", "Aspiration", "aspiration", "Puissance", "puissance", "Vitesse", "vitesse", "Adhérence", "adhérence", "Direction", "direction", "Masse", "masse", "Aéro", "bonus et malus", "uniquement en virage", "Chocs renforcés", "Arrière plus libre en dérive", "portée et poussée", "jusqu'à", "par rafales", "Recharge après effet", "Recharge", "Niveau aléatoire", "plein gaz", "frein maximal", "3 tirs d'impact", "espacés de 1 s", "après 3 s", "échanger avec", "attirer l'agresseur vers vous", "sur 1 s", "renvoyer l'impact", "champ répulsif moyen", "large champ répulsif", "champ répulsif", "jusqu'au choc", "aveugle", "aléatoire", "immédiatement", "Bélier explosif", "Recul", "Poussée", "agresseur", "s'il est devant", "voitures pendant", "voiture pendant", "Les rivaux vous traversent", "invisible", "Annule la vengeance ciblée", "Les malus restent", "Intangible", "poussée", "bouclier", "vous passez devant", "le rival devant finit dernier"},
            {"Freie Gerade", "Schneller Kurvenausgang", "Kurvenausgang", "Kurve voraus", "Kurve oder Verkehr", "Verkehr oder Kurve", "Rivale auf Gerader", "Rivale in Nähe", "Rivaltreffer", "Treffer erhalten", "Treffer erhalten", "Lange Gerade", "Windschatten", "Kurve", "Driften", "Abseits", "Jede Aktivierung", "Im Windschatten", "Auf Strecke bleiben", "Sichere Rückkehr", "Driftausgang", "Windschatten verlassen", "Rennereignisse", "Rennereignis", "Hintere Position", "Saubere Runde", "Saubere Fahrt", "Überholen", "Power-up", "Gemeinsame Karten und Rache", "Rache", "Windschatten", "Windschatten", "Leistung", "Leistung", "Tempo", "Tempo", "Haftung", "Haftung", "Lenkung", "Lenkung", "Masse", "Masse", "Aero", "Boni und Mali", "nur in Kurven", "Stärkere Treffer", "Freieres Heck beim Driften", "Reichweite und Schub", "bis zu", "in Schüben", "Abklingzeit nach Effekt", "Abklingzeit", "zufällige Stufe", "Vollgas", "Vollbremsung", "3 Aufprallschüsse", "im Abstand von 1 s", "nach 3 s", "tauschen mit", "Angreifer zu dir ziehen", "über 1 s", "Aufprall zurückwerfen", "mittleres Abstoßfeld", "breites Abstoßfeld", "Abstoßfeld", "bis Kollision", "blind", "zufällig", "sofort", "Explosiver Rammstoß", "Rückstoß", "Schub", "Angreifer", "falls voraus", "Autos für", "Auto für", "Rivalen fahren durch dich", "unsichtbar", "Bricht gezielte Rache ab", "Schwächungen bleiben", "Unberührbar", "Schub", "Schild", "du gehst in Führung", "führender Rivale fällt zurück"},
            {"Rettilineo libero", "Uscita veloce", "Uscita di curva", "Curva vicina", "Curva o traffico", "Traffico o curva", "Rivale vicino sul rettilineo", "Rivale vicino", "Colpo rivale", "Colpo subito", "Colpo subito", "Lungo rettilineo", "Scia", "Curva", "Derapata", "Fuori pista", "Ogni attivazione", "In scia", "Resta in pista", "Rientro sicuro", "Uscita derapata", "Esci dalla scia", "Eventi di gara", "Evento di gara", "Posizione arretrata", "Giro pulito", "Guida pulita", "Sorpasso", "Potenziamento", "Carte e Vendetta condivise", "Vendetta", "Scia", "scia", "Potenza", "potenza", "Velocità", "velocità", "Aderenza", "aderenza", "Sterzo", "sterzo", "Massa", "massa", "Aero", "bonus e penalità", "solo in curva", "Urti più forti", "Retrotreno libero in derapata", "portata e spinta", "fino a", "a raffiche", "Ricarica dopo l'effetto", "Ricarica", "Livello casuale", "tutto gas", "frenata massima", "3 colpi d'impatto", "a distanza di 1 s", "dopo 3 s", "scambia con", "trascina l'aggressore verso di te", "in 1 s", "riflette l'impatto", "campo repulsivo medio", "ampio campo repulsivo", "campo repulsivo", "fino alla collisione", "cieco", "casuale", "subito", "Ariete esplosivo", "Rinculo", "Spinta", "aggressore", "se davanti", "auto per", "auto per", "I rivali ti attraversano", "invisibile", "Annulla la vendetta mirata", "I malus restano", "Intangibile", "spinta", "scudo", "vai davanti", "il rivale davanti finisce ultimo"}
        };
        String result = text;
        String[] replacements = target[languageColumn(language) - 1];
        for (int i = 0; i < source.length; i++) {
            result = result.replace(source[i], replacements[i]);
        }
        return result.replace(
                "bonuses only",
                select(language, "bonus uniquement", "nur Boni", "solo bonus"));
    }

    private static boolean looksLikeCardEffect(String text) {
        return text.indexOf('|') >= 0
                || text.indexOf('\n') >= 0
                || text.indexOf("->") >= 0
                || text.indexOf(':') >= 0;
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
