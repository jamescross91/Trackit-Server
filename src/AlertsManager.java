import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

public class AlertsManager {
	private DeviceLocation deviceLocation;
	private Device sourceDevice;
	private ArrayList<Device> parentDevices = new ArrayList<Device>();
	private static Logger logger = Logger.getLogger(AlertsManager.class);

	public AlertsManager(DeviceLocation deviceLocation) {
		this.deviceLocation = deviceLocation;
		sourceDevice = new Device(deviceLocation.device_id);
		sourceDevice.loadDevice();
	}

	public boolean processAlerts() {
		loadParents();

		for (int i = 0; i < parentDevices.size(); i++) {
			processAlerts(parentDevices.get(i));
		}

		return true;
	}

	private void processAlerts(Device parentDevice) {
		switch (parentDevice.OS) {
		case "Android":
			sendAndroidAlert(parentDevice);
			break;
		}
	}

	private void sendAndroidAlert(Device parentDevice) {
		AndroidPushNotification notif = new AndroidPushNotification(AndroidPushNotification.LOCATION_UPDATE, parentDevice.device_id);
		notif.setDeviceLocation(deviceLocation);
		notif.pushMessage();
	}

	@SuppressWarnings({ "unchecked", "unused" })
	private void loadParents() {
		List<HashMap<String, Object>> result = null;
		String sqlString = "SELECT * FROM device_details WHERE parent_username = ? AND is_child = 0";

		LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
		data.put("parent_username", sourceDevice.parent_username);

		try {
			result = DatabaseCore.executeSqlQuery(sqlString, data);
		} catch (Exception e) {
			logger.error("Error loading parents device details from the database");
			e.printStackTrace();
		}

		if (result.size() == 0) {
			// No devices
		}

		for (int i = 0; i < result.size(); i++) {
			HashMap<String, Object> thisEntry = result.get(i); 
			Device thisDevice = new Device((String) thisEntry.get("device_id"));
			thisDevice.loadDevice();
			parentDevices.add(thisDevice);
		}
	}
}
