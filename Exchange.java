package com.wiko.emarket.entity;

import java.math.BigDecimal;

/**
 * @author WIKO
 * @Date: 2022/4/29 - 04 - 29 - 16:30
 * @projectName:PSI
 * @Description: com.wiko.psi.entity
 */
public class Exchange {
    private String rateDate;
    private String fromCurrency;
    private String toCurrency;
    private BigDecimal rate;

    public String getRateDate() {
        return rateDate;
    }

    public void setRateDate(String rateDate) {
        this.rateDate = rateDate;
    }

    public String getFromCurrency() {
        return fromCurrency;
    }

    public void setFromCurrency(String fromCurrency) {
        this.fromCurrency = fromCurrency;
    }

    public String getToCurrency() {
        return toCurrency;
    }

    public void setToCurrency(String toCurrency) {
        this.toCurrency = toCurrency;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    @Override
    public String toString() {
        return "Exchange{" +
                "rateDate='" + rateDate + '\'' +
                ", fromCurrency='" + fromCurrency + '\'' +
                ", toCurrency='" + toCurrency + '\'' +
                ", rate=" + rate +
                '}';
    }
}
