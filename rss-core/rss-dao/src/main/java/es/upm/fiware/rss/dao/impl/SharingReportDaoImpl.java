/**
 * Copyright (C) 2015 CoNWeT Lab., Universidad Politécnica de Madrid
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
package es.upm.fiware.rss.dao.impl;

import es.upm.fiware.rss.dao.SharingReportDao;
import es.upm.fiware.rss.model.SharingReport;
import org.hibernate.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public class SharingReportDaoImpl extends GenericDaoImpl<SharingReport, Integer> implements
        SharingReportDao {
    /**
     * Variable to print the trace.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SharingReportDaoImpl.class);

    @Override
    protected Class<SharingReport> getDomainClass() {
        return SharingReport.class;
    }

    @Override
    public Optional<List<SharingReport>> getSharingReportsByParameters(String aggregator, String providerId, String productClass, boolean onlyPaid, int offset, int size) {
        // Build queries
        // TODO: Use prepared strings to avoid SQLi
        String hql = "from SharingReport l";

        boolean first = true;

        if (onlyPaid) {
            hql += " where (l.paid is null or l.paid = false)";
            first = false;
        }

        if (null != aggregator && !aggregator.isEmpty()) {
            String start = (first) ? " where" : " and";

            hql += start + " l.owner.id.aggregator.txEmail= '" + aggregator + "'";

            if (null != providerId && !providerId.isEmpty()) {
                hql += " and l.owner.id.txAppProviderId= '" + providerId + "'";

                if (null != productClass && !productClass.isEmpty()) {
                    hql += " and l.productClass= '" + productClass + "'";
                }
            }
        }

        hql += " order by l.id desc";

        List<SharingReport> resultList;
        try {
            Query q = this.getSession().createQuery(hql);

            q.setFirstResult(Integer.max(offset, 0));  // If offset negative, set it as 0

            if (size >= 0) {
                q.setMaxResults(size);
            }

            resultList = (List<SharingReport>) q.list();
        } catch (Exception e) {
            return Optional.empty();
        }

        if (null == resultList || resultList.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(resultList);
    }
}
