package cz.malyzajic.audiorabbit;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author daop
 */
public class Db {

    Connection connection;

    public Db() {
        try {
            Class.forName("org.hsqldb.jdbc.JDBCDriver");
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Db.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void connect() {
        try {
            connection = DriverManager.getConnection("jdbc:hsqldb:file:/tmp/testdb", "sa", "");
            if (connection != null) {
                System.out.println("Connection created successfully");

            } else {
                System.out.println("Problem with creating connection");
            }
        } catch (SQLException ex) {
            Logger.getLogger(Db.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void disconnect() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException ex) {
            Logger.getLogger(Db.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void testAndPrepareDb() {
        try {
            Statement st = null;
            ResultSet rs = null;

            st = connection.createStatement();         // statement objects can be reused with

            DatabaseMetaData meta = connection.getMetaData();
            ResultSet tables = meta.getTables(null, null, "MUSIC_ITEM", null);
            if (tables.next()) {

            } else {
                Statement createSt = connection.createStatement();
                int i = createSt.executeUpdate("CREATE TABLE music_item (\n"
                        + "   id INT NOT NULL,\n"
                        + "   title VARCHAR(150) NOT NULL,\n"
                        + "   album VARCHAR(120) NOT NULL,\n"
                        + "   author VARCHAR(120) NOT NULL,\n"
                        + "   path VARCHAR(250) NOT NULL,\n"
                        + "   ord INT,\n"
                        + "   PRIMARY KEY (id) \n"
                        + ");");
                System.out.println("aaaaaaaaaaaaaaaaaaa" + i);
            }
            st.close();    // NOTE!! if you close a statement the associated ResultSet is
        } catch (SQLException ex) {
            Logger.getLogger(Db.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public synchronized void update(String expression) throws SQLException {

        Statement st;
        st = connection.createStatement();    // statements
        int i = st.executeUpdate(expression);    // run the query
        if (i == -1) {
            System.out.println("db error : " + expression);
        }

        st.close();
    }

}
