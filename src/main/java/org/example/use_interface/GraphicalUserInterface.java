package org.example.use_interface;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

public class GraphicalUserInterface extends JPanel {

    private BufferedImage image;
    Point srcPoint;
    Point sinkPoint;
    List<CustomPoint> objSeedSet;
    List<CustomPoint> bkgSeedSet;
    boolean selectingObjSeed = false;
    boolean selectingBkgSeed = false;


    public GraphicalUserInterface(BufferedImage image){
        this.image = image;
        this.objSeedSet = new ArrayList<>();
        this.bkgSeedSet = new ArrayList<>();
        this.setPreferredSize(new java.awt.Dimension(image.getWidth(), image.getHeight()));

        addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e){
                Point clicked = e.getPoint();

                if(srcPoint == null){
                    srcPoint = clicked;
                } else if(sinkPoint == null){
                    sinkPoint = clicked;
                    selectingObjSeed = true;
                } else {
                    if(selectingObjSeed){
                        objSeedSet.add(new CustomPoint(clicked.x, clicked.y));
                    } else if(selectingBkgSeed){
                        bkgSeedSet.add(new CustomPoint(clicked.x, clicked.y));
                    }
                }

                repaint();
            }
        });
    }


    @Override
    protected void paintComponent(Graphics g){
        super.paintComponent(g);

        g.drawImage(image, 0, 0, null);

        // paints source and sink pixels

        if(srcPoint != null){
            g.setColor(Color.RED);
            g.fillOval(srcPoint.x - 4, srcPoint.y - 4, 8, 8);
        }

        if (sinkPoint != null) {
            g.setColor(Color.BLUE);
            g.fillOval(sinkPoint.x - 4, sinkPoint.y - 4, 8, 8);
        }

        //paints all pixels in O and B sets

        if(!objSeedSet.isEmpty()) {
            g.setColor(Color.RED);
            for (CustomPoint p : objSeedSet) {
                g.fillOval(p.x() - 4, p.y() - 4, 8, 8);
            }
        }

        if(!bkgSeedSet.isEmpty()) {
            g.setColor(Color.BLUE);
            for (CustomPoint p : bkgSeedSet) {
                g.fillOval(p.x() - 4, p.y() - 4, 8, 8);
            }
        }

    }

    /**
     * change the BufferedImage instance to be displayed in panel
     */
    public void setImage(BufferedImage image){
        this.image = image;
    }

    /**
     * changes from hard obj selection to hard bkg selection and vice versa
     */
    public void toggleSelectingObjSeed(){
        assert srcPoint != null && sinkPoint != null;

        this.selectingObjSeed = !selectingObjSeed;
        this.selectingBkgSeed = !selectingObjSeed;
    }

    /**
     *
     */
    public record CustomPoint(int x, int y){
        public int pointToIndex(int imgWidth){
            return y*imgWidth + x;
        }
    }


    public CustomPoint getSrcLoc(){
        return new CustomPoint(srcPoint.x, srcPoint.y);
    }


    public CustomPoint getSinkLoc(){
        return new CustomPoint(sinkPoint.x, sinkPoint.y);
    }

    public List<CustomPoint> getObjSeedSet(){
        return objSeedSet;
    }

    public List<CustomPoint> getBkgSeedSet(){
        return bkgSeedSet;
    }


}
