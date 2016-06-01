/**
 * Copyright (C) 2015 - 2016 CoNWeT Lab., Universidad Politécnica de Madrid
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

import es.upm.fiware.rss.algorithm.AlgorithmProcessor;
import es.upm.fiware.rss.exception.RSSException;
import es.upm.fiware.rss.exception.UNICAExceptionType;
import es.upm.fiware.rss.model.RSSModel;
import es.upm.fiware.rss.model.StakeholderModel;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author fdelavega
 */
public class FixedPercentageProcessor implements AlgorithmProcessor {

    private void validatePercent(BigDecimal value) throws RSSException{
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            String[] args = {"percentage must be greater than 0"};
            throw new RSSException(UNICAExceptionType.INVALID_INPUT_VALUE, args);
        } else if (value.compareTo(BigDecimal.valueOf(100)) > 0) {
            String[] args = {"percentage must be equal or lower than 100"};
            throw new RSSException(UNICAExceptionType.INVALID_INPUT_VALUE, args);
        }
    }

    /**
     * Validates the RS Model according to the fixed percentage distribution
     * algorithm.
     * 
     * Percentages must be greather than 0 and lower than 100.
     * The total accumulated percentage must be equal to 100
     * 
     * @param model RSSModel to be validated
     * @throws RSSException if the model is not valid
     */
    @Override
    public void validateModel(RSSModel model) throws RSSException{
        BigDecimal accumulatedValue;

        // Validate values
        this.validatePercent(model.getAggregatorValue());
        accumulatedValue = model.getAggregatorValue();

        this.validatePercent(model.getOwnerValue());
        accumulatedValue = accumulatedValue.add(model.getOwnerValue());

        if (model.getStakeholders() != null) {
            for (StakeholderModel stakeholderModel: model.getStakeholders()) {
                this.validatePercent(stakeholderModel.getModelValue());
                accumulatedValue = accumulatedValue.add(stakeholderModel.getModelValue());
            }
        }

         // Check that the total percentage is equal to 100
        if (accumulatedValue.compareTo(new BigDecimal("100")) != 0) {
            String[] args = {"The fixed percentage algorithm requires percentage values in the RS model to equals 100%, Current value: " + accumulatedValue };
            throw new RSSException(UNICAExceptionType.INVALID_INPUT_VALUE, args);
        }
    }

    private BigDecimal calculatePercentage(BigDecimal value, BigDecimal perc) {
        return value.multiply(perc).divide(new BigDecimal("100"));
    }

    /**
     * Calculates the distribution of revenues according to a revenue sharing
     * model and a total amount using the fixed percentage algorithm.
     * 
     * @param model Revenue sharing model to be used
     * @param value Total amount ot be distributed
     * 
     * @return RSSModel with the total amount divided according to the RSSModel
     * povided
     * 
     * @throws RSSException 
     */
    @Override
    public RSSModel calculateRevenue(RSSModel model, BigDecimal value) throws RSSException {
        RSSModel result = new RSSModel();

        result.setProductClass(model.getProductClass());
        result.setAlgorithmType(model.getAlgorithmType());

        // Calculate aggregator value
        result.setAggregatorId(model.getAggregatorId());
        result.setAggregatorShare(this.calculatePercentage(value, model.getAggregatorValue()));

        // Calculate provider value
        result.setOwnerProviderId(model.getOwnerProviderId());
        result.setOwnerValue(this.calculatePercentage(value, model.getOwnerValue()));

        // Calculate stakeholders value
        List<StakeholderModel> stRev = new ArrayList<>();

        if (model.getStakeholders() != null) {
            model.getStakeholders().stream().map((st) -> {
                StakeholderModel m = new StakeholderModel();
                m.setStakeholderId(st.getStakeholderId());
                m.setModelValue(this.calculatePercentage(value, st.getModelValue()));
                return m;
            }).forEach((m) -> {
                stRev.add(m);
            });
        }

        result.setStakeholders(stRev);

        return result;
    }
}
