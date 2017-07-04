package de.halfminer.hmc.module.sell;

import de.halfminer.hmc.CoreClass;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.StringArgumentSeparator;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.logging.Level;

/**
 * Default implementation of {@link SellableMap}.
 */
public class DefaultSellableMap extends CoreClass implements SellableMap {

    // integers used to determine the dynamic cycle time
    private int cycleTimeSecondsMax;
    private int cycleTimeSecondsMin;
    private int cycleMinPlayerCount;

    // sell data and price determination, passed to sellables
    private double priceAdjustMultiplier;
    private double priceVarianceFactor;
    private int unitsUntilIncrease;

    private Map<Integer, List<Sellable>> sellables = new HashMap<>();
    private SellCycle currentCycle;


    public DefaultSellableMap() {
        super(false);

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
    public double getPriceAdjustMultiplier() {
        return priceAdjustMultiplier;
    }

    @Override
    public double getPriceVarianceFactor() {
        return priceVarianceFactor;
    }

    @Override
    public int getUnitsUntilIncrease() {
        return unitsUntilIncrease;
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
        return currentCycle.getMatchingSellable(item.getType(), item.getDurability());
    }

    @Override
    public void configReloaded(ConfigurationSection sellableSection,
                               int cycleTimeSecondsMax, int cycleTimeSecondsMin, int cycleMinPlayerCount,
                               double priceAdjustMultiplier, double priceVarianceFactor, int unitsUntilIncrease) {

        this.priceAdjustMultiplier = Math.max(priceAdjustMultiplier, 0.01d);
        this.priceVarianceFactor = Math.max(priceVarianceFactor, 0.0d);
        this.unitsUntilIncrease = Math.max(unitsUntilIncrease, 1);

        this.cycleTimeSecondsMax = Math.max(cycleTimeSecondsMax, 10);
        this.cycleTimeSecondsMin = Math.max(cycleTimeSecondsMin, 10);
        this.cycleMinPlayerCount = Math.max(cycleMinPlayerCount, 0);

        // read sellables from config
        sellables = new HashMap<>();
        for (String group : sellableSection.getKeys(false)) {
            int groupAsInt;
            try {
                groupAsInt = Integer.parseInt(group);
            } catch (NumberFormatException e) {
                MessageBuilder.create("modSellMapLogGroupInvalid", hmc)
                        .addPlaceholderReplace("%GROUP%", group)
                        .logMessage(Level.WARNING);
                continue;
            }

            for (String sellable : sellableSection.getStringList(group)) {
                StringArgumentSeparator separator = new StringArgumentSeparator(sellable, ',');
                if (!separator.meetsLength(4)) {
                    MessageBuilder.create("modSellMapLogSellableInvalidFormat", hmc)
                            .addPlaceholderReplace("%SELLABLE%", sellable)
                            .logMessage(Level.WARNING);
                    continue;
                }

                Material material = Material.matchMaterial(separator.getArgument(1));
                short durability = (short) separator.getArgumentIntMinimum(2, -1);
                String messageName = separator.getArgument(0);
                int baseUnitAmount = separator.getArgumentIntMinimum(3, 1);

                if (material != null) {
                    Sellable currentSellable = new DefaultSellable(
                            this, groupAsInt, material, durability, messageName, baseUnitAmount
                    );

                    if (sellables.containsKey(currentSellable.getGroupId())) {
                        sellables.get(currentSellable.getGroupId()).add(currentSellable);
                    } else {
                        List<Sellable> newList = new ArrayList<>();
                        newList.add(currentSellable);
                        sellables.put(currentSellable.getGroupId(), newList);
                    }
                } else {
                    MessageBuilder.create("modSellMapLogMaterialNotExists", hmc)
                            .addPlaceholderReplace("%MATERIAL%", separator.getArgument(1))
                            .logMessage(Level.WARNING);
                }
            }
        }

        // only proceed if any sellables were loaded
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

                        int groupId = cycleSection.getInt(key + ".groupId");
                        Material material = Material.matchMaterial(cycleSection.getString(key + ".material", ""));
                        short durability = (short) cycleSection.getInt(key + ".durability");
                        String stateString = cycleSection.getString(key + ".state");

                        List<Sellable> sellablesInGroup = sellables.get(groupId);
                        if (sellablesInGroup != null && material != null) {

                            for (Sellable sellable : sellablesInGroup) {
                                if (sellable.getMaterial().equals(material) && sellable.getDurability() == durability) {
                                    sellable.setState(stateString);
                                    currentCycle.addSellableToCycle(sellable);
                                    break;
                                }
                            }
                        }
                    }
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

                for (Sellable sellable : sellables.get(sellableOldCycle.getGroupId())) {
                    if (sellable.isSimiliar(sellableOldCycle)) {
                        sellable.copyStateFromSellable(sellableOldCycle);
                        currentCycle.addSellableToCycle(sellable);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void storeCurrentCycle() {
        currentCycle.storeCurrentCycle(coreStorage);
    }

    @Override
    public void createNewCycle() {

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

            currentCycle.getSellables().forEach(Sellable::doRandomReset);
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
        for (Integer amount : sellables.keySet()) {

            Set<Sellable> group = new HashSet<>(sellables.get(amount));
            for (int leftToAdd = Math.min(amount, group.size()); leftToAdd > 0; leftToAdd--) {

                int currentElement = 0;
                int elementToGet = rnd.nextInt(group.size());

                Iterator<Sellable> iterator = group.iterator();
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
            } else if (expiresInSeconds < 120 && expiresInSeconds >= 60) {
                runTaskLater(() -> MessageBuilder.create("modSellMapCycleMinuteLeftBroadcast", hmc, "Sell")
                        .broadcastMessage(false), expiresInSeconds - 60);
            }
        } else {
            createNewCycle();
        }
    }

    private void runTaskLater(Runnable runnable, long seconds) {
        scheduler.runTaskLater(hmc, runnable, seconds * 20L);
    }
}
