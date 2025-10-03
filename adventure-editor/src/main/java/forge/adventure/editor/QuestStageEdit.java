package forge.adventure.editor;

import forge.adventure.data.*;
import forge.adventure.util.AdventureQuestController;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class QuestStageEdit extends FormPanel {
    private boolean updating = false;
    AdventureQuestStage currentData;
    AdventureQuestData currentQuestData;

    public JTextField name = new JTextField("", 25);
    public JTextField description = new JTextField("", 25);
    public TextListEdit itemNames = new TextListEdit();
    public TextListEdit spriteNames = new TextListEdit();
    public TextListEdit equipNames = new TextListEdit();
    public TextListEdit prerequisites = new TextListEdit();

    JTabbedPane tabs = new JTabbedPane();
    DialogEditor prologueEditor = new DialogEditor();
    DialogEditor epilogueEditor = new DialogEditor();

    // Equipment lists for multi-select slots
    private JList<String> neckList;
    private JList<String> bodyList;
    private JList<String> leftList;
    private JList<String> rightList;
    private JList<String> ability1List;
    private JList<String> ability2List;
    private DefaultListModel<String> allItemsModel = new DefaultListModel<>();

    public QuestStageEdit() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(getInfoTab());
        add(tabs);

        tabs.add("Objective", getObjectiveTab());
        tabs.add("Prologue", getPrologueTab());
        tabs.add("Epilogue", getEpilogueTab());
        tabs.add("Prerequisites", getPrereqTab());

        addListeners();
    }

    private JPanel getInfoTab() {
        JPanel infoTab = new JPanel();
        FormPanel center = new FormPanel();

        center.add("Name:", name);
        center.add("Description:", description);

        name.getDocument().addDocumentListener(new DocumentChangeListener(this::updateStage));
        description.getDocument().addDocumentListener(new DocumentChangeListener(this::updateStage));

        // Equipment multi-select lists
        neckList = new JList<>(allItemsModel);
        neckList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        bodyList = new JList<>(allItemsModel);
        bodyList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        leftList = new JList<>(allItemsModel);
        leftList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        rightList = new JList<>(allItemsModel);
        rightList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        ability1List = new JList<>(allItemsModel);
        ability1List.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        ability2List = new JList<>(allItemsModel);
        ability2List.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        center.add("Neck:", new JScrollPane(neckList));
        center.add("Body:", new JScrollPane(bodyList));
        center.add("Left:", new JScrollPane(leftList));
        center.add("Right:", new JScrollPane(rightList));
        center.add("Ability1:", new JScrollPane(ability1List));
        center.add("Ability2:", new JScrollPane(ability2List));

        infoTab.add(center);
        return infoTab;
    }

    private JPanel getPrologueTab() {
        JPanel prologueTab = new JPanel();
        prologueTab.setLayout(new BoxLayout(prologueTab, BoxLayout.Y_AXIS));
        FormPanel center = new FormPanel();
        center.add(prologueEditor);
        prologueTab.add(center);
        return prologueTab;
    }

    private JPanel getEpilogueTab() {
        JPanel epilogueTab = new JPanel();
        epilogueTab.setLayout(new BoxLayout(epilogueTab, BoxLayout.Y_AXIS));
        FormPanel center = new FormPanel();
        center.add(epilogueEditor);
        epilogueTab.add(center);
        return epilogueTab;
    }

    private void refresh() {
        if (currentData == null) return;
        updating = true;

        name.setText(currentData.name);
        description.setText(currentData.description);
        prologueEditor.loadData(currentData.prologue);
        epilogueEditor.loadData(currentData.epilogue);

        // Populate allItemsModel from itemNames
        allItemsModel.clear();
        if (currentData.itemNames != null) {
            currentData.itemNames.forEach(allItemsModel::addElement);
        }

        // Set selected items in each slot
        setSelectedItems(neckList, currentData.equipSlots.get("Neck"));
        setSelectedItems(bodyList, currentData.equipSlots.get("Body"));
        setSelectedItems(leftList, currentData.equipSlots.get("Left"));
        setSelectedItems(rightList, currentData.equipSlots.get("Right"));
        setSelectedItems(ability1List, currentData.equipSlots.get("Ability1"));
        setSelectedItems(ability2List, currentData.equipSlots.get("Ability2"));

        updating = false;
    }

    private void setSelectedItems(JList<String> list, List<String> items) {
        if (items == null) return;
        List<Integer> indices = items.stream()
                .map(allItemsModel::indexOf)
                .filter(i -> i >= 0)
                .collect(Collectors.toList());
        int[] selectedIndices = indices.stream().mapToInt(i -> i).toArray();
        list.setSelectedIndices(selectedIndices);
    }

    public void updateStage() {
        if (currentData == null || updating) return;

        currentData.name = name.getText();
        currentData.description = description.getText();
        currentData.prologue = prologueEditor.getDialogData();
        currentData.epilogue = epilogueEditor.getDialogData();
        currentData.itemNames = Collections.list(allItemsModel.elements());

        currentData.equipSlots.put("Neck", neckList.getSelectedValuesList());
        currentData.equipSlots.put("Body", bodyList.getSelectedValuesList());
        currentData.equipSlots.put("Left", leftList.getSelectedValuesList());
        currentData.equipSlots.put("Right", rightList.getSelectedValuesList());
        currentData.equipSlots.put("Ability1", ability1List.getSelectedValuesList());
        currentData.equipSlots.put("Ability2", ability2List.getSelectedValuesList());

        emitChanged();
    }

    public void setCurrentStage(AdventureQuestStage stageData, AdventureQuestData data) {
        if (stageData == null) stageData = new AdventureQuestStage();
        if (data == null) data = new AdventureQuestData();
        currentData = stageData;
        currentQuestData = data;
        setVisible(true);
        refresh();
    }

    public void addChangeListener(ChangeListener listener) {
        listenerList.add(ChangeListener.class, listener);
    }

    protected void emitChanged() {
        ChangeListener[] listeners = listenerList.getListeners(ChangeListener.class);
        if (listeners != null && listeners.length > 0) {
            ChangeEvent evt = new ChangeEvent(this);
            for (ChangeListener listener : listeners) {
                listener.stateChanged(evt);
            }
        }
    }

    private void addListeners() {
        neckList.addListSelectionListener(e -> updateStage());
        bodyList.addListSelectionListener(e -> updateStage());
        leftList.addListSelectionListener(e -> updateStage());
        rightList.addListSelectionListener(e -> updateStage());
        ability1List.addListSelectionListener(e -> updateStage());
        ability2List.addListSelectionListener(e -> updateStage());
        itemNames.getEdit().getDocument().addDocumentListener(new DocumentChangeListener(this::refresh));
        prologueEditor.addChangeListener(e -> updateStage());
        epilogueEditor.addChangeListener(e -> updateStage());
        name.getDocument().addDocumentListener(new DocumentChangeListener(this::updateStage));
        description.getDocument().addDocumentListener(new DocumentChangeListener(this::updateStage));
    }
}
