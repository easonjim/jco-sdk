import java.util.concurrent.CountDownLatch;

import com.sap.conn.jco.AbapException;
import com.sap.conn.jco.JCoContext;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoField;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoFunctionTemplate;
import com.sap.conn.jco.JCoStructure;
import com.sap.conn.jco.JCoTable;

/**
 * basic examples for Java to ABAP communication  
 */
public class StepByStepClient
{
    static String ABAP_AS = "ABAP_AS_WITHOUT_POOL";
    static String ABAP_AS_POOLED = "ABAP_AS_WITH_POOL";
    static String ABAP_MS = "ABAP_MS_WITHOUT_POOL";

    /**
     * This example demonstrates the destination concept introduced with JCO 3.
     * The application does not deal with single connections anymore. Instead
     * it works with logical destinations like ABAP_AS and ABAP_MS which separates
     * the application logic from technical configuration.     
     * @throws JCoException
     */
    public static void step1Connect() throws JCoException
    {
        JCoDestination destination = JCoDestinationManager.getDestination(ABAP_AS);
        System.out.println("Attributes:");
        System.out.println(destination.getAttributes());
        System.out.println();

        destination = JCoDestinationManager.getDestination(ABAP_MS);
        System.out.println("Attributes:");
        System.out.println(destination.getAttributes());
        System.out.println();
    }
    
    /**
     * This example uses a connection pool. However, the implementation of
     * the application logic is still the same. Creation of pools and pool management
     * are handled by the JCo runtime. 
     * 
     * @throws JCoException
     */
    public static void step2ConnectUsingPool() throws JCoException
    {
        JCoDestination destination = JCoDestinationManager.getDestination(ABAP_AS_POOLED);
        destination.ping();
        System.out.println("Attributes:");
        System.out.println(destination.getAttributes());
        System.out.println();
    }
    
    /**
     * The following example executes a simple RFC function STFC_CONNECTION.
     * In contrast to JCo 2 you do not need to take care of repository management. 
     * JCo 3 manages the repository caches internally and shares the available
     * function metadata as much as possible. 
     * @throws JCoException
     */
    public static void step3SimpleCall() throws JCoException
    {
        //JCoDestination is the logic address of an ABAP system and ...
        JCoDestination destination = JCoDestinationManager.getDestination(ABAP_AS_POOLED);
        // ... it always has a reference to a metadata repository
        JCoFunction function = destination.getRepository().getFunction("STFC_CONNECTION");
        if(function == null)
            throw new RuntimeException("BAPI_COMPANYCODE_GETLIST not found in SAP.");

        //JCoFunction is container for function values. Each function contains separate
        //containers for import, export, changing and table parameters.
        //To set or get the parameters use the APIS setValue() and getXXX(). 
        function.getImportParameterList().setValue("REQUTEXT", "Hello SAP");
        
        try
        {
            //execute, i.e. send the function to the ABAP system addressed 
            //by the specified destination, which then returns the function result.
            //All necessary conversions between Java and ABAP data types
            //are done automatically.
            function.execute(destination);
        }
        catch(AbapException e)
        {
            System.out.println(e.toString());
            return;
        }
        
        System.out.println("STFC_CONNECTION finished:");
        System.out.println(" Echo: " + function.getExportParameterList().getString("ECHOTEXT"));
        System.out.println(" Response: " + function.getExportParameterList().getString("RESPTEXT"));
        System.out.println();
    }
    
    /**
     * ABAP APIs often uses complex parameters. This example demonstrates
     * how to read the values from a structure.  
     * @throws JCoException
     */
    public static void step3WorkWithStructure() throws JCoException
    {
        JCoDestination destination = JCoDestinationManager.getDestination(ABAP_AS_POOLED);
        JCoFunction function = destination.getRepository().getFunction("RFC_SYSTEM_INFO");
        if(function == null)
            throw new RuntimeException("RFC_SYSTEM_INFO not found in SAP.");

        try
        {
            function.execute(destination);
        }
        catch(AbapException e)
        {
            System.out.println(e.toString());
            return;
        }
        
        JCoStructure exportStructure = function.getExportParameterList().getStructure("RFCSI_EXPORT");
        System.out.println("System info for " + destination.getAttributes().getSystemID() + ":\n");

        //The structure contains some fields. The loop just prints out each field with its name.
        for(int i = 0; i < exportStructure.getMetaData().getFieldCount(); i++) 
        {
            System.out.println(exportStructure.getMetaData().getName(i) + ":\t" + exportStructure.getString(i));
        }
        System.out.println();
        
        //JCo still supports the JCoFields, but direct access via getXXX is more efficient as field iterator
        System.out.println("The same using field iterator: \nSystem info for " + destination.getAttributes().getSystemID() + ":\n");
        for(JCoField field : exportStructure)
        {
            System.out.println(field.getName() + ":\t" + field.getString());
        }
        System.out.println();
    }

    /**
     * A slightly more complex example than before. Query the companies list
     * returned in a table and then obtain more details for each company. 
     * @throws JCoException
     */
    public static void step4WorkWithTable() throws JCoException
    {
        JCoDestination destination = JCoDestinationManager.getDestination(ABAP_AS_POOLED);
        JCoFunction function = destination.getRepository().getFunction("BAPI_COMPANYCODE_GETLIST");
        if(function == null)
            throw new RuntimeException("BAPI_COMPANYCODE_GETLIST not found in SAP.");

        try
        {
            function.execute(destination);
        }
        catch(AbapException e)
        {
            System.out.println(e.toString());
            return;
        }
        
        JCoStructure returnStructure = function.getExportParameterList().getStructure("RETURN");
        if (! (returnStructure.getString("TYPE").equals("")||returnStructure.getString("TYPE").equals("S"))  )   
        {
           throw new RuntimeException(returnStructure.getString("MESSAGE"));
        }
        
        JCoTable codes = function.getTableParameterList().getTable("COMPANYCODE_LIST");
        for (int i = 0; i < codes.getNumRows(); i++) 
        {
            codes.setRow(i);
            System.out.println(codes.getString("COMP_CODE") + '\t' + codes.getString("COMP_NAME"));
        }

        //move the table cursor to first row
        codes.firstRow();
        for (int i = 0; i < codes.getNumRows(); i++, codes.nextRow()) 
        {
            function = destination.getRepository().getFunction("BAPI_COMPANYCODE_GETDETAIL");
            if (function == null) 
                throw new RuntimeException("BAPI_COMPANYCODE_GETDETAIL not found in SAP.");

            function.getImportParameterList().setValue("COMPANYCODEID", codes.getString("COMP_CODE"));
            
            //We do not need the addresses, so set the corresponding parameter to inactive.
            //Inactive parameters will be  either not generated or at least converted.  
            function.getExportParameterList().setActive("COMPANYCODE_ADDRESS",false);
            
            try
            {
                function.execute(destination);
            }
            catch (AbapException e)
            {
                System.out.println(e.toString());
                return;
            }

            returnStructure = function.getExportParameterList().getStructure("RETURN");
            if (! (returnStructure.getString("TYPE").equals("") ||
                   returnStructure.getString("TYPE").equals("S") ||
                   returnStructure.getString("TYPE").equals("W")) ) 
            {
                throw new RuntimeException(returnStructure.getString("MESSAGE"));
            }
            
            JCoStructure detail = function.getExportParameterList().getStructure("COMPANYCODE_DETAIL");
            
            System.out.println(detail.getString("COMP_CODE") + '\t' +
                               detail.getString("COUNTRY") + '\t' +
                               detail.getString("CITY"));
        }//for
    }
    
    /**
     * this example shows the "simple" stateful call sequence. Since all calls belonging to one
     * session are executed within the same thread, the application does not need
     * to take into account the SessionReferenceProvider. MultithreadedExample.java 
     * illustrates the more complex scenario, where the calls belonging to one session are 
     * executed in different threads.
     * 
     * Note: this example uses Z_GET_COUNTER and Z_INCREMENT_COUNTER. Most ABAP systems
     * contain function modules GET_COUNTER and INCREMENT_COUNTER that are not remote-enabled.
     * Copy these functions to Z_GET_COUNTER and Z_INCREMENT_COUNTER (or implement as wrapper)
     * and declare them to be remote enabled.
     * @throws JCoException
     */
    public static void step4SimpleStatefulCalls() throws JCoException
    {
        final JCoFunctionTemplate incrementCounterTemplate, getCounterTemplate;

        JCoDestination destination = JCoDestinationManager.getDestination(ABAP_MS);
        incrementCounterTemplate = destination.getRepository().getFunctionTemplate("Z_INCREMENT_COUNTER");
        getCounterTemplate = destination.getRepository().getFunctionTemplate("Z_GET_COUNTER");
        if(incrementCounterTemplate == null || getCounterTemplate == null)
            throw new RuntimeException("This example cannot run without Z_INCREMENT_COUNTER and Z_GET_COUNTER functions");
        
        final int threadCount = 5;
        final int loops = 5;
        final CountDownLatch startSignal = new CountDownLatch(threadCount);
        final CountDownLatch doneSignal = new CountDownLatch(threadCount);
        
        Runnable worker = new Runnable()
        {
            public void run()
            {
                startSignal.countDown();
                try
                {
                    //wait for other threads
                    startSignal.await();

                    JCoDestination dest = JCoDestinationManager.getDestination(ABAP_MS);
                    JCoContext.begin(dest);
                    try
                    {
                        for(int i=0; i < loops; i++)
                        {
                            JCoFunction incrementCounter = incrementCounterTemplate.getFunction();
                            incrementCounter.execute(dest);
                        }
                        JCoFunction getCounter = getCounterTemplate.getFunction();
                        getCounter.execute(dest);
                        
                        int remoteCounter = getCounter.getExportParameterList().getInt("GET_VALUE");
                        System.out.println("Thread-" + Thread.currentThread().getId() + 
                                " finished. Remote counter has " + (loops==remoteCounter?"correct":"wrong") + 
                                " value [" + remoteCounter + "]");
                    }
                    finally
                    {
                        JCoContext.end(dest);                    
                    }
                }
                catch(Exception e)
                {
                    System.out.println("Thread-" + Thread.currentThread().getId() + " ends with exception " + e.toString());
                }
                
                doneSignal.countDown();
            }
        };
        
        for(int i = 0; i < threadCount; i++)
        {
            new Thread(worker).start();
        }
        
        try
        {
            doneSignal.await();
        }
        catch(Exception e)
        {
        }
        
    }
    
    public static void main(String[] args) throws JCoException
    {
        step1Connect();
        step2ConnectUsingPool();
        step3SimpleCall();
        step4WorkWithTable();
        step4SimpleStatefulCalls();
    }
}
