//Class containing common functionality for applications wishing to connect to the system externally
public class ExtConnect{
	//Authenticate
	//Login
	//Execute SQL - remember we will need to handle both querys and inserts here
	//Privs will need to be changed in DatabaseCore.  Try mapping from a class to a username/password - which will in turn be configured in MySQL
	
	public boolean authenticate( String username, String password){
		//Perform an SQL query against the database to check and if the string username exists, and if it does
		//get the salted hash of its password
		
		String sqlString = "SELECT * FROM parent_details WHERE username = " + username;
		try {
			DatabaseCore.executeSqlQuery(sqlString);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}
}
