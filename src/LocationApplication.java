import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;

public class LocationApplication extends Application {

	/**
	 * Creates a root Restlet that will receive all incoming calls.
	 */
	@Override
	public synchronized Restlet createInboundRoot() {
		// Create a router Restlet that routes each call to a new instance of
		// HelloWorldResource.
		Router router = new Router(getContext());

		// Defines only one route
		router.attach("/child/location", DeviceLocationResource.class);
		router.attach("/child/login", DeviceLoginResource.class);
		router.attach("/parent/login", ParentLoginResource.class);
		router.attach("/parent/geofence/save", GeoFenceSaveResource.class);
		router.attach("/parent/geofence/load", GeoFenceLoadResource.class);
		router.attach("/parent/geofence/delete", GeoFenceDeleteResource.class);
			
		return router;
	}

}