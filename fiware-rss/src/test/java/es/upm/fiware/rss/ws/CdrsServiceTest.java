/**
 * Revenue Settlement and Sharing System GE
 * Copyright (C) 2011-2014, Javier Lucio - lucio@tid.es
 * Telefonica Investigacion y Desarrollo, S.A.
 *
 * Copyright (C) 2015, CoNWeT Lab., Universidad Politécnica de Madrid
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

package es.upm.fiware.rss.ws;

import es.upm.fiware.rss.exception.RSSException;
import es.upm.fiware.rss.exception.UNICAExceptionType;
import es.upm.fiware.rss.model.Aggregator;
import javax.ws.rs.core.Response;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import es.upm.fiware.rss.model.CDR;
import es.upm.fiware.rss.model.RSSProvider;
import es.upm.fiware.rss.model.RSUser;
import es.upm.fiware.rss.service.AggregatorManager;
import es.upm.fiware.rss.service.CdrsManager;
import es.upm.fiware.rss.service.ProviderManager;
import es.upm.fiware.rss.service.UserManager;
import java.util.LinkedList;
import java.util.List;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;


public class CdrsServiceTest {

    @Mock private UserManager userManager;
    @Mock private CdrsManager cdrsManager;
    @Mock private AggregatorManager aggregatorManager;
    @Mock private ProviderManager providerManager;
    @InjectMocks private CdrsService toTest;

    private RSUser user;
    private final String aggregatorId = "aggregator@mail.com";
    private final String providerId = "providerId";

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        this.user = new RSUser();
        this.user.setId("userId");
        this.user.setDisplayName("username");
        this.user.setEmail("user@mail.com");

        when(userManager.getCurrentUser()).thenReturn(this.user);
    }

    @Test
    public void createCdr() throws Exception {
        List <CDR> list = new LinkedList<>();
        when(userManager.isAdmin()).thenReturn(true);

        toTest.createCdr(list);
        verify(cdrsManager).createCDRs(list);
    }

    @Test
    public void createCdrNotAllowed() throws Exception {
        List<CDR> list = new LinkedList<>();

        try {
            toTest.createCdr(list);
            Assert.fail();
        } catch (RSSException e) {
            Assert.assertEquals(UNICAExceptionType.NON_ALLOWED_OPERATION, e.getExceptionType());
            Assert.assertEquals("Operation is not allowed: You are not allowed to create transactions", e.getMessage());
        }
    }

    private void testCorrectCDRsRetrieving(String effectiveAggregator,
            String effectiveProvider, String aggregator,
            String provider) throws Exception {

        List <CDR> list = new LinkedList<>();
        when(cdrsManager.getCDRs(effectiveAggregator, effectiveProvider)).thenReturn(list);

        Response response = toTest.getCDRs(aggregator, provider);

        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(list, response.getEntity());
    }

    @Test
    public void getProviderCDRsAdminUser() throws Exception {
        when(userManager.isAdmin()).thenReturn(true);
        this.testCorrectCDRsRetrieving(
                this.aggregatorId, this.providerId, this.aggregatorId, this.providerId);
    }

    @Test
    public void getDefaultAggregatorCDRsAggregatorUser() throws Exception {
        when(userManager.isAggregator()).thenReturn(true);

        Aggregator defaultAggregator = new Aggregator();
        defaultAggregator.setAggregatorId(this.user.getEmail());

        when(aggregatorManager.getDefaultAggregator()).thenReturn(defaultAggregator);

        this.testCorrectCDRsRetrieving(this.user.getEmail(), null, null, null);
    }

    @Test
    public void getProviderCDRsSellerUser() throws Exception {
        when(userManager.isSeller()).thenReturn(true);

        RSSProvider provider = new RSSProvider();
        provider.setAggregatorId(this.aggregatorId);
        provider.setProviderId(providerId);
        this.user.setId(providerId);

        when(providerManager.getProvider(this.aggregatorId, this.providerId)).thenReturn(provider);

        this.testCorrectCDRsRetrieving(this.aggregatorId, this.providerId, this.aggregatorId, this.providerId);
    }

    @Test
    public void getDefaultProviderCDRsSellerUser() throws Exception {
        when(userManager.isSeller()).thenReturn(true);

        RSSProvider provider = new RSSProvider();
        provider.setAggregatorId(this.aggregatorId);

        when(providerManager.getProvider(this.aggregatorId, this.user.getId())).thenReturn(provider);

        this.testCorrectCDRsRetrieving(this.aggregatorId, this.user.getId(), this.aggregatorId, null);
    }

    private void testExceptionCDRRetrieving (
            String aggregator, String provider, String msg) throws Exception {
        try {
            toTest.getCDRs(aggregator, provider);
            Assert.fail();
        } catch (RSSException e) {
            Assert.assertEquals(UNICAExceptionType.NON_ALLOWED_OPERATION, e.getExceptionType());
            Assert.assertEquals("Operation is not allowed: " + msg, e.getMessage());
        }
    }

    @Test
    public void throwExceptionGetCDRsNoRole() throws Exception {
        this.testExceptionCDRRetrieving(
                this.aggregatorId, this.providerId,
                "You are not allowed to retrieve transactions");
    }

    @Test
    public void throwExceptionNoDefaultAggregator() throws Exception {
        when(userManager.isAggregator()).thenReturn(true);
        this.testExceptionCDRRetrieving(
                null, null, "There isn't any aggregator registered");
    }

    @Test
    public void throwExceptionAggregatorNotAllowed() throws Exception {
        when(userManager.isAggregator()).thenReturn(true);
        this.testExceptionCDRRetrieving(aggregatorId, null,
                "You are not allowed to retrieve transactions of the specified aggregator");
    }

    @Test
    public void throwExceptionProviderPofileNotExisting() throws Exception {
        when(userManager.isSeller()).thenReturn(true);
        this.testExceptionCDRRetrieving(
                aggregatorId, null, "You do not have a provider profile, please contact with the administrator");
    }

    @Test
    public void throwExceptionProviderNotAllowed() throws Exception {
        when(userManager.isSeller()).thenReturn(true);

        RSSProvider provider = new RSSProvider();
        provider.setAggregatorId(this.aggregatorId);
        provider.setProviderId(this.user.getId());

        when(providerManager.getProvider(this.aggregatorId, this.user.getId())).thenReturn(provider);

        this.testExceptionCDRRetrieving(
                this.aggregatorId, this.providerId,
                "You are not allowed to retrieve transactions of the specified provider");
    }
}
