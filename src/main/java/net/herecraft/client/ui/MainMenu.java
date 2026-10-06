package net.herecraft.client.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;


public class MainMenu {
    private static final int TILE_SIZE = 32;
    private static final int AIR = 0;
    private static final int GRASS = 1;
    private static final int DIRT = 2;
    private static final int STONE = 3;

    public static boolean show() {
        CountDownLatch closed = new CountDownLatch(1);
        AtomicBoolean play = new AtomicBoolean(false);

        try {
            SwingUtilities.invokeAndWait(() -> createWindow(closed, play));
            closed.await();
            return play.get();
        } catch(Exception exception) {
            throw new RuntimeException("Could not opem the main menu", exception);
        }
    }

    private static void createWindow(CountDownLatch closed, AtomicBoolean play) {
        JFrame window = new JFrame("Herecraft");
        window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        window.setUndecorated(true);
        window.setExtendedState(JFrame.MAXIMIZED_BOTH);

        MenuPanel background = new MenuPanel();
        background.setLayout(new GridBagLayout());

        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS));

        JButton playButton = makeButton("Play");
        JButton quitButton = makeButton("Quit");

        playButton.addActionListener(e -> {
           play.set(true);
           window.dispose();
           closed.countDown();
        });

        quitButton.addActionListener(e -> {
            window.dispose();
            closed.countDown();
        });

        buttons.add(playButton);
        buttons.add(Box.createVerticalStrut(14));
        buttons.add(quitButton);

        background.add(buttons);
        window.setContentPane(background);

        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                window.dispose();
                closed.countDown();
            }
        });

        window.setVisible(true);
    }

    private static JButton makeButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Monospaced", Font.BOLD, 26));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(92, 92, 92));
        button.setFocusPainted(false);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setPreferredSize(new Dimension(240, 58));
        button.setMaximumSize(new Dimension(240, 58));
        button.setContentAreaFilled(true);

        Border darkBorder = BorderFactory.createLineBorder(new Color(35, 35, 35), 4);
        Border lightBorder = BorderFactory.createLineBorder(new Color(180, 180, 180), 2);
        button.setBorder(BorderFactory.createCompoundBorder(lightBorder, darkBorder));

        return button;
    }

    private static class MenuPanel extends JPanel {
        private final BufferedImage grassSide = loadTexture("/assets/herecraft/textures/block/grass_block_side.png");
        private final BufferedImage grassOverlay = loadTexture("/assets/herecraft/textures/block/grass_block_side_overlay.png");
        private final BufferedImage dirt = loadTexture("/assets/herecraft/textures/block/dirt.png");
        private final BufferedImage stone = loadTexture("/assets/herecraft/textures/block/stone.png");

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);

            Graphics2D g = (Graphics2D)graphics.create();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

            int columns = (getWidth() + TILE_SIZE - 1) / TILE_SIZE;
            int rows = (getHeight() + TILE_SIZE - 1) / TILE_SIZE;
            int[][] tiles = makeTerrain(columns, rows);

            g.setColor(new Color(120, 185, 250));
            g.fillRect(0, 0, getWidth(), getHeight());

            for(int row = 0; row < rows; row++) {
                for(int column = 0; column < columns; column++) {
                    int x = column * TILE_SIZE;
                    int y = row * TILE_SIZE;

                    switch(tiles[row][column]) {
                        case GRASS -> {
                            drawTile(g, grassSide, x, y);
                            drawTile(g, grassOverlay, x, y);
                        }
                        case DIRT -> drawTile(g, dirt, x, y);
                        case STONE -> drawTile(g, stone, x, y);
                    }
                }
            }

            g.dispose();
        }

        private int[][] makeTerrain(int columns, int rows) {
            int[][] tiles = new  int[rows][columns];
            int baseSurface = Math.max(2, (int)(rows * 0.70));

            for(int column = 0;  column < columns; column++) {
                double hill = Math.sin(column * 0.045) * 3.0 +  Math.sin(column * 0.12) * 1.5;
                int surface = baseSurface - (int)Math.round(hill);
                surface = Math.max(1, Math.min(rows - 2, surface));

                for(int row = 0; row < rows; row++) {
                    if(row == rows - 1) {
                        tiles[row][column] = STONE;
                    } else if(row < surface) {
                        tiles[row][column] = AIR;
                    } else if(row == surface) {
                        tiles[row][column] = GRASS;
                    } else {
                        tiles[row][column] = DIRT;
                    }
                }
            }

            return tiles;
        }

        private void drawTile(Graphics2D g, BufferedImage texture, int x, int y) {
            if(texture != null) {
                g.drawImage(texture, x, y, TILE_SIZE, TILE_SIZE, null);
            }
        }

        private BufferedImage loadTexture(String path) {
            try(InputStream stream = getClass().getResourceAsStream(path)) {
                return stream == null ? null : ImageIO.read(stream);
            } catch(Exception exception) {
                return null;
            }
        }
    }
}
