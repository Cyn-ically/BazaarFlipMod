package uwu.ramona.bazaar.command;

import uwu.ramona.bazaar.BazaarFlip;
import cc.polyfrost.oneconfig.utils.commands.annotations.Command;
import cc.polyfrost.oneconfig.utils.commands.annotations.Main;


@Command(value = "bazaarflip", description = "Access the " + BazaarFlip.NAME + " GUI.")
public class OpenCommand {
    @Main
    private void handle() {
        BazaarFlip.INSTANCE.config.openGui();
    }
}
