package ldap.db2ldap;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.ldap.LdapName;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.BaseLdapNameAware;
import org.springframework.ldap.samples.useradmin.domain.Group;
import org.springframework.ldap.samples.useradmin.domain.GroupRepo;
import org.springframework.ldap.samples.useradmin.domain.JWOrganization;
import org.springframework.ldap.samples.useradmin.domain.JWUser;
import org.springframework.ldap.samples.useradmin.domain.User;
import org.springframework.ldap.samples.useradmin.service.OrganizationService;
import org.springframework.ldap.samples.useradmin.service.UserService;
import org.springframework.ldap.support.LdapNameBuilder;
import org.springframework.ldap.support.LdapUtils;
import org.springframework.ldap.test.LdapTestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={
    "classpath:applicationContext.xml"
})

public class LdapTest  implements BaseLdapNameAware {

	@Autowired
    private UserService userService;
	
	@Autowired
	private OrganizationService orgService;
    
    @Autowired
    private LdapTemplate ldapTemplate;
    
    @Autowired
    private ContextSource contextSource;
    
    @Autowired
    private GroupRepo groupRepo;
    
    private LdapName baseLdapPath;
    
    @Test
	public void createUser(){
    	JWUser user = new JWUser();
    	user.setId("cn=111, ou=慧通事业部, ou=业务");
		user.setEmail("123@126.com");
		user.setEmployeeNumber("123");
		user.setLastName("lastName");
		user.setPhone("123");
		user.setTitle("title");
		user.setUid("ZH201703019");
		user.setUserPassword("c9c4c39a6ce3413ed32214ba89c1e777");
		
		userService.createJWUser(user);
		addMemberToGroup(user);
//		ldapTemplate.create(user);
	}
    @Test
	public void createOrganization(){
    	JWOrganization org = new JWOrganization();
    	org.setId("ou=1, ou=慧通事业部, ou=业务");
    	orgService.createJWOrg(org);
//		ldapTemplate.create(org);
//		userService.createJWUser(user);
	}
    
    /**
     * 添加OU
     */
	@Test
	public void createNode(){
		Attributes attr = new BasicAttributes(); 
		BasicAttribute ocattr = new BasicAttribute("objectclass");
		ocattr.add("organizationalUnit");
		ocattr.add("top");
		attr.put(ocattr);
		
		ldapTemplate.bind("ou=Group", null, attr);
	}
	@Test
	public void createGroup(){
		Attributes attr = new BasicAttributes(); 
		BasicAttribute ocattr = new BasicAttribute("objectclass");
		ocattr.add("groupOfNames");
		ocattr.add("top");
		attr.put(ocattr);
		attr.put("member", "cn=ZH201506006,ou=大数据平台研发工程师,ou=大数据平台部,ou=技术中心,ou=职能");
		ldapTemplate.bind("cn=ROLE_USER, ou=Group", null, attr);
	}
	
    /**
     * 向Group中添加member
     */
	public void addMemberToGroup(JWUser savedUser){
	    Group userGroup = groupRepo.findByName(GroupRepo.USER_GROUP);
	    LdapName ldapName = LdapNameBuilder.newInstance(baseLdapPath).add(savedUser.getId()).build();
	    // The DN the member attribute must be absolute
	    userGroup.addMember(LdapUtils.newLdapName(savedUser.getId()));
//	    userGroup.addMember(ldapName);
	    groupRepo.save(userGroup);
	}
	
	
    /**
     * 添加User
     */
	@Test
	public void createU(){
//		@Entry(objectClasses = { "inetOrgPerson", "organizationalPerson", "person", "top", "shadowAccount" })
		Attributes attr = new BasicAttributes(); 
		BasicAttribute ocattr = new BasicAttribute("objectclass");
		ocattr.add("top");
		ocattr.add("organizationalPerson");
		ocattr.add("shadowAccount");
		attr.put(ocattr);
		attr.put("userPassword", "12");
		attr.put("sn", "12");
		attr.put("uid", "12");
		
//		ldapTemplate.bind("ou=IT", null, attr);// buildDN() function
		ldapTemplate.bind("cn=jg2h1,ou=慧通事业部,ou=慧通事业部, ou=业务", null, attr);
	}
	
	
	@Test
	public void unbindOu(){
		ldapTemplate.unbind("cn=ROLE_USER, ou=Group");
	}
	
	@Test
	public void search(){
		User user = userService.findUser("ou=产品部,ou=慧通事业部,ou=业务");
		System.out.println(user.getEmail());
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
	@Override
	public void setBaseLdapPath(LdapName baseLdapPath) {
		this.baseLdapPath = baseLdapPath;
	}
    
}
