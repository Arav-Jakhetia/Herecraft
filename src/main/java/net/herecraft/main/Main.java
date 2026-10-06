package net.herecraft.main;

import net.herecraft.Herecraft;
import net.herecraft.client.ui.MainMenu;
import net.herecraft.client.ui.WorldSelectMenu;

public class Main {
    public static void main(String args[]) {
        if(!MainMenu.show()) {
            return;
        }

        java.io.File worldFolder = WorldSelectMenu.show();
        if(worldFolder != null) {
            new Herecraft(worldFolder).run();
        }
    }
}
