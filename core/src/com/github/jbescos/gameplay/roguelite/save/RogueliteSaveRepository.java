package com.github.jbescos.gameplay.roguelite.save;

import com.badlogic.gdx.utils.Json;

public final class RogueliteSaveRepository {
    private static final int JOURNAL_VERSION = 1;
    private static final String ACTIVE_SLOT_KEY = "active";
    private static final String SLOT_A_KEY = "slot.a";
    private static final String SLOT_B_KEY = "slot.b";

    private final RogueliteSaveStore store;
    private final Json json;

    public RogueliteSaveRepository(RogueliteSaveStore store) {
        if (store == null) {
            throw new IllegalArgumentException("Save store is required.");
        }
        this.store = store;
        json = new Json();
        json.setIgnoreUnknownFields(true);
    }

    public RogueliteSaveData load() {
        Slot slotA = readSlot(SLOT_A_KEY);
        Slot slotB = readSlot(SLOT_B_KEY);
        Slot latest = newer(slotA, slotB);
        return latest == null ? null : decode(latest.envelope.payload);
    }

    public boolean save(RogueliteSaveData data) {
        if (data == null || !data.isStructurallyValid()) {
            return false;
        }
        String payload;
        try {
            payload = json.toJson(data, RogueliteSaveData.class);
        } catch (RuntimeException exception) {
            return false;
        }

        Slot current = newer(readSlot(SLOT_A_KEY), readSlot(SLOT_B_KEY));
        long generation = current == null ? 1L : current.envelope.generation + 1L;
        String targetKey =
                current != null && SLOT_A_KEY.equals(current.key)
                        ? SLOT_B_KEY
                        : SLOT_A_KEY;
        Envelope envelope = new Envelope();
        envelope.journalVersion = JOURNAL_VERSION;
        envelope.generation = generation;
        envelope.payload = payload;
        envelope.checksum = checksum(generation, payload);

        try {
            store.put(targetKey, json.toJson(envelope, Envelope.class));
            store.flush();
            if (readSlot(targetKey) == null) {
                return false;
            }
            store.put(ACTIVE_SLOT_KEY, targetKey);
            store.flush();
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public void delete() {
        try {
            store.remove(ACTIVE_SLOT_KEY);
            store.remove(SLOT_A_KEY);
            store.remove(SLOT_B_KEY);
            store.flush();
        } catch (RuntimeException ignored) {
            // A later save or delete retries the operation.
        }
    }

    private Slot readSlot(String key) {
        String encoded;
        try {
            encoded = store.get(key);
        } catch (RuntimeException exception) {
            return null;
        }
        if (encoded == null || encoded.length() == 0) {
            return null;
        }

        try {
            Envelope envelope = json.fromJson(Envelope.class, encoded);
            if (envelope == null
                    || envelope.journalVersion != JOURNAL_VERSION
                    || envelope.generation < 1L
                    || envelope.payload == null
                    || !checksum(envelope.generation, envelope.payload)
                            .equals(envelope.checksum)
                    || decode(envelope.payload) == null) {
                return null;
            }
            return new Slot(key, envelope);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private RogueliteSaveData decode(String payload) {
        try {
            RogueliteSaveData data = json.fromJson(RogueliteSaveData.class, payload);
            data = migrate(data);
            return data != null && data.isStructurallyValid() ? data : null;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private RogueliteSaveData migrate(RogueliteSaveData data) {
        if (data == null || data.version > RogueliteSaveData.CURRENT_VERSION) {
            return null;
        }
        switch (data.version) {
            case RogueliteSaveData.CURRENT_VERSION:
                return data;
            default:
                return null;
        }
    }

    private static Slot newer(Slot left, Slot right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return right.envelope.generation > left.envelope.generation ? right : left;
    }

    private static String checksum(long generation, String payload) {
        int hash = 0x811c9dc5;
        String value = Long.toString(generation) + "\n" + payload;
        for (int i = 0; i < value.length(); i++) {
            hash ^= value.charAt(i);
            hash *= 0x01000193;
        }
        return Integer.toHexString(hash);
    }

    private static final class Slot {
        private final String key;
        private final Envelope envelope;

        private Slot(String key, Envelope envelope) {
            this.key = key;
            this.envelope = envelope;
        }
    }

    public static final class Envelope {
        public int journalVersion;
        public long generation;
        public String payload = "";
        public String checksum = "";
    }
}
