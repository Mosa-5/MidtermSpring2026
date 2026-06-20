import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Human input parsing through the InputSource seam (draw/pass, card codes, colour, UNO).
class ConsoleInputTest {

    private final ConsoleView view = TestSupport.quietView();

    private InputSource queue(final String... lines) {
        return new InputSource() {
            int idx = 0;

            public String nextLine() {
                return lines[idx++];
            }
        };
    }

    private ArrayList<String> hand(String... cards) {
        return new ArrayList<String>(Arrays.asList(cards));
    }

    @Test
    void typingDrawReturnsMinusOneEvenWithALegalPlay() {
        ConsoleInput in = new ConsoleInput(queue("draw"), view);
        assertEquals(-1, in.askHuman(hand("R5"), "R9", ""));
    }

    @Test
    void typingACardCodeReturnsItsIndexWhenLegal() {
        ConsoleInput in = new ConsoleInput(queue("R5"), view);
        assertEquals(0, in.askHuman(hand("R5"), "R9", ""));
    }

    @Test
    void anIllegalCodeRepromptsThenAcceptsALegalOne() {
        ConsoleInput in = new ConsoleInput(queue("BAD", "R5"), view);
        assertEquals(0, in.askHuman(hand("R5"), "R9", ""));
    }

    @Test
    void askColorReturnsTheChosenColor() {
        ConsoleInput in = new ConsoleInput(queue("G"), view);
        assertEquals("G", in.askColor());
    }

    @Test
    void askUnoIsTrueOnlyWhenTheCallIsTyped() {
        assertTrue(new ConsoleInput(queue("uno"), view).askUno());
        assertFalse(new ConsoleInput(queue("no"), view).askUno());
    }
}
