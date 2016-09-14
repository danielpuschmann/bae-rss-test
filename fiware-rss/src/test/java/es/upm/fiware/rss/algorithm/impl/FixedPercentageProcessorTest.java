/**
 * Copyright (C) 2016 CoNWeT Lab., Universidad Politécnica de Madrid
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
package es.upm.fiware.rss.algorithm.impl;

import es.upm.fiware.rss.algorithm.Algorithms;
import es.upm.fiware.rss.exception.RSSException;
import es.upm.fiware.rss.exception.UNICAExceptionType;
import es.upm.fiware.rss.model.RSSModel;
import es.upm.fiware.rss.model.StakeholderModel;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author francisco
 */
public class FixedPercentageProcessorTest {

    private FixedPercentageProcessor toTest;
    private RSSModel model;

    @Before
    public void setUp() {
        toTest = new FixedPercentageProcessor();
        model = new RSSModel();

        model.setAggregatorId("aggregator@email.com");
        model.setAggregatorShare(new BigDecimal("25.5"));
        model.setAlgorithmType(Algorithms.FIXED_PERCENTAGE.toString());
        model.setOwnerProviderId("provider");
        model.setOwnerValue(new BigDecimal("60.4"));
        model.setProductClass("productClass");

        List<StakeholderModel> stakeholders = new ArrayList<>();

        StakeholderModel st1 = new StakeholderModel();
        st1.setStakeholderId("provider1");
        st1.setModelValue(new BigDecimal("5.1"));

        StakeholderModel st2 = new StakeholderModel();
        st2.setStakeholderId("provider2");
        st2.setModelValue(new BigDecimal("9"));

        stakeholders.add(st1);
        stakeholders.add(st2);

        model.setStakeholders(stakeholders);
    }

    private void setNoStakeholders() {
        model.setStakeholders(null);
        model.setAggregatorShare(new BigDecimal("50"));
        model.setOwnerValue(new BigDecimal("50"));
    }

    @Test
    public void shouldValidateValidRSModel() throws RSSException {
        toTest.validateModel(model);
    }

    @Test
    public void shouldValidateValidRSModelNoStakeholders () throws RSSException {
        setNoStakeholders();

        toTest.validateModel(model);
    }

    private void testValidaateModelException(String message) {
        try {
            toTest.validateModel(model);
            Assert.fail();
        } catch (RSSException e) {
            Assert.assertEquals(
                    UNICAExceptionType.INVALID_INPUT_VALUE, e.getExceptionType());

            Assert.assertEquals(message, e.getMessage());
        }
    }

    @Test
    public void throwsRssExceptionRSModelValueLowerThanZero() {
        model.setOwnerValue(new BigDecimal("-1"));
        testValidaateModelException(
                "Invalid parameter value: percentage must be greater than 0");
    }

    @Test
    public void throwsRssExceptionRSModelValueZero() {
        model.setOwnerValue(BigDecimal.ZERO);
        testValidaateModelException(
                "Invalid parameter value: percentage must be greater than 0");
    }

    @Test
    public void throwsRssExceptionValueGreaterThanHundred() {
        model.setOwnerValue(new BigDecimal("101"));
        testValidaateModelException(
                "Invalid parameter value: percentage must be equal or lower than 100");
    }

    @Test
    public void throwsRssExceptionOveralGreaterThanHundred() {
        model.setOwnerValue(new BigDecimal("70.4"));
        testValidaateModelException(
                "Invalid parameter value: The fixed percentage algorithm requires percentage values in the RS model to equals 100%, Current value: 110.0");
    }

    @Test
    public void shouldCalculateRevenueDistrubution() throws RSSException {
        RSSModel result = toTest.calculateRevenue(model, new BigDecimal("1500.7"));

        // Validate result info
        Assert.assertEquals(model.getAggregatorId(), result.getAggregatorId());
        Assert.assertEquals(model.getAlgorithmType(), result.getAlgorithmType());
        Assert.assertEquals(model.getOwnerProviderId(), result.getOwnerProviderId());

        Assert.assertEquals(new BigDecimal("382.6785"), result.getAggregatorValue());
        Assert.assertEquals(new BigDecimal("906.4228"), result.getOwnerValue());

        // Validate stakeholders info
        Assert.assertEquals(2, model.getStakeholders().size());
        Assert.assertEquals(
                model.getStakeholders().get(0).getStakeholderId(),
                result.getStakeholders().get(0).getStakeholderId());

        Assert.assertEquals(
                new BigDecimal("76.5357"),
                result.getStakeholders().get(0).getModelValue());

        Assert.assertEquals(
                model.getStakeholders().get(1).getStakeholderId(),
                result.getStakeholders().get(1).getStakeholderId());

        Assert.assertEquals(
                new BigDecimal("135.063"),
                result.getStakeholders().get(1).getModelValue());
    }

    @Test
    public void shouldCalculateRevenueDistributionNoStakeholders()
            throws RSSException {

        setNoStakeholders();
        RSSModel result = toTest.calculateRevenue(model, new BigDecimal("1000"));

        Assert.assertEquals(new BigDecimal("500"), result.getAggregatorValue());
        Assert.assertEquals(new BigDecimal("500"), result.getOwnerValue());

        Assert.assertEquals(0, result.getStakeholders().size());
    }
}
