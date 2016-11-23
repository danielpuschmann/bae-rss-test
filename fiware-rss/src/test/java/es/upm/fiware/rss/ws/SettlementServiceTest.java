/**
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
import es.upm.fiware.rss.model.Count;
import es.upm.fiware.rss.model.RSSReport;
import es.upm.fiware.rss.model.SettlementJob;
import es.upm.fiware.rss.service.SettlementManager;
import es.upm.fiware.rss.service.UserManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class SettlementServiceTest {

    @Mock SettlementManager settlementManager;
    @Mock UserManager userManager;
    @InjectMocks SettlementService toTest;

    private final String aggregatorId = "aggregator@mail.com";
    private final String providerId = "provider";
    private final String effectiveProvider = "provider2";

    private Map<String, String> ids = new HashMap<>();
    private SettlementJob task = new SettlementJob();

    public SettlementServiceTest() {
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        ids.put("aggregator", aggregatorId);
        ids.put("provider", effectiveProvider);

        task.setAggregatorId(aggregatorId);
        task.setProviderId(providerId);
        task.setProductClass("productClass");
        task.setCallbackUrl("http://google.com");
    }

    @Test
    public void correctlyExecuteSettlementWithValidTask() throws Exception {
        when(userManager.getAllowedIds(
                aggregatorId, providerId, "launch settlement")).thenReturn(ids);

        Response response = toTest.launchSettlement(task);

        Assert.assertEquals(
                Response.Status.ACCEPTED.getStatusCode(), response.getStatus());

        Assert.assertEquals(effectiveProvider, task.getProviderId());

        verify(settlementManager).runSettlement(task);
    }

    @Test
    public void throwsRSSExceptionInvalidUrl() throws Exception {
        task.setCallbackUrl("invalid");

        try {
            toTest.launchSettlement(task);
            Assert.fail();
        } catch (RSSException e) {
            Assert.assertEquals(
                    UNICAExceptionType.CONTENT_NOT_WELL_FORMED,
                    e.getExceptionType());

            Assert.assertEquals(
                    "Parser Error: callbackUrl content not well formed",
                    e.getMessage());
        }
    }

    @Test
    public void reportsRetrieved() throws Exception {
        List<RSSReport> expResult = new ArrayList<>();
        RSSReport rep = new RSSReport();

        expResult.add(rep);
        String productClass = "productClass";

        when(userManager.getAllowedIds(
                aggregatorId, providerId, "RS reports")).thenReturn(ids);

        when(settlementManager.getSharingReports(
                aggregatorId, effectiveProvider, productClass, false, 0, -1)).thenReturn(expResult);

        Response response = toTest.getReports(
                aggregatorId, providerId, productClass, null, false, 0, -1);

        Assert.assertEquals(
                Response.Status.OK.getStatusCode(), response.getStatus());

        List<RSSReport> resp = (List<RSSReport>) response.getEntity();
        Assert.assertEquals(expResult, resp);
    }

    @Test
    public void reportsCountRetrieved() throws Exception {
        Count expResult = new Count();
        String productClass = "productClass";

        when(userManager.getAllowedIds(
                aggregatorId, providerId, "RS reports")).thenReturn(ids);

        when(settlementManager.countSharingReports(
                aggregatorId, effectiveProvider, productClass, false)).thenReturn(expResult);

        Response response = toTest.getReports(
                aggregatorId, providerId, productClass, "count", false, 0, -1);

        Assert.assertEquals(
                Response.Status.OK.getStatusCode(), response.getStatus());

        Count resp = (Count) response.getEntity();
        Assert.assertEquals(expResult, resp);
    }
}
