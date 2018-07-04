package de.halfminer.hml.land.contract;

import de.halfminer.hml.land.Land;
import org.bukkit.entity.Player;

/**
 * Implementing class stores and handles {@link AbstractContract} for buying/selling of {@link Land}.
 */
public interface ContractManager {

    /**
     * @param player player to check
     * @return true if given player has a active contract
     */
    boolean hasContract(Player player);

    /**
     * Store a contract.
     *
     * @param contract which contract should be stored
     */
    void setContract(AbstractContract contract);

    /**
     * Get the current contract for a given land.
     *
     * @param player contract owner
     * @param land current land
     * @return contract if exists for given land, else null
     */
    AbstractContract getContract(Player player, Land land);

    /**
     * Calls {@link AbstractContract#fulfill(Land)}.
     *
     * @param contract to fulfill
     */
    void fulfillContract(AbstractContract contract);
}
