package de.halfminer.hml.cmd;

public class Cmdfree extends LandCommand {

    public Cmdfree() {
        super("free");
    }

    @Override
    public void execute() {
        sender.sendMessage("not yet implemented");
        //TODO implement /land free <give/take/reset> <player> [amount]
    }
}
