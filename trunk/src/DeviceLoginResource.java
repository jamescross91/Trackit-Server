import org.restlet.data.Form;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;


public class DeviceLoginResource extends ServerResource{
	@Get
	public String represent() {
		return "hello, world";
	}

	@Post
	public Representation acceptItem(Representation entity) {
		System.out.println("Device attempting to log in");
		Representation result = null;
		Form form = new Form(entity);

		System.out.println("hello");

		//result.
		
		return (result);
	}
}
