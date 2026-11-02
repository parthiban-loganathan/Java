// L06: if / else-if / else chains and switch.
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        double rain = sc.nextDouble();          // 24-hour rainfall, mm
        char group = sc.next().charAt(0);       // hydrologic soil group A, B, C or D

        // IMD terminology for 24-hour rainfall at a station
        String category;
        if (rain < 0) {
            category = "Invalid (negative rainfall)";
        } else if (rain == 0) {
            category = "No rain";
        } else if (rain <= 2.4) {
            category = "Very light rain";
        } else if (rain <= 15.5) {
            category = "Light rain";
        } else if (rain <= 64.4) {
            category = "Moderate rain";
        } else if (rain <= 115.5) {
            category = "Heavy rain";
        } else if (rain <= 204.4) {
            category = "Very heavy rain";
        } else {
            category = "Extremely heavy rain";
        }
        System.out.println(rain + " mm in 24 h: " + category);

        // NRCS hydrologic soil groups (NEH Part 630, Chapter 7)
        String description;
        switch (Character.toUpperCase(group)) {
            case 'A': description = "low runoff potential (high infiltration)"; break;
            case 'B': description = "moderately low runoff potential"; break;
            case 'C': description = "moderately high runoff potential"; break;
            case 'D': description = "high runoff potential (very low infiltration)"; break;
            default:  description = "not a valid group";
        }
        System.out.println("Soil group " + group + ": " + description);

        boolean warning = rain > 115.5 && (group == 'C' || group == 'D');
        System.out.println("Flood watch: " + (warning ? "YES" : "no"));
        sc.close();
    }
}
