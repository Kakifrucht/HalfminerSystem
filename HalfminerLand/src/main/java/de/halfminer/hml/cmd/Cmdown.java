package de.halfminer.hml.cmd;

public class Cmdown extends LandCommand {


    public Cmdown() {
        super("own");
    }

    @Override
    public void execute() {
        sender.sendMessage("not yet implemented");
        //TODO implement /land own <-r|-s|playername>
    }
}
