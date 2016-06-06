package com.kotari;

/**
 * Created by fuad on 5/31/16.
 */
public class Tariffs {
    public static final String[] TARIFF_NAMES = {
            "Single Phase",
            "Three Phase",
            "Passive-Reactive"
    };

    public static final int ID_TARIFF_SINGLE_PHASE = 0;
    public static final int ID_TARIFF_THREE_PHASE = 1;
    public static final int ID_TARIFF_PASSIVE_REACTIVE = 2;

    public static final double RATE_SINGLE = 1.4;
    public static final double RATE_THREE = 14.49;
    public static final double RATE_PASSIVE = 1.4;

    public static String getTariffName(int id) {
        if (id > ID_TARIFF_PASSIVE_REACTIVE) return "--UNDEFINED--";
        return TARIFF_NAMES[id];
    }

    public static double getTariffRate(int id) {
        switch (id) {
            case ID_TARIFF_SINGLE_PHASE: return RATE_SINGLE;
            case ID_TARIFF_THREE_PHASE: return RATE_THREE;
            case ID_TARIFF_PASSIVE_REACTIVE: return RATE_PASSIVE;
            default:
                return 0.0;
        }
    }

    public static class TariffInfo {
        String name;
        int id;

        public TariffInfo(String n, int i) { name = n; id = i; }

        @Override
        public String toString() {
            return name;
        }
    }

    public static final TariffInfo TARIFF_SINGLE_PHASE;
    public static final TariffInfo TARIFF_THREE_PHASE;
    public static final TariffInfo TARIFF_PASSIVE_REACTIVE;

    public static final TariffInfo[] TARIFF_ARRAY;
    static {
        TARIFF_SINGLE_PHASE = new TariffInfo(TARIFF_NAMES[ID_TARIFF_SINGLE_PHASE], ID_TARIFF_SINGLE_PHASE);
        TARIFF_THREE_PHASE = new TariffInfo(TARIFF_NAMES[ID_TARIFF_THREE_PHASE], ID_TARIFF_THREE_PHASE);
        TARIFF_PASSIVE_REACTIVE = new TariffInfo(TARIFF_NAMES[ID_TARIFF_PASSIVE_REACTIVE], ID_TARIFF_PASSIVE_REACTIVE);

        TARIFF_ARRAY = new TariffInfo[3];
        TARIFF_ARRAY[0] = TARIFF_SINGLE_PHASE;
        TARIFF_ARRAY[1] = TARIFF_THREE_PHASE;
        TARIFF_ARRAY[2] = TARIFF_PASSIVE_REACTIVE;
    }
}
