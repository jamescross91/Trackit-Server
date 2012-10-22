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
    public Representation acceptItem(Representation entity){
    	Representation result = null;
    	Form form = new Form(entity);
    	//String itemName = form.getFirstValue(name) 
    	String lat = form.getFirstValue("latitude");
    	String lng = form.getFirstValue("longitude");
    	
    	System.out.println("My current latitude is " + lat + " and longitude is " + lng + "\n");
    	
    	return(result);
    }
    
}