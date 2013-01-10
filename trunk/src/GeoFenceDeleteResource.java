import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Form;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

public class GeoFenceDeleteResource extends ServerResource {

	private static Logger logger = Logger
			.getLogger(GeoFenceDeleteResource.class);

	@Get
	public String represent() {
		return "hello, world";
	}

	@Post
	public Representation acceptItem(Representation entity) {
		System.out.println("Device attempting to remove marker");
		Representation result = null;
		Form form = new Form(entity);

		String device_id = form.getFirstValue("device_id");
		String auth_token = form.getFirstValue("auth_token");
		long marker_id = Long.parseLong(form.getFirstValue("marker_id"));

		Device thisDevice = new Device(device_id);
		thisDevice.loadDevice();
		if (thisDevice.authenticateToken(auth_token)) {
			RadialGeofenceHandler handler = new RadialGeofenceHandler(marker_id);
			if (!handler.deletePoint()) {
				result = new JsonRepresentation(getErrorObj());
			}
			result = new JsonRepresentation(getSuccessObj());
		} else {
			result = new JsonRepresentation(getErrorObj());
		}

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

	private JSONObject getSuccessObj() {
		JSONObject object = new JSONObject();
		try {
			object.put("Success", "Marker deleted");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return object;
	}

}
