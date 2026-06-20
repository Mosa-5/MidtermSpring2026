import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class Deck {
    private ArrayList<String> drawPile = new ArrayList<String>();
    private ArrayList<String> discardPile = new ArrayList<String>();
    private Random random;

    public Deck(Random random) {
        this.random = random;
    }

    void buildFresh() {
        drawPile.clear();
        discardPile.clear();
        String[] colors = {"R", "Y", "G", "B"};
        for (int c = 0; c < colors.length; c++) {
            drawPile.add(colors[c] + "0");
            for (int n = 1; n <= 9; n++) {
                drawPile.add(colors[c] + n);
                drawPile.add(colors[c] + n);
            }
            drawPile.add(colors[c] + "S");
            drawPile.add(colors[c] + "S");
            drawPile.add(colors[c] + "R");
            drawPile.add(colors[c] + "R");
            drawPile.add(colors[c] + "+2");
            drawPile.add(colors[c] + "+2");
        }
        for (int i = 0; i < 4; i++) {
            drawPile.add("W");
            drawPile.add("W4");
        }
        Collections.shuffle(drawPile, random);
    }

    String draw() {
        if (drawPile.size() == 0) {
            drawPile.addAll(discardPile);
            discardPile.clear();
            Collections.shuffle(drawPile, random);
        }
        if (drawPile.size() == 0) {
            return "W";
        }
        return drawPile.remove(0);
    }

    void discard(String card) {
        discardPile.add(card);
    }

    boolean hasCards() {
        return drawPile.size() + discardPile.size() > 0;
    }

    // Number of cards currently in the draw pile. Used by tests to verify deck composition.
    int drawPileSize() {
        return drawPile.size();
    }
}
