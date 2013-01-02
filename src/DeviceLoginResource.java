import org.restlet.data.Form;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

public class DeviceLoginResource extends ServerResource {
	@Get
	public String represent() {
		return "hello, world";
	}

	@Post
	public Representation acceptItem(Representation entity) {
		System.out.println("Device attempting to log in");
		Representation result = null;
		Form form = new Form(entity);

		String username = form.getFirstValue("username");
		String password = form.getFirstValue("password");
		String make = form.getFirstValue("make");
		String model = form.getFirstValue("model");
		String OS = form.getFirstValue("OS");
		String id = form.getFirstValue("deviceID");
		String gcm = form.getFirstValue("gcm_token");
		double phone_number = Double.parseDouble(form
				.getFirstValue("phoneNumber"));
		//
		// public DeviceLogin(String username, String password, String make,
		// String model, double phone_number, String OS, boolean is_child) {
		DeviceLogin newDevice = new DeviceLogin(id, username, password, make,
				model, phone_number, OS, true, gcm);
		
		newDevice.login();
		
		result = new JsonRepresentation(newDevice.toJson());
		
		System.out.println("hello");

		// result.

		return (result);
	}
}
