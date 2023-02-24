package com.c5000.mastery.client.events;

import com.c5000.mastery.client.events.MasteryEvents.MasteryEvent;
import com.google.gwt.user.client.ui.Composite;

public class NavigationE implements MasteryEvent {
	public String token;

	public NavigationE(String token) {
		this.token = token;
	}
}