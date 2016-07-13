package es.upm.fiware.rss.dao.impl.test;

import es.upm.fiware.rss.dao.impl.SharingReportDaoImpl;
import es.upm.fiware.rss.exception.InterfaceExceptionType;
import org.hibernate.Query;
import org.hibernate.Session;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by mgarcia on 13/07/16.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(value = SharingReportDaoImpl.class)
public class SharingReportDaoImplTest {

    private void executeTest(String aggregator, String providerId, String productClass, boolean all, int offset, int size, String expected) throws Exception {
        SharingReportDaoImpl spy = PowerMockito.spy(new SharingReportDaoImpl());

        Session s = Mockito.mock(Session.class);
        Query q = Mockito.mock(Query.class);
        Mockito.when(s.createQuery(anyString())).thenReturn(q);

        PowerMockito.doReturn(s).when(spy, "getSession");

        PowerMockito.verifyPrivate(spy, times(0)).invoke("getSession");
        spy.getSharingReportsByParameters(aggregator, providerId, productClass, all, offset, size);
        PowerMockito.verifyPrivate(spy, times(1)).invoke("getSession");

        verify(s, times(1)).createQuery(expected);

        // When offset negative, it should be called with 0
        offset = Integer.max(offset, 0);
        verify(q, times(1)).setFirstResult(offset);

        int timesToCall = (size >= 0) ? 1 : 0;
        verify(q, times(timesToCall)).setMaxResults(size);

        verify(q, times(1)).list();
    }

    @Test
    public void testNoAggNoProvNoProd() throws Exception {
        String expected = "from SharingReport l order by l.id desc";
        executeTest(null, null, null, true, 0, -1, expected);
        executeTest("", "", "", true, 0, -1, expected);
        executeTest(null, "provider", null, true, 0, -1, expected);
        executeTest(null, null, "product", true, 0, -1, expected);
        executeTest(null, "provider", "product", true, 0, -1, expected);
    }

    @Test
    public void testAggNoProvNoProd() throws Exception {
        String expected = "from SharingReport l where l.owner.id.aggregator.txEmail= 'a@b.c' order by l.id desc";
        executeTest("a@b.c", null, null, true, 0, -1, expected);
        executeTest("a@b.c", null, "product", true, 0, -1, expected);
    }

    @Test
    public void testAggProvNoProd() throws Exception {
        String expected = "from SharingReport l where l.owner.id.aggregator.txEmail= 'a@b.c' and l.owner.id.txAppProviderId= 'provider' order by l.id desc";
        executeTest("a@b.c", "provider", null, true, 0, -1, expected);
    }

    @Test
    public void testAggProvProd() throws Exception {
        String expected = "from SharingReport l where l.owner.id.aggregator.txEmail= 'a@b.c' and l.owner.id.txAppProviderId= 'provider' and l.productClass= 'product' order by l.id desc";
        executeTest("a@b.c", "provider", "product", true, 0, -1, expected);
    }

    @Test
    public void testFilterNotPaid() throws Exception {
        String expected = "from SharingReport l where (l.paid is null or l.paid = false) order by l.id desc";
        executeTest(null, null, null, false, 0, -1, expected);

        expected = "from SharingReport l where (l.paid is null or l.paid = false) and l.owner.id.aggregator.txEmail= 'a@b.c' order by l.id desc";
        executeTest("a@b.c", null, null, false, 0, -1, expected);

        expected = "from SharingReport l where (l.paid is null or l.paid = false) and l.owner.id.aggregator.txEmail= 'a@b.c' and l.owner.id.txAppProviderId= 'provider' order by l.id desc";
        executeTest("a@b.c", "provider", null, false, 0, -1, expected);

        expected = "from SharingReport l where (l.paid is null or l.paid = false) and l.owner.id.aggregator.txEmail= 'a@b.c' and l.owner.id.txAppProviderId= 'provider' and l.productClass= 'product' order by l.id desc";
        executeTest("a@b.c", "provider", "product", false, 0, -1, expected);
    }

    @Test
    public void testOffset() throws Exception {
        String expected = "from SharingReport l order by l.id desc";
        executeTest(null, null, null, true, 0, -1, expected);
        executeTest(null, null, null, true, 12, -1, expected);
        executeTest(null, null, null, true, 50, -1, expected);
        executeTest(null, null, null, true, -1, -1, expected);

        executeTest(null, null, null, true, Integer.MIN_VALUE, -1, expected);
        executeTest(null, null, null, true, Integer.MAX_VALUE, -1, expected);
    }

    @Test
    public void testSize() throws Exception {
        String expected = "from SharingReport l order by l.id desc";
        executeTest(null, null, null, true, 0, 0, expected);
        executeTest(null, null, null, true, 0, 12, expected);
        executeTest(null, null, null, true, 0, 50, expected);
        executeTest(null, null, null, true, 0, -1, expected);

        executeTest(null, null, null, true, 0, Integer.MIN_VALUE, expected);
        executeTest(null, null, null, true, 0, Integer.MAX_VALUE, expected);
    }

}
