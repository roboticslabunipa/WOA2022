/* Initial beliefs and rules */

correct_position(Item, Label) :-
  label(Item, CorrectLabel) &
  Label == CorrectLabel.

hand_busy :-
  .count(in_hand(Item)[inn_sp], N) &
  N == 2.

hand_free :-
  .count(in_hand(Item)[inn_sp], N) &
  N == 0.

/* Initial goals */

!start.

/* Plans */

+!start : .my_name(Me) & leader(Me)
        <- .print("Hello, my name is ",Me,"! I am the leader.");
           .wait(3000);
           .send(nao, achieve, lay_the_table);
           !lay_the_table.

+!start : .my_name(Me) <- .print("Hello, my name is ",Me,"! I'm ready.").

// Main goal: the agents read one task at a time from the board until these are finished
@lt[main]
+!lay_the_table : not tasks_completed
        <- readTask(Item);
           if(Item \== none) {
            !place(Item);
           }
           !lay_the_table.

/* Only the leader is responsible for checking that the objects are in the correct positions and, 
   if necessary, putting them back in order */
@lt1[main]
+!lay_the_table : .my_name(Me) & leader(Me) & tasks_completed
        <- !check_all_position;
           .print("Goal completed! All items are placed correctly on the table.").

// To wait for the end of all tasks
@lt2[main]
+!lay_the_table : .my_name(Me) & leader(Me) & not tasks_completed
        <- .wait(2500);
           !lay_the_table.

// Required for agents who are not the leader to stop recursion
@lt3[main]
+!lay_the_table : tasks_completed <- .print("Finish!").


// Sub goal: useful for decomposing the main objective in smaller/simpler tasks
@pl[sub]
+!place(Item) : label(Item, Label)
        <- !take(Item);
           !put_in(Item,Label);
           taskCompleted(Item).

// In case of failure it places the task back on the board so that another agent can take care of it eventually
@pl1[fail]
-!place(Item) <- taskFailed(Item).


// Plans for leader: fix the wrong position of object, if anyone is out of position
@cap[sub]
+!check_all_position : position(Label, Item) & not correct_position(Item,Label)
        <- !take_from(Item,Label);
           !fix_position(Item);
           !check_all_position.

// Do nothing if all objects are correctly positioned
@cap1[sub]
+!check_all_position.


// Assumes that the agent has already taken the item that was in the wrong position
@fp[alt]
+!fix_position(Item) : label(Item,CorrectLabel) & position(CorrectLabel,empty) <- !put_in(Item,CorrectLabel).

/* This plan provides that the two items are one in the position where the other should be:
   however, since the agent has already taken the item for which he had to correct the position,
   the old position will be empty.
   The empty position may not even be the old position of the item,
   but the position left empty by previous adjustments */
@fp1[alt]
+!fix_position(Item) : label(Item,CorrectLabel) & position(CorrectLabel,OtherItem) &
                       label(OtherItem,OtherLabel) & position(OtherLabel,empty)
        <- !swap(Item,CorrectLabel,OtherItem,OtherLabel).

// Recursive case where more than one object are in wrong position
@fp2[alt]
+!fix_position(Item) : label(Item,CorrectLabel) & position(CorrectLabel,OtherItem) &
                       label(OtherItem,OtherLabel) & not position(OtherLabel,empty)
        <- !take_from(OtherItem,CorrectLabel);
           !put_in(Item,CorrectLabel);
           !fix_position(OtherItem).

@fp3[alt]
+!fix_position(empty).

/* Simpler plans: indicate a single ATOMIC ACTION and the context in which it can be performed */

@t[atomic,act]
+!take(Item) : contains(Item)[artifact_id(Id),artifact_name(Art)] & not hand_busy
        <- take(Item)[artifact_id(Id)]; .send(self,tell,in_hand(Item)[inn_sp]).

@tf[atomic,act]
+!take_from(Item,Label) : position(Label,Item)[artifact_id(Id),artifact_name(Art)] & not hand_busy
        <- take_from(Item,Label)[artifact_id(Id)]; .send(self,tell,in_hand(Item)[inn_sp]).


@p[atomic,act]
+!put(Item) : label(Item, Label) & position(Label,empty)[artifact_id(Id),artifact_name(Art)]
        <- put_in(Item,Label)[artifact_id(Id)]; .send(self,untell,in_hand(Item)[inn_sp]).

@pi[atomic,act]
+!put_in(Item,Label) : position(Label,empty)[artifact_id(Id),artifact_name(Art)]
        <- put_in(Item,Label)[artifact_id(Id)]; .send(self,untell,in_hand(Item)[inn_sp]).


// @s1 denotes the case where the agent swaps objects that are both on the table
@s1[atomic,act]
+!swap(Item1,Label1,Item2,Label2) : position(Label1,Item2)[artifact_id(Id),artifact_name(Art)] &
                                    position(Label2,Item1)[artifact_id(Id),artifact_name(Art)] &
                                    hand_free
        <- swap(Item1,Label1,Item2,Label2)[artifact_id(Id)].

// @s2 indicates the exchange of the two objects in case the former has already been taken by the agent
@s2[atomic,act]
+!swap(Item1,Label1,Item2,Label2) : position(Label1,Item2)[artifact_id(Id),artifact_name(Art)] &
                                    position(Label2,empty)[artifact_id(Id),artifact_name(Art)] & 
                                    not hand_busy
        <- swap(Item1,Label1,Item2,Label2)[artifact_id(Id)]; .send(self,untell,in_hand(Item1)[inn_sp]).


/* Artifact signal handler with speech act to update own belief base */

+all_tasks_completed[artifact_name(Art)] <- .send(self,tell,tasks_completed[source(Art)]).

+new_task[artifact_name(Art)] : tasks_completed <- .send(self,untell,tasks_completed[source(Art)]).

/* Utility plans */

+?discover(ArtName, ArtId) <- lookupArtifact(ArtName, ArtId).
-?discover(ArtName, ArtId) <- .wait(100); ?discover(ArtName, ArtId).

{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }
