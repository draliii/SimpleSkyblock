package eu.mcminers.drali;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 *
 * @author dita
 */
public class SkyAdmin implements CommandExecutor{

  private final SimpleSkyblock plugin;

  public SkyAdmin(SimpleSkyblock plugin) {
    this.plugin = plugin;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    plugin.getLogger().info("Yay, someone used the sbadmin command!");
    return true;
  }
}
