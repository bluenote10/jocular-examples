package org.saintandreas.android.ui;

public class MenuItem {
  enum Type {
    COMMAND,
    SUBMENU
  }
  public final int icon;
  public final int string;
  public final int command;

  MenuItem(int command, int string, int icon) {
    this.icon = icon;
    this.string = string;
    this.command = command;
  }

  MenuItem(int command, int string) {
    this.icon = -1;
    this.string = string;
    this.command = command;
  }
}
