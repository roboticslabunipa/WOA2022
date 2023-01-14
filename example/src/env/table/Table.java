package table;

import cartago.*;
import jason.asSyntax.Atom;
import java.util.Arrays;
import java.util.List;

public class Table extends Artifact {
	void init() {
		defineObsProperty("position", new Atom("f"), new Atom("empty"));
		defineObsProperty("position", new Atom("d"), new Atom("empty"));
		defineObsProperty("position", new Atom("k"), new Atom("empty"));
		defineObsProperty("position", new Atom("g"), new Atom("empty"));
	}

	void init(String l) throws CartagoException {
		init(l, new String());
	}
	/* Like Constructor: takes the list passed from .jcm file as single string.
	   After split them, define the Observable properties relative to labels (l) and items (it) */
	void init(String l, String it) throws CartagoException {
		String[] labels = l.substring(1,l.length()-1).split("\\s*,\\s*");
		int n = labels.length, m = 0, i = 0;

		if(n == 0)
			throw new CartagoException();

		if(!it.equals("")) {
			String[] items = it.substring(1,it.length()-1).split("\\s*,\\s*");
			m = items.length;

			if(m > n)
				throw new CartagoException();

			while(i < m) {
				defineObsProperty("position",new Atom(labels[i]),new Atom(items[i]));
				++i;
			}
		}

		while(i < n) {
			defineObsProperty("position",new Atom(labels[i]),new Atom("empty"));
			++i;
		}
	}

	/* await_time function is used to simulate the progress of the actions */
	@OPERATION void take_from(String artName, String label){
		artName = artName.trim();
		label = label.trim();

		if (hasObsPropertyByTemplate("position", new Atom(label), new Atom("empty"))) {
			log("This position is empty!");
		}
		else if (hasObsPropertyByTemplate("position", new Atom(label), new Atom(artName))) {
			log(getCurrentOpAgentId().getAgentName()+" took the "+artName+" from "+label.toUpperCase());

			await_time(1000);

			ObsProperty obprop = getObsPropertyByTemplate("position", new Atom(label), new Atom(artName));
			obprop.updateValue(1,new Atom("empty"));
		}
		else{
			log("The "+artName+" is not in this position!");
		}
	}

	@OPERATION void put_in(String artName, String label){
		artName = artName.trim();
		label = label.trim();

		if (hasObsPropertyByTemplate("position", new Atom(label), new Atom("empty"))) {
			ObsProperty obprop = getObsPropertyByTemplate("position", new Atom(label), new Atom("empty"));
			obprop.updateValue(1,new Atom(artName));

			await_time(1000);

			log(getCurrentOpAgentId().getAgentName()+" put the "+artName+" in "+label.toUpperCase());
		}
		else{
			log(getCurrentOpAgentId().getAgentName()+": position already busy!");
		}
	}

	@OPERATION void swap(String artName1, String label1, String artName2, String label2){
		artName1 = artName1.trim();
		label1 = label1.trim();
		artName2 = artName2.trim();
		label2 = label2.trim();

		if(hasObsPropertyByTemplate("position", new Atom(label1), new Atom(artName2))) {
			ObsProperty obprop2 = getObsPropertyByTemplate("position", new Atom(label2),
															hasObsPropertyByTemplate("position", new Atom(label2), new Atom(artName1)) ?
															new Atom(artName1) : new Atom("empty")
														);
			if (obprop2 != null) {
				ObsProperty obprop1 = getObsPropertyByTemplate("position", new Atom(label1), new Atom(artName2));
				obprop1.updateValue(1,new Atom(artName1));

				obprop2.updateValue(1,new Atom(artName2));

				await_time(1000);

				log(getCurrentOpAgentId().getAgentName()+" swap the "+artName1+" with the "+artName2+" from "+label2.toUpperCase()+" to "+label1.toUpperCase());
			}
			else{
				log(getCurrentOpAgentId().getAgentName()+": the item "+artName2+" is not in position "+label1.toUpperCase()+"!");
			}
		}
		else{
			log(getCurrentOpAgentId().getAgentName()+": the item "+artName1+" is not in the indicated position!");
		}
	}
}
