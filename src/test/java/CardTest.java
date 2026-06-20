import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Card parsing helpers and point values.
class CardTest {

    @Test
    void colorReadsThePrefixAndIsEmptyForWilds() {
        assertEquals("R", Card.color("R5"));
        assertEquals("", Card.color("W"));
    }

    @Test
    void rankClassifiesEverySuffix() {
        assertEquals("NUMBER", Card.rank("R5"));
        assertEquals("SKIP", Card.rank("BS"));
        assertEquals("REVERSE", Card.rank("GR"));
        assertEquals("DRAW_TWO", Card.rank("R+2"));
        assertEquals("WILD", Card.rank("W"));
        assertEquals("WILD_DRAW_FOUR", Card.rank("W4"));
    }

    @Test
    void numberExtractsValueAndIsMinusOneForActionCards() {
        assertEquals(7, Card.number("R7"));
        assertEquals(-1, Card.number("RS"));
    }

    @Test
    void pointsMatchStandardUnoScoring() {
        assertEquals(5, Card.points("R5"));
        assertEquals(20, Card.points("BS"));
        assertEquals(20, Card.points("GR"));
        assertEquals(20, Card.points("R+2"));
        assertEquals(50, Card.points("W"));
        assertEquals(50, Card.points("W4"));
    }
}
