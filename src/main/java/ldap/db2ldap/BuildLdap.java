package ldap.db2ldap;

import java.util.ArrayList;
import java.util.List;

import javax.naming.Binding;
import javax.naming.ContextNotEmptyException;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.ldap.NameAlreadyBoundException;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.samples.useradmin.domain.JWOrganization;
import org.springframework.ldap.samples.useradmin.domain.JWUser;
import org.springframework.ldap.samples.useradmin.service.OrganizationService;
import org.springframework.ldap.samples.useradmin.service.UserService;
import org.springframework.ldap.support.LdapUtils;
import org.springframework.util.StringUtils;

public class BuildLdap{
	protected static final Log log = LogFactory.getLog(BuildLdap.class);
	
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private ContextSource contextSource;

	@Autowired
	private UserService userService;

	@Autowired
	private OrganizationService orgService;
	
	private static Boolean isAll = false;
	
	List<JWOrganization> positionList = new ArrayList<JWOrganization>();// 只存储 机构类型=岗位 的org
	
	String allUserSQL = "select [部门],[岗位],[姓名],[用户名],[员工号],[邮箱],[密码] from hr_zh LEFT JOIN org_zh on hr_zh.[岗位] = org_zh.[机构编码] where 机构类型='岗位' and [密码] is not null";
	
	String updateUserSQL = allUserSQL + " and CONVERT(varchar(100), [修改时间], 23) = CONVERT(varchar(100), getdate()-1, 23)";
	
	ApplicationContext ac;
	
	public BuildLdap() {
		ac = new ClassPathXmlApplicationContext("classpath:applicationContext.xml");
		jdbcTemplate = (JdbcTemplate)ac.getBean("jdbcTemplate");
		contextSource = (ContextSource)ac.getBean("contextSource");
		userService = (UserService)ac.getBean("userService");
		orgService = (OrganizationService)ac.getBean("orgService");
	}
	
	public static void main(String [] args) {
		if(args.length != 1){
			log.info("isAll参数默认false，确定是否全量更新LDAP组织机构数据");
		}
		isAll = Boolean.parseBoolean(args[0]);
		BuildLdap buildLdap = new BuildLdap();
		buildLdap.build(isAll);
	}
	
	private void build(boolean isAll){
		if(isAll){
			clear();//清空LDAP组织结构
		}
		List<JWOrganization> depList = getOrgRoot();// 查询组织结构
		log.info("LDAP组织结构同步：" + System.currentTimeMillis());
		// 部门及岗位
		for (JWOrganization org : depList) {
			getOrgTree(org.getOrgCode()); // 再添加部门下的节点
		}
		log.info("LDAP人员同步：" + System.currentTimeMillis());
		// 添加人员
		addPerson(positionList, isAll);

		log.info("LDAP组织人员同步结束：" + System.currentTimeMillis());
	}

	/**
	 * 得到部门根目录
	 * 
	 * @return
	 */
	public List<JWOrganization> getOrgRoot() {
		List<JWOrganization> list = new ArrayList<JWOrganization>();

		String sql = "select 机构编码,机构名称,上级编码,机构类型  from org_zh where 上级编码='1' and 机构类型='部门';";
		SqlRowSet set = jdbcTemplate.queryForRowSet(sql);

		while (set.next()) {
			String orgCode = set.getString(1);
			String orgName = set.getString(2);
			String orgParentCode = set.getString(3);
			String orgType = set.getString(4);

			JWOrganization org = new JWOrganization();
			org.setOrgCode(orgCode);
			org.setOrgName(orgName);
			org.setOrgParentCode(orgParentCode);
			org.setOrgType(orgType);
			list.add(org);
		}
		return list;
	}

	/**
	 * 构造 Org Tree，以公司以下业务部门开始查询作为根目录。不采用公司级别作为根目录减轻SQL对DB递归查询风险
	 * 
	 * @return
	 */
	public JWOrganization getOrgTree(String orgId) {
		JWOrganization orgRoot = null;
		List<JWOrganization> list = new ArrayList<JWOrganization>();

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

		SqlRowSet set = jdbcTemplate.queryForRowSet(sb.toString(),
				new Object[] { orgId });

		int i = 0;
		while (set.next()) {
			String orgCode = set.getString(1);
			String orgName = set.getString(2);
			String orgParentCode = set.getString(3);
			String orgType = set.getString(4);
			if (i == 0) {// 第一次获取 Root组织结构节点
				orgRoot = new JWOrganization(orgCode, orgName, orgParentCode,
						orgType);
				orgRoot.setId("ou=" + orgRoot.getOrgName());
				
				createOrg(orgRoot);
			} else {
				JWOrganization org = new JWOrganization(orgCode, orgName,
						orgParentCode, orgType);
				list.add(org);
				if (StringUtils.endsWithIgnoreCase("岗位", org.getOrgType())) {
					positionList.add(org);
				}
			}
			i++;
		}

		addChild(orgRoot, list);// 生成组织树
		return orgRoot;
	}
	
	private void createOrg(JWOrganization org){
		if(orgService.findJWOrg(org.getId()) == null){
			orgService.createJWOrg(org);
			log.info("LDAP组织结构更新：" + LdapUtils.newLdapName(org.getId()));
		}
	}

	private void addChild(JWOrganization father, List<JWOrganization> list) {
		for (JWOrganization org : list) {
			if (org.getOrgParentCode().equals(father.getOrgCode())) {// 是father的子节点
				org.setId(LdapUtils.prepend(LdapUtils.newLdapName("ou="
						+ StringUtils.replace(org.getOrgName(), "/", "")),
						father.getId()));
				father.getChildren().add(org);
				try {
					createOrg(org);
				} catch (NameAlreadyBoundException e1) {
					e1.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				if (org.getOrgType().equals("部门")) {
					addChild(org, list);
				}
			}
		}
	}

	/**
	 * clear ldap
	 */
	public void clear() {
		try {
			Name name = LdapUtils.emptyLdapName();
			DirContext ctx = null;
			try {
				ctx = contextSource.getReadWriteContext();
				clearSubContexts(ctx, name);
			} finally {
				try {
					ctx.close();
				} catch (Exception e) {
					// Never mind this
				}
			}
		} catch (NamingException e) {
			e.printStackTrace();
		}
	}

	public static void clearSubContexts(DirContext ctx, Name name)
			throws NamingException {
		NamingEnumeration enumeration = null;
		try {
			enumeration = ctx.listBindings(name);
			while (enumeration.hasMore()) {
				Binding element = (Binding) enumeration.next();
				Name childName = LdapUtils.newLdapName(element.getName());
				childName = LdapUtils.prepend(childName, name);

				try {
					if(element.getName().equals("ou=Group")){
						continue;//不清空Group内信息
					}
					ctx.unbind(childName);
				} catch (ContextNotEmptyException e) {
					clearSubContexts(ctx, childName);
					ctx.unbind(childName);
				}
			}
		} catch (NamingException e) {
			e.printStackTrace();
		} finally {
			try {
				enumeration.close();
			} catch (Exception e) {
				// Never mind this
			}
		}
	}

	/**
	 * isAll 是否全量更新用户
	 * 初始化添加用户
	 */
	// @Test
	public void addPerson(List<JWOrganization> orgList, boolean isAll) {
		SqlRowSet set = null;
		if(isAll){
			set = jdbcTemplate.queryForRowSet(allUserSQL);
		} else {
			set = jdbcTemplate.queryForRowSet(updateUserSQL);
		}
		

		while (set.next()) {
			String dep = set.getString(1);
			String postid = set.getString(2);// 岗位ID
			String name = set.getString(3);
			String username = set.getString(4);
			String userId = set.getString(5);
			String email = set.getString(6);
			String pwd = set.getString(7);
			
			JWUser user = new JWUser();
			for (JWOrganization org : orgList) {
				if (org.getOrgCode().equals(postid)) {
					user.setId(LdapUtils.prepend(LdapUtils.newLdapName("cn=" + StringUtils.replace(username, "/", "")), org.getId()));
					user.setTitle(org.getOrgName());
				}
			}
			user.setEmail(email);
			user.setEmployeeNumber(userId);
			user.setLastName(name);
			user.setUid(username);
			user.setUserPassword(pwd);
			if(userService.findJWUser(LdapUtils.newLdapName(user.getId())) == null){
				log.info("LDAP组织结构更新：" + LdapUtils.newLdapName(user.getId()));
				userService.createJWUser(user);
			}
		}
//		userService.createJWUser(userList);// 批量添加jwuser
	}
}
