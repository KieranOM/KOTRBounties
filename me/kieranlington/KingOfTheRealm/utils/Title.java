package me.kieranlington.KingOfTheRealm.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import net.amoebaman.util.Reflection;

public class Title {
	// TRIMMED VERSION OF MVdW's TITLE CLASS
	// http://git.mvdw-software.be/maxim/titlemotd/blob/master/src/be/maximvdw/titlemotd/ui/Title.java
	
	private static Class<?> packetTitle;
	private static Class<?> packetActions;
	private static Class<?> nmsChatSerializer;
	private static Class<?> chatBaseComponent;
	private String title = "";
	private String subtitle = "";
	private int fadeTime = 5;
	private int stayTime = -1;

	private static final Map<Class<?>, Class<?>> CORRESPONDING_TYPES = new HashMap<Class<?>, Class<?>>();
	
	public Title(String title, String subtitle, int stayTime) {
		this.title = title == null ? "" : ChatColor.translateAlternateColorCodes('&', title);
		this.subtitle = subtitle == null ? "" : ChatColor.translateAlternateColorCodes('&', subtitle);
		this.stayTime = stayTime;
		loadClasses();
	}
	
	private void loadClasses() {
		if (packetTitle == null) {
			packetTitle = getNMSClass("PacketPlayOutTitle");
			packetActions = getNMSClass("PacketPlayOutTitle$EnumTitleAction");
			chatBaseComponent = getNMSClass("IChatBaseComponent");
			nmsChatSerializer = getNMSClass("ChatComponentText");
		}
	}
	
	public void send(Player player) {
		if (packetTitle != null) {
			// First reset previous settings
			resetTitle(player);
			try {
				// Send timings first
				Object handle = getHandle(player);
				Object connection = getField(handle.getClass(),
						"playerConnection").get(handle);
				Object[] actions = packetActions.getEnumConstants();
				Method sendPacket = getMethod(connection.getClass(),
						"sendPacket");
				Object packet = packetTitle.getConstructor(packetActions,
						chatBaseComponent, Integer.TYPE, Integer.TYPE,
						Integer.TYPE).newInstance(actions[2], null,
						fadeTime,
						stayTime,
						fadeTime);
				// Send if set
				if (fadeTime != -1 && fadeTime != -1 && stayTime != -1)
					sendPacket.invoke(connection, packet);

				// Send title
				Object serialized = nmsChatSerializer.getConstructor(
						String.class).newInstance(
						ChatColor.translateAlternateColorCodes('&', title));
				packet = packetTitle.getConstructor(packetActions,
						chatBaseComponent).newInstance(actions[0], serialized);
				sendPacket.invoke(connection, packet);
				if (subtitle != "") {
					// Send subtitle if present
					serialized = nmsChatSerializer.getConstructor(String.class)
							.newInstance(
									ChatColor.translateAlternateColorCodes('&',
											subtitle));
					packet = packetTitle.getConstructor(packetActions,
							chatBaseComponent).newInstance(actions[1],
							serialized);
					sendPacket.invoke(connection, packet);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void updateTimes(Player player) {
		if (Title.packetTitle != null) {
			try {
				Object handle = getHandle(player);
				Object connection = getField(handle.getClass(),
						"playerConnection").get(handle);
				Object[] actions = Title.packetActions.getEnumConstants();
				Method sendPacket = getMethod(connection.getClass(),
						"sendPacket");
				Object packet = Title.packetTitle.getConstructor(
						new Class[] { Title.packetActions, chatBaseComponent,
								Integer.TYPE, Integer.TYPE, Integer.TYPE })
						.newInstance(
								new Object[] {
										actions[2],
										null,
										Integer.valueOf(this.fadeTime),
										Integer.valueOf(this.stayTime),
										Integer.valueOf(this.fadeTime)});
				if ((this.fadeTime != -1) && (this.fadeTime != -1)
						&& (this.stayTime != -1)) {
					sendPacket.invoke(connection, packet);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public void broadcast() {
		for (Player p : Bukkit.getOnlinePlayers()) send(p);
	}
	
	public void resetTitle(Player player) {
		try {
			// Send timings first
			Object handle = getHandle(player);
			Object connection = getField(handle.getClass(), "playerConnection")
					.get(handle);
			Object[] actions = packetActions.getEnumConstants();
			Method sendPacket = getMethod(connection.getClass(), "sendPacket");
			Object packet = packetTitle.getConstructor(packetActions,
					chatBaseComponent).newInstance(actions[4], null);
			sendPacket.invoke(connection, packet);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Class<?> getPrimitiveType(Class<?> clazz) {
		return CORRESPONDING_TYPES.containsKey(clazz) ? CORRESPONDING_TYPES
				.get(clazz) : clazz;
	}

	private Class<?>[] toPrimitiveTypeArray(Class<?>[] classes) {
		int a = classes != null ? classes.length : 0;
		Class<?>[] types = new Class<?>[a];
		for (int i = 0; i < a; i++)
			types[i] = getPrimitiveType(classes[i]);
		return types;
	}

	private static boolean equalsTypeArray(Class<?>[] a, Class<?>[] o) {
		if (a.length != o.length)
			return false;
		for (int i = 0; i < a.length; i++)
			if (!a[i].equals(o[i]) && !a[i].isAssignableFrom(o[i]))
				return false;
		return true;
	}

	private Object getHandle(Object obj) {
		try {
			return getMethod("getHandle", obj.getClass()).invoke(obj);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private Method getMethod(String name, Class<?> clazz,
			Class<?>... paramTypes) {
		Class<?>[] t = toPrimitiveTypeArray(paramTypes);
		for (Method m : clazz.getMethods()) {
			Class<?>[] types = toPrimitiveTypeArray(m.getParameterTypes());
			if (m.getName().equals(name) && equalsTypeArray(types, t))
				return m;
		}
		return null;
	}

	private Class<?> getNMSClass(String className) {
		String fullName = "net.minecraft.server." + Reflection.getVersion() + className;
		Class<?> clazz = null;
		try {
			clazz = Class.forName(fullName);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return clazz;
	}

	private Field getField(Class<?> clazz, String name) {
		try {
			Field field = clazz.getDeclaredField(name);
			field.setAccessible(true);
			return field;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private Method getMethod(Class<?> clazz, String name, Class<?>... args) {
		for (Method m : clazz.getMethods())
			if (m.getName().equals(name)
					&& (args.length == 0 || ClassListEqual(args,
							m.getParameterTypes()))) {
				m.setAccessible(true);
				return m;
			}
		return null;
	}

	private boolean ClassListEqual(Class<?>[] l1, Class<?>[] l2) {
		boolean equal = true;
		if (l1.length != l2.length)
			return false;
		for (int i = 0; i < l1.length; i++)
			if (l1[i] != l2[i]) {
				equal = false;
				break;
			}
		return equal;
	}
}