public class Card {

    static String color(String card) {
        if (card.startsWith("R")) {
            return "R";
        }
        if (card.startsWith("Y")) {
            return "Y";
        }
        if (card.startsWith("G")) {
            return "G";
        }
        if (card.startsWith("B")) {
            return "B";
        }
        return "";
    }

    static String rank(String card) {
        if (card.equals("W")) {
            return "WILD";
        }
        if (card.equals("W4")) {
            return "WILD_DRAW_FOUR";
        }
        if (card.endsWith("S")) {
            return "SKIP";
        }
        if (card.endsWith("R")) {
            return "REVERSE";
        }
        if (card.endsWith("+2")) {
            return "DRAW_TWO";
        }
        return "NUMBER";
    }

    static int number(String card) {
        if (rank(card).equals("NUMBER")) {
            return Integer.parseInt(card.substring(1));
        }
        return -1;
    }

    static int points(String card) {
        String r = rank(card);
        if (r.equals("NUMBER")) {
            return number(card);
        }
        if (r.equals("SKIP") || r.equals("REVERSE") || r.equals("DRAW_TWO")) {
            return 20;
        }
        if (r.equals("WILD") || r.equals("WILD_DRAW_FOUR")) {
            return 50;
        }
        return 0;
    }
}
