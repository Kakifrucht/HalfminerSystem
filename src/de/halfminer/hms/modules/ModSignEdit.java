package de.halfminer.hms.modules;

import de.halfminer.hms.util.Language;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;

public class ModSignEdit implements HalfminerModule, Listener {

    private final Map<Player, EditInfo> edit = new HashMap<>();

    public ModSignEdit() {
        reloadConfig();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    @SuppressWarnings("unused")
    public void onInteract(PlayerInteractEvent e) {

        if (edit.isEmpty() || !edit.containsKey(e.getPlayer())) return;

        Player player = e.getPlayer();
        Block block = e.getClickedBlock();
        if (block != null && (block.getType() == Material.SIGN || block.getType() == Material.SIGN_POST || block.getType() == Material.WALL_SIGN)) {

            Sign sign = (Sign) block.getState();
            EditInfo info = edit.get(player);

            if (info.amountToCopy > 0 && info.copySign == null) {

                info.copySign = sign.getLines();
                player.sendMessage(Language.getMessagePlaceholderReplace("signeditSignCopied", true, "%PREFIX%", "Hinweis"));

            } else {

                if (info.amountToCopy > 0) {
                    for (int i = 0; i < 4; i++) sign.setLine(i, info.copySign[i]);
                    if (--info.amountToCopy == 0) edit.remove(player);
                    player.sendMessage(Language.getMessagePlaceholderReplace("signeditSignPasted", true, "%PREFIX%", "Hinweis", "%AMOUNT%", Integer.toString(info.amountToCopy)));
                } else {
                    sign.setLine(info.numberEdit, info.signText);
                    player.sendMessage(Language.getMessagePlaceholderReplace("signeditLinePasted", true, "%PREFIX%", "Hinweis"));
                    edit.remove(player);
                }

                sign.update();

            }

            e.setCancelled(true);
        }
    }

    public void makeCopies(Player player, int amount) {
        edit.put(player, new EditInfo(amount));
    }

    public void setLine(Player player, byte line, String toEdit) {
        edit.put(player, new EditInfo(line, toEdit));
    }

    @Override
    public void reloadConfig() {
    }

    private class EditInfo {

        //Single line editing
        byte numberEdit;
        String signText;

        //Sign copy
        int amountToCopy;
        String[] copySign;

        EditInfo(int amountToCopy) {
            this.amountToCopy = amountToCopy;
        }

        EditInfo(byte line, String signText) {
            numberEdit = (byte) (line - 1);
            this.signText = signText;
        }

    }

}
