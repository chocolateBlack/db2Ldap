package ldap.db2ldap;

import java.util.ArrayList;
import java.util.List;

import javax.naming.Binding;
import javax.naming.ContextNotEmptyException;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.ldap.NameAlreadyBoundException;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.samples.useradmin.domain.JWOrganization;
import org.springframework.ldap.samples.useradmin.domain.JWUser;
import org.springframework.ldap.samples.useradmin.domain.User;
import org.springframework.ldap.samples.useradmin.service.OrganizationService;
import org.springframework.ldap.samples.useradmin.service.UserService;
import org.springframework.ldap.support.LdapUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.StringUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext.xml" })
public class BuildLdapTreeTest {
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private LdapTemplate ldapTemplate;

	@Autowired
	private ContextSource contextSource;

	@Autowired
	private UserService userService;

	@Autowired
	private OrganizationService orgService;

	@Test
	public void test() {
		List<JWOrganization> depList = getOrgRoot();// 查询组织结构
		System.out.println(System.currentTimeMillis());
		// 部门及岗位
		for (JWOrganization org : depList) {
			getOrgTree(org.getOrgCode()); // 再添加部门下的节点
		}
		System.out.println(System.currentTimeMillis());
		// 人员
		addPerson(positionList);
		System.out.println(System.currentTimeMillis());
		// initPerson();
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

	private void initPerson() {
		String sql = "select 部门,岗位,姓名,用户名,员工号,邮箱,密码,修改日期 from org_zh";
		SqlRowSet set = jdbcTemplate.queryForRowSet(sql);
		List<Organization> list = new ArrayList<Organization>();

		while (set.next()) {
			String orgCode = set.getString(1);
			String orgName = set.getString(2);
			String orgParentCode = set.getString(3);
			String orgType = set.getString(4);
			list.add(new Organization(orgCode, orgName, orgParentCode, orgType));
		}
	}

	List<JWOrganization> positionList = new ArrayList<JWOrganization>();// 只存储
																		// 机构类型=岗位
																		// 的org

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
				orgService.createJWOrg(orgRoot);
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

		// Attributes attr = new BasicAttributes();
		// BasicAttribute ocattr = new BasicAttribute("objectclass");
		// ocattr.add("organizationalUnit");
		// ocattr.add("top");
		// attr.put(ocattr);

		// addLdapOrganization(attr, orgRoot);
		// orgService.createJWOrg(orgList);

		return orgRoot;
	}

	private void addChild(JWOrganization father, List<JWOrganization> list) {
		for (JWOrganization org : list) {
			if (org.getOrgParentCode().equals(father.getOrgCode())) {// 是father的子节点
/*				if (org.getOrgType().equals("部门")) {
					org.setId(LdapUtils.prepend(LdapUtils.newLdapName("ou="
							+ StringUtils.replace(org.getOrgName(), "/", "")),
							father.getId()));
				} else if(org.getOrgType().equals("岗位")){//采用orgCode作为ou的value，解决同一部门下两个同名岗位的情况
					org.setId(LdapUtils.prepend(LdapUtils.newLdapName("ou="
							+ StringUtils.replace(org.getOrgCode(), "/", "")),
							father.getId()));
				}*/
				
				org.setId(LdapUtils.prepend(LdapUtils.newLdapName("ou="
						+ StringUtils.replace(org.getOrgName(), "/", "")),
						father.getId()));
				father.getChildren().add(org);
				try {
					orgService.createJWOrg(org);
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
	@Test
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
	 * 测试批量初始化添加用户
	 */
	// @Test
	public void addPerson(List<JWOrganization> orgList) {
		StringBuilder sb = new StringBuilder();
		sb.append("select [部门],[岗位],[姓名],[用户名],[员工号],[邮箱],[密码] from hr_zh LEFT JOIN org_zh on hr_zh.[岗位] = org_zh.[机构编码] where 机构类型='岗位' and [密码] is not null");
		SqlRowSet set = jdbcTemplate.queryForRowSet(sb.toString());
		List<JWUser> userList = new ArrayList<JWUser>();
		System.out.println("orgList大小:" + orgList.size());
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
					System.out.println(LdapUtils.newLdapName(user.getId()));
					user.setTitle(org.getOrgName());
				}
			}
			user.setEmail(email);
			user.setEmployeeNumber(userId);
			user.setLastName(name);
			user.setUid(username);
			user.setUserPassword(pwd);
			userService.createJWUser(user);
			userList.add(user);
		}
		userService.createJWUser(userList);// 批量添加jwuser
	}

	@Test
	public void searchPerson() {
		User user = userService
				.findUser("ou=103020601,ou=管理层,ou=慧云事业部,ou=业务,dc=openldap,dc=jw,dc=cn");
		System.out.println(user.getEmail());
	}
	
	@Test
	public void searchOrg() {
		JWOrganization jwOrg = orgService.findJWOrg("ou=103020601,ou=管理层,ou=慧云事业部,ou=业务");
		System.out.println(jwOrg.getFullName());
	}
}
