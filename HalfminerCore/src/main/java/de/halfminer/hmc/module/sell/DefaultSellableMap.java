package de.halfminer.hmc.module.sell;

import de.halfminer.hmc.CoreClass;
import de.halfminer.hms.manageable.Reloadable;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.StringArgumentSeparator;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.logging.Level;

/**
 * Default implementation of {@link SellableMap}.
 */
public class DefaultSellableMap extends CoreClass implements Reloadable, SellableMap {

    // integers used to determine the dynamic cycle time
    private int cycleTimeSecondsMax;
    private int cycleTimeSecondsMin;
    private int cycleMinPlayerCount;

    private Map<SellableGroup, List<Sellable>> sellables = new HashMap<>();
    private SellCycle currentCycle;
    private BukkitTask currentTask;


    public DefaultSellableMap() {
        // dispatch task to run every minute for storing and cycle expiry checking
        scheduler.runTaskTimer(hmc, new Runnable() {

            private long lastCycleStoreTimestamp = System.currentTimeMillis();

            @Override
            public void run() {

                if (!hasCycle()) {
                    return;
                }

                checkNextCycle();

                // store cycle every ~15 minutes
                long currentTimeStamp = System.currentTimeMillis();
                if (lastCycleStoreTimestamp + 900000 < currentTimeStamp) {
                    lastCycleStoreTimestamp = currentTimeStamp;
                    scheduler.runTaskAsynchronously(hmc, () -> storeCurrentCycle());
                }
            }
        }, 1200L, 1200L);
    }

    @Override
    public void loadConfig() {

        FileConfiguration config = hmc.getConfig();
        sellables = new HashMap<>();

        // read cycle times and values for price determination
        this.cycleTimeSecondsMax = config.getInt("sell.cycleTime.maxMinutes", 200) * 60;
        this.cycleTimeSecondsMin = config.getInt("sell.cycleTime.minMinutes", 50) * 60;
        this.cycleMinPlayerCount = config.getInt("sell.cycleTime.minPlayerCount", 40);
        if (cycleTimeSecondsMax < cycleTimeSecondsMin) {
            cycleTimeSecondsMax = cycleTimeSecondsMin;
        }
        this.cycleTimeSecondsMax = Math.max(cycleTimeSecondsMax, 10);
        this.cycleTimeSecondsMin = Math.max(cycleTimeSecondsMin, 10);
        this.cycleMinPlayerCount = Math.max(cycleMinPlayerCount, 0);

        Map<String, SellableGroup> groups = new HashMap<>();
        for (String groupString : config.getStringList("sell.groups")) {

            StringArgumentSeparator separator = new StringArgumentSeparator(groupString, ',');
            if (!separator.meetsLength(2)) {
                MessageBuilder.create("modSellMapLogGroupInvalid", hmc)
                        .addPlaceholder("%GROUP%", groupString)
                        .logMessage(Level.WARNING);
                continue;
            }

            String groupName = separator.getArgument(0).toLowerCase();
            int amountPerCycle = separator.getArgumentIntMinimum(1, 1);
            int unitsUntilIncrease = 100;
            double priceAdjustMultiplier = 1.5d;

            if (separator.meetsLength(3)) {
                unitsUntilIncrease = separator.getArgumentIntMinimum(2, 1);
                if (separator.meetsLength(4)) {
                    priceAdjustMultiplier = separator.getArgumentDoubleMinimum(3, 1.0d);
                }
            }

            SellableGroup newGroup = new DefaultSellableGroup(
                    groupName, amountPerCycle, unitsUntilIncrease, priceAdjustMultiplier
            );
            groups.put(groupName, newGroup);
        }

        if (groups.isEmpty()) {
            currentCycle = null;
            return;
        }

        // read sellables from config
        for (String groupString : config.getConfigurationSection("sell.sellables").getKeys(false)) {
            String groupStringLower = groupString.toLowerCase();

            SellableGroup group;
            if (groups.containsKey(groupStringLower)) {
                group = groups.get(groupStringLower);
            } else {
                continue;
            }

            for (String sellable : config.getStringList("sell.sellables." + groupString)) {
                StringArgumentSeparator separator = new StringArgumentSeparator(sellable, ',');
                if (!separator.meetsLength(3)) {
                    MessageBuilder.create("modSellMapLogSellableInvalidFormat", hmc)
                            .addPlaceholder("%SELLABLE%", sellable)
                            .logMessage(Level.WARNING);
                    continue;
                }

                Material material = Material.matchMaterial(separator.getArgument(1));
                if (material != null) {

                    String messageName = separator.getArgument(0);
                    int baseUnitAmount = separator.getArgumentIntMinimum(2, 1);

                    Sellable currentSellable = new DefaultSellable(
                            group, material, messageName, baseUnitAmount
                    );

                    if (sellables.containsKey(group)) {
                        sellables.get(group).add(currentSellable);
                    } else {
                        List<Sellable> newList = new ArrayList<>();
                        newList.add(currentSellable);
                        sellables.put(group, newList);
                    }

                } else {
                    MessageBuilder.create("modSellMapLogMaterialNotExists", hmc)
                            .addPlaceholder("%MATERIAL%", separator.getArgument(1))
                            .logMessage(Level.WARNING);
                }
            }
        }

        if (sellables.isEmpty()) {
            currentCycle = null;
            return;
        }

        if (!hasCycle()) {

            // read old cycle from storage if possible (persistance after full reloads/restarts)
            Object cycleSectionObject = coreStorage.get("sellcycle");
            if (cycleSectionObject instanceof ConfigurationSection) {

                ConfigurationSection cycleSection = (ConfigurationSection) cycleSectionObject;
                long cycleExpiry = cycleSection.getLong("expires");
                currentCycle = new DefaultSellCycle(cycleExpiry);

                if (currentCycle.getSecondsTillExpiry() > 0) {

                    for (String key : cycleSection.getKeys(false)) {

                        if (key.equals("expires")) {
                            continue;
                        }

                        String group = cycleSection.getString(key + ".group");
                        Material material = Material.matchMaterial(cycleSection.getString(key + ".material", ""));
                        String stateString = cycleSection.getString(key + ".state");

                        if (material != null && groups.containsKey(group)) {
                            List<Sellable> sellablesInGroup = sellables.get(groups.get(group));
                            for (Sellable sellable : sellablesInGroup) {
                                if (sellable.getMaterial().equals(material)) {
                                    sellable.setState(stateString);
                                    currentCycle.addSellableToCycle(sellable);
                                    break;
                                }
                            }
                        }
                    }

                    logCurrentCycle();
                }

            } else if (cycleSectionObject != null) {
                MessageBuilder.create("modSellMapLogCycleInvalidFormat", hmc).logMessage(Level.WARNING);
            }

            checkNextCycle();

        } else /* if (hasCycle()) */ {

            // cycle was already loaded, update newly loaded sellables with old cycle data and create new cycle
            long expiryOldCycle = currentCycle.getExpiryTimestamp();
            List<Sellable> oldCycle = currentCycle.getSellables();

            currentCycle = new DefaultSellCycle(expiryOldCycle);
            for (Sellable sellableOldCycle : oldCycle) {

                if (sellables.containsKey(sellableOldCycle.getGroup())) {

                    for (Sellable sellable : sellables.get(sellableOldCycle.getGroup())) {
                        if (sellable.isSimiliar(sellableOldCycle)) {
                            sellable.copyStateFromSellable(sellableOldCycle);
                            currentCycle.addSellableToCycle(sellable);
                            break;
                        }
                    }
                }
            }
        }
    }

    @Override
    public SellCycle getCurrentCycle() {
        return currentCycle;
    }

    @Override
    public boolean hasCycle() {
        return currentCycle != null && !currentCycle.isEmpty();
    }

    @Override
    public Sellable getSellableAtSlot(int slotId) {
        return hasCycle() ? currentCycle.getSellableAtSlot(slotId) : null;
    }

    @Override
    public Sellable getSellableFromItemStack(ItemStack item) {
        return currentCycle.getSellable(item.getType());
    }

    @Override
    public void storeCurrentCycle() {
        if (hasCycle()) {
            currentCycle.storeCurrentCycle(coreStorage);
        }
    }

    @Override
    public void createNewCycle() {

        if (sellables.isEmpty()) {
            return;
        }

        // determine top seller and amount in last cycle, will be passed via event
        Sellable sellableSoldMost = null;
        UUID uuidSoldMost = null;
        int amountSoldMost = 0;
        if (hasCycle()) {
            for (Sellable sellable : currentCycle.getSellables()) {
                Map.Entry<UUID, Integer> playerAmountPair = sellable.soldMostBy();
                if (playerAmountPair != null
                        && (sellableSoldMost == null
                        || sellable.soldMostBy().getValue() > amountSoldMost)) {
                    sellableSoldMost = sellable;
                    uuidSoldMost = playerAmountPair.getKey();
                    amountSoldMost = playerAmountPair.getValue();
                }
            }

            currentCycle.getSellables().forEach(Sellable::doReset);
        }

        // dynamically determine time until next cycle
        long timeUntilNextCycle;
        int currentPlayerCount = server.getOnlinePlayers().size();
        if (currentPlayerCount == 0) {
            timeUntilNextCycle = cycleTimeSecondsMax;
        } else if (currentPlayerCount >= cycleMinPlayerCount) {
            timeUntilNextCycle = cycleTimeSecondsMin;
        } else {
            int difference = cycleTimeSecondsMax - cycleTimeSecondsMin;
            int toTakeFromMax = (int) (difference * ((double) currentPlayerCount / cycleMinPlayerCount));
            timeUntilNextCycle = cycleTimeSecondsMax - toTakeFromMax;
        }

        currentCycle = new DefaultSellCycle((System.currentTimeMillis() / 1000) + timeUntilNextCycle);

        // randomly determine cycle items
        Random rnd = new Random();
        for (SellableGroup group : sellables.keySet()) {

            Set<Sellable> sellablesInGroup = new HashSet<>(sellables.get(group));
            for (int leftToAdd = Math.min(group.getAmountPerCycle(), sellablesInGroup.size()); leftToAdd > 0; leftToAdd--) {

                int currentElement = 0;
                int elementToGet = rnd.nextInt(sellablesInGroup.size());

                Iterator<Sellable> iterator = sellablesInGroup.iterator();
                while (iterator.hasNext()) {
                    Sellable current = iterator.next();
                    if (elementToGet == currentElement) {
                        currentCycle.addSellableToCycle(current);
                        iterator.remove();
                        break;
                    }
                    currentElement++;
                }
            }
        }

        cancelCurrentTask();
        logCurrentCycle();
        storeCurrentCycle();
        server.getPluginManager().callEvent(
                new SellCycleRefreshEvent(timeUntilNextCycle, sellableSoldMost, uuidSoldMost, amountSoldMost)
        );
    }

    private void checkNextCycle() {
        if (hasCycle()) {
            long expiresInSeconds = currentCycle.getSecondsTillExpiry();
            if (expiresInSeconds < 1) {
                createNewCycle();
            } else if (expiresInSeconds < 60) {
                runTaskLater(this::createNewCycle, expiresInSeconds);
            } else if (expiresInSeconds < 120) {
                runTaskLater(() -> MessageBuilder.create("modSellMapCycleMinuteLeftBroadcast", hmc, "Sell")
                        .broadcastMessage(false), expiresInSeconds - 60);
            }
        } else {
            createNewCycle();
        }
    }

    private void logCurrentCycle() {

        if (hasCycle()) {

            StringBuilder sellableString = new StringBuilder();
            for (Sellable sellable : currentCycle.getSellables()) {
                sellableString.append(sellable.toString()).append(", ");
            }

            sellableString.setLength(sellableString.length() - 2);
            MessageBuilder.create("modSellCurrentCycleLog", hmc)
                    .addPlaceholder("%TIMELEFT%", currentCycle.getSecondsTillExpiry() / 60)
                    .addPlaceholder("%SELLABLES%", sellableString.toString())
                    .logMessage(Level.INFO);
        }
    }

    private void runTaskLater(Runnable runnable, long seconds) {
        cancelCurrentTask();
        currentTask = scheduler.runTaskLater(hmc, runnable, seconds * 20L);
    }

    private void cancelCurrentTask() {
        if (currentTask != null) {
            currentTask.cancel();
            currentTask = null;
        }
    }
}
