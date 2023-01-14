package table;

import cartago.*;
import jason.asSyntax.Atom;
import java.util.Collections;
import java.util.Arrays;
import java.util.ArrayList;

public class BlackBoard extends Artifact {
  private boolean free = true;
  private int n_task = 0;
  // Change ArrayList with Set if want to have only one specific task in the list
  private ArrayList<String> todo = new ArrayList<>();
  private ArrayList<String> completed = new ArrayList<>();

  void init() {
    defineObsProperty("label", new Atom("fork"), new Atom("f"));
    defineObsProperty("label", new Atom("dish"), new Atom("d"));
    defineObsProperty("label", new Atom("knife"), new Atom("k"));
    defineObsProperty("label", new Atom("glass"), new Atom("g"));

    todo.add("fork");
    todo.add("dish");
    todo.add("knife");
    todo.add("glass");

    n_task = 4;
  }

  void init(String it, String l) throws CartagoException {
    init(it,l,"all");
  }
  /* Like Constructor: takes the list passed from .jcm file as single string.
     After split them, define the Observable properties relative to items (it), labels (l) and position (p) */
  void init(String it, String l, String p) throws CartagoException {
    String[] labels = l.substring(1,l.length()-1).split("\\s*,\\s*");
		String[] items = it.substring(1,it.length()-1).split("\\s*,\\s*");
    int[] pos;
    int n = labels.length;

    if(n != items.length){
      throw new CartagoException();
    }

    for(int i = 0; i < n; ++i){
      defineObsProperty("label", new Atom(items[i]), new Atom(labels[i]));
    }

    if (!(p.equals("all") || p.equals("none"))){
      pos = Arrays.stream(p.substring(1,p.length()-1).split("\\s*,\\s*"))
                  .filter(e -> e.trim().length() > 0).mapToInt(Integer::parseInt).toArray();

      for(int idx : pos) {
        todo.add(items[idx-1]);
      }

      n_task = pos.length;
    }
    else if(p.equals("all")){
      Collections.addAll(todo, items);
      n_task = n;
    }
  }

  private boolean checkTasksCompleted(){
    return n_task == completed.size();
  }

  @OPERATION void allTasksCompleted(OpFeedbackParam res){
    res.set(checkTasksCompleted());
  }

  // Remove controls when using the Set class or in the case of multiple identical tasks
  @OPERATION void taskCompleted(String item){
    item = item.trim();

    if(!completed.contains(item))
      completed.add(item);

    if(checkTasksCompleted()){
      signal("all_tasks_completed");
    }
  }

  @OPERATION void taskFailed(String item){
    item = item.trim();

    if(!todo.contains(item))
      todo.add(item);
  }

  @GUARD boolean isFree(OpFeedbackParam<Atom> task){
    return free;
  }
  @OPERATION(guard="isFree") void readTask(OpFeedbackParam<Atom> task){
    free = false;
    log("OK");
    if(todo.isEmpty()) {
      // Alternative: use failed() primitive with meaningful error msg
      signal("all_tasks_completed");
      task.set(new Atom("none"));
    }
    else
      task.set(new Atom(todo.remove(0)));

    free = true;
  }
}
