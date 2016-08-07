package me.kieranlington.KingOfTheRealm.utils;
import java.lang.reflect.Constructor;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import me.kieranlington.KingOfTheRealm.KOTR;
import net.amoebaman.util.Reflection;

public class Title {
	// Default values for titles
	private String title = "";
	private String subtitle = "";
	private int fadeTime = 5;
	private int stayTime = 60;
	
	// Get the NMS classes of the following
	private Class<?> PacketPlayOutTitle = Reflection.getNMSClass("PacketPlayOutTitle");
	private Class<?> IChatBaseComponent = Reflection.getNMSClass("IChatBaseComponent");
	private Class<?> Packet = Reflection.getNMSClass("Packet");
	
	// Title constructor
	public Title(String title, String subtitle) {
		// Convert null to "", and grab '&' colours
		this.title = title == null ? "" : ChatColor.translateAlternateColorCodes('&', title);
		this.subtitle = subtitle == null ? "" : ChatColor.translateAlternateColorCodes('&', subtitle);
		// Get the display time from the KOTR plugin
		this.stayTime = KOTR.getOptions().misc.titleStay * 20;
	}
	
	public void send(Player player) {
		try {
			
			Constructor<?> titleConstructor = PacketPlayOutTitle.getConstructor(PacketPlayOutTitle.getDeclaredClasses()[0], IChatBaseComponent, int.class, int.class, int.class);

			// Construct the packet for the TITLE
			Object enumTitle = PacketPlayOutTitle.getDeclaredClasses()[0].getField("TITLE").get(null);
			Object titleChat = Reflection.getNMSClass("IChatBaseComponent").getDeclaredClasses()[0].getMethod("a", String.class).invoke(null, Utils.getJSON(title) );
			Object titlePacket = titleConstructor.newInstance(enumTitle, titleChat, fadeTime, stayTime, fadeTime);
			
			// Construct the packet for the SUBTITLE
			Object enumSubtitle = PacketPlayOutTitle.getDeclaredClasses()[0].getField("SUBTITLE").get(null);
			Object subtitleChat = Reflection.getNMSClass("IChatBaseComponent").getDeclaredClasses()[0].getMethod("a", String.class).invoke(null, Utils.getJSON(subtitle) );
			Object subtitlePacket = titleConstructor.newInstance(enumSubtitle, subtitleChat, fadeTime, stayTime, fadeTime);
			
			// Send both of the packets to the player
			sendPacket(player, titlePacket);
			sendPacket(player, subtitlePacket);
		}
	   
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void sendPacket(Player player, Object packet) {
		try {
			// Grab the player's connection
			Object handle = player.getClass().getMethod("getHandle").invoke(player);
			Object connection = handle.getClass().getField("playerConnection").get(handle);
			// Send the packet to them
			connection.getClass().getMethod("sendPacket", Packet).invoke(connection, packet);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}