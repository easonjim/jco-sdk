import java.util.Hashtable;
import java.util.Map;

import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.server.JCoServer;
import com.sap.conn.jco.server.JCoServerContext;
import com.sap.conn.jco.server.JCoServerFactory;
import com.sap.conn.jco.server.JCoServerFunctionHandler;
import com.sap.conn.jco.server.JCoServerFunctionHandlerFactory;

/**
 * This example demonstrate the stateful RFC function modules.
 * To run the example, please create in your SAP System:<br>
 * <ul>
 *  <li>remote enabled function Z_INCREMENT_COUNTER wrapping the INCREMENT_COUNTER<br>
 *      <pre>
 *          FUNCTION Z_INCREMENT_COUNTER.
 *          CALL FUNCTION 'INCREMENT_COUNTER'.
 *          ENDFUNCTION.
 *      </pre>
 *  <li>remote enabled function Z_GET_COUNTER wrapping the GET_COUNTER<br>
 *      <pre>
 *          FUNCTION Z_GET_COUNTER.
 *          CALL FUNCTION 'GET_COUNTER'
 *              IMPORTING
 *                 GET_VALUE = GET_VALUE
 *          .
 *          ENDFUNCTION.
 *      </pre>
 *      with GET_VALUE TYPE  I as export parameter
 *  <li>report ZJCO_STATEFUL_COUNTER<br>
 *      <pre>
 *          REPORT  ZJCO_STATEFUL_COUNTER.
 *          PARAMETER dest TYPE RFCDEST.
 *          
 *          DATA value TYPE i.
 *          DATA loops TYPE i VALUE 5.
 *          
 *          DO loops TIMES.
 *              CALL FUNCTION 'Z_INCREMENT_COUNTER' DESTINATION dest.
 *          ENDDO.
 *          
 *          CALL FUNCTION 'Z_GET_COUNTER' DESTINATION dest
 *              IMPORTING
 *                 GET_VALUE       = value
 *          .
 *          
 *          IF value <> loops.
 *            write: / 'Error expecting ', loops, ', but get ', value, ' as counter value'.
 *          ELSE.
 *            write: / 'success'.
 *          ENDIF.
 *      </pre>
 * </ul>
 *
 * The function modules are required in this example for repository queries only. 
 * The client-side stateful communication is illustrated by the client examples.
 */
public class StatefulServerExample
{
    static class MyFunctionHandlerFactory implements JCoServerFunctionHandlerFactory
    {
        class SessionContext
        {
            Hashtable<String, Object> cachedSessionData = new Hashtable<String, Object>();
        }
        
        private Map<String, SessionContext> statefulSessions = 
            new Hashtable<String, SessionContext>();

        private ZGetCounterFunctionHandler zGetCounterFunctionHandler = 
            new ZGetCounterFunctionHandler();

        private ZIncrementCounterFunctionHandler zIncrementCounterFunctionHandler = 
            new ZIncrementCounterFunctionHandler();
        
        public JCoServerFunctionHandler getCallHandler(JCoServerContext serverCtx, String functionName)
        {
            JCoServerFunctionHandler handler = null;
            
            if(functionName.equals("Z_INCREMENT_COUNTER"))
                handler = zIncrementCounterFunctionHandler;
            else if(functionName.equals("Z_GET_COUNTER"))
                handler = zGetCounterFunctionHandler;
            
            if(handler instanceof StatefulFunctionModule)
            {
                SessionContext cachedSession;
                if(!serverCtx.isStatefulSession())
                {
                    serverCtx.setStateful(true);
                    cachedSession = new SessionContext();
                    statefulSessions.put(serverCtx.getSessionID(), cachedSession);
                }
                else
                {
                    cachedSession = statefulSessions.get(serverCtx.getSessionID());
                    if(cachedSession == null)
                        throw new RuntimeException("Unable to find the session context for session id " + serverCtx.getSessionID());
                }
                ((StatefulFunctionModule)handler).setSessionData(cachedSession.cachedSessionData);
                return handler;
            }
            
            //null leads to a system failure on the ABAP side 
            return null;
        }

        public void sessionClosed(JCoServerContext serverCtx, String message, boolean error)
        {
            System.out.println("Session " + serverCtx.getSessionID() + " was closed " + (error?message:"by SAP system"));
            statefulSessions.remove(serverCtx.getSessionID());
        }
    }

    static abstract class StatefulFunctionModule implements JCoServerFunctionHandler
    {
        Hashtable<String, Object> sessionData;
        public void setSessionData(Hashtable<String, Object> sessionData)
        {
            this.sessionData = sessionData;
        }
    }
    
    
    static class ZGetCounterFunctionHandler extends StatefulFunctionModule
    {
        public void handleRequest(JCoServerContext serverCtx, JCoFunction function)
        {
            System.out.println("ZGetCounterFunctionHandler: return counter");
            Integer counter = (Integer)sessionData.get("COUNTER");
            if(counter == null)
                function.getExportParameterList().setValue("GET_VALUE", 0);
            else
                function.getExportParameterList().setValue("GET_VALUE", counter.intValue());
        }
        
    }

    static class ZIncrementCounterFunctionHandler extends StatefulFunctionModule
    {
        public void handleRequest(JCoServerContext serverCtx, JCoFunction function)
        {
            System.out.println("ZIncrementCounterFunctionHandler: increase counter");
            Integer counter = (Integer)sessionData.get("COUNTER");
            if(counter == null)
                sessionData.put("COUNTER", new Integer(1));
            else
                sessionData.put("COUNTER", new Integer(counter.intValue()+1));
        }
    }
    
    public static void main(String[] args)
    {
        String serverName = "SERVER";
        JCoServer server;
        try
        {
            server = JCoServerFactory.getServer(serverName);
        }
        catch(JCoException ex)
        {
            throw new RuntimeException("Unable to create the server " + serverName + ", because of " + ex.getMessage(), ex);
        }
        
        server.setCallHandlerFactory(new MyFunctionHandlerFactory());
        
        server.start();
        System.out.println("The program can be stopped using <ctrl>+<c>");
    }
}
