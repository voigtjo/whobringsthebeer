package de.joevoi.whobringsthebeer.form;

import java.util.Date;

public class EventStructureForm {
	private String websafeGroupKey;
    private String location;
    private Date eventDate;

    

    public EventStructureForm() {
		super();
	}

	public EventStructureForm(String websafeGroupKey, String location, Date eventDate) {
    	this.websafeGroupKey = websafeGroupKey;
        this.location = location;
        this.eventDate = eventDate == null ? null : new Date(eventDate.getTime());
    }
    
	public String getWebsafeGroupKey() {
		return websafeGroupKey;
	}

	public String getLocation() {
		return location;
	}

	public Date getEventDate() {
		return eventDate;
	}

	@Override
	public String toString() {
		return "EventStructureForm [websafeGroupKey=" + websafeGroupKey + ", location=" + location + ", eventDate="
				+ eventDate + "]";
	}

	
    
    
}
