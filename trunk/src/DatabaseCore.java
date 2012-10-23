import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

//Class implementing core database functionality
public class DatabaseCore {
	private Connection connect = null;

	public DatabaseCore() {
		try {
			connect();
		} catch (Exception e) {
			System.out.println("Connection to the database failed\n");
			e.printStackTrace();
		}
	}

	private void connect() throws Exception {
		// Load the MySQL driver required to connect to the database server
		Class.forName(ReadProperties.getProperty("dbdriver"));

		// Build up a connection string object, e.g.
		// jdbc:mysql://localhost/dbname?
		String connectionString = (ReadProperties
				.getProperty("connectionprotocol")
				+ ReadProperties.getProperty("dbserver")
				+ "/"
				+ ReadProperties.getProperty("dbname") + "?");

		// Build up an authentication string object, e.g.
		// user=sqluser&password=sqluserpw
		String authString = ("user="
				+ ReadProperties.getProperty("locwriteuser") + "&password=" + ReadProperties
				.getProperty("locwritepass"));

		// Connect to the database
		connect = DriverManager.getConnection(connectionString + authString);
	}

	public void disconnect() throws SQLException {
		if (connect != null)
			connect.close();
	}

	public ResultSet executeSql(String sqlString) {
		ResultSet result = null;

		// Check the connection object
		try {
			if (connect == null)
				connect();

			// Create an SQL statement and attempt to execute it against the
			// database
			Statement statement;
			statement = connect.createStatement();
			result = statement.executeQuery(sqlString);

		} catch (Exception e) {
			System.out.println("Error executing SQL statement: " + sqlString
					+ " on the database\n");
			e.printStackTrace();
		}

		return result;
	}
}
