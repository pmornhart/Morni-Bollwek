package net.morni.bollwerk.logic;

/**
 * Gulden: Belohnung für Siege über Gegner und für jede geschaffte Welle. Rein rechnerisch.
 */
public final class Economy {

    /** Startgeld jeder Person (Vorgabe, änderbar in der config.yml). */
    public static final int START = 50;

    private Economy() {
    }

    /** Gulden für einen Gegner: Grundwert + 5 % je Welle. */
    public static int killReward(EnemyType type, int wave) {
        return (int) Math.round(type.reward() * (1 + 0.05 * (Math.max(1, wave) - 1)));
    }

    /** Bonus für jede Person, wenn eine Welle geschafft ist: 15 + 5 × Welle. */
    public static int waveBonus(int wave) {
        return 15 + 5 * Math.max(1, wave);
    }

    /** Teilt eine Belohnung (z. B. Turm-Treffer) auf alle auf – aufgerundet, damit niemand leer ausgeht. */
    public static int share(int total, int players) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.ceil(total / (double) Math.max(1, players));
    }

    /** Geldbeutel einer Person. */
    public static final class Wallet {

        private int balance;
        private int earned;

        public Wallet(int start) {
            this.balance = Math.max(0, start);
        }

        public int balance() {
            return balance;
        }

        /** Insgesamt verdient (ohne Startgeld). */
        public int earned() {
            return earned;
        }

        public void earn(int amount) {
            if (amount > 0) {
                balance += amount;
                earned += amount;
            }
        }

        public boolean canPay(int price) {
            return price >= 0 && balance >= price;
        }

        /** Bezahlen – {@code false} (und nichts passiert), wenn es nicht reicht. */
        public boolean pay(int price) {
            if (!canPay(price)) {
                return false;
            }
            balance -= price;
            return true;
        }
    }
}
