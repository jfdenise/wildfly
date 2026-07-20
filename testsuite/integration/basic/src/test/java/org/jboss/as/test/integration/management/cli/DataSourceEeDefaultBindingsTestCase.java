/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.management.cli;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.as.test.integration.management.util.CLIOpResult;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Integration tests for WFLY-21956: CLI datasource commands that manage
 * {@code /subsystem=ee/service=default-bindings:datasource} atomically
 * alongside add/remove operations.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>{@code data-source add --set-default-datasource} sets the EE default binding.</li>
 *   <li>{@code --set-default-datasource} without {@code --jndi-name} fails before sending any op.</li>
 *   <li>{@code data-source remove --unset-if-default-datasource} clears the binding when DS is the default.</li>
 *   <li>{@code data-source remove --unset-if-default-datasource} is a no-op when DS is not the EE default.</li>
 *   <li>{@code data-source remove --unset-if-default-datasource} succeeds when no EE default is configured.</li>
 *   <li>XA variants of the add and remove cases above.</li>
 * </ul>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DataSourceEeDefaultBindingsTestCase extends AbstractCliTestBase {

    // -----------------------------------------------------------------------
    // Constants used across tests
    // -----------------------------------------------------------------------

    /** Non-XA datasource name */
    private static final String DS_NAME   = "TestEeDefaultDS";
    /** XA datasource name */
    private static final String XA_DS_NAME = "TestEeDefaultXADS";
    /** JNDI name for the non-XA datasource */
    private static final String DS_JNDI   = "java:jboss/datasources/TestEeDefaultDS";
    /** JNDI name for the XA datasource */
    private static final String XA_DS_JNDI = "java:jboss/datasources/TestEeDefaultXADS";
    /** JNDI name of a second datasource used in the "not the default" test */
    private static final String OTHER_DS_NAME = "TestEeOtherDS";
    private static final String OTHER_DS_JNDI = "java:jboss/datasources/TestEeOtherDS";

    /** Management address of the EE default-bindings resource */
    private static final String EE_DEFAULT_BINDINGS_ADDR =
            "/subsystem=ee/service=default-bindings";

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    @BeforeClass
    public static void beforeClass() throws Exception {
        AbstractCliTestBase.initCLI();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        AbstractCliTestBase.closeCLI();
    }

    /**
     * Best-effort cleanup after each test so failures do not cascade.
     * Errors are swallowed intentionally.
     */
    @After
    public void cleanup() throws Exception {
        // Clear EE default datasource binding
        cli.sendLine(EE_DEFAULT_BINDINGS_ADDR + ":undefine-attribute(name=datasource)", true);
        // Remove datasources that may have been left behind
        removeIfExists("data-source", DS_NAME);
        removeIfExists("xa-data-source", XA_DS_NAME);
        removeIfExists("data-source", OTHER_DS_NAME);
    }

    // -----------------------------------------------------------------------
    // Non-XA datasource — add with --set-default-datasource
    // -----------------------------------------------------------------------

    /**
     * {@code data-source add --set-default-datasource} must create the datasource
     * and atomically set {@code /subsystem=ee/service=default-bindings:datasource}
     * to the datasource's JNDI name.
     */
    @Test
    public void testAddDataSourceSetsEeDefault() throws Exception {
        cli.sendLine(
                "data-source add" +
                " --name=" + DS_NAME +
                " --jndi-name=" + DS_JNDI +
                " --driver-name=h2" +
                " --connection-url=jdbc:h2:mem:testeedefault;DB_CLOSE_DELAY=-1" +
                " --set-default-datasource");

        // Datasource must be present
        assertDatasourceExists("data-source", DS_NAME);

        // EE default binding must now point to this datasource's JNDI name
        Assert.assertEquals(DS_JNDI, readEeDefaultDatasource());
    }

    /**
     * {@code --set-default-datasource} without {@code --jndi-name} must fail
     * with an error before any management operation is executed. The
     * datasource must not exist afterwards.
     */
    @Test
    public void testAddDataSourceMissingJndiNameFails() throws Exception {
        // Deliberately omit --jndi-name
        cli.sendLine(
                "data-source add" +
                " --name=" + DS_NAME +
                " --driver-name=h2" +
                " --connection-url=jdbc:h2:mem:testeedefault;DB_CLOSE_DELAY=-1" +
                " --set-default-datasource",
                true /* ignore outcome */);

        // The output should contain a non-success indication
        final String output = cli.readOutput();
        Assert.assertNotNull("Expected error output but got null", output);
        // The CLI must have printed an error (jndi-name is required)
        Assert.assertFalse("Expected failure output to be non-empty", output.trim().isEmpty());

        // The EE default binding must remain untouched (null / undefined)
        Assert.assertNull("EE default datasource must not have been set", readEeDefaultDatasource());
    }

    // -----------------------------------------------------------------------
    // Non-XA datasource — remove with --unset-if-default-datasource
    // -----------------------------------------------------------------------

    /**
     * When the datasource being removed is currently the EE default, the
     * composite operation must undefine the binding and then remove the DS
     * atomically.
     */
    @Test
    public void testRemoveDataSourceUnsetsEeDefault() throws Exception {
        // Pre-condition: DS exists and is the EE default
        addDataSource(DS_NAME, DS_JNDI);
        setEeDefaultDatasource(DS_JNDI);

        cli.sendLine(
                "data-source remove" +
                " --name=" + DS_NAME +
                " --unset-if-default-datasource");

        // Datasource must be gone
        assertDatasourceAbsent("data-source", DS_NAME);

        // EE default binding must have been cleared
        Assert.assertNull("EE default datasource must have been unset", readEeDefaultDatasource());
    }

    /**
     * When a different datasource is the EE default, removing a datasource
     * with {@code --unset-if-default-datasource} must not change the EE binding.
     */
    @Test
    public void testRemoveDataSourceNotDefaultIsNoOp() throws Exception {
        // Pre-condition: another DS is the EE default; the DS to remove is NOT the default
        addDataSource(OTHER_DS_NAME, OTHER_DS_JNDI);
        addDataSource(DS_NAME, DS_JNDI);
        setEeDefaultDatasource(OTHER_DS_JNDI);

        cli.sendLine(
                "data-source remove" +
                " --name=" + DS_NAME +
                " --unset-if-default-datasource");

        // DS must be gone
        assertDatasourceAbsent("data-source", DS_NAME);

        // EE default must still point to the other datasource
        Assert.assertEquals(
                "EE default datasource must remain unchanged",
                OTHER_DS_JNDI,
                readEeDefaultDatasource());
    }

    /**
     * When no EE default is configured at all, remove with
     * {@code --unset-if-default-datasource} must succeed without error.
     */
    @Test
    public void testRemoveDataSourceNoDefaultConfiguredIsNoOp() throws Exception {
        // Pre-condition: DS exists, EE default is undefined
        addDataSource(DS_NAME, DS_JNDI);
        // Ensure no default is set
        cli.sendLine(EE_DEFAULT_BINDINGS_ADDR + ":undefine-attribute(name=datasource)", true);

        cli.sendLine(
                "data-source remove" +
                " --name=" + DS_NAME +
                " --unset-if-default-datasource");

        // DS must be gone
        assertDatasourceAbsent("data-source", DS_NAME);

        // EE default must still be undefined
        Assert.assertNull("EE default datasource must remain undefined", readEeDefaultDatasource());
    }

    // -----------------------------------------------------------------------
    // XA datasource — add with --set-default-datasource
    // -----------------------------------------------------------------------

    /**
     * {@code xa-data-source add --set-default-datasource} must create the XA
     * datasource and atomically set the EE default binding.
     */
    @Test
    public void testAddXaDataSourceSetsEeDefault() throws Exception {
        cli.sendLine(
                "xa-data-source add" +
                " --name=" + XA_DS_NAME +
                " --jndi-name=" + XA_DS_JNDI +
                " --driver-name=h2" +
                " --xa-datasource-properties={\"url\"=>\"jdbc:h2:mem:testxaeedefault\"}" +
                " --set-default-datasource");

        // XA datasource must be present
        assertDatasourceExists("xa-data-source", XA_DS_NAME);

        // EE default binding must now point to the XA datasource's JNDI name
        Assert.assertEquals(XA_DS_JNDI, readEeDefaultDatasource());
    }

    // -----------------------------------------------------------------------
    // XA datasource — remove with --unset-if-default-datasource
    // -----------------------------------------------------------------------

    /**
     * When the XA datasource being removed is the EE default, the composite
     * operation must clear the binding atomically.
     */
    @Test
    public void testRemoveXaDataSourceUnsetsEeDefault() throws Exception {
        // Pre-condition: XA DS exists and is the EE default
        addXaDataSource(XA_DS_NAME, XA_DS_JNDI);
        setEeDefaultDatasource(XA_DS_JNDI);

        cli.sendLine(
                "xa-data-source remove" +
                " --name=" + XA_DS_NAME +
                " --unset-if-default-datasource");

        // XA datasource must be gone
        assertDatasourceAbsent("xa-data-source", XA_DS_NAME);

        // EE default binding must have been cleared
        Assert.assertNull("EE default datasource must have been unset after XA DS removal",
                readEeDefaultDatasource());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Adds a plain H2 datasource with the given name and JNDI name. */
    private void addDataSource(String name, String jndiName) throws Exception {
        cli.sendLine(
                "data-source add" +
                " --name=" + name +
                " --jndi-name=" + jndiName +
                " --driver-name=h2" +
                " --connection-url=jdbc:h2:mem:" + name.toLowerCase() + ";DB_CLOSE_DELAY=-1");
    }

    /** Adds an H2 XA datasource with the given name and JNDI name. */
    private void addXaDataSource(String name, String jndiName) throws Exception {
        cli.sendLine(
                "xa-data-source add" +
                " --name=" + name +
                " --jndi-name=" + jndiName +
                " --driver-name=h2" +
                " --xa-datasource-properties={\"url\"=>\"jdbc:h2:mem:" + name.toLowerCase() + "\"}");
    }

    /**
     * Writes the {@code datasource} attribute on
     * {@code /subsystem=ee/service=default-bindings} via the management API.
     */
    private void setEeDefaultDatasource(String jndiName) throws Exception {
        cli.sendLine(EE_DEFAULT_BINDINGS_ADDR +
                ":write-attribute(name=datasource,value=" + jndiName + ")");
        CLIOpResult result = cli.readAllAsOpResult();
        Assert.assertTrue("Setting EE default datasource must succeed", result.isIsOutcomeSuccess());
    }

    /**
     * Reads and returns the current value of
     * {@code /subsystem=ee/service=default-bindings:datasource}, or
     * {@code null} if the attribute is undefined or the resource does not exist.
     */
    private String readEeDefaultDatasource() throws Exception {
        cli.sendLine(EE_DEFAULT_BINDINGS_ADDR + ":read-attribute(name=datasource)");
        CLIOpResult result = cli.readAllAsOpResult();
        if (!result.isIsOutcomeSuccess()) {
            return null;
        }
        final Object value = result.getResult();
        if (value == null || "undefined".equals(value.toString())) {
            return null;
        }
        return value.toString();
    }

    /**
     * Asserts that the named datasource resource exists in the management model.
     *
     * @param type either {@code "data-source"} or {@code "xa-data-source"}
     * @param name datasource name
     */
    private void assertDatasourceExists(String type, String name) throws Exception {
        cli.sendLine("/subsystem=datasources/" + type + "=" + name + ":read-resource()");
        CLIOpResult result = cli.readAllAsOpResult();
        Assert.assertTrue(
                type + "=" + name + " must exist in the model",
                result.isIsOutcomeSuccess());
    }

    /**
     * Asserts that the named datasource resource does NOT exist in the management model.
     *
     * @param type either {@code "data-source"} or {@code "xa-data-source"}
     * @param name datasource name
     */
    private void assertDatasourceAbsent(String type, String name) throws Exception {
        cli.sendLine("/subsystem=datasources/" + type + "=" + name + ":read-resource()", true);
        CLIOpResult result = cli.readAllAsOpResult();
        Assert.assertFalse(
                type + "=" + name + " must not exist in the model after removal",
                result.isIsOutcomeSuccess());
    }

    /**
     * Removes the named datasource (any type) if it is present; swallows errors.
     *
     * @param type either {@code "data-source"} or {@code "xa-data-source"}
     * @param name datasource name
     */
    private void removeIfExists(String type, String name) throws Exception {
        cli.sendLine("/subsystem=datasources/" + type + "=" + name + ":read-resource()", true);
        CLIOpResult check = cli.readAllAsOpResult();
        if (check.isIsOutcomeSuccess()) {
            cli.sendLine(type + " remove --name=" + name, true);
        }
    }
}
