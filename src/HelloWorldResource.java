import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

/**
 * Resource which has only one representation.
 */
public class HelloWorldResource extends ServerResource {

    @Get
    public String represent() {
        return "hello, world";
    }
    
    @Post
    public Representation acceptItem(Representation entity){
    	Representation result = null;
    	
    	    	
    	return(result);
    }
    
}