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
	private ArrayList<RadialGeofenceHandler> points = new ArrayList<RadialGeofenceHandler>();
	private static Logger logger = Logger.getLogger(AlertsManager.class);

	public AlertsManager(DeviceLocation deviceLocation) {
		this.deviceLocation = deviceLocation;
		sourceDevice = new Device(deviceLocation.device_id);
		sourceDevice.loadDevice();
		loadPoints();
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
			sendAndroidLocAlert(parentDevice);
			sendAndroidGeoAlerts(parentDevice);
			break;
		}
	}

	private void sendAndroidLocAlert(Device parentDevice) {
		AndroidPushNotification notif = new AndroidPushNotification(AndroidPushNotification.LOCATION_UPDATE, parentDevice.device_id);
		notif.setDeviceLocation(deviceLocation);
		notif.pushMessage();
	}
	
	private void sendAndroidGeoAlerts(Device parentDevice){
		for(int i = 0; i < points.size(); i++){
			RadialGeofenceHandler point = points.get(i);
			String alertString = point.requiresAlert(deviceLocation, sourceDevice);
			if(alertString.compareTo(null) != 0){
				AndroidPushNotification notif = new AndroidPushNotification(AndroidPushNotification.GEOFENCE_CROSS, parentDevice.device_id);
				
				notif.pushMessage();
			}
		}
	}
	
	private void loadPoints() {

		//Load all GeoFences for this parent from the database 
		List<HashMap<String, Object>> result = null;

		String sqlString = "SELECT marker_id FROM radial_geofences WHERE parent_username = ?";
		LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
		data.put("parent_username", sourceDevice.parent_username);

		try {
			result = DatabaseCore.executeSqlQuery(sqlString, data);
		} catch (Exception e) {
			logger.error("Error extracting geofence from the database");
			e.printStackTrace();
		}

		for (int i = 0; i < result.size(); i++) {
			HashMap<String, Object> thisMarker = result.get(i);

			RadialGeofenceHandler marker = new RadialGeofenceHandler(
					Long.valueOf((int) thisMarker.get("marker_id")));
			marker.loadPoint();
			points.add(marker);
		}
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
