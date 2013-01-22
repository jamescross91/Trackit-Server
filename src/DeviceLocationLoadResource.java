import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.restlet.data.Form;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

public class DeviceLocationLoadResource extends ServerResource {
	
	private static Logger logger = Logger.getLogger(DeviceLocationLoadResource.class);

	@Get
	public String represent() {
		return "42";
	}

	@Post
	public Representation acceptItem(Representation entity) {
		Representation result = null;
		Form form = new Form(entity);
		String deviceId = form.getFirstValue("deviceID");
		String authToken = form.getFirstValue("authToken");

		Device device = new Device(deviceId);
		device.loadDevice();
		//Does the device authenticate?
		if(!device.authenticateToken(authToken))
			return null;
		
		//Load the most recent location for the device
		ArrayList<Device> children = loadParentsChildren(deviceId);
		for(int i = 0; i < children.size(); i++){
			AlertsManager manager = new AlertsManager();
			manager.setDevice(children.get(i).device_id);
			DeviceLocation latestLoc = new DeviceLocation();
			latestLoc.loadLatest(children.get(i).device_id);
			manager.setLocation(latestLoc);
			manager.processAlerts();
		}

		return (result);
	}


	private ArrayList<Device> loadParentsChildren(String parent_device_id) {
		Device device = new Device(parent_device_id);
		device.loadDevice();
		
		ArrayList<Device> devices = new ArrayList<Device>();

		List<HashMap<String, Object>> result = null;
		String sqlString = "SELECT * FROM device_details WHERE parent_username = ? AND is_child = 1";

		LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
		data.put("parent_username", device.parent_username);

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
			devices.add(thisDevice);
		}

		return devices;
	}
}