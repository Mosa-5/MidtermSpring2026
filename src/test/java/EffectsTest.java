import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Action-card effects on turn order and hands, applied through GameState (no console).
class EffectsTest {

    private GameState state(int players) {
        GameState gs = TestSupport.builtDeckState(players, 1);
        gs.currentPlayer = 0;
        gs.direction = 1;
        return gs;
    }

    @Test
    void skipAdvancesTwoPlayersInThreePlayerGame() {
        GameState gs = state(3);
        gs.applyEffect("BS");
        assertEquals(2, gs.currentPlayer);
    }

    @Test
    void reverseInTwoPlayerGameReturnsToSamePlayerAndFlipsDirection() {
        GameState gs = state(2);
        gs.applyEffect("BR");
        assertEquals(0, gs.currentPlayer);
        assertEquals(-1, gs.direction);
    }

    @Test
    void reverseInThreePlayerGameFlipsDirectionAndStepsBackward() {
        GameState gs = state(3);
        gs.applyEffect("BR");
        assertEquals(-1, gs.direction);
        assertEquals(2, gs.currentPlayer);
    }

    @Test
    void drawTwoTargetGainsTwoCardsAndIsSkipped() {
        GameState gs = state(3);
        gs.applyEffect("R+2");
        assertEquals(2, gs.players.get(1).hand().size());
        assertEquals(2, gs.currentPlayer);
    }

    @Test
    void wildDrawFourTargetGainsFourCardsAndIsSkipped() {
        GameState gs = state(3);
        gs.applyEffect("W4");
        assertEquals(4, gs.players.get(1).hand().size());
        assertEquals(2, gs.currentPlayer);
    }
}
