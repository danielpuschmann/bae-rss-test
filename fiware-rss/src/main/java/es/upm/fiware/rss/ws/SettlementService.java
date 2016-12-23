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

import es.upm.fiware.rss.controller.JsonResponse;
import es.upm.fiware.rss.exception.RSSException;
import es.upm.fiware.rss.exception.UNICAExceptionType;
import es.upm.fiware.rss.model.RSSReport;
import es.upm.fiware.rss.model.Count;
import es.upm.fiware.rss.model.SettlementJob;
import es.upm.fiware.rss.service.SettlementManager;
import es.upm.fiware.rss.service.UserManager;
import es.upm.fiware.rss.ws.patch.PATCH;
import es.upm.fiware.rss.ws.patch.PatchAction;
import org.springframework.beans.factory.annotation.Autowired;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
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
        Map<String, String> ids = this.userManager.getAllowedIds(
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

    private boolean applyPatch(int id, PatchAction action) {
        // If we need more patchs add them here, by now only replace paids
        if (action.getPath().equals("/paid") && action.getOp().equals("replace")) {
            return settlementManager.setPayReport(id, action.getValue().equals("true")).orElse(false);
        }

        return false;
    }

    /**
     * PATCH based on RFC5789, where it receives a list of description of changes.
     * We get a list of descriptions like {"op": "replace", "path": "/parameter", "value": "value"}
     * To read more about it: http://williamdurand.fr/2014/02/14/please-do-not-patch-like-an-idiot/
     */
    @WebMethod
    @PATCH
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/reports/{id}")
    public Response patchReport(@PathParam("id") int id, List<PatchAction> actions) throws Exception {
        Response.ResponseBuilder rb = Response.status(Response.Status.OK.getStatusCode());
        JsonResponse response = new JsonResponse();

        if (this.userManager.isAdmin()) {
            // Apply actions (First map needs lambda function because lack of partial application
            response.setSuccess(actions.stream().map(act -> this.applyPatch(id, act)).allMatch(Boolean::booleanValue));
        }
        rb.entity(response);
        return rb.build();
    }

    @WebMethod
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/reports")
    public Response getReports(
            @QueryParam("aggregatorId") String aggregatorId,
            @QueryParam("providerId") String providerId,
            @QueryParam("productClass") String productClass,
            @QueryParam("action") String action,
            @DefaultValue("false") @QueryParam("onlyPaid") boolean onlyPaid,
            @DefaultValue("0") @QueryParam("offset") int offset,
            @DefaultValue("-1") @QueryParam("size") int size)
            throws Exception {

        Object resp;

        // Check basic permissions
        Map<String, String> ids = this.userManager.getAllowedIds(
                aggregatorId, providerId, "RS reports");

        if (action != null && action.toLowerCase().equals("count")) {
            resp = (Count) settlementManager.countSharingReports(
                ids.get("aggregator"), ids.get("provider"), productClass, onlyPaid);
        } else {
            resp = (List<RSSReport>) settlementManager.getSharingReports(
                ids.get("aggregator"), ids.get("provider"), productClass, onlyPaid, offset, size);
        }

        Response.ResponseBuilder rb = Response.status(Response.Status.OK.getStatusCode());
        rb.entity(resp);
        return rb.build();
    }
}
