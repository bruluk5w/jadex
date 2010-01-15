package jadex.bdi.simulation.client;

import jadex.bdi.runtime.Plan;
import jadex.bdi.simulation.helper.Constants;
import jadex.bdi.simulation.model.Observer;

import java.util.ArrayList;

public class InitOnChangeObserversPlan extends Plan {

	@Override
	public void body() {
		// TODO Auto-generated method stub
		ArrayList observers = (ArrayList) getParameter(Constants.ON_CHANGE_OBSERVER_LIST).getValue();
		
		System.out.println("#InitOnChangeObserversPlan# Elementname: "  + ((Observer)observers.get(0)).getData().getName());
	}

}
