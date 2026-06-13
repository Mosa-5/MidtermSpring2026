public class Rules {

    static boolean isLegal(String card, String up, String call) {
        if (card.startsWith("W")) {
            return true;
        }
        if (Card.color(card).equals(Card.color(up))) {
            return true;
        }
        if (!call.equals("") && Card.color(card).equals(call)) {
            return true;
        }
        if (Card.rank(card).equals(Card.rank(up)) && !Card.rank(card).equals("NUMBER")) {
            return true;
        }
        if (Card.rank(card).equals("NUMBER") && Card.rank(up).equals("NUMBER") && Card.number(card) == Card.number(up)) {
            return true;
        }
        return false;
    }
}
