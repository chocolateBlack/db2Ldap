# db2Ldap
在spring ldap test基础上开发的，将关系型数据库中组织机构数据同步到LDAP中。

LdapTest存包含各种测试用例，测试向LDAP中增加组织结构，增加GROUP,增加User。

BuildLdapTreeTest是基于自身环境，开发能够全量和增量同步关系数据库中结构至LDAP中。（代码中以SQL Server作为关系型数据）  
1、获取关系型DB中组织机构关系  

2、生成树型数据结构  

3、LDAP增加组织结构节点  

4、获取关系型数据库中用户与组织机构关联关系。  

5、LDAP增加用户节点
