package ldap.db2ldap;

import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.samples.useradmin.domain.JWUser;
import org.springframework.ldap.samples.useradmin.service.UserService;
import org.springframework.ldap.support.LdapUtils;
import org.springframework.ldap.test.LdapTestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.StringUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={
    "classpath:applicationContext.xml"
})
public class InitOrgTest {
	@Autowired
	private JdbcTemplate jdbcTemplate;

    @Autowired
    private LdapTemplate ldapTemplate;
    
    @Autowired
    private ContextSource contextSource;

    @Autowired
    private UserService userService;
    
    @Test
    public void test(){
//    	List<Organization> depList = initOrgTree();
//    	initOrganization(depList);
    	
    	addPerson(positionList);
//    	initPerson();
    }
	
    public List<Organization> initOrgTree() {  
        List<Organization> list = new ArrayList<Organization>();
        
        String sql = "select 机构编码,机构名称,上级编码,机构类型  from org_zh where 上级编码='1' and 机构类型='部门';";  
        SqlRowSet set = jdbcTemplate.queryForRowSet(sql);
        
        while(set.next()){
        	String orgCode = set.getString(1);
        	String orgName = set.getString(2);
        	String orgParentCode = set.getString(3);
        	String orgType = set.getString(4);
        	list.add(new Organization(orgCode, orgName, orgParentCode, orgType));
        }
        return list;
    } 
    
    
    /**
     * 构造 Org Tree
     * @return
     */
    public List<Organization> getOrgTree() {  
        String sql = "select 机构编码,机构名称,上级编码,机构类型  from org_zh ";  
        SqlRowSet set = jdbcTemplate.queryForRowSet(sql);
        List<Organization> list = new ArrayList<Organization>();
        while(set.next()){
        	String orgCode = set.getString(1);
        	String orgName = set.getString(2);
        	String orgParentCode = set.getString(3);
        	String orgType = set.getString(4);
        	list.add( new Organization(orgCode,orgName,orgParentCode, orgType));
        }
        return list;
    } 
	
    private void initPerson() {
        String sql = "select 部门,岗位,姓名,用户名,员工号,邮箱,密码,修改日期 from org_zh";
        SqlRowSet set = jdbcTemplate.queryForRowSet(sql);
        List<Organization> list = new ArrayList<Organization>();
        
        while(set.next()){
        	String orgCode = set.getString(1);
        	String orgName = set.getString(2);
        	String orgParentCode = set.getString(3);
        	String orgType = set.getString(4);
        	list.add( new Organization(orgCode,orgName,orgParentCode, orgType));
        }
	}
    
    List<Organization> positionList = new ArrayList<Organization>();//只存储  机构类型=岗位 的org
    
    /**
     * 构造 Org Tree，以公司以下业务部门开始查询作为根目录。不采用公司级别作为根目录减轻SQL对DB递归查询风险
     * @return
     */
    public List<Organization> getOrgTree(String orgId) {  
    	Organization orgRoot = null;
    	List<Organization> list = new ArrayList<Organization>();
    	
    	StringBuilder sb = new StringBuilder();
    	sb.append(";WITH  CTE");
    	sb.append(" AS ");
    	sb.append("(");
    	sb.append(" SELECT  机构编码, [机构名称], 上级编码, 机构类型");
    	sb.append(" FROM org_zh");
    	sb.append(" WHERE 机构编码=?");
    	sb.append(" UNION ALL");
    	sb.append(" SELECT t.机构编码, t.[机构名称], t.上级编码, t.机构类型");
    	sb.append(" FROM org_zh AS t");
    	sb.append(" JOIN CTE AS a ON t.上级编码 = a.机构编码");
    	sb.append(" )");
    	sb.append(" select * from  CTE;");

        SqlRowSet set = jdbcTemplate.queryForRowSet(sb.toString(), new Object[]{orgId});
        
        int i = 0;
        while(set.next()){
        	String orgCode = set.getString(1);
        	String orgName = set.getString(2);
        	String orgParentCode = set.getString(3);
        	String orgType = set.getString(4);
        	if(i==0){//第一次获取 Root组织结构节点
        		orgRoot = new Organization(orgCode, orgName, orgParentCode, orgType);
        		orgRoot.setDn("ou=" + orgRoot.getOrgName());
        	} else {
        		Organization org = new Organization(orgCode, orgName, orgParentCode, orgType);
        		list.add(org);
        		if("岗位".equals(org.getOrgType())){
        			positionList.add(org);
        		}
        	}
        	i++;
        }
        addChild(orgRoot, list);//生成组织树
        
    	Attributes attr = new BasicAttributes(); 
		BasicAttribute ocattr = new BasicAttribute("objectclass");
		ocattr.add("organizationalUnit");
		ocattr.add("top");
		attr.put(ocattr);
        
		addLdapOrganization(attr, orgRoot);
        //查看结果
//		getChild(orgRoot);
        return list;
    } 
    
    private void getChild(Organization orgRoot){
    	 for(Organization org : orgRoot.getChildren()){
         	System.out.println(org.getOrgName() + "--" + org.getDn());
         	if(org.getOrgType().equals("部门")){//有子节点
         		getChild(org);
         	}
         }
    }
    
    private void addChild(Organization father, List<Organization> list){
    	for(Organization org: list){
    		if(org.getOrgParentCode().equals(father.getOrgCode())){//是father的子节点
    			org.setDn("ou=" + org.getOrgName() + ", " + father.getDn());
    			father.getChildren().add(org);
    			if(org.getOrgType().equals("部门")){
    				addChild(org, list);
    			}
    		}
    	}
    }
    
    private void addLdapOrganization(Attributes attr, Organization orgRoot){
    	for(Organization org : orgRoot.getChildren()){
    		ldapTemplate.bind(StringUtils.replace(org.getDn(), "/", ""), null, attr);
    		if(org.getOrgType().equals("部门")){//有子节点
    			addLdapOrganization(attr, org);
         	}
        }
    }
    
    
//    @Test
    public void initOrganization(List<Organization> depList){
    	Attributes attr = new BasicAttributes(); 
		BasicAttribute ocattr = new BasicAttribute("objectclass");
		ocattr.add("organizationalUnit");
		ocattr.add("top");
		attr.put(ocattr);
		
		System.out.println(System.currentTimeMillis());
		for(Organization org: depList){
			ldapTemplate.bind("ou="+org.getOrgName(), null, attr);
			getOrgTree(org.getOrgCode());
		}
		System.out.println(System.currentTimeMillis());
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
	
	/**
	 * 测试批量初始化添加用户
	 */
	public void addPerson(List<Organization> orgList){
		StringBuilder sb = new StringBuilder();
		sb.append("select [部门],[岗位],[姓名],[用户名],[员工号],[邮箱],[密码]  from hr_zh LEFT JOIN org_zh on hr_zh.[岗位] = org_zh.[机构编码] where 机构类型='岗位'");
		SqlRowSet set = jdbcTemplate.queryForRowSet(sb.toString());
		List<JWUser> userList = new ArrayList<JWUser>();
		
		while(set.next()){
			String dep = set.getString(1);
			String postid = set.getString(2);//岗位ID
			String name = set.getString(3);
			String username = set.getString(4);
			String userId = set.getString(5);
			String email = set.getString(6);
			String pwd = set.getString(7);
			
	    	JWUser user = new JWUser();
	    	for(Organization org: orgList){
	    		if(org.getOrgCode().equals(postid)){
	    			user.setId("cn=" + username + ", " +org.getDn());
	    		}
	    	}
			user.setEmail(email);
			user.setEmployeeNumber(userId);
			user.setLastName("lastName");
//			user.setPhone("123");
			user.setTitle(postid);
//			user.setFullName(name);
			user.setUid(username);
			user.setUserPassword(pwd);
			
			userList.add(user);
		}
		
		userService.createJWUser(userList);// 批量添加jwuser
	}
}
