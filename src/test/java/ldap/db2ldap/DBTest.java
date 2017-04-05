package ldap.db2ldap;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;
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
public class DBTest{
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	ApplicationContext ac = null;
	
	@Before
	public void initDB(){
		String basepath = new File("").getAbsolutePath();
		String path = basepath + "/src/main/resources/applicationContext.xml";
		ac = new FileSystemXmlApplicationContext(path);
		jdbcTemplate = (JdbcTemplate) ac.getBean("jdbcTemplate");
	}
	
	/**
	 * 获取组织结构数据
	 */
	@Test
    public void initOrgTree() {  
        String sql = "select 机构编码,机构名称,上级编码,机构类型  from org_zh";  
        SqlRowSet set = jdbcTemplate.queryForRowSet(sql);
        HashMap<String,Organization> map = new HashMap<String,Organization>();
        
        while(set.next()){
        	String orgCode = set.getString(1);
        	String orgName = set.getString(2);
        	String orgParentCode = set.getString(3);
        	String orgType = set.getString(4);
        	map.put(orgCode, new Organization(orgCode,orgName,orgParentCode, orgType));
        }
    } 
	
	/**
	 * 获取用户信息
	 */
	@Test
    public void getUser() {  
        String sql = "select 部门,岗位,姓名,用户名,员工号,邮箱,密码  from hr_zh";  
        SqlRowSet set = jdbcTemplate.queryForRowSet(sql);
        HashMap<String, Person> map = new HashMap<String, Person>();
        
        while(set.next()){
        	String depId = set.getString(1);
        	String positionId = set.getString(2);
        	String name = set.getString(3);
        	String userName = set.getString(4);
        	String userId = set.getString(5);
        	String email = set.getString(6);
        	String pwd = set.getString(7);
        	
        	map.put(userId, new Person(depId, positionId, name, userName, userId, email, pwd));
        }
    } 
	
}

class Person{
	private String dep;//部门
	private String position;//岗位
	private String name;//姓名
	private String userName;//用户名
	private String userId;//员工号
	private String email;//邮箱
	private String md5Pwd;
	private String loginName;//邮箱名
	
	public Person(String dep, String position, String name, String userName,
			String userId, String email, String md5Pwd) {
		super();
		this.dep = dep;
		this.position = position;
		this.name = name;
		this.userName = userName;
		this.userId = userId;
		this.email = email;
		this.md5Pwd = md5Pwd;
	}
	public String getDep() {
		return dep;
	}
	public void setDep(String dep) {
		this.dep = dep;
	}
	public String getPosition() {
		return position;
	}
	public void setPosition(String position) {
		this.position = position;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getMd5Pwd() {
		return md5Pwd;
	}
	public void setMd5Pwd(String md5Pwd) {
		this.md5Pwd = md5Pwd;
	}
	public String getLoginName() {
		if(StringUtils.isNotBlank(email)){
			loginName = StringUtils.trimToEmpty(email.split("@")[0]);
		}
		return loginName;
	}
	public void setLoginName(String loginName) {
		this.loginName = loginName;
	}
	
}

class Organization{
	private String orgCode;
	private String orgName;
	private String orgParentCode;
	private String orgType;
	private String alias;//组织结构别名，bwtech
	private String dn;
	
	public Organization(String orgCode, String orgName, String orgParentCode,
			String orgType) {
		super();
		this.orgCode = orgCode;
		this.orgName = orgName;
		this.orgParentCode = orgParentCode;
		this.orgType = orgType;
	}
	
	private List<Organization> children = new ArrayList<Organization>();
	
	public List<Organization> getChildren() {
		return children;
	}
	public void setChildren(List<Organization> children) {
		this.children = children;
	}
	public String getAlias() {
		return alias;
	}
	public void setAlias(String alias) {
		this.alias = alias;
	}
	public String getDn() {
		return dn;
	}
	public void setDn(String dn) {
		this.dn = dn;
	}
	public String getOrgCode() {
		return orgCode;
	}
	public void setOrgCode(String orgCode) {
		this.orgCode = orgCode;
	}
	public String getOrgName() {
		return orgName;
	}
	public void setOrgName(String orgName) {
		this.orgName = orgName;
	}
	public String getOrgParentCode() {
		return orgParentCode;
	}
	public void setOrgParentCode(String orgParentCode) {
		this.orgParentCode = orgParentCode;
	}
	public String getOrgType() {
		return orgType;
	}
	public void setOrgType(String orgType) {
		this.orgType = orgType;
	}
	
}
