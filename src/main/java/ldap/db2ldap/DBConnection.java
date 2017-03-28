package ldap.db2ldap;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

public class DBConnection 
{
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Test
    public SqlRowSet insertQuery() {  
        String sql = "select * from  org_zh";  
        SqlRowSet set = jdbcTemplate.queryForRowSet(sql);
        return set;
    } 
}
