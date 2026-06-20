import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Deck composition (standard 108-card UNO deck) and draw-pile behaviour.
class DeckTest {

    @Test
    void freshDeckHasStandard108CardComposition() {
        Deck deck = new Deck(new Random(1));
        deck.buildFresh();

        int total = deck.drawPileSize();
        assertEquals(108, total, "deck should hold 108 cards");

        Map<String, Integer> rankCounts = new HashMap<String, Integer>();
        Map<String, Integer> colorCounts = new HashMap<String, Integer>();
        int wilds = 0;
        for (int i = 0; i < total; i++) {
            String card = deck.draw();
            rankCounts.merge(Card.rank(card), 1, Integer::sum);
            if (card.startsWith("W")) {
                wilds++;
            } else {
                colorCounts.merge(Card.color(card), 1, Integer::sum);
            }
        }

        // 4 colours x (one 0 + two each of 1-9) = 76 number cards.
        assertEquals(76, rankCounts.get("NUMBER").intValue());
        // Two each of Skip / Reverse / Draw-Two per colour = 8 of each.
        assertEquals(8, rankCounts.get("SKIP").intValue());
        assertEquals(8, rankCounts.get("REVERSE").intValue());
        assertEquals(8, rankCounts.get("DRAW_TWO").intValue());
        // 4 Wild + 4 Wild Draw Four.
        assertEquals(8, wilds);
        // 25 coloured cards per colour.
        for (String color : new String[]{"R", "Y", "G", "B"}) {
            assertEquals(25, colorCounts.get(color).intValue(), "colour " + color);
        }
    }

    @Test
    void drawReshufflesTheDiscardWhenTheDrawPileIsEmpty() {
        Deck deck = new Deck(new Random(1));
        deck.discard("R5");
        assertEquals("R5", deck.draw());
    }

    @Test
    void drawReturnsWildWhenBothPilesAreEmpty() {
        Deck deck = new Deck(new Random(1));
        deck.discard("R5");
        deck.draw();
        assertEquals("W", deck.draw());
    }
}
