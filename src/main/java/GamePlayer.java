import java.util.ArrayList;

// A single UNO player during play: their name, whether they are human-controlled, the
// cards in hand, their cumulative match score, and whether they have called "UNO" while
// holding one card. Replaces the old index-synced parallel lists in Main.
//
// Note: this is the in-memory game-domain player, distinct from the JPA `Player`
// entity (the persisted players table used by the stats repository).
public class GamePlayer {

    private final String name;
    private final boolean human;
    private final ArrayList<String> hand = new ArrayList<String>();
    private int totalScore = 0;
    private boolean calledUno = false;

    public GamePlayer(String name, boolean human) {
        this.name = name;
        this.human = human;
    }

    public String name() {
        return name;
    }

    public boolean isHuman() {
        return human;
    }

    public ArrayList<String> hand() {
        return hand;
    }

    public int totalScore() {
        return totalScore;
    }

    public void addScore(int points) {
        totalScore += points;
    }

    public boolean hasCalledUno() {
        return calledUno;
    }

    public void setCalledUno(boolean calledUno) {
        this.calledUno = calledUno;
    }
}
