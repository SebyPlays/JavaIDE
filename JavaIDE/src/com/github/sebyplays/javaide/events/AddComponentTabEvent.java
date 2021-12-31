package com.github.sebyplays.javaide.events;

import com.github.sebyplays.javaide.JavaIDE;
import com.github.sebyplays.jevent.Event;
import lombok.Getter;

import javax.swing.*;

public class AddComponentTabEvent extends Event {

    @Getter private JTabbedPane jTabbedPane;
    @Getter private String title;
    @Getter private String tooltip;
    @Getter private Icon icon;
    @Getter private JComponent component;
    @Getter private JavaIDE javaIDE;

    public AddComponentTabEvent(){}
    public AddComponentTabEvent(JavaIDE javaIDE, JTabbedPane jTabbedPane, String name, JComponent component, String tooltip, Icon icon) {
        this.javaIDE = javaIDE;
        this.jTabbedPane = jTabbedPane;
        this.title = name;
        this.component = component;
        this.tooltip = tooltip;
        this.icon = icon;
    }
}
