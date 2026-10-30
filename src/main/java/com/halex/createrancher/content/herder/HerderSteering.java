package com.halex.createrancher.content.herder;

import net.createmod.catnip.math.AngleHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

// Moves the plunger towards where it's going
public class HerderSteering {

    private static final double EPSILON = 1e-4;
    private static final double MIN_SWEEP_RADIUS = .25;

    public static Vec3 step(Vec3 from, Vec3 to, double minTravelHeight, double axisX, double axisZ, double distance) {
        double travelHeight = Math.max(minTravelHeight, to.y);
        Vec3 position = from;
        double budget = distance;

        for (int stage = 0; stage < 3 && budget > EPSILON; stage++) {
            double sideways = horizontalDistance(position, to);
            if (sideways > EPSILON) {
                if (position.y < travelHeight - EPSILON) {
                    double rise = Math.min(budget, travelHeight - position.y);
                    position = position.add(0, rise, 0);
                    budget -= rise;
                    continue;
                }
                if (sideways <= budget) {
                    position = new Vec3(to.x, position.y, to.z);
                    budget -= sideways;
                    continue;
                }
                return sweepBy(position, to, axisX, axisZ, budget);
            }

            double vertical = to.y - position.y;
            if (Math.abs(vertical) <= budget)
                return to;
            return position.add(0, Math.copySign(budget, vertical), 0);
        }
        return position;
    }

    private static double horizontalDistance(Vec3 from, Vec3 to) {
        return Math.sqrt((to.x - from.x) * (to.x - from.x) + (to.z - from.z) * (to.z - from.z));
    }

    // Moves along the sweep around the turret
    private static Vec3 sweepBy(Vec3 from, Vec3 to, double axisX, double axisZ, double distance) {
        double low = 0;
        double high = 1;
        for (int i = 0; i < 20; i++) {
            double mid = (low + high) / 2;
            if (horizontalDistance(from, sweep(from, to, axisX, axisZ, mid)) > distance)
                high = mid;
            else
                low = mid;
        }
        return sweep(from, to, axisX, axisZ, low);
    }

    // A point part way along the sweep
    private static Vec3 sweep(Vec3 from, Vec3 to, double axisX, double axisZ, double fraction) {
        double fromRadius = radius(from, axisX, axisZ);
        double toRadius = radius(to, axisX, axisZ);
        if (fromRadius < MIN_SWEEP_RADIUS || toRadius < MIN_SWEEP_RADIUS)
            return new Vec3(Mth.lerp(fraction, from.x, to.x), from.y, Mth.lerp(fraction, from.z, to.z));
        double radius = Mth.lerp(fraction, fromRadius, toRadius);
        double angle = AngleHelper.angleLerp(fraction, angle(from, axisX, axisZ), angle(to, axisX, axisZ))
                * Mth.DEG_TO_RAD;
        return new Vec3(axisX + Math.cos(angle) * radius, from.y, axisZ + Math.sin(angle) * radius);
    }

    private static double radius(Vec3 point, double axisX, double axisZ) {
        return Math.sqrt((point.x - axisX) * (point.x - axisX) + (point.z - axisZ) * (point.z - axisZ));
    }

    private static float angle(Vec3 point, double axisX, double axisZ) {
        return AngleHelper.deg(Mth.atan2(point.z - axisZ, point.x - axisX));
    }
}
