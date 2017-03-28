package ldap.db2ldap;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

/**
 * Unit test for simple App.
 */
public class AppTest{
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	ApplicationContext ac = null;
	
	@Before
	public void initDB(){
		String basepath = new File("").getAbsolutePath();
		String path = basepath + "/src/main/resources/applicationContext.xml";
		ac = new FileSystemXmlApplicationContext(path);
	}
	
	@Test
    public void insertQuery() {  
        String sql = "select * from  org_zh";  
        SqlRowSet set = jdbcTemplate.queryForRowSet(sql);
        System.out.println(set);
    } 
}
