package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PlayerNameEditorTest {
    @Test
    public void defaultNameStartsAsAnEmptyEditableValue() {
        PlayerNameEditor editor = new PlayerNameEditor();

        editor.begin("YOU");

        assertTrue(editor.isEditing());
        assertEquals("", editor.getDraft());
    }

    @Test
    public void acceptsTypingBackspaceAndAtMostFifteenCharacters() {
        PlayerNameEditor editor = new PlayerNameEditor();
        editor.begin("YOU");

        for (char character : "ABCDEFGHIJKLMNOP".toCharArray()) {
            editor.type(character);
        }
        editor.type('\b');
        editor.type('P');

        assertEquals("ABCDEFGHIJKLMNP", editor.getDraft());
        assertEquals("ABCDEFGHIJKLMNP", editor.commit());
        assertFalse(editor.isEditing());
    }

    @Test
    public void emptyCommitRestoresFallbackAndCancelLeavesEditing() {
        PlayerNameEditor editor = new PlayerNameEditor();
        editor.begin("YOU");

        assertEquals("YOU", editor.commit());

        editor.begin("RACER");
        editor.type('X');
        editor.cancel();
        assertFalse(editor.isEditing());
    }
}
