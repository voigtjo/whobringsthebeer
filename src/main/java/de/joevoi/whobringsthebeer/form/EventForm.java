package de.joevoi.whobringsthebeer.form;

import java.util.Date;

public class EventForm {

    private String location;
    private Date eventDate;


    public EventForm(String location, Date eventDate) {
        this.location = location;
        this.eventDate = eventDate == null ? null : new Date(eventDate.getTime());
    }

	public String getLocation() {
		return location;
	}

	public Date getEventDate() {
		return eventDate;
	}

    
    
}
