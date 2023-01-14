package jason.asSemantics;

//import java.util.*;
import java.util.List;
import java.util.Queue;
import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.regex.Pattern;

//import jason.asSyntax.*;
import jason.asSyntax.Plan;
import jason.asSyntax.Trigger;
import jason.asSyntax.Literal;

public class ISAgent extends Agent {

    private static final long serialVersionUID = -841375841710372772L;
    
    private Pattern[] patternsTE = {
        Pattern.compile("^[a-zA-Z0-9]+\\([a-zA-Z0-9]+\\)$", Pattern.CASE_INSENSITIVE),                              // case 0
        Pattern.compile("^[a-zA-Z0-9]+_[a-zA-Z0-9]+\\([a-zA-Z0-9]+,\\s?[a-zA-Z0-9]+\\)$", Pattern.CASE_INSENSITIVE) // case 1
    };
    private Pattern[] patternsCntx = {
        Pattern.compile("is_[a-zA-Z0-9]+\\([a-zA-Z0-9]+\\)", Pattern.CASE_INSENSITIVE),                   // case 0
        Pattern.compile("(too|in)_[a-zA-Z0-9]+\\([a-zA-Z0-9]+\\)", Pattern.CASE_INSENSITIVE),             // case 1
        Pattern.compile("contains\\([a-zA-Z0-9]+\\)\\[.*\\]", Pattern.CASE_INSENSITIVE),                  // case 2
        Pattern.compile("position\\([a-zA-Z0-9]+,empty\\)\\[.*\\]", Pattern.CASE_INSENSITIVE),            // case 3
        Pattern.compile("position\\([a-zA-Z0-9]+,\\s?[a-zA-Z0-9]+\\)\\[.*\\]", Pattern.CASE_INSENSITIVE), // case 4
        Pattern.compile("hand_(busy|free)", Pattern.CASE_INSENSITIVE),                                    // case 5
        Pattern.compile("on\\([a-zA-Z0-9]+,\\s?[a-zA-Z0-9]+\\)", Pattern.CASE_INSENSITIVE),               // case 6
    };

    private Message lastTaskReceived = null;
    private Message awaitReply = null;

    /** Default function, except in the case of performative 'achieve': 
     * the agent accepts only tasks from teammate (sender) 
     * */
    @Override
    public boolean socAcc(Message m){
        if(m.isAchieve()){
            Literal s = Literal.parseLiteral("teammate("+m.getSender()+")");
            Literal l = Literal.parseLiteral("leader("+m.getSender()+")");
            if(bb.contains(s) != null || bb.contains(l) != null){
                lastTaskReceived = m;
                return true;
            }
        
            return false;
        }
    
        return super.socAcc(m);
    }

    /** Messages have now a priority attribute setting by default to 0. 
    *   To select messages with higher priority we use a Comparator and a PriorityQueue 
    */
    @Override
    public Message selectMessage(Queue<Message> messages){
        PriorityQueue<Message> ml = new PriorityQueue<>(new MessagePriorityComparator());
        ml.addAll(messages);
        
        Message m = super.selectMessage(ml);
        messages.remove(m);

        return m;
    }

    // @Override
    // public Event selectEvent(Queue<Event> events){
    //     // PriorityQueue<Event> el = new PriorityQueue<>(new EventPriorityComparator());
    //     // el.addAll(events);
        
    //     // Event e = super.selectEvent(el);
    //     // events.remove(e);
    //     Event e = super.selectEvent(events);
    //     Trigger te = e.getTrigger();
    //     logger.info(te.hasSource() ? te.toString() : "null trigger");
    //     Option o = e.getOption();
    //     logger.info(o != null ? o.toString() : "null option");
    //     Intention i = e.getIntention();
    //     logger.info(i != null ? i.toString() : "null intention");
    //     return e;
    // }
    
    /** When an Option is selected, retrieve information about the actual plan 
    *   after invoking the method of the super class 
    */
    @Override
    public Option selectOption(List<Option> options){
        Option o = super.selectOption(options);
       
        if(o != null){
            Plan p = o.getPlan();
            Unifier u = o.getUnifier();
            String la = p.getLabel().getAnnots().getAsList().get(0).toString();
            
            switch (la) {
                case "act":
                   justifyAction(p, u, true);
                   break;
                
                case "act_fail":
                   justifyAction(p, u, false);
                   break;
                
                default:
                   break;
            }
        }
        return o;
    }
    
    /**
    * Method to justify an action, both when it's performed correctly and also when it cannot be performed
    * @param p  "action plan"
    * @param u  Unifier
    * @param isPerformed  flag to indicate the status of the action 
    */
    private void justifyAction(Plan p, Unifier u, boolean isPerformed){
        String act = p.getTrigger().getLiteral().capply(u).toString();
        String precond = (p.getContext() != null) ? p.getContext().capply(u).toString() : null;
        logger.info(convertFormulaTE(act, isPerformed)+" "+convertFormulaCntx(precond));
    }
    
    /**
    * Convert a trigger event of a plan, with is arguments, in a justification
    * @param te trigger event (without the operator)
    * @param isPerformed status of the action
    * @return converted string
    */
    private String convertFormulaTE(String te, boolean isPerformed){
        int i = 0;
        for (Pattern p : patternsTE) {
            if(p.matcher(te).find()){
                te = applyConversionRuleTE(te.trim(), i, isPerformed);
                break;
            }
            i++;
        }
    
        return te.trim();
    }
    
    /**
    * Convert the context in a justification      
    * @param cntx context of a plan
    * @return converted string
    */
    private String convertFormulaCntx(String cntx){
        if(cntx == null)
            return "";
        
        if(cntx.startsWith("("))
            cntx = cntx.substring(1, cntx.length()-1);

        String[] sub = Arrays.stream(cntx.trim().split("\\s*&\\s*")).filter(e -> e.trim().length() > 0).toArray(String[]::new);
        cntx = "because ";
    
        int i = 0;
        for (String s : sub) {
            int j = 0;
            for (Pattern p : patternsCntx) {
                if(p.matcher(s).find()){
                    sub[i] = applyConversionRuleCntx(s.trim(), j, s.contains("not"));
                    break;
                }
                j++;
            }
            cntx += sub[i++]+" and ";
        }
    
        return cntx.substring(0, cntx.lastIndexOf("and")).trim();
    }
    
    /**
    * Apply a specific rule for each pattern (Trigger event)
    * @param s string 
    * @param idxPtrn index of matched pattern
    * @param isPerformed status action
    * @return converted string
    */
    private String applyConversionRuleTE(String s, int idxPtrn, boolean isPerformed){
        switch (idxPtrn) {
            case 0:
                /* Ex: tr.evt = take(Obj)*/
                s = (isPerformed ? "I can " : "I can't ")+s.replace("(", " the ").replace(")",""); 
                break;
                
            case 1:
                /* Ex: tr.evt = put_on(X,Y) OR put_in(Item, Label)*/
                String[] sub = Arrays.stream(s.split("[_(,) ]+")).filter(e -> e.trim().length() > 0).toArray(String[]::new);
                s = (isPerformed ? "I can " : "I can't ")+sub[0]+" the "+sub[2]+" "+sub[1]+(sub[3].trim().length()>1 ? " the "+sub[3] : " "+sub[3].toUpperCase());
                break;
            
            default:
                break;
        }
    
        return s;
    }
    
    /**
    * Apply a specific rule for each pattern (Context)
    * @param s string
    * @param idxPtrn index of matched pattern
    * @param not negation
    * @return converted string
    */
    private String applyConversionRuleCntx(String s, int idxPtrn, boolean not){
        
        if(not)
            s = s.replace("not ", "");
        
        if(s.contains("artifact_"))
            s = s.replace("artifact_id", "").replace("artifact_name", "").replaceFirst("cobj_[0-9]+", "");
        
        String[] sub = Arrays.stream(s.trim().split("[_(),\\[\\]\\s]+")).filter(e -> e.trim().length() > 0).toArray(String[]::new);
        
        switch (idxPtrn) {
            case 0:
                /* Ex: context = is_free(item) */
                s = sub[2]+" "+sub[0]+((not) ? " not " : " ")+sub[1];
                break;
            
            case 1:
                /* Ex: context = too_heavy(item) OR in_hand(item) */
                s = "the "+sub[2]+" is"+((not) ? " not " : " ")+sub[0]+" "+sub[1];
                break;
            
            case 2:
                /* Ex: context = contains(Item)[, art_name] */
                s = "the "+sub[2]+" "+sub[0]+" the "+sub[1];
                break;

            case 3:
                /* Ex: context = position(Label,empty)[, art_name] */
                s = "the position "+sub[1].toUpperCase()+" on "+sub[3]+" is empty";
                break;
            
            case 4:
                /* Ex: context = position(Label,Item)[, art_name] */
                s = "the "+sub[2]+" is in position "+sub[1].toUpperCase()+" on "+sub[3];
                break;
            
            case 5:
                /* Ex: context = hand_busy OR hand_free */
                s = "my "+sub[0]+" is "+(not ? "not " : "")+sub[1];
                break;

            case 6:
                /* Ex: context = on(X,Y) */
                s = sub[1]+" is"+((not) ? " not " : " ")+sub[0]+" the "+sub[2];
                break;

            default:
                break;
        }
        
        return s;
    }

    /*  Bisogna trovare un'alternativa all'invio in broadcast, magari tenendo conto di quali agenti sono considerati teammate e 
        gestendo il caso in cui solo alcuni non sono raggiungibili; altra alternativa mantenere traccia di chi delega un compito*/
    public boolean requestForPlan(Trigger te) throws Exception {
        try {
            awaitReply = new Message("ask-how", ts.getAgArch().getAgName(), null, te.getTriggerAsLiteral());
            ts.getAgArch().broadcast(awaitReply);
            return true;
        } catch (Exception e) {
            //TODO: handle exception
            e.printStackTrace();
            return false;
        }
    }

    private class MessagePriorityComparator implements Comparator<Message>{
        public int compare(Message m1, Message m2){
            return m1.getPriority()-m2.getPriority();
        }
    }

    private class EventPriorityComparator implements Comparator<Event>{
        public int compare(Event e1, Event e2){
            return e1.getPriority()-e2.getPriority();
        }
    }

    // class EventPriorityComparator implements Comparator<Event>{
    //     public int compare(Event e1, Event e2){
    //         int p1 = 1, p2 = 1;

    //         try{
                   // Non ritorna alcuna annotazione, tanto meno la sorgente
    //             p1 = (int) ((NumberTerm) e1.getTrigger().getAnnot("priority").getTerm(0)).solve();
    //         }
    //         catch(Exception e){
    //             e.printStackTrace();
    //         }
    
    //         try{
    //             p2 = (int) ((NumberTerm) e2.getTrigger().getAnnot("priority").getTerm(0)).solve();
    //         }
    //         catch(Exception e){
    //             e.printStackTrace();
    //         }

    //         if(p1 > p2)
    //             return 1;
    //         if(p1 < p2)
    //             return -1;
    //         return 0;
    //     }
    // }
}
