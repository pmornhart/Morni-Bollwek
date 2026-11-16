package net.morni.bollwerk.logic;

/**
 * Alles im Krämerladen. Waffen und Rüstung gibt es in Stufen ({@code group} + {@code tier}): man kann nur Besseres
 * kaufen, als man schon hat. Baupläne kosten so viel wie ihr Bau ({@link DefenceType#price()}).
 */
public enum Offer {

    EISENSCHWERT("eisenschwert", Category.WAFFEN, 35, "schwert", 2),
    DIAMANTSCHWERT("diamantschwert", Category.WAFFEN, 90, "schwert", 3),
    BOGEN_KRAFT("bogen_kraft", Category.WAFFEN, 45, "bogen", 2),
    BOGEN_MEISTER("bogen_meister", Category.WAFFEN, 90, "bogen", 3),
    PFEILE("pfeile", Category.WAFFEN, 10, null, 0),
    KETTENRUESTUNG("kettenruestung", Category.RUESTUNG, 40, "ruestung", 2),
    EISENRUESTUNG("eisenruestung", Category.RUESTUNG, 80, "ruestung", 3),
    DIAMANTRUESTUNG("diamantruestung", Category.RUESTUNG, 150, "ruestung", 4),
    HEILTRANK("heiltrank", Category.TRAENKE, 15, null, 0),
    STAERKETRANK("staerketrank", Category.TRAENKE, 30, null, 0),
    TEMPOTRANK("tempotrank", Category.TRAENKE, 20, null, 0),
    GOLDAPFEL("goldapfel", Category.TRAENKE, 25, null, 0),
    BAUPLAN_PALISADE("bauplan_palisade", Category.BAUTEN, DefenceType.PALISADE.price(), null, 0),
    BAUPLAN_KESSELFALLE("bauplan_kesselfalle", Category.BAUTEN, DefenceType.KESSELFALLE.price(), null, 0),
    BAUPLAN_PFEILTURM("bauplan_pfeilturm", Category.BAUTEN, DefenceType.PFEILTURM.price(), null, 0),
    BAUPLAN_HEILBANNER("bauplan_heilbanner", Category.BAUTEN, DefenceType.HEILBANNER.price(), null, 0),
    HERZ_FLICKEN("herz_flicken", Category.REPARATUR, 25, null, 0),
    MAUERN_FLICKEN("mauern_flicken", Category.REPARATUR, 0, null, 0);

    /** Abteilungen im Laden. */
    public enum Category {
        WAFFEN, RUESTUNG, TRAENKE, BAUTEN, REPARATUR
    }

    /** Lebenspunkte, die „Herz flicken“ zurückgibt. */
    public static final int HEART_PATCH = 50;

    private final String id;
    private final Category category;
    private final int price;
    private final String group;
    private final int tier;

    Offer(String id, Category category, int price, String group, int tier) {
        this.id = id;
        this.category = category;
        this.price = price;
        this.group = group;
        this.tier = tier;
    }

    public String id() {
        return id;
    }

    public Category category() {
        return category;
    }

    /** Festpreis (Mauern flicken: hängt vom Schaden ab, siehe {@link DefenceType#repairPrice(double)}). */
    public int price() {
        return price;
    }

    /** Stufen-Gruppe („schwert“, „bogen“, „ruestung“) oder null. */
    public String group() {
        return group;
    }

    public int tier() {
        return tier;
    }

    /** Bauplan für welchen Bau (oder null). */
    public DefenceType defence() {
        return switch (this) {
            case BAUPLAN_PALISADE -> DefenceType.PALISADE;
            case BAUPLAN_KESSELFALLE -> DefenceType.KESSELFALLE;
            case BAUPLAN_PFEILTURM -> DefenceType.PFEILTURM;
            case BAUPLAN_HEILBANNER -> DefenceType.HEILBANNER;
            default -> null;
        };
    }

    /** Darf man das bei dieser Ausrüstung kaufen? Stufen nur aufwärts. */
    public boolean allowed(Gear gear) {
        if (group == null) {
            return true;
        }
        return tier > gear.tier(group);
    }

    public static Offer byId(String id) {
        for (Offer o : values()) {
            if (o.id.equalsIgnoreCase(id)) {
                return o;
            }
        }
        return null;
    }

    /** Ausrüstungs-Stufen einer Person (Start: Steinschwert 1, einfacher Bogen 1, Leder 1). */
    public static final class Gear {

        private int sword = 1;
        private int bow = 1;
        private int armour = 1;

        public int tier(String group) {
            return switch (group) {
                case "schwert" -> sword;
                case "bogen" -> bow;
                case "ruestung" -> armour;
                default -> 0;
            };
        }

        /** Kauf übernehmen. */
        public void apply(Offer o) {
            if (o.group == null || !o.allowed(this)) {
                return;
            }
            switch (o.group) {
                case "schwert" -> sword = o.tier;
                case "bogen" -> bow = o.tier;
                case "ruestung" -> armour = o.tier;
                default -> {
                    // nichts
                }
            }
        }
    }
}
