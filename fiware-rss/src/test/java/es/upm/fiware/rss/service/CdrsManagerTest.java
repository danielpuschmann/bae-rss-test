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

package es.upm.fiware.rss.service;

import org.junit.Before;
import org.junit.Test;
import es.upm.fiware.rss.dao.CurrencyDao;
import es.upm.fiware.rss.dao.DbeAggregatorDao;
import es.upm.fiware.rss.dao.DbeAppProviderDao;
import es.upm.fiware.rss.dao.DbeTransactionDao;
import es.upm.fiware.rss.exception.RSSException;
import es.upm.fiware.rss.exception.UNICAExceptionType;
import es.upm.fiware.rss.model.BmCurrency;
import es.upm.fiware.rss.model.CDR;
import es.upm.fiware.rss.model.Count;
import es.upm.fiware.rss.model.DbeAggregator;
import es.upm.fiware.rss.model.DbeAppProvider;
import es.upm.fiware.rss.model.DbeAppProviderId;
import es.upm.fiware.rss.model.DbeTransaction;
import es.upm.fiware.rss.model.ProductClasses;
import es.upm.fiware.rss.model.RSUser;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import org.junit.Assert;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;


public class CdrsManagerTest {
    /**
     *
     */
    @Mock UserManager userManagerMock;
    @Mock DbeAggregatorDao dbeAggregatorDaoMock;
    @Mock RSSModelsManager modelsManagerMock;
    @Mock DbeAppProviderDao dbeAppProviderMock;
    @Mock CurrencyDao currencyDao;
    @Mock Properties rssProps;
    @Mock Logger logger;
    @Mock DbeTransactionDao transactionDao;
    @InjectMocks private CdrsManager cdrsManager;

    private RSUser user;
    private String aggregatorId;
    private String providerId;
    private String currency;
    private Integer correlation;

    /**
     * Method to insert data before test.
     *
     * @throws Exception
     *             from dbb
     */
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        this.user = new RSUser();
        this.user.setEmail("user@mail.com");
        
        this.aggregatorId = "mail@mail.com";
        this.providerId = "appProvider1";
        this.currency = "EUR";
        this.correlation = 10;
    }

    private CDR buildTestCDR() {
        CDR cdr = new CDR();
        cdr.setAppProvider(this.providerId);
        cdr.setApplication("application1");
        cdr.setCdrSource(this.aggregatorId);
        cdr.setChargedAmount(BigDecimal.ZERO);
        cdr.setChargedTaxAmount(BigDecimal.TEN);
        cdr.setCorrelationNumber(this.correlation);
        cdr.setCurrency(this.currency);
        cdr.setCustomerId("customerId1");
        cdr.setDescription("description1");
        cdr.setEvent("event1");
        cdr.setProductClass("productClass1");
        cdr.setReferenceCode("referenceCode1");
        cdr.setTimestamp(new Date());
        cdr.setTransactionType("C");

        return cdr;
    }

    private void mockUser() throws RSSException{
        when(userManagerMock.isAdmin()).thenReturn(true);
        when(userManagerMock.getCurrentUser()).thenReturn(this.user);
    }

    private DbeAggregator mockAggregator() {
        DbeAggregator aggregator = new DbeAggregator("name", this.aggregatorId);
        when(dbeAggregatorDaoMock.getById(this.aggregatorId))
                .thenReturn(aggregator);
        
        return aggregator;
    }

    private DbeAppProvider mockProvider() {
        DbeAppProvider dbeAppProvider = mock(DbeAppProvider.class);
        when(dbeAppProviderMock.getProvider(this.aggregatorId, this.providerId)).thenReturn(dbeAppProvider);
        when(dbeAppProvider.getTxCorrelationNumber()).thenReturn(this.correlation);

        return dbeAppProvider;
    }

    private BmCurrency mockCurrency() {
        BmCurrency currencyObj = new BmCurrency();
        when(currencyDao.getByIso4217StringCode(this.currency)).thenReturn(currencyObj);
        return currencyObj;
    }
    @Test
    public void shouldCreateATransaction() throws RSSException {

        this.mockUser();
        DbeAggregator aggregator = this.mockAggregator();
        DbeAppProvider dbeAppProvider = this.mockProvider();
        BmCurrency currencyObj = this.mockCurrency();

        Date preDate = new Date();
        when(dbeAppProvider.getTxTimeStamp()).thenReturn(preDate);

        List <CDR> cdrs = new LinkedList<>();
        CDR cdr = this.buildTestCDR();
        cdrs.add(cdr);

        cdrsManager.createCDRs(cdrs);
        ArgumentCaptor<DbeTransaction> captor = ArgumentCaptor
                .forClass(DbeTransaction.class);

        verify(this.transactionDao).create(captor.capture());
        DbeTransaction result = captor.getValue();

        Assert.assertEquals(cdr.getProductClass(), result.getTxProductClass());
        Assert.assertEquals("pending", result.getState());
        Assert.assertEquals(aggregator, result.getCdrSource());
        Assert.assertEquals(cdr.getCorrelationNumber(), result.getTxPbCorrelationId());
        Assert.assertEquals(cdr.getTimestamp(), result.getTsClientDate());
        Assert.assertEquals(cdr.getApplication(), result.getTxApplicationId());
        Assert.assertEquals(cdr.getTransactionType(), result.getTcTransactionType());
        Assert.assertEquals(cdr.getEvent(), result.getTxEvent());
        Assert.assertEquals(cdr.getReferenceCode(), result.getTxReferenceCode());
        Assert.assertEquals(cdr.getDescription(), result.getTxOperationNature());
        Assert.assertEquals(cdr.getChargedAmount(), result.getFtChargedAmount());
        Assert.assertEquals(cdr.getChargedTaxAmount(), result.getFtChargedTaxAmount());
        Assert.assertEquals(currencyObj, result.getBmCurrency());
        Assert.assertEquals(cdr.getCustomerId(), result.getTxEndUserId());
        Assert.assertEquals(dbeAppProvider, result.getAppProvider());
    }

    private void testErrorCreation(
            List <CDR> cdrs, UNICAExceptionType type, String msg) {

        try {
            cdrsManager.createCDRs(cdrs);
            Assert.fail();
        } catch (RSSException e) {
            Assert.assertEquals(type, e.getExceptionType());
            Assert.assertEquals(msg, e.getMessage());
        }
    }
    @Test
    public void throwsRSSExceptionWhenNoPermissions() throws RSSException {
        List <CDR> cdrs = new LinkedList<>();
        cdrs.add(this.buildTestCDR());

        when(userManagerMock.isAdmin()).thenReturn(false);
        this.user.setEmail("new@email.com");
        when(userManagerMock.getCurrentUser()).thenReturn(this.user);

        String msg = "Operation is not allowed: You are not allowed to register a transaction for the Store owned by " + this.aggregatorId;
        this.testErrorCreation(
                cdrs, UNICAExceptionType.NON_ALLOWED_OPERATION, msg);
    }

    @Test
    public void throwsRSSExceptionInvalidAggregator() throws RSSException {
        List <CDR> cdrs = new LinkedList<>();
        cdrs.add(this.buildTestCDR());

        this.mockUser();

        String msg = "Resource Aggregator (" + this.aggregatorId + ") does not exist";
        this.testErrorCreation(
                cdrs, UNICAExceptionType.NON_EXISTENT_RESOURCE_ID, msg);
    }

    @Test
    public void throwsRSSEceptionInvalidCurrency() throws RSSException {
        List <CDR> cdrs = new LinkedList<>();
        cdrs.add(this.buildTestCDR());

        this.mockUser();
        this.mockAggregator();
        this.mockProvider();

        String msg = "Resource currency (" + this.currency + ") does not exist";
        this.testErrorCreation(
                cdrs, UNICAExceptionType.NON_EXISTENT_RESOURCE_ID, msg);
    }

    @Test
    public void throwsRSSExceptionInvalidCorrelation() throws RSSException {
        List <CDR> cdrs = new LinkedList<>();
        cdrs.add(this.buildTestCDR());

        this.mockUser();
        this.mockAggregator();
        DbeAppProvider appProvider = this.mockProvider();
        when(appProvider.getTxCorrelationNumber()).thenReturn(11);

        this.mockCurrency();

        String msg = "Invalid parameter: Invalid correlation number, expected 11";
        this.testErrorCreation(
                cdrs, UNICAExceptionType.INVALID_PARAMETER, msg);
    }

    @Test
    public void throwsRSSExceptionInvalidTimeStamp() throws RSSException {
        List <CDR> cdrs = new LinkedList<>();
        cdrs.add(this.buildTestCDR());

        this.mockUser();
        this.mockAggregator();
        DbeAppProvider appProvider = this.mockProvider();

        Date preDate = new Date();
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(preDate);
        cal.add(Calendar.DATE, 10);
        preDate = cal.getTime();

        when(appProvider.getTxTimeStamp()).thenReturn(preDate);

        this.mockCurrency();
        String msg = "Invalid parameter: Invalid timestamp, the given time is earlier that the prevoius one";
        this.testErrorCreation(
                cdrs, UNICAExceptionType.INVALID_PARAMETER, msg);
    }

    private List <CDR> mockCDRCreationCalls() throws RSSException{
        this.mockUser();
        this.mockAggregator();
        DbeAppProvider appProvider = this.mockProvider();

        Date preDate = new Date();
        when(appProvider.getTxTimeStamp()).thenReturn(preDate);

        this.mockCurrency();

        List <CDR> cdrs = new LinkedList<>();
        cdrs.add(this.buildTestCDR());
        
        return cdrs;
    }
    
    @Test
    public void throwsRSSExceptionInvalidTxType() throws RSSException {

        List <CDR> cdrs = this.mockCDRCreationCalls();
        cdrs.get(0).setTransactionType("O");

        String msg = "Invalid parameter: The transaction type O is not supported, must be C (charge) or R (refund)";
        this.testErrorCreation(
                cdrs, UNICAExceptionType.INVALID_PARAMETER, msg);
    }

    @Test
    public void throwsRSSExceptionMissingProductClass() throws RSSException {

        List <CDR> cdrs = this.mockCDRCreationCalls();
        cdrs.get(0).setProductClass(null);

        String msg = "Missing mandatory parameter: productClass";
        this.testErrorCreation(
                cdrs, UNICAExceptionType.MISSING_MANDATORY_PARAMETER, msg);
    }

    @Test
    public void throwsRSSExceptionMissingReferenceCode() throws RSSException {

        List <CDR> cdrs = this.mockCDRCreationCalls();
        cdrs.get(0).setReferenceCode(null);

        String msg = "Missing mandatory parameter: referenceCode";
        this.testErrorCreation(
                cdrs, UNICAExceptionType.MISSING_MANDATORY_PARAMETER, msg);
    }

    @Test
    public void shouldReturnAListOfCDRsWhenExisting() throws RSSException {
        List<DbeTransaction> txResp = new ArrayList<>();
        DbeTransaction tx = new DbeTransaction();

        Date date = new Date();
        DbeAppProvider appProvider = new DbeAppProvider();
        DbeAppProviderId provId = new DbeAppProviderId();
        provId.setTxAppProviderId(this.providerId);
        appProvider.setId(provId);

        tx.setAppProvider(appProvider);

        DbeAggregator aggregator = new DbeAggregator();
        aggregator.setTxEmail(this.aggregatorId);
        tx.setCdrSource(aggregator);

        BmCurrency curr = new BmCurrency();
        curr.setTxIso4217Code(this.currency);
        tx.setBmCurrency(curr);

        tx.setTxApplicationId("application1");
        tx.setTxEndUserId("customerId1");
        tx.setTxOperationNature("description1");
        tx.setTxEvent("Transaction test");
        tx.setTxProductClass("productClass1");
        tx.setTxReferenceCode("referenceCode1");
        tx.setTsClientDate(date);
        tx.setTcTransactionType("C");
        tx.setFtChargedAmount(BigDecimal.ZERO);
        tx.setFtChargedTaxAmount(BigDecimal.TEN);
        tx.setTxPbCorrelationId(this.correlation);
        tx.setState("pending");

        txResp.add(tx);

        when(this.transactionDao
                .getPagedTransactions(aggregatorId, providerId, null, 0, -1))
                .thenReturn(Optional.of(txResp));

        List<CDR> result = cdrsManager.getCDRs(aggregatorId, providerId, 0, -1);

        Assert.assertEquals(1, result.size());
        CDR cdr = result.get(0);

        Assert.assertEquals(tx.getTxProductClass(), cdr.getProductClass());
        Assert.assertEquals(this.aggregatorId, cdr.getCdrSource());
        Assert.assertEquals(tx.getTxPbCorrelationId(), cdr.getCorrelationNumber());
        Assert.assertEquals(tx.getTsClientDate(), cdr.getTimestamp());
        Assert.assertEquals(tx.getTxApplicationId(), cdr.getApplication());
        Assert.assertEquals(tx.getTcTransactionType(), cdr.getTransactionType());
        Assert.assertEquals(tx.getTxEvent(), cdr.getEvent());
        Assert.assertEquals(tx.getTxReferenceCode(), cdr.getReferenceCode());
        Assert.assertEquals(tx.getTxOperationNature(), cdr.getDescription());
        Assert.assertEquals(tx.getFtChargedAmount(), cdr.getChargedAmount());
        Assert.assertEquals(tx.getFtChargedTaxAmount(), cdr.getChargedTaxAmount());
        Assert.assertEquals(this.currency, cdr.getCurrency());
        Assert.assertEquals(tx.getTxEndUserId(), cdr.getCustomerId());
        Assert.assertEquals(this.providerId, cdr.getAppProvider());
    }

    @Test
    public void shouldReturnEmptyListWhenNoTx() throws RSSException {
        when(this.transactionDao
                .getPagedTransactions(aggregatorId, providerId, null, 0, -1))
                .thenReturn(Optional.empty());

        List<CDR> result = cdrsManager.getCDRs(aggregatorId, providerId, 0, -1);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void shouldReturnCountWhenTxs() throws RSSException {
        List<DbeTransaction> txResp = new ArrayList<>();
        txResp.add(new DbeTransaction());
        txResp.add(new DbeTransaction());

        when(this.transactionDao
                .getTransactions(aggregatorId, providerId, null))
                .thenReturn(Optional.of(txResp));

        Count result = this.cdrsManager.countCDRs(aggregatorId, providerId);
        Assert.assertEquals(2, (long) result.getSize());
    }

    @Test
    public void shouldReturnZeroWhenNoTxs() throws RSSException {
        when(this.transactionDao
                .getTransactions(aggregatorId, providerId, null))
                .thenReturn(Optional.empty());

        Count result = this.cdrsManager.countCDRs(aggregatorId, providerId);
        Assert.assertEquals(0, (long) result.getSize());
    }

    @Test
    public void shouldReturnTheListtOfProductClasses() throws RSSException {
        List<DbeTransaction> txResp = new ArrayList<>();
        DbeTransaction tx1 = new DbeTransaction();
        tx1.setTxProductClass("productClass1");
        txResp.add(tx1);

        DbeTransaction tx2 = new DbeTransaction();
        tx2.setTxProductClass("productClass2");
        txResp.add(tx2);

        DbeTransaction tx3 = new DbeTransaction();
        tx3.setTxProductClass("productClass1");
        txResp.add(tx3);

        when(this.transactionDao
                .getTransactions(aggregatorId, providerId, null))
                .thenReturn(Optional.of(txResp));

        ProductClasses result = this.cdrsManager.getCDRClasses(aggregatorId, providerId);

        Assert.assertEquals(2, result.getProductClasses().size());
        Assert.assertTrue((result.getProductClasses().get(0).equals("productClass1")
                && result.getProductClasses().get(1).equals("productClass2")) ||
                (result.getProductClasses().get(0).equals("productClass2")
                && result.getProductClasses().get(1).equals("productClass1")));
    }

    @Test
    public void shouldReturnEmptyListProductClasses() throws RSSException {
        when(this.transactionDao
                .getTransactions(aggregatorId, providerId, null))
                .thenReturn(Optional.empty());

        ProductClasses result = this.cdrsManager.getCDRClasses(aggregatorId, providerId);
        Assert.assertTrue(result.getProductClasses().isEmpty());
    }
}
