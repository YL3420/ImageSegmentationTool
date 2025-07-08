package org.example.imaging;

public class HistogramModel {

    private final int[] distrO = new int[256];
    private final int[] distrB = new int[256];
    private int objTotal = 0;
    private int bkgTotal = 0;


    public void addObjSeed(int intensity){
        distrO[intensity]++;
        objTotal++;
    }

    public void addBkgSeed(int intensity){
        distrB[intensity]++;
        bkgTotal++;
    }

    public double pObj(int intensity){
        return (distrO[intensity] + 1.0) / (objTotal + 256.0);
    }

    public double pBkg(int intensity){
        return (distrB[intensity] + 1.0) / (bkgTotal + 256.0);
    }

    public long objEnergy(int intensity){
        return (long)(-Math.log(pObj(intensity)) * 100);
    }

    public long bkgEnergy(int intensity){
        return (long)(-Math.log(pBkg(intensity)) * 100);
    }

}
