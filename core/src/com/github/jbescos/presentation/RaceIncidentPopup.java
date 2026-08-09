package com.github.jbescos.presentation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Maintains a compact, clickable log of notable incidents during a race. */
public final class RaceIncidentPopup {
    private static final int MAX_LOG_ENTRIES = 6;
    private static final float INCIDENT_TTL_SECONDS = 18f;
    private static final float DUPLICATE_WINDOW_SECONDS = 0.8f;

    private enum EventType {
        HIT,
        REVENGE,
        OVERTAKE
    }

    private static final class HitIncident {
        private final int victimId;
        private final String victimName;
        private final int offenderId;
        private final String offenderName;
        private final String impactVerb;
        private float age;

        private HitIncident(
                int victimId,
                String victimName,
                int offenderId,
                String offenderName,
                String impactVerb) {
            this.victimId = victimId;
            this.victimName = victimName;
            this.offenderId = offenderId;
            this.offenderName = offenderName;
            this.impactVerb = impactVerb;
        }
    }

    private static final class Commentary {
        private final int primaryVehicleId;
        private final int secondaryVehicleId;
        private final String eventKey;
        private final EventType eventType;
        private final String headline;
        private final String impactLine;
        private final String detailLine;
        private final String logLine;
        private float age;

        private Commentary(HitIncident incident) {
            primaryVehicleId = incident.victimId;
            secondaryVehicleId = incident.offenderId;
            eventKey = "HIT";
            eventType = EventType.HIT;
            headline = "IMPACT";
            impactLine = incident.offenderName
                    + " "
                    + incident.impactVerb
                    + " "
                    + incident.victimName;
            detailLine = "";
            logLine = "HIT: " + impactLine;
        }

        private Commentary(HitIncident incident, String cardName) {
            primaryVehicleId = incident.victimId;
            secondaryVehicleId = incident.offenderId;
            eventKey = "REVENGE:" + cardName;
            eventType = EventType.REVENGE;
            headline = "REVENGE";
            impactLine = incident.offenderName
                    + " "
                    + incident.impactVerb
                    + " "
                    + incident.victimName;
            detailLine = incident.victimName
                    + " USED "
                    + cardName
                    + " ON "
                    + incident.offenderName;
            logLine = "REVENGE: " + detailLine;
        }

        private Commentary(
                int overtakerId,
                String overtakerName,
                int passedVehicleId,
                String passedVehicleName,
                int rivalsPassed) {
            int safeRivalsPassed = Math.max(1, rivalsPassed);
            primaryVehicleId = overtakerId;
            secondaryVehicleId = passedVehicleId;
            eventKey = "OVERTAKE:" + safeRivalsPassed;
            eventType = EventType.OVERTAKE;
            headline = "OVERTAKE";
            impactLine = safeName(overtakerName)
                    + " PASSED "
                    + safeName(passedVehicleName);
            detailLine = safeRivalsPassed == 1
                    ? "1 POSITION GAINED"
                    : safeRivalsPassed + " POSITIONS GAINED";
            logLine = "PASS: "
                    + impactLine
                    + (safeRivalsPassed > 1 ? " (+" + safeRivalsPassed + ")" : "");
        }

        private boolean matches(
                int primaryVehicleId,
                int secondaryVehicleId,
                String eventKey) {
            return this.primaryVehicleId == primaryVehicleId
                    && this.secondaryVehicleId == secondaryVehicleId
                    && this.eventKey.equals(eventKey);
        }

        private boolean involves(int vehicleId) {
            return primaryVehicleId == vehicleId || secondaryVehicleId == vehicleId;
        }
    }

    private final Map<Long, HitIncident> pendingHits =
            new HashMap<Long, HitIncident>();
    private final Set<Integer> discardedVehicles = new HashSet<Integer>();
    private final List<Commentary> entries = new ArrayList<Commentary>(MAX_LOG_ENTRIES);
    private int cameraLogIndex = -1;
    private int nextCameraParticipant;
    private long displaySequence;

    public void recordHit(
            int victimId,
            String victimName,
            int offenderId,
            String offenderName,
            float normalizedImpactStrength) {
        if (!acceptsParticipants(victimId, offenderId)) {
            return;
        }
        HitIncident incident = new HitIncident(
                victimId,
                safeName(victimName),
                offenderId,
                safeName(offenderName),
                impactVerb(normalizedImpactStrength));
        pendingHits.put(incidentKey(victimId, offenderId), incident);
        if (!wasRecentlyLogged(victimId, offenderId, "HIT")) {
            append(new Commentary(incident));
        }
    }

    public void showRevenge(
            int victimId,
            String victimName,
            int offenderId,
            String offenderName,
            String cardName) {
        if (!acceptsParticipants(victimId, offenderId)) {
            return;
        }
        String safeCardName = safeName(cardName);
        String eventKey = "REVENGE:" + safeCardName;
        if (wasRecentlyLogged(victimId, offenderId, eventKey)) {
            return;
        }

        long incidentKey = incidentKey(victimId, offenderId);
        HitIncident incident = pendingHits.remove(incidentKey);
        if (incident == null) {
            incident = new HitIncident(
                    victimId,
                    safeName(victimName),
                    offenderId,
                    safeName(offenderName),
                    "HIT");
        }
        append(new Commentary(incident, safeCardName));
    }

    public void showOvertake(
            int overtakerId,
            String overtakerName,
            int passedVehicleId,
            String passedVehicleName,
            int rivalsPassed) {
        if (!acceptsParticipants(overtakerId, passedVehicleId)
                || rivalsPassed <= 0) {
            return;
        }
        String eventKey = "OVERTAKE:" + Math.max(1, rivalsPassed);
        if (wasRecentlyLogged(overtakerId, passedVehicleId, eventKey)) {
            return;
        }
        append(
                new Commentary(
                        overtakerId,
                        overtakerName,
                        passedVehicleId,
                        passedVehicleName,
                        rivalsPassed));
    }

    public void update(float delta) {
        float elapsed = Math.max(0f, delta);
        Iterator<Map.Entry<Long, HitIncident>> iterator =
                pendingHits.entrySet().iterator();
        while (iterator.hasNext()) {
            HitIncident incident = iterator.next().getValue();
            incident.age += elapsed;
            if (incident.age >= INCIDENT_TTL_SECONDS) {
                iterator.remove();
            }
        }
        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).age += elapsed;
        }
    }

    public void reset() {
        pendingHits.clear();
        discardedVehicles.clear();
        entries.clear();
        cameraLogIndex = -1;
        nextCameraParticipant = 0;
    }

    public void discardVehicle(int vehicleId) {
        if (vehicleId < 0) {
            return;
        }
        discardedVehicles.add(Integer.valueOf(vehicleId));
        Iterator<Map.Entry<Long, HitIncident>> pendingIterator =
                pendingHits.entrySet().iterator();
        while (pendingIterator.hasNext()) {
            HitIncident incident = pendingIterator.next().getValue();
            if (incident.victimId == vehicleId || incident.offenderId == vehicleId) {
                pendingIterator.remove();
            }
        }
        Iterator<Commentary> entryIterator = entries.iterator();
        while (entryIterator.hasNext()) {
            if (entryIterator.next().involves(vehicleId)) {
                entryIterator.remove();
            }
        }
        cameraLogIndex = -1;
        nextCameraParticipant = 0;
    }

    public boolean isVisible() {
        return !entries.isEmpty();
    }

    public float getAlpha() {
        return entries.isEmpty() ? 0f : 1f;
    }

    public String getHeadline() {
        Commentary latest = latest();
        return latest == null ? "" : latest.headline;
    }

    public String getImpactLine() {
        Commentary latest = latest();
        return latest == null ? "" : latest.impactLine;
    }

    public String getDetailLine() {
        Commentary latest = latest();
        return latest == null ? "" : latest.detailLine;
    }

    public boolean isRevenge() {
        Commentary latest = latest();
        return latest != null && latest.eventType == EventType.REVENGE;
    }

    public int getLogLineCount() {
        return entries.size();
    }

    public String getLogLine(int index) {
        return isValidLogIndex(index) ? entries.get(index).logLine : "";
    }

    public boolean isLogLineRevenge(int index) {
        return isValidLogIndex(index)
                && entries.get(index).eventType == EventType.REVENGE;
    }

    public boolean isLogLineHit(int index) {
        return isValidLogIndex(index)
                && entries.get(index).eventType == EventType.HIT;
    }

    public long getDisplaySequence() {
        return displaySequence;
    }

    public int getPrimaryVehicleId() {
        Commentary latest = latest();
        return latest == null ? -1 : latest.primaryVehicleId;
    }

    public int nextCameraVehicleId() {
        return nextCameraVehicleId(entries.size() - 1);
    }

    public int nextCameraVehicleId(int logIndex) {
        if (!isValidLogIndex(logIndex)) {
            return -1;
        }
        if (cameraLogIndex != logIndex) {
            cameraLogIndex = logIndex;
            nextCameraParticipant = 0;
        }
        Commentary commentary = entries.get(logIndex);
        int vehicleId = nextCameraParticipant == 0
                ? commentary.primaryVehicleId
                : commentary.secondaryVehicleId;
        nextCameraParticipant = (nextCameraParticipant + 1) % 2;
        return vehicleId;
    }

    int getQueuedCount() {
        return Math.max(0, entries.size() - 1);
    }

    private void append(Commentary commentary) {
        if (entries.size() >= MAX_LOG_ENTRIES) {
            entries.remove(0);
        }
        entries.add(commentary);
        cameraLogIndex = -1;
        nextCameraParticipant = 0;
        displaySequence++;
    }

    private boolean wasRecentlyLogged(
            int primaryVehicleId,
            int secondaryVehicleId,
            String eventKey) {
        for (int i = entries.size() - 1; i >= 0; i--) {
            Commentary entry = entries.get(i);
            if (entry.age >= DUPLICATE_WINDOW_SECONDS) {
                break;
            }
            if (entry.matches(primaryVehicleId, secondaryVehicleId, eventKey)) {
                return true;
            }
        }
        return false;
    }

    private Commentary latest() {
        return entries.isEmpty() ? null : entries.get(entries.size() - 1);
    }

    private boolean isValidLogIndex(int index) {
        return index >= 0 && index < entries.size();
    }

    private boolean acceptsParticipants(int primaryVehicleId, int secondaryVehicleId) {
        return primaryVehicleId >= 0
                && secondaryVehicleId >= 0
                && primaryVehicleId != secondaryVehicleId
                && !discardedVehicles.contains(Integer.valueOf(primaryVehicleId))
                && !discardedVehicles.contains(Integer.valueOf(secondaryVehicleId));
    }

    private static String impactVerb(float normalizedImpactStrength) {
        if (normalizedImpactStrength >= 0.68f) {
            return "SLAMMED";
        }
        if (normalizedImpactStrength >= 0.30f) {
            return "SHOVED";
        }
        return "CLIPPED";
    }

    private static long incidentKey(int victimId, int offenderId) {
        return ((long) victimId << 32) ^ (offenderId & 0xffffffffL);
    }

    private static String safeName(String value) {
        if (value == null || value.trim().length() == 0) {
            return "UNKNOWN";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
