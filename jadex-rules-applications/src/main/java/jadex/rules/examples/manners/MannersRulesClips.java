package jadex.rules.examples.manners;import jadex.rules.parser.conditions.ParserHelper;import jadex.rules.rulesystem.IAction;import jadex.rules.rulesystem.ICondition;import jadex.rules.rulesystem.IRule;import jadex.rules.rulesystem.IVariableAssignments;import jadex.rules.rulesystem.rules.Rule;import jadex.rules.state.IOAVState;
/**
 *  The rules of the manners benchmark using Clips condition language.
 */
public class MannersRulesClips implements IMannersRuleSet
{	//-------- rules --------		/**	 *  Create rule "assign first seat". 	 */	public IRule createAssignFirstSeatRule()	{		//		;;; *****************//		;;; assign_first_seat//		;;; *****************////		(defrule assign_first_seat//		   ?f1 <- (context (state start))//		   (guest (name ?n))//		   ?f3 <- (count (c ?c))//		   =>//		   (assert (seating (seat1 1) (name1 ?n) (name2 ?n) (seat2 1) (id ?c) (pid 0) (path_done yes)))//		   (assert (path (id ?c) (name ?n) (seat 1)))//		   (modify ?f3 (c (+ ?c 1)))//		   (printout ?*output* "seat 1 " ?n " " ?n " 1 " ?c " 0 1" crlf)//		   (modify ?f1 (state assign_seats)))		ICondition	cond	= ParserHelper.parseClipsCondition(				"?f1 <- (context (context_has_state \"start\"))"				+"(guest (guest_has_name ?n))"				+"?f3 <- (count (count_has_c ?c))",				Manners.manners_type_model);				IAction action = new IAction()		{			public void execute(IOAVState state, IVariableAssignments assignments)			{				Object f1 = assignments.getVariableValue("?f1");				Object n = assignments.getVariableValue("?n");				Object f3 = assignments.getVariableValue("?f3");				Integer c = (Integer)assignments.getVariableValue("?c");				System.out.println("Assign first seat: "+f1+" "+n+" "+f3+" "+c);								Object seating = state.createRootObject(Manners.seating_type);				state.setAttributeValue(seating, Manners.seating_has_seat1, new Integer(1));				state.setAttributeValue(seating, Manners.seating_has_name1, n);				state.setAttributeValue(seating, Manners.seating_has_name2, n);				state.setAttributeValue(seating, Manners.seating_has_seat2, new Integer(1));				state.setAttributeValue(seating, Manners.seating_has_id, c);				state.setAttributeValue(seating, Manners.seating_has_pid, new Integer(0));				state.setAttributeValue(seating, Manners.seating_has_pathdone, Boolean.TRUE);								Object path = state.createRootObject(Manners.path_type);				state.setAttributeValue(path, Manners.path_has_id, c);				state.setAttributeValue(path, Manners.path_has_name, n);						state.setAttributeValue(path, Manners.path_has_seat, new Integer(1));								state.setAttributeValue(f3, Manners.count_has_c, new Integer(c.intValue()+1));									System.out.println("seat 1: "+n+" "+n+" 1 "+c+" 0 1");								state.setAttributeValue(f1, Manners.context_has_state, "assign_seats");			}		};				return new Rule("assign first seat", cond, action);	}		/**	 *  Create find_seating rule.	 */	public IRule	createFindSeatingRule()	{//		;;; ************//		;;; find_seating//		;;; ************//		//		(defrule find_seating//		   ?f1 <- (context (state assign_seats))//		   (seating (seat1 ?seat1) (seat2 ?seat2) (name2 ?n2) (id ?id) (pid ?pid) (path_done yes))//		   (guest (name ?n2) (sex ?s1) (hobby ?h1))//		   (guest (name ?g2) (sex ~?s1) (hobby ?h1))//		   ?f5 <- (count (c ?c))//		   (not (path (id ?id) (name ?g2)))//		   (not (chosen (id ?id) (name ?g2) (hobby ?h1)))//		   =>//		   (assert (seating (seat1 ?seat2) (name1 ?n2) (name2 ?g2) (seat2 (+ ?seat2 1)) (id ?c) (pid ?id) (path_done no)))//		   (assert (path (id ?c) (name ?g2) (seat (+ ?seat2 1))))//		   (assert (chosen (id ?id) (name ?g2) (hobby ?h1)))//		   (modify ?f5 (c (+ ?c 1)))//		   (printout ?*output* seat " " ?seat2 " " ?n2 " " ?g2 crlf)//		   (modify ?f1 (state make_path)))				ICondition	cond	= ParserHelper.parseClipsCondition(				"?f1 <- (context (context_has_state \"assign_seats\"))"				+"(seating (seating_has_seat1 ?seat1) (seating_has_seat2 ?seat2) (seating_has_name2 ?n2) (seating_has_id ?id) (seating_has_pid ?pid) (seating_has_pathdone true))"				+"(guest (guest_has_name ?n2) (guest_has_sex ?s1) (guest_has_hobby ?h1))"				+"(guest (guest_has_name ?g2) (guest_has_sex ~?s1) (guest_has_hobby ?h1))"				+"?f5 <- (count (count_has_c ?c))"				+"(not (path (path_has_id ?id) (path_has_name ?g2)))"				+"(not (chosen (chosen_has_id ?id) (chosen_has_name ?g2) (chosen_has_hobby ?h1)))",				Manners.manners_type_model);		IAction fs_action = new IAction()		{			public void execute(IOAVState state, IVariableAssignments assigments)			{				Integer	seat2	= (Integer)assigments.getVariableValue("?seat2");				String	n2	= (String)assigments.getVariableValue("?n2");				String	g2	= (String)assigments.getVariableValue("?g2");				Integer	c	= (Integer)assigments.getVariableValue("?c");				Integer	id	= (Integer)assigments.getVariableValue("?id");				Integer	pid	= (Integer)assigments.getVariableValue("?pid");				String	h1	= (String)assigments.getVariableValue("?h1");				Object	f1	= assigments.getVariableValue("?f1");				Object	f5	= assigments.getVariableValue("?f5");								Object	seating	= state.createRootObject(Manners.seating_type);				state.setAttributeValue(seating, Manners.seating_has_seat1, seat2);				state.setAttributeValue(seating, Manners.seating_has_name1, n2);				state.setAttributeValue(seating, Manners.seating_has_name2, g2);				state.setAttributeValue(seating, Manners.seating_has_seat2, new Integer(seat2.intValue()+1));				state.setAttributeValue(seating, Manners.seating_has_id, c);				state.setAttributeValue(seating, Manners.seating_has_pid, id);								Object	path	= state.createRootObject(Manners.path_type);				state.setAttributeValue(path, Manners.path_has_id, c);				state.setAttributeValue(path, Manners.path_has_name, g2);				state.setAttributeValue(path, Manners.path_has_seat, new Integer(seat2.intValue()+1));								Object	chosen	= state.createRootObject(Manners.chosen_type);				state.setAttributeValue(chosen, Manners.chosen_has_id, id);				state.setAttributeValue(chosen, Manners.chosen_has_name, g2);				state.setAttributeValue(chosen, Manners.chosen_has_hobby, h1);								state.setAttributeValue(f5, Manners.count_has_c, new Integer(c.intValue()+1));								if(Manners.print)					System.out.println("find seating: seat2="+seat2+", n2="+n2+", g2="+g2+", pid="+pid);								state.setAttributeValue(f1, Manners.context_has_state, "make_path");			}		};				return new Rule("find seating", cond, fs_action);	}		/**	 *  Create rule "make path". 	 */	public IRule createMakePathRule()	{//		;;; *********//		;;; make_path//		;;; *********////		(defrule make_path//		   (context (state make_path))//		   (seating (id ?id) (pid ?pid) (path_done no))//		   (path (id ?pid) (name ?n1) (seat ?s))//		   (not (path (id ?id) (name ?n1)))//		   =>//		   (assert (path (id ?id) (name ?n1) (seat ?s))))		ICondition	cond	= ParserHelper.parseClipsCondition(				"(context (context_has_state \"make_path\"))"				+"(seating (seating_has_id ?id) (seating_has_pid ?pid) (seating_has_pathdone false))"				+"(path (path_has_id ?pid) (path_has_name ?n1) (path_has_seat ?s))"				+"(not (path (path_has_id ?id) (path_has_name ?n1)))",				Manners.manners_type_model);				IAction action = new IAction()		{			public void execute(IOAVState state, IVariableAssignments assignments)			{				Integer	id	= (Integer)assignments.getVariableValue("?id");				String	n1	= (String)assignments.getVariableValue("?n1");				Integer	s	= (Integer)assignments.getVariableValue("?s");								if(Manners.print)					System.out.println("Make path: "+id+" "+n1+" "+s);								Object	path	= state.createRootObject(Manners.path_type);				state.setAttributeValue(path, Manners.path_has_id, id);				state.setAttributeValue(path, Manners.path_has_name, n1);				state.setAttributeValue(path, Manners.path_has_seat, s);			}		};				return new Rule("make path", cond, action);	}		/**	 *  Create rule "path done". 	 */	public IRule createPathDoneRule()	{//		;;; *********//		;;; path_done//		;;; *********////		(defrule path_done//		   ?f1 <- (context (state make_path))//		   ?f2 <- (seating (path_done no))//		   =>//		   (modify ?f2 (path_done yes))//		   (modify ?f1 (state check_done)))		 		ICondition	cond	= ParserHelper.parseClipsCondition(				"?f1 <- (context (context_has_state \"make_path\"))"				+"?f2 <- (seating (seating_has_pathdone false))",				Manners.manners_type_model);				IAction action = new IAction()		{			public void execute(IOAVState state, IVariableAssignments assignments)			{				Object f1 = assignments.getVariableValue("?f1");				Object f2 = assignments.getVariableValue("?f2");								if(Manners.print)					System.out.println("Path done: "+f1+" "+f2);								state.setAttributeValue(f2, Manners.seating_has_pathdone, Boolean.TRUE);				state.setAttributeValue(f1, Manners.context_has_state, "check_done");			}		};				return new Rule("path done", cond, action);	}		/**	 *  Create rule "we are done". 	 */	public IRule createAreWeDoneRule()	{//		;;; ***********//		;;; are_we_done//		;;; ***********////		(defrule are_we_done//		   ?f1 <- (context (state check_done))//		   (last_seat (seat ?l_seat))//		   (seating (seat2 ?l_seat))//		   =>//		   (printout ?*output* crlf "Yes, we are done!!" crlf)//		   (modify ?f1 (state print_results)))				ICondition	cond	= ParserHelper.parseClipsCondition(				"?f1 <- (context (context_has_state \"check_done\"))"				+"(lastseat (lastseat_has_seat ?l_seat))"				+"(seating (seating_has_seat2 ?l_seat))",				Manners.manners_type_model);				IAction action = new IAction()		{			public void execute(IOAVState state, IVariableAssignments assignments)			{				Object f1 = assignments.getVariableValue("?f1");				System.out.println("Yes, we are done!! "+f1);								state.setAttributeValue(f1, Manners.context_has_state, "print_results");			}		};				return new Rule("are we done", cond, action);	}		/**	 *  Create rule "continue". 	 */	public IRule createContinueRule()	{//		;;; ********//		;;; continue//		;;; ********////		(defrule continue//		   ?f1 <- (context (state check_done))//		   =>//		   (modify ?f1 (state assign_seats)))		 		ICondition	c	= ParserHelper.parseClipsCondition(			"?f1 <- (context (context_has_state \"check_done\"))",			Manners.manners_type_model);				IAction action = new IAction()		{			public void execute(IOAVState state, IVariableAssignments assignments)			{				Object f1 = assignments.getVariableValue("?f1");				if(Manners.print)					System.out.println("Continue: "+f1);								state.setAttributeValue(f1, Manners.context_has_state, "assign_seats");			}		};				return new Rule("continue", c, action);			}		/**	 *  Create rule "print results". 	 */	public IRule createPrintResultsRule()	{//		;;; *************//		;;; print_results//		;;; *************////		(defrule print_results//		   (context (state print_results))//		   (seating (id ?id) (seat2 ?s2))//		   (last_seat (seat ?s2))//		   ?f4 <- (path (id ?id) (name ?n) (seat ?s))//		   =>//		   (retract ?f4)//		   (printout ?*output* ?n " " ?s crlf))		ICondition	cond	= ParserHelper.parseClipsCondition(			"(context (context_has_state \"print_results\"))"			+"(seating (seating_has_id ?id) (seating_has_seat2 ?s2))"			+"(lastseat (lastseat_has_seat ?s2))"			+"?f4 <- (path (path_has_id ?id) (path_has_name ?n) (path_has_seat ?s))",			Manners.manners_type_model);		IAction action = new IAction()		{			public void execute(IOAVState state, IVariableAssignments assignments)			{				Object f4 = assignments.getVariableValue("?f4");				Object n = assignments.getVariableValue("?n");				Object s = assignments.getVariableValue("?s");				System.out.println("Result: guest="+n+" seat="+s);								state.dropObject(f4);			}		};				return new Rule("print results", cond, action);	}			/**	 *  Create rule "all done".	 */	public IRule createAllDoneRule()	{//		;;; ********//		;;; all_done//		;;; ********////		(defrule all_done//		   (context (state print_results))//		   =>//		   (halt))				ICondition	ad	= ParserHelper.parseClipsCondition(			"(context (context_has_state \"print_results\"))",			Manners.manners_type_model);				IAction action = new IAction()		{			public void execute(IOAVState state, IVariableAssignments assignments)			{				System.out.println("TERMINATED!!!");			}		};				return new Rule("all done", ad, action);	}}
