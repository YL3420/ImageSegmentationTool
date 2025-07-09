package org.example;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.example.imaging.ProcessedImage;
import org.example.use_interface.GraphicalUserInterface;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {


    public static void main(String[] args) {

        // process image for algoithm
        ProcessedImage image = new ProcessedImage("images/cat.jpg");
        image.grayScaleImage();
        image.resizeImage(9);


        // image props
        BufferedImage originalImageInstance = image.getOriginalImageInstance();
        int imgWidth = image.getDimensions().width();
        int imgHeight = image.getDimensions().height();

        SwingUtilities.invokeLater(() -> {
            try {
                JFrame frame = new JFrame("Segmentation Selector");
                GraphicalUserInterface panel = new GraphicalUserInterface(
                        image.getProcessedImageInstance());

                JButton toggleSelector = new JButton("toggle selector");
                toggleSelector.addActionListener(e -> panel.toggleSelectingObjSeed());

                JButton runButton = new JButton("Run Segmentation");
                runButton.addActionListener(e -> {

                    System.out.println("algo started");

                    boolean[] minCut = image.runGraphCut(
                            panel.getSrcLoc().pointToIndex(imgWidth),
                            panel.getSinkLoc().pointToIndex(imgWidth),
                            panel.getObjSeedSet(),
                            panel.getBkgSeedSet()
                    );

                    System.out.println("algo done");

                    // label obj and modify
                    for (int y = 0; y < imgHeight; y++) {
                        for (int x = 0; x < imgWidth; x++) {
                            int curr = y * imgWidth + x;

                            if (minCut[curr]) {
                                int originalRGB = originalImageInstance.getRGB(x, y);

                                int r = (originalRGB >> 16) & 0xFF;
                                int g = (originalRGB >> 8) & 0xFF;
                                int b = originalRGB & 0xFF;

                                int newR = (int) (0.5 * r + 0.5 * 255);
                                int newG = (int) (0.5 * g);
                                int newB = (int) (0.5 * b);

                                int blended = (0xFF << 24) | (newR << 16) | (newG << 8) | newB;
                                image.setRGB(x, y, blended);
                            }
                        }
                    }

                    panel.setImage(image.getProcessedImageInstance());
                    panel.repaint();
                });

                JPanel buttonPanel = new JPanel();
                buttonPanel.setLayout(new FlowLayout());
                buttonPanel.add(toggleSelector);
                buttonPanel.add(runButton);

                JPanel wrapper = new JPanel(new BorderLayout());
                wrapper.add(panel, BorderLayout.CENTER);
                wrapper.add(buttonPanel, BorderLayout.SOUTH);

                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setContentPane(wrapper);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);

            } catch (Exception e) {
                System.out.println("error!");
            }
        });

    }


}


