import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import snaq.db.ConnectionPool;

//Class implementing core database functionality.  Everything is static, this will never be instanced
public class DatabaseCore {
	private static ConnectionPool pool = null;
	private static Logger logger = Logger.getLogger(DatabaseCore.class);

	private static void connect() throws Exception {
		try {
			// Load the MySQL driver required to connect to the database server,
			// and register it
			@SuppressWarnings("rawtypes")
			Class c = Class.forName(ReadProperties.getProperty("dbdriver"));
			Driver dbDriver = (Driver) c.newInstance();
			DriverManager.registerDriver(dbDriver);

			// Build up a connection string object, e.g.
			// jdbc:mysql://localhost/dbname?
			String connectionString = (ReadProperties
					.getProperty("connectionprotocol")
					+ ReadProperties.getProperty("dbserver")
					+ "/"
					+ ReadProperties.getProperty("dbname") + "?");

			// Create a new connection pool
			pool = new ConnectionPool("dbpool", 0, 10, 100, 10,
					connectionString,
					ReadProperties.getProperty("locwriteuser"),
					ReadProperties.getProperty("locwritepass"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static List<HashMap<String, Object>> extractData(ResultSet result)
			throws SQLException {

		// Extracts the data from the ResultSet generated by an SQL expression,
		// and puts it into a list of hash maps, relating the column name
		// (string) to the value (object)
		ResultSetMetaData meta = result.getMetaData();
		int columns = meta.getColumnCount();
		List<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
 
		while (result.next()) {
			HashMap<String, Object> row = new HashMap<String, Object>(columns);
			for (int i = 1; i <= columns; ++i) {
				row.put(meta.getColumnName(i), result.getObject(i));
			}
			list.add(row);
		}

		return list;
	}

	public static List<HashMap<String, Object>> executeSqlQuery(String sqlString)
			throws Exception {
		ResultSet result = null;
		Connection con = null;
		Statement statement = null;
		List<HashMap<String, Object>> list = null;

		if (pool == null) {
			connect();
		}

		try {
			con = pool.getConnection(Long.parseLong(ReadProperties
					.getProperty("pooltimeout")));
			if (con != null) {
				// Create an SQL statement and attempt to execute it against the
				// database
				statement = con.createStatement();
				result = statement.executeQuery(sqlString);

				list = extractData(result);
			} else {
				logger.error("Critial error: unable to get access to the database because the pool was saturated with requests!");
			}

		} catch (Exception e) {
			logger.error("Error executing SQL statement: " + sqlString
					+ " on the database\n");
			e.printStackTrace();
		} finally {
			// Do this in a finally block so we release the resource even if
			// there is an exception!
			result.close();
			statement.close();
			con.close();
		}

		return list;
	}
}

// /RegisterDevice calls DeviceManager calls DbManager, DeviceManager gets a
// resultset from Db manager and does shit, then returns an instance of Device
// to RegisterDevice after closing the connection
