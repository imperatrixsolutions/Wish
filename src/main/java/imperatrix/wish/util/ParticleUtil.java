package imperatrix.wish.util;

import imperatrix.wish.Wish;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class ParticleUtil {

    /**
     * Spawn particles that travel to an end location on a curved path
     *
     * @param plugin Plugin instance for the BukkitRunnable
     * @param start Start location
     * @param end End location
     * @param particle The particle to spawn
     * @param count The amount of particles to spawn per segment
     */
    public static void spawnCurvedLine(Wish plugin, Location start, Location end, Particle particle, int count) {
        Random random = new Random();
        boolean negXOffset = random.nextBoolean();
        boolean negZOffset = random.nextBoolean();
        Location p0 = start.clone();
        Location p2 = end.clone();
        double x = p0.getX() + (Math.random() * (negXOffset ? -1 : 1));
        double y = p0.getY() + (p2.getY() - p0.getY());
        double z = p0.getZ() + (Math.random() * (negZOffset ? -1 : 1));
        Location p1 = new Location(start.getWorld(), x, y, z);
        List<Location> curve = MathUtil.bezierCurve(100, p0, p1, p2);

        new BukkitRunnable() {
            final Iterator<Location> locIterator = curve.iterator();

            @Override
            public void run() {
                if (!locIterator.hasNext()) {
                    cancel();
                }
                Location particleLocation = locIterator.next();

                if (particleLocation.getWorld() == null) {
                    return;
                }

                particleLocation.getWorld().spawnParticle(particle, particleLocation, count);
            }
        }.runTaskTimer(plugin, 0, 1L);
    }

    /**
     * Spawn particles that travel to an end location on a curved path
     *
     * @param plugin Plugin instance for the BukkitRunnable
     * @param start Start location
     * @param end End location
     * @param particle The particle to spawn
     * @param count The amount of particles to spawn per segment
     */
    public static <T>void spawnCurvedLine(Wish plugin, Location start, Location end, Particle particle, T data, int count) {
        Random random = new Random();
        boolean negXOffset = random.nextBoolean();
        boolean negZOffset = random.nextBoolean();
        Location p0 = start.clone();
        Location p2 = end.clone();
        double x = p0.getX() + (Math.random() * (negXOffset ? -1 : 1));
        double y = p0.getY() + (p2.getY() - p0.getY());
        double z = p0.getZ() + (Math.random() * (negZOffset ? -1 : 1));
        Location p1 = new Location(start.getWorld(), x, y, z);
        List<Location> curve = MathUtil.bezierCurve(21, p0, p1, p2);

        new BukkitRunnable() {
            final Iterator<Location> locIterator = curve.iterator();
            final List<Location> particleLocations = new ArrayList<>();

            @Override
            public void run() {
                if (!locIterator.hasNext()) {
                    cancel();
                    return;
                }

                particleLocations.add(locIterator.next());
                for (Location particleLocation : particleLocations) {
                    if (particleLocation.getWorld() == null) {
                        continue;
                    }

                    particleLocation.getWorld().spawnParticle(particle, particleLocation, count, data);
                }
            }
        }.runTaskTimer(plugin, 0, 1L);
    }

    public static <T>void spawnCircle(Location start, double radius, Particle particle, T data, int count, boolean hollow) {
        List<Location> particleLocations = MathUtil.circle(start, radius, hollow);

        for (Location particleLocation : particleLocations) {
            if (particleLocation.getWorld() == null) {
                continue;
            }

            particleLocation.getWorld().spawnParticle(particle, particleLocation, count, data);
        }
    }

    public static <T>void spawnStraightLine(Location start, Location end, Particle particle, T data, int count) {
        Vector dir = end.clone().subtract(start).toVector();

        if (start.getWorld() == null) {
            return;
        }

        for (double i = 0.1; i < start.distance(end); i += 0.1) {
            dir.multiply(i);
            start.add(dir);
            start.getWorld().spawnParticle(particle, start, count, data);
            start.subtract(dir);
            dir.normalize();
        }
    }
}
