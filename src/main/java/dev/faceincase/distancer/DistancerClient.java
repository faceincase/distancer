package dev.faceincase.distancer;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.waypoints.TrackedWaypoint;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DistancerClient implements ClientModInitializer {
	public static final String MOD_ID = "distancer";
	private static final Logger LOGGER = LogUtils.getLogger();

	/** A dot counts as "in the middle of the bar" when its bearing is within this many degrees of where you look. */
	private static final double FACING_TOLERANCE_DEG = 6.0;

	/** What we know about one player on the locator bar. */
	static final class Target {
		final Triangulator triangulator = new Triangulator();
		double bearingDeg;
		int color;
		/** Distance in blocks, or NaN when there is no estimate yet. */
		double distance = Double.NaN;
		/** True for a triangulated estimate, false when the game itself knows roughly where the player is. */
		boolean estimated;
		boolean confident;
	}

	private final Map<UUID, Target> targets = new HashMap<>();
	private Identifier lastDimension;

	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
		HudElementRegistry.attachElementAfter(VanillaHudElements.INFO_BAR,
			Identifier.fromNamespaceAndPath(MOD_ID, "distance_box"),
			(graphics, deltaTracker) -> DistanceBox.render(graphics, this));
		LOGGER.info("[Distancer] loaded");
	}

	private void onTick(Minecraft mc) {
		if (mc.player == null || mc.level == null) {
			targets.clear();
			lastDimension = null;
			return;
		}
		Identifier dimension = mc.level.dimension().identifier();
		if (!dimension.equals(lastDimension)) {
			// Coordinates mean something different after a dimension change, so start over.
			targets.clear();
			lastDimension = dimension;
		}

		Vec3 eye = mc.player.getEyePosition();
		UUID self = mc.player.getUUID();
		Map<UUID, Boolean> seen = new HashMap<>();

		TrackedWaypoint.Camera camera = new TrackedWaypoint.Camera() {
			@Override public float yaw() { return 0f; }
			@Override public Vec3 position() { return eye; }
		};

		mc.player.connection.getWaypointManager().forEachWaypoint(mc.player, waypoint -> {
			UUID id = waypoint.id().left().orElse(null);
			if (id == null || id.equals(self)) {
				return;
			}
			// With camera yaw 0 the "angle to camera" is the waypoint's own yaw in degrees.
			double yaw = waypoint.yawAngleToCamera(mc.level, camera, e -> 1.0f);
			if (Double.isNaN(yaw)) {
				return;
			}
			seen.put(id, true);
			Target t = targets.computeIfAbsent(id, k -> new Target());
			t.bearingDeg = yaw;
			t.color = waypoint.icon().color.orElseGet(() -> ARGB.setBrightness(ARGB.color(255, id.hashCode()), 0.9F));

			double distSq = waypoint.distanceSquared(mc.player);
			if (!Double.isInfinite(distSq)) {
				// The game already knows roughly where this player is, nothing to estimate.
				t.triangulator.clear();
				t.distance = Math.sqrt(distSq);
				t.estimated = false;
				t.confident = true;
				return;
			}

			t.estimated = true;
			t.triangulator.add(eye.x, eye.z, yaw);
			Triangulator.Estimate est = t.triangulator.solve();
			if (est == null) {
				t.distance = Double.NaN;
				t.confident = false;
			} else {
				t.distance = Math.hypot(est.x() - eye.x, est.z() - eye.z);
				// Fits the lines well, the lines cross at a decent angle, and the answer is in front of us.
				t.confident = est.ahead() && est.spreadDeg() >= 3.0 && est.rmsError() <= Math.max(5.0, t.distance * 0.1);
			}
		});

		targets.keySet().removeIf(id -> !seen.containsKey(id));
	}

	/** The player whose dot is closest to the middle of the locator bar, or null if none is centred. */
	Target facedTarget(Minecraft mc) {
		float cameraYaw = mc.gameRenderer.mainCamera().yaw();
		Target best = null;
		double bestDiff = FACING_TOLERANCE_DEG;
		for (Target t : targets.values()) {
			double diff = Math.abs(Mth.degreesDifference(cameraYaw, (float) t.bearingDeg));
			if (diff <= bestDiff) {
				bestDiff = diff;
				best = t;
			}
		}
		return best;
	}
}
