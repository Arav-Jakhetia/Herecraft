package net.herecraft.client.ui;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class WorldSelectMenu {
    private static final File WORLD_DIR = new File(System.getProperty("user.home"), ".herecraft/saves");

    public static File show() {
        CountDownLatch closed = new CountDownLatch(1);
        AtomicReference<File> selectedWorld = new  AtomicReference<>();

        try {
            SwingUtilities.invokeAndWait(() -> openWindow(closed, selectedWorld));
            closed.await();
            return selectedWorld.get();
        } catch(Exception exception) {
            throw new RuntimeException("Could not open world selection", exception);
        }
    }

    private static void openWindow(CountDownLatch closed, AtomicReference<File> selectedWorld) {
        WORLD_DIR.mkdirs();

        JFrame window = new JFrame("Select World");
        window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        window.setSize(720, 520);
        window.setLocationRelativeTo(null);

        DefaultListModel<WorldEntry> model = new DefaultListModel<>();
        JList<WorldEntry> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new WorldRenderer());

        Runnable refresh = () -> {
            model.clear();
            File[] folders = WORLD_DIR.listFiles(File::isDirectory);
            if(folders == null) return;

            for(File folder :  folders) {
                File metadata = new File(folder, "world.properties");
                if(metadata.isFile()) {
                    model.addElement(new WorldEntry(folder));
                }
            }
        };

        refresh.run();

        JButton play = new JButton("Play Selected World");
        JButton create = new JButton("Create");
        JButton edit = new JButton("Edit");
        JButton delete = new JButton("Delete");
        JButton back = new JButton("Back");

        play.addActionListener(e -> {
            WorldEntry entry = list.getSelectedValue();
            if(entry != null) {
                selectedWorld.set(entry.folder);
                window.dispose();
                closed.countDown();
            }
        });

        create.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(window, "World name:");
            if(name == null || name.isBlank()) return;

            File folder = new File(WORLD_DIR, UUID.randomUUID().toString());
            folder.mkdirs();

            Properties properties = new Properties();
            properties.setProperty("name", name.trim());
            properties.setProperty("created", Long.toString(System.currentTimeMillis()));

            try(FileOutputStream output = new FileOutputStream(new File(folder, "world.properties"))) {
                properties.store(output, "Herecraft world");
                refresh.run();
            } catch(Exception exception) {
                JOptionPane.showMessageDialog(window, "Could not create world.");
            }
        });

        edit.addActionListener(e -> {
            WorldEntry entry = list.getSelectedValue();
            if(entry == null) return;

            String name = JOptionPane.showInputDialog(window, "World name:", entry.name);
            if(name == null || name.isBlank()) return;

            entry.saveName(name.trim());
            refresh.run();
        });

        delete.addActionListener(e -> {
            WorldEntry entry = list.getSelectedValue();
            if(entry == null) return;

            int answer = JOptionPane.showConfirmDialog(window, "Delete \"" + entry.name + "\"?", "Delete World", JOptionPane.YES_NO_OPTION);
            if(answer == JOptionPane.YES_OPTION) {
                deleteFolder(entry.folder);
                refresh.run();
            }
        });

        back.addActionListener(e -> {
            window.dispose();
            closed.countDown();
        });

        JPanel buttons = new JPanel(new FlowLayout());
        buttons.add(play);
        buttons.add(create);
        buttons.add(edit);
        buttons.add(delete);
        buttons.add(back);

        window.add(new JScrollPane(list), BorderLayout.CENTER);
        window.add(buttons, BorderLayout.SOUTH);
        window.setVisible(true);
    }

    private static void deleteFolder(File folder) {
        File[] childern = folder.listFiles();
        if(childern != null) {
            for(File child : childern) {
                if(child.isDirectory()) {
                    deleteFolder(child);
                } else {
                    child.delete();
                }
            }
        }
        folder.delete();
    }

    private static class WorldEntry {
        final File folder;
        final String name;
        final ImageIcon preview;

        WorldEntry(File folder) {
            this.folder = folder;

            Properties properties =  new Properties();
            try(FileInputStream input = new FileInputStream(new File(folder, "world.properties"))) {
                properties.load(input);
            } catch(Exception ignored) {}

            name = properties.getProperty("name", folder.getName());

            File imageFile = new File(folder, "icon.png");
            ImageIcon loadedPreview = null;
            if(imageFile.isFile()) {
                loadedPreview = new ImageIcon(new ImageIcon(imageFile.getAbsolutePath()).getImage().getScaledInstance(64, 64, Image.SCALE_DEFAULT));
            }
            preview = loadedPreview;
        }

        void saveName(String newName) {
            Properties properties = new Properties();
            properties.setProperty("name", newName);

            File metadata = new File(folder, "world.properties");
            try(FileInputStream input = new FileInputStream(metadata)) {
                properties.load(input);
            } catch(Exception ignored) {}

            properties.setProperty("name", newName);
            try(FileOutputStream output = new FileOutputStream(metadata)) {
                properties.store(output, "Herecraft world");
            } catch(Exception ignored) {}
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static class WorldRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focused) {
            super.getListCellRendererComponent(list, value, index, selected, focused);

            WorldEntry entry = (WorldEntry)value;
            setText(entry.name);
            setIcon(entry.preview);
            setPreferredSize(new Dimension(600, 76));
            return this;
        }
    }
}
