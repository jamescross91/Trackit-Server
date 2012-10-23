import org.restlet.Component;
import org.restlet.data.Protocol;

public class Launch {
	public static void main(String[] args) throws Exception {

		// Create a new Component.
		Component component = new Component();

		// Add a new HTTP server listening on port 8182.
		component.getServers().add(Protocol.HTTP, 2610);

		// Attach the sample application.
		component.getDefaultHost().attach("/device", new LocationApplication());

		// Start the component.
		component.start();
	}
}
