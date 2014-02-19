package eu.mcminers.drali;

/**
 * @author dita
 */
import java.io.Serializable;

public class Island implements Serializable {

  private static final long serialVersionUID = 1L;
  private SimpleSkyblock plugin;
  public int x;
  public int z;
  public int id;
  public long date;
  public String ownerNick;
  public boolean active;
  
  Island(SimpleSkyblock plugin){
    this.plugin = plugin;
  }
  
  Island(int x, int z, SimpleSkyblock plugin){
    this.x = x;
    this.z = z;
    this.plugin = plugin;
  }

  @Override
  public String toString(){
    
    return plugin.out.format("command.info.out", this.ownerNick, this.id, this.x, this.z, this.active);
    //change \n to something that doesn't ake spaces on next line
/*
    String result = "";
    result += "Island owner: " + this.ownerNick + "\n";
    result += "Island ID: " + this.id + "\n";
    result += "Coordinates: [" + this.x + ", " + this.z + "]\n";
    String activity = "no";
    if(this.active){
      activity = "yes";
    }
    result += "Active: " + activity + "\n";
    return result;*/
  }
}