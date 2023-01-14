package jacamo.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import jacamo.infra.JaCaMoInfrastructureFactory;
import jacamo.project.parser.JaCaMoProjectParser;
import jacamo.project.parser.ParseException;
import jason.JasonException;
import jason.infra.InfrastructureFactory;
import jason.mas2j.AgentParameters;
import jason.mas2j.ClassParameters;
import jason.mas2j.MAS2JProject;
import jason.runtime.SourcePath;

public class JaCaMoProject extends MAS2JProject implements Serializable {

    private static final long serialVersionUID = 1L;

    protected static Logger logger = Logger.getLogger(JaCaMoProject.class.getName());

    protected Map<String,JaCaMoWorkspaceParameters> workspaces = new HashMap<>();
    protected List<JaCaMoOrgParameters>       orgs       = new ArrayList<>();
    protected List<JaCaMoInstParameters>      insts      = new ArrayList<>();
    protected Map<String,String> nodeHosts = new HashMap<>();
    //protected Set<String> toDebug = new HashSet<String>();

    protected SourcePath orgPaths = new SourcePath();
    protected SourcePath javaSourcePaths = new SourcePath();

    Map<String, String[]> platformParameters = new HashMap<>();
    
    protected Properties deployHosts = new Properties();
    
    protected String runArgs;

    public JaCaMoProject() {
        // default asl-path
        addSourcePath(".");
        addSourcePath("src/agt");
        addSourcePath("src/agt/inc");
        
        addClassPath("lib");
        
        // default org-path
        orgPaths.addPath(".");
        orgPaths.addPath("src/org");
        
        // default java-path
        javaSourcePaths.addPath(".");
        javaSourcePaths.addPath("src/env");
        javaSourcePaths.addPath("src/agt");
    }

    public JaCaMoProject(MAS2JProject project) {
        this();
        this.setSocName(project.getSocName());
        this.setProjectFile(project.getProjectFile());
        this.setDirectory( project.getDirectory());
        importProject(project);
    }

    public SourcePath getOrgPaths() {
        return orgPaths;
    }
    public void addOrgSourcePath(String s) {
        orgPaths.addPath(s);
    }
    public SourcePath getJavaSourcePaths() {
        return javaSourcePaths;
    }
    public void addJavaSourcePath(String s) {
        javaSourcePaths.addPath(s);
    }

    public void importProject(String directory, String fileName) throws ParseException {
        // import project
        try {
              if (! fileName.endsWith(".jcm")) fileName = fileName+".jcm";
              
              if (! new File(fileName).exists()) fileName = directory+"/"+fileName.toString();
              JaCaMoProjectParser parser = new JaCaMoProjectParser(new FileReader(fileName) );
              importProject(parser.parse(directory));
        } catch (ParseException e) {
            throw new ParseException("Error parsing included file "+fileName+"!\n"+e);
        } catch (FileNotFoundException e) {
            throw new ParseException("File not found "+fileName+"\n"+e);
        }
    }

    public void importProject(MAS2JProject project) {
        // import from mas2j project
        // COPY all other parameters from project to this
        getSourcePaths().addAll(project.getSourcePaths());

        for (String p: project.getClassPaths())
            this.addClassPath(p);

        if (project instanceof JaCaMoProject) {
            JaCaMoProject jproject = (JaCaMoProject)project;

            // also import JaCaMo part
            for (AgentParameters a: project.getAgents()) {
                this.addAgent(a);
                ((JaCaMoAgentParameters)a).setProject(this);
            }
            for (JaCaMoOrgParameters o: jproject.orgs) {
                this.addOrg(o);
            }
            for (JaCaMoWorkspaceParameters w: jproject.workspaces.values()) {
                if (getOrg(w.getName()) == null)
                    this.addWorkspace(w);
            }
            //this.deplNodes.addAll(jproject.deplNodes);
            this.nodeHosts.putAll(jproject.nodeHosts);
            this.platformParameters.putAll(jproject.platformParameters);

        } else {
            for (AgentParameters a: project.getAgents()) {
                this.addAgent(new JaCaMoAgentParameters(this,a));
            }
        }
    }

    /*
    public Deployment getDeployment() {
        return dep;
    }
    */

    @Override
    public void setupDefault() {
    }

    @Override
    public InfrastructureFactory getInfrastructureFactory() throws JasonException {
        return new JaCaMoInfrastructureFactory();
    }

    @Override
    public boolean isJade() {
        return getPlatformParameters().keySet().contains("jade");
    }

    /*
    public void addAgInstance(String agId, String sQty, String host) {
        AgentParameters ap = getAg(agId);
        if (ap == null) {
            logger.warning("Agent "+agId+" was not declared and so no instances can be defined for it!");
        } else {
            ap = ap.copy();
            ap.setNbInstances(Integer.parseInt(sQty));
            ap.setHost(host);
            dep.addAgInstance(ap);
        }
    }
    */

    public void addAgFocus(String agId, String nameSpace, String artId, JaCaMoWorkspaceParameters wks) {
        if (agId.equals("*")) {
            for (AgentParameters ap: getAgents()) {
                JaCaMoAgentParameters jap = (JaCaMoAgentParameters)ap;
                jap.addFocus(artId,nameSpace,wks.getName());
            }
        } else {
            JaCaMoAgentParameters ap = (JaCaMoAgentParameters)getAg(agId);
            if (ap == null) {
                logger.warning("Agent "+agId+" was not declared and so cannot focus on "+artId);
            } else {
                JaCaMoAgentParameters jap = (JaCaMoAgentParameters)ap;
                jap.addFocus(artId,nameSpace,wks.getName());
            }
        }
    }

    public JaCaMoWorkspaceParameters getArtifactWorkspace(String artId) {
        for (JaCaMoWorkspaceParameters w: workspaces.values()) {
            if (w.getArtifact(artId) != null)
                return w;
        }
        return null;
    }

    public JaCaMoOrgParameters getGroupOrg(String gId) {
        for (JaCaMoOrgParameters o: orgs) {
            if (o.getGroup(gId) != null)
                return o;
        }
        return null;
    }

    public void parserFinished() {
        for (AgentParameters a: getAgents()) {
            ((JaCaMoAgentParameters)a).fix();
        }
    }

    /*
    public void addWksInstance(String wksId, String host) {
        JaCaMoWorkspaceParameters wp = workspaces.get(wksId);
        if (wp == null) {
            wp = new JaCaMoWorkspaceParameters(this);
            wp.setName(wksId);
        }
        wp.setHost(host);
    }
    */

    public void addWorkspace(JaCaMoWorkspaceParameters w) {
        workspaces.put(w.getName(),w);
    }
    public Collection<JaCaMoWorkspaceParameters> getWorkspaces() {
        return workspaces.values();
    }
    public JaCaMoWorkspaceParameters getWorkspace(String wid) {
        JaCaMoWorkspaceParameters w = workspaces.get(wid);
        if (w == null) {
            for (JaCaMoOrgParameters o: orgs) {
                if (o.getName().equals(wid))
                    return o;
            }
        }
        return w;
    }

    public void addOrg(JaCaMoOrgParameters o) {
        orgs.add(o);
    }
    public Collection<JaCaMoOrgParameters> getOrgs() {
        return orgs;
    }
    public JaCaMoOrgParameters getOrg(String oid) {
        for (JaCaMoOrgParameters o: orgs) {
            if (o.getName().equals(oid))
                return o;
        }
        return null;
    }


    public void addInstitution(JaCaMoInstParameters o) {
        insts.add(o);
    }
    public Collection<JaCaMoInstParameters> getInstitutions() {
        return insts;
    }
    public JaCaMoInstParameters getInstitution(String iId) {
        for (JaCaMoInstParameters o: insts) {
            if (o.getName().equals(iId))
                return o;
        }
        return null;
    }

    public void addAgWorkspace(String agId, JaCaMoWorkspaceParameters w) {
        if (agId.equals("*")) {
            for (AgentParameters ap: getAgents()) {
                JaCaMoAgentParameters jap = (JaCaMoAgentParameters)ap;
                jap.addWorkspace(w.getName());
            }
        } else {
            JaCaMoAgentParameters ap = (JaCaMoAgentParameters)getAg(agId);
            if (ap == null) {
                logger.warning("Agent "+agId+" was not declared and so cannot be included in workspace "+w.getName());
            } else {
                ap.addWorkspace(w.getName());
            }
        }
    }

    public void addAgRole(String agId, JaCaMoOrgParameters org, JaCaMoGroupParameters group, String role) {
        JaCaMoAgentParameters ap = (JaCaMoAgentParameters)getAg(agId);
        if (ap == null) {
            logger.warning("Agent "+agId+" was not declared and so cannot play role "+role);
        } else {
            ap.addRole(org.getName(), group.getName(), role);
            addAgWorkspace(agId, org);
        }
    }

    public Collection<String> getPlatforms() {
        return platformParameters.keySet();
    }

    public Collection<String> getCustomPlatforms() {
        ArrayList<String> l = new ArrayList<>();
        for (String pId: platformParameters.keySet()) {
            if (pId.contains(".")) {
                l.add(pId);
            }           
        }
        return l;
    }
    
    public void resetPlatform() {
        platformParameters.clear();
    }

    public void addPlatformParameters(ClassParameters cp) {
        if (cp.getClassName().equals("centralised")) {
            setInfrastructure(cp);
        }
        if (cp.getClassName().equals("local")) {
            setInfrastructure(cp);
        }
        platformParameters.put(cp.getClassName(), cp.getParametersArray());
    }

    public Map<String,String[]> getPlatformParameters() {
        return platformParameters;
    }
    
    private static String[] emptyStringArray = {};
    
    public String[] getPlatformParameters(String p) {
        String a[] = platformParameters.get(p);
        if (a == null)
            return emptyStringArray;
        else
            return a;
    }
    public boolean hasPlatformParameter(String p, String arg) {
        String[] args = platformParameters.get(p);
        if (args != null) {
            for (String pa: args) {
                if (pa.contains(arg))
                    return true;
            }
        }
        return false;
    }

    /*
    public void resetDeploymentNode() {
        deplNodes.clear();
    }
    public void addDeploymentNode(String n) {
        if (n.equals("*")) {
            deplNodes.addAll( getAllCitedNodes() );
        } else {
            deplNodes.add(n);
        }
    }
    */
    /*public Collection<String> getAllCitedNodes() {
        Set<String> an = new HashSet<>();
        for (AgentParameters ap: getAgents())
            if (ap.getHost() != null)
                an.add(ap.getHost());
        for (JaCaMoWorkspaceParameters wp: getWorkspaces())
            if (wp.getNode() != null)
                an.add(wp.getNode());
        for (JaCaMoOrgParameters op: getOrgs())
            if (op.getNode() != null)
                an.add(op.getNode());
        return an;
    }
    */
    /*public Collection<String> getDeploymentNodes() {
        return deplNodes;
    }
    */


    /*public boolean isInDeployment(String node) {
        return node == null ||
               //deplNodes.isEmpty() ||
               deplNodes.contains(node) ||
               nodeHosts.get(node) == null;
    }

    public void resetNodeHosts() {
        nodeHosts.clear();
    }
    */
    public void addNodeHost(String n, String h, boolean running) {
        //if (!running)
        //    deplNodes.add(n);
        nodeHosts.put(n, h);
    }
    public String getNodeHost(String n) {
        return nodeHosts.get(n);
    }

    /*
    private static int lastNode = 1;
    public String createNodeForHost(String h) {
        String nodeId = "n__" + (lastNode++);
        addNodeHost(nodeId, h);
        return nodeId;
    }

    public void addDebugFor(String n) {
        toDebug.add(n);
    }
    public boolean hasDebug(String n) {
        return toDebug.contains(n);
    }

    public void addDebugParameter(String n, String k, String v) {
        //System.out.println("*** not used yet, debug "+n+"{"+k+":"+v+"}");
    }
    */

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();

        s.append("/*\n");
        s.append("    JaCaMo Project\n\n");
        s.append("    -- created on "+new SimpleDateFormat("MMMM dd, yyyy").format(new Date())+"\n");
        s.append("*/\n\n");
        s.append("mas " + getSocName() + " {\n\n");

        // agents
        Iterator<AgentParameters> i = getAgents().iterator();
        while (i.hasNext()) {
            s.append("   "+i.next());
            s.append("\n");
        }

        for (JaCaMoWorkspaceParameters w: workspaces.values()) {
            s.append(w.toString()+"\n\n");
        }

        for (JaCaMoWorkspaceParameters o: insts) {
            s.append(o.toString()+"\n\n");
        }

        for (JaCaMoWorkspaceParameters o: orgs) {
            s.append(o.toString()+"\n\n");
        }

        // deployment

        /*String bgn = "";
        for (String n: deplNodes) {
            s.append(bgn+n);
            bgn = ",";
        }
        if (deplNodes.isEmpty()) {
            s.append("*");
        }

        s.append(" {\n\n");
        */

        String bgn = "   class-path: ";
        for (String p: getClassPaths()) {
            s.append(bgn+p+"\n");
            bgn = "               ";
        }

        bgn = "   asl-path:   ";
        for (String p: getSourcePaths().getPaths()) {
            s.append(bgn+p+"\n");
            bgn = "               ";
        }

        for (String n: nodeHosts.keySet()) {
            s.append("   node "+n+"@"+nodeHosts.get(n)+"\n");
        }
        /*
        for (String n: toDebug) {
            s.append("   debug: "+n+"\n");
        }

        bgn = "   ag-instances: ";
        for (AgentParameters ap: dep.getAgInstances()) {
            s.append(bgn+ap.getNbInstances()+" "+ap.getAgName());
            if (ap.getHost() != null) {
                s.append(" @ "+ap.getHost());
            }
            s.append("\n");
            bgn = "                 ";
        }

        bgn = "   wks-instances: ";
        for (JaCaMoWorkspaceParameters w: getWorkspaces()) {
            if (w.getHost() != null) {
                s.append(bgn+w.getName()+" @ "+w.getHost()+"\n");
                bgn = "                 ";
            }
        }
        */

        bgn = "   platform:     ";
        for (String p: getPlatformParameters().keySet()) {
            s.append(bgn+p+"(");
            String v = "";
            for (String arg: getPlatformParameters(p)) {
                s.append(v+arg);
                v = ", ";
            }
            s.append(")\n");
            bgn = "                 ";
        }

        s.append("}");

        return s.toString();
    }
    
    public void setDeployHosts(String fileName) {
        try {
            deployHosts.load(new FileInputStream(fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public String getDeployHost(String id) {
        if (deployHosts != null && deployHosts.get(id) != null) {
            return deployHosts.get(id).toString();
        }
        return null;
    }

    public String getRunArgs() {
        return runArgs;
    }

    public void setRunArgs(String runArgs) {
        this.runArgs = runArgs;
    }

}
