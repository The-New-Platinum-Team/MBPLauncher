package com.marbleblast.mblauncher;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

public class Config {
	//Hardcoded because lazy
	public static String configJson = "http://marbleblast.com/files/launcher/config.json";
	static Map<String, Object> jsonConfig;
	
	public static String thisOS = Utils.getOS();
	public static boolean selfupdate;
	public static boolean online;

	public static void init() {
		jsonConfig = new HashMap<String, Object>();
		
		byte[] jsonlisting = Utils.downloadCachedFile(configJson, "config");
		if (Utils.hostAvailabilityCheck() || (jsonlisting != null)) {
			JSONObject object = new JSONObject(new String(jsonlisting));
			String names[] = JSONObject.getNames(object);
			
			for (String name : names) {
				Object val = object.get(name);
				
				if (val instanceof String) {
					//Non-OS Specific
					jsonConfig.put(name, object.get(name));
				} else if (val instanceof JSONObject) {
					//OS Specific
					if (((JSONObject) val).has(thisOS.toLowerCase())) {
						jsonConfig.put(name, ((JSONObject) val).get(thisOS.toLowerCase()));
					} else if (((JSONObject) val).has("other")) {
						jsonConfig.put(name, ((JSONObject) val).get("other"));
					} else {
						//Maybe not, just an array
						jsonConfig.put(name, object.get(name));
					}
				}
			}
			online = true;
		} else {
			//Load defaults
			jsonConfig.put("title", "Marble Blast Launcher - Offline");
			online = false;
			selfupdate = false;
		}
	}

	public static Object get(String key) {
		return jsonConfig.get(key);
	}
}
