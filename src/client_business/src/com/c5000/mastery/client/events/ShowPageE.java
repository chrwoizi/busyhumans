package com.c5000.mastery.client.events;

import com.c5000.mastery.client.events.MasteryEvents.MasteryEvent;
import com.google.gwt.user.client.ui.Composite;

public class ShowPageE implements MasteryEvent {
	public Composite page;

	public ShowPageE(Composite page) {
		this.page = page;
	}
}