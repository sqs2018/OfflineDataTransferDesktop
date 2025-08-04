package com.sqs.util;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Base64;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;


public class FileSplitter {
    public static int DEFAULT_SPLIT_LENGTH = 700; // Default split length of 1 MB
    private static int DEFAULT_QR_CODE_WIDTH = 400;
    private static int DEFAULT_QR_CODE_HEIGHT = 400;
    public static FileSplitterInfo getFileSplitterInfo(File inputFile) {
        FileSplitterInfo fileSplitterInfo = new FileSplitterInfo();
        fileSplitterInfo.setFileName(inputFile.getName());
        fileSplitterInfo.setFileSize(inputFile.length());
        fileSplitterInfo.setSplitLength(inputFile.length()/DEFAULT_SPLIT_LENGTH + (inputFile.length() % DEFAULT_SPLIT_LENGTH == 0 ? 0 : 1)); // Calculate number of splits
        return fileSplitterInfo;
    }


    public static ImageIcon generateBinaryQRCode(long seq,byte[] binaryData)
            throws WriterException, IOException {
        // Convert binary data to a Base64 string or directly encode it
        //String data = new String(binaryData);
        String base64Str=Base64.getEncoder().encodeToString(binaryData);

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(seq+":::"+base64Str, BarcodeFormat.QR_CODE, DEFAULT_QR_CODE_WIDTH, DEFAULT_QR_CODE_HEIGHT);

        BufferedImage image = new BufferedImage(DEFAULT_QR_CODE_WIDTH, DEFAULT_QR_CODE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < DEFAULT_QR_CODE_WIDTH; x++) {
            for (int y = 0; y < DEFAULT_QR_CODE_HEIGHT; y++) {
                image.setRGB(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }

        // Convert BufferedImage to ImageIcon
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return new ImageIcon(image);
    }



}