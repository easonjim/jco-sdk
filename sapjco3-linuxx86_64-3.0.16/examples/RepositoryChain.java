import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import com.sap.conn.jco.JCo;
import com.sap.conn.jco.JCoClassMetaData;
import com.sap.conn.jco.JCoCustomRepository;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoFunctionTemplate;
import com.sap.conn.jco.JCoListMetaData;
import com.sap.conn.jco.JCoRecordMetaData;
import com.sap.conn.jco.JCoRepository;
import com.sap.conn.jco.JCoRequest;
import com.sap.conn.jco.monitor.JCoRepositoryMonitor;
import com.sap.conn.jco.server.DefaultServerHandlerFactory;
import com.sap.conn.jco.server.JCoServer;
import com.sap.conn.jco.server.JCoServerFactory;

/**
 * Metadata Repository in the JCo 2.x offers chaining of repositories. The idea behind was to
 * enable automatically the look up in more than one SAP system and/or in the custom repository/repositories.  
 * 
 * This feature is not supported by JCo 3.0, because it was rarely used in the past and because
 * of troubles caused by incompatible metadata in different systems and out-dated custom repositories.
 * However the chain of repositories can be easily implemented and effectively used in
 * some specific scenarios.
 * 
 * The class RepositoryChain demonstrates how the repository chain can be implemented and
 * provides some tests in the main function. The implementation can be changed for the application
 * needs. 
 * 
 * Note: the repository monitor was not implemented for the RepositoryChain in this example.
 * Note: the implementation does not check the cycles in the chain
 */
public class RepositoryChain implements JCoRepository
{
    private JCoRepository current;
    private RepositoryChain next;
    
    /**
     * Creates a first chain pointed to the given repository
     * @param repository repository in the first chain
     */
    public RepositoryChain(JCoRepository repository)
    {
        if(repository instanceof RepositoryChain)
            current = ((RepositoryChain)repository).current;
        else
            current = repository;
    }
    
    /**
     * creates and sets the next chain using the given repository
     * @param repository for the next chain
     * @return removed repository if the next chain was already specified or null
     */
    public JCoRepository setNextRepository(JCoRepository repository)
    {
        if(repository instanceof RepositoryChain)
            return setNextRepositoryChain((RepositoryChain)repository);
        
        RepositoryChain removed = null;
        removed = next;
        next = new RepositoryChain(repository);
        return removed!=null?removed.getRepository():null;
    }
    
    /**
     * Returns the repository instance from the next chain
     * @return the repository instance from the next chain or null, if not set
     */
    public JCoRepository getNextRepository()
    {
        return next!=null?next.getRepository():null;
    }

    /**
     * sets the next chain
     * @param repositoryChain the next chain
     * @return removed repository chain if the next chain was already specified or null
     */
    public RepositoryChain setNextRepositoryChain(RepositoryChain repositoryChain)
    {
        RepositoryChain removed = null;
        removed = next;
        next = repositoryChain;
        return removed;
    }
    
    /**
     * Returns the next repository chain
     * @return the next repository chain or null, if not set
     */
    public RepositoryChain getNextRepositoryChain()
    {
        return next;
    }
    
    /**
     * Returns the repository stored in this chain
     * @return the repository stored in this chain
     */
    public JCoRepository getRepository()
    {
        return current;
    }

    /**
     * not supported
     */
    public JCoRepositoryMonitor getMonitor()
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * returns generated name containing the names of repositories in the chain
     */
    public String getName()
    {
        StringBuilder sb = new StringBuilder("Repository chain [");
        appendName(sb);
        sb.append("]");
        return sb.toString();
    }
    
    void appendName(StringBuilder sb)
    {
        sb.append(current.getName());
        if(next!=null)
        {
            sb.append(", ");
            next.appendName(sb);
        }
    }
    
    /**
     * return the function names cached in all chains
     */
    public String[] getCachedFunctionTemplateNames()
    {
        List<String> list = new ArrayList<String>();
        list = getCachedFunctionTemplateNames(list);
        return (String[]) list.toArray();
    }
    
    List<String> getCachedFunctionTemplateNames(List<String> list)
    {
        String[] cachedNames = current.getCachedFunctionTemplateNames();
        for(String cachedName : cachedNames)
            list.add(cachedName);
        if(next!=null)
            list = next.getCachedFunctionTemplateNames(list);
        return list;
    }
    
    /**
     * return the record names cached in all chains
     */
    public String[] getCachedRecordMetaDataNames()
    {
        List<String> list = new ArrayList<String>();
        list = getCachedRecordMetaDataNames(list);
        return (String[]) list.toArray();
    }
    
    List<String> getCachedRecordMetaDataNames(List<String> list)
    {
        String[] cachedNames = current.getCachedRecordMetaDataNames();
        for(String cachedName : cachedNames)
            list.add(cachedName);
        if(next!=null)
            list = next.getCachedRecordMetaDataNames(list);
        return list;
    }

    /**
     * returns a function, if available in one of the repositories in the chain
     * or null
     * @param functionName function name
     */
    public JCoFunction getFunction(String functionName) throws JCoException
    {
        JCoFunction function = current.getFunction(functionName);
        if(function == null && next != null)
            return next.getFunction(functionName);
        return function;
    }
    
    /**
     * returns a function interface, if available in one of the repositories in the chain
     * or null
     * @param functionName function name
     */
    public JCoListMetaData getFunctionInterface(String functionName) throws JCoException
    {
        JCoListMetaData functionInterface = current.getFunctionInterface(functionName);
        if(functionInterface == null && next != null)
            return next.getFunctionInterface(functionName);
        return functionInterface;
    }
    
    /**
     * returns a function template, if available in one of the repositories in the chain
     * or null
     * @param functionName function name
     */
    public JCoFunctionTemplate getFunctionTemplate(String functionName) throws JCoException
    {
        JCoFunctionTemplate template = current.getFunctionTemplate(functionName);
        if(template == null && next != null)
            return next.getFunctionTemplate(functionName);
        return template;
    }
    
    /**
     * returns a record metadata (structure) description, if available in one of the repositories in the chain
     * or null
     * @param recordName record/structure name
     */
    public JCoRecordMetaData getRecordMetaData(String recordName) throws JCoException
    {
        JCoRecordMetaData template = current.getRecordMetaData(recordName);
        if(template == null && next != null)
            return next.getRecordMetaData(recordName);
        return template;
    }
    
    /**
     * returns a request metadata, if available in one of the repositories in the chain
     * or null
     * @param functionName function name
     */
    public JCoRequest getRequest(String functionName) throws JCoException
    {
        JCoRequest template = current.getRequest(functionName);
        if(template == null && next != null)
            return next.getRequest(functionName);
        return template;
    }
    
    /**
     * returns a structure description, if available in one of the repositories in the chain
     * or null
     * @param structureName record/structure name
     */
    public JCoRecordMetaData getStructureDefinition(String structureName) throws JCoException
    {
        return getRecordMetaData(structureName);
    }
    
    
    /**
     * return the record names cached in all chains
     */
    public String[] getCachedClassMetaDataNames()
    {
        List<String> list = new ArrayList<String>();
        list = getCachedClassMetaDataNames(list);
        return (String[]) list.toArray();
    }
    
    List<String> getCachedClassMetaDataNames(List<String> list)
    {
        String[] cachedNames = current.getCachedClassMetaDataNames();
        for(String cachedName : cachedNames)
            list.add(cachedName);
        if(next!=null)
            list = next.getCachedClassMetaDataNames(list);
        return list;
    }

    public JCoClassMetaData getClassMetaData(String className) throws JCoException
    {
        JCoClassMetaData classMeta = current.getClassMetaData(className);
        if(classMeta == null && next != null)
            return next.getClassMetaData(className);
        return classMeta;
    }

    /**
     * returns true if all repositories in the chain contains the unicode metadata
     */
    public boolean isUnicode()
    {
        boolean isUnicode = current.isUnicode();
        if(isUnicode && next!=null)
            isUnicode = next.isUnicode();
        return isUnicode;
    }
    
    /**
     * clears all repositories in the chain 
     */
    public void clear()
    {
        current.clear();
        if(next!=null)
            next.clear();
    }

    /**
     * removes the specified function in all repositories of the chain
     * @param functionName function name
     */
    public void removeFunctionTemplateFromCache(String functionName)
    {
        current.removeFunctionTemplateFromCache(functionName);
        if(next!=null)
            next.removeFunctionTemplateFromCache(functionName);
    }
    
    /**
     * removes the specified record (structure) in all repositories of the chain
     * @param recordName record/structure name
     */
    public void removeRecordMetaDataFromCache(String recordName)
    {
        current.removeRecordMetaDataFromCache(recordName);
        if(next!=null)
            next.removeRecordMetaDataFromCache(recordName);
    }
    
    /**
     * removes the specified class  in all repositories of the chain
     * @param className class name
     */
    public void removeClassMetaDataFromCache(String className)
    {
        current.removeClassMetaDataFromCache(className);
        if(next!=null)
            next.removeClassMetaDataFromCache(className);
    }
    
    public void save(Writer writer) throws IOException
    {
        current.save(writer);
        if(next!=null)
        {
        	writer.write(",");
        	writer.write(System.getProperty("line.separator"));
            next.save(writer);
        }
    }

	public void load(Reader reader) throws IOException
    {
        current.load(reader);
        if(next!=null)
        {
        	char c = (char)reader.read();
        	if(c == ',')
                next.load(reader);
        	else
        		throw new RuntimeException("Unexpected char ["+c+" between repositories in chain");
        }
    }

	public static void main(String[] a) throws JCoException, IOException
    {
        //prepare chain
        JCoDestination dest = JCoDestinationManager.getDestination("V9U");
        JCoRepository rep = dest.getRepository();
        JCoCustomRepository cRep1 = JCo.createCustomRepository("Custom Repository 1"); 
        JCoCustomRepository cRep2 = JCo.createCustomRepository("Custom Repository 2");
        JCoFunctionTemplate STFC_CONNECTION = rep.getFunctionTemplate("STFC_CONNECTION");
        JCoFunctionTemplate RFC_GET_SYSTEM_INFO = rep.getFunctionTemplate("RFC_GET_SYSTEM_INFO");
        cRep1.addFunctionTemplateToCache(STFC_CONNECTION);
        cRep2.addFunctionTemplateToCache(RFC_GET_SYSTEM_INFO);
        
        //build chains
        JCoRepository chain1 = new RepositoryChain(cRep1);
        RepositoryChain chain2 = new RepositoryChain(cRep2);
        
        //tests
        if(chain1.getFunction("STFC_CONNECTION")==null)
            System.out.println("error in chain1: STFC_CONNECTION should be avaialble");
        if(chain1.getFunction("RFC_GET_SYSTEM_INFO")!=null)
            System.out.println("error in chain1: RFC_GET_SYSTEM_INFO should not be avaialble");

        if(chain2.getFunction("STFC_CONNECTION")!=null)
            System.out.println("error in chain1: STFC_CONNECTION should not be avaialble");
        if(chain2.getFunction("RFC_GET_SYSTEM_INFO")==null)
            System.out.println("error in chain1: RFC_GET_SYSTEM_INFO should be avaialble");
        
        //connects cRep1 to cRep2
        ((RepositoryChain)chain1).setNextRepository(chain2);
        if(chain1.getFunction("STFC_CONNECTION")==null)
            System.out.println("error in chain1: STFC_CONNECTION should be avaialble");
        if(chain1.getFunction("RFC_GET_SYSTEM_INFO")==null)
            System.out.println("error in chain1: RFC_GET_SYSTEM_INFO should be avaialble now");
        
        //connects cRep2 to rep
        chain2.setNextRepository(rep);
        if(chain2.getFunction("STFC_STRUCTURE")==null)
            System.out.println("error in chain2: STFC_STRUCTURE should be avaialble via rep(BIN)");
        if(chain1.getFunction("STFC_STRUCTURE")==null)
            System.out.println("error in chain1: STFC_STRUCTURE should be avaialble via rep(BIN)");
        
        chain2.save(new FileWriter("chain.txt"));
        
        System.out.println("client tests are finished");
        
        JCoServer server = JCoServerFactory.getServer("SERVER");
        server.setRepository(chain1);
        server.setCallHandlerFactory(new DefaultServerHandlerFactory.FunctionHandlerFactory());
        server.start();
        System.out.println("stop with <cntl-c>");
        
    }
    
}
