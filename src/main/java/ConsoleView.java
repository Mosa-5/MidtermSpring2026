import java.util.ArrayList;

public class ConsoleView {

    private boolean quiet = false;

    void setQuiet(boolean q) {
        this.quiet = q;
    }

    boolean isQuiet() {
        return quiet;
    }

    void showGameHeader(int gameNumber) {
        if (quiet) return;
        System.out.println("\n=== Game " + gameNumber + " ===");
    }

    void showUpCard(String upCard, String calledColor) {
        if (quiet) return;
        System.out.println("\nUp card: " + upCard + (calledColor.equals("") ? "" : " called " + calledColor));
    }

    void showHand(String name, ArrayList<String> hand) {
        if (quiet) return;
        System.out.println(name + " hand: " + join(hand));
    }

    void announceDraw(String name, String card) {
        if (quiet) return;
        System.out.println(name + " draws " + card);
    }

    void announceInvalidIndex(String name) {
        if (quiet) return;
        System.out.println(name + " selected an invalid index and draws a penalty card.");
    }

    void announceIllegalCard(String name, String card) {
        if (quiet) return;
        System.out.println(name + " tried illegal card " + card + " and draws a penalty card.");
    }

    void announcePlay(String name, String card) {
        if (quiet) return;
        System.out.println(name + " plays " + card);
    }

    void announceColorCall(String name, String calledColor) {
        if (quiet) return;
        System.out.println(name + " calls " + calledColor);
    }

    void announceUno(String name) {
        if (quiet) return;
        System.out.println(name + " says UNO!");
    }

    void announceUnoPenalty(String name) {
        if (quiet) return;
        System.out.println(name + " forgot to call UNO and draws two penalty cards.");
    }

    void announceWin(String name, int points) {
        if (quiet) return;
        System.out.println(name + " wins and scores " + points);
    }

    void announceDrawTwo(String name) {
        if (quiet) return;
        System.out.println(name + " draws two.");
    }

    void announceDrawFour(String name) {
        if (quiet) return;
        System.out.println(name + " draws four.");
    }

    void announceSafetyLimit() {
        if (quiet) return;
        System.out.println("Game stopped at safety limit.");
    }

    void showFinalScores(ArrayList<String> names, int[] scores) {
        System.out.println("\nFinal scores:");
        for (int i = 0; i < names.size(); i++) {
            System.out.println(names.get(i) + ": " + scores[i]);
        }
    }

    void announceChampion(String name, int score) {
        System.out.println("\n" + name + " wins the match with " + score + " points!");
    }

    private String join(ArrayList<String> cards) {
        String out = "";
        for (int i = 0; i < cards.size(); i++) {
            out += i + ":" + cards.get(i);
            if (i < cards.size() - 1) {
                out += " ";
            }
        }
        return out;
    }
}
