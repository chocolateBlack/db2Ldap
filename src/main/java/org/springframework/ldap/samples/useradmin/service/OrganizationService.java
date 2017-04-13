/*
 * Copyright 2005-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.ldap.samples.useradmin.service;

import java.util.List;

import javax.naming.Name;
import javax.naming.ldap.LdapName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.support.BaseLdapNameAware;
import org.springframework.ldap.samples.useradmin.domain.DirectoryType;
import org.springframework.ldap.samples.useradmin.domain.JWOrganization;
import org.springframework.ldap.samples.useradmin.domain.JWOrganizationRepo;
import org.springframework.ldap.samples.useradmin.domain.JWUser;
import org.springframework.ldap.support.LdapUtils;

/**
 * @author Mattias Hellborg Arthursson
 */
public class OrganizationService implements BaseLdapNameAware {
    private JWOrganizationRepo jwOrgRepo;
    private LdapName baseLdapPath;
    private DirectoryType directoryType;

    @Autowired
    public OrganizationService(JWOrganizationRepo jwOrgRepo) {
    	this.jwOrgRepo = jwOrgRepo;
    }

    public Iterable<JWOrganization> createJWOrg(List<JWOrganization> userList) {
    	Iterable<JWOrganization> list = jwOrgRepo.save(userList);
        return list;
    }
    
    public JWOrganization createJWOrg(JWOrganization org) {
    	JWOrganization jwOrg = jwOrgRepo.save(org);
        return jwOrg;
    }
    
    public JWOrganization findJWOrg(String orgId) {
        return jwOrgRepo.findOne(LdapUtils.newLdapName(orgId));
    }
    
    public JWOrganization findJWOrg(Name orgId) {
        return jwOrgRepo.findOne(orgId);
    }

	@Override
	public void setBaseLdapPath(LdapName baseLdapPath) {
        this.baseLdapPath = baseLdapPath;
	}
	
    public void setDirectoryType(DirectoryType directoryType) {
        this.directoryType = directoryType;
    }

}
