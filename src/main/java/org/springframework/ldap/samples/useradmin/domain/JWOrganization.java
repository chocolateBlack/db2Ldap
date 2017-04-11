package org.springframework.ldap.samples.useradmin.domain;

import java.util.ArrayList;
import java.util.List;

import javax.naming.Name;

import org.springframework.data.domain.Persistable;
import org.springframework.ldap.odm.annotations.Attribute;
import org.springframework.ldap.odm.annotations.DnAttribute;
import org.springframework.ldap.odm.annotations.Entry;
import org.springframework.ldap.odm.annotations.Id;
import org.springframework.ldap.odm.annotations.Transient;
import org.springframework.ldap.support.LdapUtils;

/**
 * @author jgh
 */
@Entry(objectClasses = { "organizationalUnit",  "top"})
public final class JWOrganization implements Persistable<Name>{
	private static final long serialVersionUID = 1L;
	
	@Id
	private Name id;
	
    @Attribute(name = "ou")
    @DnAttribute(value="ou")
    private String fullName;
    
	@Transient
   	private String orgCode;
	@Transient
	private String orgName;
	@Transient
	private String orgParentCode;
	@Transient
	private String orgType;
	@Transient
	private List<JWOrganization> children = new ArrayList<JWOrganization>();
	
	public List<JWOrganization> getChildren() {
		return children;
	}
	public void setChildren(List<JWOrganization> children) {
		this.children = children;
	}
	
    public JWOrganization(String orgCode, String orgName,
			String orgParentCode, String orgType) {
		this.orgCode=orgCode;
		this.orgName=orgName;
		this.orgParentCode= orgParentCode;
		this.orgType=orgType;
		this.fullName = orgName;
	}

	public JWOrganization() {
		// TODO Auto-generated constructor stub
	}
	public Name getId() {
        return id;
    }

    public void setId(Name id) {
        this.id = id;
    }
    
    public void setId(String id) {
        this.id = LdapUtils.newLdapName(id);
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

	public String getFullName() {
		return fullName;
	}
	public void setFullName(String fullName) {
		this.fullName = fullName;
	}
	@Override
	public boolean isNew() {
		return true;
	}
	
}
