package hse;

import org.deckfour.uitopia.api.event.TaskListener;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XsDateTimeFormat;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.connections.Connection;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.plugins.petrinet.PetriNetVisualization;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Lyange Dmitry
 * Date: 28.02.14
 * Time: 9:50
 */


public class NestedPetrinetCreator {

    private UIPluginContext uiPluginContext;
    private XLog systemLog;
    private List<XLog> elementaryLogs;
    private List<Petrinet> petrinets;
    private List<JComponent> visualizedPetrinets;



    private List<SystemEventWrapper> systemEventsWhichCanBeConnected;
    private List<String> systemEventsWhichWereMet;
    private List<SynchronizationRecord> synchronizationRecords;


    private int userDelta = 2; // value of precision in measuring average time; base value - 2

    private JTextField textField;

    @Plugin(
            name = "Nested Petrinet Creator",
            parameterLabels = { "Event Log Array" },
            returnLabels = { "Petrinet" },
            returnTypes = { Petrinet.class },
            userAccessible = true,
            help = ""
    )


    @UITopiaVariant(
            affiliation = "Higher School of Economics",
            author = "D.Lyange",
            email = "lyange95@gmail.com"
    )


    public Petrinet createNestedNetMain(UIPluginContext context, XLog[] logs) {

        this.uiPluginContext = context;
        TaskListener.InteractionResult choice;
        int logState; // state based on input logs (0 - zero input logs; 1 - only one log; 2 - more than one log)
        int netNumber = 0; // number of petrinet to be shown (initialisation value - system net)
        int pageAndFrameState = 10; // state based on the type of the page and frame to be shown
        // (initialisation value - welcome page)
        int finishedPressed = 0; // counter of Finished button pressed (first time - to progress from initial page,
        // second time - to save results and exit plugin)

        if (logs.length > 1) {
            systemLog = logs[0];
            elementaryLogs = new ArrayList<>();
            petrinets = new ArrayList<>();
            visualizedPetrinets = new ArrayList<>();

            for (int i = 1; i < logs.length; i ++) {
                elementaryLogs.add(logs[i]);
            }
            logState = 2;

        } else if (logs.length == 1) {
            systemLog = logs[0];
            petrinets = new ArrayList<>();
            visualizedPetrinets = new ArrayList<>();
            logState = 1;

        } else {
            logState = 0;
        }


        while (true) {
            switch (pageAndFrameState) {

                case -1: // Incorrect data (for example, no proper logs)
                    choice = TaskListener.InteractionResult.CANCEL;
                    break;

                case 10: // first page
                    choice = showFirstPage();
                    break;

                case 11: // last page
                    choice = showLastPage();
                    break;

                default:
                    choice = showPetrinet(visualizedPetrinets.get(netNumber), pageAndFrameState);
                    break;
            }

            switch (choice) {
                case CANCEL:
                    return null;

                case FINISHED:
                    if (finishedPressed == 0) {
                        try {
                            userDelta = Integer.parseInt(textField.getText());
                        } catch (NumberFormatException e) {
                            JOptionPane.showMessageDialog(null, "Invalid data format!");
                        }
                        finishedPressed ++;
                        pageAndFrameState = calculatePageAndFrameState(logState);
                    } else {
                        if (logState == 2) {
                            //JFileChooser chooser = new JFileChooser();
                            //int retrival = chooser.showSaveDialog(null);
                            //if (retrival == JFileChooser.APPROVE_OPTION) {
                            //    try {
                            //        OutputStream out = new FileOutputStream(chooser.getSelectedFile() + ".npnets");
                            //        serialize(petrinets, out);
                            //    } catch (Exception ex) {
                            //        JOptionPane.showMessageDialog(null, "Fail while saving");
                            //    }
                            //}
                            try {
                                OutputStream out = new FileOutputStream(new File("npnets_result/save.npnets")) {
                                };
                                PnmlNpnetsSerializer pnmlNpnetsSerializer = new PnmlNpnetsSerializer(synchronizationRecords);
                                pnmlNpnetsSerializer.serialize(petrinets, out);
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(null, "Fail while saving");
                            }
                        } else {
                            JOptionPane.showMessageDialog(null, "If there is only one system log," +
                                    " than .npnets file will not be produced");
                        }

                        return petrinets.get(0);
                    }

                    break;

                case NEXT:
                    netNumber ++;
                    if (netNumber < logs.length - 1) { // not the last net
                        pageAndFrameState = 1;
                    } else if (netNumber == logs.length - 1) { // the last net
                        pageAndFrameState = 2;
                    } else if (netNumber == logs.length){ // the last page
                        pageAndFrameState = 11;
                    } else {
                        JOptionPane.showMessageDialog(null, "Cannot show something that does not exist!");
                    }

                    break;

                case PREV:
                    netNumber --;
                    if (pageAndFrameState == 11) {
                        pageAndFrameState = 2;
                    } else {
                        if (netNumber > 0) { // not the first(system) net
                            pageAndFrameState = 1;
                        } else if (netNumber == 0) { // the first(system) net
                            pageAndFrameState = 0;
                        } else {
                            JOptionPane.showMessageDialog(null, "Cannot show something that does not exist!");
                        }
                    }

                    break;
            }
        }
    }

    /**
     * Calculates which type of page should be shown and in which type of frame petrinet should be put
     *
     * @param logState      state based on input logs (0 - zero input logs; 1 - only one log; 2 - more than one log)
     * @return type of the page and frame to be shown (<code>-1</code> - incorrect data; <code>0</code> - first out of
     * many petrinet; <code>3</code> - the only petrinet)
     *
     */
    private int calculatePageAndFrameState(int logState) {

        int pageAndFrameState = 3;

        switch (logState) {

            case 0: // no proper logs in input file
                JOptionPane.showMessageDialog(null, "Incorrect data");
                pageAndFrameState = -1;
                break;

            case 1: // only system log exists (basically, simple petrinet will be created)
                try {
                    petrinets.add(uiPluginContext.tryToFindOrConstructFirstNamedObject(Petrinet.class, "Alpha Miner",
                            Connection.class, "", systemLog));
                    visualizedPetrinets.add(visualizePetrinet(petrinets.get(0)));

                    pageAndFrameState = 3;
                } catch (ConnectionCannotBeObtained e) {
                    JOptionPane.showMessageDialog(null, "Fail");   //TODO
                }

                break;

            case 2: //system log and at least one event log exist
                try {
                    petrinets.add(uiPluginContext.tryToFindOrConstructFirstNamedObject(Petrinet.class, "Alpha Miner",
                            Connection.class, "", systemLog));

                    for (int i = 0; i < elementaryLogs.size(); i ++) {
                        petrinets.add(uiPluginContext.tryToFindOrConstructFirstNamedObject(Petrinet.class, "Alpha Miner",
                                Connection.class, "", elementaryLogs.get(i)));

                        systemEventsWhichWereMet = new ArrayList<>();
                        systemEventsWhichCanBeConnected = new ArrayList<>();
                        synchronizeWithSystemLog(elementaryLogs.get(i), i + 1);
                    }

                    synchronizationRecords = new ArrayList<>();

                    String systemNetTransitionUuid = null;
                    // TODO повторяющиеся?
                    for (SystemEventWrapper systemEventWrapper: systemEventsWhichCanBeConnected) {
                        for (Transition systemTransition: petrinets.get(0).getTransitions()) {
                            if ((systemEventWrapper.getName() + "+").equals(systemTransition.toString())) {
                                systemNetTransitionUuid = "npn" + systemTransition.getId().toString().substring(5);
                                break;
                            }
                        }

                        String eventNetTransitionUuid = null;

                        for (ElementaryEventWrapper elementaryEventWrapper:
                                systemEventWrapper.getCandidatesForSynchronisation()) {

                            int eventNetNumber = elementaryEventWrapper.getEventNetNumber();
                            for (Transition eventTransition: petrinets.get(eventNetNumber).getTransitions()) {
                                if ((elementaryEventWrapper.getName() + "+").equals(eventTransition.toString())) {
                                    eventNetTransitionUuid = "npn" + eventTransition.getId().toString().substring(5);
                                    break;
                                }
                            }

                            synchronizationRecords.add(new SynchronizationRecord(systemEventWrapper.getName() + "+",
                                    elementaryEventWrapper.getName() + "+", systemNetTransitionUuid,
                                    eventNetTransitionUuid));
                        }
                    }


                    for (Petrinet petrinet: petrinets) {
                        visualizedPetrinets.add(visualizePetrinet(petrinet));
                    }

                    pageAndFrameState = 0;
                } catch (ConnectionCannotBeObtained e) {
                    JOptionPane.showMessageDialog(null, "Fail");
                }

                break;

            default:
                pageAndFrameState = -1;
        }

        return pageAndFrameState;
    }

    /**
     * TODO
     *
     * @param elementaryLog     currently processed event log
     *
     */
    private void synchronizeWithSystemLog(XLog elementaryLog, int elementaryLogNumber) {

        long systemDelta; // the average duration of event in system petrinet
        long elementaryDelta; // the average duration of event in current elementary petrinet
        long averageDelta; // average duration of event for both average durations of system and elementary petrinets

        List<SystemEventWrapper> systemEvents;
        List<ElementaryEventWrapper> elementaryEvents;

        boolean isFirstSystemTrace = true;

        for (XTrace xSystemTrace: systemLog) {
            for (XTrace xElementaryTrace: elementaryLog) {
                if (xSystemTrace.getAttributes().get("concept:name").equals(
                        xElementaryTrace.getAttributes().get("concept:name"))) {

                    systemEvents = new ArrayList<>();
                    elementaryEvents = new ArrayList<>();

                    Date temp;
                    XsDateTimeFormat format = new XsDateTimeFormat();

                    for (XEvent xSystemEvent: xSystemTrace) {
                        String eventDate = xSystemEvent.getAttributes().get("time:timestamp").toString();
                        try {
                            temp = format.parseObject(eventDate);
                            systemEvents.add(new SystemEventWrapper(temp.getTime(),
                                    xSystemEvent.getAttributes().get("concept:name").toString()));
                        } catch (ParseException e) {
                            JOptionPane.showMessageDialog(null, "Invalid date format!");
                        }

                    }

                    Collections.sort(systemEvents);
                    systemDelta = (systemEvents.get(systemEvents.size() - 1).getDate() - systemEvents.get(0).getDate())
                            / systemEvents.size();
                    systemDelta /= userDelta;

                    // TODO create function for these repeating blocks of code

                    for (XEvent xElementaryEvent: xElementaryTrace) {
                        String eventDate = xElementaryEvent.getAttributes().get("time:timestamp").toString();
                        try {
                            temp = format.parseObject(eventDate);
                            elementaryEvents.add(new ElementaryEventWrapper(temp.getTime(),
                                    xElementaryEvent.getAttributes().get("concept:name").toString(), elementaryLogNumber));
                        } catch (ParseException e) {
                            JOptionPane.showMessageDialog(null, "Invalid date format!");
                        }

                    }

                    Collections.sort(elementaryEvents);
                    elementaryDelta = (elementaryEvents.get(elementaryEvents.size() - 1).getDate() -
                            elementaryEvents.get(0).getDate()) / elementaryEvents.size();
                    elementaryDelta /= userDelta;

                    averageDelta = (systemDelta + elementaryDelta) / 2;


                    // because it is first trace, we can add all Events from systemEvents
                    if (isFirstSystemTrace) {
                        for (SystemEventWrapper systemEventWrapper: systemEvents) {
                            for (ElementaryEventWrapper elementaryEventWrapper: elementaryEvents) {
                                if ((systemEventWrapper.getDate() - averageDelta < elementaryEventWrapper.getDate()) &&
                                        (systemEventWrapper.getDate() + averageDelta > elementaryEventWrapper.getDate())) {
                                    systemEventWrapper.getCandidatesForSynchronisation().add(elementaryEventWrapper);
                                } else if (systemEventWrapper.getDate() + averageDelta < elementaryEventWrapper.getDate()) {
                                    break;
                                }
                            }
                            systemEventsWhichCanBeConnected.add(systemEventWrapper);
                            systemEventsWhichWereMet.add(systemEventWrapper.getName());
                        }
                    } else {
                        for (SystemEventWrapper systemEventWrapper: systemEvents) {

                            if (systemEventsWhichWereMet.contains(systemEventWrapper.getName())) {
                                for (ElementaryEventWrapper elementaryEventWrapper:
                                        systemEventWrapper.getCandidatesForSynchronisation()) {
                                    if ((systemEventWrapper.getDate() - averageDelta > elementaryEventWrapper.getDate())
                                            || (systemEventWrapper.getDate() + averageDelta <
                                            elementaryEventWrapper.getDate())) {
                                        systemEventWrapper.getCandidatesForSynchronisation().remove(elementaryEventWrapper);
                                    }
                                }
                            } else {
                                for (ElementaryEventWrapper elementaryEventWrapper: elementaryEvents) {
                                    if ((systemEventWrapper.getDate() - averageDelta < elementaryEventWrapper.getDate()) &&
                                            (systemEventWrapper.getDate() + averageDelta > elementaryEventWrapper.getDate())) {
                                        systemEventWrapper.getCandidatesForSynchronisation().add(elementaryEventWrapper);
                                    } else if (systemEventWrapper.getDate() + averageDelta < elementaryEventWrapper.getDate()) {
                                        break;
                                    }
                                }
                                systemEventsWhichCanBeConnected.add(systemEventWrapper);
                                systemEventsWhichWereMet.add(systemEventWrapper.getName());
                            }
                        }
                    }
                }
            }
            isFirstSystemTrace = false;
        }
    }



    /**
     * TODO
     *
     * @param petrinet
     * @return
     */
    private JComponent visualizePetrinet(Petrinet petrinet) {

        PetriNetVisualization visualization = new PetriNetVisualization();
        PluginContext additionalContext = uiPluginContext.createChildContext("Visualise net");
        uiPluginContext.getPluginLifeCycleEventListeners().firePluginCreated(additionalContext);
        return visualization.visualize(additionalContext, petrinet);

    }

    /**
     * TODO
     *
     * @return
     */
    private TaskListener.InteractionResult showFirstPage() {

        JPanel panel = new JPanel();
        JTextArea textArea1 = new JTextArea();
        textArea1.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        textArea1.setBackground(Color.LIGHT_GRAY);
        textArea1.setText(" In order to correctly generate nested Petrinet and be able to interpret results," +
                " please look through these rules, following them \n will greatly increase the chances of successful" +
                " outcome:\n" +
                " 1) First log which you have entered will represent the system log, all other logs will act as " +
                "elementary logs.\n" +
                " 2) In order to produce visualization for each individual net, a-algorithm has been used to create " +
                "Petrinets from inputted logs. So beware the limits\n" +
                " of this method of process mining and do not be disappointed, if your log will be interpreted into " +
                "an incorrect net.\n" +
                " 3) This plugin can create only two-leveled nested nets and also creates only vertical " +
                "synchronization mark( not horizontal! ), so elementary nets\n" +
                " will be independant from each other.\n" +
                " 4) In order to generate correct vertical synchronization marks, please verify, that your events in " +
                "logs are written in the chronological order.\n" +
                " 5) Also for all logs to be interpreted and used as one for the creation of the nested Petrinet, all" +
                " logs should have equal in count and matching\n" +
                " in names amount of Traces\n" +
                " 6) Also you can change the value of delta for synchronisation: the larger the delta, the stricter " +
                "will be created the nested Petrinet. This delta is\n" +
                " locate in the label below. Feel free to change it and behold the difference in results. Also" +
                " remember, that the initial value is 2.");

        textArea1.setEditable(false);

        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        textField = new JTextField();
        textField.setText(Integer.toString(userDelta));
        textField.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        textField.setEditable(true);

        panel.add(textArea1);
        panel.add(textField);

        return uiPluginContext.showWizard("Introduction", true, true, panel);
    }

    /**
     * TODO
     *
     * @return
     */
    private TaskListener.InteractionResult showLastPage() {

        JPanel panel = new JPanel();
        JTextArea textArea1 = new JTextArea();
        String results;

        results = "Current delta: " + Integer.toString(userDelta) + "\n\n";
        for (SystemEventWrapper systemEventWrapper: systemEventsWhichCanBeConnected) {
            if (systemEventWrapper.getCandidatesForSynchronisation().size() > 0) {
                results += systemEventWrapper.getName() + ":  ";
                for (ElementaryEventWrapper elementaryEventWrapper: systemEventWrapper.getCandidatesForSynchronisation()) {
                    results += elementaryEventWrapper.getName() + " ";
                }
                results += "\n";
            }
        }

        textArea1.setEditable(false);
        textArea1.setBackground(Color.LIGHT_GRAY);
        textArea1.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        textArea1.setText(results);
        panel.add(textArea1);

        return uiPluginContext.showWizard("Results", false, true, panel);
    }

    /**
     * TODO
     *
     * @param visualizedPetrinet
     * @param wizardType
     * @return
     */
    private TaskListener.InteractionResult showPetrinet(JComponent visualizedPetrinet, int wizardType) {

        switch (wizardType) {
            case 0: // first but not last net to display
                return uiPluginContext.showWizard("System net", true, false, visualizedPetrinet);

            case 1: // neither first nor last net to display
                return uiPluginContext.showWizard("Event net", false, false, visualizedPetrinet);

            case 2: // not first but last net to display
                return uiPluginContext.showWizard("Event net", false, false, visualizedPetrinet);

            case 3: // first and last net to display
            default:
                return uiPluginContext.showWizard("The net given", true, true, visualizedPetrinet);
        }

    }

}