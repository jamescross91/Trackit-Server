import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Form;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

public class GeoFenceSaveResource extends ServerResource {
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
		if(thisDevice.authenticateToken(auth_token)){
			RadialGeofenceHandler handler = new RadialGeofenceHandler(marker_id);
			handler.setLat(lat);
			handler.setLng(lng);
			handler.setRadius(radius);
			handler.setParent_username(thisDevice.parent_username);
			handler.savePoint();
			
			AlertsManager manager = new AlertsManager();
			manager.setDevice(device_id);
			manager.setMarker_id(marker_id);
			manager.processGeofenceUpdates();
			
			result = new JsonRepresentation(handler.toJson());
		}
		else
			result = new JsonRepresentation(getErrorObj());
		
		return (result);
	}
	
	private JSONObject getErrorObj(){
		JSONObject object = new JSONObject();
		try {
			object.put("failure", "Device did not authenticate");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return object;
	}
	
}
