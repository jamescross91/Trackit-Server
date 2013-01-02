import org.restlet.data.Form;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

public class DeviceLocationResource extends ServerResource {

	@Get
	public String represent() {
		return "hello, world";
	}

	@Post
	public Representation acceptItem(Representation entity) {
		Representation result = null;
		Form form = new Form(entity);
		// String itemName = form.getFirstValue(name)
		double lat = Double.parseDouble(form.getFirstValue("latitude"));
		double lng = Double.parseDouble(form.getFirstValue("longitude"));
		double accuracy = Double.parseDouble(form.getFirstValue("accuracy"));
		double altitude = Double.parseDouble(form.getFirstValue("altitude"));
//		String provider = form.getFirstValue("provider");
		double bearing =Double.parseDouble(form.getFirstValue("bearing"));
		double speed = Double.parseDouble(form.getFirstValue("speed"));
		String deviceId = form.getFirstValue("deviceID");
		String authToken = form.getFirstValue("authToken");
//		int battery = Integer.parseInt(form.getFirstValue("battlevel"));
//		boolean charging = Boolean.parseBoolean(form.getFirstValue("charging"));

		String provider = "";
		int battery = 10;
		boolean charging = true;
		
//		public DeviceLocation(int device_id, String parent_username,
//				String auth_token, double latitude,
//				double longitude, String location_source, double altitude,
//				double accuracy, double bearing, int battery, boolean is_charging,
//				String network, String data_connection, double velocity) {

		DeviceLocation locInput = new DeviceLocation(deviceId, authToken, lat, lng, provider, altitude, accuracy, bearing, battery, charging, "guff", "3g", speed);
		locInput.persistLocation();
		
		AlertsManager manager = new AlertsManager(locInput);
		manager.processAlerts();
		
		System.out.println("My current latitude is " + lat
				+ " and longitude is " + lng + "\n");

		return (result);
	}

}