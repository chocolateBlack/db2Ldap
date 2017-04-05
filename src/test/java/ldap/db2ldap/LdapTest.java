package ldap.db2ldap;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.samples.useradmin.domain.JWUser;
import org.springframework.ldap.samples.useradmin.service.UserService;
import org.springframework.ldap.support.LdapUtils;
import org.springframework.ldap.test.LdapTestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={
    "classpath:applicationContext.xml"
})

public class LdapTest {
    @Autowired
    private UserService userService;
    
    @Autowired
    private LdapTemplate ldapTemplate;
    
    @Autowired
    private ContextSource contextSource;

    @Test
	public void createUser(){
    	JWUser user = new JWUser();
		user.setEmail("123@126.com");
		user.setEmployeeNumber(111);
		user.setFirstName("firstName");
		user.setLastName("lastName");
		user.setPhone("123");
		user.setTitle("title");
		user.setFullName("中文名");
		System.out.println(userService.getLdapPath());
		userService.createUser(user);
	}
    
    @Test 
    public void AddUser(){
    	JWUser user = new JWUser();
		user.setEmail("123@126.com");
		user.setEmployeeNumber(111);
		user.setFirstName("firstName");
		user.setLastName("lastName");
		user.setPhone("123");
		user.setTitle("title");
		user.setFullName("中文名");
		userService.createUser(user);
    }
	
    /**
     * 添加OU
     */
	@Test
	public void createOu(){
		Attributes attr = new BasicAttributes(); 
		BasicAttribute ocattr = new BasicAttribute("objectclass");
		ocattr.add("organizationalUnit");
		ocattr.add("top");
		attr.put(ocattr);
		
//		ldapTemplate.bind("ou=IT", null, attr);// buildDN() function
		ldapTemplate.bind("ou=PMS集团直连组, ou=产品组, ou=产品与研发部, ou=慧通事业部, ou=业务", null, attr);
	}
	
	@Test
	public void unbindOu(){
		ldapTemplate.unbind("ou=IT");
	}
	
	/**
	 * clear ldap 
	 */
	@Test
	public void clear(){
		try {
			LdapTestUtils.clearSubContexts(contextSource, LdapUtils.emptyLdapName());
		} catch (NamingException e) {
			e.printStackTrace();
		}
	}
}
