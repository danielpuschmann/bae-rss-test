/**
 * Revenue Settlement and Sharing System GE
 * Copyright (C) 2011-2014, Javier Lucio - lucio@tid.es
 * Telefonica Investigacion y Desarrollo, S.A.
 *
 * Copyright (C) 2015 - 2016, CoNWeT Lab., Universidad Politénica de Madrid
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

import es.upm.fiware.rss.dao.DbeTransactionDao;
import es.upm.fiware.rss.dao.SharingReportDao;
import es.upm.fiware.rss.exception.RSSException;
import es.upm.fiware.rss.model.*;
import es.upm.fiware.rss.settlement.ProductSettlementTask;
import es.upm.fiware.rss.settlement.SettlementTaskFactory;
import es.upm.fiware.rss.settlement.ThreadPoolManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.mockito.Mockito.*;

public class SettlementManagerTest {

    @Mock private DbeTransactionDao transactionDao;
    @Mock private SettlementTaskFactory taskFactory;
    @Mock private AggregatorManager aggregatorManager;
    @Mock private ProviderManager providerManager;
    @Mock private RSSModelsManager modelsManager;
    @Mock private ThreadPoolManager poolManager;
    @Mock private SharingReportDao sharingReportDao;
    @InjectMocks private SettlementManager toTest;

    private String aggregatorId;
    private String providerId;
    private String productClass;
    private String callbackUrl;
    private SettlementJob job;
    private Aggregator aggregator;
    private RSSProvider rSSProvider;
    private RSSModel model;
    private List <RSSModel> models;
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        
        // Build models
        this.aggregatorId = "aggregator@mail.com";
        this.providerId = "provider@mail.com";
        this.productClass = "productClass";
        this.callbackUrl = "http://callback.com";
        
        this.job = new SettlementJob();
        
        this.job.setAggregatorId(aggregatorId);
        this.job.setProviderId(providerId);
        this.job.setProductClass(productClass);
        this.job.setCallbackUrl(callbackUrl);
        
        this.aggregator = this.buildAggregator(aggregatorId);
        this.rSSProvider = this.buildProvider(aggregatorId, providerId);
        this.model = this.buildModel(aggregatorId, providerId, productClass);
        
        this.models = new LinkedList<>();
        this.models.add(model);
    }

    private Aggregator buildAggregator (String aggregatorId) {
        Aggregator agg = new Aggregator();
        agg.setAggregatorId(aggregatorId);
        agg.setAggregatorName(aggregatorId);
        
        return agg;
    }

    private List<Aggregator> buildAggregatorList(String... aggs) {
        List<Aggregator> aggregators = new ArrayList<>();
        for (String agg: aggs) {
            aggregators.add(this.buildAggregator(agg));
        }
        return aggregators;
    }

    private RSSProvider buildProvider(String aggregatorId, String providerId) {
        RSSProvider provider = new RSSProvider();
        provider.setAggregatorId(aggregatorId);
        provider.setProviderId(providerId);
        provider.setProviderName(providerId);
        
        return provider;
    }

    private List<RSSProvider> buildProviders(String aggregatorId, String... providerIds) {
        List<RSSProvider> providers = new ArrayList<>();
        for (String prov: providerIds) {
            providers.add(this.buildProvider(aggregatorId, prov));
        }
        return providers;
    }

    private RSSModel buildModel(String aggregatorId, String providerId, String productClass) {
        RSSModel mod = new RSSModel();
        mod.setAggregatorId(aggregatorId);
        mod.setAggregatorShare(BigDecimal.ZERO);
        mod.setAlgorithmType(aggregatorId);
        mod.setOwnerProviderId(providerId);
        mod.setOwnerValue(BigDecimal.ZERO);
        mod.setProductClass(productClass);
        mod.setStakeholders(null);
        return mod;
    }

    private List<RSSModel> buildModels(String aggregatorId,
            String providerId, String... productClasses) {

        List<RSSModel> mods = new ArrayList<>();
        for (String prodClass: productClasses) {
            mods.add(this.buildModel(aggregatorId, providerId, prodClass));
        }
        return mods;
    }

    private Optional<List<DbeTransaction>> buildTransactions() {
        List <DbeTransaction> transactions = new LinkedList<>();
        DbeTransaction transaction = new DbeTransaction();
        transactions.add(transaction);

        return Optional.of(transactions);
    }

    private ProductSettlementTask buildTask(RSSModel model) {
        Optional<List<DbeTransaction>> tx1 = this.buildTransactions();
        when(transactionDao.getTransactions(model.getAggregatorId(), model.getOwnerProviderId(), model.getProductClass()))
                .thenReturn(tx1);
        
        ProductSettlementTask t1 = new ProductSettlementTask();
        when(taskFactory.getSettlementTask(model, tx1.get(), callbackUrl)).thenReturn(t1);
        
        return t1;
    }

    private void mockSingleModel() throws RSSException {
        when(modelsManager.existModel(aggregatorId, providerId, productClass)).thenReturn(Boolean.TRUE);
        when(aggregatorManager.getAggregator(aggregatorId)).thenReturn(aggregator);
        when(providerManager.getProvider(aggregatorId, providerId)).thenReturn(rSSProvider);
        when(modelsManager.getRssModels(aggregatorId, providerId, productClass, 0, -1)).thenReturn(models);
    }

    @Test
    /**
     * Validates the run settlement process for the specific transactions of a 
     * provider product class
     */
    public void testRunSettlementProviderTx() throws RSSException {
        Optional<List <DbeTransaction>> transactions = this.buildTransactions();

        // Create Mocks
        this.mockSingleModel();
        when(transactionDao.getTransactions(aggregatorId, providerId, productClass)).thenReturn(transactions);

        ProductSettlementTask settlementTask = new ProductSettlementTask();
        when(taskFactory.getSettlementTask(model, transactions.get(), callbackUrl)).thenReturn(settlementTask);

        // Execute method
        toTest.runSettlement(job);
        
        // Validate calls
        verify(modelsManager).checkValidAppProvider(aggregatorId, providerId);
        verify(poolManager).submitTask(settlementTask, callbackUrl);
        verify(poolManager).closeTaskPool(callbackUrl);
    }
    
    @Test
    /**
     * Verifies the run settlement process when no transaction available for a 
     * specific provider and product class
     */
    public void testRunSettlementProviderNoTransactions() throws RSSException {
        // Create Mocks
        this.mockSingleModel();
        when(transactionDao.getTransactions(aggregatorId, providerId, productClass)).thenReturn(Optional.empty());
        
        // Execute Mwethod
        toTest.runSettlement(job);
        
        // Validate calls
        verify(modelsManager).checkValidAppProvider(aggregatorId, providerId);
        verify(poolManager, never()).submitTask(isA(ProductSettlementTask.class), isA(String.class));
        verify(poolManager).closeTaskPool(callbackUrl);
    }
    
    /*
     * Verifies the run settlement process for all pending transactions
     */
    @Test
    public void testRunSettlementAll() throws RSSException {
        // Create Mocks
        this.job.setAggregatorId(null);
        this.job.setProviderId(null);
        this.job.setProductClass(null);
        
        // Mock aggregators
        List<Aggregator> aggregators = this.buildAggregatorList("aggregator1@email.com", "aggregator2@email.com");
        when(aggregatorManager.getAPIAggregators()).thenReturn(aggregators);
        
        // Mock providers
        List<RSSProvider> providers1 = this.buildProviders("aggregator1@email.com", "provider1", "provider2");
        List<RSSProvider> providers2 = this.buildProviders("aggregator2@email.com", "provider3", "provider4");
        
        when(providerManager.getAPIProviders("aggregator1@email.com")).thenReturn(providers1);
        when(providerManager.getAPIProviders("aggregator2@email.com")).thenReturn(providers2);

        // Mock models
        List<RSSModel> models1 = this.buildModels("aggregator1@email.com", "provider1", "class1", "class2");
        List<RSSModel> models2 = this.buildModels("aggregator1@email.com", "provider2", "class3", "class4");
        List<RSSModel> models3 = this.buildModels("aggregator2@email.com", "provider3", "class5", "class6");
        List<RSSModel> models4 = this.buildModels("aggregator2@email.com", "provider4", "class7", "class8");
        
        when(modelsManager.getRssModels("aggregator1@email.com", "provider1", null, 0, -1)).thenReturn(models1);
        when(modelsManager.getRssModels("aggregator1@email.com", "provider2", null, 0, -1)).thenReturn(models2);
        when(modelsManager.getRssModels("aggregator2@email.com", "provider3", null, 0, -1)).thenReturn(models3);
        when(modelsManager.getRssModels("aggregator2@email.com", "provider4", null, 0, -1)).thenReturn(models4);
        
        // Mock Transactions
        List<ProductSettlementTask> tasks = new ArrayList<>();
        tasks.add(this.buildTask(models1.get(0)));
        tasks.add(this.buildTask(models1.get(1)));
        tasks.add(this.buildTask(models2.get(0)));
        tasks.add(this.buildTask(models2.get(1)));
        tasks.add(this.buildTask(models3.get(0)));
        tasks.add(this.buildTask(models3.get(1)));
        tasks.add(this.buildTask(models4.get(0)));
        tasks.add(this.buildTask(models4.get(1)));
        
        // Execute Method
        toTest.runSettlement(job);
        
        // Validate calls
        tasks.stream().forEach((task) -> {
            verify(poolManager).submitTask(task, callbackUrl);
        });

        verify(poolManager).closeTaskPool(callbackUrl);
    }

    private SharingReport mockSharingReport(int id, boolean paid) {
        BmCurrency curr = new BmCurrency();
        curr.setTxIso4217Code("EUR");

        DbeAggregator aggr = new DbeAggregator("name", aggregatorId);

        DbeAppProviderId provId = new DbeAppProviderId();
        provId.setAggregator(aggr);
        provId.setTxAppProviderId(providerId);

        DbeAppProvider provider = new DbeAppProvider(provId, "name", new HashSet<>());

        SharingReport report = new SharingReport(id,
                "FIXED_PERCENTAGE",
                provider,
                new BigDecimal(0.3),
                new BigDecimal(0.7),
                new HashSet<>(),
                new BigDecimal(0.0),
                paid);
        report.setCurrency(curr);
        report.setProductClass(productClass);
        report.setDate(new Date());
        return report;
    }

    private List<SharingReport> generateReports(int paid, int notpaid) {
        if (paid < 0 || notpaid < 0) {
            return new ArrayList<>();
        }

        return IntStream.range(0, paid + notpaid).mapToObj(id -> {
            return mockSharingReport(id, id + 1 <= paid);
        }).collect(Collectors.toList());
    }

    @Test
    public void shouldReturnAnEmptyListOfReports() {
        Optional<List<SharingReport>> sreports1 = Optional.empty();
        when(sharingReportDao.getSharingReportsByParameters(aggregatorId, providerId, productClass, false, 0, -1))
                .thenReturn(sreports1);

        List<RSSReport> reports1 = toTest.getSharingReports(aggregatorId, providerId, productClass, false, 0, -1);

        Assert.assertTrue((reports1.isEmpty()));
    }

    @Test
    public void shouldReturnAListOfReports() {
        List<SharingReport> sharingReportList = generateReports(1, 1);
        Optional<List<SharingReport>> sharingReportListOpt = Optional.of(sharingReportList);
        when(sharingReportDao.getSharingReportsByParameters(aggregatorId, providerId, productClass, false, 0, -1))
                .thenReturn(sharingReportListOpt);

        List<RSSReport> rssReportsList = toTest.getSharingReports(aggregatorId, providerId, productClass, false, 0, -1);
        Assert.assertEquals(2, rssReportsList.size());

        IntStream.range(0, 2).forEach(i -> {
            RSSReport rss = rssReportsList.get(i);

            Assert.assertEquals(i, rss.getId());
            Assert.assertEquals(aggregatorId, rss.getAggregatorId());
            Assert.assertEquals(new BigDecimal(0.3), rss.getAggregatorValue());
            Assert.assertEquals(providerId, rss.getOwnerProviderId());
            Assert.assertEquals(new BigDecimal(0.7), rss.getOwnerValue());

            Assert.assertEquals("FIXED_PERCENTAGE", rss.getAlgorithmType());
            Assert.assertEquals("EUR", rss.getCurrency());

            Assert.assertEquals(productClass, rss.getProductClass());
            Assert.assertTrue(rss.getTimestamp().before(new Date()));
            Assert.assertEquals(i < 1, rss.isPaid());
        });
    }

    @Test
    public void shouldReturnAValidCount() {
        List<SharingReport> sharingReportList = this.generateReports(2, 0);
        Optional<List<SharingReport>> sharingReportListOpt = Optional.of(sharingReportList);

        when(sharingReportDao.getSharingReportsByParameters(aggregatorId, providerId, productClass, false, 0, -1))
                .thenReturn(sharingReportListOpt);

        Count result = toTest.countSharingReports(aggregatorId, providerId, productClass, false);
        Assert.assertEquals(2, (long) result.getSize());
    }

    @Test
    (expected = RSSException.class)
    public void runSettlementRSSExceptionTest() throws RSSException {
        when(modelsManager.existModel(aggregatorId, providerId, productClass)).thenReturn(false);

        toTest.runSettlement(job);
    }

    @Test
    public void setTxStateTest() {
        String state = "state";
        List <DbeTransaction> transactions = new LinkedList<>();

        DbeTransaction transaction = new DbeTransaction();
        transactions.add(transaction);

        toTest.setTxState(transactions, state, true);
    }

    @Test
    public void setTxStateNotFlushTest() {
        String state = "state";
        List <DbeTransaction> transactions = new LinkedList<>();

        DbeTransaction transaction = new DbeTransaction();
        transactions.add(transaction);

        toTest.setTxState(transactions, state, false);
    }

}
