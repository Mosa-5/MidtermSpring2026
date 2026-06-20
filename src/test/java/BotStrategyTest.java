import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Bot card-selection priority (Draw Two > Skip > Number > Wild) and colour choice.
class BotStrategyTest {

    private final BotStrategy bot = new BotStrategy();

    private ArrayList<String> hand(String... cards) {
        return new ArrayList<String>(Arrays.asList(cards));
    }

    @Test
    void picksNumberBeforeWild() {
        assertEquals(1, bot.chooseCard(hand("B3", "R4", "W"), "R9", ""));
    }

    @Test
    void prefersDrawTwoOverSkipAndNumber() {
        assertEquals(2, bot.chooseCard(hand("R3", "RS", "R+2", "W"), "R9", ""));
    }

    @Test
    void prefersSkipOverNumber() {
        assertEquals(1, bot.chooseCard(hand("R3", "RS", "W"), "R9", ""));
    }

    @Test
    void prefersNumberOverWildWhenLegal() {
        assertEquals(0, bot.chooseCard(hand("R3", "W"), "R9", ""));
    }

    @Test
    void fallsBackToWildWhenNothingElseIsLegal() {
        assertEquals(1, bot.chooseCard(hand("B3", "W"), "R9", ""));
    }

    @Test
    void returnsMinusOneWhenNoLegalCardAndNoWild() {
        assertEquals(-1, bot.chooseCard(hand("B3", "Y5"), "R9", ""));
    }

    @Test
    void colorChoicePicksTheMajority() {
        assertEquals("B", bot.chooseColor(hand("B1", "B2", "R3")));
    }

    @Test
    void colorChoiceBreaksFourWayTieTowardRed() {
        assertEquals("R", bot.chooseColor(hand("R1", "Y1", "G1", "B1")));
    }

    @Test
    void colorChoiceBreaksTieYellowOverGreen() {
        assertEquals("Y", bot.chooseColor(hand("Y1", "G1")));
    }
}
