package table;

import cartago.*;
import jason.asSyntax.Atom;
import java.util.HashMap;

public class Cabinet extends Artifact {
	private HashMap<String,Integer> qnt = new HashMap<>();

	void init() {
		defineObsProperty("contains", new Atom("fork"));
		defineObsProperty("contains", new Atom("dish"));
		defineObsProperty("contains", new Atom("knife"));
		defineObsProperty("contains", new Atom("glass"));

		qnt.put("fork",1);
		qnt.put("dish",1);
		qnt.put("knife",1);
		qnt.put("glass",1);
	}

	void init(String it) {
		init(it,new String());
	}
	/* Like Constructor: takes the list passed from .jcm file as single string.
	   After split them, define the Observable properties relative to items (it) and the relative quantities (q) */
	void init(String it, String q) {
		String[] items = it.substring(1,it.length()-1).split("\\s*,\\s*");
		int i = 0, n = 0, m = items.length;

		if(!q.equals("")){
			String[] qt = q.substring(1,q.length()-1).split("\\s*,\\s*");
			n = qt.length < m ? qt.length : m;

			while(i < n){
				String key = items[i];
				defineObsProperty("contains",new Atom(key));
				qnt.put(key,Integer.parseInt(qt[i]));
				++i;
			}
		}

		while(i < m) {
			String key = items[i];
			defineObsProperty("contains",new Atom(key));
			qnt.put(key,1);
			++i;
		}
	}

	/* await_time function is used to simulate the progress of the actions */
	@OPERATION void take(String artName) {
		artName = artName.trim();

		if (!hasObsPropertyByTemplate("contains", new Atom(artName))) {
			log(artName+" not found!");
		}
		else{
			int itemLeft = qnt.get(artName);

			if(itemLeft == 1)
				removeObsPropertyByTemplate("contains", new Atom(artName));

			qnt.put(artName,itemLeft-1);

			await_time(1000);

			log(getCurrentOpAgentId().getAgentName()+" took the "+artName);
		}
	}

	@OPERATION void put(String artName) {
		artName = artName.trim();

		if (!hasObsPropertyByTemplate("contains", new Atom(artName)))
			defineObsProperty("contains", new Atom(artName));

		qnt.put(artName, qnt.getOrDefault(artName,0)+1);

		await_time(1000);

		log(getCurrentOpAgentId().getAgentName()+" put the "+artName);
	}

	@OPERATION void count(String artName,OpFeedbackParam<Integer> op) {
		artName = artName.trim();
		op.set(qnt.getOrDefault(artName,0));
	}

	/*	Alternatives for updating the value for a key:
			1) qnt.computeIfPresent(String key, (k, v) -> fun(v)),
				 where the second param is a BiFunction<K,V,V> that accepts two inputs and returns a result
			2) qnt.merge(String key, Integer value, Integer::func), where the second param is the value used by the remapping func
			   while the third param is the BiFunction (es: Integer::sum) */
}
