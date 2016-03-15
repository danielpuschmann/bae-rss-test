/**
 * Revenue Settlement and Sharing System GE
 * Copyright (C) 2015 - 2016 , CoNWeT Lab., Universidad Politécnica de Madrid
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package es.upm.fiware.rss.service;

import es.upm.fiware.rss.common.properties.AppProperties;
import es.upm.fiware.rss.dao.UserDao;
import es.upm.fiware.rss.exception.RSSException;
import es.upm.fiware.rss.exception.UNICAExceptionType;
import es.upm.fiware.rss.model.RSUser;
import es.upm.fiware.rss.model.Role;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

/**
 *
 * @author jortiz
 */

public class UserManagerTest {

    @Mock UserDao userDaoMock;
    @Mock AppProperties appProperties;
    @InjectMocks private UserManager userManager;

    public UserManagerTest() {}

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        
        when(appProperties.getProperty("config.grantedRole")).thenReturn("admin");
        when(appProperties.getProperty("config.sellerRole")).thenReturn("seller");
        when(appProperties.getProperty("config.aggregatorRole")).thenReturn("aggregator");
    }

    @Test
    public void getCurrentUser() throws RSSException {
        RSUser rSUser = new RSUser();
        when(userDaoMock.getCurrentUser()).thenReturn(rSUser);

        RSUser returned = userManager.getCurrentUser();

        assertEquals(rSUser, returned);
    }

    @Test
    public void getCurrentUserNoneExisting() throws RSSException{
        when(userDaoMock.getCurrentUser()).thenReturn(null);

        try {
            userManager.getCurrentUser();
        } catch (RSSException e) {
            assertEquals(UNICAExceptionType.NON_ALLOWED_OPERATION, e.getExceptionType());
            assertEquals("Your user is not authorized to access the RSS", e.getMessage());
        }
    }

    private void mockUser(String ... roles) {
        RSUser rSUser = new RSUser();
        Set <Role> rolesSet = new HashSet<>();
        
        for (String role: roles) {
            Role r = new Role();
            r.setId(role);
            r.setName(role);
            rolesSet.add(r);
        }
        rSUser.setRoles(rolesSet);
        
        when(userDaoMock.getCurrentUser()).thenReturn(rSUser);
    }

    @Test
    public void isAdmin() throws RSSException {
        this.mockUser("other", "admin");
        assertTrue(userManager.isAdmin());
    }

    @Test
    public void isNotAdmin() throws RSSException {
        this.mockUser();
        assertFalse(userManager.isAdmin());
    }

    @Test
    public void isAggregator () throws RSSException {
        this.mockUser("aggregator", "other");
        assertTrue(userManager.isAggregator());
    }

    @Test
    public void isNotAggregator () throws RSSException {
        this.mockUser("provider", "other");
        assertFalse(userManager.isAggregator());
    }

    @Test
    public void isSeller () throws RSSException {
        this.mockUser("aggregator", "seller");
        assertTrue(userManager.isSeller());
    }

    @Test
    public void isNotSeller () throws RSSException {
        this.mockUser("provider", "other");
        assertFalse(userManager.isSeller());
    }
}
