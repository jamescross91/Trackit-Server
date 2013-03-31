package com.reading.trackit;
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
	private HashMap<String, HashMap<String, ConvexHullPoint>> convexMarkerLists = new HashMap<String, HashMap<String, ConvexHullPoint>>();
	private static Logger logger = Logger.getLogger(AlertsManager.class);
	private long marker_id;

	public void setLocation(DeviceLocation deviceLocation) {
		this.deviceLocation = deviceLocation;
		sourceDevice = new Device(deviceLocation.getDevice().device_id);
		sourceDevice.loadDevice();
		loadRadialPoints();
		loadConvexPoints();
	}

	public void setDevice(Device device) {
		sourceDevice = device;
		loadRadialPoints();
		loadConvexPoints();
	}

	public boolean processAlerts() {
		loadParents();

		for (int i = 0; i < parentDevices.size(); i++) {
			processAlerts(parentDevices.get(i));
		}

		return true;
	}

	public boolean pushLocations() {
		loadParents();

		for (int i = 0; i < parentDevices.size(); i++) {
			processLocationAlerts(parentDevices.get(i));
		}

		return true;
	}

	public boolean processGeofenceUpdates() {
		loadParents();

		// Ignore the parent issuing the update they dont need to get the alert!
		for (int i = 0; i < parentDevices.size(); i++) {
			if (parentDevices.get(i).device_id
					.compareTo(sourceDevice.device_id) != 0) {
				processGeoChange(parentDevices.get(i));
			}
		}

		return true;
	}

	private void processGeoChange(Device parentDevice) {
		switch (parentDevice.OS) {
		case "Android":
			sendGeoChangeAlert(parentDevice);
			break;
		}
	}

	private void processAlerts(Device parentDevice) {
		switch (parentDevice.OS) {
		case "Android":
			sendAndroidLocAlert(parentDevice);
			sendAndroidGeoAlerts(parentDevice);
			sendAndroidConvexAlerts(parentDevice);
			break;
		}
	}

	private void processLocationAlerts(Device parentDevice) {
		switch (parentDevice.OS) {
		case "Android":
			sendAndroidLocAlert(parentDevice);
			break;
		}
	}

	private void sendGeoChangeAlert(Device parentDevice) {
		AndroidPushNotification notif = new AndroidPushNotification(
				AndroidPushNotification.MARKER_UPDATE, parentDevice);
		notif.setmarker_id(marker_id);
		notif.pushMessage();
	}

	private void sendAndroidLocAlert(Device parentDevice) {
		AndroidPushNotification notif = new AndroidPushNotification(
				AndroidPushNotification.LOCATION_UPDATE, parentDevice);
		notif.setDeviceLocation(deviceLocation);
		notif.pushMessage();
	}

	private void sendAndroidConvexAlerts(Device parentDevice) {
		// Iterate over each group (if we have any)
		// For each group instatiate the ConvexHullHandler
		// Check if this location needs an alert
		// If it does, issue it!
		if (convexMarkerLists != null) {
			for (Entry<String, HashMap<String, ConvexHullPoint>> entry : convexMarkerLists
					.entrySet()) {
				// Extract the details and create a handler
				String group_id = entry.getKey();
				HashMap<String, ConvexHullPoint> map = entry.getValue();
				ConvexHullHandler handler = new ConvexHullHandler(
						Integer.valueOf(group_id), map,
						parentDevice.parent_username);

				// Check if an alert is required
				String alertString = handler.requiresAlert(deviceLocation,
						sourceDevice);

				if (alertString != null) {
					AndroidPushNotification notif = new AndroidPushNotification(
							AndroidPushNotification.GEOFENCE_CROSS,
							parentDevice);
					notif.setAlertMessage(alertString);
					notif.pushMessage();
				}
			}
		}
	}

	private void sendAndroidGeoAlerts(Device parentDevice) {
		for (int i = 0; i < points.size(); i++) {
			RadialGeofenceHandler point = points.get(i);
			String alertString = point.requiresAlert(deviceLocation,
					sourceDevice);
			if (alertString != null) {
				AndroidPushNotification notif = new AndroidPushNotification(
						AndroidPushNotification.GEOFENCE_CROSS, parentDevice);
				notif.setAlertMessage(alertString);
				notif.pushMessage();
			}
		}
	}

	private void loadRadialPoints() {

		// Load all GeoFences for this parent from the database
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

	private void loadConvexPoints() {
		ConvexHullHandler handler = new ConvexHullHandler(
				sourceDevice.parent_username);
		convexMarkerLists = handler.loadPoints();
	}

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

	public long getMarker_id() {
		return marker_id;
	}

	public void setMarker_id(long marker_id) {
		this.marker_id = marker_id;
	}
}
