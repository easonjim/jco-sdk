import java.util.Hashtable;
import java.util.Map;

import com.sap.conn.jco.JCo;
import com.sap.conn.jco.JCoCustomRepository;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoFunctionTemplate;
import com.sap.conn.jco.JCoListMetaData;
import com.sap.conn.jco.JCoMetaData;
import com.sap.conn.jco.server.DefaultServerHandlerFactory;
import com.sap.conn.jco.server.JCoServer;
import com.sap.conn.jco.server.JCoServerContext;
import com.sap.conn.jco.server.JCoServerContextInfo;
import com.sap.conn.jco.server.JCoServerErrorListener;
import com.sap.conn.jco.server.JCoServerExceptionListener;
import com.sap.conn.jco.server.JCoServerFactory;
import com.sap.conn.jco.server.JCoServerFunctionHandler;
import com.sap.conn.jco.server.JCoServerState;
import com.sap.conn.jco.server.JCoServerStateChangedListener;
import com.sap.conn.jco.server.JCoServerTIDHandler;

public class StepByStepServer
{
    static String SERVER_NAME1 = "SERVER";
    static String DESTINATION_NAME1 = "ABAP_AS_WITHOUT_POOL";
    static String DESTINATION_NAME2 = "ABAP_AS_WITH_POOL";
    static MyTIDHandler myTIDHandler = null;

    /**
     * This class provides the implementation for the function STFC_CONNECTION. You will
     * find the RFC-enabled function STFC_CONNECTION in almost any ABAP system. The
     * function is pretty simple - it has 1 input parameter and 2 output parameter. The content
     * of the input parameter REQUTEXT is copied to the output parameter ECHOTEXT. The 
     * output parameter RESPTEXT is set to "Hello World". 
     */
    static class StfcConnectionHandler implements JCoServerFunctionHandler
    {
        public void handleRequest(JCoServerContext serverCtx, JCoFunction function)
        {
            System.out.println("----------------------------------------------------------------");
            System.out.println("call              : " + function.getName());
            System.out.println("ConnectionId      : " + serverCtx.getConnectionID());
            System.out.println("SessionId         : " + serverCtx.getSessionID());
            System.out.println("TID               : " + serverCtx.getTID());
            System.out.println("repository name   : " + serverCtx.getRepository().getName());
            System.out.println("is in transaction : " + serverCtx.isInTransaction());
            System.out.println("is stateful       : " + serverCtx.isStatefulSession());
            System.out.println("----------------------------------------------------------------");
            System.out.println("gwhost: " + serverCtx.getServer().getGatewayHost());
            System.out.println("gwserv: " + serverCtx.getServer().getGatewayService());
            System.out.println("progid: " + serverCtx.getServer().getProgramID());
            System.out.println("----------------------------------------------------------------");
            System.out.println("attributes  : ");
            System.out.println(serverCtx.getConnectionAttributes().toString());
            System.out.println("----------------------------------------------------------------");
            System.out.println("CPIC conversation ID: " + serverCtx.getConnectionAttributes().getCPICConversationID());
            System.out.println("----------------------------------------------------------------");
            System.out.println("req text: " + function.getImportParameterList().getString("REQUTEXT"));
            function.getExportParameterList().setValue("ECHOTEXT", function.getImportParameterList().getString("REQUTEXT"));
            function.getExportParameterList().setValue("RESPTEXT", "Hello World");
            
            // In sample 3 (tRFC Server) we also set the status to executed:
            if(myTIDHandler != null)
                myTIDHandler.execute(serverCtx);
        }
    }
    
    /**
     * First server example. At first we get an instance of the JCoServer through JCoServerFactory. The requested instance
     * will be created, or an existing one will be returned if the instance was created before. It is not possible to
     * run more then one instance with a particular configuration. Then we register the implementation for the
     * function STFC_CONNECTION provided by class StfcConnectionHandler through FunctionHandlerFactory 
     * provided by JCo. You are free to write your own implementation JCoServerFunctionHandlerFactory, if you need more
     * than simple mapping between function name and java class implementing the function. 
     * Now we can start the server instance. After a while the JCo runtime opens the server connections. You may
     * check the server connections via sm59 or invoke STFC_CONNECTION via se37. 
     */
    static void step1SimpleServer()
    {
        JCoServer server;
        try
        {
            server = JCoServerFactory.getServer(SERVER_NAME1);
        }
        catch(JCoException ex)
        {
            throw new RuntimeException("Unable to create the server " + SERVER_NAME1 + " because of " + ex.getMessage(), ex);
        }
        
        JCoServerFunctionHandler stfcConnectionHandler = new StfcConnectionHandler();
        DefaultServerHandlerFactory.FunctionHandlerFactory factory = new DefaultServerHandlerFactory.FunctionHandlerFactory();
        factory.registerHandler("STFC_CONNECTION", stfcConnectionHandler);
        server.setCallHandlerFactory(factory);
        
        server.start();
        System.out.println("The program can be stoped using <ctrl>+<c>");
    }
    
    static class MyThrowableListener implements JCoServerErrorListener, JCoServerExceptionListener
    {
        
        public void serverErrorOccurred(JCoServer jcoServer, String connectionId, JCoServerContextInfo serverCtx, Error error)
        {
            System.out.println(">>> Error occured on " + jcoServer.getProgramID() + " connection " + connectionId);
            error.printStackTrace();
        }
        
        public void serverExceptionOccurred(JCoServer jcoServer, String connectionId, JCoServerContextInfo serverCtx, Exception error)
        {
            System.out.println(">>> Error occured on " + jcoServer.getProgramID() + " connection " + connectionId);
            error.printStackTrace();
        }
    }
    
    static class MyStateChangedListener implements JCoServerStateChangedListener
    {
        public void serverStateChangeOccurred(JCoServer server, JCoServerState oldState, JCoServerState newState)
        {
            
            // Defined states are: STARTED, DEAD, ALIVE, STOPPED;
            // see JCoServerState class for details.
            // Details for connections managed by a server instance
            // are available via JCoServerMonitor.
            System.out.println("Server state changed from " + oldState.toString() + " to " + newState.toString() + " on server with program id "
                    + server.getProgramID());
        }
    }
    
    static void step2SimpleServer()
    {
        JCoServer server;
        try
        {
            server = JCoServerFactory.getServer(SERVER_NAME1);
        }
        catch(JCoException ex)
        {
            throw new RuntimeException("Unable to create the server " + SERVER_NAME1 + " because of " + ex.getMessage(), ex);
        }
        
        JCoServerFunctionHandler stfcConnectionHandler = new StfcConnectionHandler();
        DefaultServerHandlerFactory.FunctionHandlerFactory factory = new DefaultServerHandlerFactory.FunctionHandlerFactory();
        factory.registerHandler("STFC_CONNECTION", stfcConnectionHandler);
        server.setCallHandlerFactory(factory);
        
        // additionally to step 1
        MyThrowableListener eListener = new MyThrowableListener();
        server.addServerErrorListener(eListener);
        server.addServerExceptionListener(eListener);
        
        MyStateChangedListener slistener = new MyStateChangedListener();
        server.addServerStateChangedListener(slistener);
        
        server.start();
        System.out.println("The program can be stoped using <ctrl>+<c>");
    }
    
    static class MyTIDHandler implements JCoServerTIDHandler
    {
        
        Map<String, TIDState> availableTIDs = new Hashtable<String, TIDState>();
        
        public boolean checkTID(JCoServerContext serverCtx, String tid)
        {
            // This example uses a Hashtable to store status information. Normally, however,
            // you would use a database. If the DB is down throw a RuntimeException at
            // this point. JCo will then abort the tRFC and the R/3 backend will try
            // again later.
            
            System.out.println("TID Handler: checkTID for " + tid);
            TIDState state = availableTIDs.get(tid);
            if(state == null)
            {
                availableTIDs.put(tid, TIDState.CREATED);
                return true;
            }
            
            if(state == TIDState.CREATED || state == TIDState.ROLLED_BACK)
                return true;

            return false;
            // "true" means that JCo will now execute the transaction, "false" means
            // that we have already executed this transaction previously, so JCo will
            // skip the handleRequest() step and will immediately return an OK code to R/3.
        }
        
        public void commit(JCoServerContext serverCtx, String tid)
        {
            System.out.println("TID Handler: commit for " + tid);
            
            // react on commit, e.g. commit on the database;
            // if necessary throw a RuntimeException, if the commit was not possible
            availableTIDs.put(tid, TIDState.COMMITTED);
        }
        
        public void rollback(JCoServerContext serverCtx, String tid)
        {
            System.out.println("TID Handler: rollback for " + tid);
            availableTIDs.put(tid, TIDState.ROLLED_BACK);
            
            // react on rollback, e.g. rollback on the database
        }
        
        public void confirmTID(JCoServerContext serverCtx, String tid)
        {
            System.out.println("TID Handler: confirmTID for " + tid);
            
            try
            {
                // clean up the resources
            }
            // catch(Throwable t) {} //partner won't react on an exception at
            // this point
            finally
            {
                availableTIDs.remove(tid);
            }
        }
        
        public void execute(JCoServerContext serverCtx)
        {
            String tid = serverCtx.getTID();
            if(tid != null)
            {
                System.out.println("TID Handler: execute for " + tid);
                availableTIDs.put(tid, TIDState.EXECUTED);
            }
        }
        
        private enum TIDState
        {
            CREATED, EXECUTED, COMMITTED, ROLLED_BACK, CONFIRMED;
        }
    }
    
    /**
     * Follow server example demonstrates how to implement the support for tRFC calls, 
     * calls executed BACKGROUND TASK.  
     * At first we write am implementation for JCoServerTIDHandler interface. This implementation is
     * registered by the server instance and will be used for each call send in "background task". Without
     * such implementation JCo runtime deny any tRFC calls. See javadoc for interface JCoServerTIDHandler for
     * details.  
     */ 
    static void step3SimpleTRfcServer()
    {
        JCoServer server;
        try
        {
            server = JCoServerFactory.getServer(SERVER_NAME1);
        }
        catch(JCoException ex)
        {
            throw new RuntimeException("Unable to create the server " + SERVER_NAME1 + " because of " + ex.getMessage(), ex);
        }
        
        JCoServerFunctionHandler stfcConnectionHandler = new StfcConnectionHandler();
        DefaultServerHandlerFactory.FunctionHandlerFactory factory = new DefaultServerHandlerFactory.FunctionHandlerFactory();
        factory.registerHandler("STFC_CONNECTION", stfcConnectionHandler);
        server.setCallHandlerFactory(factory);
        
        // additionally to step 1
        myTIDHandler = new MyTIDHandler();
        server.setTIDHandler(myTIDHandler);
        
        server.start();
        System.out.println("The program can be stopped using <ctrl>+<c>");
    }
    
    
    
    /**
     * The following server example demonstrates how to develop a function module available only on Java side. At first
     * we create the respective function meta data, because the function is not available in ABAP DDIC. Then the function
     * meta data is stored in a custom repository which is registered with the server instance. Naturally we also
     * need the implementation of the function - see the class SetTraceHandler. 
     * 
     * Last but not least, the following ABAP report invokes the function module SetTraceHandler.

        REPORT  ZTEST_JCO_SET_TRACE.
        
        DATA trace_level TYPE N.
        DATA trace_path TYPE STRING.
        DATA msg(255) TYPE C.
        
        trace_level = '5'.
        trace_path = '.'.
        
        CALL FUNCTION 'JCO_SET_TRACE' destination 'JCO_SERVER'
          EXPORTING
            TRACE_LEVEL = trace_level
            TRACE_PATH = trace_path
         EXCEPTIONS
           COMMUNICATION_FAILURE       = 1
           SYSTEM_FAILURE              = 2 MESSAGE msg
           RESOURCE_FAILURE            = 3
           OTHERS                      = 4
                  .
        IF SY-SUBRC <> 0.
           write: 'ERROR: ',  SY-SUBRC, msg.
        ENDIF.     
     */
    static void step4StaticRepository()
    {
        JCoCustomRepository cR = JCo.createCustomRepository("MyCustomRepository");
        JCoListMetaData impList = JCo.createListMetaData("IMPORTS");
        impList.add("TRACE_LEVEL", JCoMetaData.TYPE_NUM, 1, 2, 0, null, null, JCoListMetaData.IMPORT_PARAMETER, null, null);
        impList.add("TRACE_PATH", JCoMetaData.TYPE_STRING, 8, 8, 0, null, null, JCoListMetaData.IMPORT_PARAMETER, null, null);
        impList.lock();
        JCoFunctionTemplate fT = JCo.createFunctionTemplate("JCO_SET_TRACE", impList, null, null, null, null);
        cR.addFunctionTemplateToCache(fT);
        
        JCoServer server;
        try
        {
            server = JCoServerFactory.getServer(SERVER_NAME1);
        }
        catch(JCoException ex)
        {
            throw new RuntimeException("Unable to create the server " + SERVER_NAME1 + " because of " + ex.getMessage(), ex);
        }
        
        String repDest = server.getRepositoryDestination();
        if(repDest!=null)
        {
            try
            {
                cR.setDestination(JCoDestinationManager.getDestination(repDest));
            }
            catch (JCoException e) 
            {
                e.printStackTrace();
                System.out.println(">>> repository contains static function definition only");
            }
        }
        server.setRepository(cR);
        
        
        JCoServerFunctionHandler setTraceHandler = new SetTraceHandler();
        DefaultServerHandlerFactory.FunctionHandlerFactory factory = new DefaultServerHandlerFactory.FunctionHandlerFactory();
        factory.registerHandler(fT.getName(), setTraceHandler);
        server.setCallHandlerFactory(factory);
        
        server.start();
        System.out.println("The program can be stoped using <ctrl>+<c>");
    }
    
    static class SetTraceHandler implements JCoServerFunctionHandler
    {
        public void handleRequest(JCoServerContext serverCtx, JCoFunction function)
        {
            int level = function.getImportParameterList().getInt("TRACE_LEVEL");
            String path = function.getImportParameterList().getString("TRACE_PATH");
            System.out.println(">>> SetTrace invoked with " + level + (path!=null?(", "+path):""));
            JCo.setTrace(level, path);
        }
    }
    

    public static void main(String[] a)
    {
//        step1SimpleServer();
//        step2SimpleServer();
//        step3SimpleTRfcServer();
        step4StaticRepository();
    }
}
