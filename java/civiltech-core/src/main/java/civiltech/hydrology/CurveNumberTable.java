package civiltech.hydrology;

/**
 * Curve numbers (antecedent runoff condition II) for the ESA WorldCover 2021 land-cover classes.
 *
 * <p>Values are copied from USDA SCS Technical Release 55 (TR-55, 2nd ed., 1986), Tables 2-2a to
 * 2-2c. TR-55 does not use WorldCover classes, so each class is assigned to the TR-55 cover type
 * that describes it best. These assignments are modelling assumptions, listed in
 * {@link #COVER_TYPE}; users can and should change them for their own site.
 *
 * <p>Classes 80 (water) and 90/95 (wetland, mangrove) have no TR-55 entry. Water is given CN 98
 * (treated as an impervious surface, every millimetre of rain becomes runoff after a 1 mm initial
 * abstraction); wetlands are given the TR-55 value for "Pasture, grassland, or range - good".
 * Both are flagged as assumptions.
 */
public final class CurveNumberTable {

    /** Hydrologic soil groups in the column order of the tables below. */
    public static final String[] SOIL_GROUPS = {"A", "B", "C", "D"};

    /** WorldCover codes supported by this table. */
    public static final int[] CLASSES = {10, 20, 30, 40, 50, 60, 70, 80, 90, 95, 100};

    /** TR-55 cover type assumed for each class in {@link #CLASSES}. */
    public static final String[] COVER_TYPE = {
        "Woods, fair hydrologic condition (TR-55 Table 2-2c)",
        "Brush, fair condition (TR-55 Table 2-2c)",
        "Pasture, grassland or range, fair condition (TR-55 Table 2-2c)",
        "Row crops, straight row, good condition (TR-55 Table 2-2b)",
        "Residential, 1/8 acre or less, 65% impervious (TR-55 Table 2-2a)",
        "Fallow, bare soil (TR-55 Table 2-2b)",
        "Not applicable in this region (set equal to bare soil)",
        "ASSUMPTION: open water treated as impervious, CN 98",
        "ASSUMPTION: wetland treated as pasture/grassland, good condition",
        "ASSUMPTION: mangrove treated as woods, good condition",
        "ASSUMPTION: moss/lichen treated as pasture/grassland, fair condition",
    };

    /** CN for soil groups A, B, C, D (rows follow {@link #CLASSES}). */
    private static final int[][] CN = {
        {36, 60, 73, 79},   // woods fair
        {35, 56, 70, 77},   // brush fair
        {49, 69, 79, 84},   // pasture fair
        {67, 78, 85, 89},   // row crops SR good
        {77, 85, 90, 92},   // residential 1/8 acre
        {77, 86, 91, 94},   // fallow bare soil
        {77, 86, 91, 94},   // snow/ice -> bare soil
        {98, 98, 98, 98},   // water (assumption)
        {39, 61, 74, 80},   // wetland -> pasture good (assumption)
        {30, 55, 70, 77},   // mangrove -> woods good (assumption)
        {49, 69, 79, 84},   // moss/lichen -> pasture fair (assumption)
    };

    private CurveNumberTable() { }

    /** Index (0..3) of a hydrologic soil group letter. */
    public static int soilGroupIndex(String group) {
        for (int i = 0; i < SOIL_GROUPS.length; i++) {
            if (SOIL_GROUPS[i].equalsIgnoreCase(group)) {
                return i;
            }
        }
        throw new IllegalArgumentException("soil group must be A, B, C or D, got " + group);
    }

    /** Curve number of a WorldCover class for a soil group (0 = A ... 3 = D). */
    public static int curveNumber(int worldCoverClass, int soilGroup) {
        for (int i = 0; i < CLASSES.length; i++) {
            if (CLASSES[i] == worldCoverClass) {
                return CN[i][soilGroup];
            }
        }
        throw new IllegalArgumentException("unknown WorldCover class " + worldCoverClass);
    }

    /**
     * Lookup array indexed directly by WorldCover code (length 101). Codes that are not WorldCover
     * classes contain 0.
     */
    public static double[] lookup(int soilGroup) {
        double[] t = new double[101];
        for (int i = 0; i < CLASSES.length; i++) {
            t[CLASSES[i]] = CN[i][soilGroup];
        }
        return t;
    }
}
