package de.joevoi.whobringsthebeer.form;

import java.util.Date;

public class InvitationStructureForm {
	private String websafeEventKey;
    private Date invitationDate;

    

    public InvitationStructureForm() {
		super();
	}

	public InvitationStructureForm(String websafeEventKey, Date invitationDate) {
    	this.websafeEventKey = websafeEventKey;
        this.invitationDate = invitationDate == null ? null : new Date(invitationDate.getTime());
    }
    
	public String getWebsafeEventKey() {
		return websafeEventKey;
	}

	public Date getInvitationDate() {
		return invitationDate;
	}


    
}
