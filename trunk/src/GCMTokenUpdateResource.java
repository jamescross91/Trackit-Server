import java.util.LinkedHashMap;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Form;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

public class GCMTokenUpdateResource extends ServerResource {
	
	private static Logger logger = Logger
			.getLogger(GCMTokenUpdateResource.class);
	@Get
	public String represent() {
		return "hello, world";
	}

	@Post
	public Representation acceptItem(Representation entity) {
		System.out.println("Device attempting to update GCM token");
		Form form = new Form(entity);

		String device_id = form.getFirstValue("device_id");
		String auth_token = form.getFirstValue("auth_token");
		String gcm = form.getFirstValue("gcm_token");

		Device thisDevice = new Device(device_id);
		thisDevice.loadDevice();
		if (thisDevice.authenticateToken(auth_token)) {
			
			LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
			String sqlString;

				sqlString = "UPDATE device_details SET gcm_token = ? WHERE device_id = ?";
				data.put("gcm_token", gcm);
				data.put("device_id", device_id);
			try {
				DatabaseCore.executeSqlUpdate(sqlString, data);
			} catch (Exception e) {
				logger.error("Error updating devices GCM token, device id: " + device_id);
				e.printStackTrace();
				return new JsonRepresentation(getErrorObj());
			}
			return null;
		}
		
		return new JsonRepresentation(getErrorObj());
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
}
