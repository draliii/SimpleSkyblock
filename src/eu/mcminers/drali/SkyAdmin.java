package eu.mcminers.drali;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author dita
 */
public class SkyAdmin implements CommandExecutor {

  private final SimpleSkyblock plugin;

  public SkyAdmin(SimpleSkyblock plugin) {
    this.plugin = plugin;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 0) {
      sender.sendMessage(plugin.out.get("command.error.unknown"));
      return true;
    }
    else {
      boolean isConsole = !(sender instanceof Player);
      switch (args[0]) {
        case "home":
          if (args.length == 2) {
            if (isConsole) {
              plugin.print("Sorry, this command is not available in the console", false, "info");
              return true;
            }
            try {
              plugin.skyCommand.cmdHomeFriend((Player) sender, args[1]);
            }
            catch (Exception e) {
              sender.sendMessage("Sorry, Exception");
              e.printStackTrace();
            }
          }
          break;
        case "clearcache":
          if(!isConsole){
            if(!plugin.checkPerk((Player)sender, "simpleskyblock.admin.clearcache")){
              sender.sendMessage("You don't have permission");
              return true;
            }
          }
          plugin.playerIslands.clear();
          break;


      }
    }
    return true;
  }
}
