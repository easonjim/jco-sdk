import java.util.Collection;
import java.util.Hashtable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.sap.conn.jco.JCoContext;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoFunctionTemplate;
import com.sap.conn.jco.ext.Environment;
import com.sap.conn.jco.ext.JCoSessionReference;
import com.sap.conn.jco.ext.SessionException;
import com.sap.conn.jco.ext.SessionReferenceProvider;

/**
 *  MultiThreadedExample is rather complex. It demonstrates how to use the SessionReferenceProvider
 *  defined in the package com.sap.conn.jco.ext. 
 *  
 *  Before discussing situations requiring SessionReferenceProvider, we provide a short 
 *  description of how the JCo Runtime handles the stateful and stateless calls by default.
 *  By default all RFC calls (JCoFunction.execute(JCoDestination)) are stateless. That means
 *  the ABAP context associated with the connection will be destroyed. Some RFC modules save
 *  a particular state/data in the ABAP context's area. In order to keep a JCo connection 
 *  and use it for subsequent (stateful) calls, the JCoConext.begin(JCoDestination) API can be used.
 *  In the case of multithreaded applications some calls to a destination can be executed concurrently, so 
 *  JCo Runtime needs to associate a particular call or connection to an internal session. By default 
 *  JCo Runtime associates each thread with a sessionof its own, so that most applications that execute 
 *  all stateful requests en bloc or at least in the same thread will run correctly.  
 *  
 *  Applications that wish to execute calls belonging to a stateful sequence by employing different 
 *  threads have to implement and register the SessionReferenceProvider. The main goal of 
 *  the implementation is to determine to which session the calls executing in the current thread belong.
 *  
 *  This example defines MultiStepJob having several execution steps. The test starts a 
 *  certain number of threads (see runJobs). Each thread is designed to take a job, execute one step, and put the
 *  job back to the shared job list. There are two jobs as an example: StatelessMultiStepExample and
 *  StatefulMultiStepExample. Both invoke the same RFC modules, but StatefulMultiStepExample
 *  uses JCoContext.begin and JCoContext.end to specify the stateful calls.
 *  
 *  To be able to execute a stateful call sequence distributed over several steps, we register 
 *  a custom implementation of SessionReferenceProvider called MySessionReferenceProvider. 
 *  The idea behind MySessionReferenceProvider is simple: each thread 
 *  holds the current session reference in its local storage. To achieve that WorkerThread.run 
 *  sets this session reference before executing the next step and removes it after the
 *  step is finished.
 */
public class MultiThreadedExample
{
    interface MultiStepJob
    {
        boolean isFinished();
        public void runNextStep();
        String getName();
        public void cleanUp();
    }
    
    static class StatelessMultiStepExample implements MultiStepJob
    {
        static AtomicInteger JOB_COUNT = new AtomicInteger(0); 
        int jobID = JOB_COUNT.addAndGet(1);
        int calls;
        JCoDestination destination;
        
        int executedCalls = 0;
        Exception ex = null;
        int remoteCounter;
        
        StatelessMultiStepExample(JCoDestination destination, int calls) 
        { 
            this.calls = calls;
            this.destination = destination;
        }
        
        public boolean isFinished() { return executedCalls == calls || ex != null; }
        public String getName() { return "stateless Job-"+jobID; }
        
        public void runNextStep()
        {
            try
            {
                JCoFunction incrementCounter = incrementCounterTemplate.getFunction();
                incrementCounter.execute(destination);
                JCoFunction getCounter = getCounterTemplate.getFunction();
                executedCalls++;
                
                if(isFinished())
                {
                    getCounter.execute(destination);
                    remoteCounter = getCounter.getExportParameterList().getInt("GET_VALUE");
                }
            }
            catch(JCoException je)
            {
                ex = je;
            }
            catch(RuntimeException re)
            {
                ex = re;
            }
        }

        public void cleanUp() 
        {
            StringBuilder sb = new StringBuilder("Task ").append(getName()).append(" is finished ");
            if(ex!=null)
                sb.append("with exception ").append(ex.toString());
            else
                sb.append("successful. Counter is ").append(remoteCounter);
            System.out.println(sb.toString());
        }
        
    }
    
    static class StatefulMultiStepExample extends StatelessMultiStepExample
    {
        StatefulMultiStepExample(JCoDestination destination, int calls) 
        { 
            super(destination, calls);
            
        }

        @Override
        public String getName() { return "stateful Job-"+jobID; }
        
        @Override
        public void runNextStep()
        {
            if(executedCalls == 0)
                JCoContext.begin(destination);
            super.runNextStep();
        }
        
        @Override
        public void cleanUp() 
        {
            try
            {
                JCoContext.end(destination);
            }
            catch (JCoException je)
            {
                ex = je;
            }
            super.cleanUp();
        }
    }
    
    static class MySessionReferenceProvider implements SessionReferenceProvider
    {
        public JCoSessionReference getCurrentSessionReference(String scopeType)
        {
            MySessionReference sesRef = WorkerThread.localSessionReference.get();
            if(sesRef != null)
                return sesRef; 
            
            throw new RuntimeException("Unknown thread:" + Thread.currentThread().getId());
        }

        public boolean isSessionAlive(String sessionId)
        {
            Collection<MySessionReference> availableSessions = WorkerThread.sessions.values();
            for(MySessionReference ref : availableSessions)
            {
                if(ref.getID().equals(sessionId))
                    return true;
            }
            return false;
        }

        public void jcoServerSessionContinued(String sessionID) throws SessionException
        {
        }

        public void jcoServerSessionFinished(String sessionID)
        {
        }

        public void jcoServerSessionPassivated(String sessionID) throws SessionException
        {
        }

        public JCoSessionReference jcoServerSessionStarted() throws SessionException
        {
            return null;
        }
    }
    
    static class MySessionReference implements JCoSessionReference
    {
        static AtomicInteger atomicInt = new AtomicInteger(0);
        private String id = "session-"+String.valueOf(atomicInt.addAndGet(1));;
        
        public void contextFinished()
        {
        }

        public void contextStarted()
        {
        }

        public String getID()
        {
            return id;
        }
        
    }
    
    static class WorkerThread extends Thread
    {
        static Hashtable<MultiStepJob, MySessionReference> sessions = new Hashtable<MultiStepJob, MySessionReference>();
        static ThreadLocal<MySessionReference> localSessionReference = new ThreadLocal<MySessionReference>();
        
        private CountDownLatch doneSignal;
        WorkerThread(CountDownLatch doneSignal)
        {
            this.doneSignal = doneSignal;
        }
        
        @Override
        public void run()
        {
            try
            {
                for(;;)
                {
                    MultiStepJob job = queue.poll(10, TimeUnit.SECONDS);
                    
                    //stop if nothing to do
                    if(job == null)
                        return;

                    MySessionReference sesRef = sessions.get(job);
                    if(sesRef == null)
                    {
                        sesRef = new MySessionReference();
                        sessions.put(job, sesRef);
                    }
                    localSessionReference.set(sesRef);
                    
                    System.out.println("Task "+job.getName()+" is started.");
                    try
                    {
                        job.runNextStep();
                    }
                    catch (Throwable th)
                    {
                        th.printStackTrace();
                    }

                    if(job.isFinished())
                    {
                        System.out.println("Task "+job.getName()+" is finished.");
                        sessions.remove(job);
                        job.cleanUp();
                    }
                    else
                    {
                        System.out.println("Task "+job.getName()+" is passivated.");
                        queue.add(job);
                    }
                    localSessionReference.set(null);
                }
            }
            catch (InterruptedException e)
            {
                //just leave
            }
            finally
            {
                doneSignal.countDown();
            }
        }
    }

    private static BlockingQueue<MultiStepJob> queue = new LinkedBlockingQueue<MultiStepJob>();
    private static JCoFunctionTemplate incrementCounterTemplate, getCounterTemplate;

    
    static void runJobs(JCoDestination destination, int jobCount, int threadCount)
    {
        System.out.println(">>> Start");
        for(int i = 0; i < jobCount; i++)
        {
            queue.add(new StatelessMultiStepExample(destination, 10));
            queue.add(new StatefulMultiStepExample(destination, 10));
        }

        CountDownLatch doneSignal = new CountDownLatch(threadCount);
        for(int i = 0; i < threadCount; i++)
            new WorkerThread(doneSignal).start();
        
        System.out.print(">>> Wait ... ");
        try
        {
            doneSignal.await();
        }
        catch (InterruptedException ie)
        {
            //just leave
        }
        System.out.println(">>> Done");
    }

    public static void main(String[] argv)
    {
        //JCo.setTrace(5, ".");
        Environment.registerSessionReferenceProvider(new MySessionReferenceProvider());
        try
        {
            JCoDestination destination = JCoDestinationManager.getDestination("ABAP_AS_WITH_POOL");
            incrementCounterTemplate = destination.getRepository().getFunctionTemplate("Z_INCREMENT_COUNTER");
            getCounterTemplate = destination.getRepository().getFunctionTemplate("Z_GET_COUNTER");
            if(incrementCounterTemplate == null || getCounterTemplate == null)
                throw new RuntimeException("This example cannot run without Z_INCREMENT_COUNTER and Z_GET_COUNTER functions");

            runJobs(destination, 5, 2);
        }
        catch(JCoException je)
        {
            je.printStackTrace();
        }
        
    }
}
