package com.github.jbescos.presentation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Tracks notable incidents used by the TV camera director. */
public final class RaceIncidentPopup {
    private static final int MAX_INCIDENT_ENTRIES = 6;
    private static final float INCIDENT_TTL_SECONDS = 18f;
    private static final float DUPLICATE_WINDOW_SECONDS = 0.8f;

    private static final class HitIncident {
        private final int victimId;
        private final int offenderId;
        private float age;

        private HitIncident(int victimId, int offenderId) {
            this.victimId = victimId;
            this.offenderId = offenderId;
        }
    }

    private static final class Commentary {
        private final int primaryVehicleId;
        private final int secondaryVehicleId;
        private final String eventKey;
        private float age;

        private Commentary(HitIncident incident) {
            primaryVehicleId = incident.victimId;
            secondaryVehicleId = incident.offenderId;
            eventKey = "HIT";
        }

        private Commentary(HitIncident incident, String cardName) {
            primaryVehicleId = incident.victimId;
            secondaryVehicleId = incident.offenderId;
            eventKey = "REVENGE:" + cardName;
        }

        private Commentary(int overtakerId, int passedVehicleId, int rivalsPassed) {
            int safeRivalsPassed = Math.max(1, rivalsPassed);
            primaryVehicleId = overtakerId;
            secondaryVehicleId = passedVehicleId;
            eventKey = "OVERTAKE:" + safeRivalsPassed;
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
    private final List<Commentary> entries =
            new ArrayList<Commentary>(MAX_INCIDENT_ENTRIES);
    private long displaySequence;

    public void recordHit(int victimId, int offenderId) {
        if (!acceptsParticipants(victimId, offenderId)) {
            return;
        }
        HitIncident incident = new HitIncident(victimId, offenderId);
        pendingHits.put(incidentKey(victimId, offenderId), incident);
        if (!wasRecentlyLogged(victimId, offenderId, "HIT")) {
            append(new Commentary(incident));
        }
    }

    public void showRevenge(
            int victimId,
            int offenderId,
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
            incident = new HitIncident(victimId, offenderId);
        }
        append(new Commentary(incident, safeCardName));
    }

    public void showOvertake(
            int overtakerId,
            int passedVehicleId,
            int rivalsPassed) {
        if (!acceptsParticipants(overtakerId, passedVehicleId)
                || rivalsPassed <= 0) {
            return;
        }
        String eventKey = "OVERTAKE:" + Math.max(1, rivalsPassed);
        if (wasRecentlyLogged(overtakerId, passedVehicleId, eventKey)) {
            return;
        }
        append(new Commentary(overtakerId, passedVehicleId, rivalsPassed));
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
    }

    public boolean isVisible() {
        return !entries.isEmpty();
    }

    int getIncidentCount() {
        return entries.size();
    }

    public long getDisplaySequence() {
        return displaySequence;
    }

    public int getPrimaryVehicleId() {
        Commentary latest = latest();
        return latest == null ? -1 : latest.primaryVehicleId;
    }

    private void append(Commentary commentary) {
        if (entries.size() >= MAX_INCIDENT_ENTRIES) {
            entries.remove(0);
        }
        entries.add(commentary);
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

    private boolean acceptsParticipants(int primaryVehicleId, int secondaryVehicleId) {
        return primaryVehicleId >= 0
                && secondaryVehicleId >= 0
                && primaryVehicleId != secondaryVehicleId
                && !discardedVehicles.contains(Integer.valueOf(primaryVehicleId))
                && !discardedVehicles.contains(Integer.valueOf(secondaryVehicleId));
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
