// L12: Map (LinkedHashMap, TreeMap) and Set (TreeSet) for lookups and counting.
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class Main {
    public static void main(String[] args) {
        // Curve numbers, TR-55, hydrologic soil group C (insertion order kept)
        Map<String, Integer> curveNumber = new LinkedHashMap<>();
        curveNumber.put("Tree cover", 73);
        curveNumber.put("Shrubland", 70);
        curveNumber.put("Cropland", 85);
        curveNumber.put("Built-up", 90);
        System.out.println("lookup table: " + curveNumber);
        System.out.println("CN of Cropland = " + curveNumber.get("Cropland"));
        System.out.println("has Water? " + curveNumber.containsKey("Water")
                + ", default for Water = " + curveNumber.getOrDefault("Water", 98));

        // Land cover of cells along a transect
        String[] cells = {"Tree cover", "Tree cover", "Shrubland", "Cropland", "Tree cover",
                          "Built-up", "Shrubland", "Cropland", "Cropland", "Tree cover"};
        Map<String, Integer> counts = new TreeMap<>();     // sorted by class name
        for (String c : cells) {
            counts.put(c, counts.getOrDefault(c, 0) + 1);
        }
        System.out.println("cell counts: " + counts);

        double weighted = 0;
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            weighted += curveNumber.get(e.getKey()) * e.getValue();
        }
        weighted /= cells.length;
        System.out.printf("area-weighted CN = %.1f%n", weighted);

        Set<String> distinct = new TreeSet<>();
        for (String c : cells) {
            distinct.add(c);
        }
        System.out.println("distinct classes: " + distinct + " (" + distinct.size() + ")");
        System.out.println("add Shrubland again -> " + distinct.add("Shrubland"));
    }
}
