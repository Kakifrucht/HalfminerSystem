package de.halfminer.hms.modules;

import de.halfminer.hms.HalfminerSystem;
import org.bukkit.Server;
import org.bukkit.event.Listener;

import java.util.LinkedList;

public class ModTps implements HalfminerModule, Listener {

    private final static HalfminerSystem hms = HalfminerSystem.getInstance();
    private final static Server server = hms.getServer();

    private final LinkedList<Double> tpsHistory = new LinkedList<>();
    private double lastAverageTps = 20.0d;
    private long lastTaskTimestamp = System.currentTimeMillis();
    private int ticksBetweenUpdate;
    private boolean serverIsLaggy;

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

        tpsHistory.clear();
        tpsHistory.add(20.0);
        hms.getServer().getScheduler().scheduleSyncRepeatingTask(hms, new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                long lastUpdate = now - lastTaskTimestamp;

                double currentTps = ticksBetweenUpdate * 1000.0 / lastUpdate;

                if (tpsHistory.size() > 9) tpsHistory.remove(0);
                tpsHistory.add(currentTps);

                //Get average value
                lastAverageTps = 0.0;
                for (Double val : tpsHistory) lastAverageTps += val;
                lastAverageTps /= tpsHistory.size();
                lastAverageTps = Math.round(lastAverageTps * 100.0) / 100.0; //round value to two decimals

                if (serverIsLaggy && lastAverageTps > 18.0d) {
                    server.dispatchCommand(server.getConsoleSender(), "timings paste");
                    server.dispatchCommand(server.getConsoleSender(), "timings off");
                    serverIsLaggy = false;
                    server.broadcast("Server laggt nicht mehr", "hms.notifylag");
                } else if (!serverIsLaggy && lastAverageTps < 15.0) {
                    serverIsLaggy = true;
                    server.broadcast("Server laggt", "hms.notifylag"); //TODO proper messages, plugin.yml
                    server.dispatchCommand(server.getConsoleSender(), "timings on");
                }

                lastTaskTimestamp = now;
            }
        }, ticksBetweenUpdate, ticksBetweenUpdate);
    }
}
