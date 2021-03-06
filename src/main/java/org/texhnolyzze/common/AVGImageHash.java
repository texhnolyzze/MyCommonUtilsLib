package org.texhnolyzze.common;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 *
 * @author Texhnolyze
 */
public class AVGImageHash {
    
    private BitBuffer hash;
    
    private BufferedImage mask;
    private final BufferedImage img;
    
    public AVGImageHash(BufferedImage img) {
        this.img = img;
    }

    public BufferedImage img() {
        return img;
    }
    
    public BufferedImage mask() {
        if (hash == null)
            hash = calculateHash();
        return mask;
    }
    
    public BitBuffer hash() {
        if (hash == null)
            hash = calculateHash();
        return hash;
    }
    
    public int hammingDistance(AVGImageHash other) {
        return BitUtils.hammingDistance(hash(), other.hash());
    }
    
    private static final int N = 8;
    
    private BufferedImage scale() {
        BufferedImage scaled = new BufferedImage(N, N, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.drawImage(img, 0, 0, N, N, null);
        g.dispose();
        return scaled;
    }
    
    private void toGrayscale(BufferedImage img) {
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                int rgb = img.getRGB(x, y);
                int b = rgb & 0xff;
                int g = (rgb & 0xff00) >> 8;
                int r = (rgb & 0xff0000) >> 16;
                int grayByte = toGrayByteValue(r, g, b);
                img.setRGB(x, y, grayByte);
            }
        }
    }
    
    private int calculateAVGColor(BufferedImage img) {
        int avg = 0;
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                avg += (img.getRGB(x, y) & 0xff);
            }
        }
        return avg / (img.getWidth() * img.getHeight());
    }
    
    private BitBuffer buildBitsChain(BufferedImage img, int avg) {
        BitBuffer bb = new BitBuffer((img.getWidth() * img.getHeight()) / BitBuffer.R);
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                if ((img.getRGB(x, y) & 0xff) > avg) {
                    bb.append(1);
                    img.setRGB(x, y, Color.BLACK.getRGB());
                } else {
                    bb.append(0);
                    img.setRGB(x, y, Color.WHITE.getRGB());
                }
            }
        }
        return bb;
    }
    
    private BitBuffer calculateHash() {
        BufferedImage scaled = scale();
        toGrayscale(scaled);
        mask = scaled;
        int avg = calculateAVGColor(mask);
        return buildBitsChain(mask, avg);
    }

    @Override
    public String toString() {
        return hash().toString();
    }
    
    private static final double GRAY_VECTOR_LEN = Math.sqrt(1 + 1 + 1);
    private static final double UNIT_GRAY_VECTOR_COMPONENT = 1.0 / GRAY_VECTOR_LEN;
    
    private static int toGrayByteValue(int r, int g, int b) {
        double rgbLenSqr = r * r + g * g + b * b;
        int x = -r;
        int y = -g;
        int z = -b;
        int i = y - z;
        int j = z - x;
        int k = x - y; // (i, j, k) is the cross product: (-r, -g, -b) x (1, 1, 1)
        double crossLen = Math.sqrt(i * i + j * j + k * k);
        double distFromGrayLine = crossLen / GRAY_VECTOR_LEN;
        double len = Math.sqrt(rgbLenSqr - distFromGrayLine * distFromGrayLine);
        return (int) (UNIT_GRAY_VECTOR_COMPONENT * len);
    }
    
    public static double getSimilarityPercentage(AVGImageHash p1, AVGImageHash p2) {
        double d = (double) p1.hammingDistance(p2) / (N * N);
        return 1.0 - d;
    }
    
}
