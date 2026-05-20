import java.util.ArrayList;

public class ConsoleInput {

    private InputSource in;
    private ConsoleView view;

    public ConsoleInput(InputSource in, ConsoleView view) {
        this.in = in;
        this.view = view;
    }

    int askHuman(ArrayList<String> hand, String upCard, String calledColor) {
        while (true) {
            if (!view.isQuiet()) {
                System.out.print("Choose card index/code or draw: ");
            }
            String input = in.nextLine().trim().toUpperCase();
            if (input.equals("DRAW")) {
                return -1;
            }
            try {
                int index = Integer.parseInt(input);
                if (index >= 0 && index < hand.size()) {
                    return index;
                }
            } catch (Exception ignored) {
            }
            for (int i = 0; i < hand.size(); i++) {
                if (hand.get(i).equals(input)) {
                    if (Rules.isLegal(hand.get(i), upCard, calledColor)) {
                        return i;
                    }
                    if (!view.isQuiet()) {
                        System.out.println("That card is not legal.");
                    }
                }
            }
            if (!view.isQuiet()) {
                System.out.println("Card not found.");
            }
        }
    }

    String askColor() {
        while (true) {
            if (!view.isQuiet()) {
                System.out.print("Call color R/Y/G/B: ");
            }
            String input = in.nextLine().trim().toUpperCase();
            if (input.equals("R")) {
                return "R";
            }
            if (input.equals("Y")) {
                return "Y";
            }
            if (input.equals("G")) {
                return "G";
            }
            if (input.equals("B")) {
                return "B";
            }
            if (!view.isQuiet()) {
                System.out.println("Bad color.");
            }
        }
    }

    boolean askPlayDrawn(String drawn) {
        if (!view.isQuiet()) {
            System.out.print("Play drawn card " + drawn + "? y/n: ");
        }
        String answer = in.nextLine();
        return answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes");
    }
}
