package de.halfminer.hml.land;

import de.halfminer.hml.LandClass;
import de.halfminer.hml.land.contract.AbstractContract;
import de.halfminer.hml.land.contract.ContractManager;
import de.halfminer.hms.handler.HanStorage;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import de.halfminer.hms.handler.storage.PlayerNotFoundException;
import de.halfminer.hms.manageable.Sweepable;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Pair;
import de.halfminer.hms.util.StringArgumentSeparator;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.stream.Collectors;

public class LandBoard extends LandClass implements Board, ContractManager, Sweepable {

    private static final String ROOT_MAP_PATH = "map";
    private static final int CHUNK_SHOWN_SECONDS = 10;
    private static final int CHUNK_SHOWN_HEIGHT = 4;
    private static final int CHUNK_SHOWN_SPACES = 2;

    private final HanStorage systemStorage;

    private final ConfigurationSection landStorageSection;
    private final LandMap landMap;

    private final Map<Player, BukkitTask> chunkPlayerParticleMap;
    private final Map<Player, AbstractContract> contractMap;


    public LandBoard() {

        this.systemStorage = hms.getStorageHandler();

        this.landStorageSection = hml.getLandStorage().getConfigurationSection(ROOT_MAP_PATH);
        this.landMap = new LandMap();

        this.chunkPlayerParticleMap = new HashMap<>();
        this.contractMap = new HashMap<>();

        // load board from disk
        for (String worldStr : landStorageSection.getKeys(false)) {

            World world = server.getWorld(worldStr);
            if (world == null) {
                hml.getLogger().warning("World " + worldStr + " not loaded, skipping land");
                continue;
            }

            ConfigurationSection worldSection = landStorageSection.getConfigurationSection(worldStr);
            for (String coordinates : worldSection.getKeys(false)) {

                StringArgumentSeparator chunkXZ = new StringArgumentSeparator(coordinates, 'z');
                Chunk chunk = world.getChunkAt(chunkXZ.getArgumentInt(0), chunkXZ.getArgumentInt(1));

                Land land = new Land(chunk, landStorageSection);
                landMap.addLand(land);
            }
        }

        // set locations for online players
        for (Player player : hml.getServer().getOnlinePlayers()) {
            updatePlayerLocation(player, null, player.getLocation().getChunk());
        }

        hml.getLogger().info("Loaded " + getOwnedLandSet().size() + " lands");
    }

    @Override
    public Pair<Land, Land> updatePlayerLocation(Player player, Chunk previousChunk, Chunk newChunk) {

        Land previousLand = null;
        if (previousChunk != null) {

            previousLand = getLandAt(previousChunk);
            boolean canBeRemoved = previousLand.playerLeft();
            if (canBeRemoved) {
                landMap.removeLand(previousLand);
            }

            // don't show marked chunk on world change
            if (newChunk != null
                    && !previousChunk.getWorld().equals(newChunk.getWorld())
                    && chunkPlayerParticleMap.containsKey(player)) {
                cancelChunkParticles(player);
            }
        }

        Land newLand = null;
        if (newChunk != null) {
            newLand = getLandAt(newChunk);
            newLand.playerEntered();
        } else {
            // logout
            cancelChunkParticles(player);
        }

        return new Pair<>(previousLand, newLand);
    }

    @Override
    public Land getLandAt(Player player) {
        return getLandAt(player.getLocation().getChunk());
    }

    @Override
    public Land getLandAt(Location location) {
        return getLandAt(location.getChunk());
    }

    @Override
    public Land getLandAt(Chunk chunk) {

        Land landToReturn = landMap.getLandAt(chunk);
        if (landToReturn == null) {
            landToReturn = new Land(chunk, landStorageSection);
            landMap.addLand(landToReturn);
        }
        return landToReturn;
    }

    @Override
    public Land getLandFromTeleport(String teleportName) {
        return landMap.getLandFromTeleport(teleportName);
    }

    @Override
    public Set<Land> getLands(UUID uuid) {
        try {
            return landMap.getOwnedLandSet(systemStorage.getPlayer(uuid));
        } catch (PlayerNotFoundException e) {
            return Collections.emptySet();
        }
    }

    @Override
    public Set<Land> getLands(Player player) {
        return getLands(player.getUniqueId());
    }

    @Override
    public Set<Land> getLands(HalfminerPlayer player) {
        return getLands(player.getUniqueId());
    }

    @Override
    public Set<Land> getOwnedLandSet() {
        return landMap.getLandCollection()
                .stream()
                .filter(Land::hasOwner)
                .collect(Collectors.toSet());
    }

    @Override
    public void showChunkParticles(Player player, Land landToShow) {

        cancelChunkParticles(player);

        Chunk chunk = landToShow.getChunk();
        World world = chunk.getWorld();
        int chunkX = chunk.getX() * 16;
        int chunkZ = chunk.getZ() * 16;

        String barMessage = MessageBuilder.returnMessage("boardShowChunkBar", hml, false);
        hms.getBarHandler().sendBar(player, barMessage, BarColor.GREEN, BarStyle.SOLID, CHUNK_SHOWN_SECONDS);

        BukkitTask newTask = server.getScheduler().runTaskTimer(hml, new Runnable() {

            private int totalRuns = CHUNK_SHOWN_SECONDS;

            @Override
            public void run() {

                int playerYLevel = player.getLocation().getBlockY();

                // green region particles
                for (int x = 0; x <= 16; x += CHUNK_SHOWN_SPACES) {
                    for (int y = Math.max(0, playerYLevel - CHUNK_SHOWN_HEIGHT); y <= Math.min(255, playerYLevel + CHUNK_SHOWN_HEIGHT); y += 2) {
                        for (int z = 0; z <= 16; z += CHUNK_SHOWN_SPACES) {
                            Block block = world.getBlockAt(chunkX + x, y, chunkZ + z);
                            Particle toSpawn = isCorner(x, z) ? Particle.BARRIER : Particle.VILLAGER_HAPPY;
                            player.spawnParticle(toSpawn, block.getLocation(), 1);
                        }
                    }
                }

                if (--totalRuns == 0) {
                    cancelChunkParticles(player);
                }
            }

            private boolean isCorner(int x, int z) {
                return (x == 0 || x == 16) && (z == 0 || z == 16);
            }

        }, 20L, 20L);

        chunkPlayerParticleMap.put(player, newTask);
    }

    private void cancelChunkParticles(Player toCancel) {
        if (chunkPlayerParticleMap.containsKey(toCancel)) {
            chunkPlayerParticleMap.remove(toCancel).cancel();
            hms.getBarHandler().removeBar(toCancel);
        }
    }

    @Override
    public void landWasUpdated(Land land) {
        landMap.addLand(land);
    }

    @Override
    public boolean hasContract(Player player) {
        if (contractMap.containsKey(player)) {

            AbstractContract contract = contractMap.get(player);

            // check if expired
            if (contract.getCreationTimestamp() + (CHUNK_SHOWN_SECONDS * 1000L) < System.currentTimeMillis()) {
                contractMap.remove(player);
            } else {
                return true;
            }
        }

        return false;
    }

    @Override
    public void setContract(AbstractContract contract) {
        contractMap.put(contract.getPlayer(), contract);
    }

    @Override
    public AbstractContract getContract(Player player, Land land) {

        if (hasContract(player)) {
            AbstractContract contract = contractMap.get(player);
            if (contract.getChunk().equals(land.getChunk())) {
                return contract;
            }
        }

        return null;
    }

    @Override
    public void fulfillContract(AbstractContract contract) {
        Player player = contract.getPlayer();
        Land land = getLandAt(contract.getChunk());

        cancelChunkParticles(player);
        contract.fulfill(land);

        landWasUpdated(land);
        contractMap.remove(player);
    }

    @Override
    public void sweep() {
        for (Player player : new HashSet<>(contractMap.keySet())) {
            hasContract(player);
        }
    }
}
