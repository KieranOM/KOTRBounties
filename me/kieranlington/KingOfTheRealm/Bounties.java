package me.kieranlington.KingOfTheRealm;

import static org.bukkit.ChatColor.RED;

import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import me.kieranlington.KingOfTheRealm.realm.Realm;
import me.kieranlington.KingOfTheRealm.realm.RealmHandler;
import me.kieranlington.KingOfTheRealm.utils.Title;
import me.kieranlington.KingOfTheRealm.utils.messages.MessageOptions;
import me.kieranlington.KingOfTheRealm.utils.messages.MessageType;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

public class Bounties extends JavaPlugin implements Listener {
	
	// Instance of KOTR for later usage
	KOTR kotr;
	
	public void onEnable() {
		// If KOTR isn't preset, shout at the console and disable this plugin
		if( getServer().getPluginManager().getPlugin("KOTR") == null ) {
			getLogger().warning("King of The Realm - Not found! Disabling KOTR Bounties...");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		// If an economy can't be established, shout at the console and disable this plugin
		if( !setupEconomy() ) {
			getLogger().warning("Vault - Not found/no economy! Disabling KOTR Bounties...");
            getServer().getPluginManager().disablePlugin(this);
            return;
		}
		
		// Register these events to the server
		Bukkit.getServer().getPluginManager().registerEvents(this, this);
		
		// Get the instance of KOTR from the plugin
		kotr = (KOTR) getServer().getPluginManager().getPlugin("KOTR");
		
		// Copy the default config & save
		getConfig().options().copyDefaults(true);
		saveConfig();
	}
	
	private static Economy econ = null;
	
	private boolean setupEconomy() {
		// If vault isn't found, BAIL!
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        
        // Get the provider of whom has registered the Economy service (any Economy plugin)
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        
        // If there isn't one, BAIL!
        if (rsp == null) return false;
        
        // Set the economy equal to the provider retrieved above
        econ = rsp.getProvider();
        
        // Return whether the economy is in a valid state (successfully registered or not)
        return econ != null;
    }
	
	@SuppressWarnings("deprecation")
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		
		// Get the name of the command stated in the plugin.yml
		final String command = cmd.getName();
		
		// Just an easier way of if(this||that||theother)
		switch (command) {
		
		case "bounties":
			if (sender.hasPermission("kotr.bounties.list")) {
				// We have permission to do this command! :D
				
				// Get the UUIDs of the current bounties from the config.
				Set<String> bounties = getConfig().getConfigurationSection("active-bounties").getKeys(false);
				
				if (bounties.size() > 0) {
					// There are bounties
					
					// Create the header for the message: Active bounties (X):
					String message = ChatColor.GREEN + "Active bounties (" + bounties.size() + "):";
					// Loop over the UUIDs retrieved from the config above (in string format)
					for (String uuid : bounties) {
						// Convert string key to UUID
						final UUID id = UUID.fromString(uuid);
						
						// Build the message: - NAME $AMOUNT
						message += "\n- " + Utils.UUIDtoName(id) + " $" + getBounty(id);
					}
					// Send the newly created message
					sender.sendMessage(message);
					
				} else {
					// There are no active bounties
					sender.sendMessage(ChatColor.RED + "There are no active bounties.");
				}
				
			} else {
				// We don't have permission (sadface)
				noPermission(sender);
				
			}
			return true;
		
		case "bounty":
			if (sender.hasPermission("kotr.bounties.set")) {
				// We have that dank permission again lads!
				
				// Handle separate commands for the console and the player
				if (sender instanceof Player) {
					// Player command
					
					// Get the player instance from the CommandSender
					final Player player = (Player) sender;
					
					// Check if the command is in the correct format (/bounty <player> <amount>)
					if (args.length > 1) {
						// We are correctly formatted!
						
						// Might as well check the number first to prevent wasted stuff (backwards, I know)...
						try {
							
							final int amount = Integer.valueOf(args[1]);
							final String targetName = args[0];
							
							// Check if the amount is over the minimum required
							if (amount >= getConfig().getInt("minimum-bounty")) {
								// The amount is good
								
								// Check whether the target is actually online
								if (Utils.isOnline(targetName)) {
									// The target is online!
									Player targetPlayer = Bukkit.getPlayer(targetName);
									
									// Get the current player's tax-allocated realm
									// Check for forced permission nodes (kotr.tax-forced.REALMID)
									Realm realm = RealmHandler.getRealmFromForcedPermissionNode(player);
									// If not, get it from their location
									if (realm == null) realm = RealmHandler.getRealmFromPlayer(player);
																		
									// Create the tax amount from the current realm (if applicable)
									final int tax = (realm == null ? 0 : realm.getTax());
																		
									// Calculate the amount cut from the tax
									int taxDeduct = 0;
									if (!realm.isAssociated(player)) {
										taxDeduct = Math.round(amount * (tax / 100.0F));
									}
																		
									// Deduct the player's money by the amount
									EconomyResponse withdraw = econ.withdrawPlayer(player, amount);
									
									// Check if it was all fine and dandy
									if (withdraw.transactionSuccess()) {
										// They had enough money to place the bounty
										
										boolean rulerOnline = false;

										// Deposit the tax deduction into the realm's ruler's bank account
										if (realm.thereIsRuler()) {
											econ.depositPlayer(Bukkit.getOfflinePlayer(realm.getRuler()), taxDeduct);
											
											// Set whether they are online
											rulerOnline = Utils.isOnline(realm.getRuler());
										}
										
										// Place the bounty
										placeBounty(targetPlayer, amount-taxDeduct);
										
										// If the ruler is online, and they're not the one that set the bounty...
										if (rulerOnline && !player.getUniqueId().equals(realm.getRuler())) {
											// Inform them of their newly acquired money
											Bukkit.getPlayer(realm.getRuler()).sendMessage(ChatColor.GOLD + "You have received " + ChatColor.YELLOW + "$" + taxDeduct + ChatColor.GOLD + " from the bounty's tax");
										}
										
									} else {
										// Couldn't afford it
										player.sendMessage(ChatColor.RED + "ERROR: You can't afford this bounty");
									}
									
								} else {
									// The target isn't online...
									
									// Use KOTR's fancy message-sending-placeholding-next-generation-game-engine-thingy thing
									kotr.sendMessage(MessageType.TARGET_OFFLINE, player, new MessageOptions().targetName(targetName));
								}
								
							} else {
								// CHEAPSKATE!
								
								// Tell them how cheap they are, in a polite way of course
								player.sendMessage(ChatColor.RED + "You cannot place a bounty under $" + getConfig().getInt("minimum-bounty"));
							}
							
						} catch (NumberFormatException e) {
							// Caught the exception, now tell them off!
							player.sendMessage(ChatColor.RED + "ERROR: Invalid value");
							
						}
						
					} else {
						// NOPE, not correctly formatted...
						player.sendMessage(ChatColor.RED + "Usage: /bounty <player> <amount>");
					}
					
				} else {
					// Console command (same as above but without the realm/tax check)
					// Yes, I could have done this without the separation but just for readability's sake, and none-bodgeness, I decided not to
					if (args.length > 1) {

						try {
							
							final int amount = Integer.valueOf(args[1]);
							final String targetName = args[0];
							
							if (amount >= getConfig().getInt("minimum-bounty")) {
								
								if (Utils.isOnline(targetName)) {
									Player targetPlayer = Bukkit.getPlayer(targetName);
									placeBounty(targetPlayer, amount);
								} else {
									kotr.sendMessage(MessageType.TARGET_OFFLINE, sender, new MessageOptions().targetName(targetName));
								}
								
							} else {
								sender.sendMessage(ChatColor.RED + "You cannot place a bounty under $" + getConfig().getInt("minimum-bounty"));
							}
							
						} catch (NumberFormatException e) {
							sender.sendMessage(ChatColor.RED + "ERROR: Invalid value");
						}
						
					} else {
						sender.sendMessage(ChatColor.RED + "Usage: /bounty <player> <amount>");
					}
					
				}
				
			} else {
				// We don't have permission, again, (sadface)
				noPermission(sender);
			}
			
		}
		
		return true;
	} 

	private void placeBounty(Player placed, Integer amount) {
		// Get the UUID from the placed player
		final UUID uuid = placed.getUniqueId();
		
		// If the player already has a bounty, add it to the current one
		if (hasBounty(placed)) getConfig().set("active-bounties." + uuid, getBounty(uuid) + amount);
		
		// Otherwise, just set it
		else getConfig().set("active-bounties." + uuid, amount);
		
		// Save the changes to the config
		saveConfig();
		
		// Send a broadcast
		Bukkit.getServer().broadcastMessage(ChatColor.DARK_GREEN + "A " + ChatColor.GREEN + "bounty" + ChatColor.DARK_GREEN + " of " + ChatColor.GREEN + "$" + amount + ChatColor.DARK_GREEN + " has been placed on " + ChatColor.GREEN + placed.getName());
		
		// Send a title to the placed player, using the stay times from KOTR
		new Title("&cWatch out!", "&4Someone placed a bounty on you...", KOTR.getOptions().misc.titleStay * 20).send(placed);
		
		// Play the anvil landing sound, using the 1.8 Sound enum fallback
		try {
			placed.playSound(placed.getLocation(), Sound.valueOf("BLOCK_ANVIL_LAND"), 1, 1);
		} catch (IllegalArgumentException e) {
			placed.playSound(placed.getLocation(), Sound.valueOf("ANVIL_LAND"), 1, 1);
		}
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler
	private void onDeath(PlayerDeathEvent e) {
		// Get the player who died
		final Player killed = e.getEntity();
		
		// Check whether they have a bounty
		if (hasBounty(killed)) {
			
			// Create a null instance of a Player so that it can be figured out later
			Player killer = null;
			
			// Try your hardest to get the Player instance from the killer
			if (killed.getKiller() instanceof Player) {
				killer = (Player) killed.getKiller();
			} else {
				if (killed.getKiller() instanceof Projectile) {
					final Projectile shot = (Projectile) killed.getKiller();
					if (shot.getShooter() instanceof Player) {
						killer = (Player) shot.getShooter();
					}
				}
			}
			
			// Make the killer final for the 1 tick delay
			final Player killerPlayer = killer;
			
			// Check whether the killer is actually something AND the killer is not the same guy who got killed
			// Using De Morgan's Law, in proposition logic, to deduce !(A || B) from (!A && !B) because Computer Science course
			if (!(killer == null || killer == killed)) {
				
				// Get the reward
				final int reward = getBounty(killed.getUniqueId());
				
				// Broadcast the claim after 1 tick
				Bukkit.getScheduler().scheduleAsyncDelayedTask(this, new BukkitRunnable() {

					@Override
					public void run() {
						
						Bukkit.getServer().broadcastMessage(ChatColor.GOLD + killerPlayer.getName() + " has claimed the $" + reward +" bounty on " + killed.getName());
						
					}
					
				});
				
				// Remove the bounty & save
				getConfig().set("active-bounties." + killed.getUniqueId(), null);
				saveConfig();
				
				// Deposit the money into the killer's bank account
				econ.depositPlayer(killer, reward);

				// Play the level-up sound, using the 1.8 Sound enum fallback
				try {
					killer.playSound(killer.getLocation(), Sound.valueOf("ENTITY_PLAYER_LEVELUP"), 1, 1);
				} catch (IllegalArgumentException ex) {
					killer.playSound(killer.getLocation(), Sound.valueOf("LEVEL_UP"), 1, 1);
				}
				
				// Handle head-drops
				if (getConfig().getBoolean("heads.drop")) {
					
					// Prevent players w/ the kotr.bounties.drop-prevent from dropping their heads
					if (!killed.hasPermission("kotr.bounties.head-drop")) {
						
						// Generate a new random up to 100, use this to check it against the specified percentage chance
						if (new Random().nextInt(100) <= getConfig().getInt("heads.chance")) {
							
							// Create new skull ItemStack
							ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) SkullType.PLAYER.ordinal());
			                   
		                    SkullMeta meta = (SkullMeta) skull.getItemMeta();
		                    meta.setOwner(killed.getName());
		                    meta.setDisplayName(ChatColor.GREEN + killed.getName() + "'s Head " + ChatColor.GRAY + "($" + reward + " Bounty)");
		                    skull.setItemMeta(meta);
							
							// Get location of eyes (close enough to the head)
							final Location loc = killed.getLocation();
							final Location eyes = loc.add(0, killed.getEyeHeight(), 0);
													
							// Drop the item
							eyes.getWorld().dropItemNaturally(eyes, skull);
							
						}
					
					}
					
				}
			}
			
		}
	}
	
	public boolean hasBounty(Player player) {
		// Return whether this part of the config has been set (player has a bounty)
		return (getConfig().isSet("active-bounties." + player.getUniqueId()));
	}
	
	public int getBounty(UUID uuid) {
		// Returns the value
		return getConfig().getInt("active-bounties." + uuid);
	}
	
	private void noPermission(CommandSender sender) {
		sender.sendMessage(RED + "I'm sorry, you do not have permission for this command...");
	}
}
