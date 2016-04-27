package de.halfminer.hms.handlers;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.api.UserDoesNotExistException;
import de.halfminer.hms.exception.HookException;
import de.halfminer.hms.util.Language;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * - Hooks external plugins
 * - Checks if plugins are loaded
 * - Fast reference to external api
 */
@SuppressWarnings("unused")
public class HanHooks extends HalfminerHandler {

    private final Essentials essentialsHook;
    private Chat vaultChatHook;

    public HanHooks() {
        essentialsHook = (Essentials) server.getPluginManager().getPlugin("Essentials");

        if (server.getPluginManager().getPlugin("Vault") != null) {

            RegisteredServiceProvider<Chat> provider = server.getServicesManager()
                    .getRegistration(net.milkbowl.vault.chat.Chat.class);
            if (provider != null) vaultChatHook = provider.getProvider();
            else hms.getLogger().warning(Language.getMessage("hanHooksLoadChatFailed"));

        } else hms.getLogger().warning(Language.getMessage("hanHooksLoadChatFailed"));
    }

    public Essentials getEssentialsHook() {
        return essentialsHook;
    }

    public void setLastTpLocation(Player player) {
        essentialsHook.getUser(player).setLastLocation();
    }

    public double getMoney(Player player) throws HookException {
        double balance;
        try {
            balance = net.ess3.api.Economy.getMoneyExact(player.getName()).doubleValue();
            return Math.round(balance * 100.0d) / 100.0d;
        } catch (UserDoesNotExistException e) {
            throw new HookException();
        }
    }

    public String getPrefix(Player player) throws HookException {
        if (vaultChatHook == null) throw new HookException();
        return vaultChatHook.getPlayerPrefix(player);
    }

    public String getSuffix(Player player) throws HookException {
        if (vaultChatHook == null) throw new HookException();
        return vaultChatHook.getPlayerSuffix(player);
    }
}