package fedora.services.oaiprovider;

import java.util.*;

import org.apache.log4j.*;
import org.jrdf.graph.Literal;
import org.jrdf.graph.Node;
import org.jrdf.graph.URIReference;
import org.trippi.*;

import proai.*;
import proai.driver.*;
import proai.error.*;

public class FedoraSetInfoIterator implements RemoteIterator {

    private static final Logger logger =
            Logger.getLogger(FedoraOAIDriver.class.getName());

    private TupleIterator m_tuples;

    private List m_nextGroup;

    private SetInfo m_next;

    /**
     * Initialize with tuples.
     *
     * The tuples should look like:
     * <pre>
     * "setSpec"    ,"setName"         ,"setDiss"
     * prime        ,Prime             ,null
     * prime        ,Prime             ,info:fedora/demo:SetPrime/SetInfo.xml
     * abovetwo:even,Above Two and Even,null
     * abovetwo:even,Above Two and Even,info:fedora/demo:SetAboveTwoEven/SetInfo.xml
     * abovetwo:odd ,Above Two and Odd ,null
     * abovetwo     ,Above Two         ,null
     * abovetwo     ,Above Two         ,info:fedora/demo:SetAboveTwo/SetInfo.xml
     * </pre>
     */
    public FedoraSetInfoIterator(TupleIterator tuples) throws RepositoryException {
        m_tuples = tuples;
        m_nextGroup = new ArrayList();
        m_next = getNext();
    }

    private SetInfo getNext() throws RepositoryException {
        try {
            List group = getNextGroup();
            if (group.size() == 0) return null;
            String[] values = (String[]) group.get(group.size() - 1);
            return new FedoraSetInfo(values[0], values[1], values[2]);
/*
            if (m_tuples.hasNext()) {
                List group = getNextGroup();
                String[] values = getValues(m_tuples.next());
                String setSpec = values[0];
                String setName = values[1];
                String setDiss = values[2];
                if (setDiss != null) {
                    return new FedoraSetInfo(setSpec, setName, setDiss);
                } else {
                    if (m_tuples.hasNext()) {
                        String[] nextValues = getValues(m_tuples.next());
                        String nextSetSpec = nextValues[0];
                        String nextSetName = nextValues[1];
                        String nextSetDiss = nextValues[2];
                        if (nextSetSpec.equals(setSpec)) {
                        } else {
                        }
                    } else {
                        return new FedoraSetInfo(setSpec, setName, setDiss);
                    }
                    // if setDiss is null, check if it hasNext()
                }
            } else {
                return null;
            }
*/
        } catch (TrippiException e) {
            throw new RepositoryException("Error getting next tuple", e);
        }
    }

    /**
     * Return the next group of value[]s that have the same value for the first element.
     * The first element must not be null.
     */
    private List getNextGroup() throws RepositoryException,
                                       TrippiException {
        List group = m_nextGroup;
        m_nextGroup = new ArrayList();
        String commonValue = null;
        if (group.size() > 0) {
            commonValue = ((String[]) group.get(0))[0];
        }
        while (m_tuples.hasNext() && m_nextGroup.size() == 0) {
            String[] values = getValues(m_tuples.next());
            String firstValue = values[0];
            if (firstValue == null) throw new RepositoryException("Not allowed: First value in tuple was null");
            if (commonValue == null) {
                commonValue = firstValue;
            }
            if (firstValue.equals(commonValue)) {
                group.add(values);
            } else {
                m_nextGroup.add(values);
            }
        }
        return group;
    }

    private String[] getValues(Map valueMap) throws RepositoryException {
        try {
            String[] names = m_tuples.names();
            String[] values = new String[names.length];
            for (int i = 0; i < names.length; i++) {
                values[i] = getString((Node) valueMap.get(names[i]));
            }
            return values;
        } catch (Exception e) {
            throw new RepositoryException("Error getting values from tuple", e);
        }
    }

    private String getString(Node node) throws RepositoryException {
       if (node == null) return null;
       if (node instanceof Literal) {
           return ((Literal) node).getLexicalForm();
       } else if (node instanceof URIReference) {
           return ((URIReference) node).getURI().toString();
       } else {
           throw new RepositoryException("Unhandled node type: " + node.getClass().getName());
       }
    }


/*
        Map parms = m_queryFactory.setInfoQuery();
        TupleIterator tuples = null;
        try {
            tuples = getTuples(parms);
            while (tuples.hasNext()) {
                Map tuple = tuples.next();
                Literal setSpecLiteral = (Literal) tuple.get("setSpec");
                if (setSpecLiteral == null) throw new RepositoryException("Unexpected: got null setSpec");
                String setSpec = setSpecLiteral.getLexicalForm();
                Literal setNameLiteral = (Literal) tuple.get("setName");
                if (setNameLiteral == null) throw new RepositoryException("Unexpected: got null setName");
                String setName = setNameLiteral.getLexicalForm();
                URIReference setDissReference = (URIReference) tuple.get("setDiss");
                if (setDissReference == null) {
                    System.out.println(setSpec + " -> " + setName);
                } else {
                    String setDiss = setDissReference.getURI().toString();
                    System.out.println(setSpec + " -> " + setName + " -> " + setDiss);
                }
            }
        } catch (Exception e) {
            throw new RepositoryException("Error querying for set information", e);
        } finally {
            if (tuples != null) try { tuples.close(); } catch (Exception e) { }
        }
        return null;
*/

    public boolean hasNext() throws RepositoryException {
        return (m_next != null);
    }

    public Object next() throws RepositoryException {
        try {
            return m_next;
        } finally {
            if (m_next != null) m_next = getNext();
        }
    }

    public void close() throws RepositoryException {
        try {
            m_tuples.close();
        } catch (TrippiException e) {
            throw new RepositoryException("Unable to close tuple iterator", e);
        }
    }

    public void remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("FedoraSetInfoIterator does not support remove().");
    }

}