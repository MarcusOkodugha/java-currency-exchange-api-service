package org.example;

import java.time.LocalDate;

public class Rate {

    private final LocalDate date;
    private final String currencyPair;
    private final double exchangeRate;

    public Rate(LocalDate date, String currencyPair, double exchangeRate) {
        this.date = date;
        this.currencyPair = currencyPair;
        this.exchangeRate = exchangeRate;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getCurrencyPair() {
        return currencyPair;
    }

    public double getExchangeRate() {
        return exchangeRate;
    }
}