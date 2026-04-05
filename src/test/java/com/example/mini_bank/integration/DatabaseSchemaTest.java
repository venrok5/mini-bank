package com.example.mini_bank.integration;


import java.sql.SQLException;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;


@SpringBootTest
@ActiveProfiles("test") // application-test.yml
class DatabaseSchemaTest {

	@Autowired
    private javax.sql.DataSource dataSource;
	
	@Autowired
	private EntityManagerFactory emf;
	
	@BeforeEach
	void runLiquibase() throws Exception {
	    try (var conn = dataSource.getConnection()) {
	        Database database = DatabaseFactory.getInstance()
	                                           .findCorrectDatabaseImplementation(new JdbcConnection(conn));
	        Liquibase liquibase = new Liquibase(
	            "db/changelog/changelog-master.xml", 
	            new ClassLoaderResourceAccessor(),
	            database
	        );
	        liquibase.update(new Contexts(), new LabelExpression());
	    }
	}
	
	@Test
	void validateHibernateEntities() {
	    try {
	        emf.createEntityManager(); 
	        System.out.println("Hibernate entities OK");
	    } catch (PersistenceException e) {
	        System.err.println("Schema mismatch: " + e.getMessage());
	        throw e;
	    }
	}
	
    @Test
    void checkTablesExist() throws Exception {
    	try (var conn = dataSource.getConnection()) {
    	    assertTableExists("users", conn);
    	    assertTableExists("cards", conn);
    	    assertTableExists("transactions", conn);
        } catch (Exception e) {
            System.out.println("ERROR: Table check failed: " + e.getMessage());
            
            throw e;
        }
    }
    private void assertTableExists(String tableName, java.sql.Connection conn) throws SQLException {
       
    	try (var stmt = conn.createStatement()) {
        	stmt.executeQuery("SELECT 1 FROM " + tableName + " LIMIT 1");
            System.out.println("OK: " + tableName + " exists");
            
        } catch (SQLException e) {
            System.err.println("ERROR: " + tableName + " does NOT exist");
            
            throw e;
        }
    }
}