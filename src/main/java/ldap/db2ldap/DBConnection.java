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
	
    public SqlRowSet getBaseDepartment() {  
        String sql = "select * from org_zh where 上级编码='1' and 机构类型='部门'";  
        SqlRowSet set = jdbcTemplate.queryForRowSet(sql);
        return set;
    } 
}
