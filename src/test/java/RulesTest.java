import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Legal-play validation: a card is playable by color, number, action type, or wild.
class RulesTest {

    @Test
    void matchesByColor() {
        assertTrue(Rules.isLegal("R2", "R9", ""));
    }

    @Test
    void matchesByNumberAcrossColors() {
        assertTrue(Rules.isLegal("G9", "R9", ""));
    }

    @Test
    void matchesByActionTypeAcrossColors() {
        assertTrue(Rules.isLegal("BS", "RS", ""));
        assertTrue(Rules.isLegal("BR", "GR", ""));
        assertTrue(Rules.isLegal("B+2", "G+2", ""));
    }

    @Test
    void wildsAreAlwaysLegal() {
        assertTrue(Rules.isLegal("W", "R9", ""));
        assertTrue(Rules.isLegal("W4", "G+2", ""));
    }

    @Test
    void matchesTheCalledColorAfterAWild() {
        assertTrue(Rules.isLegal("B3", "W", "B"));
    }

    @Test
    void rejectsActionCardOnANumber() {
        assertFalse(Rules.isLegal("BS", "R5", ""));
    }

    @Test
    void rejectsColorAndNumberMismatch() {
        assertFalse(Rules.isLegal("B3", "R9", ""));
    }
}
