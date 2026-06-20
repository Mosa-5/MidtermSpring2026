import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Missed-UNO penalty: a player left on one card without calling UNO draws two cards.
class UnoPenaltyTest {

    private GameEngine engine(GameState gs) {
        return new GameEngine(gs, TestSupport.quietView(), null, new BotStrategy());
    }

    private GamePlayer withHand(GameState gs, int index, String... cards) {
        GamePlayer p = gs.players.get(index);
        p.hand().clear();
        for (String c : cards) {
            p.hand().add(c);
        }
        return p;
    }

    @Test
    void penalizesAPlayerOnOneCardWhoDidNotCallUno() {
        GameState gs = TestSupport.builtDeckState(2, 1);
        GamePlayer p = withHand(gs, 0, "R5");
        p.setCalledUno(false);

        assertTrue(engine(gs).enforceUnoPenalty(p));
        assertEquals(3, p.hand().size());
    }

    @Test
    void noPenaltyWhenUnoWasCalled() {
        GameState gs = TestSupport.builtDeckState(2, 1);
        GamePlayer p = withHand(gs, 0, "R5");
        p.setCalledUno(true);

        assertFalse(engine(gs).enforceUnoPenalty(p));
        assertEquals(1, p.hand().size());
    }

    @Test
    void noPenaltyWhenHoldingMoreThanOneCard() {
        GameState gs = TestSupport.builtDeckState(2, 1);
        GamePlayer p = withHand(gs, 0, "R5", "G7");
        p.setCalledUno(false);

        assertFalse(engine(gs).enforceUnoPenalty(p));
        assertEquals(2, p.hand().size());
    }
}
