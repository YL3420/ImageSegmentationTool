package org.example.imaging;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.Buffer;
import java.util.HashSet;
import java.util.List;
import javax.imageio.ImageIO;
import org.example.network.EdmondsKarpSolver;
import org.example.network.NetworkFlowSolverBase;
import org.example.use_interface.GraphicalUserInterface.CustomPoint;

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

    public void grayScaleImage(){

        BufferedImage gray = new BufferedImage(
                this.width,
                this.height,
                BufferedImage.TYPE_BYTE_GRAY
        );


        gray.getGraphics().drawImage(originalImage, 0, 0, null);

//        System.out.println(gray.getRaster().getWidth()*gray.getRaster().getHeight());

        this.processedImageInstance = gray;
    }

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

//        System.out.println(resized.getRaster().getWidth()*resized.getRaster().getHeight());

        this.processedImageInstance = resized;
    }

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



//    private long computeRegionalTerm(){
//
//    }



    boolean graphCutPerformed = false;
    boolean[] graphCut;

    public boolean[] runGraphCut(int src, int sink, List<CustomPoint> objSeedSet,
            List<CustomPoint> bkgSeedSet){

        if(graphCutPerformed) return graphCut;

        int imgSize = this.width * this.height;

        // impose topological constraints with hard coded sets O and B
        int[] objSeed = new int[imgSize];
        int[] bkgSeed = new int[imgSize];


        NetworkFlowSolverBase graph = new EdmondsKarpSolver(imgSize, src, sink);
        int[] intensities = new int[imgSize];

        // populate intensity array
        for(int y=0; y<this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                int curr = y*this.width+x;

                int rgb = processedImageInstance.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8)  & 0xFF;
                int b = rgb & 0xFF;

                int intensity = (int)(0.299*r + 0.587*g + 0.114*b);

                intensities[curr] = intensity;
            }
        }

        HistogramModel hist = new HistogramModel();
        HashSet<Integer> inO = new HashSet<>();
        HashSet<Integer> inB = new HashSet<>();

        // impose hard constraints
        for(CustomPoint p : objSeedSet){
            int o = p.pointToIndex(processedImageInstance.getWidth());
            graph.addEdge(src, o, Long.MAX_VALUE);

            hist.addObjSeed(intensities[p.pointToIndex(width)]);
            inO.add(p.pointToIndex(this.width));
        }

        for(CustomPoint p : bkgSeedSet){
            int b = p.pointToIndex(this.width);
            graph.addEdge(b, sink, Long.MAX_VALUE);

            hist.addBkgSeed(intensities[p.pointToIndex(width)]);
            inB.add(p.pointToIndex(this.width));
        }



        // remaining neighbor edge operations
        // O(N0
        for(int y=0; y<this.height; y++){
            for(int x=0; x<this.width; x++){

                int curr = y*this.width+x;


                // adding t-links
                if(!inO.contains(curr) && ! inB.contains(curr)){
                    long weightSrc = hist.objEnergy(intensities[curr]);
                    long weightSink = hist.bkgEnergy(intensities[curr]);

                    graph.addEdge(src, curr, weightSink);
                    graph.addEdge(curr, sink, weightSrc);
                }


                int[] dx = {1, 0, -1, 0};
                int[] dy = {0, 1, 0, -1};

                // adding n-links
                // O(1)
                for(int d=0; d<4; d++){
                    int nx = x + dx[d];
                    int ny = y + dy[d];

                    int neighbor = ny * this.width + nx;
                    if(0 <= nx && nx < this.width && 0 <= ny && ny < this.height){

                        int diff = Math.abs(intensities[curr] - intensities[neighbor]);
                        int noise = 30;
                        long weight = (long)(100 * Math.exp(- ((double)diff * diff) / (2 * noise * noise)));
                        long weightSafe = Math.max(1, weight);

                        graph.addEdge(curr, neighbor, weightSafe);
                    }

                }
            }
        }

        // solves the min cut
        this.graphCut = graph.getMinCut();
        graphCutPerformed = true;
        return graphCut;
    }

}
