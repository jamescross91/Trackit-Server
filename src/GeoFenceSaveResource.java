import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Form;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

public class GeoFenceSaveResource extends ServerResource {

	private static Logger logger = Logger.getLogger(GeoFenceSaveResource.class);

	@Get
	public String represent() {
		return "hello, world";
	}

	@Post
	public Representation acceptItem(Representation entity) {
		System.out.println("Device attempting to save geomarker");
		Representation result = null;
		Form form = new Form(entity);

		String device_id = form.getFirstValue("device_id");
		String auth_token = form.getFirstValue("auth_token");
		double lat = Double.parseDouble(form.getFirstValue("latitude"));
		double lng = Double.parseDouble(form.getFirstValue("longitude"));
		double radius = Double.parseDouble(form.getFirstValue("radius"));
		long marker_id = Long.parseLong(form.getFirstValue("marker_id"));

		Device thisDevice = new Device(device_id);
		thisDevice.loadDevice();
		if (thisDevice.authenticateToken(auth_token)) {
			RadialGeofenceHandler handler = new RadialGeofenceHandler(marker_id);
			handler.setLat(lat);
			handler.setLng(lng);
			handler.setRadius(radius);
			handler.setParent_username(thisDevice.parent_username);
			handler.savePoint();

			//Issue an update to devices asking them to reload their geofences
			AlertsManager manager = new AlertsManager();
			manager.setDevice(device_id);
			manager.setMarker_id(marker_id);
			manager.processGeofenceUpdates();
			
			//Recompute the location alerts
			manager.setLocation(loadLatest(device_id, auth_token));
			manager.processAlerts();

			result = new JsonRepresentation(handler.toJson());
		} else
			result = new JsonRepresentation(getErrorObj());

		return (result);
	}

	private JSONObject getErrorObj() {
		JSONObject object = new JSONObject();
		try {
			object.put("failure", "Device did not authenticate");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return object;
	}

	private DeviceLocation loadLatest(String device_id, String auth_token) {

		List<HashMap<String, Object>> result = null;

		String sqlString = "SELECT * FROM location_details WHERE device_id = ? ORDER BY entry_time DESC LIMIT  1";
		LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
		data.put("device_id", device_id);

		try {
			result = DatabaseCore.executeSqlQuery(sqlString, data);
		} catch (Exception e) {
			logger.error("Error loading latest device location for device: "
					+ device_id);
			e.printStackTrace();
		}

		if (result.size() > 1) {
			logger.error("Multiple locations found for: " + device_id
					+ " something is broken!");
			return null;
		}

		if (result.size() == 0) {
			logger.error("Unable to find the most recent location for device id: "
					+ device_id);
		}

		// Check the username provided against the database
		HashMap<String, Object> thisLoc = result.get(0);
		double lat = (double) thisLoc.get("latitude");
		double lng = (double) thisLoc.get("longitude");
		String locSource = (String) thisLoc.get("location_source");
		double alt = (double) thisLoc.get("altitude");
		double accuracy = (double) thisLoc.get("accuracy");
		double bearing = (double) thisLoc.get("bearing");
		int batt = (int) thisLoc.get("battery");
		boolean charge = (boolean) thisLoc.get("is_charging");
		String network = (String) thisLoc.get("network");
		String data_connection = (String) thisLoc.get("data_connection");
		double velocity = (double) thisLoc.get("velocity");

		DeviceLocation loc = new DeviceLocation(device_id, auth_token, lat,
				lng, locSource, alt, accuracy, bearing, batt, charge, network,
				data_connection, velocity);
		
		return loc;
	}
}
