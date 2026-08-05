package com.github.jbescos.presentation;

/** Mutable text-editing state for the short player name shown by the game UI. */
public final class PlayerNameEditor {
    private final StringBuilder draft = new StringBuilder(PlayerDisplayName.MAX_LENGTH);
    private boolean editing;

    public void begin(String currentName) {
        draft.setLength(0);
        draft.append(PlayerDisplayName.editableValue(currentName));
        editing = true;
    }

    public boolean isEditing() {
        return editing;
    }

    public String getDraft() {
        return draft.toString();
    }

    public void type(char character) {
        if (!editing) {
            return;
        }
        if (character == '\b' || character == 127) {
            backspace();
            return;
        }
        if (character >= 32
                && character <= 126
                && draft.length() < PlayerDisplayName.MAX_LENGTH) {
            draft.append(character);
        }
    }

    public String commit() {
        editing = false;
        return PlayerDisplayName.sanitize(draft.toString());
    }

    public void cancel() {
        editing = false;
    }

    private void backspace() {
        if (draft.length() > 0) {
            draft.deleteCharAt(draft.length() - 1);
        }
    }
}
