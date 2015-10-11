package de.halfminer.hms.modules;

import de.halfminer.hms.HalfminerSystem;
import de.halfminer.hms.util.Language;
import org.bukkit.event.Listener;

import java.util.LinkedList;

public class ModTps implements HalfminerModule, Listener {

    private final static HalfminerSystem hms = HalfminerSystem.getInstance();

    private int taskId;

    private final LinkedList<Double> tpsHistory = new LinkedList<>();
    private double lastAverageTps;
    private long lastTaskTimestamp;

    //config
    private int ticksBetweenUpdate;
    private int historySize;
    private double alertStaff;

    public ModTps() {
        reloadConfig();
    }

    /**
     * Returns average TPS over last 10 polled values
     *
     * @return Double, average in tpsHistory
     */
    public double getTps() {
        return lastAverageTps;
    }

    @Override
    public void reloadConfig() {

        ticksBetweenUpdate = hms.getConfig().getInt("tps.ticksBetweenUpdate", 100);
        historySize = hms.getConfig().getInt("tps.historySize", 6);
        alertStaff = hms.getConfig().getDouble("tps.alertThreshold", 17.0d);

        if (taskId > 0) hms.getServer().getScheduler().cancelTask(taskId);

        tpsHistory.clear();
        tpsHistory.add(20.0);
        lastAverageTps = 20.0;
        lastTaskTimestamp = System.currentTimeMillis();
        taskId = hms.getServer().getScheduler().scheduleSyncRepeatingTask(hms, new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                long lastUpdate = now - lastTaskTimestamp; //time in milliseconds since last check
                lastTaskTimestamp = now;

                double currentTps = ticksBetweenUpdate * 1000.0 / lastUpdate;

                if (currentTps > 20.0d) return;

                if (tpsHistory.size() >= historySize) tpsHistory.remove(0);
                tpsHistory.add(currentTps);

                //Get average value
                lastAverageTps = 0.0;
                for (Double val : tpsHistory) lastAverageTps += val;
                lastAverageTps /= tpsHistory.size();
                lastAverageTps = Math.round(lastAverageTps * 100.0) / 100.0; //round value to two decimals

                //send message if server is unstable
                if (lastAverageTps < alertStaff && tpsHistory.size() == historySize)
                    hms.getServer().broadcast(Language.getMessagePlaceholderReplace("modTpsServerUnstable", true, "%PREFIX%", "Lag", "%TPS%", String.valueOf(lastAverageTps)), "hms.notifylag");
            }
        }, ticksBetweenUpdate, ticksBetweenUpdate);
    }
}
