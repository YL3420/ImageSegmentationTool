package org.example.imaging;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import javax.imageio.ImageIO;
import org.example.network.EdmondsKarpSolver;
import org.example.network.NetworkFlowSolverBase;
import org.example.use_interface.GraphicalUserInterface.CustomPoint;
import org.example.network.GraphCut;
import org.example.network.Terminal;

public class ProcessedImage {

    private final String src;
    private BufferedImage originalImage;
    private BufferedImage processedImageInstance;
    private int height;
    private int width;
    private int downsizeFactor;

    public ProcessedImage(String src){

        this.src = src;

        try {
            this.originalImage = ImageIO.read(new File(src));

            if(originalImage == null){
                System.out.println("failed to read image");
                return;
            }

            this.height = originalImage.getHeight();
            this.width = originalImage.getWidth();

            this.processedImageInstance = originalImage;

        } catch (Exception e){
            e.printStackTrace();
        }
    }


    /**
     *  converts processedImageInstance from 3-channel RGB to single channel intensity [0, 255]
     */
    public void grayScaleImage(){

        BufferedImage gray = new BufferedImage(
                this.width,
                this.height,
                BufferedImage.TYPE_BYTE_GRAY
        );
        gray.getGraphics().drawImage(originalImage, 0, 0, null);

        this.processedImageInstance = gray;
    }


    /**
     *  downsize the processedImageInstance and decrease resolution
     *  divides width and height by factor
     * @param factor
     */
    public void resizeImage(int factor){
        int targetHeight = height/factor;
        int targetWidth = width/factor;

        assert targetWidth > 0 && targetHeight > 0;

        this.downsizeFactor = factor;
        this.width = targetWidth;
        this.height = targetHeight;

        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(processedImageInstance, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();

        this.processedImageInstance = resized;
    }


    /**
     * write processedImageInstance as file to src and adds label (addition)
     * @param src
     * @param addition
     */
    public void writeProcessedImage(String src, String addition){
        try {
            ImageIO.write(processedImageInstance, "jpg", new File(src.substring(0, src.length()-4) + addition + ".jpg"));
        } catch(Exception e){
            System.out.println("failed to write image");
        }
    }


    public record Dimensions(int width, int height){

    }

    public Dimensions getDimensions(){
        return new Dimensions(width, height);
    }


    /**
     *  returns a copy of originalImageInstance before any processing
     */
    public BufferedImage getOriginalImageInstance(){
        BufferedImage copy = new BufferedImage(
                originalImage.getWidth(),
                originalImage.getHeight(),
                originalImage.getType()
        );
        Graphics2D g2d = copy.createGraphics();
        g2d.drawImage(originalImage, 0, 0, null);
        g2d.dispose();
        return copy;
    }


    /**
     *  returns a copy of the latest processedImageInstance
     */
    public BufferedImage getProcessedImageInstance(){
        BufferedImage copy = new BufferedImage(
                processedImageInstance.getWidth(),
                processedImageInstance.getHeight(),
                processedImageInstance.getType()
        );
        Graphics2D g2d = copy.createGraphics();
        g2d.drawImage(processedImageInstance, 0, 0, null);
        g2d.dispose();
        return copy;
    }

    public void setRGB(int x, int y, int blended){
        processedImageInstance.setRGB(x, y, blended);
    }


    /**
     * runs the graph cut given source, sink, and the O, B sets
     */

    boolean graphCutPerformed = false;
    boolean[] graphCut;

    public boolean[] runGraphCut(int src, int sink, List<CustomPoint> objSeedSet, List<CustomPoint> bkgSeedSet) {

        if (graphCutPerformed) return graphCut;

        int imgSize = this.width * this.height;
        GraphCut graph = new GraphCut(imgSize + 2, imgSize + 4 * width * height); // buffer for neighbors + source/sink links

        int[] intensities = new int[imgSize];
        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                int idx = y * this.width + x;
                int rgb = processedImageInstance.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                intensities[idx] = (int)(0.299 * r + 0.587 * g + 0.114 * b);
            }
        }

        HistogramModel hist = new HistogramModel();
        HashSet<Integer> inO = new HashSet<>();
        HashSet<Integer> inB = new HashSet<>();

        // Add terminal connections (seeds)
        for (CustomPoint p : objSeedSet) {
            int idx = p.pointToIndex(this.width);
            graph.setTerminalWeights(idx, Float.POSITIVE_INFINITY, 0);
            hist.addObjSeed(intensities[idx]);
            inO.add(idx);
        }

        for (CustomPoint p : bkgSeedSet) {
            int idx = p.pointToIndex(this.width);
            graph.setTerminalWeights(idx, 0, Float.POSITIVE_INFINITY);
            hist.addBkgSeed(intensities[idx]);
            inB.add(idx);
        }

        // Add n-links and soft t-links
        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                int curr = y * this.width + x;

                if (!inO.contains(curr) && !inB.contains(curr)) {
                    long weightSrc = hist.objEnergy(intensities[curr]);
                    long weightSink = hist.bkgEnergy(intensities[curr]);
                    graph.setTerminalWeights(curr, weightSink, weightSrc);
                }

                int[] dx = {1, 0, -1, 0};
                int[] dy = {0, 1, 0, -1};
                for (int d = 0; d < 4; d++) {
                    int nx = x + dx[d];
                    int ny = y + dy[d];
                    if (nx < 0 || nx >= this.width || ny < 0 || ny >= this.height) continue;

                    int neighbor = ny * this.width + nx;
                    int diff = Math.abs(intensities[curr] - intensities[neighbor]);
                    int noise = 30;
                    long weight = (long)(100 * Math.exp(-((double)diff * diff) / (2 * noise * noise)));
                    graph.setEdgeWeight(curr, neighbor, weight);
                }
            }
        }

        // Run graph cut
        graph.computeMaximumFlow(false, null);

        // Mark segmentation: FOREGROUND = true, BACKGROUND = false
        graphCut = new boolean[imgSize];
        for (int i = 0; i < imgSize; i++) {
            graphCut[i] = graph.getTerminal(i) == Terminal.FOREGROUND;
        }

        graphCutPerformed = true;
        return graphCut;
    }


}
