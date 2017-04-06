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
	
	public void initOrganization(){
		List<Organization> depList = initOrgTree();
    	
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
  
    
    /**
     *  检查组织结构 树
     * @param orgRoot
     */
    private void getChild(Organization orgRoot){
    	for(Organization org : orgRoot.getChildren()){
         	System.out.println(org.getOrgName() + "--" + org.getDn());
         	if(org.getOrgType().equals("部门")){//有子节点
         		getChild(org);
         	}
         }
    }
    
    /**
     * 递归获取组织结构Tree节点
     * @param father
     * @param list
     */
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
    
    /**
     * 生成组织结构树
     * @param attr
     * @param orgRoot
     */
    private void addLdapOrganization(Attributes attr, Organization orgRoot){
    	for(Organization org : orgRoot.getChildren()){
    		ldapTemplate.bind(StringUtils.replace(org.getDn(), "/", ""), null, attr);
    		if(org.getOrgType().equals("部门")){//有子节点
    			addLdapOrganization(attr, org);
         	}
        }
    }
    
    /**
     * 部门根节点
     * @return
     */
  	public List<Organization> initOrgTree() {  
      List<Organization> list = new ArrayList<Organization>();
      
      String sql = "select 机构编码,机构名称,上级编码,机构类型  from org_zh where 上级编码='1' and 机构类型='部门';";  
      SqlRowSet set = jdbcTemplate.queryForRowSet(sql);
      
      while(set.next()){
      	String orgCode = set.getString(1);
      	String orgName = set.getString(2);
      	String orgParentCode = set.getString(3);
      	String orgType = set.getString(4);
      	list.add(new Organization(orgCode,orgName,orgParentCode, orgType));
      }
      return list;
  } 
	
}
