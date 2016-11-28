/**
 * Revenue Settlement and Sharing System GE
 * Copyright (C) 2011-2014, Javier Lucio - lucio@tid.es
 * Telefonica Investigacion y Desarrollo, S.A.
 *
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

package es.upm.fiware.rss.service;

import es.upm.fiware.rss.dao.*;
import es.upm.fiware.rss.exception.RSSException;
import es.upm.fiware.rss.exception.UNICAExceptionType;
import es.upm.fiware.rss.model.*;
import es.upm.fiware.rss.settlement.ProductSettlementTask;
import es.upm.fiware.rss.settlement.SettlementTaskFactory;
import es.upm.fiware.rss.settlement.ThreadPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


@Service
@Transactional(rollbackFor = Exception.class)
public class SettlementManager {

    /***
     * Logging system.
     */
    private final Logger logger = LoggerFactory.getLogger(SettlementManager.class);

    /**
     *
     */
    @Autowired
    private DbeTransactionDao transactionDao;

    @Autowired
    private DbeAppProviderDao appProviderDao;

    @Autowired
    private SharingReportDao sharingReportDao;

    @Autowired
    private ReportProviderDao reportProviderDao;

    @Autowired
    private CurrencyDao currencyDao;

    @Autowired
    private SettlementTaskFactory taskFactory;

    @Autowired
    private AggregatorManager aggregatorManager;

    @Autowired
    private ProviderManager providerManager;

    @Autowired
    private RSSModelsManager modelsManager;

    @Autowired
    private ThreadPoolManager poolManager;

    private List<Aggregator> getAggregators(String aggregatorId)
            throws RSSException{

        // Get given aggregators if needed
        List<Aggregator> aggregators;
        if (aggregatorId == null || aggregatorId.isEmpty()) {
            aggregators = this.aggregatorManager.getAPIAggregators();
        } else {
            aggregators = new ArrayList<>();
            aggregators.add(this.aggregatorManager.getAggregator(aggregatorId));
        }
        return aggregators;
    }

    private List<RSSProvider> getProviders(String aggregatorId,
            String providerId) throws RSSException {

        List<RSSProvider> providers;
        if (providerId == null || providerId.isEmpty()) {
            providers = this.providerManager.getAPIProviders(aggregatorId);
        } else {
            providers = new ArrayList<>();
            providers.add(this.providerManager.getProvider(aggregatorId, providerId));
        }

        return providers;
    }

    private List<RSSModel> getModels(String aggregatorId,
            String providerId, String productClass) throws RSSException {

        return this.modelsManager.getRssModels(aggregatorId, providerId, productClass, 0, -1);
    }

    /**
     * Launch settlement process.
     *
     * @param job
     * @throws RSSException
     */
    public void runSettlement(SettlementJob job) throws RSSException {

        // Validate fields
        if (job.getAggregatorId() != null && !job.getAggregatorId().isEmpty()) {
            if (job.getProviderId() != null && !job.getProviderId().isEmpty()) {

                // Check that the given provider belongs to the aggregator
                this.modelsManager.checkValidAppProvider(job.getAggregatorId(), job.getProviderId());

                // Check that the provider has a RS model
                // for the specified product class
                if (job.getProductClass() != null && !job.getProductClass().isEmpty()
                        && !this.modelsManager.existModel(job.getAggregatorId(), job.getProviderId(), job.getProductClass())) {

                    String[] args = {job.getProductClass()};
                    throw new RSSException(UNICAExceptionType.NON_EXISTENT_RESOURCE_ID, args);
                }
            }
        }

        // Launch settlement for the given transactions
        for (Aggregator ag: this.getAggregators(job.getAggregatorId())) {
            List<RSSProvider> providers = this.getProviders(ag.getAggregatorId(), job.getProviderId());

            for(RSSProvider pv: providers) {
                List<RSSModel> models =
                        this.getModels(ag.getAggregatorId(), pv.getProviderId(), job.getProductClass());

                models.stream().forEach((m) -> {
                    // Get related transactions
                    Optional<List<DbeTransaction>> txs = this.transactionDao.
                            getTransactions(m.getAggregatorId(), m.getOwnerProviderId(), m.getProductClass());

                    // Set transactions as processing
                    if (txs.isPresent() && !txs.get().isEmpty()) {
                        this.setTxState(txs.get(), "processing", true);

                        // Create processing task
                        ProductSettlementTask settlementTask
                                = this.taskFactory.getSettlementTask(m, txs.get(), job.getCallbackUrl());

                        poolManager.submitTask(settlementTask, job.getCallbackUrl());
                    }
                });
            }
        }
        poolManager.closeTaskPool(job.getCallbackUrl());
    }

    /**
     *
     * @param transactions
     * @param state
     * @param toFlush
     */
    public void setTxState(List<DbeTransaction> transactions,
            String state, boolean toFlush) {

        transactions.stream().map((tx) -> {
            tx.setState(state);
            return tx;
        }).forEach((tx) -> {
            this.transactionDao.update(tx);
        });
        // Flush state to the database
        if (toFlush) {
            this.transactionDao.flush();
        }
    }

    public void generateReport(RSSModel sharingRes, String curr) throws IOException {
        this.logger.info("Generating report: "
                    + sharingRes.getAggregatorId() + " "
                    + sharingRes.getOwnerProviderId() + " "
                    + sharingRes.getProductClass());

        // Fill basic report information
        SharingReport report = new SharingReport();
        report.setAlgorithmType(sharingRes.getAlgorithmType());
        report.setProductClass(sharingRes.getProductClass());
        report.setDate(new Date());
        report.setAggregatorValue(sharingRes.getAggregatorValue());
        report.setOwnerValue(sharingRes.getOwnerValue());
        report.setPaid(false);

        // Get provider object
        report.setOwner(this.appProviderDao.getProvider(
                sharingRes.getAggregatorId(), sharingRes.getOwnerProviderId()));

        // Set currency
        BmCurrency currency = this.currencyDao.getByIso4217StringCode(curr);
        report.setCurrency(currency);

        // Include stakeholders info
        if (sharingRes.getStakeholders() != null) {
            Set<ReportProvider> stakeholders = new HashSet<>();

            sharingRes.getStakeholders().stream().map((stakeholderModel) -> {
                DbeAppProvider stakeholder = this.appProviderDao.
                        getProvider(sharingRes.getAggregatorId(), stakeholderModel.getStakeholderId());
                // Build stakeholder id
                ReportProviderId stModelId = new ReportProviderId();
                stModelId.setStakeholder(stakeholder);
                stModelId.setReport(report);
                // Build stakeholder
                ReportProvider stModel = new ReportProvider();
                stModel.setId(stModelId);
                stModel.setModelValue(stakeholderModel.getModelValue());
                return stModel;
            }).forEach((stModel) -> {
                // Add stakeholder to the set
                stakeholders.add(stModel);
            });

            report.setStakeholders(stakeholders);
        }

        Set<SharingReport> reports = report.getOwner().getReports();

        if (null == reports) {
            reports = new HashSet<>();
        }
        reports.add(report);

        // Save new report
        this.sharingReportDao.create(report);

        // Save stakeholders info
        report.getStakeholders().stream().forEach((st) -> {
            this.reportProviderDao.create(st);
        });
    }

    /**
     * Get the number of revenue sharing reports that fit the provided filters
     *
     * @param aggregator, aggregator of the returned sharing reports, used to filter the results
     * @param provider, provider of the returned sharing reports, used to filter the results
     * @param productClass, Product class of the returned sharing reports, used to filter the results
     * @param onlyPaid, If true, the result will only contain paid reports
     * @return Count object with the number of reports
     */
    public Count countSharingReports(
            String aggregator, String provider, String productClass, boolean onlyPaid) {

        Optional<List<SharingReport>> dbReports = this.sharingReportDao.
                getSharingReportsByParameters(aggregator, provider, productClass, onlyPaid, 0, -1);

        Count count = new Count();
        Integer resSize = dbReports.isPresent() ? dbReports.get().size() : 0;
        count.setSize(resSize);

        return  count;
    }

    /**
     * Get the generated sharing reports filtered by some parameters.
     *
     * @param aggregator, aggregator of the returned sharing reports, used to filter the results
     * @param provider, provider of the returned sharing reports, used to filter the results
     * @param productClass, Product class of the returned sharing reports, used to filter the results
     * @param onlyPaid, If true, the result will only contain paid reports
     * @param offset, First element to be returned, used for paginating the results
     * @param size, Number of elements ti be returned, used for paginating the results
     * @return List of RSSReport objects
     */
    public List<RSSReport> getSharingReports(
            String aggregator, String provider, String productClass, boolean onlyPaid, int offset, int size) {
        logger.debug("Into getSettlementFiles method.");

        // Get reports
        Optional<List<SharingReport>> dbReports = this.sharingReportDao.
                getSharingReportsByParameters(aggregator, provider, productClass, onlyPaid, offset, size);

        return dbReports.map(s ->
                s.stream().map(rp -> {
                    RSSReport rep = new RSSReport();
                    rep.setId(rp.getId());
                    rep.setAggregatorId(rp.getOwner().getId().getAggregator().getTxEmail());
                    rep.setAggregatorValue(rp.getAggregatorValue());

                    rep.setAlgorithmType(rp.getAlgorithmType());
                    rep.setCurrency(rp.getCurrency().getTxIso4217Code());
                    rep.setOwnerProviderId(rp.getOwner().getId().getTxAppProviderId());
                    rep.setOwnerValue(rp.getOwnerValue());
                    rep.setProductClass(rp.getProductClass());
                    rep.setTimestamp(rp.getDate());
                    rep.setPaid(rp.isPaid());

                    // Set stakeholders
                    List<StakeholderModel> st = rp.getStakeholders().stream()
                            .map(rPro -> {
                                StakeholderModel stakeholder = new StakeholderModel();
                                stakeholder.setStakeholderId(rPro.getStakeholder().getId().getTxAppProviderId());
                                stakeholder.setModelValue(rPro.getModelValue());
                                return stakeholder;
                            }).collect(Collectors.toList());

                    rep.setStakeholders(st);
                    return rep;
                })
                .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    public Optional<Boolean> setPayReport(int id, Boolean pay) {
        try {
            this.sharingReportDao.getById(id).setPaid(pay);
            return Optional.of(true);
        } catch (Exception e){
            return Optional.empty();
        }
    }
}
