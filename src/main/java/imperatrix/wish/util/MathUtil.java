package imperatrix.wish.util;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class MathUtil {

    public static Location bezierPoint(float t, Location p0, Location p1, Location p2) {
        float a = (1-t)*(1-t);
        float b = 2*(1-t)*t;
        float c = t*t;

        return p0.clone().multiply(a).add(p1.clone().multiply(b)).add(p2.clone().multiply(c));
    }

    public static List<Location> bezierCurve(int segmentCount, Location p0, Location p1, Location p2) {
        List<Location> points = new ArrayList<>();
        for (int i = 1; i < segmentCount; i++) {
            float t = i / (float) segmentCount;
            points.add(bezierPoint(t, p0, p1, p2));
        }
        return points;
    }

    public static List<Location> circle(Location start, double radius, boolean hollow) {
        List<Location> locations = new ArrayList<>();

        if (hollow) {
            return circle(start, radius);
        }

        for (double i = 0.1; i < radius; i += 0.1) {
            locations.addAll(circle(start, i));
        }

        return locations;
    }

    public static List<Location> circle(Location start, double radius) {
        List<Location> locations = new ArrayList<>();

        for (double i = 0; i < Math.PI * 2; i += 0.05) {
            double xOffset = radius * Math.cos(i);
            double zOffset = radius * Math.sin(i);

            locations.add(start.clone().add(xOffset, 0, zOffset));
        }

        return locations;
    }
}
