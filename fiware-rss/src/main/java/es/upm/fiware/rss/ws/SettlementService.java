/**
 * Copyright (C) 2015 - 2016, CoNWeT Lab., Universidad Politécnica de Madrid
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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.MediaType;

import org.springframework.beans.factory.annotation.Autowired;

import es.upm.fiware.rss.exception.RSSException;
import es.upm.fiware.rss.exception.UNICAExceptionType;
import es.upm.fiware.rss.model.RSSReport;
import es.upm.fiware.rss.model.RSUser;
import es.upm.fiware.rss.model.SettlementJob;
import es.upm.fiware.rss.service.SettlementManager;
import es.upm.fiware.rss.service.UserManager;
import java.util.Map;

/**
 *
 * @author fdelavega
 */
@WebService(serviceName = "settlement", name = "settlement")
@Path("/")
public class SettlementService {

    @Autowired
    SettlementManager settlementManager;

    @Autowired
    UserManager userManager;

    private boolean isValidURL(String urlStr) {
        boolean res = true;
        try {
            new URL(urlStr);
        }
        catch (MalformedURLException e) {
            res = false;
        }
        return res;
    }

    @WebMethod
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response launchSettlement(SettlementJob task) throws Exception {

        // Check basic permissions
        Map<String, String> ids = this.userManager.getAllowedIdsSingleProvider(
                task.getAggregatorId(), task.getProviderId(), "launch settlement");

        //Override RS models fields with the effective aggregator and provider
        task.setAggregatorId(ids.get("aggregator"));
        task.setProviderId(ids.get("provider"));

        // Validate task URL
        if(!this.isValidURL(task.getCallbackUrl())) {
            String[] args = {"callbackUrl"};
            throw new RSSException(UNICAExceptionType.CONTENT_NOT_WELL_FORMED, args);
        }
        
        // Launch process
        settlementManager.runSettlement(task);
        Response.ResponseBuilder rb = Response.status(Response.Status.ACCEPTED.getStatusCode());
        return rb.build();
    }

    @WebMethod
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/reports")
    public Response getReports(
            @QueryParam("aggregatorId") String aggregatorId,
            @QueryParam("providerId") String providerId,
            @QueryParam("productClass") String productClass)
            throws Exception {
   
        // Check basic permissions
        Map<String, String> ids = this.userManager.getAllowedIds(
                aggregatorId, providerId, "RS reports");

        List<RSSReport> files = settlementManager.getSharingReports(
                ids.get("aggregator"), ids.get("provider"), productClass);

        Response.ResponseBuilder rb = Response.status(Response.Status.OK.getStatusCode());
        rb.entity(files);
        return rb.build();
    }
}
