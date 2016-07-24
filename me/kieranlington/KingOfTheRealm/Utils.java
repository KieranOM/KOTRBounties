package me.kieranlington.KingOfTheRealm;

import java.util.Collection;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public interface Utils {
	
	static String UUIDtoName(UUID uuid) {
		if( uuid == null ) return "";
		OfflinePlayer p = Bukkit.getServer().getOfflinePlayer( uuid );
		return p.getName();
	}
	
	static Collection<? extends Player> getOnlinePlayers() {
		return Bukkit.getOnlinePlayers();
	}
	
	static boolean isOnline(UUID uuid) {
		if (uuid == null) return false;
		else return Bukkit.getOfflinePlayer(uuid).isOnline();
	}
	
	static boolean isOnline(Player player) {
		return getOnlinePlayers().contains(player);
	}
	
	static boolean isOnline(String name) {
		for (Player player : getOnlinePlayers()) if (player.getName().equals(name)) return true;
		return false;
	}
	
}
