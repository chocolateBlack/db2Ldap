package ldap.db2ldap;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.samples.useradmin.domain.DepartmentRepo;
import org.springframework.ldap.samples.useradmin.domain.User;
import org.springframework.ldap.samples.useradmin.service.UserService;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={
    "classpath:applicationContext.xml"
})

public class LdapTest {
	private LdapTemplate ldapTemplate;
	ApplicationContext ac = null;
	
    @Autowired
    private UserService userService;

    @Autowired
    private DepartmentRepo departmentRepo;
    
	@Before
	public void initLdap(){
		String basepath = new File("").getAbsolutePath();
//		String path = basepath + "/src/main/resources/applicationContext.xml";
//		ac = new FileSystemXmlApplicationContext(path);
//		userService = (UserService) ac.getBean("userService");
	}
	
	@Test
	public void createUser(){
		User user = new User();
		user.setDepartment("Accounting");
		user.setEmail("123@126.com");
		user.setEmployeeNumber(111);
		user.setFirstName("firstName");
		user.setLastName("lastName");
		user.setPhone("123");
		user.setTitle("title");
		user.setFullName("我");
		user.setUnit("General");
		System.out.println(userService.getLdapPath());
		userService.createUser(user);
	}
}
