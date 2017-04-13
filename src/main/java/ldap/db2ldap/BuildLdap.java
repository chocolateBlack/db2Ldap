package ldap.db2ldap;

import java.util.ArrayList;
import java.util.List;

import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.samples.useradmin.service.UserService;
import org.springframework.util.StringUtils;


public class BuildLdap{
	@Autowired
	private JdbcTemplate jdbcTemplate;

    @Autowired
    private LdapTemplate ldapTemplate;
    
    @Autowired
    private ContextSource contextSource;

    @Autowired
    private UserService userService;
    
	public static void main(String[] args) {
		
	}
	
}
