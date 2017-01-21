package de.halfminer.hms.modules;

import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Utils;
import org.bukkit.scheduler.BukkitTask;

import java.util.LinkedList;

/**
 * - Calculates ticks per second
 * - Notifies staff when servers ticks TPS is too low
 */
public class ModTps extends HalfminerModule {

    private final LinkedList<Double> tpsHistory = new LinkedList<>();
    private BukkitTask task;
    private double lastAverageTps;
    private long lastTaskTimestamp;

    // Config
    private int ticksBetweenUpdate;
    private int historySize;
    private double alertStaff;

    /**
     * Returns average TPS over last 10 polled values
     *
     * @return Double, average in tpsHistory
     */
    public double getTps() {
        return lastAverageTps;
    }

    @Override
    public void loadConfig() {

        ticksBetweenUpdate = hms.getConfig().getInt("tps.ticksBetweenUpdate", 100);
        historySize = hms.getConfig().getInt("tps.historySize", 6);
        alertStaff = hms.getConfig().getDouble("tps.alertThreshold", 17.0d);

        tpsHistory.clear();
        tpsHistory.add(20.0);
        lastAverageTps = 20.0;
        lastTaskTimestamp = System.currentTimeMillis();

        if (task != null) task.cancel();
        task = scheduler.runTaskTimer(hms, () -> {
            long now = System.currentTimeMillis();
            long lastUpdate = now - lastTaskTimestamp; // time in milliseconds since last check
            lastTaskTimestamp = now;

            double currentTps = ticksBetweenUpdate * 1000.0 / lastUpdate;

            // disregard peaks due to fluctuations
            if (currentTps > 21.0d) return;

            if (tpsHistory.size() >= historySize) tpsHistory.removeFirst();
            tpsHistory.add(currentTps);

            // Get average value
            lastAverageTps = 0.0;
            for (Double val : tpsHistory) lastAverageTps += val;
            lastAverageTps /= tpsHistory.size();
            lastAverageTps = Utils.roundDouble(lastAverageTps); // round value to two decimals

            // send message if server is unstable
            if (lastAverageTps < alertStaff && tpsHistory.size() == historySize)
                MessageBuilder.create(hms, "modTpsServerUnstable", "Lag")
                        .addPlaceholderReplace("%TPS%", String.valueOf(lastAverageTps))
                        .broadcastMessage("hms.lag.notify", true);
        }, ticksBetweenUpdate, ticksBetweenUpdate);
    }
}
