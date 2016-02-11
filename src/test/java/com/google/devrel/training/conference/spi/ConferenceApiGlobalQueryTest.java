package com.google.devrel.training.conference.spi;

import static de.joevoi.whobringsthebeer.service.OfyService.ofy;
import static org.junit.Assert.*;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.common.collect.ImmutableList;

import de.joevoi.whobringsthebeer.domain.Event;
import de.joevoi.whobringsthebeer.form.EventForm;
import de.joevoi.whobringsthebeer.form.EventQueryForm;
import de.joevoi.whobringsthebeer.spi.EventApi;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Tests for ConferenceApi API methods.
 */
public class ConferenceApiGlobalQueryTest {

    private static final String USER_ID = "123456789";

    private static final String NAME1 = "GCP Live";

    private static final String NAME2 = "Google I/O";

    private static final String NAME3 = "GCP Roadshow";

    private static final String DESCRIPTION1 = "New announcements for Google Cloud Platform";

    private static final String DESCRIPTION2 = "Google's annual developer event.";

    private static final String DESCRIPTION3 = "World tour for Google Cloud Platform";

    private static final String CITY1 = "Mountain View";

    private static final String CITY2 = "San Francisco";

    private static final String CITY3 = "Tokyo";

    private static final List<String> TOPICS1 = ImmutableList.of("Cloud", "Platform");

    private static final List<String> TOPICS2 = ImmutableList.of("Developer", "Platform");

    private static final List<String> TOPICS3 = ImmutableList.of("Cloud", "Platform", "Japan");

    private static final int CAP1 = 500;

    private static final int CAP2 = 1000;

    private static final int CAP3 = 1500;

    private Date startDate1;

    private Date startDate2;

    private Date startDate3;

    private Date endDate1;

    private Date endDate2;

    private Date endDate3;

    private EventApi conferenceApi;

    /**
     * The helper here is intentionally use 0 for the percentage, since we test our global queries.
     */
    private final LocalServiceTestHelper helper =
            new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig()
                    .setDefaultHighRepJobPolicyUnappliedJobPercentage(0));

    private Event conference1;

    private Event conference2;

    private Event conference3;

    @Before
    public void setUp() throws Exception {
        helper.setUp();
        conferenceApi = new EventApi();
        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");

        // Create 3 Conferences.
        startDate1 = dateFormat.parse("03/25/2014");
        endDate1 = dateFormat.parse("03/26/2014");
        EventForm conferenceForm1 = new EventForm(
                NAME1, DESCRIPTION1, TOPICS1, CITY1, startDate1, endDate1, CAP1);
        conference1 = new Event(1001L, USER_ID, conferenceForm1);

        startDate2 = dateFormat.parse("06/25/2014");
        endDate2 = dateFormat.parse("06/26/2014");
        EventForm conferenceForm2 = new EventForm(
                NAME2, DESCRIPTION2, TOPICS2, CITY2, startDate2, endDate2, CAP2);
        conference2 = new Event(1002L, USER_ID, conferenceForm2);

        startDate3 = dateFormat.parse("09/25/2014");
        endDate3 = dateFormat.parse("09/26/2014");
        EventForm conferenceForm3 = new EventForm(
                NAME3, DESCRIPTION3, TOPICS3, CITY3, startDate3, endDate3, CAP3);
        conference3 = new Event(1003L, USER_ID, conferenceForm3);
        ofy().save().entities(conference1, conference2, conference3).now();
    }

    @After
    public void tearDown() throws Exception {
        ofy().clear();
        helper.tearDown();
    }

    @Test
    public void testEmptyQuery() throws Exception {
        // Empty query.
        EventQueryForm conferenceQueryForm = new EventQueryForm();
        List<Event> conferences = conferenceApi.queryConferences(conferenceQueryForm);
        assertEquals(3, conferences.size());
        assertTrue("The result should contain conference1.", conferences.contains(conference1));
        assertTrue("The result should contain conference2.", conferences.contains(conference2));
        assertTrue("The result should contain conference3.", conferences.contains(conference3));
        assertEquals(conference1, conferences.get(0));
        assertEquals(conference3, conferences.get(1));
        assertEquals(conference2, conferences.get(2));
    }

    @Test
    public void testCityQuery() throws Exception {
        // A query only specifies the city.
        EventQueryForm conferenceQueryForm = new EventQueryForm()
                .filter(new EventQueryForm.Filter(
                        EventQueryForm.Field.CITY,
                        EventQueryForm.Operator.EQ,
                        "Tokyo"
                ));
        List<Event> conferences = conferenceApi.queryConferences(conferenceQueryForm);
        assertEquals(1, conferences.size());
        assertTrue("The result should contain conference3.", conferences.contains(conference3));
    }

    @Test
    public void testTopicsQuery() throws Exception {
        // A query only specifies the topics.
        EventQueryForm conferenceQueryForm = new EventQueryForm()
                .filter(new EventQueryForm.Filter(
                        EventQueryForm.Field.TOPIC,
                        EventQueryForm.Operator.EQ,
                        "Japan"
                ));
        List<Event> conferences = conferenceApi.queryConferences(conferenceQueryForm);
        assertEquals(1, conferences.size());
        assertTrue("The result should contain conference3.", conferences.contains(conference3));
    }

    @Test
    public void testComplexQuery() throws Exception {
        // A query specifies the city and topics and month.
        EventQueryForm conferenceQueryForm = new EventQueryForm()
                .filter(new EventQueryForm.Filter(
                        EventQueryForm.Field.TOPIC,
                        EventQueryForm.Operator.EQ,
                        "Platform"
                ))
                .filter(new EventQueryForm.Filter(
                        EventQueryForm.Field.CITY,
                        EventQueryForm.Operator.EQ,
                        "San Francisco"
                ))
                .filter(new EventQueryForm.Filter(
                        EventQueryForm.Field.MONTH,
                        EventQueryForm.Operator.EQ,
                        "6"
                ));
        List<Event> conferences = conferenceApi.queryConferences(conferenceQueryForm);
        assertEquals(1, conferences.size());
        assertTrue("The result should contain conference2.", conferences.contains(conference2));
    }

    @Test
    public void testMaxAttendeesGT() throws Exception {
        // A query specifies the maxAttendees > 999.
        EventQueryForm conferenceQueryForm = new EventQueryForm()
                .filter(new EventQueryForm.Filter(
                        EventQueryForm.Field.MAX_ATTENDEES,
                        EventQueryForm.Operator.GT,
                        "999"
                ));
        List<Event> conferences = conferenceApi.queryConferences(conferenceQueryForm);
        assertEquals(2, conferences.size());
        assertTrue("The result should contain conference2.", conferences.contains(conference2));
        assertTrue("The result should contain conference3.", conferences.contains(conference3));
        assertEquals(conference2, conferences.get(0));
        assertEquals(conference3, conferences.get(1));
    }

    @Test
    public void testMaxAttendeesLT() throws Exception {
        // A query specifies the maxAttendees > 1001.
        EventQueryForm conferenceQueryForm = new EventQueryForm()
                .filter(new EventQueryForm.Filter(
                        EventQueryForm.Field.MAX_ATTENDEES,
                        EventQueryForm.Operator.LT,
                        "1001"
                ));
        List<Event> conferences = conferenceApi.queryConferences(conferenceQueryForm);
        assertEquals(2, conferences.size());
        assertTrue("The result should contain conference1.", conferences.contains(conference1));
        assertTrue("The result should contain conference2.", conferences.contains(conference2));
        assertEquals(conference1, conferences.get(0));
        assertEquals(conference2, conferences.get(1));
    }

    @Test
    public void testMaxAttendeesGTEQ() throws Exception {
        // A query specifies the maxAttendees >= 1000.
        EventQueryForm conferenceQueryForm = new EventQueryForm()
                .filter(new EventQueryForm.Filter(
                        EventQueryForm.Field.MAX_ATTENDEES,
                        EventQueryForm.Operator.GTEQ,
                        "1000"
                ));
        List<Event> conferences = conferenceApi.queryConferences(conferenceQueryForm);
        assertEquals(2, conferences.size());
        assertTrue("The result should contain conference2.", conferences.contains(conference2));
        assertTrue("The result should contain conference3.", conferences.contains(conference3));
        assertEquals(conference2, conferences.get(0));
        assertEquals(conference3, conferences.get(1));
    }

    @Test
    public void testMaxAttendeesLTEQ() throws Exception {
        // A query specifies the maxAttendees <= 1000.
        EventQueryForm conferenceQueryForm = new EventQueryForm()
                .filter(new EventQueryForm.Filter(
                        EventQueryForm.Field.MAX_ATTENDEES,
                        EventQueryForm.Operator.LTEQ,
                        "1000"
                ));
        List<Event> conferences = conferenceApi.queryConferences(conferenceQueryForm);
        assertEquals(2, conferences.size());
        assertTrue("The result should contain conference1.", conferences.contains(conference1));
        assertTrue("The result should contain conference2.", conferences.contains(conference2));
        assertEquals(conference1, conferences.get(0));
        assertEquals(conference2, conferences.get(1));
    }

    @Test
    public void testMaxAttendeesNE() throws Exception {
        // A query specifies the maxAttendees != 1000.
        EventQueryForm conferenceQueryForm = new EventQueryForm()
                .filter(new EventQueryForm.Filter(
                        EventQueryForm.Field.MAX_ATTENDEES,
                        EventQueryForm.Operator.NE,
                        "1000"
                ));
        List<Event> conferences = conferenceApi.queryConferences(conferenceQueryForm);
        assertEquals(2, conferences.size());
        assertTrue("The result should contain conference1.", conferences.contains(conference1));
        assertTrue("The result should contain conference3.", conferences.contains(conference3));
        assertEquals(conference1, conferences.get(0));
        assertEquals(conference3, conferences.get(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMultipleInequalityFilter() throws Exception {
        // A query specifies the maxAttendees <= 1000 and month != 6.
        EventQueryForm conferenceQueryForm = new EventQueryForm()
                .filter(new EventQueryForm.Filter(
                        EventQueryForm.Field.MAX_ATTENDEES,
                        EventQueryForm.Operator.LTEQ,
                        "1000"
                ))
                .filter(new EventQueryForm.Filter(
                        EventQueryForm.Field.MONTH,
                        EventQueryForm.Operator.NE,
                        "6"
                ));
    }
}
