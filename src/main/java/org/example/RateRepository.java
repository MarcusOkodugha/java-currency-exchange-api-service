package org.example;

import java.sql.*;

public class RateRepository {

    private static final String URL = "jdbc:h2:./ratesdb";

    public RateRepository() {
        try (Connection conn = DriverManager.getConnection(URL, "sa", "");
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE IF NOT EXISTS rates (date DATE, currency_pair VARCHAR(30), exchange_rate DOUBLE)");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void save(Rate rate) {
        try (Connection conn = DriverManager.getConnection(URL, "sa", "");
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO rates (date, currency_pair, exchange_rate) VALUES (?, ?, ?)")) {

            ps.setDate(1, Date.valueOf(rate.getDate()));
            ps.setString(2, rate.getCurrencyPair());
            ps.setDouble(3, rate.getExchangeRate());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteAll() {
        try (Connection conn = DriverManager.getConnection(URL, "sa", "");
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("DELETE FROM rates");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public Double findLatestRate(String currencyPair) {
        try (Connection conn = DriverManager.getConnection(URL, "sa", "");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT exchange_rate FROM rates WHERE currency_pair = ? ORDER BY date DESC LIMIT 1")) {

            ps.setString(1, currencyPair);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }

            return null;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}