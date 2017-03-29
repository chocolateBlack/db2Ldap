package ldap.db2ldap;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.ldap.core.LdapTemplate;

public class LdapTest {
	private LdapTemplate ldapTemplate;
	ApplicationContext ac = null;
	@Before
	public void initLdap(){
		String basepath = new File("").getAbsolutePath();
		String path = basepath + "/src/main/resources/applicationContext.xml";
		ac = new FileSystemXmlApplicationContext(path);
		ldapTemplate = (LdapTemplate) ac.getBean("ldapTemplate");
	}
	
	@Test
	public void getUser(){
		
	}
}
