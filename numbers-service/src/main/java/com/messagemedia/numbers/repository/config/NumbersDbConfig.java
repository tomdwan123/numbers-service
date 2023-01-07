/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.repository.config;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.beans.PropertyVetoException;
import java.util.Properties;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories("com.messagemedia.numbers.repository")
@PropertySource("classpath:db.properties")
public class NumbersDbConfig {

    @Value("${numbers.service.database.url}")
    private String jdbcUrl;

    @Value("${numbers.service.database.username}")
    private String username;

    @Value("${numbers.service.database.password}")
    private String password;

    @Value("${numbers.service.database.driver}")
    private String dbDriver;

    @Value("${numbers.service.database.connectionPool.checkoutTimeout}")
    private Integer checkoutTimeout;

    @Value("${numbers.service.database.connectionPool.acquireIncrement}")
    private Integer acquireIncrement;

    @Value("${numbers.service.database.connectionPool.maxPoolSize}")
    private Integer maxPoolSize;

    @Value("${numbers.service.database.connectionPool.minPoolSize}")
    private Integer minPoolSize;

    @Value("${numbers.service.database.connectionPool.initialPoolSize}")
    private Integer initialPoolSize;

    @Bean
    public DataSource dataSource() throws PropertyVetoException, ClassNotFoundException {
        Class.forName(dbDriver);
        ComboPooledDataSource dataSource = new ComboPooledDataSource();
        dataSource.setDriverClass(dbDriver);
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setUser(username);
        dataSource.setPassword(password);
        dataSource.setCheckoutTimeout(checkoutTimeout);
        dataSource.setAcquireIncrement(acquireIncrement);
        dataSource.setMaxPoolSize(maxPoolSize);
        dataSource.setMinPoolSize(minPoolSize);
        dataSource.setInitialPoolSize(initialPoolSize);
        dataSource.setPreferredTestQuery("SELECT (1)");
        dataSource.setTestConnectionOnCheckout(true);

        runFlyway(dataSource);
        return dataSource;
    }

    private Flyway runFlyway(DataSource dataSource) {
        Flyway flyway = new Flyway();
        flyway.setDataSource(dataSource);
        flyway.setValidateOnMigrate(true);
        flyway.setBaselineOnMigrate(true);
        flyway.migrate();
        return flyway;
    }

    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory, DataSource dataSource) {
        JpaTransactionManager txManager = new JpaTransactionManager();
        txManager.setEntityManagerFactory(entityManagerFactory);
        txManager.setDataSource(dataSource);
        return txManager;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(JpaVendorAdapter jpaVendorAdapter, DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
        entityManagerFactoryBean.setDataSource(dataSource);
        entityManagerFactoryBean.setPackagesToScan("com.messagemedia.numbers.repository.entities");
        entityManagerFactoryBean.setJpaVendorAdapter(jpaVendorAdapter);
        entityManagerFactoryBean.setJpaProperties(getHibernateProperties());
        entityManagerFactoryBean.setPersistenceUnitName("NumbersDbPersistenceUnit");

        return entityManagerFactoryBean;
    }

    private Properties getHibernateProperties() {
        Properties properties = new Properties();
        properties.put("hibernate.hbm2ddl.auto", "validate");
        properties.put("hibernate.validator.autoregister_listeners", "false");
        properties.put("hibernate.validator.apply_to_ddl", "false");
        properties.put("org.hibernate.envers.audit_table_suffix", "_HISTORY");
        properties.put("org.hibernate.envers.store_data_at_delete", "true");
        return properties;
    }

    @Bean
    public JpaVendorAdapter jpaVendorAdapter() {
        HibernateJpaVendorAdapter jpaVendorAdapter = new HibernateJpaVendorAdapter();
        jpaVendorAdapter.setDatabase(Database.POSTGRESQL);
        /**
         * PostgreSQL95Dialect supports PostgreSQL version 9.5 and later.
         * See https://docs.jboss.org/hibernate/orm/5.2/javadocs/org/hibernate/dialect/package-summary.html
         */
        jpaVendorAdapter.setDatabasePlatform("org.hibernate.dialect.PostgreSQL95Dialect");
        return jpaVendorAdapter;
    }
}

