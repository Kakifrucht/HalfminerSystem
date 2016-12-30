package de.halfminer.hms.exception;

import de.halfminer.hms.util.Utils;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Thrown when an {@link de.halfminer.hms.util.CustomitemCache} was not able to give the item, either due to
 * a syntax error in the customitems.txt, because the item does not exist or the inventory did not have enough
 * space left.
 */
public class GiveItemException extends Exception {

    private final Reason reason;
    private Map<Integer, ItemStack> notGiven;

    public GiveItemException(Reason status) {
        this.reason = status;
    }

    public GiveItemException(Map<Integer, ItemStack> notGiven) {
        this.reason = Reason.INVENTORY_FULL;
        this.notGiven = notGiven;
    }

    public Reason getReason() {
        return reason;
    }

    public String getCleanReason() {
        return Utils.makeStringFriendly(reason.toString());
    }

    public Map<Integer, ItemStack> getNotGivenItems() {
        return notGiven;
    }

    public enum Reason {
        INVENTORY_FULL,
        ITEM_NOT_FOUND,
        ITEM_SYNTAX_ERROR
    }
}
