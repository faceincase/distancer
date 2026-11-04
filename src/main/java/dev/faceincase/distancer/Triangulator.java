package dev.faceincase.distancer;

import java.util.ArrayList;
import java.util.List;

/**
 * Estimates a point from several bearing lines (a position plus a yaw), in the XZ plane.
 * Minimises the sum of squared perpendicular distances from the point to every line.
 */
public final class Triangulator {
	private static final int MAX_SAMPLES = 16;
	/** Minimum distance you must move before a new sample is worth recording. */
	private static final double MIN_SAMPLE_SPACING = 12.0;

	public record Sample(double x, double z, double yawDeg) {
		double dirX() { return -Math.sin(Math.toRadians(yawDeg)); }
		double dirZ() { return Math.cos(Math.toRadians(yawDeg)); }
	}

	public record Estimate(double x, double z, double rmsError, double spreadDeg, int samples, boolean ahead) {}

	private final List<Sample> samples = new ArrayList<>();

	public void clear() { samples.clear(); }

	public int size() { return samples.size(); }

	/** Adds a sample if the player has moved far enough from the previous one (or the bearing changed a lot). */
	public void add(double x, double z, double yawDeg) {
		if (!samples.isEmpty()) {
			Sample last = samples.get(samples.size() - 1);
			double moved = Math.hypot(x - last.x, z - last.z);
			double turned = Math.abs(wrap(yawDeg - last.yawDeg));
			if (moved < MIN_SAMPLE_SPACING) {
				return;
			}
			if (turned < 0.05) {
				return; // same line again, adds nothing
			}
		}
		samples.add(new Sample(x, z, yawDeg));
		if (samples.size() > MAX_SAMPLES) {
			samples.remove(0);
		}
	}

	/** Returns null until there are two lines that are not (nearly) parallel. */
	public Estimate solve() {
		if (samples.size() < 2) {
			return null;
		}
		double a11 = 0, a12 = 0, a22 = 0, b1 = 0, b2 = 0;
		for (Sample s : samples) {
			// Unit normal to the line direction.
			double nx = s.dirZ(), nz = -s.dirX();
			double d = nx * s.x + nz * s.z;
			a11 += nx * nx;
			a12 += nx * nz;
			a22 += nz * nz;
			b1 += nx * d;
			b2 += nz * d;
		}
		double det = a11 * a22 - a12 * a12;
		if (det < 1e-3) {
			return null; // lines too parallel to intersect reliably
		}
		double x = (a22 * b1 - a12 * b2) / det;
		double z = (a11 * b2 - a12 * b1) / det;

		double sumSq = 0;
		int aheadCount = 0;
		double maxSpread = 0;
		for (Sample s : samples) {
			double nx = s.dirZ(), nz = -s.dirX();
			double dist = nx * (x - s.x) + nz * (z - s.z);
			sumSq += dist * dist;
			if (s.dirX() * (x - s.x) + s.dirZ() * (z - s.z) > 0) {
				aheadCount++;
			}
			for (Sample o : samples) {
				maxSpread = Math.max(maxSpread, Math.abs(wrap(s.yawDeg - o.yawDeg)));
			}
		}
		return new Estimate(x, z, Math.sqrt(sumSq / samples.size()), maxSpread, samples.size(), aheadCount * 2 > samples.size());
	}

	private static double wrap(double deg) {
		deg %= 360.0;
		if (deg >= 180.0) deg -= 360.0;
		if (deg < -180.0) deg += 360.0;
		return deg;
	}
}
