package net.amoebaman.util;

import org.bukkit.Bukkit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class Reflection {

	public synchronized static String getVersion() {
		String name = Bukkit.getServer().getClass().getPackage().getName();
		String version = name.substring(name.lastIndexOf('.') + 1) + ".";
		return version;
	}

	private static final Map<String, Class<?>> _loadedNMSClasses = new HashMap<String, Class<?>>();

	private static final Map<String, Class<?>> _loadedOBCClasses = new HashMap<String, Class<?>>();

	public synchronized static Class<?> getNMSClass(String className) {
		if (_loadedNMSClasses.containsKey(className)) {
			return _loadedNMSClasses.get(className);
		}

		String fullName = "net.minecraft.server." + getVersion() + className;
		Class<?> clazz = null;
		try {
			clazz = Class.forName(fullName);
		} catch (Exception e) {
			e.printStackTrace();
			_loadedNMSClasses.put(className, null);
			return null;
		}
		_loadedNMSClasses.put(className, clazz);
		return clazz;
	}

	public synchronized static Class<?> getOBCClass(String className) {
		if (_loadedOBCClasses.containsKey(className)) {
			return _loadedOBCClasses.get(className);
		}

		String fullName = "org.bukkit.craftbukkit." + getVersion() + className;
		Class<?> clazz = null;
		try {
			clazz = Class.forName(fullName);
		} catch (Exception e) {
			e.printStackTrace();
			_loadedOBCClasses.put(className, null);
			return null;
		}
		_loadedOBCClasses.put(className, clazz);
		return clazz;
	}

	public synchronized static Object getHandle(Object obj) {
		try {
			return getMethod(obj.getClass(), "getHandle").invoke(obj);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static final Map<Class<?>, Map<String, Field>> _loadedFields = new HashMap<Class<?>, Map<String, Field>>();

	public synchronized static Field getField(Class<?> clazz, String name) {
		Map<String, Field> loaded;
		if (!_loadedFields.containsKey(clazz)) {
			loaded = new HashMap<String, Field>();
			_loadedFields.put(clazz, loaded);
		} else {
			loaded = _loadedFields.get(clazz);
		}
		if (loaded.containsKey(name)) {
			return loaded.get(name);
		}
		try {
			Field field = clazz.getDeclaredField(name);
			field.setAccessible(true);
			loaded.put(name, field);
			return field;
		} catch (Exception e) {
			e.printStackTrace();
			loaded.put(name, null);
			return null;
		}
	}

	private static final Map<Class<?>, Map<String, Map<ArrayWrapper<Class<?>>, Method>>> _loadedMethods = new HashMap<Class<?>, Map<String, Map<ArrayWrapper<Class<?>>, Method>>>();

	public synchronized static Method getMethod(Class<?> clazz, String name, Class<?>... args) {
		if (!_loadedMethods.containsKey(clazz)) {
			_loadedMethods.put(clazz, new HashMap<String, Map<ArrayWrapper<Class<?>>, Method>>());
		}

		Map<String, Map<ArrayWrapper<Class<?>>, Method>> loadedMethodNames = _loadedMethods.get(clazz);
		if (!loadedMethodNames.containsKey(name)) {
			loadedMethodNames.put(name, new HashMap<ArrayWrapper<Class<?>>, Method>());
		}

		Map<ArrayWrapper<Class<?>>, Method> loadedSignatures = loadedMethodNames.get(name);
		ArrayWrapper<Class<?>> wrappedArg = new ArrayWrapper<Class<?>>(args);
		if (loadedSignatures.containsKey(wrappedArg)) {
			return loadedSignatures.get(wrappedArg);
		}

		for (Method m : clazz.getMethods()) {
  	  	  	if (m.getName().equals(name) && Arrays.equals(args, m.getParameterTypes())) {
  	  	  	  	m.setAccessible(true);
  	  	  	  	loadedSignatures.put(wrappedArg, m);
  	  	  	  	return m;
  	  	  	}
  	  	}
		loadedSignatures.put(wrappedArg, null);
		return null;
	}

}