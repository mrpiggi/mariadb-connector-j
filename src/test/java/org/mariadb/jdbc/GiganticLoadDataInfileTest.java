package org.mariadb.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;


public class GiganticLoadDataInfileTest extends BaseTest {
	private Statement statement;

    @Before
    public void setUp() throws Exception {
    	super.before();
    	if (statement == null) {
    		statement = connection.createStatement();
    	}
        statement.execute("drop table if exists gigantic_load_data_infile");
        statement.execute("create table gigantic_load_data_infile (id int not null primary key auto_increment, name char(20)) ENGINE=myisam");
    }
    
    @After
    public void cleanup() throws SQLException {
    	statement.execute("DROP TABLE IF EXISTS gigantic_load_data_infile");
    	super.after();
    }
    
    @Ignore
    @Test
    public void giganticLoadDataInfileTest() throws SQLException, IOException {        
        VeryLongAutoGeneratedInputStream in = new VeryLongAutoGeneratedInputStream(300000000);
        if (statement.isWrapperFor(MySQLStatement.class)) {
            MySQLStatement mariaDbStatement = statement.unwrap(MySQLStatement.class);
            mariaDbStatement.setLocalInfileInputStream(in);
        } else {
        	in.close();
            throw new RuntimeException("Mariadb JDBC adaptor must be used");
        }
        
        String sql =
                        "LOAD DATA LOCAL INFILE 'dummyFileName' " +
                        "INTO TABLE gigantic_load_data_infile " + 
                        " FIELDS TERMINATED BY '\\t' ENCLOSED BY ''" +
                        " ESCAPED BY '\\\\' LINES TERMINATED BY '\\n'"
                        ;
            
        statement.execute(sql);
        ResultSet resultSet = statement.executeQuery("select count(*) from gigantic_load_data_infile");
        assertTrue(resultSet.next());
        int numberOfRowsInTable = resultSet.getInt(1);
        assertEquals(in.numberOfRows, numberOfRowsInTable);
    }
    
    /**
     * Custom memory conserving generator of a LOAD DATA INFILE that generates a stream like this:
     * 
     * 1\tname1
     * 2\tname2
     * 3\tname3
     */
    private static class VeryLongAutoGeneratedInputStream extends InputStream {
        
        private final int numberOfRows;
        private int currentPosInBuffer;
        private byte[] buffer;
        private int currentRow;

        private VeryLongAutoGeneratedInputStream(int numberOfRows) {
            this.numberOfRows = numberOfRows;
            currentRow = 0;
        }
        
        @Override
        public int read() throws IOException {
            if (currentRow > numberOfRows) {
                return -1;
            }
            if (buffer != null && currentPosInBuffer >= buffer.length) {
                buffer = null;
            }
            if (buffer == null) {
                currentRow++;
                currentPosInBuffer = 0;
                buffer = new String(currentRow + "\tname" + currentRow + "\n").getBytes();
            }            
            return buffer[currentPosInBuffer++];
        }
    }
    
}
